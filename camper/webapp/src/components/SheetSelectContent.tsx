import type { ComponentProps } from 'react';
import { Select } from '@radix-ui/themes';
import { useSheetContainer } from './useSheetContainer';

/**
 * Drop-in replacement for `<Select.Content>` for a `Select` rendered
 * inside a sheet — portals into the sheet's own content node (see
 * `useSheetContainer`) instead of `document.body`. `Dialog`'s scroll
 * lock only recognizes wheel/touch-move as "inside the dialog" within
 * that node's subtree (it's the `shards` element react-remove-scroll is
 * given); a `Select` portalled past it, as a `document.body`-level
 * sibling, is treated as scrolling the page behind the sheet instead —
 * it works for a moment, then the lock's listeners take over and it gets
 * stuck.
 *
 * Must be used as a genuine JSX descendant of `<Sheet>` — e.g. directly
 * inside a page component's own returned `<Sheet>...</Sheet>`, or inside
 * a component (like `IngredientPicker`) that a page renders as a child
 * of `<Sheet>`. It reads nothing useful if called from the same
 * component instance that CREATES the `<Sheet>` element, since that
 * component's own position in the tree is above the sheet, not below
 * it — `useSheetContainer()` only sees a sheet's context from inside it.
 *
 * Outside any sheet (e.g. `LinesEditor` on the plain `NewRecipePage`),
 * `useSheetContainer()` returns `null`, and `Select.Content` treats a
 * `null`/`undefined` `container` the same as omitting the prop —
 * portalling to `document.body` as usual — so this is also safe to use
 * for a `Select` that only sometimes renders inside a sheet.
 */
export function SheetSelectContent(props: ComponentProps<typeof Select.Content>) {
  const container = useSheetContainer();
  return <Select.Content container={container} {...props} />;
}
