import { createContext, useContext } from 'react';

// The current sheet's own `Dialog.Content` DOM node, for portalling a
// Select/DropdownMenu INTO it rather than to `document.body` (see
// `useSheetContainer`). `null` outside any sheet, or before the node has
// mounted — both cases fine, since a `container` of `undefined`/`null`
// makes Radix's Portal fall back to `document.body` on its own. Provided
// by `Sheet.tsx`; kept in its own file (not exported alongside the `Sheet`
// component) so Fast Refresh only ever sees one component per file.
export const SheetPortalContext = createContext<HTMLDivElement | null>(null);

/**
 * The enclosing sheet's content element, to pass as the `container` prop
 * of a `Select.Content` / `DropdownMenu.Content` rendered inside a sheet
 * (Radix Themes forwards `container` to the primitive's own `Portal`).
 * Without this, that popup portals to `document.body` by default — a
 * sibling of the Dialog's own portal, OUTSIDE the subtree that Radix's
 * scroll lock (`react-remove-scroll`, via the Dialog's `shards` option)
 * treats as "inside the dialog": wheel/touch-move over the popup then
 * reads as scrolling the page behind the sheet, which the lock blocks
 * after the first moment. Portalling the popup into the sheet's own
 * content node puts it inside that allowed subtree instead, and it still
 * stacks and positions correctly there — Select/DropdownMenu content
 * uses Radix's default `position="popper"`, positioned via floating-ui
 * with its own `position: fixed`, computed from the trigger's bounding
 * rect independent of where in the DOM tree it's mounted.
 *
 * Returns `null` outside a sheet (e.g. `LinesEditor` on `NewRecipePage`,
 * a plain page) — passed straight through as `container`, which Radix
 * treats the same as omitting the prop, portalling to `document.body`.
 *
 * Must be called from a genuine JSX descendant of `<Sheet>` — see
 * `SheetSelectContent`'s own doc comment for why.
 */
export function useSheetContainer(): HTMLDivElement | null {
  return useContext(SheetPortalContext);
}
