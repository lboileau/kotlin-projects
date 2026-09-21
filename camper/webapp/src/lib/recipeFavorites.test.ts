import { describe, it, expect } from 'vitest';
import {
  applyFavoriteToDetail,
  applyFavoriteToList,
  applyFavoriteToPeople,
  favouritesCountLabel,
  formatFavouritedBy,
  matchesShowFilter,
  parseShowFilter,
  setFavorite,
  type FavouritableRecipe,
  type FavouritePerson,
  type RecipeShowFilter,
} from './recipeFavorites';

const ME = 'user-me';
const SOMEONE_ELSE = 'user-other';

function person(userId: string, username: string): FavouritePerson {
  return { userId, username };
}

function makeRecipe(overrides: Partial<FavouritableRecipe> = {}): FavouritableRecipe {
  return {
    id: 'recipe-1',
    createdBy: ME,
    favoriteCount: 0,
    favoritedByMe: false,
    ...overrides,
  };
}

describe('parseShowFilter', () => {
  it('reads an absent param as all', () => {
    expect(parseShowFilter(null)).toBe('all');
  });

  it('reads each valid value', () => {
    expect(parseShowFilter('all')).toBe('all');
    expect(parseShowFilter('mine')).toBe('mine');
    expect(parseShowFilter('favourites')).toBe('favourites');
    expect(parseShowFilter('my-favourites')).toBe('my-favourites');
  });

  it('reads the dead ?mine=1 value as all', () => {
    // The old Mine switch wrote `?mine=1`; that param is gone, and a `show`
    // of "1" (or anything else unrecognised) must not blank the list.
    expect(parseShowFilter('1')).toBe('all');
  });

  it('reads an arbitrary string as all', () => {
    expect(parseShowFilter('favorites')).toBe('all');
    expect(parseShowFilter('')).toBe('all');
    expect(parseShowFilter('MINE')).toBe('all');
  });
});

describe('matchesShowFilter', () => {
  // Every combination of (created by me / by someone else) x (0 / >0 count)
  // x (favoritedByMe true / false), against each of the four filters.
  const combos = [ME, SOMEONE_ELSE].flatMap((createdBy) =>
    [0, 3].flatMap((favoriteCount) =>
      [false, true].map((favoritedByMe) => makeRecipe({ createdBy, favoriteCount, favoritedByMe })),
    ),
  );

  it('all matches everything', () => {
    for (const recipe of combos) {
      expect(matchesShowFilter(recipe, 'all', ME)).toBe(true);
    }
  });

  it('mine matches only the signed-in user’s own recipes', () => {
    for (const recipe of combos) {
      expect(matchesShowFilter(recipe, 'mine', ME)).toBe(recipe.createdBy === ME);
    }
  });

  it('favourites matches anything anyone has favourited', () => {
    for (const recipe of combos) {
      expect(matchesShowFilter(recipe, 'favourites', ME)).toBe(recipe.favoriteCount > 0);
    }
  });

  it('my-favourites matches only what the signed-in user favourited', () => {
    for (const recipe of combos) {
      expect(matchesShowFilter(recipe, 'my-favourites', ME)).toBe(recipe.favoritedByMe);
    }
  });

  it('matches nothing under mine or my-favourites with no signed-in user', () => {
    for (const recipe of combos) {
      expect(matchesShowFilter(recipe, 'mine', undefined)).toBe(false);
      expect(matchesShowFilter(recipe, 'my-favourites', undefined)).toBe(false);
    }
  });

  it('still matches everything under all and favourites with no signed-in user', () => {
    const favourited = makeRecipe({ favoriteCount: 2, favoritedByMe: true });
    expect(matchesShowFilter(favourited, 'all', undefined)).toBe(true);
    expect(matchesShowFilter(favourited, 'favourites', undefined)).toBe(true);
  });

  it('treats an unknown filter as all', () => {
    // parseShowFilter is the only producer, but the predicate must not
    // silently hide the library if a new value ever reaches it.
    const unknownFilter = 'nonsense' as unknown as RecipeShowFilter;
    expect(matchesShowFilter(makeRecipe(), unknownFilter, ME)).toBe(true);
  });
});

describe('setFavorite', () => {
  it('false to true flips the flag and increments the count', () => {
    const before = makeRecipe({ favoriteCount: 2, favoritedByMe: false });
    const after = setFavorite(before, true);
    expect(after.favoritedByMe).toBe(true);
    expect(after.favoriteCount).toBe(3);
    expect(before.favoriteCount).toBe(2); // input untouched
  });

  it('true to false flips the flag and decrements the count', () => {
    const after = setFavorite(makeRecipe({ favoriteCount: 3, favoritedByMe: true }), false);
    expect(after.favoritedByMe).toBe(false);
    expect(after.favoriteCount).toBe(2);
  });

  it('returns the identical object when already in the target state', () => {
    // This is what makes a rollback against the CURRENT cache safe: it can
    // never double-count if something already brought the row back.
    const on = makeRecipe({ favoriteCount: 1, favoritedByMe: true });
    expect(setFavorite(on, true)).toBe(on);
    const off = makeRecipe({ favoriteCount: 0, favoritedByMe: false });
    expect(setFavorite(off, false)).toBe(off);
  });

  it('never takes the count below zero', () => {
    // A stale cache saying "you favourited it" while the count reads 0.
    const after = setFavorite(makeRecipe({ favoriteCount: 0, favoritedByMe: true }), false);
    expect(after.favoriteCount).toBe(0);
  });

  it('keeps every other field', () => {
    const extra = { ...makeRecipe(), name: 'Camp Guacamole' };
    expect(setFavorite(extra, true).name).toBe('Camp Guacamole');
  });
});

