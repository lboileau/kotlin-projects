# Orchestrator Handoff

## Workflow
feature-build

## Project Path
/Users/louisboileau/Development/kotlin-projects/camper

## Feature Name
voice-shopping-list

## Plan
to be created by architect

## Feature Description

Let the user add several items to a plan's shopping list in one go by dictating them, using the
**phone keyboard's built-in dictation mic** (iOS / Android). Nothing paid, no speech API, no new
permissions: the app only provides a good place to dictate into and turns one stream of text into
several manual shopping items.

The owner uses the app primarily on their phone. The Web Speech API (`SpeechRecognition`) was
considered and **rejected**: unreliable on iOS (stops on pauses, historically broken in home-screen
web apps), needs HTTPS for phone testing. Do not use it.

### User flow

1. On the shopping list (`/plans/:planId/shopping`), a mic icon button sits in the bottom quick-add
   bar next to the existing input and "+" button
   (`webapp/src/pages/shopping/ShoppingPage.tsx:267-283`, inside `<BottomBar>`).
2. Tapping it opens a **sheet at a child route** `/plans/:planId/shopping/dictate` (sheets are always
   child routes rendered through the parent's `<Outlet/>`, never local state — `webapp/CLAUDE.md`).
   Register it next to the existing `switch` and `add` children of the shopping route.
3. The sheet contains:
   - an autofocused multi-line `<textarea>` (so the keyboard opens immediately, same pattern as the
     other sheets with an autofocused input, e.g. `NewIngredientSheet`);
   - a short hint: tap the mic on your keyboard and list your items; pause or say "comma" / "and"
     between items. A web page cannot start keyboard dictation itself, so the hint matters;
   - a **live preview** of the parsed items as chips, updating as text streams in;
   - tapping a chip removes that item from what will be added;
   - one primary button: **"Add N items"** (disabled at 0).
4. On confirm, every remaining item is added as a free-text manual item, the sheet closes back to the
   list (`useSheet(parentPath).close()`), and an info toast says "Added N items".
5. Typing by hand into the textarea works identically (one item per line or comma-separated), so this
   doubles as a paste / multi-add box.

### Parsing — `splitDictation` (pure function, the core of the feature)

New file `webapp/src/lib/splitDictation.ts` with a Vitest suite alongside
(`webapp/src/lib/splitDictation.test.ts`; Vitest is already set up — `package.json` `"test": "vitest run"`,
existing example `src/lib/mealPlanSummary.test.ts`).

Signature roughly: `splitDictation(text: string, knownNames: string[]): string[]`.

Rules, in order:
1. Hard separators always split: newline, comma, semicolon, period / full stop (but not a decimal
   point inside a number such as "1.5 litres").
2. Within each segment, **protect known ingredient names** before splitting on " and ": if the segment
   contains a known name that itself contains "and" (e.g. "mac and cheese", "salt and pepper" when in
   the catalogue), that span is kept whole. Match case-insensitively on word boundaries, longest
   name first.
3. Split the rest on the standalone word "and" (also "&", and a leading "and" as in ", and bread").
4. **Separator-less runs**: for a segment with no separators (Android often gives "milk eggs bread"),
   greedily match known ingredient names (longest first, word boundaries, case-insensitive, tolerate a
   trailing plural "s"/"es"). Only segment when the **entire** run is covered by known names plus
   optional leading quantity words (digits, "a", "an", "two", "a dozen"...) attached to the
   name that follows them. If any word is left unexplained, leave the segment as one item — the user
   sees it in the preview and can fix the text. Never guess.
5. Clean each item: trim, collapse whitespace, strip leading fillers, drop empties, capitalise the
   first letter. **Amended 2026-09-21 by the owner after trying the first build:** filler words are
   stripped, "some" included ("some milk" -> "Milk"), along with spoken fillers ("um", "the",
   "please", "get", "buy"...) and leading phrases ("I need", "we also need", "don't forget"...).
   `FILLERS` / `FILLER_PHRASES` in `splitDictation.ts` are the source of truth. The original text of
   this rule said "some" is NOT stripped; that no longer holds.
6. De-duplicate case-insensitively within one dictation, keeping the first occurrence.

Items stay **free text**: "two avocados" is added as the description "Two avocados". No quantity or
unit parsing, no linking to ingredient ids.

Test cases must include at least: commas; newlines; periods vs "1.5 litres"; "milk and eggs";
"mac and cheese" with and without that name in the catalogue; ", and bread"; "milk eggs bread" with
all three known; "milk eggs dragonfruit" with one unknown (stays one item); quantities
("two avocados and a dozen eggs"); duplicates; empty / whitespace-only input; iOS-style
auto-punctuated text ("Milk, eggs, and bread.").

The known names come from the existing ingredient catalogue: `useIngredients()`
(`webapp/src/queries/ingredients.ts:17`) → `GET /api/ingredients`, full unpaginated list of
`IngredientResponse { id, name, category, defaultUnit }` (`webapp/src/api/ingredients.ts:24-27`).
The sheet must work while that query is loading or failed (parse with an empty catalogue).

### Adding the items — frontend only, no backend change

Verified: the existing optimistic hook already supports several concurrent adds.
`useAddManualShoppingItem` (`webapp/src/queries/shopping.ts:139-171`) mints a unique
`temp-${crypto.randomUUID()}` per call, replaces/removes exactly its own temp row on
success/error, and `onSettled: invalidateIfLast(...)` only refetches once the last in-flight
mutation for the plan settles. It posts to `POST /api/meal-plans/{id}/shopping-list/items`
(`MealPlanController.kt:296-315`) with `{ description }` (`webapp/src/api/shopping.ts:78`).

So the sheet calls `addManualItem.mutate(text)` once per item, in order. **Do not add a batch
endpoint** — it is not needed for the ~5-15 items a dictation produces, and keeps this feature
frontend-only. Failed items already surface via that hook's own error toast and the temp row is
removed; error toasts of the same tone replace each other (`lib/toastStore.ts`), so there is no pile-up.

## Entities
None new. Reuses manual shopping items (`ManualShoppingItemResponse`, `webapp/src/api/shopping.ts:41`)
and the ingredient catalogue (read-only, for parsing).

## API Surface
No new endpoints. Uses existing:
- `POST /api/meal-plans/{id}/shopping-list/items` body `{ description: string }` → 201 `ManualItemResponse`
- `GET /api/ingredients` → `IngredientResponse[]`

## Database Changes
None.

## Special Considerations

- **Never write to the textarea's value programmatically while the user is dictating.** On iOS,
  changing a focused field's value from code (trimming, normalising, removing a chip's text) ends the
  dictation session. The textarea is a plain controlled input that only echoes what the user/keyboard
  produced. Consequently **chip removal must not edit the text**: keep a set of excluded items
  (keyed by normalised item text) in state and filter the parsed list through it.
