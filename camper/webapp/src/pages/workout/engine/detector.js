// detector.js — pure signal-processing core for Rep Counter.
// No DOM access: the same code runs in the browser (index.html), in Node tests
// (test/detector.test.mjs) and in the offline replay tool (tools/replay.mjs).

export const clamp = (v, lo, hi) => (v < lo ? lo : v > hi ? hi : v);

export function median(values) {
  const a = Array.from(values).filter(Number.isFinite).sort((p, q) => p - q);
  if (!a.length) return NaN;
  const m = a.length >> 1;
  return a.length % 2 ? a[m] : (a[m - 1] + a[m]) / 2;
}

/**
 * RepDetector — turning-point rep counter with adaptive hysteresis.
 *
 * Feed it a 1-D "lift" signal (anything that rises when the weight goes up: plate
 * height in pixels, cable travel in mm, …) plus timestamps in seconds. It finds
 * direction reversals (peaks and valleys) with a hysteresis band and counts a rep
 * when a valley→peak excursion is big enough. Every rep is measured from its own
 * bottom (the valley the detector just found) to its own top, so there is no fixed
 * rest position: the bottom can creep along the cable during a set, you can pause
 * mid-rep, the eccentric can be slow, and the camera can drift a little.
 *
 * Options (signal units unless noted):
 *   minAmp         absolute floor: smallest valley→peak travel that can ever count
 *   minHyst        absolute floor for a reversal (must exceed tracker jitter)
 *   hystFrac       reversal = this fraction of the learned range of motion (ROM)
 *   romFrac        a rep must travel ≥ this fraction of ROM, else it is a "partial"
 *   minReturnFrac  since the last counted peak the weight must have come down by
 *                  this fraction of ROM before a new rep can count (kills double
 *                  counts from a hitch mid-lift or from pumping at the top)
 *   minRepTime / maxRepTime   valley→peak duration limits, seconds
 *   partials       false = no partial classification and no time/return rules
 *   romMemory      number of recent rep amplitudes whose median is the learned ROM
 *
 * Events returned by update():
 *   {type:'rep', n, amp, dur, v, t, at, valleyT}  counted rep; t = time of the peak,
 *                                                 at = time it was detected
 *   {type:'partial', amp, dur, v, t, at, reason}  a reversal too small/short/early to count
 *   {type:'valley', v, t, at}                     bottom reversal (for charts)
 *   {type:'peak', v, t, at, reason}               a top reversal not even worth a partial
 */
export class RepDetector {
  constructor(opts = {}) {
    this.opts = Object.assign({
      minAmp: 20, minHyst: 3, hystFrac: 0.2, romFrac: 0.5, minReturnFrac: 0.5,
      minRepTime: 0.12, maxRepTime: 20, romMemory: 5,
      partials: true,   // false = no partial classification and no time/return rules (used by PatternCounter)
    }, opts);
    this.reset();
  }

  reset() {
    this.count = 0; this.partials = 0; this.rom = null; this.amps = [];
    this.dir = 0; this.ext = null; this.extT = 0; this.lo = null; this.hi = null;
    this.valley = null; this.lastPeakV = null; this.lastRepAt = null;
    this.events = [];
  }

  /** Current reversal band: a move against the trend bigger than this is a turning point. */
  get hyst() {
    return Math.max(this.opts.minHyst, this.opts.hystFrac * (this.rom ?? this.opts.minAmp));
  }

  /**
   * The bottom of the current rep, measured, not fixed: the last confirmed valley while the
   * weight is rising, the running minimum while it is falling. Null before the first sample.
   */
  get bottom() {
    if (this.ext === null) return null;
    if (this.dir > 0) return this.valley ? this.valley.v : (this.lo ? this.lo.v : null);
    if (this.dir < 0) return this.ext;
    return this.lo ? this.lo.v : this.ext;
  }

  /** Smallest valley→peak travel that counts as a rep right now. */
  get requiredAmp() {
    return this.rom == null ? this.opts.minAmp : Math.max(this.opts.minAmp, this.opts.romFrac * this.rom);
  }

