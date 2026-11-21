// The workout page's engine: camera, frame loop, the appearance counter, audio cues and the
// workout state machine — everything that is not React. A plain module singleton with a
// subscribe/getSnapshot pair so the screens read it through useSyncExternalStore; the screens
// only call its methods and render its snapshot.
//
// Ported from rep-counter/workout.js on 2026-11-21, grid mode only: the whole frame is watched
// with 16 px cells (tuned on real recordings, see appearance.js), nothing is drawn or tapped,
// and every rep counts from the first one.
import { toGray } from './detector.js';
import { AppearanceCounter } from './appearance.js';

export type VoiceMode = 'checkpoints' | 'announce' | 'off';
export interface Exercise {
  name: string;
  reps: number;
}
export interface WorkoutPlan {
  exercises: Exercise[];
  rest: number;
  rounds: number;
}
export interface WorkoutSettings {
  voiceMode: VoiceMode;
  beep: boolean;
  facing: 'user' | 'environment';
  camId: string;
  current: WorkoutPlan;
}
export type EndReason = 'target' | 'skip' | 'end';
export interface RunExercise {
  name: string;
  round: number;
  target: number;
  counted: number;
  duration: number;
  t0: number | null;
  reason: EndReason | null;
}
export type Phase = 'exercise' | 'rest' | 'done';
export interface Run {
  startedAt: string;
  t0: number;
  rest: number;
  current: number;
  phase: Phase;
  restEnd: number;
  lastTick: number | null;
  pausedTotal: number;
  rounds: number;
  exercises: RunExercise[];
  total: number;
  duration: number;
}
export interface SummaryExercise {
  name: string;
  target: number;
  counted: number;
  duration: number;
  reason: EndReason | null;
}
export interface Summary {
  when: string;
  duration: number;
  total: number;
  exercises: SummaryExercise[];
}
export interface CameraOption {
  deviceId: string;
  label: string;
}
export type Tone = 'ok' | 'warn' | 'bad' | '';
export interface Snapshot {
  settings: WorkoutSettings;
  run: Run | null;
  paused: boolean;
  /** The camera is delivering frames. */
  live: boolean;
  cameras: CameraOption[];
  facingKnown: boolean;
  status: { text: string; tone: Tone };
  /** Border colour of the preview thumbnail during a workout. */
  thumb: 'ok' | 'hunt' | '';
  /** Reps of the exercise in progress; frozen while paused. */
  count: number;
  /** Seconds left before the workout starts by itself (camera screen), else null. */
  countdown: number | null;
  /** Seconds into the workout, paused time excluded. */
  elapsed: number;
  /** Whole seconds left in the rest, during a rest. */
  restLeft: number;
  grid: { cells: string; period: number | null; pi: number } | null;
  summary: Summary | null;
  summaryText: string;
  wakeLockSupported: boolean;
}

const KEY = 'camper.workout.settings';
const PROC_W = 320; // frames are analysed at this width; height follows the camera's aspect
const GRID_CELL_PX = 16; // appearance-grid cell size at that width
const COUNTDOWN_S = 5; // get-in-position time once the camera is live
const DEFAULT_NAMES = ['Rise', 'Ulnar deviation', 'Pronation', 'Supination', 'Wrist kickbacks', 'Cup'];
const OLD_DEFAULT_NAMES = ['Cupping', 'Pronation', 'Rising', 'Back pressure', 'Hook', 'Top roll']; // the port's first defaults, replaced once in saved settings

const now = () => performance.now() / 1000;
const clampInt = (v: number, lo: number, hi: number) => Math.min(hi, Math.max(lo, Math.round(v) || lo));
export const fmtDur = (s: number): string => {
  const t = Math.max(0, Math.round(s || 0));
  return `${Math.floor(t / 60)}:${String(t % 60).padStart(2, '0')}`;
};
const fmtWhen = (iso: string): string => {
  const d = new Date(iso);
  return `${d.toLocaleDateString(undefined, { month: 'short', day: 'numeric' })} ${d.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' })}`;
};

export const defaultPlan = (): WorkoutPlan => ({ exercises: DEFAULT_NAMES.map((name) => ({ name, reps: 100 })), rest: 10, rounds: 2 });