- The preview and the "Add N items" button must stay visible **above the on-screen keyboard** on iOS.
  Follow what the existing sheets with inputs do and respect `lib/viewportGuard.ts`; cap the
  textarea height and let the chip area scroll inside the sheet.
- Textarea attributes: `autoCapitalize="sentences"`, `autoCorrect="on"`, `enterKeyHint="enter"`
  (Enter inserts a newline here = next item; it must NOT submit), `rows` ~3.
- Follow the sheet rules in `webapp/CLAUDE.md`: `components/Sheet` + `useSheet(parentPath)`, the only
  way to close; sheets never change the tab; page areas are one lazy chunk per `pages/{area}/index.ts`
  barrel, so export the new sheet from `pages/shopping/index.ts`.
- `useSheet`'s `canClose` is not needed: the adds are optimistic and the sheet closes immediately.
- The mic button needs an `aria-label` ("Dictate items") and must not shrink the quick-add input
  below a usable width on a 320px-wide screen.
- Live updates: each successful add already publishes `shopping-list updated` from the controller;
  nothing to do.
- Accessibility: chips are buttons with `aria-label="Remove <item>"`; the item count is announced via
  the button label.
- Update `webapp/CLAUDE.md` (doc-updater) with the "never mutate a dictating field" rule and the new
  route.

## Notes

- **Branch: `meal-app-frontend`. The working tree has a large set of UNCOMMITTED changes across
  `webapp/` (≈50 modified files, including `ShoppingPage.tsx`, `ShoppingPage.css`, `BottomBar.tsx`,
  `useSheet.ts`, `webapp/CLAUDE.md`).** They are the owner's in-progress work. Do not stash, reset,
  checkout or otherwise discard them; build on top of the files as they are on disk, and check with
  `git status` before any PR-stack operation that would move them.
- Web-only feature: architect → web-dev → test-engineer (Vitest for `splitDictation`, plus component
  coverage in whatever style the webapp already uses) → code-reviewer and test-reviewer. No
  kotlin-dev or db-dev work. Always run both reviewers.
- Real dictation cannot be automated; final verification is the owner trying it on their phone
  (iOS keyboard mic). Typing/pasting the same text exercises the identical code path.
- Save `plan.md` and `retro.md` alongside this file in `docs/voice-shopping-list/`.

## Amendments after the first build (owner requests, 2026-09-21)

1. **Filler words are stripped**, "some" included — see parsing rule 5 above.
2. **Inline several-items mode in the quick-add bar.** The owner asked for the normal input to switch
   to the dictation mode by itself. A page cannot detect the keyboard's mic, so the quick-add bar
   goes by content: once its text parses into two or more items it shows the chips above the field
   and the "+" becomes "Add N", in place, without moving focus (moving focus ends an iOS dictation).
   "Add as one item instead" is the way out of a wrong split. The quick-add field became a
   one-line textarea that grows through CSS only.
3. **Manual-item adds are sent one at a time per plan** so a dictated list keeps its spoken order.

4. **The mic button and the dictate sheet are removed** (owner: "only a single flow"). The route
   `/plans/:planId/shopping/dictate`, `DictateItemsSheet` and `MicIcon` no longer exist; the user
   flow and sheet considerations above describe the first build, not the current app. The quick-add
   bar is the one place items are added, one or several.
