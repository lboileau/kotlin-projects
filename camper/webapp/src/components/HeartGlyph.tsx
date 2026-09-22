interface HeartGlyphProps {
  /** Filled means favourited, everywhere in the app; the outline is only for the recipe page's empty button. */
  filled?: boolean;
  size?: number;
  className?: string;
}

/**
 * The app's heart: fuller and rounder than the 15px icon-set one, and one
 * shape for both states so the outline and the filled heart line up exactly
 * when the favourite button switches between them. Decorative — whatever
 * shows it carries the accessible text. Colour comes from `currentColor`.
 */
export function HeartGlyph({ filled = true, size = 16, className }: HeartGlyphProps) {
  return (
    <svg
      className={className}
      width={size}
      height={size}
      viewBox="0 0 24 24"
      aria-hidden="true"
      focusable="false"
      fill={filled ? 'currentColor' : 'none'}
      stroke="currentColor"
      strokeWidth={filled ? 0 : 1.9}
      strokeLinejoin="round"
    >
      <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z" />
    </svg>
  );
}
