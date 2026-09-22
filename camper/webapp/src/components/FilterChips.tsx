import './FilterChips.css';

export interface FilterChipOption {
  value: string;
  label: string;
}

interface FilterChipsProps {
  /** What the row filters by, for assistive tech ("Filter by category"). */
  label: string;
  options: FilterChipOption[];
  /** The selected option's value; null is "All". */
  value: string | null;
  onChange: (value: string | null) => void;
}

/**
 * A one-line, sideways-scrolling row of filter chips under a list's search
 * box: "All" and then one chip per option, one active at a time; tapping the
 * active chip goes back to All. The same look as the Recipes list's meal
 * chips, which predate this component and still carry their own copy of it
 * (`RecipesPage`): move them onto this when that page is next touched.
 *
 * Not `CategoryChips`, which is a form field for choosing an ingredient's
 * category; this filters a list.
 */
export function FilterChips({ label, options, value, onChange }: FilterChipsProps) {
  return (
    <div className="filter-chips" role="group" aria-label={label}>
      <button
        type="button"
        className={`filter-chips__chip${value === null ? ' filter-chips__chip--active' : ''}`}
        aria-pressed={value === null}
        onClick={() => onChange(null)}
      >
        All
      </button>
      {options.map((option) => (
        <button
          key={option.value}
          type="button"
          className={`filter-chips__chip${value === option.value ? ' filter-chips__chip--active' : ''}`}
          aria-pressed={value === option.value}
          onClick={() => onChange(value === option.value ? null : option.value)}
        >
          {option.label}
        </button>
      ))}
    </div>
  );
}
