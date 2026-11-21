import { useEffect, useRef, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { AlertDialog, Button, Flex } from '@radix-ui/themes';
import { engine, fmtDur } from './engine/workoutEngine';
import { useEngine } from './useEngine';

/**
 * The workout itself, glanceable from across the garage: one huge count, the exercise, the clock,
 * the camera as a thumbnail top-right; a rest countdown between exercises. On a laptop Space
 * pauses, N skips and Esc ends, unadvertised: the page is for a phone.
 */
export function WorkoutLiveScreen() {
  const snap = useEngine();
  const topRef = useRef<HTMLElement>(null);
  const [confirmEnd, setConfirmEnd] = useState(false);

  useEffect(() => {
    engine.setThumbAnchor(topRef.current);
    return () => engine.setThumbAnchor(null);
  }, []);

  useEffect(() => {
    function onKey(event: KeyboardEvent): void {
      if ((event.target as HTMLElement | null)?.matches('input, select, textarea')) return;
      if (event.code === 'Space') {
        event.preventDefault();
        engine.togglePause();
      } else if (event.code === 'Escape') setConfirmEnd(true);
      else if (event.code === 'KeyN') engine.skip();
    }
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, []);

  const run = snap.run;
  if (!run) return <Navigate to="/workout" replace />;
  const e = run.exercises[run.current],
    n = run.exercises.length,
    next = run.exercises[run.current + 1];
  const target = e?.target ?? 0,
    count = snap.count;
  const digits = String(count).length;

  return (
    <section className="workout-screen workout-screen--wide workout-live" data-paused={snap.paused || undefined}>
      <header ref={topRef} className="workout-live__top">
        <div className="workout-live__head">
          <div className="workout-live__index">
            Exercise {run.current + 1} of {n}
            {run.rounds > 1 && e ? ` · round ${e.round}` : ''}
          </div>
          <div className="workout-live__name">{e?.name ?? '—'}</div>
        </div>
        <div className="workout-live__clock">{fmtDur(snap.elapsed)}</div>
      </header>
      <div className="workout-live__main">
        {run.phase === 'rest' ? (
          <div className="workout-phase">
            <div className="workout-phase__label">Rest</div>
            <div className="workout-count workout-count--rest" data-digits={Math.min(4, String(snap.restLeft).length)}>
              {snap.restLeft}
            </div>
            <div className="workout-next">
              <div className="workout-live__index">Next</div>
              <div className="workout-next__name">{next?.name ?? '—'}</div>
            </div>
          </div>
        ) : (
          <div className="workout-phase">
            {snap.paused && <div className="workout-paused">Paused</div>}
            {/* keyed on the count so each rep remounts it and replays the pop (CSS keyframes, none under reduced motion) */}
            <div key={count} className="workout-count workout-count--pop" data-digits={Math.min(4, digits)}>
              {count}
            </div>
            <div className="workout-progress">
              <div className="workout-progress__bar" style={{ width: `${target ? Math.min(100, (100 * count) / target) : 0}%` }} />
            </div>
            <div className="workout-count-sub">
              {count} of {target}
            </div>
          </div>
        )}
      </div>
      <footer className="workout-actions workout-live__actions">
        <div className="workout-row workout-row--even">
          <Button variant={snap.paused ? 'solid' : 'soft'} size="3" onClick={() => engine.togglePause()}>
            {snap.paused ? 'Resume' : 'Pause'}
          </Button>
          <Button variant="soft" size="3" onClick={() => engine.skip()}>
            Skip ›
          </Button>
          <Button variant="soft" color="red" size="3" onClick={() => setConfirmEnd(true)}>
            End
          </Button>
        </div>
      </footer>

      <AlertDialog.Root open={confirmEnd} onOpenChange={setConfirmEnd}>
        <AlertDialog.Content maxWidth="360px">
          <AlertDialog.Title>End the workout?</AlertDialog.Title>
          <AlertDialog.Description size="2">The exercises done so far go on the summary.</AlertDialog.Description>
          <Flex gap="3" mt="4" justify="end">
            <AlertDialog.Cancel>
              <Button variant="soft" color="gray">
                Keep going
              </Button>
            </AlertDialog.Cancel>
            <AlertDialog.Action>
              <Button color="red" onClick={() => engine.endWorkout()}>
                End workout
              </Button>
            </AlertDialog.Action>
          </Flex>
        </AlertDialog.Content>
      </AlertDialog.Root>
    </section>
  );
}
