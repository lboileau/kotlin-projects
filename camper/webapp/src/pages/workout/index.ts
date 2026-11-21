// Barrel for the "workout" route area: the layout and its four screens are one lazy chunk (see
// router.tsx and vite.config.ts), so moving between setup, camera, live and summary never fetches.
export { WorkoutLayout } from './WorkoutLayout';
export { WorkoutSetupScreen } from './WorkoutSetupScreen';
export { WorkoutCameraScreen } from './WorkoutCameraScreen';
export { WorkoutLiveScreen } from './WorkoutLiveScreen';
export { WorkoutSummaryScreen } from './WorkoutSummaryScreen';
