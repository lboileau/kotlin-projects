import { useNavigate } from 'react-router-dom';
import { SegmentedControl } from '@radix-ui/themes';
import './RecipesIngredientsToggle.css';

/**
 * The Recipes/Ingredients segmented control shown atop both `RecipesPage`
 * and `IngredientsPage`. The two are siblings, both root screens of the
 * Recipes tab, so switching replaces the history entry: Back leaves the pair
 * instead of bouncing between them.
 */
export function RecipesIngredientsToggle({ active }: { active: 'recipes' | 'ingredients' }) {
  const navigate = useNavigate();

  return (
    <SegmentedControl.Root
      size="3"
      className="recipes-ingredients-toggle"
      value={active}
      onValueChange={(next) => {
        if (next !== active) navigate(next === 'ingredients' ? '/ingredients' : '/recipes', { replace: true });
      }}
    >
      <SegmentedControl.Item value="recipes" aria-current={active === 'recipes' ? 'page' : undefined}>
        Recipes
      </SegmentedControl.Item>
      <SegmentedControl.Item value="ingredients" aria-current={active === 'ingredients' ? 'page' : undefined}>
        Ingredients
      </SegmentedControl.Item>
    </SegmentedControl.Root>
  );
}
