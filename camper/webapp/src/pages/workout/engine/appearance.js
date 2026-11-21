// appearance.js — appearance-based repetition counter ("grid mode").
//
// Counts repetitive motion that changes how a region LOOKS rather than where a patch IS:
// a hand pronating, a forearm twisting, a fist lifting — anything that repeats in front of the camera.
// Tuned on real recordings (five phone clips of 10 reps each: fist twists, wrist curls, open-hand
// waves, close and far) with tools/replay-video.mjs, and on the synthetic suite in test/appearance.test.mjs.
// The pipeline, per frame (the README's "Appearance mode" section has the full story):
//
//   frame → grid of cell means (16 px cells over the whole frame), resampled to a fixed 30 Hz
//         → log domain ×100 (a gain change is the same offset in every cell) → common mode (median) removed
//           (a 0.3–4 Hz band-passed copy g(t) feeds the period search only)
//         → FOCUS: per-cell frame-difference energy (fast attack, 1 s decay); the cells with ≥ 30 % of the
//           strongest cell's energy above the noise floor (the quietest fifth of the cells), at most 64, take
//           part in everything below. Body sway and the background are excluded without any box being drawn.
//           When even the strongest cell is at the noise floor the counter is idle.
//         → POSE p(t): the per-cell change integrated only in frames where the cell actually changes (motion
//           gated), so a pose held still stays put and slow drift never accumulates; blurred over the grid
//           (σ ≈ 1 cell) so an object sliding across cell borders traces a smooth path.
//         → STROKES by the farthest-point rule: from an anchor pose the RMS distance (per moving cell) grows
//           during a stroke and shrinks on the way back. When it has fallen hystFrac (and ≥ hyst) below its
//           maximum, and the progress along the anchor→far chord has fallen as much (sideways wandering is not
//           a return), and the motion had slowed at the far point (a reversal is a velocity minimum), the far
//           point was a reversal and becomes the next anchor.
//         → REPS: a reciprocating movement is two strokes, so a rep is counted at every odd stroke — the far
//           end reached, confirmed as soon as the return begins. A stroke counts if it is ≥ ampFrac of the
//           typical recent stroke (first: ≥ firstAmp) and not within the refractory. Nothing is ever retracted.
//         → STALLS: a stroke whose far point stops advancing for stallTime has ended there (a hold, or a move
//           into position); the next stroke starts from that point, and the stalled one counts once the next
//           stroke grows to ampFrac of its size, else it is dropped.
//         → period: multivariate autocorrelation over the focus cells, lag picked by prominence; it only
//           tunes the refractory and the stall time and is shown as "periodicity".
//
// Why not the earlier principal-component projection (appearance-v1.js): on real clips the leading component
// locked onto body sway and drift (many weakly moving cells outvote the few with the rep), and an axis
// cannot be learned before the first rep. The farthest-point rule needs no axis and no learning.
//
// No DOM. Shares the interface shape of RepDetector/PatternCounter (count, events, adjust, resync,
// lastRepAt) so the app can use it as a signal source.

import { median, clamp } from './detector.js';

/** RBJ cookbook biquad coefficients (normalised so a0 = 1). type: 'hp' | 'lp'. */
export function biquad(type, fc, fs, Q = Math.SQRT1_2) {
  const w0 = 2 * Math.PI * fc / fs, cw = Math.cos(w0), sw = Math.sin(w0), alpha = sw / (2 * Q);
  let b0, b1, b2;
  if (type === 'hp') { b0 = (1 + cw) / 2; b1 = -(1 + cw); b2 = (1 + cw) / 2; }
  else { b0 = (1 - cw) / 2; b1 = 1 - cw; b2 = (1 - cw) / 2; }
  const a0 = 1 + alpha;
  return { b0: b0 / a0, b1: b1 / a0, b2: b2 / a0, a1: -2 * cw / a0, a2: (1 - alpha) / a0 };
}

/** One biquad applied to D parallel channels (direct form I). */
class BiquadBank {
  constructor(coef, D) { this.c = coef; this.x1 = new Float32Array(D); this.x2 = new Float32Array(D); this.y1 = new Float32Array(D); this.y2 = new Float32Array(D); this.primed = false; }
  /** Start in steady state for a constant input (gainDC = 0 for a high-pass, 1 for a low-pass) so there is no start-up transient. */
  prime(x, gainDC) { for (let i = 0; i < x.length; i++) { this.x1[i] = this.x2[i] = x[i]; this.y1[i] = this.y2[i] = gainDC * x[i]; } this.primed = true; }
  run(x, out) {
    const { b0, b1, b2, a1, a2 } = this.c, x1 = this.x1, x2 = this.x2, y1 = this.y1, y2 = this.y2;
    for (let i = 0; i < x.length; i++) {
      const y = b0 * x[i] + b1 * x1[i] + b2 * x2[i] - a1 * y1[i] - a2 * y2[i];
      x2[i] = x1[i]; x1[i] = x[i]; y2[i] = y1[i]; y1[i] = y; out[i] = y;
    }
    return out;
  }
}

