import { useState, type FormEvent } from 'react';
import { useLocation, useParams } from 'react-router-dom';
import { Button, Callout, Select, Spinner, Text, TextField } from '@radix-ui/themes';
import { ExclamationTriangleIcon } from '@radix-ui/react-icons';
import { Sheet } from '../../components/Sheet';
import { SheetSelectContent } from '../../components/SheetSelectContent';
import { useSheet, type UseSheetResult } from '../../components/useSheet';
import { IngredientPicker } from '../../components/IngredientPicker';
import { useRecipe, useRemoveRecipeIngredient, useResolveRecipeIngredient } from '../../queries/recipes';
import { UNITS, normalizeCategory, normalizeUnit } from '../../lib/ingredientConstants';
import { lineSheetParentPath } from '../../lib/lineSheetParentPath';
import { parseQuantity } from '../../lib/parseQuantity';
import type { IngredientResponse } from '../../api/ingredients';
import type { RecipeIngredientResponse } from '../../api/recipes';
import './LineSheet.css';

export function EditLineSheet() {
  const { recipeId, lineId } = useParams<{ recipeId: string; lineId: string }>();
  const location = useLocation();
  const sheet = useSheet(lineSheetParentPath(location.pathname));
  const { data: recipe, isLoading } = useRecipe(recipeId);

  const line = recipe?.ingredients.find((l) => l.id === lineId);

  if (isLoading) {
    return (
      <Sheet {...sheet.sheetProps} title="Edit ingredient">
        <Spinner />
      </Sheet>
    );
  }

  if (!recipeId || !line) {
    return (
      <Sheet {...sheet.sheetProps} title="Edit ingredient">
        <Text as="p" size="2" color="gray">
          This ingredient line is no longer on the recipe.
        </Text>
      </Sheet>
    );
  }

  // Keyed on the line id so the form's local state below is seeded
  // fresh from `line` exactly once via useState's lazy initializer,
  // rather than synced from a prop with an effect.
  return <EditLineForm key={line.id} recipeId={recipeId} line={line} sheet={sheet} />;
}

function EditLineForm({
  recipeId,
  line,
  sheet,
}: {
  recipeId: string;
  line: RecipeIngredientResponse;
  sheet: UseSheetResult;
}) {
  const resolveLine = useResolveRecipeIngredient(recipeId);
  const removeLine = useRemoveRecipeIngredient(recipeId);

  const initialIngredient = line.ingredient ?? line.matchedIngredient ?? null;
  const [ingredient, setIngredient] = useState<IngredientResponse | null>(initialIngredient);
  const [quantity, setQuantity] = useState(String(line.quantity));
  const [unit, setUnit] = useState(line.unit);
  const [error, setError] = useState<string | null>(null);

  const initialQuery = initialIngredient?.name ?? line.suggestedIngredientName ?? line.originalText ?? '';

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (!ingredient) {
      setError('Pick an ingredient from the list, or create it.');
      return;
    }
    const parsedQuantity = parseQuantity(quantity);
    if (parsedQuantity === null) {
      setError('Enter a valid quantity (e.g. "2" or "1.5").');
      return;
    }
    setError(null);
    const keepingMatch = line.status === 'pending_review' && line.matchedIngredient?.id === ingredient.id;
    await resolveLine.mutateAsync({
      lineId: line.id,
      payload: {
        action: keepingMatch ? 'CONFIRM_MATCH' : 'SELECT_EXISTING',
        ingredientId: ingredient.id,
        quantity: parsedQuantity,
        unit,
      },
    });
    sheet.close();
  }

  async function handleRemove() {
    await removeLine.mutateAsync(line.id);
    sheet.close();
  }

  return (
    <Sheet {...sheet.sheetProps} title="Edit ingredient">
      {line.status === 'pending_review' && line.originalText && (
        <Callout.Root color="amber" variant="surface" size="1" className="line-sheet__scraped">
          <Callout.Text>Scraped as &ldquo;{line.originalText}&rdquo;.</Callout.Text>
        </Callout.Root>
      )}

      <form className="line-sheet__form" onSubmit={handleSubmit}>
        <IngredientPicker
          value={ingredient}
          onSelect={setIngredient}
          onClear={() => setIngredient(null)}
          initialQuery={initialQuery}
          // Only an import's suggestion pre-fills the create panel's category; normalising
          // a missing one would yield 'other' and defeat "category is required, no default".
          suggestedCategory={line.suggestedCategory ? normalizeCategory(line.suggestedCategory) : null}
          suggestedUnit={normalizeUnit(line.suggestedUnit)}
          autoFocus
        />

        <div className="line-sheet__fields">
          <Text as="label" size="2" weight="medium" className="line-sheet__field">
            Quantity
            <TextField.Root
              inputMode="decimal"
              placeholder="1.5"
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
              <SheetSelectContent>
                {UNITS.map((u) => (
                  <Select.Item key={u} value={u}>
                    {u}
                  </Select.Item>
                ))}
              </SheetSelectContent>
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

        <div className="line-sheet__actions">
          <Button type="submit" size="3" loading={resolveLine.isPending}>
            Save
          </Button>
          <Button type="button" variant="soft" color="red" size="3" onClick={handleRemove} loading={removeLine.isPending}>
            Remove
          </Button>
        </div>
      </form>
    </Sheet>
  );
}
