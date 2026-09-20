import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  addManualItem,
  getShoppingList,
  removeManualItem,
  resetPurchases,
  updatePurchase,
  type ShoppingListResponse,
} from '../api/shopping';
import { ApiError } from '../api/http';
import { shoppingKey } from './plans';
import {
  applyRowToggle,
  captureManualItem,
  insertTempManualItem,
  reinsertManualItem,
  removeManualItemFromList,
  replaceTempManualItem,
  restoreRowEntries,
  type ShoppingRow,
} from '../lib/shoppingRows';
import { toast } from '../lib/toastStore';

export { shoppingKey };

export function useShoppingList(planId: string | undefined) {
  return useQuery({
    queryKey: shoppingKey(planId ?? ''),
    queryFn: () => getShoppingList(planId!),
    enabled: !!planId,
    retry: (failureCount, error) => {
      // A 404 means "not found", a 403 means "no access" — both show
      // immediately, neither is worth retrying (mirrors usePlan).
      if (error instanceof ApiError && (error.status === 404 || error.status === 403)) return false;
      return failureCount < 1;
    },
  });
}

function rowDisplayName(row: ShoppingRow): string {
  return row.ingredientName ?? row.description ?? 'Item';
}

/** Only invalidate once every in-flight mutation for this list has settled, so rapid taps across rows never flicker or lose state. */
function invalidateIfLast(queryClient: ReturnType<typeof useQueryClient>, planId: string) {
  if (queryClient.isMutating({ mutationKey: shoppingKey(planId) }) === 1) {
    void queryClient.invalidateQueries({ queryKey: shoppingKey(planId) });
  }
}

// Per-row (by plan + row key) network serialization + coalescing. Two
// PATCHes for the same row can otherwise reach the server out of order
// (tap check then uncheck fast — "uncheck" can land first). `rowChains`
// serializes: each row's next send waits for its previous one to
// settle. `rowLatestChecked` coalesces: if a newer tap for that row
// arrives before an earlier one's turn comes, the earlier one is
// skipped entirely and only the latest desired state is ever sent — the
// optimistic cache write (in onMutate, unaffected by any of this) is
// what keeps the UI feeling instant regardless.
//
// Keyed by `${planId}:${row.key}`, not `row.key` alone: ingredients are a
// global table, so the same ingredient can appear in two different
// plans' shopping lists with the identical row key. Keying by row alone
// let a toggle in one plan coalesce with a toggle of the same ingredient
// in another plan (e.g. switching plans quickly via SwitchPlanSheet),
// silently dropping one of the two sends.
const rowChains = new Map<string, Promise<void>>();
const rowLatestChecked = new Map<string, boolean>();

function chainKey(planId: string, row: ShoppingRow): string {
  return `${planId}:${row.key}`;
}

function sendRowState(planId: string, row: ShoppingRow, checked: boolean): Promise<void> {
  return Promise.all(
    row.entries.map((entry) => {
      const quantityPurchased = checked ? entry.quantityRequired : 0;
      return entry.manualItemId
        ? updatePurchase(planId, { manualItemId: entry.manualItemId, quantityPurchased })
        : updatePurchase(planId, { ingredientId: entry.ingredientId!, unit: entry.unit!, quantityPurchased });
    }),
  ).then(() => undefined);
}

function syncRowState(planId: string, row: ShoppingRow, checked: boolean): Promise<void> {
  const rowKey = chainKey(planId, row);
  rowLatestChecked.set(rowKey, checked);

  const previousLink = rowChains.get(rowKey) ?? Promise.resolve();
  const thisLink = previousLink
    .catch(() => {
      // A previous link's own failure is reported through THAT specific
      // mutate() call, not this one — don't let it block this row's
      // future taps from ever running.
    })
    .then(() => {
      const desired = rowLatestChecked.get(rowKey);
      if (desired === undefined) return undefined; // a later link already sent it
      rowLatestChecked.delete(rowKey);
      return sendRowState(planId, row, desired);
    });

  rowChains.set(rowKey, thisLink);
  return thisLink;
}

