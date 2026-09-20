import { useState, type FormEvent } from 'react';
import { Outlet, useParams } from 'react-router-dom';
import { Badge, Button, Callout, Heading, IconButton, Select, Skeleton, Text, TextArea, TextField } from '@radix-ui/themes';
import { ExclamationTriangleIcon, ExternalLinkIcon, MinusIcon, PlusIcon } from '@radix-ui/react-icons';
import { PageHeader } from '../../components/PageHeader';
import { SheetLink } from '../../components/SheetLink';
import { QueryErrorState } from '../../components/QueryErrorState';
import { useRecipe, useUpdateRecipe } from '../../queries/recipes';
import { MEALS, THEMES, capitalize } from '../../lib/ingredientConstants';
import { formatQuantity } from '../../lib/formatQuantity';
import { toast } from '../../lib/toastStore';
import { ApiError } from '../../api/http';
import type { RecipeDetailResponse } from '../../api/recipes';
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
        <PageHeader title="Edit recipe" backTo={backTo} />
        <div className="recipe-form-page__body">
          <Skeleton className="recipe-form-page__skeleton-block" />
        </div>
        <Outlet />
      </div>
    );
  }

  if (isError || !recipe) {
    const notFound = error instanceof ApiError && error.status === 404;
    return (
      <div className="recipe-form-page">
        <PageHeader title="Edit recipe" backTo="/recipes" />
        <div className="recipe-form-page__body">
          {notFound ? (
            <Callout.Root color="red" variant="surface" role="alert">
              <Callout.Icon>
                <ExclamationTriangleIcon />
              </Callout.Icon>
              <Callout.Text>This recipe couldn&apos;t be found.</Callout.Text>
            </Callout.Root>
          ) : (
            <QueryErrorState message="Couldn't load this recipe." onRetry={() => void refetch()} />
          )}
        </div>
        <Outlet />
      </div>
    );
  }

  // Keyed on the recipe id so the form's local state (below) is seeded
  // fresh from `recipe` exactly once per recipe, via useState's lazy
  // initializer, instead of syncing from a prop with an effect — a
  // background refetch (live sync, window focus) updates `recipe`
  // without remounting this, so it never clobbers in-progress edits.
  return <EditRecipeForm key={recipe.id} recipe={recipe} backTo={backTo} />;
}

