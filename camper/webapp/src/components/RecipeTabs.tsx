import { Tabs } from '@radix-ui/themes';
import type { RecipeTab } from '../lib/recipeSteps';
import './RecipeTabs.css';

interface RecipeTabsListProps {
  /** A small count after a tab's name when there is something there. */
  counts?: Partial<Record<RecipeTab, number>>;
  /** A tab that exists but can't be used yet (Photos on a recipe not yet saved). */
  disabled?: RecipeTab[];
}

/**
 * The Ingredients · Instructions · Photos tab row, the same on the recipe
 * page and on the edit/new form so the two screens read as one thing. Pair
 * with a `Tabs.Root` whose value is a `RecipeTab` (see `parseRecipeTab`) and
 * `Tabs.Content` panels for the three values.
 */
export function RecipeTabsList({ counts = {}, disabled = [] }: RecipeTabsListProps) {
  const tab = (value: RecipeTab, label: string) => (
    <Tabs.Trigger value={value} disabled={disabled.includes(value)}>
      {label}
      {(counts[value] ?? 0) > 0 && <span className="recipe-tabs__count">{counts[value]}</span>}
    </Tabs.Trigger>
  );
  return (
    <Tabs.List size="2" className="recipe-tabs">
      {tab('ingredients', 'Ingredients')}
      {tab('instructions', 'Instructions')}
      {tab('photos', 'Photos')}
    </Tabs.List>
  );
}
