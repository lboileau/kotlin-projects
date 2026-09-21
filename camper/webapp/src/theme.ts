import type { ThemeProps } from '@radix-ui/themes';

/**
 * Single swap point for the app's accent color. The plan calls for
 * comparing violet / purple / iris on the first built screen — change
 * this one constant to try another. Custom CSS should reference the
 * generic `--accent-*` tokens (never `--violet-*` etc.) so it follows
 * this swap automatically, the same way Radix's own components do.
 *
 * The accent is the one colour for everything that can be tapped, in every
 * tab. What differs per tab is the header's colour: see styles/areas.css.
 */
export const ACCENT_COLOR: ThemeProps['accentColor'] = 'violet';
