import { useState, type FormEvent } from 'react';
import { useParams } from 'react-router-dom';
import { Button, Callout, Skeleton, Text } from '@radix-ui/themes';
import { ExclamationTriangleIcon } from '@radix-ui/react-icons';
import { PageHeader } from '../../components/PageHeader';
import { useBack } from '../../components/useBack';
import { QueryErrorState } from '../../components/QueryErrorState';
import { useRecipe, useSaveRecipeEdits, type RecipeEdits } from '../../queries/recipes';
import { parseQuantity } from '../../lib/parseQuantity';
import { enterMovesOn } from '../../lib/enterMovesOn';
import { toast } from '../../lib/toastStore';
import { ApiError } from '../../api/http';
import type { RecipeDetailResponse, UpdateRecipeRequest } from '../../api/recipes';
import type { DraftLine, PendingLine } from './LinesEditor';
import { RecipeFormFields } from './RecipeFormFields';
import { validateRecipeForm, type RecipeFormValues } from './recipeForm';
import './RecipeForm.css';

/**
 * description/meal/theme: `undefined` means "omit — unchanged", a string
 * (including `""`) means "send this — the user changed it". Comparing
 * against the ORIGINAL server value (not e.g. re-trimming an already-empty
 * string) is what lets clearing a previously-set value send `""` rather
 * than being indistinguishable from "never touched it".
 */
function fieldPatch(original: string | null, current: string): string | undefined {
  const originalValue = original ?? '';
  const trimmedCurrent = current.trim();
  return trimmedCurrent === originalValue ? undefined : trimmedCurrent;
}

export function EditRecipePage() {
  const { recipeId } = useParams<{ recipeId: string }>();
  const { data: recipe, isLoading, isError, error, refetch } = useRecipe(recipeId);
  const backTo = recipeId ? `/recipes/${recipeId}` : '/recipes';

  if (isLoading) {
    return (
      <div className="recipe-form-page">
        <PageHeader title="Edit recipe" backTo={backTo} task />
        <div className="recipe-form-page__body">
          <Skeleton className="recipe-form-page__skeleton-block" />
        </div>
      </div>
    );
  }

  // A 404 wins even over stale data (checked before the data-presence gate
  // below): the recipe was deleted while its stale detail was still
  // cached. See RecipeDetailPage for the same split.
  const notFound = isError && error instanceof ApiError && error.status === 404;
  if (notFound) {
    return (
      <div className="recipe-form-page">
        <PageHeader title="Edit recipe" backTo="/recipes" task />
        <div className="recipe-form-page__body">
          <Callout.Root color="red" variant="surface" role="alert">
            <Callout.Icon>
              <ExclamationTriangleIcon />
            </Callout.Icon>
            <Callout.Text>This recipe couldn&apos;t be found.</Callout.Text>
          </Callout.Root>
        </div>
      </div>
    );
  }

  // Gated on the absence of data: a background refetch error (window
  // focus) must not blank an in-progress edit of an already-loaded recipe.
  if (isError && !recipe) {
    return (
      <div className="recipe-form-page">
        <PageHeader title="Edit recipe" backTo="/recipes" task />
        <div className="recipe-form-page__body">
          <QueryErrorState message="Couldn't load this recipe." onRetry={() => void refetch()} />
        </div>
      </div>
    );
  }

  if (!recipe) {
    return null;
  }

  // Keyed on the recipe id so the form's local state (below) is seeded
  // fresh from `recipe` exactly once per recipe, via useState's lazy
  // initializer, instead of syncing from a prop with an effect — a
  // background refetch (live sync, window focus) updates `recipe`
  // without remounting this, so it never clobbers in-progress edits.
  return <EditRecipeForm key={recipe.id} recipe={recipe} backTo={backTo} />;
}

