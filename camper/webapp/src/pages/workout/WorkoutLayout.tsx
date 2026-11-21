import { useEffect, useRef } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { Theme } from '@radix-ui/themes';
import { engine } from './engine/workoutEngine';
import { useEngine } from './useEngine';
import { usePageTitle } from '../../lib/usePageTitle';
import './WorkoutPage.css';

/**
 * Layout route for /workout: its own dark Radix theme (near-black slate, one green accent) over the whole viewport,
 * the one camera stage (a video and an overlay canvas) that the engine fits over whichever
 * screen's slot is showing or parks as a thumbnail during a workout, and the screens as child
 * routes — setup, camera, live, summary — so every state has a URL. Standalone: no sign-in, no
 * API, no tab bar; the settings live in localStorage.
 */
export function WorkoutLayout() {
  const snap = useEngine();
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const stageRef = useRef<HTMLDivElement>(null);
  const videoRef = useRef<HTMLVideoElement>(null);
  const overlayRef = useRef<HTMLCanvasElement>(null);
  usePageTitle('Workout');

  useEffect(() => {
    engine.attachStage(stageRef.current!, videoRef.current!, overlayRef.current!);
    return () => engine.detachStage();
  }, []);

  // The browser colours the status bar from theme-color: the page's near-black here, the app's white again on leaving.
  useEffect(() => {
    const meta = document.querySelector('meta[name="theme-color"]');
    const previous = meta?.getAttribute('content');
    meta?.setAttribute('content', '#0b0c0f');
    return () => {
      if (previous != null) meta?.setAttribute('content', previous);
    };
  }, []);

  // Back on the setup screen the camera is not needed; the summary (set by the engine when the
  // last exercise ends) is its own screen.
  useEffect(() => {
    if (pathname === '/workout') engine.closeCamera();
  }, [pathname]);
  useEffect(() => {
    if (snap.summary && pathname !== '/workout/summary') navigate('/workout/summary', { replace: true });
  }, [snap.summary, pathname, navigate]);

  return (
    <Theme appearance="dark" accentColor="green" grayColor="slate" radius="large" hasBackground={false} className="workout-theme">
      <div className="workout">
        <Outlet />
        <div ref={stageRef} className="workout-stage" data-tone={snap.thumb}>
          <video ref={videoRef} playsInline muted autoPlay />
          <canvas ref={overlayRef} />
        </div>
      </div>
    </Theme>
  );
}
