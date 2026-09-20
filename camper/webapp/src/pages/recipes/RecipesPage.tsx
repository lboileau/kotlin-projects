import { useMemo } from 'react';
import { Outlet, useNavigate, useSearchParams } from 'react-router-dom';
import { Badge, Button, DropdownMenu, IconButton, Skeleton, Switch, Text, TextField } from '@radix-ui/themes';
import {
  Cross2Icon,
  DownloadIcon,
  InfoCircledIcon,
  Link2Icon,
  MagnifyingGlassIcon,
  PlusIcon,
} from '@radix-ui/react-icons';
import { PageHeader } from '../../components/PageHeader';
import { RecipesIngredientsToggle } from '../../components/RecipesIngredientsToggle';
import { QueryErrorState } from '../../components/QueryErrorState';
import { useAuth } from '../../auth/useAuth';
import { useRecipes } from '../../queries/recipes';
import { MEALS, capitalize } from '../../lib/ingredientConstants';
import type { RecipeResponse } from '../../api/recipes';
import './RecipesPage.css';

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

  const q = searchParams.get('q') ?? '';
  const meal = searchParams.get('meal') ?? 'all';
  const mine = searchParams.get('mine') === '1';
  const hasFilters = q.trim().length > 0 || meal !== 'all' || mine;

  const patch = (values: Record<string, string | null>) => updateParams(searchParams, setSearchParams, values);
  const clearFilters = () => patch({ q: null, meal: null, mine: null });

  const mealsPresent = useMemo(() => {
    const present = new Set((recipes ?? []).map((r) => r.meal).filter((m): m is string => Boolean(m)));
    return MEALS.filter((m) => present.has(m));
  }, [recipes]);

  const filtered = useMemo(() => {
    let list = recipes ?? [];
    if (mine && user) list = list.filter((r) => r.createdBy === user.id);
    if (meal !== 'all') list = list.filter((r) => r.meal === meal);
    const needle = q.trim().toLowerCase();
    if (needle) list = list.filter((r) => r.name.toLowerCase().includes(needle));
    return [...list].sort((a, b) => a.name.localeCompare(b.name, undefined, { sensitivity: 'base' }));
  }, [recipes, mine, meal, q, user]);

  return (
    <div className="recipes-page">
      <PageHeader
        title="Recipes"
        actions={
          <>
            <IconButton
              size="3"
              variant="soft"
              aria-label="Import recipe"
              className="recipes-page__icon-button"
              onClick={() => navigate('/recipes/import')}
            >
              <DownloadIcon />
            </IconButton>
            <Button size="3" onClick={() => navigate('/recipes/new')}>
              <PlusIcon /> New
            </Button>
          </>
        }
      />

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
          onChange={(event) => patch({ q: event.target.value })}
        >
          <TextField.Slot>
            <MagnifyingGlassIcon />
          </TextField.Slot>
          {q && (
            <TextField.Slot>
              <IconButton size="2" variant="ghost" aria-label="Clear search" onClick={() => patch({ q: null })}>
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

        <label className="recipes-page__mine">
          <Text as="span" size="2">
            Mine
          </Text>
          <Switch checked={mine} onCheckedChange={(checked) => patch({ mine: checked ? '1' : null })} />
        </label>
      </div>

      <div className="recipes-page__list">
        {isLoading && (
          <div className="recipes-page__skeletons">
            {[0, 1, 2, 3].map((i) => (
              <Skeleton key={i} className="recipes-page__skeleton-row" />
            ))}
          </div>
        )}

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
              isOwner={recipe.createdBy === user?.id}
              onOpen={() => navigate(`/recipes/${recipe.id}`)}
              onAddToPlan={() => navigate(`/recipes/${recipe.id}/add-to-plan`)}
              onEdit={() => navigate(`/recipes/${recipe.id}/edit`)}
            />
          ))}
      </div>

      <Outlet />
    </div>
  );
}

function RecipeRow({
  recipe,
  isOwner,
  onOpen,
  onAddToPlan,
  onEdit,
}: {
  recipe: RecipeResponse;
  isOwner: boolean;
  onOpen: () => void;
  onAddToPlan: () => void;
  onEdit: () => void;
}) {
  return (
    <div className="recipes-page__row">
      <button type="button" className="recipes-page__row-open" onClick={onOpen}>
        <div className="recipes-page__row-main">
          <Text as="span" size="3" weight="medium" className="recipes-page__row-name">
            {recipe.name}
          </Text>
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

      <DropdownMenu.Root>
        <DropdownMenu.Trigger>
          <IconButton
            type="button"
            variant="ghost"
            color="gray"
            size="3"
            aria-label={`More about ${recipe.name}`}
            className="recipes-page__row-info"
          >
            <InfoCircledIcon />
          </IconButton>
        </DropdownMenu.Trigger>
        <DropdownMenu.Content>
          <DropdownMenu.Item onSelect={onOpen}>View recipe</DropdownMenu.Item>
          <DropdownMenu.Item onSelect={onAddToPlan}>Add to plan</DropdownMenu.Item>
          {isOwner && <DropdownMenu.Item onSelect={onEdit}>Edit recipe</DropdownMenu.Item>}
        </DropdownMenu.Content>
      </DropdownMenu.Root>
    </div>
  );
}
