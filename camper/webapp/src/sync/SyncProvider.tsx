import { useEffect, useMemo, useRef, type ReactNode } from 'react';
import { Client, type StompSubscription } from '@stomp/stompjs';
import { useAuth } from '../auth/useAuth';
import { SyncContext, type SyncContextValue, type SyncMessage } from './SyncContext';

interface TopicEntry {
  handlers: Set<(message: SyncMessage) => void>;
  subscription: StompSubscription | null;
}

/**
 * One STOMP client for the whole app. Mounted in the authenticated shell
 * (AppShell) so it only connects once signed in, and deactivates on
 * sign-out. Individual pages subscribe to topics via `useMealPlanSync`
 * (or `useContext(SyncContext).subscribe` directly); subscriptions
 * requested before the socket is connected are queued here and
 * established on connect, and re-established after every reconnect.
 */
export function SyncProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth();
  const clientRef = useRef<Client | null>(null);
  const topicsRef = useRef<Map<string, TopicEntry>>(new Map());
  const hasConnectedBeforeRef = useRef(false);

  useEffect(() => {
    if (!user) {
      clientRef.current?.deactivate();
      clientRef.current = null;
      hasConnectedBeforeRef.current = false;
      return;
    }

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const brokerURL = `${protocol}//${window.location.host}/ws`;

    const client = new Client({
      brokerURL,
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
    });

    client.onConnect = () => {
      const isReconnect = hasConnectedBeforeRef.current;
      hasConnectedBeforeRef.current = true;

      for (const [topic, entry] of topicsRef.current) {
        entry.subscription = client.subscribe(topic, (message) => {
          const body: SyncMessage = JSON.parse(message.body);
          for (const handler of entry.handlers) handler(body);
        });
        if (isReconnect) {
          // No real message body — each handler treats this the same
          // way it treats a genuine update, just without specifics.
          for (const handler of entry.handlers) handler(null);
        }
      }
    };

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
      clientRef.current = null;
      hasConnectedBeforeRef.current = false;
    };
  }, [user]);

  const value = useMemo<SyncContextValue>(
    () => ({
      subscribe(topic, handler) {
        let entry = topicsRef.current.get(topic);
        if (!entry) {
          entry = { handlers: new Set(), subscription: null };
          topicsRef.current.set(topic, entry);

          const client = clientRef.current;
          if (client?.connected) {
            const thisEntry = entry;
            thisEntry.subscription = client.subscribe(topic, (message) => {
              const body: SyncMessage = JSON.parse(message.body);
              for (const h of thisEntry.handlers) h(body);
            });
          }
        }

        entry.handlers.add(handler);

        return () => {
          const current = topicsRef.current.get(topic);
          if (!current) return;
          current.handlers.delete(handler);
          if (current.handlers.size === 0) {
            current.subscription?.unsubscribe();
            topicsRef.current.delete(topic);
          }
        };
      },
    }),
    [],
  );

  return <SyncContext.Provider value={value}>{children}</SyncContext.Provider>;
}
