import { Text } from '@radix-ui/themes';
import type { ReactNode } from 'react';
import './CategoryBand.css';

interface CategoryBandProps {
  children: ReactNode;
  /** Extra class on the sticky strip, for a page's own side padding. */
  className?: string;
}

/**
 * A category heading in a list grouped by category (the shopping list, the
 * Ingredients list): a soft accent pill with a small-caps label, in a strip
 * of the page colour that sticks just under the screen's pinned top until
 * the next category pushes it away. The strip, not the pill, is the sticky
 * element, so rows sliding underneath don't show through the pill's rounded
 * corners or the gap below it.
 */
export function CategoryBand({ children, className }: CategoryBandProps) {
  return (
    <div className={`category-band${className ? ` ${className}` : ''}`}>
      <Text size="2" weight="medium" className="category-band__title">
        {children}
      </Text>
    </div>
  );
}
