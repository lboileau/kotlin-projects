// Barrel for the "plans" route area — every route under /plans (the page
// and all its sheets) is dynamically imported through this one module
// specifier, so router.tsx's lazy() calls all resolve to a single chunk:
// opening a sheet after the page has loaded needs no further network
// fetch, only a synchronous re-export lookup.
export { PlansPage } from './PlansPage';
export { NewPlanSheet } from './NewPlanSheet';
export { PlanDetailPage } from './PlanDetailPage';
export { AddRecipeToPlanSheet } from './AddRecipeToPlanSheet';
export { EditPlanSheet } from './EditPlanSheet';
export { JoinPlanPage } from './JoinPlanPage';
