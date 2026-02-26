import { useEffect } from 'react';
import { useWebSocketContext } from '../contexts/WebSocketContext';

/**
 * Subscribe to a STOMP destination for real-time updates.
 * The subscription is automatically cleaned up when the component unmounts.
 *
 * @param destination  The STOMP destination to subscribe to (e.g. '/topic/listings')
 * @param handler      Callback invoked with the parsed message body
 */
export function useWebSocket<T = unknown>(
  destination: string,
  handler: (payload: T) => void
): void {
  const { subscribe } = useWebSocketContext();

  useEffect(() => {
    const unsubscribe = subscribe(destination, handler as (payload: unknown) => void);
    return unsubscribe;
  }, [destination, handler, subscribe]);
}
