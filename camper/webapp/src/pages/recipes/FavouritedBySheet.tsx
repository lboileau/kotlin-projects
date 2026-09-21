import { useParams } from 'react-router-dom';
import { Skeleton, Text } from '@radix-ui/themes';
import { Sheet } from '../../components/Sheet';
import { useSheet } from '../../components/useSheet';
import { QueryErrorState } from '../../components/QueryErrorState';
import { useRecipeFavorites } from '../../queries/recipes';
import './FavouritedBySheet.css';

/**
 * Who favourited this recipe, oldest first. Rows are plain, non-interactive
 * list items — recipes are one shared library and there is nothing to tap
 * through to. Reached from the recipe page's who-line ("♥ 3 · You, Alice
 * and 1 other ›"), which reads the same query, so opening the sheet
 * normally shows the names it was already summarising rather than a
 * spinner. The empty and loading states are still handled: a deep link
 * lands here with nothing cached.
 */
export function FavouritedBySheet() {
  const { recipeId } = useParams<{ recipeId: string }>();
  const sheet = useSheet(`/recipes/${recipeId}`);
  const { data: people, isLoading, isError, refetch } = useRecipeFavorites(recipeId);
  // Whether there's already data to show, so a background refetch error
  // (window focus) falls through to the list instead of blanking it.
  const hasData = !!people;

  return (
    <Sheet {...sheet.sheetProps} title="Favourited by">
      {isLoading && (
        <div aria-busy="true" aria-label="Loading who favourited this">
          <Skeleton height="48px" aria-hidden="true" />
        </div>
      )}

      {!isLoading && isError && !hasData && (
        <QueryErrorState
          message="Couldn't load who favourited this."
          onRetry={() => void refetch()}
        />
      )}

      {!isLoading && people && people.length === 0 && (
        <Text as="p" color="gray" size="2">
          No one has favourited this yet.
        </Text>
      )}

      {!isLoading && people && people.length > 0 && (
        <ul className="favourited-by-sheet__list">
          {people.map((person) => (
            <li key={person.userId} className="favourited-by-sheet__row">
              <Text as="span" size="2" className="favourited-by-sheet__name">
                {person.username}
              </Text>
              <Text as="span" size="1" color="gray">
                {new Date(person.favoritedAt).toLocaleDateString()}
              </Text>
            </li>
          ))}
        </ul>
      )}
    </Sheet>
  );
}
