import { useSyncExternalStore } from 'react';
import { engine, type Snapshot } from './engine/workoutEngine';

/** The engine's current state; re-renders on every change it emits (reps, status, the clock at 10 Hz). */
export function useEngine(): Snapshot {
  return useSyncExternalStore(engine.subscribe, engine.getSnapshot, engine.getSnapshot);
}