function loadSettings(): WorkoutSettings {
  const base: WorkoutSettings = { voiceMode: 'checkpoints', beep: true, facing: 'environment', camId: '', current: defaultPlan() };
  try {
    const raw = localStorage.getItem(KEY);
    if (!raw) return base;
    const s = { ...base, ...(JSON.parse(raw) as Partial<WorkoutSettings>) };
    if (!s.current || !Array.isArray(s.current.exercises) || !s.current.exercises.length) s.current = defaultPlan();
    else if (s.current.exercises.map((e) => e.name).join('|') === OLD_DEFAULT_NAMES.join('|')) s.current = defaultPlan(); // never edited: take the new defaults
    if (!['checkpoints', 'announce', 'off'].includes(s.voiceMode)) s.voiceMode = 'checkpoints';
    return s;
  } catch {
    return base;
  }
}

/**
 * Reps that get called out on the way to `target`: none up to 5; the last 3 up to 19; every 10
 * plus the last 3 up to 50; every 20 plus the last 3 above that. 34 reps → 10, 20, 30, 32, 33, 34.
 * The last one is said together with "done".
 */
export function checkpoints(target: number): Set<number> {
  const s = new Set<number>();
  if (target <= 5) return s;
  const step = target > 50 ? 20 : target >= 20 ? 10 : 0;
  if (step) for (let n = step; n < target; n += step) s.add(n);
  for (let n = Math.max(1, target - 2); n <= target; n++) s.add(n);
  return s;
}

class WorkoutEngine {
  settings: WorkoutSettings = loadSettings();
  run: Run | null = null;
  paused = false;
  summary: Summary | null = null;
  summaryText = '';

  private listeners = new Set<() => void>();
  private snapshot: Snapshot | null = null;

  // camera + processing
  private video: HTMLVideoElement | null = null;
  private overlay: HTMLCanvasElement | null = null;
  private stage: HTMLDivElement | null = null;
  private slot: HTMLElement | null = null;
  private thumbAnchor: HTMLElement | null = null;
  private stream: MediaStream | null = null;
  private cameraWanted = false;
  private loopId = 0;
  private cameras: CameraOption[] = [];
  private facingKnown = false;
  private srcW = 640;
  private srcH = 480;
  private proc = document.createElement('canvas');
  private procCtx = this.proc.getContext('2d', { willReadFrequently: true })!;
  private procW = PROC_W;
  private procH = 0;
  private gray: Float32Array | null = null;
  private frames = 0;
  private app: AppearanceCounter | null = null;
  private detector: AppearanceCounter | null = null;
  private gridLocked = false;
  private lastGrid: { period: number | null; periodValid: boolean; pi: number } | null = null;
  private carryCount: number | null = null; // a count to carry into a grid rebuilt mid-exercise (the picture changed shape)
  private status: { text: string; tone: Tone } = { text: 'Starting camera…', tone: 'warn' };
  private thumb: 'ok' | 'hunt' | '' = '';
  private uiTimer = 0;
  private pausedAt = 0;
  private pauseCount: number | null = null;
  private countdownEnd: number | null = null;
  private countdownLast = -1;

  // audio
  private audioCtx: AudioContext | null = null;
  private speechReady = false;
  private utter: SpeechSynthesisUtterance | null = null;
  private wakeLock: WakeLockSentinel | null = null;

  constructor() {
    window.addEventListener('resize', () => this.positionStage());
    document.addEventListener('visibilitychange', () => {
      if (document.visibilityState !== 'visible') return;
      if (this.audioCtx && this.audioCtx.state !== 'running') this.audioCtx.resume().catch(() => {});
      if (this.cameraWanted && !this.wakeLock) void this.requestWake();
      if (this.cameraWanted && this.stream) {
        const tr = this.stream.getVideoTracks()[0];
        if (!tr || tr.readyState === 'ended') void this.startCamera();
      }
    });
    if (navigator.mediaDevices) {
      navigator.mediaDevices.addEventListener?.('devicechange', () => void this.populateCameras());
      void this.populateCameras();
    }
  }

  /* ---------------- store ---------------- */
  subscribe = (listener: () => void): (() => void) => {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  };
  getSnapshot = (): Snapshot => {
    if (!this.snapshot) this.snapshot = this.buildSnapshot();
    return this.snapshot;
  };
  private emit(): void {
    this.snapshot = null;
    for (const l of this.listeners) l();
  }
  private buildSnapshot(): Snapshot {
    const run = this.run,
      t = now();
    return {
      settings: this.settings,
      run,
      paused: this.paused,
      live: !!this.stream,
      cameras: this.cameras,
      facingKnown: this.facingKnown,
      status: this.status,
      thumb: this.thumb,
      count: this.paused && this.pauseCount != null ? this.pauseCount : this.detector ? this.detector.count : 0,
      countdown: this.countdownEnd != null ? Math.max(0, Math.ceil(this.countdownEnd - t)) : null,
      elapsed: run ? (this.paused ? this.pausedAt : t) - run.t0 : 0,
      restLeft: run && run.phase === 'rest' ? Math.max(0, Math.ceil(run.restEnd - (this.paused ? this.pausedAt : t))) : 0,
      grid: this.app ? { cells: `${this.app.gx}×${this.app.gy}`, period: this.lastGrid?.periodValid ? this.lastGrid.period : null, pi: this.lastGrid?.pi ?? 0 } : null,
      summary: this.summary,
      summaryText: this.summaryText,
      wakeLockSupported: 'wakeLock' in navigator,
    };
  }