function EditRecipeForm({ recipe, backTo }: { recipe: RecipeDetailResponse; backTo: string }) {
  const updateRecipe = useUpdateRecipe(recipe.id);

  const [name, setName] = useState(recipe.name);
  const [description, setDescription] = useState(recipe.description ?? '');
  const [servings, setServings] = useState(recipe.baseServings);
  const [meal, setMeal] = useState(recipe.meal ?? '');
  const [theme, setTheme] = useState(recipe.theme ?? '');
  const [error, setError] = useState<string | null>(null);

  async function handleSave(event: FormEvent) {
    event.preventDefault();
    if (!name.trim()) {
      setError('Name is required.');
      return;
    }
    if (!Number.isFinite(servings) || servings < 1) {
      setError('Servings must be at least 1.');
      return;
    }
    setError(null);
    // description/meal/theme are omitted when unchanged, but sent as "" (not
    // omitted) when the user cleared a previously non-empty value — the
    // backend only clears a field it's explicitly given "" for, so omitting
    // it there would silently leave the old value in place.
    const payload: Parameters<typeof updateRecipe.mutateAsync>[0] = {
      name: name.trim(),
      baseServings: servings,
    };
    const descriptionPatch = fieldPatch(recipe.description, description);
    if (descriptionPatch !== undefined) payload.description = descriptionPatch;
    const mealPatch = fieldPatch(recipe.meal, meal);
    if (mealPatch !== undefined) payload.meal = mealPatch;
    const themePatch = fieldPatch(recipe.theme, theme);
    if (themePatch !== undefined) payload.theme = themePatch;
    await updateRecipe.mutateAsync(payload);
    toast.info('Recipe updated.');
  }

  return (
    <div className="recipe-form-page">
      <PageHeader
        title="Edit recipe"
        backTo={backTo}
        actions={
          <Button size="3" type="submit" form="edit-recipe-form" loading={updateRecipe.isPending}>
            Save
          </Button>
        }
      />
      <form id="edit-recipe-form" className="recipe-form-page__body" onSubmit={handleSave}>
        <Text as="label" size="2" weight="medium" className="recipe-form-page__field">
          Name
          <TextField.Root
            size="3"
            value={name}
            onChange={(event) => setName(event.target.value)}
            autoCapitalize="words"
            enterKeyHint="next"
          />
        </Text>

        <Text as="label" size="2" weight="medium" className="recipe-form-page__field">
          Description
          <TextArea
            size="3"
            value={description}
            onChange={(event) => setDescription(event.target.value)}
            rows={3}
            autoCapitalize="sentences"
          />
        </Text>

        <div className="recipe-form-page__field">
          <Text as="span" size="2" weight="medium">
            Servings
          </Text>
          <div className="recipe-form-page__stepper">
            <IconButton
              size="3"
              type="button"
              variant="soft"
              aria-label="Decrease servings"
              className="recipe-form-page__icon-button"
              onClick={() => setServings((s) => Math.max(1, s - 1))}
            >
              <MinusIcon />
            </IconButton>
            <Text as="span" size="4" weight="medium" className="recipe-form-page__stepper-value">
              {servings}
            </Text>
            <IconButton
              size="3"
              type="button"
              variant="soft"
              aria-label="Increase servings"
              className="recipe-form-page__icon-button"
              onClick={() => setServings((s) => s + 1)}
            >
              <PlusIcon />
            </IconButton>
          </div>
        </div>

        {recipe.webLink && (
          <div className="recipe-form-page__field">
            <Text as="span" size="2" weight="medium">
              Source
            </Text>
            <a
              href={recipe.webLink}
              target="_blank"
              rel="noopener noreferrer"
              className="recipe-form-page__readonly-link"
            >
              {recipe.webLink} <ExternalLinkIcon />
            </a>
          </div>
        )}

        <div className="recipe-form-page__row">
          <Text as="label" size="2" weight="medium" className="recipe-form-page__field">
            Meal
            <Select.Root value={meal} onValueChange={setMeal} size="3">
              <Select.Trigger placeholder="None" />
              <Select.Content>
                {MEALS.map((m) => (
                  <Select.Item key={m} value={m}>
                    {capitalize(m)}
                  </Select.Item>
                ))}
              </Select.Content>
            </Select.Root>
          </Text>

          <Text as="label" size="2" weight="medium" className="recipe-form-page__field">
            Theme
            <Select.Root value={theme} onValueChange={setTheme} size="3">
              <Select.Trigger placeholder="None" />
              <Select.Content>
                {THEMES.map((t) => (
                  <Select.Item key={t} value={t}>
                    {capitalize(t)}
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

        <div className="recipe-form-page__lines-header">
          <Heading size="3">Ingredients</Heading>
          <Text as="span" size="1" color="gray">
            Changes save immediately
          </Text>
        </div>

        {recipe.ingredients.length === 0 ? (
          <Text as="p" size="2" color="gray">
            No ingredients yet.
          </Text>
        ) : (
          <ul className="recipe-form-page__lines">
            {recipe.ingredients.map((line) => {
              const ingredientName =
                line.ingredient?.name ?? line.matchedIngredient?.name ?? line.suggestedIngredientName ?? line.originalText ?? 'Unknown ingredient';
              return (
                <li key={line.id}>
                  {/* Relative: these are children of THIS route (`/recipes/:id/edit/lines/...`),
                      not of the detail route, so a save/cancel here returns to this edit page. */}
                  <SheetLink to={`lines/${line.id}`} className="recipe-form-page__line">
                    <Text as="span" size="2" className="recipe-form-page__line-text">
                      {formatQuantity(line.quantity)} {line.unit} {ingredientName}
                    </Text>
                    {line.status === 'pending_review' && (
                      <Badge color="amber" variant="soft" size="1">
                        Needs review
                      </Badge>
                    )}
                  </SheetLink>
                </li>
              );
            })}
          </ul>
        )}

        <SheetLink to="lines/new" className="recipe-form-page__add-line">
          <PlusIcon /> Add ingredient
        </SheetLink>
      </form>
      <Outlet />
    </div>
  );
}
