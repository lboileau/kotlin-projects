import { useEffect, useRef } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Button, IconButton, Select, Text } from '@radix-ui/themes';
import { LoopIcon } from '@radix-ui/react-icons';
import { engine } from './engine/workoutEngine';
import { useEngine } from './useEngine';
import { WorkoutHeader } from './WorkoutChrome';

const DEFAULT_CAMERA = 'default';

/**
 * Aim the camera: the whole frame is watched, so there is nothing to draw or tap. Once the
 * camera is live a five-second countdown runs over the preview and the workout starts by itself;
 * "Start now" (or Space) skips the wait, the back button cancels.
 */
export function WorkoutCameraScreen() {
  const snap = useEngine();
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const slotRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    engine.openCamera();
    engine.setSlot(slotRef.current);
    return () => {
      engine.setSlot(null);
      engine.cancelCountdown();
    };
  }, []);

  // The countdown (or Start now) started the workout: on to the live screen.
  useEffect(() => {
    if (snap.run && !snap.summary && pathname === '/workout/camera') navigate('/workout/live', { replace: true });
  }, [snap.run, snap.summary, pathname, navigate]);

  useEffect(() => {
    function onKey(event: KeyboardEvent): void {
      if ((event.target as HTMLElement | null)?.matches('input, select, textarea')) return;
      if (event.code === 'Space') {
        event.preventDefault();
        engine.startNow();
      }
    }
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, []);

  const cameraLabel = snap.facingKnown ? (snap.settings.facing === 'user' ? 'Front camera' : 'Rear camera') : 'Default camera';

  return (
    <section className="workout-screen workout-screen--wide">
      <WorkoutHeader
        title="Camera"
        backTo="/workout"
        trailing={
          <>
            <Select.Root value={snap.settings.camId || DEFAULT_CAMERA} onValueChange={(v) => engine.selectCamera(v === DEFAULT_CAMERA ? '' : v)} size="2">
              <Select.Trigger className="workout-camera__select" aria-label="Camera" />
              <Select.Content position="popper">
                <Select.Item value={DEFAULT_CAMERA}>{cameraLabel}</Select.Item>
                {snap.cameras.map((c) => (
                  <Select.Item key={c.deviceId} value={c.deviceId}>
                    {c.label}
                  </Select.Item>
                ))}
              </Select.Content>
            </Select.Root>
            {snap.facingKnown && (
              <IconButton variant="soft" size="3" aria-label="Switch between rear and front camera" onClick={() => engine.flipCamera()}>
                <LoopIcon />
              </IconButton>
            )}
          </>
        }
      />
      <div ref={slotRef} className="workout-slot">
        {snap.countdown != null && (
          <div className="workout-countdown" role="timer" aria-live="assertive" aria-label={`Starting in ${snap.countdown}`}>
            <span className="workout-countdown__number">{snap.countdown}</span>
          </div>
        )}
      </div>
      <footer className="workout-actions">
        <Text as="p" size="3" className="workout-status" data-tone={snap.status.tone}>
          {snap.countdown != null ? `Starting in ${snap.countdown}… get in position` : snap.status.text}
        </Text>
        <Button variant="soft" size="3" className="workout-wide" disabled={!snap.live} onClick={() => engine.startNow()}>
          Start now
        </Button>
        {!snap.wakeLockSupported && (
          <Text as="p" size="2" color="gray" className="workout-hint">
            This browser cannot keep the screen awake. Set Auto-Lock to Never before you start.
          </Text>
        )}
      </footer>
    </section>
  );
}
