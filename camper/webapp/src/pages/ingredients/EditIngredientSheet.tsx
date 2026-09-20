import { useId, useState, type FormEvent } from 'react';
import { useParams } from 'react-router-dom';
import { Button, Callout, Select, Separator, Spinner, Text, TextField } from '@radix-ui/themes';
import { ExclamationTriangleIcon } from '@radix-ui/react-icons';
import { Sheet } from '../../components/Sheet';
import { SheetSelectContent } from '../../components/SheetSelectContent';
import { useSheet, type UseSheetResult } from '../../components/useSheet';
import { useDeleteIngredient, useIngredients, useUpdateIngredient } from '../../queries/ingredients';
import { ApiError } from '../../api/http';
import { CATEGORIES, UNITS, capitalize } from '../../lib/ingredientConstants';
import { toast } from '../../lib/toastStore';
import type { IngredientResponse } from '../../api/ingredients';
import './EditIngredientSheet.css';

export function EditIngredientSheet() {
  const { ingredientId } = useParams<{ ingredientId: string }>();
  const sheet = useSheet('/ingredients');
  const { data: ingredients, isLoading } = useIngredients();
  const ingredient = ingredients?.find((i) => i.id === ingredientId);

  if (isLoading) {
    return (
      <Sheet {...sheet.sheetProps} title="Edit ingredient">
        <Spinner />
      </Sheet>
    );
  }

  if (!ingredient) {
    return (
      <Sheet {...sheet.sheetProps} title="Edit ingredient">
        <Text as="p" size="2" color="gray">
          This ingredient couldn&apos;t be found. It may have been deleted.
        </Text>
      </Sheet>
    );
  }

  // Keyed on the ingredient id so the form seeds fresh from `ingredient`
  // once via useState's lazy initializer, rather than syncing with an effect.
  return <EditIngredientForm key={ingredient.id} ingredient={ingredient} sheet={sheet} />;
}

function EditIngredientForm({
  ingredient,
  sheet,
}: {
  ingredient: IngredientResponse;
  sheet: UseSheetResult;
}) {
  const updateIngredient = useUpdateIngredient(ingredient.id);
  const deleteIngredient = useDeleteIngredient();

  const [name, setName] = useState(ingredient.name);
  const [category, setCategory] = useState(ingredient.category);
  const [unit, setUnit] = useState(ingredient.defaultUnit);
  const [error, setError] = useState<string | null>(null);
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const errorId = useId();

  async function handleSave(event: FormEvent) {
    event.preventDefault();
    const trimmed = name.trim();
    if (!trimmed) {
      setError('Enter a name.');
      return;
    }
    setError(null);
    try {
      await updateIngredient.mutateAsync({ name: trimmed, category, defaultUnit: unit });
      toast.info('Ingredient updated.');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not update this ingredient.');
    }
  }

  async function handleDelete() {
    await deleteIngredient.mutateAsync(ingredient.id);
    toast.info('Ingredient deleted.');
    sheet.close();
  }

  return (
    <Sheet {...sheet.sheetProps} title="Edit ingredient">
      <form className="edit-ingredient-sheet__form" onSubmit={handleSave}>
        <Text as="label" size="2" weight="medium" className="edit-ingredient-sheet__field">
          Name
          <TextField.Root
            size="3"
            value={name}
            onChange={(event) => setName(event.target.value)}
            autoFocus
            autoCapitalize="words"
            enterKeyHint="done"
            aria-invalid={Boolean(error)}
            aria-describedby={error ? errorId : undefined}
          />
        </Text>

        <div className="edit-ingredient-sheet__row">
          <Text as="label" size="2" weight="medium" className="edit-ingredient-sheet__field">
            Category
            <Select.Root value={category} onValueChange={setCategory} size="3">
              <Select.Trigger />
              <SheetSelectContent>
                {CATEGORIES.map((c) => (
                  <Select.Item key={c} value={c}>
                    {capitalize(c)}
                  </Select.Item>
                ))}
              </SheetSelectContent>
            </Select.Root>
          </Text>
          <Text as="label" size="2" weight="medium" className="edit-ingredient-sheet__field">
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
          <Callout.Root id={errorId} color="red" variant="surface" size="1" role="alert">
            <Callout.Icon>
              <ExclamationTriangleIcon />
            </Callout.Icon>
            <Callout.Text>{error}</Callout.Text>
          </Callout.Root>
        )}

        <Button type="submit" size="3" loading={updateIngredient.isPending}>
          Save
        </Button>
      </form>

      <Separator my="4" size="4" />

      {!confirmingDelete ? (
        <Button variant="soft" color="red" size="3" onClick={() => setConfirmingDelete(true)}>
          Delete ingredient
        </Button>
      ) : (
        <div className="edit-ingredient-sheet__confirm-delete">
          <Callout.Root color="red" variant="surface" size="1" role="alert">
            <Callout.Icon>
              <ExclamationTriangleIcon />
            </Callout.Icon>
            <Callout.Text>
              Recipes that use this ingredient will go back to draft and need review, for everyone. This can&apos;t be
              undone.
            </Callout.Text>
          </Callout.Root>
          <div className="edit-ingredient-sheet__confirm-actions">
            <Button size="3" variant="soft" onClick={() => setConfirmingDelete(false)} disabled={deleteIngredient.isPending}>
              Cancel
            </Button>
            <Button size="3" color="red" onClick={handleDelete} loading={deleteIngredient.isPending}>
              Yes, delete
            </Button>
          </div>
        </div>
      )}
    </Sheet>
  );
}