  update(x, t) {
    if (!Number.isFinite(x) || !Number.isFinite(t)) return null;
    if (this.ext === null) {
      this.ext = x; this.extT = t; this.lo = { v: x, t }; this.hi = { v: x, t };
      return null;
    }
    const h = this.hyst;
    let ev = null;
    if (this.dir === 0) {
      // Direction unknown yet: track both extremes until one side breaks out.
      if (x < this.lo.v) this.lo = { v: x, t };
      if (x > this.hi.v) this.hi = { v: x, t };
      if (x - this.lo.v > h) {
        this.valley = this.lo; this.dir = 1; this.ext = x; this.extT = t;
        ev = { type: 'valley', v: this.lo.v, t: this.lo.t, at: t };
      } else if (this.hi.v - x > h) {
        this.dir = -1; this.ext = x; this.extT = t;
        ev = { type: 'peak', v: this.hi.v, t: this.hi.t, at: t, counted: false, reason: 'start' };
      }
    } else if (this.dir > 0) {
      // Rising: track the running max; a drop of more than h confirms the peak.
      if (x > this.ext) { this.ext = x; this.extT = t; }
      else if (this.ext - x > h) {
        ev = this._peak(this.ext, this.extT, t);
        this.dir = -1; this.ext = x; this.extT = t;
      }
    } else {
      // Falling: track the running min; a rise of more than h confirms the valley.
      if (x < this.ext) { this.ext = x; this.extT = t; }
      else if (x - this.ext > h) {
        this.valley = { v: this.ext, t: this.extT };
        ev = { type: 'valley', v: this.ext, t: this.extT, at: t };
        this.dir = 1; this.ext = x; this.extT = t;
      }
    }
    if (ev) this.events.push(ev);
    return ev;
  }

  _peak(pv, pt, now) {
    if (!this.valley) return { type: 'peak', v: pv, t: pt, at: now, counted: false, reason: 'no-valley' };
    const amp = pv - this.valley.v;
    const dur = pt - this.valley.t;
    const req = this.requiredAmp;
    const every = !this.opts.partials;
    const returned = every || this.lastPeakV == null
      || (this.lastPeakV - this.valley.v) >= this.opts.minReturnFrac * (this.rom ?? amp);
    const timeOk = every || (dur >= this.opts.minRepTime && dur <= this.opts.maxRepTime);
    if (amp >= req && returned && timeOk) {
      this.count++;
      this.amps.push(amp);
      if (this.amps.length > this.opts.romMemory) this.amps.shift();
      this.rom = median(this.amps);
      this.lastPeakV = pv; this.lastRepAt = now;
      return { type: 'rep', n: this.count, amp, dur, v: pv, t: pt, at: now, valleyT: this.valley.t, counted: true };
    }
    if (!every && amp >= Math.max(2 * this.opts.minHyst, 0.25 * req)) {
      this.partials++;
      const reason = !returned ? 'no-return' : amp < req ? 'short' : 'time';
      return { type: 'partial', amp, dur, v: pv, t: pt, at: now, valleyT: this.valley.t, counted: false, reason };
    }
    return { type: 'peak', v: pv, t: pt, at: now, counted: false, reason: 'tiny' };
  }

  /** Forget the current extreme (after a pause or a signal jump) without touching the count or the learned ROM. */
  resync() { this.ext = null; this.dir = 0; this.lo = null; this.hi = null; this.valley = null; }

  /** Manual correction (+1 / −1). Recorded as an event so recordings show where the counter was wrong. */
  adjust(delta, at) {
    this.count = Math.max(0, this.count + delta);
    const ev = { type: 'correction', delta, n: this.count, at };
    this.events.push(ev);
    return ev;
  }
}

/**
 * PatternCounter — "count every rep" without a min-travel setting.
 *
 * 1. Fine turning points are found with only the noise floor as hysteresis, so nothing is missed.
 * 2. The sizes of all fine excursions in the last `window` seconds describe the motion; the
 *    macro pattern size M is the median of the three largest (held up by 60 % of the recent
 *    rep size so a rest does not shrink it to the noise).
 * 3. Reps are re-assembled from the stored turning points at that scale: a reversal must be
 *    hystFrac·M to end a rep and a rep must travel romFrac·M to count. Wiggles inside a rep,
 *    shaking at the top and swinging at rest all fall under that and disappear.
 * 4. Because the first movement of a set cannot be judged until a pattern exists, the count is
 *    recomputed from the stored turning points on every change: a wobble counted early is taken
 *    back (an 'uncount' event) once the real pattern shows what a rep looks like.
 *
 * Same interface as RepDetector (update, count, partials, rom, requiredAmp, hyst, bottom,
 * lastRepAt, adjust, resync, events) so the app can swap between them.
 */
export class PatternCounter {
  constructor(opts = {}) {
    this.opts = Object.assign({ minHyst: 3, hystFrac: 0.25, romFrac: 0.35, window: 10, romMemory: 5, maxExtrema: 300 }, opts);
    this.reset();
  }
  reset() {
    this.count = 0; this.base = 0; this.manual = 0; this.partials = 0; this.rom = null; this.M = null; this.amps = [];
    this.events = []; this.lastRepAt = null;
    this.dir = 0; this.ext = null; this.extT = 0; this.lo = null; this.hi = null;
    this.extrema = []; this._asm = null; this._lastX = null;
  }
  get hyst() { return Math.max(this.opts.minHyst, this.M ? this.opts.hystFrac * this.M : 0); }
  get requiredAmp() { return this.M ? Math.max(2 * this.opts.minHyst, this.opts.romFrac * this.M) : 3 * this.opts.minHyst; }
  get bottom() { return this._asm ? this._asm.bottom : (this.lo ? this.lo.v : null); }
  resync() { this.ext = null; this.dir = 0; this.lo = null; this.hi = null; }
  adjust(delta, at) { this.manual += delta; this.count = Math.max(0, this.base + this._macroCount + this.manual); const ev = { type: 'correction', delta, n: this.count, at }; this.events.push(ev); return ev; }

