import React, {
  createContext,
  useContext,
  useEffect,
  useRef,
  useState,
  useCallback,
} from 'react';
import { Client } from '@stomp/stompjs';
import type { IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { getAuthToken } from '../api/client';

type WebSocketStatus = 'connecting' | 'connected' | 'disconnected' | 'error';

interface WebSocketContextValue {
  status: WebSocketStatus;
  subscribe: (destination: string, handler: (payload: unknown) => void) => () => void;
}

const WebSocketContext = createContext<WebSocketContextValue | null>(null);

interface WebSocketProviderProps {
  children: React.ReactNode;
}

const WS_RECONNECT_DELAY_MS = 3000;

export function WebSocketProvider({ children }: WebSocketProviderProps) {
  const [status, setStatus] = useState<WebSocketStatus>('disconnected');
  const clientRef = useRef<Client | null>(null);
  const handlersRef = useRef<Map<string, Set<(payload: unknown) => void>>>(
    new Map()
  );
  const mountedRef = useRef(true);

  useEffect(() => {
    mountedRef.current = true;
    const token = getAuthToken();
    if (!token) return;

    const stompClient = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: { token },
      reconnectDelay: WS_RECONNECT_DELAY_MS,
      onConnect: () => {
        if (mountedRef.current) {
          setStatus('connected');
        }
        // Re-subscribe existing handlers after reconnect
        handlersRef.current.forEach((_handlers, destination) => {
          stompClient.subscribe(destination, (message: IMessage) => {
            try {
              const body = JSON.parse(message.body);
              const destHandlers = handlersRef.current.get(destination);
              if (destHandlers) {
                destHandlers.forEach((handler) => handler(body));
              }
            } catch {
              // Ignore malformed messages
            }
          });
        });
      },
      onDisconnect: () => {
        if (mountedRef.current) {
          setStatus('disconnected');
        }
      },
      onStompError: () => {
        if (mountedRef.current) {
          setStatus('error');
        }
      },
      onWebSocketClose: () => {
        if (mountedRef.current) {
          setStatus('disconnected');
        }
      },
    });

    setStatus('connecting');
    clientRef.current = stompClient;
    stompClient.activate();

    return () => {
      mountedRef.current = false;
      if (stompClient.active) {
        stompClient.deactivate();
      }
      clientRef.current = null;
    };
  }, []);

  const subscribe = useCallback(
    (destination: string, handler: (payload: unknown) => void): (() => void) => {
      if (!handlersRef.current.has(destination)) {
        handlersRef.current.set(destination, new Set());
      }
      handlersRef.current.get(destination)!.add(handler);

      // If already connected, subscribe immediately
      const client = clientRef.current;
      let stompSub: { unsubscribe: () => void } | null = null;

      if (client && client.connected) {
        stompSub = client.subscribe(destination, (message: IMessage) => {
          try {
            const body = JSON.parse(message.body);
            const destHandlers = handlersRef.current.get(destination);
            if (destHandlers) {
              destHandlers.forEach((h) => h(body));
            }
          } catch {
            // Ignore malformed messages
          }
        });
      }

      return () => {
        handlersRef.current.get(destination)?.delete(handler);
        if (handlersRef.current.get(destination)?.size === 0) {
          handlersRef.current.delete(destination);
        }
        if (stompSub) {
          stompSub.unsubscribe();
        }
      };
    },
    []
  );

  return (
    <WebSocketContext.Provider value={{ status, subscribe }}>
      {children}
    </WebSocketContext.Provider>
  );
}

export function useWebSocketContext(): WebSocketContextValue {
  const ctx = useContext(WebSocketContext);
  if (!ctx) {
    throw new Error(
      'useWebSocketContext must be used inside a WebSocketProvider'
    );
  }
  return ctx;
}
