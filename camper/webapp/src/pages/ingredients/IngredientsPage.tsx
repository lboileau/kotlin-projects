import { useMemo } from 'react';
import { Outlet, useNavigate, useSearchParams } from 'react-router-dom';
import { Button, Callout, Skeleton, Text, TextField } from '@radix-ui/themes';
import { ExclamationTriangleIcon, MagnifyingGlassIcon, PlusIcon } from '@radix-ui/react-icons';
import { PageHeader } from '../../components/PageHeader';
import { RecipesIngredientsToggle } from '../../components/RecipesIngredientsToggle';
import { SheetLink } from '../../components/SheetLink';
import { useIngredients } from '../../queries/ingredients';
import { CATEGORIES, capitalize } from '../../lib/ingredientConstants';
import type { IngredientResponse } from '../../api/ingredients';
import './IngredientsPage.css';

export function IngredientsPage() {
  const navigate = useNavigate();
  const { data: ingredients, isLoading, isError } = useIngredients();
  const [searchParams, setSearchParams] = useSearchParams();
  const q = searchParams.get('q') ?? '';

  function handleSearch(value: string) {
    setSearchParams(
      (prev) => {
        const next = new URLSearchParams(prev);
        if (value) next.set('q', value);
        else next.delete('q');
        return next;
      },
      { replace: true },
    );
  }

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
      <PageHeader
        title="Ingredients"
        backTo="/recipes"
        actions={
          <Button onClick={() => navigate('/ingredients/new')}>
            <PlusIcon /> Add
          </Button>
        }
      />

      <div className="ingredients-page__controls">
        <RecipesIngredientsToggle active="ingredients" />
        <TextField.Root
          size="3"
          placeholder="Search ingredients"
          aria-label="Search ingredients"
          value={q}
          onChange={(event) => handleSearch(event.target.value)}
        >
          <TextField.Slot>
            <MagnifyingGlassIcon />
          </TextField.Slot>
        </TextField.Root>
      </div>

      <div className="ingredients-page__list">
        {isLoading && (
          <div className="ingredients-page__skeletons">
            {[0, 1, 2, 3, 4].map((i) => (
              <Skeleton key={i} className="ingredients-page__skeleton-row" />
            ))}
          </div>
        )}

        {isError && (
          <Callout.Root color="red" variant="surface" m="4">
            <Callout.Icon>
              <ExclamationTriangleIcon />
            </Callout.Icon>
            <Callout.Text>Couldn&apos;t load ingredients. Try again shortly.</Callout.Text>
          </Callout.Root>
        )}

        {!isLoading && !isError && groups.length === 0 && (
          <div className="ingredients-page__empty">
            <Text color="gray" size="2">
              {ingredients && ingredients.length > 0 ? 'No ingredients match your search.' : 'No ingredients yet.'}
            </Text>
          </div>
        )}

        {!isLoading &&
          !isError &&
          groups.map((group) => (
            <section key={group.category} className="ingredients-page__group">
              <Text as="p" size="1" weight="bold" color="gray" className="ingredients-page__group-header">
                {capitalize(group.category)} &middot; {group.items.length}
              </Text>
              <ul className="ingredients-page__rows">
                {group.items.map((ingredient) => (
                  <li key={ingredient.id}>
                    <SheetLink to={`/ingredients/${ingredient.id}`} className="ingredients-page__row">
                      <Text as="span" size="2">
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
