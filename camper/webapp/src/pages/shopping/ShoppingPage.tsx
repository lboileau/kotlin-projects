import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Link, Outlet, useNavigate, useParams } from 'react-router-dom';
import { AlertDialog, Button, DropdownMenu, Heading, Progress, Skeleton, Text, TextField } from '@radix-ui/themes';
import { ChevronDownIcon, DotsVerticalIcon, PersonIcon, PlusIcon } from '@radix-ui/react-icons';
import { SheetLink } from '../../components/SheetLink';
import { QueryErrorState } from '../../components/QueryErrorState';
import { ApiError } from '../../api/http';
import type { ShoppingListResponse } from '../../api/shopping';
import { usePageTitle } from '../../lib/usePageTitle';
import {
  useAddManualShoppingItem,
  useRemoveManualShoppingItem,
  useResetPurchases,
  useShoppingList,
  useToggleShoppingRow,
} from '../../queries/shopping';
import { buildShoppingRows, sortTier, type ShoppingRow } from '../../lib/shoppingRows';
import { clearSelectedPlanId, getSelectedPlanId, setSelectedPlanId } from '../../lib/selectedPlan';
import { useMealPlanSync } from '../../sync/useMealPlanSync';
import { toast } from '../../lib/toastStore';
import { ShoppingRowItem } from './ShoppingRowItem';
import './ShoppingPage.css';

function categoryLabel(category: string): string {
  return category.charAt(0).toUpperCase() + category.slice(1);
}

function isTempRow(row: ShoppingRow): boolean {
  return row.manualItemId?.startsWith('temp-') ?? false;
}

export function ShoppingPage() {
  const { planId } = useParams<{ planId: string }>();
  useMealPlanSync(planId);

  // The plan name comes from the shopping list response itself
  // (`mealPlanName`) rather than a separate `usePlan()` fetch — the plan
  // detail endpoint is one of the slower ones (N+1 on the server, see
  // plan.md's risks), and nothing else on this page needs it: `notFound`
  // is derived from this same query's own error, and the empty-state /
  // dropdown links below are plain routes that don't need plan data.
  const { data: list, isLoading, isError, error, refetch } = useShoppingList(planId);
  usePageTitle(list?.mealPlanName ? `Shopping — ${list.mealPlanName}` : 'Shopping');

  const notFound = isError && error instanceof ApiError && error.status === 404;

  useEffect(() => {
    if (planId) setSelectedPlanId(planId);
  }, [planId]);

  useEffect(() => {
    if (notFound && planId && getSelectedPlanId() === planId) clearSelectedPlanId();
  }, [notFound, planId]);

  if (notFound) {
    return (
      <div className="shopping-page">
        <div className="shopping-page__not-found">
          <Heading as="h1" size="4" weight="medium">
            Plan not found
          </Heading>
          <Text color="gray" size="2">
            It may have been deleted, or the link is wrong.
          </Text>
          <Button asChild size="3" variant="solid">
            <Link to="/plans">Back to Plans</Link>
          </Button>
        </div>
        <Outlet />
      </div>
    );
  }

  // Gated on the absence of data: this page is refetched by live sync, so
  // a background refetch error (data already loaded) must not blank an
  // already-rendered list — only a failure with nothing to show yet does.
  if (isError && !list) {
    return (
      <div className="shopping-page">
        <Heading as="h1" className="sr-only">
          Shopping
        </Heading>
        <QueryErrorState message="Couldn't load the shopping list." onRetry={() => void refetch()} />
        <Outlet />
      </div>
    );
  }

  if (isLoading || !list) {
    return (
      <div className="shopping-page">
        <Heading as="h1" className="sr-only">
          Shopping
        </Heading>
        <div className="shopping-page__skeleton" aria-busy="true" aria-label="Loading shopping list">
          <Skeleton height="32px" aria-hidden="true" />
          <Skeleton height="56px" aria-hidden="true" />
          <Skeleton height="56px" aria-hidden="true" />
          <Skeleton height="56px" aria-hidden="true" />
        </div>
        <Outlet />
      </div>
    );
  }

  // Keyed on planId: this route doesn't remount when SwitchPlanSheet
  // navigates from one plan's shopping list to another's (same route,
  // just a new :planId param), so every bit of local state below —
  // the quick-add draft, the settle-pin tiers/timers, the reset-confirm
  // dialog, and each mutation's own isPending — would otherwise carry
  // over from the previous plan. The pin tiers in particular are keyed
  // by row key, and ingredients are shared across plans, so a pin left
  // over from plan A could mis-sort a same-ingredient row in plan B for
  // up to 600ms. Remounting resets all of it cleanly.
  return <ShoppingListBody key={planId} planId={planId!} list={list} />;
}

