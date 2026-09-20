import { createContext } from 'react';

export interface MealPlanSyncMessage {
  resource: string;
  action: string;
}

/**
 * A subscription handler's message is `null` specifically on reconnect —
 * a signal that means "treat everything on this topic as possibly
 * changed" rather than a parsed STOMP frame body.
 */
export type SyncMessage = MealPlanSyncMessage | null;

export interface SyncContextValue {
  /** Subscribes to a topic; returns an unsubscribe function. Reference-counted — the underlying STOMP subscription is only torn down once every caller for that topic has unsubscribed. */
  subscribe: (topic: string, handler: (message: SyncMessage) => void) => () => void;
}

export const SyncContext = createContext<SyncContextValue | null>(null);