describe('applyFavoriteToList', () => {
  const list = [
    makeRecipe({ id: 'a', favoriteCount: 1, favoritedByMe: false }),
    makeRecipe({ id: 'b', favoriteCount: 5, favoritedByMe: true }),
  ];

  it('edits only the matching row and leaves the others referentially equal', () => {
    const next = applyFavoriteToList(list, 'a', true)!;
    expect(next[0]).toEqual({ ...list[0], favoriteCount: 2, favoritedByMe: true });
    expect(next[1]).toBe(list[1]);
  });

  it('no-ops for an id that is not in the list', () => {
    const next = applyFavoriteToList(list, 'missing', true)!;
    expect(next[0]).toBe(list[0]);
    expect(next[1]).toBe(list[1]);
  });

  it('returns undefined for an undefined list', () => {
    expect(applyFavoriteToList(undefined, 'a', true)).toBeUndefined();
  });

  it('round-trips: apply then roll back restores the original counts and flags', () => {
    // The property the mutation's onError relies on — the inverse edit is
    // applied to whatever is in the cache at that moment, not a snapshot.
    const applied = applyFavoriteToList(list, 'a', true);
    const rolledBack = applyFavoriteToList(applied, 'a', false)!;
    expect(rolledBack).toEqual(list);
  });

  it('round-trips an un-favourite the same way', () => {
    const applied = applyFavoriteToList(list, 'b', false);
    const rolledBack = applyFavoriteToList(applied, 'b', true)!;
    expect(rolledBack).toEqual(list);
  });

  it('a rollback applied twice does not double-count', () => {
    const applied = applyFavoriteToList(list, 'a', true);
    const once = applyFavoriteToList(applied, 'a', false)!;
    const twice = applyFavoriteToList(once, 'a', false)!;
    expect(twice).toEqual(list);
  });
});

describe('applyFavoriteToDetail', () => {
  const detail = makeRecipe({ id: 'a', favoriteCount: 4, favoritedByMe: false });

  it('edits a matching detail', () => {
    expect(applyFavoriteToDetail(detail, 'a', true)).toEqual({
      ...detail,
      favoriteCount: 5,
      favoritedByMe: true,
    });
  });

  it('no-ops on a different id', () => {
    expect(applyFavoriteToDetail(detail, 'b', true)).toBe(detail);
  });

  it('returns undefined for an undefined detail', () => {
    expect(applyFavoriteToDetail(undefined, 'a', true)).toBeUndefined();
  });

  it('round-trips: apply then roll back restores the original', () => {
    const applied = applyFavoriteToDetail(detail, 'a', true);
    expect(applyFavoriteToDetail(applied, 'a', false)).toEqual(detail);
  });
});

describe('applyFavoriteToPeople', () => {
  const me = person(ME, 'You-the-user');
  const alice = person('user-alice', 'Alice');

  it('appends the viewer to the end, keeping the oldest-first order', () => {
    expect(applyFavoriteToPeople([alice], me, true)).toEqual([alice, me]);
  });

  it('seeds an absent cache on an add, so the first favourite reads "You" at once', () => {
    expect(applyFavoriteToPeople(undefined, me, true)).toEqual([me]);
  });

  it('leaves an absent cache alone on a remove', () => {
    expect(applyFavoriteToPeople(undefined, me, false)).toBeUndefined();
  });

  it('removes the viewer and leaves everyone else', () => {
    expect(applyFavoriteToPeople([alice, me], me, false)).toEqual([alice]);
  });

  it('returns the identical array when already in the target state', () => {
    // The same guard as setFavorite: what makes a rollback against the
    // CURRENT cache safe rather than a duplicate or an over-removal.
    const withMe = [alice, me];
    expect(applyFavoriteToPeople(withMe, me, true)).toBe(withMe);
    const withoutMe = [alice];
    expect(applyFavoriteToPeople(withoutMe, me, false)).toBe(withoutMe);
  });

  it('round-trips: apply then roll back restores the list', () => {
    const applied = applyFavoriteToPeople([alice], me, true);
    expect(applyFavoriteToPeople(applied, me, false)).toEqual([alice]);
  });

  it('re-adds the viewer to a list a racing fetch emptied, which is the 0 -> 1 case', () => {
    // onSuccess re-applies the edit against the server's answer: on the
    // 0 -> 1 tap the people query mounts mid-flight and can overwrite the
    // seeded [me] with the [] it fetched before the write landed.
    expect(applyFavoriteToPeople([], me, true)).toEqual([me]);
  });

  it('is a no-op when the re-apply finds the cache already correct', () => {
    // The same re-apply running over a cache the fetch got right must not
    // add a second copy of the viewer, in either direction.
    const seeded = [me];
    expect(applyFavoriteToPeople(seeded, me, true)).toBe(seeded);
    const emptied: typeof seeded = [];
    expect(applyFavoriteToPeople(emptied, me, false)).toBe(emptied);
  });

  it('a rollback applied twice does not remove anyone else', () => {
    const applied = applyFavoriteToPeople([alice], me, true);
    const once = applyFavoriteToPeople(applied, me, false);
    expect(applyFavoriteToPeople(once, me, false)).toEqual([alice]);
  });
});

