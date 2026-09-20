import { useContext, useEffect } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { SyncContext, type SyncMessage } from './SyncContext';
import { planKey, plansKey, shoppingKey } from '../queries/plans';

const RECHECK_INTERVAL_MS = 300;

/**
 * Subscribes to `/topic/meal-plans/{planId}` while mounted — used by
 * PlanDetailPage and the shopping page, which share one topic
 * subscription via the provider's reference counting. On a message (or
 * a reconnect resync): `deleted` invalidates `['plans','mine']` and
 * `['plan', id]`; anything else invalidates `['plan', id]` and
 * `['shopping', id]`. If a mutation for this plan is still in flight,
 * the invalidation is deferred until it settles, so the user's own
 * optimistic rows never flicker from their own echo.
 */
export function useMealPlanSync(planId: string | undefined): void {
  const context = useContext(SyncContext);
  const queryClient = useQueryClient();

  useEffect(() => {
    if (!context || !planId) return;

    let pendingInterval: number | undefined;

    function isMutating(): boolean {
      return (
        queryClient.isMutating({ mutationKey: planKey(planId!) }) > 0 ||
        queryClient.isMutating({ mutationKey: shoppingKey(planId!) }) > 0
      );
    }

    function invalidate(deleted: boolean) {
      if (deleted) {
        // Also invalidate shoppingKey — otherwise an open shopping page
        // for a plan deleted elsewhere keeps showing its last-fetched
        // list instead of picking up the 404 that drives its not-found
        // state.
        void queryClient.invalidateQueries({ queryKey: plansKey });
        void queryClient.invalidateQueries({ queryKey: planKey(planId!) });
        void queryClient.invalidateQueries({ queryKey: shoppingKey(planId!) });
      } else {
        void queryClient.invalidateQueries({ queryKey: planKey(planId!) });
        void queryClient.invalidateQueries({ queryKey: shoppingKey(planId!) });
      }
    }

    function handleMessage(message: SyncMessage) {
      const deleted = message?.action === 'deleted';

      if (!isMutating()) {
        invalidate(deleted);
        return;
      }

      if (pendingInterval !== undefined) window.clearInterval(pendingInterval);
      pendingInterval = window.setInterval(() => {
        if (!isMutating()) {
          window.clearInterval(pendingInterval);
          pendingInterval = undefined;
          invalidate(deleted);
        }
      }, RECHECK_INTERVAL_MS);
    }

    const unsubscribe = context.subscribe(`/topic/meal-plans/${planId}`, handleMessage);

    return () => {
      unsubscribe();
      if (pendingInterval !== undefined) window.clearInterval(pendingInterval);
    };
  }, [context, planId, queryClient]);
}
