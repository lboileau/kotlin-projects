import { useSearchParams } from 'react-router-dom';
import { parseRecipeTab, type RecipeTab } from '../lib/recipeSteps';

/**
 * Which recipe tab is showing, kept in `?tab=` (every screen state has a
 * URL) and written with `replace` so Back leaves the page rather than
 * cycling tabs. Shared by the recipe page and the edit/new form, so a link
 * like `/recipes/:id/edit?tab=instructions` lands on the right tab.
 */
export function useRecipeTab(): [RecipeTab, (next: RecipeTab) => void] {
  const [searchParams, setSearchParams] = useSearchParams();
  const tab = parseRecipeTab(searchParams.get('tab'));
  function setTab(next: RecipeTab) {
    setSearchParams(
      (params) => {
        if (next === 'ingredients') params.delete('tab');
        else params.set('tab', next);
        return params;
      },
      { replace: true },
    );
  }
  return [tab, setTab];
}