describe('favouritesCountLabel', () => {
  it('is singular at one and plural otherwise', () => {
    expect(favouritesCountLabel(1)).toBe('1 favourite');
    expect(favouritesCountLabel(3)).toBe('3 favourites');
    expect(favouritesCountLabel(0)).toBe('0 favourites');
    expect(favouritesCountLabel(1, true)).toBe('1 favourite, including you');
    expect(favouritesCountLabel(3, true)).toBe('3 favourites, including you');
  });
});

describe('formatFavouritedBy', () => {
  const alice = person('user-alice', 'Alice');
  const bob = person('user-bob', 'Bob');
  const carol = person('user-carol', 'Carol');
  const me = person(ME, 'louis@example.com');

  it('puts the viewer first, as "You"', () => {
    // The server returns oldest first, so Alice comes back before the
    // viewer; the line still opens with "You".
    expect(formatFavouritedBy(2, [alice, me], ME).text).toBe('2 · You and Alice');
  });

  it('names one person by name', () => {
    expect(formatFavouritedBy(1, [alice], ME).text).toBe('1 · Alice');
  });

  it('names the viewer alone as "You"', () => {
    expect(formatFavouritedBy(1, [me], ME).text).toBe('1 · You');
  });

  it('joins two names with "and"', () => {
    expect(formatFavouritedBy(2, [alice, bob], ME).text).toBe('2 · Alice and Bob');
  });

  it('shows at most two names, then "and 1 other" — singular', () => {
    expect(formatFavouritedBy(3, [me, alice, bob], ME).text).toBe('3 · You, Alice and 1 other');
  });

  it('pluralises "others" past one', () => {
    expect(formatFavouritedBy(5, [alice, bob, carol], ME).text).toBe('5 · Alice, Bob and 3 others');
  });

  it('counts the shortfall when the count is larger than the names returned', () => {
    // The list query lags the recipe's own count (it is a separate fetch,
    // and the toggle keeps the count instantly true). The count wins.
    expect(formatFavouritedBy(9, [alice], ME).text).toBe('9 · Alice and 8 others');
  });

  it('never goes negative when more names come back than the count', () => {
    expect(formatFavouritedBy(1, [alice, bob, carol], ME).text).toBe('1 · Alice and Bob');
  });

  it('falls back to the count alone while the names are still loading', () => {
    expect(formatFavouritedBy(3, undefined, ME).text).toBe('3 people');
    expect(formatFavouritedBy(1, undefined, ME).text).toBe('1 person');
  });

  it('falls back to the count alone when the names request failed or came back empty', () => {
    expect(formatFavouritedBy(3, [], ME).text).toBe('3 people');
  });

  it('falls back to the count alone when every name is blank', () => {
    expect(formatFavouritedBy(2, [person('user-x', ''), person('user-y', '  ')], ME).text).toBe('2 people');
  });

  it('skips a blank name but still counts it among the others', () => {
    expect(formatFavouritedBy(2, [alice, person('user-x', '')], ME).text).toBe('2 · Alice and 1 other');
  });

  it('says nobody is "You" when there is no signed-in viewer', () => {
    expect(formatFavouritedBy(2, [alice, me], undefined).text).toBe('2 · Alice and louis@example.com');
  });

  it('says nobody is "You" when the viewer has not favourited it', () => {
    expect(formatFavouritedBy(2, [alice, bob], ME).text).toBe('2 · Alice and Bob');
  });

  it('is the count alone at zero, so a caller that renders it anyway reads sensibly', () => {
    // The page does not render the line at 0 at all — this only guarantees
    // the function never produces a dangling separator.
    expect(formatFavouritedBy(0, [], ME).text).toBe('0 people');
  });

  it('gives the link an accessible name that says "favourites", not a bare number', () => {
    expect(formatFavouritedBy(3, [me, alice, bob], ME).label).toBe(
      '3 favourites · You, Alice and 1 other',
    );
    expect(formatFavouritedBy(3, undefined, ME).label).toBe('3 favourites');
    expect(formatFavouritedBy(1, [alice], ME).label).toBe('1 favourite · Alice');
  });
});