  update(x, t) {
    if (!Number.isFinite(x) || !Number.isFinite(t)) return null;
    this._lastX = x;
    const h = this.opts.minHyst;
    let ev = null;
    if (this.ext === null) { this.ext = x; this.extT = t; this.lo = { v: x, t }; this.hi = { v: x, t }; return null; }
    if (this.dir === 0) {
      if (x < this.lo.v) this.lo = { v: x, t };
      if (x > this.hi.v) this.hi = { v: x, t };
      if (x - this.lo.v > h) { ev = this._extremum('min', this.lo.v, this.lo.t, x, t); this.dir = 1; this.ext = x; this.extT = t; }
      else if (this.hi.v - x > h) { ev = this._extremum('max', this.hi.v, this.hi.t, x, t); this.dir = -1; this.ext = x; this.extT = t; }
    } else if (this.dir > 0) {
      if (x > this.ext) { this.ext = x; this.extT = t; }
      else if (this.ext - x > h) { ev = this._extremum('max', this.ext, this.extT, x, t); this.dir = -1; this.ext = x; this.extT = t; }
    } else {
      if (x < this.ext) { this.ext = x; this.extT = t; }
      else if (x - this.ext > h) { ev = this._extremum('min', this.ext, this.extT, x, t); this.dir = 1; this.ext = x; this.extT = t; }
    }
    // the current position may complete a macro reversal before the next fine extremum exists
    if (!ev && this.extrema.length >= 2) ev = this._recount(x, t);
    return ev;
  }

  _extremum(kind, v, te, x, now) {
    const last = this.extrema[this.extrema.length - 1];
    if (last && last.kind === kind) { if ((kind === 'max' && v > last.v) || (kind === 'min' && v < last.v)) { last.v = v; last.t = te; } }
    else this.extrema.push({ kind, v, t: te });
    if (this.extrema.length > this.opts.maxExtrema) this._commitOldest();
    this._updateScale(now);
    return this._recount(x, now);
  }

  /** Macro pattern size from the fine excursions of the last `window` seconds. */
  _updateScale(now) {
    const amps = [];
    for (let i = 1; i < this.extrema.length; i++) { const a = this.extrema[i], b = this.extrema[i - 1]; if (a.t >= now - this.opts.window) amps.push(Math.abs(a.v - b.v)); }
    amps.sort((p, q) => q - p);
    const top = amps.slice(0, 3);
    const fromSignal = top.length ? median(top) : 0;
    const fromReps = this.rom != null ? 0.6 * this.rom : 0;
    const M = Math.max(fromSignal, fromReps);
    this.M = M >= 3 * this.opts.minHyst ? M : null;
  }

  get _macroCount() { return this._asm ? this._asm.count : 0; }

  /** Re-assemble reps from the stored turning points (plus the current position) at the macro scale. */
  _recount(x, now) {
    if (!this.M) return null;
    const asm = new RepDetector({ minAmp: this.requiredAmp, minHyst: this.hyst, hystFrac: 0, romFrac: 0, partials: false, minReturnFrac: 0, minRepTime: 0, maxRepTime: Infinity, romMemory: this.opts.romMemory });
    for (const e of this.extrema) asm.update(e.v, e.t);
    asm.update(x, now);
    this._asm = asm;
    const reps = asm.events.filter(e => e.type === 'rep');
    if (reps.length) { this.amps = reps.slice(-this.opts.romMemory).map(e => e.amp); this.rom = median(this.amps); }
    const newCount = Math.max(0, this.base + reps.length + this.manual);
    let ev = null;
    if (newCount > this.count) {
      const r = reps[reps.length - 1];
      ev = { type: 'rep', n: newCount, amp: r.amp, dur: r.dur, v: r.v, t: r.t, at: now, valleyT: r.valleyT, counted: true };
      this.lastRepAt = now;
      for (let n = this.count + 1; n < newCount; n++) this.events.push({ type: 'rep', n, amp: r.amp, dur: r.dur, v: r.v, t: r.t, at: now, counted: true });
    } else if (newCount < this.count) {
      ev = { type: 'uncount', n: newCount, at: now, from: this.count };
    }
    this.count = newCount;
    if (ev) this.events.push(ev);
    return ev;
  }

