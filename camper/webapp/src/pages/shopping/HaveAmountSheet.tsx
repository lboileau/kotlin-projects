import { useEffect, useState, type FormEvent } from 'react';
import { useParams } from 'react-router-dom';
import { Button, Spinner, Text, TextField } from '@radix-ui/themes';
import { Sheet } from '../../components/Sheet';
import { useSheet } from '../../components/useSheet';
import { useSetRowPurchases, useShoppingList } from '../../queries/shopping';
import { buildShoppingRows, haveSheetEntries, type RowPurchase } from '../../lib/shoppingRows';
import { formatQuantity } from '../../lib/formatQuantity';
import { parseQuantityOrZero } from '../../lib/parseQuantity';
import './HaveAmountSheet.css';

/**
 * "I already have some of this": sets how much of a shopping row is bought
 * or at home, instead of ticking the whole row off. Opened by tapping a
 * row's quantity. The list then shows what is left to buy ("1 clove more ·
 * have 2"), exactly as it does when a plan grows after shopping started.
 */
export function HaveAmountSheet() {
  const { planId, ingredientId } = useParams<{ planId: string; ingredientId: string }>();
  const sheet = useSheet(`/plans/${planId}/shopping`);
  const { data: list } = useShoppingList(planId);
  const setRowPurchases = useSetRowPurchases(planId ?? '');

  const row = list
    ? buildShoppingRows(list)
        .flatMap((group) => group.rows)
        .find((candidate) => candidate.key === `ingredient-${ingredientId}`)
    : undefined;
  const entries = row ? haveSheetEntries(row) : [];

  // Only the fields that were typed into, by entry index. An untouched field
  // keeps the amount it had: what it displays is rounded for reading ("⅓"),
  // and sending that back would quietly change the stored amount.
  const [typed, setTyped] = useState<Record<number, string>>({});
  const [showErrors, setShowErrors] = useState(false);

  const missing = !!list && !row;
  useEffect(() => {
    // The plan changed under the sheet and the ingredient is no longer on
    // the list (or the link was wrong). `sheet.close` is a plain function
    // recreated every render, not a stable dependency.
    if (missing) sheet.close();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [missing]);

  function amountFor(index: number): number | null {
    const text = typed[index];
    return text === undefined ? entries[index].quantityPurchased : parseQuantityOrZero(text);
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (!row) return;

    const purchases: RowPurchase[] = [];
    for (let index = 0; index < entries.length; index += 1) {
      const quantityPurchased = amountFor(index);
      if (quantityPurchased === null) {
        setShowErrors(true);
        return;
      }
      if (typed[index] !== undefined) purchases.push({ entry: entries[index], quantityPurchased });
    }

    // Optimistic, like a check-off: the list updates at once and the hook
    // reports a failure itself, so there is nothing to wait for here.
    if (purchases.length > 0) setRowPurchases.mutate({ row, purchases });
    sheet.close();
  }

  return (
    <Sheet {...sheet.sheetProps} title={row?.ingredientName ?? 'How much do you have?'}>
      {!row ? (
        <div className="have-amount-sheet__loading">
          <Spinner size="3" />
        </div>
      ) : (
        <form onSubmit={handleSubmit} className="have-amount-sheet__form">
          {entries.map((entry, index) => {
            const invalid = showErrors && amountFor(index) === null;
            const needed = `${formatQuantity(entry.quantityRequired)}${entry.unit ? ` ${entry.unit}` : ''}`;
            return (
              <label key={entry.unit ?? ''} className="have-amount-sheet__field">
                <Text as="span" size="2" weight="medium">
                  I have
                </Text>
                <TextField.Root
                  value={typed[index] ?? (entry.quantityPurchased > 0 ? formatQuantity(entry.quantityPurchased) : '')}
                  onChange={(event) => setTyped((current) => ({ ...current, [index]: event.target.value }))}
                  // Typing replaces the old amount instead of adding to it.
                  onFocus={(event) => event.target.select()}
                  placeholder="0"
                  size="3"
                  inputMode="text"
                  enterKeyHint="done"
                  autoComplete="off"
                  autoFocus={index === 0}
                  color={invalid ? 'red' : undefined}
                  aria-invalid={invalid || undefined}
                >
                  {entry.unit && <TextField.Slot side="right">{entry.unit}</TextField.Slot>}
                </TextField.Root>
                {invalid ? (
                  <Text as="span" size="1" color="red">
                    Enter an amount like 2, 1.5 or 1/2.
                  </Text>
                ) : (
                  <Text as="span" size="1" color="gray">
                    {entry.quantityRequired > 0 ? `The plan needs ${needed}.` : 'The plan no longer needs this.'}
                  </Text>
                )}
              </label>
            );
          })}

          <Button type="submit" size="3" variant="solid" className="have-amount-sheet__submit">
            Save
          </Button>
        </form>
      )}
    </Sheet>
  );
}
