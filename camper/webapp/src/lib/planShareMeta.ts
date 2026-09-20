import type { PlanRole } from '../api/mealPlans';

export interface PlanShareMeta {
  /** Show a small "Shared" badge next to the plan's name. */
  shared: boolean;
  /** "by {owner}" for a member, "Shared with {n}" for an owner with members, else null. */
  metaText: string | null;
}

/**
 * How a plan row should show its sharing status, shared across every
 * plan list (Plans home, the Shopping tab's chooser, SwitchPlanSheet,
 * AddToPlanSheet). Role always comes from the API, never from comparing
 * ids on the client.
 */
export function planShareMeta(plan: { role: PlanRole; memberCount: number; ownerName: string }): PlanShareMeta {
  if (plan.role === 'member') {
    return { shared: true, metaText: `by ${plan.ownerName}` };
  }
  if (plan.memberCount > 0) {
    return { shared: false, metaText: `Shared with ${plan.memberCount}` };
  }
  return { shared: false, metaText: null };
}
