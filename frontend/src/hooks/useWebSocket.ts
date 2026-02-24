import { useEffect } from 'react';
import { useWebSocketContext } from '../contexts/WebSocketContext';

/**
 * Subscribe to a specific WebSocket message type.
 * The subscription is automatically cleaned up when the component unmounts.
 *
 * @param type     The message type to listen for
 * @param handler  Callback invoked with the message payload
 */
export function useWebSocket<T = unknown>(
  type: string,
  handler: (payload: T) => void
): void {
  const { subscribe } = useWebSocketContext();

  useEffect(() => {
    const unsubscribe = subscribe(type, handler as (payload: unknown) => void);
    return unsubscribe;
  }, [type, handler, subscribe]);
}