  /** The buffer is full: bank the oldest finished rep and drop the turning points that formed it. */
  _commitOldest() {
    const asm = this._asm;
    const reps = asm ? asm.events.filter(e => e.type === 'rep') : [];
    if (reps.length >= 2) {
      const second = reps[1];
      const idx = this.extrema.findIndex(e => e.kind === 'min' && Math.abs(e.t - second.valleyT) < 1e-6);
      if (idx > 0) { this.extrema.splice(0, idx); this.base += 1; return; }
    }
    this.extrema.splice(0, Math.floor(this.extrema.length / 2));
  }
}

/**
 * TemplateTracker — normalized cross-correlation (NCC) template tracker.
 *
 * Lock a small grayscale patch (the top edge of the top plate against the wall is
 * ideal: unique, high contrast, rotation-invariant) and each frame find where it
 * moved. Search is a window around the last position; if the match score falls
 * below `lostScore` for `reacquireFrames` frames the search widens to the full
 * column (or the ROI) and only a strong match (`reacquireScore`) is accepted, so a
 * shelf edge or another plate cannot hijack the lock. A weak lock is re-verified
 * against the whole column every `verifyEvery` frames for the same reason.
 * Returns the patch's top-left in frame pixels with sub-pixel y.
 *
 * Frames are Float32Array grayscale, row-major, width W, height H.
 */
export class TemplateTracker {
  constructor(opts = {}) {
    this.opts = Object.assign({
      maxTaps: 1600,        // cap on template samples (speed)
      searchX: 16,          // ± px sideways search per frame (swing, or sideways reps)
      searchY: 40,          // ± px vertical search per frame
      lostScore: 0.5,       // below this the match is "lost" and the last position is held
      reacquireFrames: 2,   // lost frames before the search widens to the whole column / ROI
      reacquireScore: 0.75, // a wide search only adopts a match this strong (rejects lookalikes)
      verifyEvery: 15,      // while the lock is weaker than reacquireScore, re-check the whole column every N frames
    }, opts);
    this.template = null; this.pos = null; this.roi = null; this.lostFrames = 0; this._frame = 0;
    this._S = null; this._S2 = null; this._W = 0; this._H = 0;
  }

  setROI(roi) {
    this.roi = roi ? { x: Math.round(roi.x), y: Math.round(roi.y), w: Math.round(roi.w), h: Math.round(roi.h) } : null;
  }

  setTemplate(gray, W, H, box) {
    const x0 = clamp(Math.round(box.x), 0, W - 2), y0 = clamp(Math.round(box.y), 0, H - 2);
    const w = clamp(Math.round(box.w), 2, W - x0), h = clamp(Math.round(box.h), 2, H - y0);
    const stride = Math.max(1, Math.ceil(Math.sqrt((w * h) / this.opts.maxTaps)));
    const dx = [], dy = [], v = [];
    for (let j = 0; j < h; j += stride) for (let i = 0; i < w; i += stride) {
      dx.push(i); dy.push(j); v.push(gray[(y0 + j) * W + x0 + i]);
    }
    const n = v.length;
    let mean = 0; for (let k = 0; k < n; k++) mean += v[k]; mean /= n;
    let ss = 0; for (let k = 0; k < n; k++) { v[k] -= mean; ss += v[k] * v[k]; }
    const norm = Math.sqrt(ss) || 1;
    for (let k = 0; k < n; k++) v[k] /= norm;
    this.template = {
      x: x0, y: y0, w, h, stride, n,
      dx: Int16Array.from(dx), dy: Int16Array.from(dy), v: Float32Array.from(v),
      contrast: Math.sqrt(ss / n), // std-dev of the patch in gray levels; < ~6 means "flat wall"
    };
    this.pos = { x: x0, y: y0, score: 1, lost: false };
    this.lostFrames = 0;
    return this.template;
  }

  track(gray, W, H) {
    const T = this.template;
    if (!T) return null;
    const o = this.opts;
    this._integrate(gray, W, H);
    const maxX = W - T.w, maxY = H - T.h;
    const wide = this.lostFrames >= o.reacquireFrames;
    const best = this._search(gray, W, this._region(maxX, maxY, wide), maxX, maxY);
    this._frame++;
    if (wide) {
      // Re-acquiring: demand a strong match so a lookalike (a shelf edge, another plate) is not adopted.
      if (best.s >= o.reacquireScore) { this.lostFrames = 0; this.pos = { x: best.xSub, y: best.ySub, score: best.s, lost: false }; }
      else { this.lostFrames++; this.pos = { x: this.pos.x, y: this.pos.y, score: best.s, lost: true }; }
      return this.pos;
    }
    if (best.s >= o.lostScore) {
      this.lostFrames = 0;
      let pick = best;
      // A weak lock gets re-verified now and then: if something elsewhere matches far better, jump to it.
      if (best.s < o.reacquireScore && this._frame % o.verifyEvery === 0) {
        const alt = this._search(gray, W, this._region(maxX, maxY, true), maxX, maxY);
        if (alt.s >= o.reacquireScore && alt.s > best.s + 0.1) pick = alt;
      }
      this.pos = { x: pick.xSub, y: pick.ySub, score: pick.s, lost: false };
    } else {
      this.lostFrames++;
      this.pos = { x: this.pos.x, y: this.pos.y, score: best.s, lost: true };   // hold the last good position
    }
    return this.pos;
  }