  /* ---------------- settings (the setup screen) ---------------- */
  private save(): void {
    try {
      localStorage.setItem(KEY, JSON.stringify(this.settings));
    } catch {
      /* private mode: the page still works for this visit */
    }
    this.emit();
  }
  private plan(): WorkoutPlan {
    return this.settings.current;
  }
  updateExercise(i: number, patch: Partial<Exercise>): void {
    const e = this.plan().exercises[i];
    if (!e) return;
    Object.assign(e, patch);
    this.settings = { ...this.settings, current: { ...this.plan(), exercises: [...this.plan().exercises] } };
    this.save();
  }
  setExercises(exercises: Exercise[]): void {
    if (!exercises.length) return;
    this.settings = { ...this.settings, current: { ...this.plan(), exercises } };
    this.save();
  }
  removeExercise(i: number): void {
    const ex = this.plan().exercises;
    if (ex.length <= 1) return;
    this.settings = { ...this.settings, current: { ...this.plan(), exercises: ex.filter((_, k) => k !== i) } };
    this.save();
  }
  addExercise(): number {
    const ex = this.plan().exercises,
      last = ex[ex.length - 1];
    this.settings = { ...this.settings, current: { ...this.plan(), exercises: [...ex, { name: '', reps: last ? last.reps : 100 }] } };
    this.save();
    return this.plan().exercises.length - 1;
  }
  /** Back to the default plan (exercises, rest, rounds); the sound settings stay. */
  resetPlan(): void {
    this.settings = { ...this.settings, current: defaultPlan() };
    this.save();
  }
  setRest(rest: number): void {
    this.settings = { ...this.settings, current: { ...this.plan(), rest } };
    this.save();
  }
  setRounds(rounds: number): void {
    this.settings = { ...this.settings, current: { ...this.plan(), rounds: clampInt(rounds, 1, 20) } };
    this.save();
  }
  setVoiceMode(voiceMode: VoiceMode): void {
    this.settings = { ...this.settings, voiceMode };
    this.save();
  }
  setBeep(beep: boolean): void {
    this.settings = { ...this.settings, beep };
    this.save();
  }
  /** The plan as it will run: blank names and bad numbers fixed up. */
  private validPlan(): WorkoutPlan {
    const w = this.plan();
    return {
      exercises: w.exercises.map((e, i) => ({ name: (e.name || '').trim() || `Exercise ${i + 1}`, reps: clampInt(+e.reps, 1, 999) })),
      rest: clampInt(+w.rest, 0, 600),
      rounds: clampInt(+w.rounds, 1, 20),
    };
  }

  /* ---------------- audio: one AudioContext, created on a tap (iOS); speech warmed up on the same tap ---------------- */
  ensureAudio(): void {
    try {
      if (!this.audioCtx) this.audioCtx = new (window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext)();
      if (this.audioCtx.state !== 'running') this.audioCtx.resume().catch(() => {});
    } catch {
      /* no audio: the page still counts */
    }
    if (!this.speechReady && 'speechSynthesis' in window) {
      this.speechReady = true;
      try {
        const u = new SpeechSynthesisUtterance(' ');
        u.volume = 0;
        speechSynthesis.speak(u);
      } catch {
        /* ignore */
      }
    }
  }
  private beep(freq = 880, dur = 0.09, gain = 0.25): void {
    const ctx = this.audioCtx;
    if (!ctx) return;
    try {
      const o = ctx.createOscillator(),
        g = ctx.createGain(),
        t = ctx.currentTime;
      o.type = 'sine';
      o.frequency.value = freq;
      g.gain.setValueAtTime(0, t);
      g.gain.linearRampToValueAtTime(gain, t + 0.005);
      g.gain.exponentialRampToValueAtTime(0.001, t + dur);
      o.connect(g).connect(ctx.destination);
      o.start(t);
      o.stop(t + dur + 0.02);
    } catch {
      /* ignore */
    }
  }
  private say(text: string): void {
    if (this.settings.voiceMode === 'off' || !('speechSynthesis' in window)) return;
    try {
      const u = new SpeechSynthesisUtterance(text);
      u.rate = 1.1;
      this.utter = u;
      if (speechSynthesis.speaking || speechSynthesis.pending) {
        speechSynthesis.cancel();
        setTimeout(() => {
          if (this.utter === u) speechSynthesis.speak(u);
        }, 60);
      } else speechSynthesis.speak(u);
    } catch {
      /* ignore */
    }
  }
  private callsCount(): boolean {
    return this.settings.voiceMode === 'checkpoints';
  }

