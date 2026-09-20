import { useState, type FormEvent } from 'react';
import { useLocation, useParams } from 'react-router-dom';
import { Button, Callout, Select, Text, TextField } from '@radix-ui/themes';
import { ExclamationTriangleIcon } from '@radix-ui/react-icons';
import { Sheet } from '../../components/Sheet';
import { useSheet } from '../../components/useSheet';
import { IngredientPicker } from '../../components/IngredientPicker';
import { useAddRecipeIngredient } from '../../queries/recipes';
import { UNITS } from '../../lib/ingredientConstants';
import { lineSheetParentPath } from '../../lib/lineSheetParentPath';
import { parseQuantity } from '../../lib/parseQuantity';
import type { IngredientResponse } from '../../api/ingredients';
import './LineSheet.css';

export function AddLineSheet() {
  const { recipeId } = useParams<{ recipeId: string }>();
  const location = useLocation();
  const sheet = useSheet(lineSheetParentPath(location.pathname));
  const addLine = useAddRecipeIngredient(recipeId ?? '');

  const [ingredient, setIngredient] = useState<IngredientResponse | null>(null);
  const [quantity, setQuantity] = useState('');
  const [unit, setUnit] = useState<string>(UNITS[0]);
  const [error, setError] = useState<string | null>(null);

  function handleSelect(picked: IngredientResponse) {
    setIngredient(picked);
    setUnit(picked.defaultUnit);
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (!recipeId) return;
    if (!ingredient) {
      setError('Choose an ingredient.');
      return;
    }
    const parsedQuantity = parseQuantity(quantity);
    if (parsedQuantity === null) {
      setError('Enter a valid quantity (e.g. "1", "1.5", or "1 1/2").');
      return;
    }
    setError(null);
    await addLine.mutateAsync({ ingredientId: ingredient.id, quantity: parsedQuantity, unit });
    sheet.close();
  }

  return (
    <Sheet {...sheet.sheetProps} title="Add ingredient">
      <form className="line-sheet__form" onSubmit={handleSubmit}>
        <IngredientPicker value={ingredient} onSelect={handleSelect} autoFocus placeholder="Search ingredients" />

        <div className="line-sheet__fields">
          <Text as="label" size="2" weight="medium" className="line-sheet__field">
            Quantity
            <TextField.Root
              inputMode="text"
              placeholder="1 1/2"
              size="3"
              value={quantity}
              autoCapitalize="off"
              autoCorrect="off"
              spellCheck={false}
              autoComplete="off"
              enterKeyHint="done"
              onChange={(event) => setQuantity(event.target.value)}
            />
          </Text>
          <Text as="label" size="2" weight="medium" className="line-sheet__field">
            Unit
            <Select.Root value={unit} onValueChange={setUnit} size="3">
              <Select.Trigger />
              <Select.Content>
                {UNITS.map((u) => (
                  <Select.Item key={u} value={u}>
                    {u}
                  </Select.Item>
                ))}
              </Select.Content>
            </Select.Root>
          </Text>
        </div>

        {error && (
          <Callout.Root color="red" variant="surface" size="1" role="alert">
            <Callout.Icon>
              <ExclamationTriangleIcon />
            </Callout.Icon>
            <Callout.Text>{error}</Callout.Text>
          </Callout.Root>
        )}

        <Button type="submit" size="3" loading={addLine.isPending}>
          Add
        </Button>
      </form>
    </Sheet>
  );
}