/** Turning points of a signal with hysteresis h: [{k, v, kind:'min'|'max'}], alternating. */
export function turningPoints(arr, h) {
  const out = []; if (!arr.length) return out;
  let dir = 0, ext = arr[0], extK = 0, lo = arr[0], loK = 0, hi = arr[0], hiK = 0;
  for (let k = 1; k < arr.length; k++) {
    const x = arr[k];
    if (dir === 0) {
      if (x < lo) { lo = x; loK = k; } if (x > hi) { hi = x; hiK = k; }
      if (x - lo > h) { out.push({ k: loK, v: lo, kind: 'min' }); dir = 1; ext = x; extK = k; }
      else if (hi - x > h) { out.push({ k: hiK, v: hi, kind: 'max' }); dir = -1; ext = x; extK = k; }
    } else if (dir > 0) {
      if (x > ext) { ext = x; extK = k; } else if (ext - x > h) { out.push({ k: extK, v: ext, kind: 'max' }); dir = -1; ext = x; extK = k; }
    } else {
      if (x < ext) { ext = x; extK = k; } else if (x - ext > h) { out.push({ k: extK, v: ext, kind: 'min' }); dir = 1; ext = x; extK = k; }
    }
  }
  return out;
}

export class AppearanceCounter {
  constructor(opts = {}) {
    this.opts = Object.assign({
      grid: 10,            // cells per side over the ROI (square grid) …
      cellPx: 16,          // … or, when set, aim for cells about this many pixels across (grid adapts to the box; 4–24 per side)
      maxGrid: 24,
      fs: 30,              // analysis rate, Hz (frames are averaged into 1/fs bins)
      window: 240,         // ring buffer length in samples (8 s): period search and the pattern view
      lagMin: 15, lagMax: 180,   // period search range in samples (0.5–6 s)
      hp: 0.3, lp: 4,      // band-pass corners, Hz (0.3 Hz: body sway and slow drift go, reps up to ~3 s stay)
      sigmaFloor: 0.4, varTau: 120,   // per-cell z-scores for the overlay only
      motTau: 30,          // focus memory: per-cell motion energy forgets with this time constant (samples) …
      motAttack: 5,        // … and rises with this one, so the focus forms within a few frames of the first movement
      focusFrac: 0.3,      // a cell is in focus with at least this fraction of the strongest cell's motion energy above the quiet cells' …
      focusNoise: 5,       // … and at least this many times the quiet cells' spread (20th − 5th percentile) above them: the camera noise floor.
                           // Nothing is moving (idle) when even the strongest cell is within that of the quiet ones
      focusMax: 64,        // … capped at the strongest this many cells; fewer than focusMin ⇒ idle: every cell takes part
      focusMin: 2,         // a small far-away hand covers only a couple of cells
      acfTau: 150,         // autocorrelation forgetting (samples)
      rhoMin: 0.3, promMin: 0.3,   // a period needs ρ(T) ≥ rhoMin and ρ(T) − max ρ(0.3T..0.7T) ≥ promMin
      poseMode: 'int',     // 'int': motion-gated integral of cell changes (holds keep their pose); 'bp': the band-passed g itself
      stepMin: 0.5,        // percent change per sample a cell must show to move the pose (noise on a cell mean is ~0.1 %); scaled up with the measured noise
      poseTau: 0,          // optional leak of the pose toward zero (samples; 0 = none). Strokes are measured from anchors, so no leak is needed, and any leak moves a held pose
      hyst: 0.5,           // distance units (percent-of-brightness change per moving cell, after the blur): a stroke has reversed when the distance has fallen this much …
      hystFrac: 0.15,      // … and by this fraction of the stroke so far (a translating object still ripples the distance a few % at cell borders)
      firstAmp: 1,         // the first stroke must be at least this big (idle camera noise stays below ~0.1)
      ampFrac: 0.4,        // later strokes: at least this fraction of the typical recent stroke …
      ampMemory: 6,        // … the median of the last reversals at least firstAmp big, counted or not, so the scale follows what is being done
      strokeRefrFrac: 0.15, strokeRefr0: 0.15,   // seconds between strokes: fraction of the period, or fixed before one exists
      energyFrac: 0.7,     // a reversal is a velocity minimum: the motion energy at the far point must be below this fraction of the stroke's peak
                           // (a pose that dips mid-movement at full speed is a curved path, not a reversal), unless the pose comes back more than half way
      stallTime: 1.0,      // a stroke whose far point has not advanced for this long (or 0.6 period, if longer) has ended there — a hold, or a
                           // one-off move into position. The next stroke starts from that point; the stalled one counts once the next stroke has grown
                           // to ampFrac of its size (the hold ended, the movement came back), and is dropped if instead a smaller movement
                           // takes over there (it was a move into position)
      blur: [0.06, 0.24, 0.4, 0.24, 0.06],       // the pose is blurred over the grid (σ ≈ 1 cell) so a translating object gives a smooth distance, not a ripple at each cell border
      dilate: 2,           // a stroke's cell set is the focus grown by this many cells (where the blur spreads the change)
    }, opts);
    this.roi = null;
    this.reset();
  }

