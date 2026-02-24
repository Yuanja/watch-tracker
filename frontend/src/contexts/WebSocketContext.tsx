import React, {
  createContext,
  useContext,
  useEffect,
  useRef,
  useState,
  useCallback,
} from 'react';
import { getAuthToken } from '../api/client';

type WebSocketStatus = 'connecting' | 'connected' | 'disconnected' | 'error';

interface WebSocketMessage {
  type: string;
  payload: unknown;
}

interface WebSocketContextValue {
  status: WebSocketStatus;
  subscribe: (type: string, handler: (payload: unknown) => void) => () => void;
}

const WebSocketContext = createContext<WebSocketContextValue | null>(null);

interface WebSocketProviderProps {
  children: React.ReactNode;
}

const WS_RECONNECT_DELAY_MS = 3000;
const WS_URL = '/ws/trade-intel';

export function WebSocketProvider({ children }: WebSocketProviderProps) {
  const [status, setStatus] = useState<WebSocketStatus>('disconnected');
  const wsRef = useRef<WebSocket | null>(null);
  const handlersRef = useRef<Map<string, Set<(payload: unknown) => void>>>(
    new Map()
  );
  const reconnectTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const mountedRef = useRef(true);

  const connect = useCallback(() => {
    const token = getAuthToken();
    if (!token) return;

    // Build WebSocket URL with token as query param
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const url = `${protocol}//${window.location.host}${WS_URL}?token=${token}`;

    setStatus('connecting');
    const ws = new WebSocket(url);
    wsRef.current = ws;

    ws.onopen = () => {
      if (mountedRef.current) {
        setStatus('connected');
      }
    };

    ws.onmessage = (event) => {
      try {
        const message: WebSocketMessage = JSON.parse(event.data as string);
        const handlers = handlersRef.current.get(message.type);
        if (handlers) {
          handlers.forEach((handler) => handler(message.payload));
        }
      } catch {
        // Ignore malformed messages
      }
    };

    ws.onerror = () => {
      if (mountedRef.current) {
        setStatus('error');
      }
    };

    ws.onclose = () => {
      if (mountedRef.current) {
        setStatus('disconnected');
        // Attempt reconnect after delay
        reconnectTimerRef.current = setTimeout(() => {
          if (mountedRef.current) {
            connect();
          }
        }, WS_RECONNECT_DELAY_MS);
      }
    };
  }, []);

  useEffect(() => {
    mountedRef.current = true;
    connect();

    return () => {
      mountedRef.current = false;
      if (reconnectTimerRef.current) {
        clearTimeout(reconnectTimerRef.current);
      }
      if (wsRef.current) {
        wsRef.current.close();
        wsRef.current = null;
      }
    };
  }, [connect]);

  const subscribe = useCallback(
    (type: string, handler: (payload: unknown) => void): (() => void) => {
      if (!handlersRef.current.has(type)) {
        handlersRef.current.set(type, new Set());
      }
      handlersRef.current.get(type)!.add(handler);

      return () => {
        handlersRef.current.get(type)?.delete(handler);
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
