import { useState } from 'react';
import { api, type MealPlanDetailResponse } from '../api/client';
import { useToast } from '../context/ToastContext';
import { Button } from './ui/Button';
import './MealPlanReconcileBanner.css';

export interface MealPlanReconcileBannerProps {
  mealPlan: MealPlanDetailResponse | null;
  memberCount: number;
  planId: string;
  /** Called after PUT /meal-plans/:id succeeds. Parent should refetch data. */
  onUpdated: () => void;
}

export function MealPlanReconcileBanner({
  mealPlan,
  memberCount,
  planId,
  onUpdated,
}: MealPlanReconcileBannerProps) {
  const storageKey = `mealPlanReconcileDismissed:${planId}`;

  const [dismissed, setDismissed] = useState<boolean>(
    () => sessionStorage.getItem(storageKey) === '1',
  );
  const [updating, setUpdating] = useState(false);
  const toast = useToast();

  // Visibility guards — keep these early returns so the render is simple.
  if (mealPlan == null) return null;
  if (memberCount === 0) return null;
  if (mealPlan.servings === memberCount) return null;
  if (dismissed) return null;

  const handleUpdate = async () => {
    setUpdating(true);
    try {
      await api.updateMealPlan(mealPlan.id, { servings: memberCount });
      // Clear dismissal key so the banner reappears if servings drift again.
      sessionStorage.removeItem(storageKey);
      toast.success(`Meal plan updated to ${memberCount} servings`);
      onUpdated();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Failed to update servings');
    } finally {
      setUpdating(false);
    }
  };

  const handleDismiss = () => {
    sessionStorage.setItem(storageKey, '1');
    setDismissed(true);
  };

  const servingWord = mealPlan.servings === 1 ? 'serving' : 'servings';

  return (
    <div className="meal-plan-reconcile-banner" role="status" aria-live="polite">
      <span className="meal-plan-reconcile-banner__icon" aria-hidden="true">⚠</span>
      <span className="meal-plan-reconcile-banner__text">
        Your meal plan is set for {mealPlan.servings} {servingWord}, but you have{' '}
        {memberCount} adventurers in camp.
      </span>
      <Button
        size="sm"
        onClick={handleUpdate}
        disabled={updating}
        className="meal-plan-reconcile-banner__update"
      >
        {updating ? 'Updating…' : `Update to ${memberCount}`}
      </Button>
      <Button
        size="sm"
        variant="ghost"
        onClick={handleDismiss}
        className="meal-plan-reconcile-banner__dismiss"
        aria-label="Dismiss reminder"
      >
        ×
      </Button>
    </div>
  );
}