  /** Grid dimensions for the current ROI: fixed square, or sized so cells are about `cellPx` across. */
  _dims() {
    const o = this.opts;
    if (!o.cellPx || !this.roi) return [o.grid, o.grid];
    const g = v => Math.max(4, Math.min(o.maxGrid, Math.round(v / o.cellPx)));
    return [g(this.roi.w), g(this.roi.h)];
  }

  reset(keepCount = false) {
    const banked = keepCount ? (this.base || 0) + (this.strokes ? this._reps() : 0) : 0, manual = keepCount ? (this.manual || 0) : 0;
    const o = this.opts; [this.gx, this.gy] = this._dims(); const D = this.gx * this.gy; this.D = D;
    this.hpf = new BiquadBank(biquad('hp', o.hp, o.fs), D); this.lpf = new BiquadBank(biquad('lp', o.lp, o.fs), D); this.lpfPose = new BiquadBank(biquad('lp', o.lp, o.fs), D);
    this.p = new Float32Array(D); this.lPrev = null; this.l = new Float32Array(D); this.stepNoise = 0;
    this.acc = new Float32Array(D); this.accMot = new Float32Array(D); this.accN = 0; this.nextT = null; this.prevFrame = null;
    this.m = new Float32Array(D); this.mo = new Float32Array(D); this.tmp = new Float32Array(D); this.h = new Float32Array(D); this.g = new Float32Array(D); this.lg = new Float32Array(D); this.sorted = new Float32Array(D);
    this.vari = new Float32Array(D); this.zRaw = new Float32Array(D);
    this.mot = new Float32Array(D); this.mask = new Uint8Array(D).fill(1); this.nFocus = D; this.idle = true; this.order = Array.from({ length: D }, (_, i) => i);
    this.strokeSet = new Uint8Array(D); this.n0 = D; this.wasIdle = true; this.pb = new Float32Array(D); this.blurTmp = new Float32Array(D); this.dil = new Uint8Array(D);
    this.stall = null; this.farRefD = 0; this.farRefT = 0;
    this.E = 0; this.E1 = 0; this.E2 = 0; this.strokeEmax = 0; this.farE = 0;
    this.ring = new Float32Array(o.window * D); this.n = 0;
    this.R = new Float64Array(o.lagMax + 1); this.rho = new Float32Array(o.lagMax + 1);
    this.recalAt = -Infinity; this.recalN = 0;
    this.period = null; this.periodLag = 0; this.pi = 0; this.periodValid = false; this.everValid = false;
    this.anchor = null; this.prevAnchor = null; this.far = new Float32Array(D); this.farD = 0; this.farT = 0; this.parity = 1; this.s = 0;
    this.strokes = []; this.amps = []; this.sScale = null;
    this.base = banked; this.manual = manual; this.count = banked + manual; this.events = this.events || []; this.lastRepAt = null; this.t = 0;
  }

