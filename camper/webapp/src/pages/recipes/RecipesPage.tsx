import { useMemo } from 'react';
import { Outlet, useNavigate, useSearchParams } from 'react-router-dom';
import { Badge, Button, IconButton, Select, Text, TextField } from '@radix-ui/themes';
import {
  Cross2Icon,
  DownloadIcon,
  Link2Icon,
  MagnifyingGlassIcon,
  PlusIcon,
} from '@radix-ui/react-icons';
import { PageLoader } from '../../components/PageLoader';
import { PageHeader } from '../../components/PageHeader';
import { RowActionButton } from '../../components/RowActionButton';
import { HeartGlyph } from '../../components/HeartGlyph';
import { RecipesIngredientsToggle } from '../../components/RecipesIngredientsToggle';
import { QueryErrorState } from '../../components/QueryErrorState';
import { useAuth } from '../../auth/useAuth';
import { useRecipes } from '../../queries/recipes';
import { useAddRecipeToPlan, usePlans, useRemoveRecipeFromPlan } from '../../queries/plans';
import { toast } from '../../lib/toastStore';
import { MEALS, capitalize } from '../../lib/ingredientConstants';
import { useSearchText } from '../../lib/useSearchText';
import { favouritesCountLabel, matchesShowFilter, parseShowFilter } from '../../lib/recipeFavorites';
import type { RecipeResponse } from '../../api/recipes';
import './RecipesPage.css';

// Single choice, `All recipes` first and written as an absent `show` param.
// Replaces the old Mine switch; it ANDs with the meal chips and the search
// box. A dropdown rather than a second chip row: chips cost a whole row of
// vertical space above the list, which the owner declined.
const SHOW_OPTIONS = [
  { value: 'all', label: 'All recipes' },
  { value: 'mine', label: 'Mine' },
  { value: 'favourites', label: 'Favourites' },
  { value: 'my-favourites', label: 'My favourites' },
] as const;

function updateParams(
  searchParams: URLSearchParams,
  setSearchParams: (next: URLSearchParams, opts: { replace: boolean }) => void,
  patch: Record<string, string | null>,
) {
  const next = new URLSearchParams(searchParams);
  for (const [key, value] of Object.entries(patch)) {
    if (value === null || value === '') {
      next.delete(key);
    } else {
      next.set(key, value);
    }
  }
  setSearchParams(next, { replace: true });
}

