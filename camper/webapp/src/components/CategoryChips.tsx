import { CATEGORIES, capitalize } from '../lib/ingredientConstants';
import './CategoryChips.css';

interface CategoryChipsProps {
  /** '' when nothing is chosen yet. */
  value: string;
  onChange: (category: string) => void;
  /** Id of the visible label, for the group's accessible name. */
  labelledBy?: string;
  invalid?: boolean;
}

/**
 * An ingredient's category as a row of chips: every option visible and one
 * tap, where the old Select took three (open, scroll, tap). It has no
 * default on purpose — with "Other" pre-selected and Enter submitting, fast
 * entry filled the shared ingredient list with uncategorised items, and the
 * shopping list groups by category.
 */
export function CategoryChips({ value, onChange, labelledBy, invalid }: CategoryChipsProps) {
  return (
    <div className="category-chips" role="radiogroup" aria-labelledby={labelledBy} aria-invalid={invalid || undefined}>
      {CATEGORIES.map((category) => (
        <button
          key={category}
          type="button"
          role="radio"
          aria-checked={value === category}
          className={`category-chips__chip${value === category ? ' category-chips__chip--active' : ''}`}
          onClick={() => onChange(category)}
        >
          {capitalize(category)}
        </button>
      ))}
    </div>
  );
}