  // Coarse pass on a 2-px grid, refine ±2 px, then parabolic sub-pixel fits in x and y.
  _search(gray, W, r, maxX, maxY) {
    let bx = r.x0, by = r.y0, bs = -Infinity;
    for (let y = r.y0; y <= r.y1; y += 2) for (let x = r.x0; x <= r.x1; x += 2) {
      const s = this._score(gray, W, x, y);
      if (s > bs) { bs = s; bx = x; by = y; }
    }
    let fx = bx, fy = by, fs = bs;
    for (let y = Math.max(r.y0, by - 2); y <= Math.min(r.y1, by + 2); y++)
      for (let x = Math.max(r.x0, bx - 2); x <= Math.min(r.x1, bx + 2); x++) {
        if (x === bx && y === by) continue;
        const s = this._score(gray, W, x, y);
        if (s > fs) { fs = s; fx = x; fy = y; }
      }
    let ySub = fy, xSub = fx;
    if (fy > 0 && fy < maxY) {
      const sm = this._score(gray, W, fx, fy - 1), sp = this._score(gray, W, fx, fy + 1);
      const den = sm - 2 * fs + sp;
      if (den < -1e-9) ySub = fy + clamp(0.5 * (sm - sp) / den, -1, 1);
    }
    if (fx > 0 && fx < maxX) {
      const sl = this._score(gray, W, fx - 1, fy), sr = this._score(gray, W, fx + 1, fy);
      const den = sl - 2 * fs + sr;
      if (den < -1e-9) xSub = fx + clamp(0.5 * (sl - sr) / den, -1, 1);
    }
    return { x: fx, y: fy, s: fs, ySub, xSub };
  }

  scoreAt(gray, W, H, x, y) { this._integrate(gray, W, H); return this._score(gray, W, x, y); }

  _region(maxX, maxY, wide) {
    const o = this.opts, T = this.template, p = this.pos;
    const px = Math.round(p.x), py = Math.round(p.y), rx = wide ? o.searchX * 3 : o.searchX;
    let x0 = px - rx, x1 = px + rx;
    let y0 = wide ? 0 : py - o.searchY, y1 = wide ? maxY : py + o.searchY;
    if (this.roi) {
      x0 = Math.max(x0, this.roi.x); x1 = Math.min(x1, this.roi.x + this.roi.w - T.w);
      y0 = Math.max(y0, this.roi.y); y1 = Math.min(y1, this.roi.y + this.roi.h - T.h);
    }
    x0 = clamp(x0, 0, maxX); x1 = clamp(x1, 0, maxX); y0 = clamp(y0, 0, maxY); y1 = clamp(y1, 0, maxY);
    if (x1 < x0) x1 = x0;
    if (y1 < y0) y1 = y0;
    return { x0, x1, y0, y1 };
  }

  // Summed-area tables of gray and gray² so any window's mean/variance is O(1).
  _integrate(gray, W, H) {
    const W1 = W + 1, size = (W + 1) * (H + 1);
    if (!this._S || this._W !== W || this._H !== H) {
      this._S = new Float64Array(size); this._S2 = new Float64Array(size); this._W = W; this._H = H;
    }
    const S = this._S, S2 = this._S2;
    for (let y = 1; y <= H; y++) {
      let row = 0, row2 = 0;
      const g0 = (y - 1) * W, s0 = y * W1, p0 = (y - 1) * W1;
      for (let x = 1; x <= W; x++) {
        const g = gray[g0 + x - 1];
        row += g; row2 += g * g;
        S[s0 + x] = S[p0 + x] + row;
        S2[s0 + x] = S2[p0 + x] + row2;
      }
    }
  }

  _score(gray, W, x, y) {
    const T = this.template, base = y * W + x;
    let dot = 0;
    for (let k = 0; k < T.n; k++) dot += gray[base + T.dy[k] * W + T.dx[k]] * T.v[k];
    const W1 = this._W + 1, S = this._S, S2 = this._S2;
    const a = y * W1 + x, b = a + T.w, c = (y + T.h) * W1 + x, d = c + T.w, n = T.w * T.h;
    const sum = S[d] - S[b] - S[c] + S[a], sum2 = S2[d] - S2[b] - S2[c] + S2[a];
    const mean = sum / n, vari = sum2 / n - mean * mean;
    if (!(vari > 1e-6)) return 0;
    return dot / Math.sqrt(T.n * vari);
  }
}