  /* ---------------- keep the screen on ---------------- */
  private async requestWake(): Promise<void> {
    if (!('wakeLock' in navigator) || this.wakeLock) return;
    try {
      const l = await navigator.wakeLock.request('screen');
      this.wakeLock = l;
      l.addEventListener('release', () => {
        if (this.wakeLock === l) this.wakeLock = null;
      });
    } catch {
      /* ignore */
    }
  }
  private releaseWake(): void {
    try {
      void this.wakeLock?.release();
    } catch {
      /* ignore */
    }
    this.wakeLock = null;
  }

  /* ---------------- the stage: one video + overlay, fitted over a slot or shown as a thumbnail ---------------- */
  attachStage(stage: HTMLDivElement, video: HTMLVideoElement, overlay: HTMLCanvasElement): void {
    this.stage = stage;
    this.video = video;
    this.overlay = overlay;
    if (this.stream) {
      video.srcObject = this.stream;
      video.play().catch(() => {});
    }
    this.positionStage();
  }
  detachStage(): void {
    if (this.video) this.video.srcObject = null;
    this.stage = this.video = this.overlay = null;
  }
  setSlot(el: HTMLElement | null): void {
    this.slot = el;
    this.positionStage();
  }
  setThumbAnchor(el: HTMLElement | null): void {
    this.thumbAnchor = el;
    this.positionStage();
  }
  private positionStage(): void {
    const st = this.stage;
    if (!st) return;
    if (!this.stream) {
      st.style.visibility = 'hidden';
      return;
    }
    const ar = this.srcW / this.srcH;
    if (this.slot) {
      const r = this.slot.getBoundingClientRect();
      if (r.width < 10 || r.height < 10) {
        st.style.visibility = 'hidden';
        return;
      }
      let w = r.width,
        h = w / ar;
      if (h > r.height) {
        h = r.height;
        w = h * ar;
      }
      st.classList.remove('workout-stage--thumb');
      st.style.left = `${Math.round(r.left + (r.width - w) / 2)}px`;
      st.style.top = `${Math.round(r.top + (r.height - h) / 2)}px`;
      st.style.width = `${Math.round(w)}px`;
      st.style.height = `${Math.round(h)}px`;
    } else if (this.thumbAnchor) {
      const size = innerWidth >= 720 ? 160 : 104,
        r = this.thumbAnchor.getBoundingClientRect();
      let w: number, h: number;
      if (ar >= 1) {
        w = size;
        h = w / ar;
      } else {
        h = size;
        w = h * ar;
      }
      st.classList.add('workout-stage--thumb');
      st.style.left = `${Math.round(r.right - 16 - w)}px`;
      st.style.top = `${Math.round(r.top + 8)}px`;
      st.style.width = `${Math.round(w)}px`;
      st.style.height = `${Math.round(h)}px`;
    } else {
      st.style.visibility = 'hidden';
      return;
    }
    st.style.visibility = 'visible';
  }

