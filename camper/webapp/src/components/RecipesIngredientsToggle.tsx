import { useNavigate } from 'react-router-dom';
import { SegmentedControl } from '@radix-ui/themes';

/** The Recipes/Ingredients segmented control shown atop both `RecipesPage` and `IngredientsPage`. */
export function RecipesIngredientsToggle({ active }: { active: 'recipes' | 'ingredients' }) {
  const navigate = useNavigate();

  return (
    <SegmentedControl.Root
      value={active}
      onValueChange={(next) => {
        if (next !== active) navigate(next === 'ingredients' ? '/ingredients' : '/recipes');
      }}
    >
      <SegmentedControl.Item value="recipes">Recipes</SegmentedControl.Item>
      <SegmentedControl.Item value="ingredients">Ingredients</SegmentedControl.Item>
    </SegmentedControl.Root>
  );
}