export function RecipesPage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { data: recipes, isLoading, isError, refetch } = useRecipes();
  // Whether there's already data to show — used below so a background
  // refetch error (window focus) falls through to the normal render
  // instead of blanking an already-loaded list.
  const hasData = !!recipes;
  const [searchParams, setSearchParams] = useSearchParams();
  const { data: plans } = usePlans();
  const addRecipe = useAddRecipeToPlan();
  const removeRecipe = useRemoveRecipeFromPlan();

  // With exactly one plan there is nothing to choose, so ＋ adds straight to
  // it, with an Undo in place of the sheet. With several (or none) the sheet
  // opens as before.
  function handleAddToPlan(recipe: RecipeResponse) {
    const onlyPlan = plans?.length === 1 ? plans[0] : null;
    if (!onlyPlan) {
      navigate(`/recipes/add-to-plan/${recipe.id}${window.location.search}`);
      return;
    }
    addRecipe.mutate(
      {
        planId: onlyPlan.id,
        recipeId: recipe.id,
        recipeName: recipe.name,
        recipeWebLink: recipe.webLink,
        baseServings: recipe.baseServings,
      },
      {
        onSuccess: (result) => {
          if (result.alreadyInPlan) {
            toast.info(`Already in ${onlyPlan.name}`);
            return;
          }
          toast.info(`Added to ${onlyPlan.name}`, {
            label: 'Undo',
            onClick: () => removeRecipe.mutate({ planId: onlyPlan.id, recipeId: recipe.id }),
          });
        },
      },
    );
  }

  const [q, setQ] = useSearchText();
  const meal = searchParams.get('meal') ?? 'all';
  const show = parseShowFilter(searchParams.get('show'));
  const hasFilters = q.trim().length > 0 || meal !== 'all' || show !== 'all';

  const patch = (values: Record<string, string | null>) => updateParams(searchParams, setSearchParams, values);
  const clearFilters = () => {
    setQ('');
    patch({ q: null, meal: null, show: null });
  };

  const mealsPresent = useMemo(() => {
    const present = new Set((recipes ?? []).map((r) => r.meal).filter((m): m is string => Boolean(m)));
    return MEALS.filter((m) => present.has(m));
  }, [recipes]);

  const filtered = useMemo(() => {
    let list = recipes ?? [];
    list = list.filter((r) => matchesShowFilter(r, show, user?.id));
    if (meal !== 'all') list = list.filter((r) => r.meal === meal);
    const needle = q.trim().toLowerCase();
    if (needle) list = list.filter((r) => r.name.toLowerCase().includes(needle));
    return [...list].sort((a, b) => a.name.localeCompare(b.name, undefined, { sensitivity: 'base' }));
  }, [recipes, show, meal, q, user]);

  return (
    <div className="recipes-page">
      <PageHeader title="Recipes" />

      <div className="recipes-page__controls">
        <RecipesIngredientsToggle active="recipes" />

        <TextField.Root
          size="3"
          placeholder="Search recipes"
          aria-label="Search recipes"
          type="search"
          enterKeyHint="search"
          autoCapitalize="off"
          autoCorrect="off"
          autoComplete="off"
          value={q}
          onChange={(event) => setQ(event.target.value)}
        >
          <TextField.Slot>
            <MagnifyingGlassIcon />
          </TextField.Slot>
          {q && (
            <TextField.Slot>
              <IconButton size="2" variant="ghost" aria-label="Clear search" onClick={() => setQ('')}>
                <Cross2Icon />
              </IconButton>
            </TextField.Slot>
          )}
        </TextField.Root>

        <div className="recipes-page__chips" role="group" aria-label="Filter by meal">
          <button
            type="button"
            className={`recipes-page__chip${meal === 'all' ? ' recipes-page__chip--active' : ''}`}
            aria-pressed={meal === 'all'}
            onClick={() => patch({ meal: null })}
          >
            All
          </button>
          {mealsPresent.map((m) => (
            <button
              key={m}
              type="button"
              className={`recipes-page__chip${meal === m ? ' recipes-page__chip--active' : ''}`}
              aria-pressed={meal === m}
              onClick={() => patch({ meal: meal === m ? null : m })}
            >
              {capitalize(m)}
            </button>
          ))}
        </div>

        {/* The list's own actions sit on the list, not in the header: the
            header only says where you are. The Show dropdown is one of
            them, on the left of the same row — it is where the Mine switch
            used to be, and it costs no extra vertical space. */}
        <div className="recipes-page__actions-row">
          <Select.Root
            size="3"
            value={show}
            onValueChange={(value) => patch({ show: value === 'all' ? null : value })}
          >
            <Select.Trigger variant="soft" aria-label="Show" className="recipes-page__show" />
            {/* Plain Select.Content: this page is not inside a Sheet, so
                there is no scroll lock to portal into (see
                components/SheetSelectContent). */}
            <Select.Content>
              {SHOW_OPTIONS.map(({ value, label }) => (
                <Select.Item key={value} value={value}>
                  {label}
                </Select.Item>
              ))}
            </Select.Content>
          </Select.Root>

          <div className="recipes-page__actions">
            <Button size="3" variant="soft" onClick={() => navigate('/recipes/import')}>
              <DownloadIcon /> Import
            </Button>
            <Button size="3" onClick={() => navigate('/recipes/new')}>
              <PlusIcon /> New
            </Button>
          </div>
        </div>
      </div>

      <div className="recipes-page__list">
        {isLoading && <PageLoader area="recipes" label="Loading recipes" />}

        {isError && !hasData && <QueryErrorState message="Couldn't load recipes." onRetry={() => void refetch()} />}

        {!isLoading && (hasData || !isError) && filtered.length === 0 && (
          <div className="recipes-page__empty">
            {recipes && recipes.length > 0 ? (
              <>
                <Text as="p" color="gray" size="2">
                  No recipes match your filters.
                </Text>
                {hasFilters && (
                  <Button size="2" variant="soft" onClick={clearFilters}>
                    Clear filters
                  </Button>
                )}
              </>
            ) : (
              <>
                <Text as="p" color="gray" size="2">
                  No recipes yet.
                </Text>
                <div className="recipes-page__empty-actions">
                  <Button size="2" onClick={() => navigate('/recipes/new')}>
                    <PlusIcon /> New recipe
                  </Button>
                  <Button size="2" variant="soft" onClick={() => navigate('/recipes/import')}>
                    <DownloadIcon /> Import
                  </Button>
                </div>
              </>
            )}
          </div>
        )}

        {!isLoading &&
          (hasData || !isError) &&
          filtered.map((recipe) => (
            <RecipeRow
              key={recipe.id}
              recipe={recipe}
              onOpen={() => navigate(`/recipes/${recipe.id}`)}
              onAddToPlan={() => handleAddToPlan(recipe)}
            />
          ))}
      </div>

      <Outlet />
    </div>
  );
}

