import { useMemo } from 'react';
import { Outlet, useNavigate } from 'react-router-dom';
import { Button, Text, TextField } from '@radix-ui/themes';
import { MagnifyingGlassIcon, MixIcon, PlusIcon } from '@radix-ui/react-icons';
import { PageLoader } from '../../components/PageLoader';
import { PageHeader } from '../../components/PageHeader';
import { RecipesIngredientsToggle } from '../../components/RecipesIngredientsToggle';
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

  const filtered = useMemo(() => {
    const list = ingredients ?? [];
    const needle = q.trim().toLowerCase();
    return needle ? list.filter((i) => i.name.toLowerCase().includes(needle)) : list;
  }, [ingredients, q]);

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
                  No ingredients match your search.
                </Text>
                <Button size="2" variant="soft" onClick={() => setQ('')}>
                  Clear search
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