/**
 * MotionAxis — learns which way the target actually moves and turns 2-D positions into
 * the 1-D travel signal the RepDetector counts.
 *
 * In 'auto' mode it fits the principal axis (2-D PCA) of the positions seen in the last
 * `window` seconds, once they spread more than `minSpread` pixels, refitting every `every`
 * seconds and blending with the previous axis. The positive direction is "towards the top
 * of the rep": the weight rests at the bottom most of the time, so the projected values are
 * skewed positive in that direction. freeze() holds the axis (the app freezes it after the
 * second counted rep of a set). 'vertical' and 'horizontal' are fixed overrides.
 * Until calibrated the axis is straight up, which is right for a plate stack.
 */
export class MotionAxis {
  constructor(opts = {}) {
    this.opts = Object.assign({ mode: 'auto', window: 15, minSpread: 6, every: 1.0, smooth: 0.5 }, opts);
    this.reset(0, 0);
  }
  reset(x0, y0) {
    this.x0 = x0; this.y0 = y0; this.buf = []; this.frozen = false; this.lastFit = -Infinity; this.spread = 0;
    this.ux = 0; this.uy = -1; this.calibrated = false;
    this.setMode(this.opts.mode);
  }
  setMode(mode) {
    this.opts.mode = mode;
    if (mode === 'vertical') { this.ux = 0; this.uy = -1; this.calibrated = true; }
    else if (mode === 'horizontal') { this.ux = 1; this.uy = 0; this.calibrated = true; }
    else { this.ux = 0; this.uy = -1; this.calibrated = false; this.lastFit = -Infinity; }
  }
  /** Direction in degrees with image y flipped: 90 = straight up, 0 = right, 180 = left. */
  get angle() { return (Math.atan2(-this.uy, this.ux) * 180 / Math.PI) || 0; }
  freeze() { this.frozen = true; }
  unfreeze() { this.frozen = false; }
  add(x, y, t) {
    this.buf.push([x, y, t]);
    const cut = t - this.opts.window;
    while (this.buf.length && this.buf[0][2] < cut) this.buf.shift();
    if (this.opts.mode !== 'auto' || this.frozen) return;
    if (t - this.lastFit >= this.opts.every && this.buf.length >= 20) { this.lastFit = t; this._fit(); }
  }
  /** Travel along the axis, relative to where the target was locked. */
  project(x, y) { return (x - this.x0) * this.ux + (y - this.y0) * this.uy; }
  _fit() {
    const n = this.buf.length;
    let mx = 0, my = 0;
    for (const [x, y] of this.buf) { mx += x; my += y; }
    mx /= n; my /= n;
    let sxx = 0, syy = 0, sxy = 0;
    for (const [x, y] of this.buf) { const dx = x - mx, dy = y - my; sxx += dx * dx; syy += dy * dy; sxy += dx * dy; }
    sxx /= n; syy /= n; sxy /= n;
    const l1 = (sxx + syy) / 2 + Math.sqrt(((sxx - syy) / 2) ** 2 + sxy * sxy);
    const spread = Math.sqrt(Math.max(0, l1));
    if (spread < this.opts.minSpread) return;                 // not enough movement to tell yet
    const th = 0.5 * Math.atan2(2 * sxy, sxx - syy);
    let ux = Math.cos(th), uy = Math.sin(th);
    const p = this.buf.map(([x, y]) => (x - mx) * ux + (y - my) * uy);
    const skew = p.reduce((a, b) => a + b, 0) / n - median(p);
    const ref = this.calibrated ? [this.ux, this.uy] : [0, -1];
    if (Math.abs(skew) > 0.15 * spread) { if (skew < 0) { ux = -ux; uy = -uy; } }   // tail = top of the rep
    else if (ux * ref[0] + uy * ref[1] < 0) { ux = -ux; uy = -uy; }                  // symmetric: keep continuity
    const a = (!this.calibrated || ux * this.ux + uy * this.uy < 0) ? 1 : this.opts.smooth;
    const nx = (1 - a) * this.ux + a * ux, ny = (1 - a) * this.uy + a * uy, len = Math.hypot(nx, ny) || 1;
    this.ux = nx / len; this.uy = ny / len; this.calibrated = true; this.spread = spread;
  }
}

/**
 * updateMotionMap — exponentially decaying map of where the picture has been changing
 * (1 = changed in this frame, fading over a few seconds). Returns the fraction of pixels
 * that changed this frame. Used to find the moving part of the picture automatically.
 */
export function updateMotionMap(gray, prev, map, { decay = 0.985, thr = 30 } = {}) {
  let moving = 0;
  for (let i = 0; i < gray.length; i++) {
    const d = gray[i] - prev[i], m = (d > thr || d < -thr) ? 1 : 0;
    moving += m;
    const v = map[i] * decay;
    map[i] = m > v ? m : v;
  }
  return moving / gray.length;
}

