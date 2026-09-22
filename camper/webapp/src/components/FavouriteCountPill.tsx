import { HeartGlyph } from './HeartGlyph';
import { favouritesCountLabel } from '../lib/recipeFavorites';
import './FavouriteCountPill.css';

interface FavouriteCountPillProps {
  count: number;
  favouritedByMe: boolean;
}

/**
 * A recipe's favourite count as a small pill for list rows — a fact about
 * the row, not a control (favouriting is done on the recipe page). Renders
 * nothing at zero. Solid when you are one of the favouriters, soft when it
 * is only other people, so your own stand out down a list. `role="img"`
 * with the spelt-out label reads as "3 favourites, including you" instead
 * of a stray number, and keeps it out of the tab order.
 */
export function FavouriteCountPill({ count, favouritedByMe }: FavouriteCountPillProps) {
  if (count <= 0) return null;
  return (
    <span
      className={`favourite-count-pill${favouritedByMe ? ' favourite-count-pill--mine' : ''}`}
      role="img"
      aria-label={favouritesCountLabel(count, favouritedByMe)}
    >
      <HeartGlyph size={13} />
      {count}
    </span>
  );
}