interface ToggleRowInput {
  row: ShoppingRow;
  checked: boolean;
}

/** Optimistic check-off: flips every entry of a merged row together. Network sends are serialized and coalesced per row (see `syncRowState`) — the cache write below is what makes it feel instant. */
export function useToggleShoppingRow(planId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationKey: shoppingKey(planId),
    mutationFn: ({ row, checked }: ToggleRowInput) => syncRowState(planId, row, checked),
    onMutate: async ({ row, checked }) => {
      await queryClient.cancelQueries({ queryKey: shoppingKey(planId) });
      queryClient.setQueryData<ShoppingListResponse>(shoppingKey(planId), (current) =>
        current ? applyRowToggle(current, row, checked) : current,
      );
    },
    onError: (_error, { row }) => {
      // Targeted: only this row's entries revert, on top of whatever the
      // cache holds right now — never a whole-list snapshot, which would
      // also undo any other row a concurrent tap already committed.
      queryClient.setQueryData<ShoppingListResponse>(shoppingKey(planId), (current) =>
        current ? restoreRowEntries(current, row.entries) : current,
      );
      toast.error(`Couldn't update ${rowDisplayName(row)}.`);
    },
    meta: { suppressErrorToast: true },
    onSettled: () => invalidateIfLast(queryClient, planId),
  });
}

/** Optimistic quick-add: inserts a temp row under Misc, swaps it for the real one on success, removes it on failure. */
export function useAddManualShoppingItem(planId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationKey: shoppingKey(planId),
    mutationFn: (description: string) => addManualItem(planId, description),
    onMutate: async (description) => {
      await queryClient.cancelQueries({ queryKey: shoppingKey(planId) });
      const tempId = `temp-${crypto.randomUUID()}`;
      queryClient.setQueryData<ShoppingListResponse>(shoppingKey(planId), (current) =>
        current ? insertTempManualItem(current, description, tempId) : current,
      );
      return { tempId };
    },
    onSuccess: (created, _description, context) => {
      if (!context) return;
      queryClient.setQueryData<ShoppingListResponse>(shoppingKey(planId), (current) =>
        current ? replaceTempManualItem(current, context.tempId, created) : current,
      );
    },
    onError: (_error, description, context) => {
      // Targeted: remove just this temp row from the current cache,
      // rather than reverting to a stale pre-mutation snapshot.
      if (context) {
        queryClient.setQueryData<ShoppingListResponse>(shoppingKey(planId), (current) =>
          current ? removeManualItemFromList(current, context.tempId) : current,
        );
      }
      toast.error(`Couldn't add "${description}".`);
    },
    meta: { suppressErrorToast: true },
    onSettled: () => invalidateIfLast(queryClient, planId),
  });
}

/** Optimistic manual-item removal. */
export function useRemoveManualShoppingItem(planId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationKey: shoppingKey(planId),
    mutationFn: (manualItemId: string) => removeManualItem(planId, manualItemId),
    onMutate: async (manualItemId) => {
      await queryClient.cancelQueries({ queryKey: shoppingKey(planId) });
      const current = queryClient.getQueryData<ShoppingListResponse>(shoppingKey(planId));
      const removed = current ? captureManualItem(current, manualItemId) : null;
      if (current) {
        queryClient.setQueryData<ShoppingListResponse>(shoppingKey(planId), removeManualItemFromList(current, manualItemId));
      }
      return { removed };
    },
    onError: (_error, _manualItemId, context) => {
      // Targeted: splice just this item back into the current cache.
      if (context?.removed) {
        const removed = context.removed;
        queryClient.setQueryData<ShoppingListResponse>(shoppingKey(planId), (latest) =>
          latest ? reinsertManualItem(latest, removed) : latest,
        );
      }
      toast.error("Couldn't remove the item.");
    },
    meta: { suppressErrorToast: true },
    onSettled: () => invalidateIfLast(queryClient, planId),
  });
}

/** Not optimistic — gated behind a confirm dialog already, so waiting for the server is simplest and safest. */
export function useResetPurchases(planId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => resetPurchases(planId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: shoppingKey(planId) });
    },
  });
}