function EditRecipeForm({ recipe, backTo }: { recipe: RecipeDetailResponse; backTo: string }) {
  const saveEdits = useSaveRecipeEdits(recipe.id);
  // Across tabs too: the form may have been opened from a plan's view of the recipe.
  const { goBack } = useBack(backTo, { acrossAreas: true });

  // Lines with a confirmed ingredient are edited here. An imported draft's
  // lines that are still in review have none yet; they are resolved on the
  // recipe's page and left alone by this form.
  const [editableLines] = useState<DraftLine[]>(() =>
    recipe.ingredients.flatMap((line) =>
      line.ingredient
        ? [{ clientId: line.id, lineId: line.id, ingredient: line.ingredient, quantity: formatEditableQuantity(line.quantity), unit: line.unit }]
        : [],
    ),
  );
  const inReviewCount = recipe.ingredients.length - editableLines.length;

  const [values, setValues] = useState<RecipeFormValues>({
    name: recipe.name,
    description: recipe.description ?? '',
    servings: recipe.baseServings,
    webLink: recipe.webLink ?? '',
    meal: recipe.meal ?? '',
    theme: recipe.theme ?? '',
    lines: editableLines,
  });
  const [pendingLine, setPendingLine] = useState<PendingLine | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function handleSave(event: FormEvent) {
    event.preventDefault();
    const result = validateRecipeForm(values, pendingLine);
    if ('error' in result) {
      setError(result.error);
      return;
    }
    setError(null);

    // description/meal/theme are omitted when unchanged, but sent as "" (not
    // omitted) when the user cleared a previously non-empty value — the
    // backend only clears a field it's explicitly given "" for, so omitting
    // it there would silently leave the old value in place.
    const fields: UpdateRecipeRequest = {};
    if (values.name.trim() !== recipe.name) fields.name = values.name.trim();
    if (values.servings !== recipe.baseServings) fields.baseServings = values.servings;
    const descriptionPatch = fieldPatch(recipe.description, values.description);
    if (descriptionPatch !== undefined) fields.description = descriptionPatch;
    const mealPatch = fieldPatch(recipe.meal, values.meal);
    if (mealPatch !== undefined) fields.meal = mealPatch;
    const themePatch = fieldPatch(recipe.theme, values.theme);
    if (themePatch !== undefined) fields.theme = themePatch;

    // What happened to the lines this form started with, and what is new.
    // validateRecipeForm confirmed every quantity parses; `?? 0` never triggers.
    const kept = new Map(result.lines.flatMap((line) => (line.lineId ? [[line.lineId, line] as const] : [])));
    const edits: RecipeEdits = {
      fields: Object.keys(fields).length > 0 ? fields : undefined,
      removedLineIds: editableLines.flatMap((line) => (line.lineId && !kept.has(line.lineId) ? [line.lineId] : [])),
      changedLines: editableLines.flatMap((original) => {
        const current = original.lineId ? kept.get(original.lineId) : undefined;
        if (!current || !original.lineId) return [];
        const quantity = parseQuantity(current.quantity) ?? 0;
        const unchanged = quantity === (parseQuantity(original.quantity) ?? 0) && current.unit === original.unit;
        return unchanged ? [] : [{ lineId: original.lineId, ingredientId: current.ingredient.id, quantity, unit: current.unit }];
      }),
      addedLines: result.lines
        .filter((line) => !line.lineId)
        .map((line) => ({ ingredientId: line.ingredient.id, quantity: parseQuantity(line.quantity) ?? 0, unit: line.unit })),
    };

    try {
      await saveEdits.mutateAsync(edits);
    } catch {
      // The global mutation error toast said what failed. Some of the changes
      // may have been saved before it; the recipe is being refetched, so
      // leave the form rather than show it a state that may no longer be true.
      goBack();
      return;
    }
    toast.info('Recipe updated.');
    // An edit is done once it is saved: back to where it was opened from.
    goBack();
  }

  return (
    <div className="recipe-form-page">
      <PageHeader
        title="Edit recipe"
        backTo={backTo}
        task
        actions={
          <Button size="3" type="submit" form="edit-recipe-form" loading={saveEdits.isPending}>
            Save
          </Button>
        }
      />
      <form id="edit-recipe-form" className="recipe-form-page__body" onSubmit={handleSave} onKeyDown={enterMovesOn}>
        <RecipeFormFields
          values={values}
          onChange={(patch) => setValues((current) => ({ ...current, ...patch }))}
          onPendingLineChange={setPendingLine}
          sourceEditable={false}
          error={error}
        >
          {inReviewCount > 0 && (
            <Text as="p" size="1" color="gray">
              {inReviewCount === 1 ? '1 imported ingredient is' : `${inReviewCount} imported ingredients are`} still in
              review and not shown here. Review {inReviewCount === 1 ? 'it' : 'them'} on the recipe&apos;s page.
            </Text>
          )}
        </RecipeFormFields>
      </form>
    </div>
  );
}

/** A saved quantity as the text the line's quantity field shows: "1.5", not "1½", so it can be edited and re-parsed. */
function formatEditableQuantity(quantity: number): string {
  return String(Math.round(quantity * 1000) / 1000);
}
