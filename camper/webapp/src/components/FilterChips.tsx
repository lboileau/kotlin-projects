import { useLayoutEffect, useRef, useState } from 'react';
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
 * active chip goes back to All. The row fades out at whichever edge has more
 * chips beyond it, so it's plain that it scrolls.
 *
 * Not `CategoryChips`, which is a form field for choosing an ingredient's
 * category; this filters a list.
 */
export function FilterChips({ label, options, value, onChange }: FilterChipsProps) {
  const rowRef = useRef<HTMLDivElement>(null);
  const edges = useScrollEdges(rowRef, options);

  return (
    <div
      className={`filter-chips${edges.left ? ' filter-chips--more-left' : ''}${edges.right ? ' filter-chips--more-right' : ''}`}
    >
      <div ref={rowRef} className="filter-chips__row" role="group" aria-label={label}>
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
    </div>
  );
}

/** Whether a sideways scroller has more content past its left / right edge. */
function useScrollEdges(ref: React.RefObject<HTMLElement | null>, contents: unknown) {
  const [edges, setEdges] = useState({ left: false, right: false });

  useLayoutEffect(() => {
    const row = ref.current;
    if (!row) return;
    const measure = () => {
      const left = row.scrollLeft > 1;
      const right = row.scrollLeft + row.clientWidth < row.scrollWidth - 1;
      setEdges((prev) => (prev.left === left && prev.right === right ? prev : { left, right }));
    };
    measure();
    row.addEventListener('scroll', measure, { passive: true });
    const observer = new ResizeObserver(measure);
    observer.observe(row);
    return () => {
      row.removeEventListener('scroll', measure);
      observer.disconnect();
    };
    // Re-measure when the chips change (only categories that have ingredients are shown).
  }, [ref, contents]);

  return edges;
}
