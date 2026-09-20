import { Link } from 'react-router-dom';
import { Button, Checkbox, IconButton, Text } from '@radix-ui/themes';
import { Cross2Icon } from '@radix-ui/react-icons';
import { formatQuantityText, type ShoppingRow } from '../../lib/shoppingRows';
import './ShoppingRowItem.css';

const MAX_VISIBLE_REFS = 3;

function RecipeRefsCaption({ row }: { row: ShoppingRow }) {
  const visible = row.recipeRefs.slice(0, MAX_VISIBLE_REFS);
  const overflow = row.recipeRefs.length - visible.length;

  return (
    <Text size="1" color="gray" as="p" className="shopping-row__recipes">
      {visible.map((ref, index) => (
        <span key={ref.id}>
          {index > 0 && ' · '}
          <Link to={`/recipes/${ref.id}`} className="shopping-row__recipe-link">
            {ref.name}
          </Link>
        </span>
      ))}
      {overflow > 0 && ` +${overflow} more`}
    </Text>
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
      {row.recipeRefs.length > 0 && <RecipeRefsCaption row={row} />}
    </div>
  );
}