  setROI(roi) { this.roi = roi; const [gx, gy] = this._dims(); if (gx !== this.gx || gy !== this.gy) this.reset(true); }
  /** New set: forget the reps but keep the learned pattern (filters, focus, period, stroke size). */
  resetCount() { this.strokes = []; this.base = 0; this.manual = 0; this.count = 0; this.events = []; this.lastRepAt = null; }
  _reps() { return (this.strokes.length + 1) >> 1; }
  get bottom() { return null; }
  /** Stroke size needed for the next reversal to count. */
  get requiredAmp() { const o = this.opts; return this.sScale ? Math.max(o.hyst, o.ampFrac * this.sScale) : o.firstAmp; }
  get rom() { return this.sScale; }
  get partials() { return 0; }
  get coherence() { return this.periodValid ? this.pi : 0; }
  adjust(delta, at) { this.manual += delta; this.count = Math.max(0, this.base + this._reps() + this.manual); const ev = { type: 'correction', delta, n: this.count, at }; this.events.push(ev); return ev; }
  /** Forget the current stroke (after a pause or a correction); the next movement starts a fresh one. */
  resync() { this.anchor = null; this.prevAnchor = null; this.farD = 0; this.stall = null; }
  get activeCells() { return this.nFocus; }

  /** Feed a grayscale frame (Float32Array, row-major). Returns an analysis result for each 1/fs sample produced, else null. */
  update(gray, W, H, t) {
    this._cellMeans(gray, W, H, this.tmp);
    for (let i = 0; i < this.D; i++) this.acc[i] += this.tmp[i];
    if (this.prevFrame && this.prevFrame.length === gray.length) {
      this._cellMeans(gray, W, H, this.tmp, this.prevFrame);
      for (let i = 0; i < this.D; i++) this.accMot[i] += this.tmp[i];
    } else this.prevFrame = new Float32Array(gray.length);
    this.prevFrame.set(gray);
    this.accN++;
    if (this.nextT == null) this.nextT = t;
    if (t < this.nextT) return null;
    for (let i = 0; i < this.D; i++) { this.m[i] = this.acc[i] / this.accN; this.mo[i] = this.accMot[i] / this.accN; this.acc[i] = 0; this.accMot[i] = 0; }
    this.accN = 0;
    this.nextT += 1 / this.opts.fs;
    if (this.nextT < t) this.nextT = t + 1 / this.opts.fs;   // frames slower than fs: one sample per frame
    return this._process(this.m, this.mo, t);
  }

  /** Feed a precomputed cell-mean vector (and optional per-cell motion energy) at a fixed rate (tests, replay). */
  push(m, t, mo = null) { return this._process(m, mo || this.mo.fill(1), t); }

  /** Cell means of `gray` over the ROI; with `prev`, cell means of |gray − prev| (motion energy). */
  _cellMeans(gray, W, H, out, prev = null) {
    const GX = this.gx, GY = this.gy, r = this.roi || { x: 0, y: 0, w: W, h: H };
    const x0 = clamp(Math.round(r.x), 0, W - 1), y0 = clamp(Math.round(r.y), 0, H - 1);
    const x1 = clamp(Math.round(r.x + r.w), x0 + 1, W), y1 = clamp(Math.round(r.y + r.h), y0 + 1, H);
    const cw = (x1 - x0) / GX, ch = (y1 - y0) / GY;
    for (let j = 0; j < GY; j++) {
      const ya = y0 + Math.floor(j * ch), yb = Math.max(ya + 1, y0 + Math.floor((j + 1) * ch));
      for (let i = 0; i < GX; i++) {
        const xa = x0 + Math.floor(i * cw), xb = Math.max(xa + 1, x0 + Math.floor((i + 1) * cw));
        let sum = 0, n = 0;
        if (prev) { for (let y = ya; y < yb; y++) { const row = y * W; for (let x = xa; x < xb; x++) { const d = gray[row + x] - prev[row + x]; sum += d < 0 ? -d : d; n++; } } }
        else { for (let y = ya; y < yb; y++) { const row = y * W; for (let x = xa; x < xb; x++) { sum += gray[row + x]; n++; } } }
        out[j * GX + i] = sum / n;
      }
    }
    return out;
  }

