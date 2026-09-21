import { useEffect, useId, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { Button, Checkbox } from '@radix-ui/themes';
import { RowActionButton } from '../../components/RowActionButton';
import { SheetLink } from '../../components/SheetLink';
import { ChevronRightIcon, Cross2Icon, InfoCircledIcon } from '@radix-ui/react-icons';
import { revealInContainer } from '../../lib/scrollIntoContainer';
import { formatQuantityText, formatStillNeededText, type ShoppingRow } from '../../lib/shoppingRows';
import './ShoppingRowItem.css';

function RecipeRefsCaption({ row, id }: { row: ShoppingRow; id: string }) {
  // Each recipe is its own pill-shaped link: clearly tappable on a phone,
  // and all of them are shown (they wrap) since the list is opt-in now.
  return (
    <div className="shopping-row__recipes" id={id}>
      {row.recipeRefs.map((ref) => (
        <Link key={ref.id} to={`/recipes/${ref.id}`} className="shopping-row__recipe-link">
          <span className="shopping-row__recipe-link-text">{ref.name}</span>
          <ChevronRightIcon aria-hidden="true" />
        </Link>
      ))}
    </div>
  );
}

interface ShoppingRowItemProps {
  row: ShoppingRow;
  onToggle: (checked: boolean) => void;
  onRemoveManual?: () => void;
  onClearNoLongerNeeded?: () => void;
  /** True while this row's own temp insert hasn't settled yet, or a mutation touching it is in flight. */
  disabled?: boolean;
  /** Just quick-added: scroll it into view and flash it, so it is clear where the item went. */
  highlight?: boolean;
}

/**
 * A single merged shopping-list row. The whole checkbox+text area (a
 * real <label>+Checkbox pair, so a click anywhere in it toggles via
 * native label activation — no manual click handling, no double-fire
 * risk) is the hit target; the recipe-refs caption and the remove
 * button are outside that label and never trigger the toggle.
 *
 * The quantity sits right-justified in a column of its own, also outside the
 * label: on an ingredient row it is a link to the "have" sheet
 * (`HaveAmountSheet`), as tall as the row, so it is an easy target that can
 * never tick the row by mistake.
 */
export function ShoppingRowItem({
  row,
  onToggle,
  onRemoveManual,
  onClearNoLongerNeeded,
  disabled,
  highlight,
}: ShoppingRowItemProps) {
  const name = row.ingredientName ?? row.description ?? 'Item';
  const quantityText = formatQuantityText(row);
  const checked = row.overallStatus === 'done';
  // Bought, then the plan grew: shown as half-ticked with what is still to
  // buy, rather than quietly going back to looking never bought. Tapping it
  // ticks it fully, as for any other row.
  const stillNeededText = row.overallStatus === 'more_needed' ? formatStillNeededText(row) : '';
  const moreNeeded = stillNeededText !== '';
  const noLongerNeeded = row.overallStatus === 'no_longer_needed';
  // The recipes an item belongs to stay hidden until the info icon is tapped.
  const [showRecipes, setShowRecipes] = useState(false);
  const hasRecipes = row.recipeRefs.length > 0;
  const recipesId = useId();
  const rowRef = useRef<HTMLDivElement>(null);
  // Manual items have no quantity to have part of.
  const haveTo = row.source !== 'manual' && row.ingredientId && !disabled ? `have/${row.ingredientId}` : null;
  // "1 clove more · have 2 clove" goes on two lines in the narrow column.
  const quantityLines = (moreNeeded ? stillNeededText : quantityText).split(' · ').filter(Boolean);
  const quantityClass = `shopping-row__quantity${moreNeeded ? ' shopping-row__quantity--more' : ''}`;
  const quantityContent = quantityLines.map((line) => <span key={line}>{line}</span>);

  useEffect(() => {
    if (highlight && rowRef.current) revealInContainer(rowRef.current);
  }, [highlight]);

  if (noLongerNeeded) {
    return (
      <div className="shopping-row shopping-row--no-longer-needed">
        <span className="shopping-row__text">
          <span className="shopping-row__name">{name}</span>
          {quantityText && <span className="shopping-row__quantity">{quantityText}</span>}
        </span>
        <Button variant="soft" size="1" onClick={onClearNoLongerNeeded}>
          Clear
        </Button>
      </div>
    );
  }

  return (
    <div
      ref={rowRef}
      className={`shopping-row${checked ? ' shopping-row--checked' : ''}${highlight ? ' shopping-row--highlight' : ''}`}
    >
      <div className="shopping-row__main">
        <label className="shopping-row__hit-area">
          <Checkbox
            checked={moreNeeded ? 'indeterminate' : checked}
            disabled={disabled}
            onCheckedChange={(value) => onToggle(value === true)}
          />
          <span className="shopping-row__text">
            <span className="shopping-row__name">{name}</span>
          </span>
        </label>
        {quantityLines.length > 0 &&
          (haveTo ? (
            <SheetLink
              to={haveTo}
              className={`${quantityClass} shopping-row__quantity--link`}
              aria-label={`${quantityLines.join(', ')}. Set how much ${name} you have`}
            >
              {quantityContent}
            </SheetLink>
          ) : (
            <span className={quantityClass}>{quantityContent}</span>
          ))}
        {hasRecipes && (
          <RowActionButton
            quiet
            aria-label={`${showRecipes ? 'Hide' : 'Show'} recipes for ${name}`}
            aria-expanded={showRecipes}
            aria-controls={showRecipes ? recipesId : undefined}
            onClick={() => setShowRecipes((current) => !current)}
          >
            <InfoCircledIcon />
          </RowActionButton>
        )}
        {onRemoveManual && (
          <RowActionButton
            quiet
            color="red"
            aria-label={`Remove ${name}`}
            disabled={disabled}
            onClick={onRemoveManual}
          >
            <Cross2Icon />
          </RowActionButton>
        )}
      </div>
      {hasRecipes && showRecipes && <RecipeRefsCaption row={row} id={recipesId} />}
    </div>
  );
}
