import { useState } from 'react';
import { HeartGlyph } from '../../components/HeartGlyph';
import './FavouriteButton.css';

interface FavouriteButtonProps {
  favourited: boolean;
  onToggle: (next: boolean) => void;
}

/**
 * The recipe page's favourite toggle. The two states are meant to be
 * unmistakable at a glance: an empty ring with an outline heart, or a solid
 * accent disc with a white heart and a glow. A plain icon button that only
 * swapped a 15px outline heart for a filled one was built first, and the
 * owner found it far too subtle — don't simplify it back. Accent only, no
 * red: red means "take it away" in this app.
 *
 * Favouriting pops the heart and sends a ring outwards. That plays only for
 * a tap that turns it on — `justFavourited` is set by the click and cleared
 * when the animation ends — so neither loading a favourited recipe nor a
 * refetch replays it.
 */
export function FavouriteButton({ favourited, onToggle }: FavouriteButtonProps) {
  const [justFavourited, setJustFavourited] = useState(false);

  return (
    <button
      type="button"
      className={`favourite-button${favourited ? ' favourite-button--on' : ''}${justFavourited && favourited ? ' favourite-button--pop' : ''}`}
      aria-pressed={favourited}
      aria-label={favourited ? 'Remove from favourites' : 'Favourite'}
      onClick={() => {
        setJustFavourited(!favourited);
        onToggle(!favourited);
      }}
    >
      <span className="favourite-button__heart" onAnimationEnd={() => setJustFavourited(false)}>
        <HeartGlyph filled={favourited} size={22} />
      </span>
    </button>
  );
}