interface ShoppingListBodyProps {
  planId: string;
  list: ShoppingListResponse;
}

function ShoppingListBody({ planId, list }: ShoppingListBodyProps) {
  const navigate = useNavigate();

  const toggleRow = useToggleShoppingRow(planId);
  const addManualItem = useAddManualShoppingItem(planId);
  const removeManualItem = useRemoveManualShoppingItem(planId);
  const resetPurchases = useResetPurchases(planId);

  const [quickAddText, setQuickAddText] = useState('');
  const quickAddRef = useRef<HTMLInputElement>(null);
  const [confirmingReset, setConfirmingReset] = useState(false);

  // Keeps a just-toggled row from jumping to the bottom of its category
  // instantly — it stays pinned at its pre-toggle tier for a bit, so the
  // row that slides up to replace it doesn't absorb a second tap meant
  // for something else. Re-tapping the same row while it's still pinned
  // resets the timer but keeps the original pinned tier (not the tier it
  // would have right now, which is mid-flight and not what's on screen).
  const [pinnedTiers, setPinnedTiers] = useState<Map<string, number>>(new Map());
  const settleTimersRef = useRef<Map<string, number>>(new Map());

  useEffect(() => {
    const timers = settleTimersRef.current;
    return () => {
      for (const timer of timers.values()) window.clearTimeout(timer);
      timers.clear();
    };
  }, []);

  function pinRowBriefly(row: ShoppingRow) {
    setPinnedTiers((current) => {
      if (current.has(row.key)) return current;
      const next = new Map(current);
      next.set(row.key, sortTier(row));
      return next;
    });

    const existingTimer = settleTimersRef.current.get(row.key);
    if (existingTimer !== undefined) window.clearTimeout(existingTimer);

    const timer = window.setTimeout(() => {
      settleTimersRef.current.delete(row.key);
      setPinnedTiers((current) => {
        if (!current.has(row.key)) return current;
        const next = new Map(current);
        next.delete(row.key);
        return next;
      });
    }, 600);
    settleTimersRef.current.set(row.key, timer);
  }

  function handleToggle(row: ShoppingRow, checked: boolean) {
    pinRowBriefly(row);
    toggleRow.mutate({ row, checked });
  }

  function handleClearNoLongerNeeded(row: ShoppingRow) {
    // No pin here: a cleared no_longer_needed row drops out of the list
    // entirely (0 required / 0 purchased is filtered out), so there's no
    // "wrong tier" to freeze against.
    toggleRow.mutate({ row, checked: false });
  }

  function handleQuickAdd(event: FormEvent) {
    event.preventDefault();
    const text = quickAddText.trim();
    if (!text) return;

    setQuickAddText('');
    addManualItem.mutate(text, {
      // Only restore the failed text if the user hasn't already started
      // typing the next item.
      onError: () => setQuickAddText((current) => current || text),
    });
    quickAddRef.current?.focus();
  }

  function handleRemoveManual(manualItemId: string) {
    removeManualItem.mutate(manualItemId);
  }

  function handleResetConfirmed() {
    resetPurchases.mutate(undefined, {
      onSuccess: () => toast.info('Purchases reset'),
    });
  }

  const groups = buildShoppingRows(list, pinnedTiers);
  const isEmpty = groups.length === 0;
  const progress = list.totalItems > 0 ? (list.fullyPurchasedCount / list.totalItems) * 100 : 0;

  return (
    <div className="shopping-page">
      <header className="shopping-page__header">
        <div className="shopping-page__header-top">
          <SheetLink to="switch" className="shopping-page__plan-name">
            <Heading as="h1" size="4" weight="bold" className="shopping-page__plan-name-text">
              {list.mealPlanName}
            </Heading>
            <ChevronDownIcon />
          </SheetLink>

          <div className="shopping-page__header-actions">
            <DropdownMenu.Root>
              <DropdownMenu.Trigger>
                <button type="button" className="shopping-page__icon-button" aria-label="More actions">
                  <DotsVerticalIcon />
                </button>
              </DropdownMenu.Trigger>
              <DropdownMenu.Content>
                <DropdownMenu.Item onSelect={() => navigate(`/plans/${planId}`)}>View plan</DropdownMenu.Item>
                <DropdownMenu.Separator />
                <DropdownMenu.Item
                  color="red"
                  onSelect={(event) => {
                    event.preventDefault();
                    setConfirmingReset(true);
                  }}
                >
                  Reset all purchases
                </DropdownMenu.Item>
              </DropdownMenu.Content>
            </DropdownMenu.Root>
            <Link to="/account" className="shopping-page__icon-button" aria-label="Account">
              <PersonIcon />
            </Link>
          </div>
        </div>

        <div className="shopping-page__progress-row">
          <Text size="1" color="gray">
            {list.fullyPurchasedCount} of {list.totalItems}
          </Text>
          <Progress value={progress} size="1" className="shopping-page__progress-bar" />
        </div>
      </header>

      {isEmpty ? (
        <div className="shopping-page__empty">
          <Text size="4" weight="medium">
            Nothing to shop for yet
          </Text>
          <Text color="gray" size="2">
            Add recipes to this plan to build a shopping list, or add items below.
          </Text>
          <Button asChild size="3" variant="solid">
            <Link to={`/plans/${planId}/add`}>
              <PlusIcon /> Add recipes
            </Link>
          </Button>
        </div>
      ) : (
        <div className="shopping-page__body">
          {groups.map((group) => (
            <div key={group.category}>
              <div className="shopping-page__category-header">
                <Text size="2" weight="medium" className="shopping-page__category-title">
                  {categoryLabel(group.category)}
                </Text>
                <Text size="1" color="gray">
                  {group.rows.length}
                </Text>
              </div>
              <div className="shopping-page__rows">
                {group.rows.map((row) => (
                  <ShoppingRowItem
                    key={row.key}
                    row={row}
                    disabled={isTempRow(row)}
                    onToggle={(checked) => handleToggle(row, checked)}
                    onRemoveManual={row.source === 'manual' ? () => handleRemoveManual(row.manualItemId!) : undefined}
                    onClearNoLongerNeeded={() => handleClearNoLongerNeeded(row)}
                  />
                ))}
              </div>
            </div>
          ))}
        </div>
      )}

      <form onSubmit={handleQuickAdd} className="shopping-page__quick-add">
        <TextField.Root
          ref={quickAddRef}
          value={quickAddText}
          onChange={(event) => setQuickAddText(event.target.value)}
          placeholder="Add an item…"
          enterKeyHint="done"
          autoCapitalize="sentences"
          size="3"
          className="shopping-page__quick-add-input"
        />
        <Button type="submit" size="3" variant="solid" disabled={!quickAddText.trim()} aria-label="Add item">
          <PlusIcon />
        </Button>
      </form>

      <AlertDialog.Root open={confirmingReset} onOpenChange={setConfirmingReset}>
        <AlertDialog.Content maxWidth="400px">
          <AlertDialog.Title>Reset all purchases?</AlertDialog.Title>
          <AlertDialog.Description size="2">
            This clears every checked-off item on this list. It can&apos;t be undone.
          </AlertDialog.Description>
          <div className="shopping-page__alert-actions">
            <AlertDialog.Cancel>
              <Button variant="soft" size="3">
                Cancel
              </Button>
            </AlertDialog.Cancel>
            <AlertDialog.Action>
              <Button variant="solid" color="red" size="3" onClick={handleResetConfirmed} loading={resetPurchases.isPending}>
                Reset
              </Button>
            </AlertDialog.Action>
          </div>
        </AlertDialog.Content>
      </AlertDialog.Root>

      <Outlet />
    </div>
  );
}