  _process(m, mo, t) {
    const o = this.opts, D = this.D;
    this.t = t;
    // Log domain (×100 ≈ percent change): a gain change (flicker, auto-exposure) becomes the same
    // additive offset in every cell, which the common-mode step below removes exactly.
    for (let c = 0; c < D; c++) this.lg[c] = 100 * Math.log(Math.max(1, m[c]));
    if (!this.hpf.primed) { this.hpf.prime(this.lg, 0); this.lpf.primed = true; this.lpfPose.prime(this.lg, 1); }
    this.hpf.run(this.lg, this.tmp); this.lpf.run(this.tmp, this.h);
    // common mode = median across cells (robust to the moving minority of cells)
    this.sorted.set(this.h); this.sorted.sort();
    const common = (this.sorted[(D - 1) >> 1] + this.sorted[D >> 1]) / 2;
    this._pose();
    const lv = 1 - 1 / o.varTau, lm = 1 - 1 / o.motTau, la = 1 / o.motAttack, g = this.g, mot = this.mot;
    let motMax = 0;
    for (let c = 0; c < D; c++) {
      const v = this.h[c] - common; g[c] = v;
      this.vari[c] = lv * this.vari[c] + (1 - lv) * v * v;
      this.zRaw[c] = clamp(v / Math.max(Math.sqrt(this.vari[c]), o.sigmaFloor), -4, 4);
      mot[c] = mo[c] > mot[c] ? mot[c] + (mo[c] - mot[c]) * la : lm * mot[c] + (1 - lm) * mo[c]; if (mot[c] > motMax) motMax = mot[c];
    }
    this._focus(motMax);
    { let e = 0, k = 0; for (let c = 0; c < D; c++) if (this.mask[c]) { e += mo[c]; k++; } this.E2 = this.E1; this.E1 = this.E; this.E = k ? e / k : 0; }
    const row = (this.n % o.window) * D; this.ring.set(g, row); this.n++;
    // multivariate autocorrelation over the focus cells, with exponential forgetting
    const lr = 1 - 1 / o.acfTau, mask = this.mask, ring = this.ring;
    let r0 = 0; for (let c = 0; c < D; c++) if (mask[c]) r0 += g[c] * g[c];
    this.R[0] = lr * this.R[0] + (1 - lr) * r0;
    const maxLag = Math.min(o.lagMax, this.n - 1);
    for (let tau = 1; tau <= maxLag; tau++) {
      if (this.n - 1 - tau < this.recalN) { this.R[tau] *= lr; continue; }   // never compare a new movement with the one before it
      const prow = ((this.n - 1 - tau) % o.window) * D;
      let r = 0; for (let c = 0; c < D; c++) if (mask[c]) r += g[c] * ring[prow + c];
      this.R[tau] = lr * this.R[tau] + (1 - lr) * r;
    }
    this._pickPeriod(t);
    const event = this._stroke(t);
    return { t, s: this.s, period: this.period, periodValid: this.periodValid, pi: this.pi, coherence: this.coherence, count: this.count, event };
  }

  /** The pose: motion-gated integral of the change of the low-passed, common-mode corrected log cell means. */
  _pose() {
    const o = this.opts, D = this.D, l = this.l, p = this.p;
    if (o.poseMode === 'bp') { p.set(this.g); return; }
    this.lpfPose.run(this.lg, l); this.sorted.set(l); this.sorted.sort();
    const cm = (this.sorted[(D - 1) >> 1] + this.sorted[D >> 1]) / 2;
    for (let c = 0; c < D; c++) l[c] -= cm;
    if (!this.lPrev) { this.lPrev = Float32Array.from(l); return; }
    // noise floor of a cell step: the median |step| over all cells (most cells are static), remembered slowly
    for (let c = 0; c < D; c++) { const d = l[c] - this.lPrev[c]; this.sorted[c] = d < 0 ? -d : d; }
    this.sorted.sort(); const med = this.sorted[D >> 1];
    this.stepNoise = this.stepNoise ? 0.98 * this.stepNoise + 0.02 * med : med;
    const gate = Math.max(o.stepMin, 4 * this.stepNoise), leak = o.poseTau > 0 ? 1 - 1 / o.poseTau : 1;
    for (let c = 0; c < D; c++) { const d = l[c] - this.lPrev[c]; p[c] = leak * p[c] + ((d > gate || d < -gate) ? d : 0); this.lPrev[c] = l[c]; }
  }

  /** Focus = the cells where the motion is: ≥ focusFrac of the strongest cell's motion energy, at most focusMax of them. */
  _focus(motMax) {
    const o = this.opts, D = this.D, mask = this.mask, mot = this.mot;
    // the quietest fifth of the cells is the camera's noise floor (its 5th–20th percentile spread ≈ 0.8 σ), even inside a box where most cells move
    this.sorted.set(mot); this.sorted.sort(); const q5 = this.sorted[Math.floor(D / 20)], q20 = this.sorted[Math.floor(D / 5)], spread = Math.max(q20 - q5, 1e-3);
    this.idle = motMax < q20 + o.focusNoise * spread;
    if (this.idle) { mask.fill(1); this.nFocus = D; return; }
    const thr = q20 + Math.max(o.focusFrac * (motMax - q20), o.focusNoise * spread); let n = 0;
    for (let c = 0; c < D; c++) { mask[c] = mot[c] >= thr ? 1 : 0; n += mask[c]; }
    if (n > o.focusMax || n < o.focusMin) { const k = n > o.focusMax ? o.focusMax : o.focusMin, idx = this.order.sort((a, b) => mot[b] - mot[a]); mask.fill(0); for (let i = 0; i < k; i++) mask[idx[i]] = 1; n = k; }
    this.nFocus = n;
  }

