import { useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';

export interface LadderUpdateMessage {
  resource: string;
  action: string;
  payload?: Record<string, unknown>;
}

export function useLadderUpdates(
  ladderId: string | undefined,
  userId: string | undefined,
  onUpdate: (message: LadderUpdateMessage) => void,
) {
  const onUpdateRef = useRef(onUpdate);
  onUpdateRef.current = onUpdate;

  useEffect(() => {
    if (!ladderId || !userId) return;

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//${window.location.host}/ws`;

    const client = new Client({
      brokerURL: wsUrl,
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      // Pass X-User-Id in STOMP CONNECT headers so the backend LadderStompSessionListener
      // can identify the subscribing user for presence tracking.
      connectHeaders: {
        'X-User-Id': userId,
      },
    });

    client.onConnect = () => {
      // Pass X-User-Id on the SUBSCRIBE frame as well as CONNECT. Spring's
      // SessionSubscribeEvent only exposes subscribe-frame native headers;
      // the CONNECT-frame headers aren't propagated to subscribe events, so
      // without this LadderStompSessionListener can't identify the user and
      // drops the subscription from presence tracking.
      client.subscribe(
        `/topic/ladders/${ladderId}`,
        (message) => {
          const body: LadderUpdateMessage = JSON.parse(message.body);
          onUpdateRef.current(body);
        },
        { 'X-User-Id': userId },
      );
    };

    client.activate();

    return () => {
      client.deactivate();
    };
  }, [ladderId, userId]);
}
