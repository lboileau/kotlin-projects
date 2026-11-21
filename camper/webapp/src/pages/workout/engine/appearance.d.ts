// Type surface of appearance.js (a verbatim copy of rep-counter/appearance.js, the counter tuned on
// real recordings on 2026-11-21). Only what the workout engine touches is declared.
export interface Box {
  x: number;
  y: number;
  w: number;
  h: number;
}

export interface RepEvent {
  type: 'rep';
  n: number;
  t: number;
  at: number;
  amp: number;
  conf: number;
  interval: number | null;
  period: number | null;
}

export interface CorrectionEvent {
  type: 'correction';
  delta: number;
  n: number;
  at: number;
}

export interface RecalibrateEvent {
  type: 'recalibrate';
  at: number;
  reason: string;
  n: number;
}

export type CounterEvent = RepEvent | CorrectionEvent | RecalibrateEvent;

export interface SampleResult {
  t: number;
  s: number;
  period: number | null;
  periodValid: boolean;
  pi: number;
  coherence: number;
  count: number;
  event: RepEvent | null;
}

export interface AppearanceOptions {
  fs?: number;
  cellPx?: number | null;
  grid?: number;
  maxGrid?: number;
}

export class AppearanceCounter {
  constructor(opts?: AppearanceOptions);
  opts: Required<AppearanceOptions> & Record<string, unknown>;
  gx: number;
  gy: number;
  D: number;
  count: number;
  manual: number;
  events: CounterEvent[];
  lastRepAt: number | null;
  period: number | null;
  pi: number;
  periodValid: boolean;
  everValid: boolean;
  idle: boolean;
  nFocus: number;
  /** Per-cell z-score for the overlay (blue/amber tint). */
  zRaw: Float32Array;
  /** 1 for cells in focus (where the motion is), 0 otherwise. */
  mask: Uint8Array;
  readonly rom: number | null;
  readonly requiredAmp: number;
  readonly activeCells: number;
  setROI(roi: Box): void;
  resetCount(): void;
  resync(): void;
  recalibrate(t: number, reason?: string): RecalibrateEvent;
  adjust(delta: number, at: number): CorrectionEvent;
  update(gray: Float32Array, W: number, H: number, t: number): SampleResult | null;
}