  /* ---------------- camera ---------------- */
  private setStatus(text: string, tone: Tone): void {
    this.status = { text, tone };
  }
  private async populateCameras(): Promise<void> {
    try {
      const cams = (await navigator.mediaDevices.enumerateDevices()).filter((d) => d.kind === 'videoinput');
      this.cameras = cams.map((c, i) => ({ deviceId: c.deviceId, label: c.label || `Camera ${i + 1}` }));
      if (this.settings.camId && !cams.some((c) => c.deviceId === this.settings.camId)) this.settings = { ...this.settings, camId: '' };
      this.emit();
    } catch {
      /* no device access yet */
    }
  }
  /** The camera screen opened: start the camera (if it is not running), keep the screen on, and count down to the start once it is live. */
  openCamera(): void {
    this.ensureAudio();
    this.cameraWanted = true;
    void this.requestWake();
    if (!this.stream) void this.startCamera();
    else this.armCountdown();
  }
  /** Back on the setup screen: the camera is not needed. */
  closeCamera(): void {
    this.cameraWanted = false;
    this.cancelCountdown();
    this.stopSource();
    this.releaseWake();
    this.emit();
  }
  private armCountdown(): void {
    if (this.run || !this.stream) return;
    this.countdownEnd = now() + COUNTDOWN_S;
    this.countdownLast = -1;
    this.say('Get in position.');
    this.emit();
  }
  cancelCountdown(): void {
    this.countdownEnd = null;
  }
  /** Skip the countdown (Start now, or Space). */
  startNow(): void {
    this.cancelCountdown();
    this.startWorkout();
  }
  private countdownTick(t: number): void {
    if (this.countdownEnd == null) return;
    const left = Math.max(0, Math.ceil(this.countdownEnd - t));
    if (left === this.countdownLast) return;
    this.countdownLast = left;
    if (left >= 1 && left <= 3) this.beep(660, 0.08, 0.2);
    if (left <= 0) {
      this.countdownEnd = null;
      this.startWorkout();
    }
  }
  selectCamera(camId: string): void {
    this.ensureAudio();
    this.settings = { ...this.settings, camId };
    this.save();
    void this.startCamera();
  }
  flipCamera(): void {
    this.ensureAudio();
    this.settings = { ...this.settings, camId: '', facing: this.settings.facing === 'user' ? 'environment' : 'user' };
    this.save();
    void this.startCamera();
  }
  /** Watch the frame afresh (a new grid), keeping the count of the exercise in progress. */
  resetGrid(): void {
    this.ensureAudio();
    if (this.detector && this.run && this.run.phase === 'exercise') this.carryCount = this.detector.count;
    this.app = null;
    this.detector = this.run && this.run.phase === 'exercise' ? null : this.detector;
    this.gridLocked = false;
    this.lastGrid = null;
    this.frames = 0;
    this.updateStatus();
    this.emit();
  }
  private async startCamera(): Promise<void> {
    this.stopSource();
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
      this.setStatus(window.isSecureContext ? 'This browser has no camera access' : 'Cameras need https or localhost', 'bad');
      this.emit();
      return;
    }
    const s = this.settings,
      video: MediaTrackConstraints = { width: { ideal: 1280 }, height: { ideal: 720 }, frameRate: { ideal: 30 } };
    if (s.camId) video.deviceId = { exact: s.camId };
    else video.facingMode = { ideal: s.facing || 'environment' };
    this.setStatus('Starting camera…', 'warn');
    this.emit();
    let stream: MediaStream;
    try {
      stream = await navigator.mediaDevices.getUserMedia({ video, audio: false });
    } catch (e) {
      if (s.camId) {
        this.settings = { ...this.settings, camId: '' }; // the remembered device is gone: fall back to facing mode
        this.save();
        return this.startCamera();
      }
      const err = e as DOMException;
      this.setStatus(`Camera failed: ${err.name === 'NotAllowedError' ? 'permission denied' : err.message}`, 'bad');
      this.emit();
      return;
    }
    if (!this.cameraWanted) {
      stream.getTracks().forEach((t) => t.stop()); // the user left meanwhile
      return;
    }
    this.stream = stream;
    if (this.video) {
      this.video.srcObject = stream;
      this.video.play().catch(() => {});
    }
    const tr = stream.getVideoTracks()[0],
      st = tr.getSettings ? tr.getSettings() : {};
    this.facingKnown = !!st.facingMode;
    if (st.facingMode) this.settings = { ...this.settings, facing: st.facingMode as WorkoutSettings['facing'] };
    tr.addEventListener('ended', () => {
      // iOS stops the camera when the app is backgrounded; restart when we are back
      if (this.stream === stream && document.visibilityState === 'visible')
        setTimeout(() => {
          if (this.stream === stream) void this.startCamera();
        }, 500);
    });
    await this.populateCameras();
    this.save();
    this.frames = 0;
    this.startLoop();
    this.updateStatus();
    this.armCountdown();
    this.emit();
  }
  private stopSource(): void {
    this.loopId++;
    if (this.stream) {
      this.stream.getTracks().forEach((t) => t.stop());
      this.stream = null;
    }
    if (this.video) this.video.srcObject = null;
    this.positionStage();
  }
  private startLoop(): void {
    const id = ++this.loopId;
    if (!this.uiTimer) this.uiTimer = window.setInterval(() => this.uiTick(), 100);
    const step = () => {
      if (this.loopId !== id || !this.stream) return;
      this.processFrame();
      const v = this.video;
      if (v && 'requestVideoFrameCallback' in v) (v as HTMLVideoElement & { requestVideoFrameCallback(cb: () => void): number }).requestVideoFrameCallback(step);
      else requestAnimationFrame(step);
    };
    step();
  }
  private ensureProcSize(sw: number, sh: number): void {
    const ph = Math.max(60, Math.round(PROC_W * (sh / sw)));
    this.srcW = sw;
    this.srcH = sh;
    if (this.procH === ph && this.gray) return;
    this.proc.width = PROC_W;
    this.proc.height = ph;
    this.procW = PROC_W;
    this.procH = ph;
    this.gray = new Float32Array(PROC_W * ph);
    // the picture changed shape (phone turned?): a new grid, carrying the count of the exercise in progress
    if (this.detector && this.run && this.run.phase === 'exercise') this.carryCount = this.detector.count;
    this.frames = 0;
    this.app = null;
    this.detector = null;
    this.gridLocked = false;
    this.lastGrid = null;
  }

  /* ---------------- per-frame vision ---------------- */
  private processFrame(): void {
    const t = now(),
      v = this.video;
    if (!v) return;
    const sw = v.videoWidth,
      sh = v.videoHeight;
    if (!sw || !sh) return;
    this.ensureProcSize(sw, sh);
    this.procCtx.drawImage(v, 0, 0, this.procW, this.procH);
    toGray(this.procCtx.getImageData(0, 0, this.procW, this.procH).data, this.gray!);
    this.frames++;
    if (!this.app && this.frames > 5) this.setGrid();
    if (this.app) this.gridStep(t);
    this.drawOverlay();
  }
  private setGrid(): void {
    const box = { x: 0, y: 0, w: this.procW, h: this.procH };
    this.app = new AppearanceCounter({ fs: 30, cellPx: GRID_CELL_PX });
    this.app.setROI(box);
    this.gridLocked = false;
    this.lastGrid = null;
    if (!this.run) this.say('Ready. Go.');
    this.updateStatus();
  }
  private gridStep(t: number): void {
    const app = this.app!;
    const r = app.update(this.gray!, this.procW, this.procH, t);
    if (!r) return;
    this.lastGrid = { period: r.period, periodValid: r.periodValid, pi: r.pi };
    if (!this.gridLocked) {
      this.gridLocked = true;
      this.onGridReady();
    }
    if (r.event && !this.paused && this.run && this.run.phase === 'exercise' && this.detector === app) this.onRep();
  }
  /** The grid produced its first sample: counting can begin (or continue, after a rebuild). */
  private onGridReady(): void {
    const run = this.run,
      app = this.app!;
    if (run && run.phase === 'exercise') {
      if (!this.detector) {
        if (this.carryCount != null) {
          app.resetCount();
          app.manual = this.carryCount;
          app.count = this.carryCount;
          this.carryCount = null;
          this.detector = app;
        } else {
          this.startCounting();
          this.beep(660, 0.12);
          this.say(`${this.exercise()!.name}, go.`);
        }
      } else this.detector.resync();
    }
    this.updateStatus();
    this.emit();
  }
  private drawOverlay(): void {
    const c = this.overlay,
      st = this.stage;
    if (!c || !st || st.style.visibility === 'hidden') return;
    const rect = c.getBoundingClientRect(),
      dpr = devicePixelRatio || 1;
    const W = Math.round(rect.width * dpr),
      H = Math.round(rect.height * dpr);
    if (!W || !H) return;
    if (c.width !== W || c.height !== H) {
      c.width = W;
      c.height = H;
    }
    const ctx = c.getContext('2d')!;
    ctx.clearRect(0, 0, W, H);
    const app = this.app;
    if (!app) return;
    const sx = W / this.procW,
      sy = H / this.procH,
      thumb = st.classList.contains('workout-stage--thumb');
    const GX = app.gx,
      GY = app.gy,
      cw = W / GX,
      ch = H / GY;
    for (let j = 0; j < GY; j++)
      for (let i = 0; i < GX; i++) {
        const k = j * GX + i,
          z = app.zRaw[k];
        if (!app.mask[k]) ctx.fillStyle = 'rgba(0,0,0,.35)';
        else ctx.fillStyle = z > 0 ? `rgba(61,214,140,${Math.min(0.5, Math.abs(z) * 0.14)})` : `rgba(255,255,255,${Math.min(0.35, Math.abs(z) * 0.1)})`;
        ctx.fillRect(i * cw, j * ch, cw, ch);
      }
    ctx.strokeStyle = '#3dd68c';
    ctx.lineWidth = (thumb ? 1.5 : 2) * dpr;
    ctx.strokeRect(0, 0, this.procW * sx, this.procH * sy);
    if (!thumb) {
      const g = this.lastGrid;
      ctx.fillStyle = '#3dd68c';
      ctx.font = `${Math.round(13 * dpr)}px sans-serif`;
      ctx.fillText(
        (g && g.periodValid && g.period != null ? `rhythm ${g.period.toFixed(2)} s · ${(g.pi * 100) | 0} %` : app.idle ? 'waiting for movement' : 'counting every stroke') + ` · ${GX}×${GY}`,
        6 * dpr,
        16 * dpr,
      );
    }
  }
  private updateStatus(): void {
    if (!this.stream) return; // keep whatever startCamera said
    const g = this.lastGrid;
    if (!this.app || !this.gridLocked) {
      this.setStatus('Watching the whole frame — move and every rep counts', 'warn');
      this.thumb = 'hunt';
    } else if (g && g.periodValid && g.period != null) {
      this.setStatus(`Rhythm ${g.period.toFixed(1)} s · ${(g.pi * 100) | 0} %`, 'ok');
      this.thumb = 'ok';
    } else {
      this.setStatus(this.app.idle ? 'Ready — every rep counts from the first one' : 'Counting every stroke', 'ok');
      this.thumb = 'ok';
    }
  }
  private uiTick(): void {
    const t = now();
    this.countdownTick(t);
    this.tick(t);
    this.updateStatus();
    if (this.stream) this.positionStage();
    this.emit();
  }

  /* ---------------- the workout ---------------- */
  exercise(): RunExercise | null {
    return this.run ? (this.run.exercises[this.run.current] ?? null) : null;
  }
  /** Start the workout from the camera screen. False if the camera is not running. */
  startWorkout(): boolean {
    this.ensureAudio();
    if (!this.stream) return false;
    const w = this.validPlan();
    this.run = {
      startedAt: new Date().toISOString(),
      t0: now(),
      rest: w.rest,
      current: -1,
      phase: 'exercise',
      restEnd: 0,
      lastTick: null,
      pausedTotal: 0,
      rounds: w.rounds,
      exercises: Array.from({ length: w.rounds }, (_, r) => w.exercises.map((e) => ({ name: e.name, round: r + 1, target: e.reps, counted: 0, duration: 0, t0: null, reason: null }))).flat(),
      total: 0,
      duration: 0,
    };
    this.summary = null;
    this.paused = false;
    this.pauseCount = null;
    this.countdownEnd = null;
    void this.requestWake();
    this.beginExercise(0, 'start');
    return true;
  }
  private beginExercise(i: number, why: 'start' | 'next', said = ''): void {
    const run = this.run!,
      e = run.exercises[i];
    run.current = i;
    run.phase = 'exercise';
    run.lastTick = null;
    run.restEnd = 0;
    this.detector = null;
    e.t0 = null;
    if (this.app && why !== 'start') {
      this.app.recalibrate(now(), 'next-exercise'); // a new movement: learn its rhythm afresh
      this.lastGrid = null;
    }
    const lead = said + (why === 'next' ? 'Next: ' : '');
    if (this.app) {
      this.startCounting();
      this.beep(660, 0.12);
      this.say(`${lead}${e.name}. Go.`);
    } else this.say(`${lead}${e.name}. Keep repeating; every rep counts.`);
    this.emit();
  }
  private startCounting(): void {
    const e = this.exercise()!;
    e.t0 = now();
    this.app!.resetCount();
    this.detector = this.app;
  }
  private onRep(): void {
    const run = this.run,
      e = this.exercise();
    if (!run || run.phase !== 'exercise' || !e || !this.detector) return;
    this.announceRep(this.detector.count, e.target);
    this.emit();
    if (this.detector.count >= e.target) this.completeExercise('target');
  }
  private announceRep(n: number, target: number): void {
    if (this.settings.beep) this.beep(880, 0.09);
    if (!this.callsCount() || n >= target) return; // the final count is part of "done"
    if (checkpoints(target).has(n)) this.say(String(n));
  }
  private completeExercise(reason: EndReason): void {
    const run = this.run,
      e = this.exercise();
    if (!run || run.phase !== 'exercise' || !e) return;
    e.counted = this.detector ? this.detector.count : 0;
    e.duration = e.t0 == null ? 0 : Math.round((now() - e.t0) * 10) / 10;
    e.reason = reason;
    this.detector = null;
    const said = this.callsCount() && reason === 'target' && checkpoints(e.target).has(e.counted) ? `${e.counted}. ` : ''; // the last checkpoint, said together with what follows
    if (reason === 'end' || run.current >= run.exercises.length - 1) {
      this.finishWorkout(said);
      return;
    }
    if (run.rest <= 0) {
      this.beep(523, 0.15, 0.3);
      this.beginExercise(run.current + 1, 'next', said);
      return;
    }
    run.phase = 'rest';
    run.restEnd = now() + run.rest;
    run.lastTick = null;
    this.beep(523, 0.15, 0.3);
    this.say(`${said}Exercise done. Rest.`);
    this.emit();
  }
  private tick(t: number): void {
    const run = this.run;
    if (!run || run.phase !== 'rest' || this.paused) return;
    const left = run.restEnd - t,
      sec = Math.max(0, Math.ceil(left));
    if (sec <= 3 && sec >= 1 && run.lastTick !== sec) {
      run.lastTick = sec;
      this.beep(660, 0.08, 0.2);
    }
    if (left <= 0) this.beginExercise(run.current + 1, 'next');
  }
  togglePause(): void {
    const run = this.run;
    if (!run || run.phase === 'done') return;
    const t = now();
    if (!this.paused) {
      this.paused = true;
      this.pausedAt = t;
      this.pauseCount = this.detector ? this.detector.count : null;
      this.beep(392, 0.12, 0.2);
      this.say('Paused');
    } else {
      this.resumeClocks(t);
      this.beep(660, 0.12, 0.2);
      this.say('Go');
    }
    this.emit();
  }
  private resumeClocks(t: number): void {
    // shift every clock by the paused time so the pause is invisible to the workout
    const run = this.run!,
      dt = t - this.pausedAt;
    this.paused = false;
    run.t0 += dt;
    run.pausedTotal += dt;
    const e = this.exercise();
    if (e && e.t0 != null) e.t0 += dt;
    if (run.restEnd) run.restEnd += dt;
    if (this.detector) {
      if (this.detector.lastRepAt != null) this.detector.lastRepAt += dt;
      // moving while paused cannot count: the counter kept watching (so its picture of the movement stays
      // continuous), but whatever it counted meanwhile is taken back here, and the running stroke is forgotten
      if (this.pauseCount != null && this.detector.count !== this.pauseCount) {
        this.detector.manual -= this.detector.count - this.pauseCount;
        this.detector.count = this.pauseCount;
      }
      this.detector.resync();
    }
    this.pauseCount = null;
  }
  skip(): void {
    const run = this.run;
    if (!run || run.phase === 'done') return;
    if (this.paused) this.resumeClocks(now());
    if (run.phase === 'exercise') this.completeExercise('skip');
    else if (run.phase === 'rest') this.beginExercise(run.current + 1, 'next');
  }
  /** End the workout now (the screen confirms first). */
  endWorkout(): void {
    const run = this.run;
    if (!run || run.phase === 'done') return;
    if (this.paused) this.resumeClocks(now());
    if (run.phase === 'exercise') this.completeExercise('end');
    else this.finishWorkout();
  }
  private finishWorkout(said = ''): void {
    const run = this.run;
    if (!run || run.phase === 'done') return;
    run.phase = 'done';
    run.duration = Math.round((now() - run.t0) * 10) / 10;
    run.total = run.exercises.reduce((a, e) => a + (e.counted || 0), 0);
    const rec: Summary = {
      when: run.startedAt,
      duration: run.duration,
      total: run.total,
      exercises: run.exercises.map((e) => ({ name: e.name, target: e.target, counted: e.counted || 0, duration: e.duration || 0, reason: e.reason })),
    };
    this.beep(784, 0.1, 0.3);
    setTimeout(() => this.beep(988, 0.1, 0.3), 130);
    setTimeout(() => this.beep(1175, 0.18, 0.3), 260);
    this.say(`${said}Workout complete. ${run.total} reps.`);
    this.cameraWanted = false;
    this.stopSource();
    this.releaseWake();
    this.paused = false;
    this.summary = rec;
    this.summaryText = [
      `Workout ${fmtWhen(rec.when)}: ${rec.total} reps in ${fmtDur(rec.duration)}`,
      ...rec.exercises.map((e, i) => `${i + 1}. ${e.name}: ${e.reason ? `${e.counted}/${e.target} (${fmtDur(e.duration)})` : 'not done'}`),
    ].join('\n');
    this.emit();
  }
  /** The summary was read: back to the setup screen. */
  clearRun(): void {
    this.run = null;
    this.summary = null;
    this.summaryText = '';
    this.emit();
  }
}

export const engine = new WorkoutEngine();
