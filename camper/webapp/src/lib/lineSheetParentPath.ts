/**
 * `AddLineSheet`/`EditLineSheet` are mounted as children of two different
 * parent routes — the recipe detail page and the edit page (see
 * `router.tsx`) — so they can't close back to a single hardcoded parent
 * path. Stripping the `/lines/new` or `/lines/:lineId` suffix off the
 * current location gives whichever parent actually rendered it.
 */
export function lineSheetParentPath(pathname: string): string {
  return pathname.replace(/\/lines\/[^/]+\/?$/, '');
}
