import { useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';

export interface PlanUpdateMessage {
  resource: string;
  action: string;
}

export function usePlanUpdates(
  planId: string | undefined,
  onUpdate: (message: PlanUpdateMessage) => void,
) {
  const onUpdateRef = useRef(onUpdate);
  onUpdateRef.current = onUpdate;

  useEffect(() => {
    if (!planId) return;

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//${window.location.host}/ws`;

    const client = new Client({
      brokerURL: wsUrl,
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
    });

    client.onConnect = () => {
      client.subscribe(`/topic/plans/${planId}`, (message) => {
        const body: PlanUpdateMessage = JSON.parse(message.body);
        onUpdateRef.current(body);
      });
    };

    client.activate();

    return () => {
      client.deactivate();
    };
  }, [planId]);
}