/**
 * findMovingTarget — from a motion map built over a couple of reps and the current (still)
 * frame, work out where the moving object is and propose a template box on its leading edge:
 *   1. the biggest connected blob of cells that kept changing is the streak the object sweeps;
 *   2. its principal axis is the direction of movement;
 *   3. in the current frame the end of the streak with less texture is empty background, so
 *      scanning from there the first strong edge is the object's edge at rest;
 *   4. the box straddles that edge and spans the streak's width — the same kind of target a
 *      person would draw by hand. The axis points from the object into the empty part (the
 *      top of the rep).
 * Returns null when there is no clear moving blob yet.
 */
export function findMovingTarget(gray, map, W, H, opts = {}) {
  const o = Object.assign({ cell: 8, frac: 0.4, minLevel: 0.25, minCells: 6 }, opts);
  const cw = Math.ceil(W / o.cell), ch = Math.ceil(H / o.cell), score = new Float32Array(cw * ch);
  let max = 0;
  for (let cy = 0; cy < ch; cy++) for (let cx = 0; cx < cw; cx++) {
    let sum = 0, n = 0;
    for (let y = cy * o.cell; y < Math.min(H, (cy + 1) * o.cell); y++) for (let x = cx * o.cell; x < Math.min(W, (cx + 1) * o.cell); x++) { sum += map[y * W + x]; n++; }
    const v = sum / n; score[cy * cw + cx] = v; if (v > max) max = v;
  }
  if (max < o.minLevel) return null;
  const thr = Math.max(o.minLevel, o.frac * max), active = new Uint8Array(cw * ch);
  for (let i = 0; i < score.length; i++) active[i] = score[i] >= thr ? 1 : 0;
  // largest 4-connected component of active cells
  const label = new Int32Array(cw * ch).fill(-1); let best = null, nlab = 0;
  for (let i = 0; i < active.length; i++) {
    if (!active[i] || label[i] >= 0) continue;
    const stack = [i], cells = []; label[i] = nlab;
    while (stack.length) {
      const k = stack.pop(); cells.push(k);
      const x = k % cw, y = (k / cw) | 0;
      for (const [nx, ny] of [[x - 1, y], [x + 1, y], [x, y - 1], [x, y + 1]]) {
        if (nx < 0 || ny < 0 || nx >= cw || ny >= ch) continue;
        const j = ny * cw + nx; if (active[j] && label[j] < 0) { label[j] = nlab; stack.push(j); }
      }
    }
    if (!best || cells.length > best.length) best = cells; nlab++;
  }
  if (!best || best.length < o.minCells) return null;
  // direction of the streak (PCA of its cell centres) and its pixel bounding box (+1 cell)
  const pts = best.map(k => [((k % cw) + 0.5) * o.cell, (((k / cw) | 0) + 0.5) * o.cell]);
  let mx = 0, my = 0; for (const [x, y] of pts) { mx += x; my += y; } mx /= pts.length; my /= pts.length;
  let sxx = 0, syy = 0, sxy = 0; for (const [x, y] of pts) { const dx = x - mx, dy = y - my; sxx += dx * dx; syy += dy * dy; sxy += dx * dy; }
  const th = 0.5 * Math.atan2(2 * sxy, sxx - syy); let ux = Math.cos(th), uy = Math.sin(th);
  const vertical = Math.abs(uy) >= Math.abs(ux);
  let x0 = W, y0 = H, x1 = 0, y1 = 0;
  for (const k of best) { const cx = k % cw, cy = (k / cw) | 0; x0 = Math.min(x0, cx * o.cell); y0 = Math.min(y0, cy * o.cell); x1 = Math.max(x1, Math.min(W, (cx + 1) * o.cell)); y1 = Math.max(y1, Math.min(H, (cy + 1) * o.cell)); }
  x0 = Math.max(0, x0 - o.cell); y0 = Math.max(0, y0 - o.cell); x1 = Math.min(W, x1 + o.cell); y1 = Math.min(H, y1 + o.cell);
  // edge-strength profile along the streak in the current frame
  const len = vertical ? y1 - y0 : x1 - x0; if (len < 8) return null;
  const prof = new Float32Array(len);
  for (let i = 1; i < len - 1; i++) {
    let sum = 0, n = 0;
    if (vertical) { const y = y0 + i; for (let x = x0; x < x1; x++) { sum += Math.abs(gray[(y + 1) * W + x] - gray[(y - 1) * W + x]); n++; } }
    else { const x = x0 + i; for (let y = y0; y < y1; y++) { sum += Math.abs(gray[y * W + x + 1] - gray[y * W + x - 1]); n++; } }
    prof[i] = sum / n;
  }
  const third = Math.max(1, Math.floor(len / 3)); let tA = 0, tB = 0;
  for (let i = 0; i < third; i++) { tA += prof[i]; tB += prof[len - 1 - i]; }
  const emptyAtStart = tA <= tB;           // start = top (vertical streak) or left (horizontal streak)
  let pmax = 0; for (let i = 0; i < len; i++) if (prof[i] > pmax) pmax = prof[i];
  const ethr = Math.max(8, 0.4 * pmax); let edge = -1;
  if (emptyAtStart) { for (let i = 2; i < len - 2; i++) if (prof[i] >= ethr) { edge = i; break; } }
  else { for (let i = len - 3; i >= 2; i--) if (prof[i] >= ethr) { edge = i; break; } }
  if (edge < 0) return null;
  const thick = Math.round(Math.min(40, Math.max(10, 0.15 * len)));
  let box;
  if (vertical) { const w = Math.max(8, Math.round((x1 - x0) * 0.8)); box = { x: Math.round(x0 + (x1 - x0 - w) / 2), y: Math.round(y0 + edge - thick / 2), w, h: thick }; }
  else { const h = Math.max(8, Math.round((y1 - y0) * 0.8)); box = { x: Math.round(x0 + edge - thick / 2), y: Math.round(y0 + (y1 - y0 - h) / 2), w: thick, h }; }
  box.x = clamp(box.x, 0, Math.max(0, W - box.w)); box.y = clamp(box.y, 0, Math.max(0, H - box.h));
  const dir = vertical ? (emptyAtStart ? [0, -1] : [0, 1]) : (emptyAtStart ? [-1, 0] : [1, 0]);
  if (ux * dir[0] + uy * dir[1] < 0) { ux = -ux; uy = -uy; }
  return { box, axis: { ux, uy }, bbox: { x: x0, y: y0, w: x1 - x0, h: y1 - y0 }, edge: vertical ? y0 + edge : x0 + edge, level: max, cells: best.length };
}