  /** Separable blur of a grid vector with opts.blur (edge weights renormalised). */
  _blur(src, dst) {
    const k = this.opts.blur, r = (k.length - 1) >> 1, GX = this.gx, GY = this.gy, tmp = this.blurTmp;
    for (let j = 0; j < GY; j++) for (let i = 0; i < GX; i++) { let s = 0, w = 0; for (let d = -r; d <= r; d++) { const ii = i + d; if (ii < 0 || ii >= GX) continue; s += k[d + r] * src[j * GX + ii]; w += k[d + r]; } tmp[j * GX + i] = s / w; }
    for (let j = 0; j < GY; j++) for (let i = 0; i < GX; i++) { let s = 0, w = 0; for (let d = -r; d <= r; d++) { const jj = j + d; if (jj < 0 || jj >= GY) continue; s += k[d + r] * tmp[jj * GX + i]; w += k[d + r]; } dst[j * GX + i] = s / w; }
    return dst;
  }
  /** Box dilation of a cell mask by opts.dilate cells. */
  _dilate(src, dst) {
    const r = this.opts.dilate, GX = this.gx, GY = this.gy; dst.fill(0);
    for (let j = 0; j < GY; j++) for (let i = 0; i < GX; i++) if (src[j * GX + i]) for (let dj = -r; dj <= r; dj++) for (let di = -r; di <= r; di++) { const ii = i + di, jj = j + dj; if (ii >= 0 && ii < GX && jj >= 0 && jj < GY) dst[jj * GX + ii] = 1; }
    return dst;
  }
  /** Distance between the current (blurred) pose and a stored one over the stroke's cells, normalised by the number of cells that were moving when the stroke started (so it reads as change per moving cell, whatever the object's size). */
  _dist(pose) {
    const g = this.pb, set = this.strokeSet; let s = 0;
    for (let i = 0; i < this.D; i++) if (set[i]) { const d = g[i] - pose[i]; s += d * d; }
    return Math.sqrt(s / Math.max(1, this.n0));
  }
  /** Progress of the current pose along the stroke's chord (anchor → far point), same units as _dist: it falls only when the
   *  movement comes back, not when the pose wanders sideways (a curved path, a cell-border ripple). */
  _progress() {
    const g = this.pb, a = this.anchor, f = this.far, set = this.strokeSet; let dot = 0, nn = 0;
    for (let i = 0; i < this.D; i++) if (set[i]) { const u = f[i] - a[i]; dot += (g[i] - a[i]) * u; nn += u * u; }
    return nn > 1e-12 ? dot / Math.sqrt(nn) / Math.sqrt(Math.max(1, this.n0)) : 0;
  }
  /** A stroke starts from the current pose with the cells in focus now (grown by the blur radius); cells that start moving later join it, none leave. */
  _startStroke(t) {
    const set = this.strokeSet; if (this.idle) set.fill(1); else this._dilate(this.mask, set); this.n0 = Math.max(1, this.nFocus);
    if (!this.anchor) this.anchor = new Float32Array(this.D);
    this.anchor.set(this.pb); this.far.set(this.pb); this.farD = 0; this.farT = t; this.farRefD = 0; this.farRefT = t; this.strokeEmax = 0; this.farE = 0;
  }
  /** The stroke ended without coming back (hold, or a move into a new position): remember it, start the next stroke from its far point. */
  _stallStroke(t) {
    if (this.farD >= this.requiredAmp) {   // a stroke worth remembering; a smaller fidget just re-anchors
      if (this.stall) this._resolveStall(t, false);   // a stroke of the new movement's size went by without the pose leaving the stalled one: that was a move into position
      this.stall = { t: this.farT, amp: this.farD };
      if (this.opts.debug) (this.reversals ||= []).push({ t: this.farT, amp: this.farD, ok: false, stall: true, need: this.requiredAmp, confirmedAt: t });
    }
    this.prevAnchor = Float32Array.from(this.anchor); this.anchor.set(this.far); this.parity = -this.parity;
    this._dilate(this.mask, this.strokeSet); this.n0 = Math.max(1, this.nFocus);
    this.farD = 0; this.farT = t; this.far.set(this.pb); this.farRefD = 0; this.farRefT = t; this.strokeEmax = 0; this.farE = 0;
  }

