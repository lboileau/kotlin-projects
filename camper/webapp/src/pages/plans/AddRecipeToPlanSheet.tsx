import { useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Badge, Button, Skeleton, Text, TextField } from '@radix-ui/themes';
import { CheckIcon, MagnifyingGlassIcon } from '@radix-ui/react-icons';
import { Sheet } from '../../components/Sheet';
import { useCloseSheet } from '../../components/useCloseSheet';
import { useAuth } from '../../auth/useAuth';
import { useRecipes } from '../../queries/recipes';
import type { RecipeResponse } from '../../api/recipes';
import { useAddRecipeToPlan, usePlan } from '../../queries/plans';
import { recipeIdsInPlan } from '../../lib/flatPlan';
import { toast } from '../../lib/toastStore';
import './AddRecipeToPlanSheet.css';

export function AddRecipeToPlanSheet() {
  const { planId } = useParams<{ planId: string }>();
  const closeSheet = useCloseSheet(`/plans/${planId}`);
  const { user } = useAuth();

  const { data: recipes, isLoading } = useRecipes();
  const { data: plan } = usePlan(planId);
  const addRecipe = useAddRecipeToPlan(planId);

  const [query, setQuery] = useState('');

  const alreadyAdded = useMemo(() => (plan ? recipeIdsInPlan(plan) : new Set<string>()), [plan]);

  const visibleRecipes = useMemo(() => {
    if (!recipes) return [];
    const mine = recipes.filter((recipe) => recipe.status === 'published' || recipe.createdBy === user?.id);
    const q = query.trim().toLowerCase();
    const filtered = q ? mine.filter((recipe) => recipe.name.toLowerCase().includes(q)) : mine;
    return filtered.slice().sort((a, b) => a.name.localeCompare(b.name));
  }, [recipes, user, query]);

  function handleAdd(recipe: RecipeResponse) {
    if (!planId) return;
    addRecipe.mutate(
      {
        planId,
        recipeId: recipe.id,
        recipeName: recipe.name,
        recipeWebLink: recipe.webLink,
        baseServings: recipe.baseServings,
      },
      {
        onSuccess: (result) => {
          if (result.alreadyInPlan) toast.info(`${recipe.name} is already in this plan.`);
        },
      },
    );
  }

  return (
    <Sheet title="Add recipes" onClose={closeSheet} fullHeight>
      <div className="add-recipe-to-plan-sheet">
        <div className="add-recipe-to-plan-sheet__search">
          <TextField.Root
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Search recipes"
            size="3"
            autoFocus
          >
            <TextField.Slot>
              <MagnifyingGlassIcon />
            </TextField.Slot>
          </TextField.Root>
        </div>

        <div className="add-recipe-to-plan-sheet__list">
          {isLoading && (
            <>
              <Skeleton height="52px" />
              <Skeleton height="52px" />
              <Skeleton height="52px" />
            </>
          )}

          {!isLoading && visibleRecipes.length === 0 && (
            <Text color="gray" size="2" className="add-recipe-to-plan-sheet__empty">
              {query ? 'No recipes match.' : 'No recipes yet — create one from the Recipes tab.'}
            </Text>
          )}

          {!isLoading &&
            visibleRecipes.map((recipe) => {
              const added = alreadyAdded.has(recipe.id);
              return (
                <button
                  key={recipe.id}
                  type="button"
                  className="add-recipe-to-plan-sheet__row"
                  disabled={added}
                  onClick={() => handleAdd(recipe)}
                >
                  <span>{recipe.name}</span>
                  {added ? (
                    <Badge color="green" variant="soft">
                      <CheckIcon /> Added
                    </Badge>
                  ) : (
                    recipe.status === 'draft' && <Badge variant="soft">Draft</Badge>
                  )}
                </button>
              );
            })}
        </div>

        <div className="add-recipe-to-plan-sheet__footer">
          <Button variant="solid" size="3" onClick={closeSheet} className="add-recipe-to-plan-sheet__done">
            Done
          </Button>
        </div>
      </div>
    </Sheet>
  );
}
