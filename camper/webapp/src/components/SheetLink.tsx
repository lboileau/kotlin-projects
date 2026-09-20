import { forwardRef } from 'react';
import { Link, type LinkProps } from 'react-router-dom';

/**
 * A Link that opens a sheet (a child route rendered over its parent
 * page). Semantically distinct from a regular navigation Link so sheet
 * triggers are easy to find in usage; plain `<Link>` behavior underneath
 * is enough for `useSheet`'s history-index heuristic to detect
 * in-app navigation.
 */
export const SheetLink = forwardRef<HTMLAnchorElement, LinkProps>(function SheetLink(
  { children, ...props },
  ref,
) {
  return (
    <Link ref={ref} {...props}>
      {children}
    </Link>
  );
});
