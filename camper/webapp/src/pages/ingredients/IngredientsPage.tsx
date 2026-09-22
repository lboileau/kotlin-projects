import { useMemo } from 'react';
import { Outlet, useNavigate, useSearchParams } from 'react-router-dom';
import { Button, Text, TextField } from '@radix-ui/themes';
import { MagnifyingGlassIcon, MixIcon, PlusIcon } from '@radix-ui/react-icons';
import { PageLoader } from '../../components/PageLoader';
import { PageHeader } from '../../components/PageHeader';
import { RecipesIngredientsToggle } from '../../components/RecipesIngredientsToggle';
import { FilterChips } from '../../components/FilterChips';
import { SheetLink } from '../../components/SheetLink';
import { QueryErrorState } from '../../components/QueryErrorState';
import { useIngredients } from '../../queries/ingredients';
import { CATEGORIES, capitalize } from '../../lib/ingredientConstants';
import { useSearchText } from '../../lib/useSearchText';
import type { IngredientResponse } from '../../api/ingredients';
import './IngredientsPage.css';

export function IngredientsPage() {
  const navigate = useNavigate();
  const { data: ingredients, isLoading, isError, refetch } = useIngredients();
  // Whether there's already data to show — used below so a background
  // refetch error (window focus) falls through to the normal render
  // instead of blanking an already-loaded list.
  const hasData = !!ingredients;
  const [q, setQ] = useSearchText();
  // The category filter lives in the URL like the Recipes list's meal filter,
  // so Back, a reload and a shared link all keep it.
  const [searchParams, setSearchParams] = useSearchParams();
  const selected = searchParams.get('category');
  // A category nobody uses any more (or a mistyped link) filters nothing.
  const category = selected && (CATEGORIES as readonly string[]).includes(selected) ? selected : null;

  function setCategory(next: string | null) {
    setSearchParams(
      (previous) => {
        const params = new URLSearchParams(previous);
        if (next) params.set('category', next);
        else params.delete('category');
        return params;
      },
      { replace: true },
    );
  }

  function clearFilters() {
    setQ('');
    setCategory(null);
  }

  // Only the categories that have ingredients, in the list's own order.
  const categoryOptions = useMemo(() => {
    const present = new Set((ingredients ?? []).map((i) => i.category));
    return CATEGORIES.filter((c) => present.has(c)).map((c) => ({ value: c, label: capitalize(c) }));
  }, [ingredients]);

  const filtered = useMemo(() => {
    let list = ingredients ?? [];
    if (category) list = list.filter((i) => i.category === category);
    const needle = q.trim().toLowerCase();
    return needle ? list.filter((i) => i.name.toLowerCase().includes(needle)) : list;
  }, [ingredients, q, category]);

  const groups = useMemo(() => {
    const byCategory = new Map<string, IngredientResponse[]>();
    for (const ingredient of filtered) {
      const list = byCategory.get(ingredient.category) ?? [];
      list.push(ingredient);
      byCategory.set(ingredient.category, list);
    }
    for (const list of byCategory.values()) {
      list.sort((a, b) => a.name.localeCompare(b.name));
    }
    return CATEGORIES.map((category) => ({ category, items: byCategory.get(category) ?? [] })).filter(
      (group) => group.items.length > 0,
    );
  }, [filtered]);

  return (
    <div className="ingredients-page">
      <PageHeader title="Ingredients" icon={MixIcon} />

      <div className="ingredients-page__controls">
        <RecipesIngredientsToggle active="ingredients" />
        <TextField.Root
          size="3"
          placeholder="Search ingredients"
          aria-label="Search ingredients"
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
        </TextField.Root>
        <FilterChips label="Filter by category" options={categoryOptions} value={category} onChange={setCategory} />
        {/* Same place as the Recipes list's actions: on the list, not in the header. */}
        <div className="ingredients-page__actions">
          <Button size="3" onClick={() => navigate('/ingredients/new')}>
            <PlusIcon /> Add
          </Button>
        </div>
      </div>

      <div className="ingredients-page__list">
        {isLoading && <PageLoader area="recipes" label="Loading ingredients" />}

        {isError && !hasData && <QueryErrorState message="Couldn't load ingredients." onRetry={() => void refetch()} />}

        {!isLoading && (hasData || !isError) && groups.length === 0 && (
          <div className="ingredients-page__empty">
            {ingredients && ingredients.length > 0 ? (
              <>
                <Text as="p" color="gray" size="2">
                  No ingredients match your filters.
                </Text>
                <Button size="2" variant="soft" onClick={clearFilters}>
                  Clear filters
                </Button>
              </>
            ) : (
              <>
                <Text as="p" color="gray" size="2">
                  No ingredients yet.
                </Text>
                <Button size="2" onClick={() => navigate('/ingredients/new')}>
                  <PlusIcon /> Add ingredients
                </Button>
              </>
            )}
          </div>
        )}

        {!isLoading &&
          (hasData || !isError) &&
          groups.map((group) => (
            <section key={group.category} className="ingredients-page__group">
              <Text as="p" size="1" weight="bold" color="gray" className="ingredients-page__group-header">
                {capitalize(group.category)}
              </Text>
              <ul className="ingredients-page__rows">
                {group.items.map((ingredient) => (
                  <li key={ingredient.id}>
                    <SheetLink to={`/ingredients/${ingredient.id}`} className="ingredients-page__row">
                      <Text as="span" size="2" className="ingredients-page__row-name">
                        {ingredient.name}
                      </Text>
                      <Text as="span" size="1" color="gray">
                        {ingredient.defaultUnit}
                      </Text>
                    </SheetLink>
                  </li>
                ))}
              </ul>
            </section>
          ))}
      </div>

      <Outlet />
    </div>
  );
}