  /** Farthest-point stroke detector (see the header). Returns a rep event when an odd stroke is accepted. */
  _stroke(t) {
    const o = this.opts, set = this.strokeSet, wasIdle = this.wasIdle; this.wasIdle = this.idle;
    this._blur(this.p, this.pb); const g = this.pb;
    if (this.idle && !wasIdle && this.anchor && this.farD >= o.hyst) { this._stallStroke(t); this.s = 0; return null; }   // the movement stopped out there
    if (!this.anchor || this.idle || wasIdle) { this._startStroke(t); this.s = 0; return null; }   // nothing moves: keep starting here, so the first stroke begins where the motion does
    const dil = this._dilate(this.mask, this.dil); for (let c = 0; c < this.D; c++) if (dil[c]) set[c] = 1;   // joiners
    const d = this._dist(this.anchor);
    this.s = this.parity * (this.prevAnchor ? this._dist(this.prevAnchor) - d : -d);   // display: a triangle wave between the two latest poses
    const E = this._energy; if (E > this.strokeEmax) this.strokeEmax = E;
    if (d > this.farD) { this.farD = d; this.farT = t; this.far.set(g); this.farE = E; if (d > this.farRefD + o.hyst) { this.farRefD = d; this.farRefT = t; } }
    let ev = null;
    if (this.stall && this.farD >= o.ampFrac * this.stall.amp) ev = this._resolveStall(t, true);   // a comparable movement followed (back, or on): the stalled stroke was real
    // a reversal: the pose has come back toward the anchor (distance AND progress along the chord), from a far point where the motion had slowed
    const H = Math.max(o.hyst, o.hystFrac * this.farD), fallD = this.farD - d;
    if (fallD > H && this.farD - this._progress() > H && (this.farE <= o.energyFrac * this.strokeEmax || fallD > 0.5 * this.farD)) return this._reversal(t) || ev;
    const stallTime = this.periodValid ? Math.max(o.stallTime, 0.6 * this.period) : o.stallTime;
    if (this.farD >= o.hyst && t - this.farRefT >= stallTime) this._stallStroke(t);
    return ev;
  }

  get _energy() { return (this.E + this.E1 + this.E2) / 3; }
  /** Settle a stalled stroke: accepted, it joins the strokes (a rep if odd); dropped, it was a move into position. */
  _resolveStall(t, accept) {
    const o = this.opts, st = this.stall; this.stall = null; let ev = null;
    if (this.opts.debug) (this.reversals ||= []).push({ t: st.t, amp: st.amp, ok: accept && st.amp >= this.requiredAmp, stallResolved: true, need: this.requiredAmp, confirmedAt: t });
    if (!accept) return null;
    if (st.amp >= o.firstAmp) { this.amps.push(st.amp); if (this.amps.length > o.ampMemory) this.amps.shift(); this.sScale = median(this.amps); }
    if (st.amp >= this.requiredAmp) { this.strokes.push({ t: st.t, amp: st.amp }); if (this.strokes.length % 2 === 1) ev = this._rep(st.t, st.amp, t); }
    return ev;
  }

  /** The farthest point of the current stroke was a reversal: judge it, then start the next stroke from there. */
  _reversal(t) {
    const o = this.opts, g = this.pb;
    const amp = this.farD, at = this.farT;
    let ev = null;
    const last = this.strokes[this.strokes.length - 1];
    const refr = this.period ? o.strokeRefrFrac * this.period : o.strokeRefr0;
    const ok = amp >= this.requiredAmp && (!last || at - last.t >= refr);
    if (this.opts.debug) (this.reversals ||= []).push({ t: at, amp, ok, need: this.requiredAmp, confirmedAt: t });
    if (amp >= o.firstAmp) { this.amps.push(amp); if (this.amps.length > o.ampMemory) this.amps.shift(); this.sScale = median(this.amps); }
    if (ok) {
      if (this.stall) this._resolveStall(t, false);   // a full stroke of the new movement went by without the pose leaving the stalled one's far end: that was a move into position
      this.strokes.push({ t: at, amp });
      if (this.strokes.length % 2 === 1) ev = this._rep(at, amp, t) || ev;
    }
    this.prevAnchor = Float32Array.from(this.anchor); this.anchor.set(this.far); this.parity = -this.parity;
    this._dilate(this.mask, this.strokeSet); this.n0 = Math.max(1, this.nFocus);
    this.farD = 0; this.farT = t; this.far.set(g); this.farRefD = 0; this.farRefT = t; this.strokeEmax = 0; this.farE = 0;
    return ev;
  }

