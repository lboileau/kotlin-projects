import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Button, Checkbox, IconButton } from '@radix-ui/themes';
import { ChevronRightIcon, Cross2Icon, InfoCircledIcon } from '@radix-ui/react-icons';
import { formatQuantityText, type ShoppingRow } from '../../lib/shoppingRows';
import './ShoppingRowItem.css';

function RecipeRefsCaption({ row }: { row: ShoppingRow }) {
  // Each recipe is its own pill-shaped link: clearly tappable on a phone,
  // and all of them are shown (they wrap) since the list is opt-in now.
  return (
    <div className="shopping-row__recipes">
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
}

/**
 * A single merged shopping-list row. The whole checkbox+text area (a
 * real <label>+Checkbox pair, so a click anywhere in it toggles via
 * native label activation — no manual click handling, no double-fire
 * risk) is the hit target; the recipe-refs caption and the remove
 * button are outside that label and never trigger the toggle.
 */
export function ShoppingRowItem({ row, onToggle, onRemoveManual, onClearNoLongerNeeded, disabled }: ShoppingRowItemProps) {
  const name = row.ingredientName ?? row.description ?? 'Item';
  const quantityText = formatQuantityText(row);
  const checked = row.overallStatus === 'done';
  const noLongerNeeded = row.overallStatus === 'no_longer_needed';
  // The recipes an item belongs to stay hidden until the info icon is tapped.
  const [showRecipes, setShowRecipes] = useState(false);
  const hasRecipes = row.recipeRefs.length > 0;

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
    <div className={`shopping-row${checked ? ' shopping-row--checked' : ''}`}>
      <div className="shopping-row__main">
        <label className="shopping-row__hit-area">
          <Checkbox
            checked={checked}
            disabled={disabled}
            onCheckedChange={(value) => onToggle(value === true)}
          />
          <span className="shopping-row__text">
            <span className="shopping-row__name">{name}</span>
            {quantityText && <span className="shopping-row__quantity">{quantityText}</span>}
          </span>
        </label>
        {hasRecipes && (
          <IconButton
            type="button"
            variant="ghost"
            color="gray"
            size="3"
            className="shopping-row__info"
            aria-label={`${showRecipes ? 'Hide' : 'Show'} recipes for ${name}`}
            aria-expanded={showRecipes}
            onClick={() => setShowRecipes((current) => !current)}
          >
            <InfoCircledIcon />
          </IconButton>
        )}
        {onRemoveManual && (
          <IconButton
            type="button"
            variant="ghost"
            color="red"
            size="3"
            aria-label={`Remove ${name}`}
            disabled={disabled}
            onClick={onRemoveManual}
          >
            <Cross2Icon />
          </IconButton>
        )}
      </div>
      {hasRecipes && showRecipes && <RecipeRefsCaption row={row} />}
    </div>
  );
}
