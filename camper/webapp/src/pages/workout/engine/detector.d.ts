// Type surface of detector.js that the workout page uses. The file itself is a verbatim copy of
// rep-counter/detector.js (the tuned source of truth); appearance.js imports median/clamp from it.
export function toGray(rgba: Uint8ClampedArray, out: Float32Array): Float32Array;
export function clamp(v: number, lo: number, hi: number): number;
export function median(values: number[]): number;