/** RGBA ImageData bytes → Float32 grayscale (BT.601 weights). */
export function toGray(rgba, out) {
  for (let i = 0, j = 0; j < out.length; i += 4, j++) out[j] = 0.299 * rgba[i] + 0.587 * rgba[i + 1] + 0.114 * rgba[i + 2];
  return out;
}

/** Mean absolute difference between two equally sized frames (cheap motion-energy channel). */
export function meanAbsDiff(a, b) {
  let s = 0;
  for (let i = 0; i < a.length; i++) { const d = a[i] - b[i]; s += d < 0 ? -d : d; }
  return s / a.length;
}

/**
 * rotateGray — rotate a grayscale frame by 90°: dir = +1 clockwise, −1 counter-clockwise.
 * The result is H wide and W tall. Used when a phone is turned mid-set and the camera starts
 * delivering frames the other way up: the locked template is re-extracted from the rotated
 * last frame so tracking can carry on instead of starting over.
 */
export function rotateGray(gray, W, H, dir, out) {
  out = out || new Float32Array(W * H);
  for (let y = 0; y < H; y++) {
    const row = y * W;
    for (let x = 0; x < W; x++) {
      const v = gray[row + x];
      if (dir > 0) out[x * H + (H - 1 - y)] = v;      // (x, y) → (H-1-y, x)
      else out[(W - 1 - x) * H + y] = v;              // (x, y) → (y, W-1-x)
    }
  }
  return out;
}

/** The box {x, y, w, h} in a W×H frame after rotateGray with the same `dir`. */
export function rotateBox(b, W, H, dir) {
  return dir > 0 ? { x: H - (b.y + b.h), y: b.x, w: b.h, h: b.w } : { x: b.y, y: W - (b.x + b.w), w: b.h, h: b.w };
}

/** Bilinear resample of a grayscale frame from sw×sh to dw×dh (used with rotateGray: a 90° turn of a
 *  frame processed at a fixed width changes its processed height, so the rotated frame is rescaled). */
export function resampleGray(src, sw, sh, dw, dh, out) {
  out = out || new Float32Array(dw * dh);
  const fx = sw / dw, fy = sh / dh;
  for (let y = 0; y < dh; y++) {
    const sy = clamp((y + 0.5) * fy - 0.5, 0, sh - 1), y0 = Math.floor(sy), y1 = Math.min(sh - 1, y0 + 1), ty = sy - y0;
    for (let x = 0; x < dw; x++) {
      const sx = clamp((x + 0.5) * fx - 0.5, 0, sw - 1), x0 = Math.floor(sx), x1 = Math.min(sw - 1, x0 + 1), tx = sx - x0;
      const a = src[y0 * sw + x0], b = src[y0 * sw + x1], c = src[y1 * sw + x0], d = src[y1 * sw + x1];
      out[y * dw + x] = (a * (1 - tx) + b * tx) * (1 - ty) + (c * (1 - tx) + d * tx) * ty;
    }
  }
  return out;
}
