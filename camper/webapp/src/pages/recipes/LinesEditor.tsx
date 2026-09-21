import { useEffect, useRef, useState, type KeyboardEvent } from 'react';
import { IconButton, Select, Text, TextField } from '@radix-ui/themes';
import { PlusIcon, TrashIcon } from '@radix-ui/react-icons';
import { IngredientPicker } from '../../components/IngredientPicker';
import { RowActionButton } from '../../components/RowActionButton';
import { UNITS, capitalize } from '../../lib/ingredientConstants';
import { parseQuantity } from '../../lib/parseQuantity';
import type { IngredientResponse } from '../../api/ingredients';
import './LinesEditor.css';

export interface DraftLine {
  clientId: string;
  /** The saved line this row came from, when editing an existing recipe; absent for a line added in the form. */
  lineId?: string;
  ingredient: IngredientResponse;
  /** Kept as raw text while editing — see `parseQuantity` for the accepted formats. */
  quantity: string;
  unit: string;
}

/** What is sitting in the add row, not yet added as a line. */
export interface PendingLine {
  ingredient: IngredientResponse;
  quantity: string;
  unit: string;
}

interface LinesEditorProps {
  lines: DraftLine[];
  onChange: (lines: DraftLine[]) => void;
  /**
   * Reports the add row's contents (null when no ingredient is picked), so
   * the form can include a line the user typed but never pressed + on when
   * they save, instead of silently dropping it.
   */
  onPendingChange?: (pending: PendingLine | null) => void;
}

function isValidQuantity(quantity: string): boolean {
  return parseQuantity(quantity) !== null;
}

/**
 * Local-state ingredient lines editor for the recipe form, new and edit
 * alike (`RecipeFormFields`). Lines are held in memory and nothing is sent
 * until the form's Save: a new recipe sends them in its create request, an
 * edit works out which were added, changed and removed (`EditRecipePage`).
 */
export function LinesEditor({ lines, onChange, onPendingChange }: LinesEditorProps) {
  const [pickerKey, setPickerKey] = useState(0);
  const [selected, setSelected] = useState<IngredientResponse | null>(null);
  const [quantity, setQuantity] = useState('');
  const [unit, setUnit] = useState<string>(UNITS[0]);
  const quantityRef = useRef<HTMLInputElement>(null);

  const canAdd = selected !== null && isValidQuantity(quantity);

  useEffect(() => {
    onPendingChange?.(selected ? { ingredient: selected, quantity, unit } : null);
    // The callback's identity is the parent's business; only the row's contents matter here.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selected, quantity, unit]);

  function handleSelect(ingredient: IngredientResponse) {
    setSelected(ingredient);
    setUnit(ingredient.defaultUnit);
    quantityRef.current?.focus();
  }

  function handleAdd() {
    if (!selected || !isValidQuantity(quantity)) return;
    onChange([...lines, { clientId: crypto.randomUUID(), ingredient: selected, quantity, unit }]);
    setSelected(null);
    setQuantity('');
    setUnit(UNITS[0]);
    setPickerKey((k) => k + 1);
  }

  function handleQuantityKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'Enter') {
      event.preventDefault();
      handleAdd();
    }
  }

  function updateLine(clientId: string, patch: Partial<Pick<DraftLine, 'quantity' | 'unit'>>) {
    onChange(lines.map((line) => (line.clientId === clientId ? { ...line, ...patch } : line)));
  }

  function removeLine(clientId: string) {
    onChange(lines.filter((line) => line.clientId !== clientId));
  }

  return (
    <div className="lines-editor">
      {lines.length > 0 && (
        <ul className="lines-editor__list">
          {lines.map((line) => (
            <li key={line.clientId} className="lines-editor__row">
              <div className="lines-editor__row-info">
                <Text as="span" size="2" weight="medium" className="lines-editor__row-name">
                  {line.ingredient.name}
                </Text>
                <Text as="span" size="1" color="gray">
                  {capitalize(line.ingredient.category)}
                </Text>
              </div>
              <TextField.Root
                inputMode="text"
                placeholder="1 1/2"
                size="2"
                className="lines-editor__row-quantity"
                aria-label={`Quantity for ${line.ingredient.name}`}
                value={line.quantity}
                color={isValidQuantity(line.quantity) ? undefined : 'red'}
                autoCapitalize="off"
                autoCorrect="off"
                spellCheck={false}
                autoComplete="off"
                enterKeyHint="done"
                onChange={(event) => updateLine(line.clientId, { quantity: event.target.value })}
              />
              <Select.Root
                value={line.unit}
                onValueChange={(value) => updateLine(line.clientId, { unit: value })}
                size="2"
              >
                <Select.Trigger aria-label={`Unit for ${line.ingredient.name}`} />
                <Select.Content>
                  {UNITS.map((u) => (
                    <Select.Item key={u} value={u}>
                      {u}
                    </Select.Item>
                  ))}
                </Select.Content>
              </Select.Root>
              <RowActionButton
                quiet
                color="red"
                aria-label={`Remove ${line.ingredient.name}`}
                onClick={() => removeLine(line.clientId)}
              >
                <TrashIcon />
              </RowActionButton>
            </li>
          ))}
        </ul>
      )}

      <div className="lines-editor__add-row">
        <IngredientPicker
          key={pickerKey}
          value={selected}
          onSelect={handleSelect}
          placeholder="Add ingredient"
          // Focus the fresh picker after an Add (pickerKey > 0 means this is a
          // remount, not the editor's initial one) so entry continues back to
          // back without a tap; the remount and this prop land in the same
          // commit as handleAdd's state updates, so the keyboard stays up.
          autoFocus={pickerKey > 0}
        />
        <div className="lines-editor__add-fields">
          <TextField.Root
            ref={quantityRef}
            inputMode="text"
            size="3"
            className="lines-editor__add-quantity"
            placeholder="1 1/2"
            aria-label="Quantity"
            value={quantity}
            autoCapitalize="off"
            autoCorrect="off"
            spellCheck={false}
            autoComplete="off"
            enterKeyHint="done"
            onChange={(event) => setQuantity(event.target.value)}
            onKeyDown={handleQuantityKeyDown}
          />
          <Select.Root value={unit} onValueChange={setUnit} size="3">
            <Select.Trigger aria-label="Unit" />
            <Select.Content>
              {UNITS.map((u) => (
                <Select.Item key={u} value={u}>
                  {u}
                </Select.Item>
              ))}
            </Select.Content>
          </Select.Root>
          <IconButton
            type="button"
            aria-label="Add ingredient line"
            className="lines-editor__icon-button"
            disabled={!canAdd}
            onClick={handleAdd}
          >
            <PlusIcon />
          </IconButton>
        </div>
      </div>
    </div>
  );
}