  _rep(at, amp, now) {
    this.count = Math.max(0, this.base + this._reps() + this.manual); this.lastRepAt = now;
    const reps = this.events.filter(e => e.type === 'rep'), prev = reps[reps.length - 1], d = prev && prev.t > this.recalAt ? at - prev.t : null;
    const conf = this.periodValid ? this.pi * (d == null ? 0.5 : 1 - Math.min(1, Math.abs(d - this.period) / this.period)) : 0.25;
    const ev = { type: 'rep', n: this.count, t: at, at: now, amp, conf, interval: d, period: this.period };
    this.events.push(ev);
    return ev;
  }

  /** Period from the autocorrelation: the first local maximum whose prominence over the half-period band is within 80 % of the best. */
  _periodFrom(R, rho) {
    const o = this.opts, R0 = R[0], maxLag = Math.min(o.lagMax, this.n - 1);
    if (!(R0 > 1e-9) || maxLag < o.lagMin + 2) return null;
    for (let tau = 1; tau <= maxLag; tau++) rho[tau] = clamp(R[tau] / R0, -1, 1);
    const prom = lag => { let m = -1; for (let k = Math.max(1, Math.round(0.3 * lag)); k <= Math.round(0.7 * lag); k++) m = Math.max(m, rho[k]); return rho[lag] - m; };
    const cands = [];
    for (let tau = o.lagMin + 1; tau < maxLag; tau++) if (rho[tau] >= o.rhoMin && rho[tau] >= rho[tau - 1] && rho[tau] >= rho[tau + 1]) { const p = prom(tau); if (p >= o.promMin) cands.push({ tau, p }); }
    if (!cands.length) return null;
    let best = 0; for (const c of cands) if (c.p > best) best = c.p;
    const pick = cands.find(c => c.p >= 0.8 * best).tau;
    if (this.n - this.recalN < 2 * pick) return null;   // two cycles of data since the last recalibration
    const a = rho[pick - 1], b = rho[pick], c = rho[pick + 1], den = a - 2 * b + c;
    const lag = pick + (den < 0 ? clamp(0.5 * (a - c) / den, -1, 1) : 0);
    return { lag, period: lag / o.fs, pi: b };
  }

  _pickPeriod(t) {
    const p = this._periodFrom(this.R, this.rho);
    if (!p) { this.periodValid = false; return; }
    this.periodLag = p.lag; this.period = p.period; this.pi = p.pi; this.periodValid = true; this.everValid = true;
  }

  /** Bank the reps so far and learn the movement again from scratch (new exercise, new movement). */
  recalibrate(t, reason = 'manual') {
    this.base += this._reps(); this.strokes = []; this.amps = []; this.sScale = null;
    this.R.fill(0); this.periodValid = false; this.everValid = false; this.period = null; this.pi = 0;
    this.recalAt = t; this.recalN = this.n; this.resync();
    const ev = { type: 'recalibrate', at: t, reason, n: this.count };
    this.events.push(ev);
    return ev;
  }

  /** The learned movement, to be stored per exercise (grid and stroke size). */
  exportCalibration() { return { gx: this.gx, gy: this.gy, cellPx: this.opts.cellPx, sScale: this.sScale, period: this.period, pi: this.pi }; }
  /** Start from a remembered stroke size so the first stroke is judged against it. Returns false if the grid differs. */
  importCalibration(cal) {
    if (!cal || cal.gx !== this.gx || cal.gy !== this.gy) return false;
    this.sScale = cal.sScale || null; this.amps = this.sScale ? [this.sScale] : [];
    return true;
  }

  /** Self-similarity matrix of the last `size` samples over the focus cells (for a debug view). Float32Array size×size in [-1, 1]. */
  ssm(size = 120) {
    const o = this.opts, D = this.D, W = Math.min(this.n, o.window, size), out = new Float32Array(W * W), ring = this.ring, start = this.n - W, mask = this.mask;
    const norms = new Float32Array(W);
    for (let i = 0; i < W; i++) { const r = ((start + i) % o.window) * D; let s = 0; for (let c = 0; c < D; c++) if (mask[c]) s += ring[r + c] * ring[r + c]; norms[i] = Math.sqrt(s) || 1; }
    for (let i = 0; i < W; i++) { const ri = ((start + i) % o.window) * D; for (let j = i; j < W; j++) { const rj = ((start + j) % o.window) * D; let s = 0; for (let c = 0; c < D; c++) if (mask[c]) s += ring[ri + c] * ring[rj + c]; const v = s / (norms[i] * norms[j]); out[i * W + j] = v; out[j * W + i] = v; } }
    return { size: W, data: out };
  }
}