function RecipeRow({
  recipe,
  onOpen,
  onAddToPlan,
}: {
  recipe: RecipeResponse;
  onOpen: () => void;
  onAddToPlan: () => void;
}) {
  return (
    <div className="recipes-page__row">
      <button type="button" className="recipes-page__row-open" onClick={onOpen}>
        <div className="recipes-page__row-main">
          <Text as="span" size="3" weight="medium" className="recipes-page__row-name">
            {recipe.name}
          </Text>
          {/* On the title line, right after the name: under it, it fought
              "Serves" and the tags for one line. Read-only, and only once
              someone has favourited it — favouriting is done on the recipe
              page. Solid when you are one of them, soft when it is only
              other people, so your own stand out down the list.
              `role="img"` with the spelt-out label reads as "3 favourites,
              including you" instead of a stray number, and keeps it out of
              the tab order: a fact about the row, not a control. */}
          {recipe.favoriteCount > 0 && (
            <span
              className={`recipes-page__row-favourites${recipe.favoritedByMe ? ' recipes-page__row-favourites--mine' : ''}`}
              role="img"
              aria-label={favouritesCountLabel(recipe.favoriteCount, recipe.favoritedByMe)}
            >
              <HeartGlyph size={13} />
              {recipe.favoriteCount}
            </span>
          )}
          {recipe.status === 'draft' && (
            <Badge color="amber" variant="soft">
              Draft
            </Badge>
          )}
          {recipe.webLink && (
            <span className="recipes-page__row-imported" role="img" aria-label="Imported from a link" title="Imported from a link">
              <Link2Icon />
            </span>
          )}
        </div>
        <div className="recipes-page__row-meta">
          <Text as="span" size="2" color="gray">
            Serves {recipe.baseServings}
          </Text>
          {recipe.meal && (
            <Badge variant="soft" color="gray">
              {capitalize(recipe.meal)}
            </Badge>
          )}
          {recipe.theme && (
            <Badge variant="soft" color="gray">
              {capitalize(recipe.theme)}
            </Badge>
          )}
        </div>
      </button>

      <RowActionButton
        aria-label={`Add ${recipe.name} to a plan`}
        className="recipes-page__row-info"
        onClick={onAddToPlan}
      >
        <PlusIcon />
      </RowActionButton>
    </div>
  );
}
