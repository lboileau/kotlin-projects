import { useEffect, useMemo, useRef, useState, type FormEvent, type KeyboardEvent } from 'react';
import { Link, Outlet, useParams } from 'react-router-dom';
import { AlertDialog, Button, Heading, Progress, Text } from '@radix-ui/themes';
import { EyeNoneIcon, EyeOpenIcon, PlusIcon, ResetIcon } from '@radix-ui/react-icons';
import { PageHeader } from '../../components/PageHeader';
import { PlanHeader } from '../../components/PlanHeader';
import { SheetLink } from '../../components/SheetLink';
import { QueryErrorState } from '../../components/QueryErrorState';
import { BottomBar } from '../../components/BottomBar';
import { ApiError } from '../../api/http';
import type { ShoppingListResponse } from '../../api/shopping';
import { usePageTitle } from '../../lib/usePageTitle';
import {
  useAddManualShoppingItem,
  useRemoveManualShoppingItem,
  useResetPurchases,
  useSetRowPurchases,
  useShoppingList,
} from '../../queries/shopping';
import { usePlans } from '../../queries/plans';
import { useIngredients } from '../../queries/ingredients';
import { buildShoppingRows, onlyStillToBuy, rowPurchasesForToggle, type ShoppingRow } from '../../lib/shoppingRows';
import { getHideBought, setHideBought } from '../../lib/hideBought';
import { clearSelectedPlanId, getSelectedPlanId, setSelectedPlanId } from '../../lib/selectedPlan';
import { useMealPlanSync } from '../../sync/useMealPlanSync';
import {
  addedToastMessage,
  buildDictationSelection,
  isMultiItem,
  quickAddItems,
  toggleExcluded,
} from '../../lib/dictationSelection';
import { toast } from '../../lib/toastStore';
import { ShoppingRowItem } from './ShoppingRowItem';
import { DictationChips } from './DictationChips';
import { PageLoader } from '../../components/PageLoader';
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

  // While the list loads, the header already shows the plan's name if the
  // plans list is in the cache (it usually is: the switcher and the Plans
  // tab both load it), so the header doesn't change as the list arrives.
  const { data: plans } = usePlans();
  const cachedPlanName = plans?.find((plan) => plan.id === planId)?.name;

  const notFound = isError && error instanceof ApiError && error.status === 404;
  // Checked the same way as `notFound` — before the "is there stale data
  // to keep showing" branch below — so a background refetch that comes
  // back 403 (e.g. the `members` sync event after the owner removes this
  // user while the page is open) wins over whatever list was cached, the
  // same way a 404 does.
  const forbidden = isError && error instanceof ApiError && error.status === 403;

  useEffect(() => {
    if (planId) setSelectedPlanId(planId);
  }, [planId]);

  useEffect(() => {
    if ((notFound || forbidden) && planId && getSelectedPlanId() === planId) clearSelectedPlanId();
  }, [notFound, forbidden, planId]);

  if (notFound) {
    return (
      <div className="shopping-page">
        <PageHeader title="Shopping" />
        <div className="shopping-page__not-found">
          <Heading as="h2" size="4" weight="medium">
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

  if (forbidden) {
    return (
      <div className="shopping-page">
        <PageHeader title="Shopping" />
        <div className="shopping-page__not-found">
          <Heading as="h2" size="4" weight="medium">
            You don&apos;t have access to this plan
          </Heading>
          <Text color="gray" size="2">
            It may have been shared with a different account, or you were removed.
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
        <PlanHeader planName={cachedPlanName} />
        <QueryErrorState message="Couldn't load the shopping list." onRetry={() => void refetch()} />
        <Outlet />
      </div>
    );
  }

  if (isLoading || !list) {
    return (
      <div className="shopping-page">
        <PlanHeader planName={cachedPlanName} />
        <PageLoader area="shopping" label="Loading shopping list" />
        <Outlet />
      </div>
    );
  }

  // Keyed on planId so every bit of local state below — the quick-add
  // draft, the reset-confirm dialog, each mutation's own isPending — starts
  // fresh for each plan. AppShell now keys the whole page by its path, which
  // includes the plan id, so switching plans already remounts this; the key
  // stays so that guarantee doesn't depend on how the shell mounts pages.
  return <ShoppingListBody key={planId} planId={planId!} list={list} />;
}

interface ShoppingListBodyProps {
  planId: string;
  list: ShoppingListResponse;
}

function ShoppingListBody({ planId, list }: ShoppingListBodyProps) {
  const setRowPurchases = useSetRowPurchases(planId);
  const addManualItem = useAddManualShoppingItem(planId);
  const removeManualItem = useRemoveManualShoppingItem(planId);
  const resetPurchases = useResetPurchases(planId);

  const [quickAddText, setQuickAddText] = useState('');
  const quickAddRef = useRef<HTMLTextAreaElement>(null);
  const [confirmingReset, setConfirmingReset] = useState(false);
  // The items just quick-added, while their rows are being pointed out.
  const [justAdded, setJustAdded] = useState<readonly string[]>([]);
  const justAddedTimerRef = useRef<number | undefined>(undefined);

  useEffect(() => () => window.clearTimeout(justAddedTimerRef.current), []);

  // "Hide bought": only what is still to buy.
  const [hideBought, setHideBoughtState] = useState(getHideBought);

  function handleHideBoughtChange(hide: boolean) {
    setHideBoughtState(hide);
    setHideBought(hide);
  }

  function handleToggle(row: ShoppingRow, checked: boolean) {
    setRowPurchases.mutate({ row, purchases: rowPurchasesForToggle(row, checked) });
  }

  function handleClearNoLongerNeeded(row: ShoppingRow) {
    setRowPurchases.mutate({ row, purchases: rowPurchasesForToggle(row, false) });
  }

  // Several items in one go. The keyboard's dictation mic can't be detected,
  // so the bar goes by what arrives instead: as soon as the text parses into
  // two or more items (dictated, pasted or typed) it shows them as chips and
  // adds them all. It has to happen here, around the field that already has
  // focus — moving focus to another field ends an iOS dictation mid-sentence.
  // The never-write rule (webapp/CLAUDE.md): `quickAddText` is only ever written from
  // the field's own onChange (verbatim) and on submit; chips never edit it.
  // No loading/error UI for the ingredients: they only sharpen the parsing.
  const { data: ingredients } = useIngredients();
  const knownNames = useMemo(() => (ingredients ?? []).map((i) => i.name), [ingredients]);
  const [excluded, setExcluded] = useState<ReadonlySet<string>>(() => new Set());
  // "Add as one item": the way out when the split is wrong ("salt and pepper").
  const [asOne, setAsOne] = useState(false);
  const selection = useMemo(
    () => buildDictationSelection(quickAddText, knownNames, excluded),
    [quickAddText, knownNames, excluded],
  );
  const multi = isMultiItem(selection, asOne);
  const itemsToAdd = quickAddItems(quickAddText, selection, asOne);

  function handleQuickAddChange(value: string) {
    setQuickAddText(value);
    // A cleared field starts over; choices made about the last text don't carry.
    if (value === '') {
      setExcluded(new Set());
      setAsOne(false);
    }
  }

  function handleQuickAdd(event: FormEvent) {
    event.preventDefault();
    if (itemsToAdd.length === 0) return;

    setQuickAddText('');
    setExcluded(new Set());
    setAsOne(false);
    setJustAdded(itemsToAdd);
    window.clearTimeout(justAddedTimerRef.current);
    justAddedTimerRef.current = window.setTimeout(() => setJustAdded([]), 1600);
    if (itemsToAdd.length === 1) {
      const text = itemsToAdd[0];
      addManualItem.mutate(text, {
        // Only restore the failed text if the user hasn't already started
        // typing the next item.
        onError: () => setQuickAddText((current) => current || text),
      });
    } else {
      // A failed item is named by the hook's own error toast; putting one of
      // several back into the field would only be confusing.
      for (const item of itemsToAdd) addManualItem.mutate(item);
      toast.info(addedToastMessage(itemsToAdd.length));
    }
    quickAddRef.current?.focus();
  }

  // The field is a textarea so a long dictation wraps instead of scrolling
  // sideways, but it still behaves like the one-line box: Enter adds.
  function handleQuickAddKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key !== 'Enter' || event.shiftKey || event.nativeEvent.isComposing) return;
    event.preventDefault();
    event.currentTarget.form?.requestSubmit();
  }

  function handleRemoveManual(manualItemId: string) {
    removeManualItem.mutate(manualItemId);
  }

  function handleResetConfirmed() {
    resetPurchases.mutate(undefined, {
      onSuccess: () => toast.info('Purchases reset'),
    });
  }

  const allGroups = buildShoppingRows(list);
  const isEmpty = allGroups.length === 0;
  const groups = hideBought ? onlyStillToBuy(allGroups) : allGroups;
  const progress = list.totalItems > 0 ? (list.fullyPurchasedCount / list.totalItems) * 100 : 0;

  return (
    <div className="shopping-page">
      <PlanHeader
        collapseTopRow
        planName={list.mealPlanName}
        actions={
          <button
            type="button"
            className="header-icon-button"
            aria-label="Reset all purchases"
            onClick={() => setConfirmingReset(true)}
          >
            <ResetIcon />
          </button>
        }
      >
        <div className="shopping-page__progress-row">
          <Text size="2" weight="medium" className="shopping-page__progress-count">
            {list.fullyPurchasedCount} of {list.totalItems}
          </Text>
          <Progress value={progress} size="2" className="shopping-page__progress-bar" />
          {!isEmpty && (
            <button
              type="button"
              className="shopping-page__hide-bought"
              aria-pressed={hideBought}
              onClick={() => handleHideBoughtChange(!hideBought)}
            >
              {hideBought ? <EyeNoneIcon aria-hidden="true" /> : <EyeOpenIcon aria-hidden="true" />}
              Hide bought
            </button>
          )}
        </div>
      </PlanHeader>

      {isEmpty ? (
        <div className="shopping-page__empty">
          <Text size="4" weight="medium">
            Nothing to shop for yet
          </Text>
          <Text color="gray" size="2">
            Add recipes to this plan to build a shopping list, or add items below.
          </Text>
          <Button asChild size="3" variant="solid">
            <SheetLink to="add">
              <PlusIcon /> Add recipes
            </SheetLink>
          </Button>
        </div>
      ) : (
        <div className="shopping-page__body">
          {groups.length === 0 && (
            <div className="shopping-page__all-bought">
              <Text size="4" weight="medium">
                Everything is bought
              </Text>
              <Button size="3" variant="soft" onClick={() => handleHideBoughtChange(false)}>
                Show bought items
              </Button>
            </div>
          )}
          {groups.map((group) => (
            <div key={group.category}>
              <div className="shopping-page__category-header">
                <Text size="2" weight="medium" className="shopping-page__category-title">
                  {categoryLabel(group.category)}
                </Text>
              </div>
              <div className="shopping-page__rows">
                {group.rows.map((row) => (
                  <ShoppingRowItem
                    key={row.key}
                    row={row}
                    disabled={isTempRow(row)}
                    highlight={row.source === 'manual' && row.description !== null && justAdded.includes(row.description)}
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

      <BottomBar>
        {multi && (
          <div className="shopping-page__quick-add-multi">
            <DictationChips
              chips={selection.chips}
              onToggle={(key) => setExcluded((current) => toggleExcluded(current, key))}
            />
            <button
              type="button"
              className="shopping-page__quick-add-as-one"
              // Keeps focus and the keyboard in the field, as the chips do.
              onPointerDown={(event) => event.preventDefault()}
              onClick={() => setAsOne(true)}
            >
              Add as one item instead
            </button>
          </div>
        )}
        <form onSubmit={handleQuickAdd} className="shopping-page__quick-add">
          <textarea
            ref={quickAddRef}
            value={quickAddText}
            onChange={(event) => handleQuickAddChange(event.target.value)}
            onKeyDown={handleQuickAddKeyDown}
            placeholder="Add an item, or a few…"
            aria-label="Add an item"
            rows={1}
            enterKeyHint="done"
            autoCapitalize="sentences"
            className="shopping-page__quick-add-input"
          />
          {multi ? (
            <Button type="submit" size="3" variant="solid" disabled={itemsToAdd.length === 0}>
              {selection.count === 0 ? 'Add' : `Add ${selection.count}`}
            </Button>
          ) : (
            <Button type="submit" size="3" variant="solid" disabled={itemsToAdd.length === 0} aria-label="Add item">
              <PlusIcon />
            </Button>
          )}
        </form>
      </BottomBar>

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
