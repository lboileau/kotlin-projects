/** Servings a new plan starts with. */
export const DEFAULT_PLAN_SERVINGS = 2;

/** "Sep 20" — today's date, the name a new plan gets when none is typed. */
export function todaysDateLabel(): string {
  return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric' }).format(new Date());
}
