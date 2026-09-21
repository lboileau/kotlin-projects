# voice-shopping-list — Implementation Plan

Source of truth for requirements: `docs/voice-shopping-list/handoff.md` (user-approved). This plan
does not revisit its decisions; it turns them into an exact, implementable specification.

---

## 1. Feature summary

Add a **mic button to the shopping list's quick-add bar** that opens a sheet at
`/plans/:planId/shopping/dictate`. The sheet is a place to dictate into with the **phone keyboard's
own mic** (iOS / Android). It parses the one stream of text into several items, previews them as
chips, and on confirm posts each one as a free-text manual shopping item through the existing
`useAddManualShoppingItem` hook.

**Frontend only.** No Kotlin, no SQL, no new endpoints, no new npm dependencies, no Web Speech API.

**Existing API used, unchanged:**

| Method | Path | Body | Use |
| --- | --- | --- | --- |
| `POST` | `/api/meal-plans/{id}/shopping-list/items` | `{ description: string }` | one call per dictated item |
| `GET` | `/api/ingredients` | — | the known-name catalogue for parsing (read-only, already cached) |

### User flow

1. On `/plans/:planId/shopping`, the docked quick-add bar gains a mic icon button between the text
   field and the `+` submit button.
2. Tapping it navigates to the child route `/plans/:planId/shopping/dictate`, which renders
   `DictateItemsSheet` through `ShoppingPage`'s `<Outlet/>`.
3. The sheet shows: an autofocused 3-row `<textarea>`, a one-line hint, a live chip preview of the
   parsed items, and one primary button `Add N items`.
4. The user taps the mic on their own keyboard and speaks. Text streams into the textarea; the chip
   row re-parses on every change.
5. Tapping a chip **excludes** it (dimmed + struck through); tapping again re-includes it. This never
   touches the textarea's text.
6. `Add N items` fires one `addManualItem.mutate(text)` per selected item in order, shows an info
   toast `Added N items`, and closes the sheet via `useSheet(parentPath).close()`.
7. Typing or pasting by hand works identically — one item per line, or comma-separated.

---

## 2. Assumptions and resolved ambiguities

Every item below is a decision made by the architect where the handoff left room. Each chooses the
safer / never-guess option.

| # | Ambiguity | Decision | Why |
| --- | --- | --- | --- |
| A1 | Does a `.` count as a decimal point only between two digits, or also at a leading position (`.5 litres`)? | Only when there is a **digit on both sides**. `.5 litres` splits. | "Never guess." A leading bare `.` is far more likely a stray full stop than a number. |
| A2 | Is `,` ever a decimal separator (`1,5 litres`, which `lib/parseQuantity.ts` accepts)? | **No.** A comma is always a hard separator. | The comma is the single most important item separator in dictation; protecting it would break the common case to serve a rare one. Locked in by a test so nobody "fixes" it silently. |
| A3 | Does the separator-less-run rule apply only to a segment with no separators at all, or also to each piece produced by the " and " split? | **Each piece, independently.** | `"milk eggs and bread butter"` is obviously four items. The run rule is all-or-nothing and never guesses, so applying it more widely can only help. Without this, `"milk eggs and bread cheese"` would produce two items. |
| A4 | Should placeholders be restored before or after the run split? | **Before.** Restore, then run-split the restored text. | A protected name like `"mac and cheese"` is itself a known name, so the run-split matches it whole (longest-first). Restoring later would leave an unexplained placeholder token and fail the whole run. |
| A5 | Exact leading-filler set. | **Revised after the build at the owner's request — `FILLERS` and `FILLER_PHRASES` in `splitDictation.ts` are now the source of truth, do not restore the original:** `and`, `also`, `plus`, `&`, `some`, `the`, `then`, `maybe`, `please`, `get`, `buy`, `grab`, `um`/`umm`/`uh`/`er`/`oh`/`ok`/`okay`, plus leading phrases (`i need`, `we need`, `i also need`, `we also need`, `i want`, `we want`, `let's get`, `don't forget (the)`). Stripped repeatedly from the front of each piece, and a single-word filler between two items of a separator-less run is skipped. `some` is no longer a quantity word. `a`, `an`, `two`, numerals are **not** stripped (they are quantities). | Handoff names `and` / `also` / `plus` and explicitly excludes `some`. `&` is added because it is already treated as an `and` synonym by the split rule. |
| A6 | Does " and " split need whitespace around it? | **Yes** — `and` / `&` must be preceded by string start or whitespace and followed by whitespace or string end. | Protects `sand`, `Andes`, `hot-and-sour`, `M&M`. |
| A7 | Plural tolerance direction. | **Symmetric**, and only on the **last token** of a candidate name. `eggs`↔`egg`, `avocados`↔`avocado`, `tomatoes`↔`tomato`. Earlier tokens of a multi-word name must match exactly. | The catalogue mixes singular and plural entries. A naive `singularise()` mangles `cheese`. |
| A8 | Is `!` / `?` a hard separator? | **No.** Left in the item text. | Not in the handoff's separator list; adding separators is a behaviour change the owner did not ask for. The user sees the chip and can fix the text. |
| A9 | What does a chip tap do exactly — hide the item, or mark it excluded? | **Toggle excluded**; the chip stays visible, dimmed and struck through, and tapping again restores it. | The textarea must never be edited by code, so an accidental tap would otherwise be **unrecoverable**. This is a direct consequence of the iOS dictation rule. |
| A10 | Are excluded keys pruned when the text changes? | **No.** They are filtered against whatever the current parse produces. | Dictation streams: a key can vanish mid-word and come back a moment later. A key that matches nothing is simply inert. |
| A11 | Which side of the `+` button does the mic go? | Between the text field and `+`; `+` stays rightmost. | Preserves the existing thumb muscle memory for the primary action. |
| A12 | `@radix-ui/react-icons` has no microphone icon. | Add one inline-SVG component, `components/MicIcon.tsx`, 15×15 to match the Radix icon box, stroke-based like `PageLoader`. Full path data is given in §6.3. | No new dependency. Precise geometry in the plan so the dev does not invent shapes. |
| A13 | Toast wording at N=1. | `Added 1 item` / `Added N items`, from a tested pure helper. | Avoids "Added 1 items". |

### Infrastructure decision (stated, not left open)

The webapp has **Vitest and nothing else** — no `test` block in `vite.config.ts`, no jsdom, no
`@testing-library/react`, no setup file, one existing test (`src/lib/mealPlanSummary.test.ts`) that
runs in the default node environment.

**Decision: do not add a component test runner for this feature.** Adding jsdom +
`@testing-library/react` + a vitest `environment` config is a separate infrastructure change and is
out of scope here. Instead, the component is designed as a **thin shell**: all decision-making logic
lives in two pure `lib/` modules with full Vitest coverage, and the `.tsx` file contains only
rendering, three `useState`s, and event wiring. The behaviour that remains untestable in CI is
enumerated in §9.

---

## 3. `splitDictation` — precise specification

**File:** `webapp/src/lib/splitDictation.ts`

```ts
/**
 * Splits one stream of dictated (or typed, or pasted) text into shopping items.
 *
 * `knownNames` is the ingredient catalogue's names, used ONLY to find boundaries —
 * never to rewrite an item. Items stay free text exactly as the user produced them
 * (apart from trimming, whitespace collapse, leading-filler removal and capitalising
 * the first character). An empty `knownNames` is fully supported: the catalogue query
 * may be loading or failed.
 *
 * Pure. No I/O, no Date, no randomness. Same inputs always give the same output.
 */
export function splitDictation(text: string, knownNames: string[]): string[]

/**
 * The case-insensitive identity of an item: `text.trim().replace(/\s+/g, ' ').toLowerCase()`.
 * Used for de-duplication inside `splitDictation` and as the key of the sheet's
 * excluded-item set. Both sides MUST use this one function.
 */
export function itemKey(text: string): string
```

Nothing else is exported.

### 3.0 Preparing `knownNames`

Once per call, before anything else:

1. Map each name to `name.trim()`, drop empties and whitespace-only entries.
2. Keep the original (trimmed) string for nothing — only the lowercase form is ever compared.

### 3.1 Phase A — hard-separator split

Hard separators: `\n`, `\r\n`, `,`, `;`, `.`

A `.` is **not** a separator when the character immediately before it and the character immediately
after it are both ASCII digits `0-9`.

**Implementation, without regex lookbehind** (lookbehind is unsupported on older iOS Safari, and this
code runs on the owner's phone):

```
const DECIMAL = '';   // private-use sentinel

// single left-to-right scan
for each index i in text:
  if text[i] === '.' && isDigit(text[i-1]) && isDigit(text[i+1]) -> emit DECIMAL
  else -> emit text[i]
```

Then `protected.split(/\r?\n|[,;.]/)`, then replace every `DECIMAL` back to `.` in each segment.

Notes: `"1.2.3"` — the scan is character-wise, so both dots are protected (unlike a `(\d)\.(\d)`
global replace, which consumes the shared digit). This is the reason for the manual scan.

### 3.2 Phase B — normalise each segment

`segment.trim().replace(/\s+/g, ' ')`. Drop the segment if it is now empty.

This runs **before** name protection so a name regex never has to cope with double spaces.

### 3.3 Phase C — protect known names containing "and" / "&"

1. Candidate set = every prepared known name whose own text would be split by the Phase-D regex,
   i.e. it contains a standalone `and` or `&` (`/(?:^|\s)(?:and|&)(?=\s|$)/i.test(name)`).
2. Sort candidates **by character length descending, then lexicographically ascending** (a
   deterministic tie-break, independent of catalogue order).
3. For each candidate in that order, applied **sequentially to the already-substituted string**:
   build `new RegExp('\\b' + escapeRegExp(name).replace(/\s+/g, '\\s+') + '\\b', 'gi')` and replace
   every match with `'' + slotIndex + ''`, pushing the **matched original text** (not the
   catalogue name) onto a slots array.
4. `escapeRegExp` escapes `.*+?^${}()|[]\\` — `&` is not a regex metacharacter and needs no escape,
   but `\b` next to `&` does not behave as a word boundary, so for a candidate that starts or ends
   with a non-word character, omit the corresponding `\b`. Simplest correct form: only prepend `\b`
   when the name's first char is a word character, and only append `\b` when the last char is.

Longest-first matters observably — see test S11.

### 3.4 Phase D — split on standalone "and" / "&"

```
piece.split(/(?:^|\s+)(?:and|&)(?:\s+|$)/gi)
```

Case-insensitive. The leading `^|\s+` and trailing `\s+|$` are what make `", and bread"` work and
what stop `sand`, `Andes`, `hot-and-sour` and `M&M` from splitting.

### 3.5 Phase E — restore, then clean each piece

In this order:

1. Restore every `<n>` placeholder to its recorded original text.
2. `trim()`, `replace(/\s+/g, ' ')`.
3. Strip leading filler **repeatedly**: while the first whitespace-delimited word, lowercased, is one
   of `and`, `also`, `plus`, `&` — remove it and re-trim.
4. Drop the piece if it is now empty.

**Revised:** `some` IS a filler (owner's request after trying the build): `"some milk"` becomes `"Milk"`. See decision A5 for the full set.

### 3.6 Phase F — separator-less run split (all-or-nothing)

Applied to every piece from Phase E, independently (decision A3).

**Tokenise** the piece on single spaces (Phase E guaranteed single spaces), recording each token's
start and end offset in the piece string.

**Build the name index once per `splitDictation` call** (not per piece, not per token):
`Map<string /* exact lowercase first token */, Array<string[]> /* lowercase token arrays */>`.
Within each bucket, sort candidates by **token count descending, then total character length
descending, then lexicographically ascending**.

**No regexes in this phase.** Matching is token-array comparison only — this is a hot path, re-run on
every keystroke of streaming dictation.

**Token equivalence** (`tokensEquivalent(a, b)`, both lowercased):
`a === b || a === b + 's' || a === b + 'es' || b === a + 's' || b === a + 'es'`.

**Candidate lookup at token position `i`**: probe the index with these keys, deduplicating the union
of their buckets — `t`, `t` without a trailing `s`, `t` without a trailing `es`, `t + 's'`,
`t + 'es'` (where `t = lower(token[i])`).

**Candidate acceptance** — a candidate name of `k` tokens matches at position `i` when
`i + k <= tokens.length` and:
- for every `j` in `0 .. k-2`: `lower(token[i+j]) === name[j]` (**exact**), and
- for `j = k-1`: `tokensEquivalent(lower(token[i+j]), name[j])` (**relaxed**).

This automatically rejects a multi-token candidate that only reached the bucket via a plural probe on
its first token.

**Quantity words** (exact lowercase token match):

```
a, an, one, two, three, four, five, six, seven, eight, nine, ten, eleven, twelve,
dozen, couple, few, several, half, pair, of

(`some` was in this list originally; it is now a filler instead — see the revised decision A5.)
```

plus any token matching `/^\d+(?:\.\d+)?$/` or `/^\d+\/\d+$/` or `/^[½¼¾⅓⅔⅛]$/`.

Units (`litres`, `kg`, `grams`, …) are deliberately **not** quantity words — `"1.5 litres of milk"`
must stay one item.

**The walk:**

```
i = 0; pendingStart = null; items = []
while i < tokens.length:
    match = longest accepted candidate at i            // known-name match is tried FIRST
    if match (k tokens):
        start = pendingStart ?? i
        items.push(piece.slice(tokenStart[start], tokenEnd[i + k - 1]))   // VERBATIM span
        pendingStart = null
        i += k
    else if isQuantityWord(tokens[i]):
        if pendingStart === null: pendingStart = i
        i += 1
    else:
        return [piece]        // unexplained word -> ABANDON, the whole piece is one item
if pendingStart !== null:
    return [piece]            // trailing quantity words with no name -> ABANDON
return items
```

Two properties this guarantees, both required by the handoff:

- **Never guesses.** Any unexplained word leaves the entire piece as one item.
- **Free text preserved.** Each emitted item is the verbatim substring of the piece, including the
  quantity prefix and whatever plural form the user said. The catalogue is used only to locate
  boundaries, never to rewrite. `"two avocados"` → `"Two avocados"`, never `"Avocado"`.

With `knownNames = []` every multi-token piece abandons on its first token and stays one item —
exactly the behaviour required while `useIngredients()` is loading or has failed.

### 3.7 Phase G — final clean

Per item: `trim()`; drop if empty; capitalise **only** the first character
(`s.charAt(0).toUpperCase() + s.slice(1)`). Everything after index 0 is untouched, so `"greek
yoghurt"` → `"Greek yoghurt"` and `"1.5 litres"` is unchanged.

### 3.8 Phase H — de-duplicate

Walk in order, keep the **first** occurrence of each `itemKey(item)`. `"MILK, milk"` → `["MILK"]`,
not `["Milk"]`.

### 3.9 Performance contract

`splitDictation` is called on every `onChange` of a streaming dictation. Per call it must do at most:
one linear scan of the text, one pass over `knownNames` to build the index, a handful of regexes for
the "and"-containing names only (typically 0–3), and token comparisons. **No `new RegExp` per known
name per piece.** The component wraps the call in `useMemo`.

---

## 4. `dictationSelection` — the sheet's pure selection logic

**File:** `webapp/src/lib/dictationSelection.ts`

This exists so the sheet component contains no branching logic at all. Everything the sheet renders
and every number it shows comes out of one pure function.

```ts
import { itemKey, splitDictation } from './splitDictation';

export interface DictationChip {
  /** The item text exactly as it will be sent as the manual item's description. */
  text: string;
  /** itemKey(text) — the React key, the excluded-set key, and the dedup identity. */
  key: string;
  /** True when this item is in the excluded set and will NOT be added. */
  excluded: boolean;
}

export interface DictationSelection {
  /** Every parsed item in order, including excluded ones (they stay on screen). */
  chips: DictationChip[];
  /** The texts that will actually be POSTed, in order. */
  selected: string[];
  /** selected.length — the number in the button label. */
  count: number;
  /** "Add items" (0) | "Add 1 item" | "Add N items". */
  buttonLabel: string;
}

/** Pure. Parse the raw textarea string, apply the excluded set, and derive everything the sheet renders. */
export function buildDictationSelection(
  text: string,
  knownNames: string[],
  excluded: ReadonlySet<string>,
): DictationSelection

/** Pure. Returns a NEW set with `key` toggled; the input set is never mutated. */
export function toggleExcluded(excluded: ReadonlySet<string>, key: string): Set<string>

/** Pure. "Added 1 item" | "Added N items". */
export function addedToastMessage(count: number): string
```

Behaviour:

- `chips` = `splitDictation(text, knownNames).map(text => ({ text, key: itemKey(text), excluded: excluded.has(itemKey(text)) }))`.
  De-duplication has already happened inside `splitDictation`, so chip keys are unique.
- `selected` = `chips.filter(c => !c.excluded).map(c => c.text)`.
- `count` = `selected.length`.
- `buttonLabel` = `count === 0 ? 'Add items' : count === 1 ? 'Add 1 item' : \`Add ${count} items\``.
- Excluded keys that match no current chip are silently ignored (decision A10).

---

## 5. Route, barrel, and code-splitting

**`webapp/src/router.tsx`** — two additions, following the existing shape exactly:

```tsx
const DictateItemsSheet = lazy(() => import('./pages/shopping').then((m) => ({ default: m.DictateItemsSheet })));
```

and, in the `plans/:planId/shopping` route's `children`, beside `switch` / `new` / `add`:

```tsx
// The keyboard-dictation multi-add box, opened from the quick-add bar's mic.
{ path: 'dictate', element: <DictateItemsSheet /> },
```

**`webapp/src/pages/shopping/index.ts`** — one line, so the sheet lands in the existing
`route-shopping` chunk and opening it needs no network fetch:

```ts
export { DictateItemsSheet } from './DictateItemsSheet';
```

Confirmed against the code as it is on disk:

- `usePagePath()` returns the page route's own match (`PAGE_MATCH_INDEX = 2`), so
  `/plans/x/shopping/dictate` yields the page path `/plans/x/shopping`. The page does **not** remount
  or animate when the sheet opens, and the quick-add draft survives.
- `areaForPath` matches `SHOPPING_UNDER_PLAN = /^\/plans\/[^/]+\/shopping(\/|$)/`, so the sheet is in
  the `shopping` area and the tab does not change. Nothing to add there.
- `vite.config.ts`'s `ROUTE_AREAS` already contains `shopping`; no build config change.

---

## 6. Component design

### 6.1 `DictateItemsSheet`

**New files:** `webapp/src/pages/shopping/DictateItemsSheet.tsx`,
`webapp/src/pages/shopping/DictateItemsSheet.css`

Structure (not literal code — the shape the dev must produce):

```tsx
export function DictateItemsSheet() {
  const { planId } = useParams<{ planId: string }>();
  // Same shape as SwitchPlanSheet / NewPlanSheet / AddRecipeToPlanSheet.
  const parentPath = useLocation().pathname.replace(/\/dictate\/?$/, '');
  const sheet = useSheet(parentPath);               // no canClose: the adds are optimistic
  const addManualItem = useAddManualShoppingItem(planId!);
  const { data: ingredients } = useIngredients();   // no loading/error UI — see below

  const [text, setText] = useState('');
  const [excluded, setExcluded] = useState<ReadonlySet<string>>(() => new Set());

  const knownNames = useMemo(() => (ingredients ?? []).map((i) => i.name), [ingredients]);
  const selection = useMemo(
    () => buildDictationSelection(text, knownNames, excluded),
    [text, knownNames, excluded],
  );

  function handleAdd() {
    const items = selection.selected;
    if (items.length === 0) return;
    for (const item of items) addManualItem.mutate(item);   // no per-call callbacks — see 6.1.2
    toast.info(addedToastMessage(items.length));
    sheet.close();
  }

  return <Sheet {...sheet.sheetProps} title="Dictate items"> … </Sheet>;
}
```

State is exactly three things: `text`, `excluded`, and what the two `useMemo`s derive. There is no
other branching in the file.

#### 6.1.1 The textarea — the iOS dictation rule, structurally enforced

```tsx
<textarea
  className="dictate-sheet__input"
  autoFocus
  rows={3}
  value={text}
  onChange={(event) => setText(event.target.value)}
  placeholder="Milk, eggs, bread…"
  aria-label="Dictated items"
  autoCapitalize="sentences"
  autoCorrect="on"
  spellCheck
  enterKeyHint="enter"
/>
```

Rules the web-dev **must** follow, and the reviewer must check:

- **`onChange` is exactly `setText(event.target.value)`.** No `.trim()`, no `.replace()`, no
  normalising, no slicing, no `maxLength`-driven truncation, no "tidy up" of any kind. This is what
  makes the controlled textarea safe: React only writes to the DOM node when the new `value` differs
  from the node's current value, so echoing the raw string back verbatim performs **no write at all**.
  The moment any transform is introduced, every keystroke becomes a DOM write into a focused field
  and iOS ends the dictation session.
- **`setText` is called from nowhere else.** Not on add (the sheet closes instead of clearing), not on
  chip removal, not from an effect, not from a timer. Chip removal edits `excluded`, never `text`.
- Do **not** convert the textarea to uncontrolled with a ref — a reviewer might suggest it as
  "safer"; it is not needed (see above) and it would break the `useMemo`-driven preview.
- **The sheet contains no `<form>` element.** The Add button is `type="button"` with `onClick`. This
  makes "Enter inserts a newline and must not submit" structural rather than a handler that can be
  forgotten. (`lib/enterMovesOn.ts` is for the long recipe forms and must **not** be used here.)

#### 6.1.2 Firing the adds — do not pass per-call callbacks

`handleAdd` calls `addManualItem.mutate(item)` once per selected item, in order, and then closes the
sheet immediately.

**This is load-bearing and non-obvious:** in TanStack Query v5, the callbacks passed to
`mutate(vars, { onSuccess, onError })` belong to the *observer* and **do not fire if the component
unmounts before the mutation settles** — which is exactly what happens here, because the sheet closes
straight away. The callbacks declared inside `useAddManualShoppingItem`'s own `useMutation` options
(`onSuccess`, `onError`, `onSettled`) live on the Mutation itself and **do** still run after unmount.
So:

- **Do not** copy `ShoppingPage`'s quick-add pattern of `mutate(text, { onError: … })` — that handler
  would silently never run.
- The hook already does everything needed after unmount: swaps the temp row for the real one on
  success, removes the temp row and shows `Couldn't add "…"` on failure (`meta.suppressErrorToast`
  keeps the global toast out of the way), and `onSettled: invalidateIfLast(...)` refetches once the
  last in-flight mutation for the plan settles. `toastStore` replaces error toasts of the same tone,
  so several failures cannot pile up.

Ordering: the optimistic temp rows are inserted from `onMutate`, which `await`s `cancelQueries` first,
so their relative order is not strictly guaranteed. This does not matter — `lib/shoppingRows.ts`'s
`sortRows` puts manual items in server creation order, and the settle-time invalidation refetches.
No action needed.

The mutation's key is `shoppingKey(planId)`, the same key the page's own hook instance uses, so
`invalidateIfLast`'s `isMutating({ mutationKey }) === 1` check still counts correctly across both
hook instances.

#### 6.1.3 The rest of the sheet

- **Hint**, one muted line under the textarea:
  `Tap the mic on your keyboard and say your items — pause, or say "comma" or "and", between them.`
  Rendered as `<Text as="p" size="1" color="gray">`. A web page cannot start keyboard dictation
  itself, which is exactly why this line exists.
- **Chip area**: a wrapping flex row inside its own capped, scrollable container. Each chip is a
  `<button type="button">`:
  - included: soft accent, `aria-label={\`Remove ${chip.text}\`}`, `aria-pressed={false}`
  - excluded: `dictate-sheet__chip--excluded` (dimmed, `text-decoration: line-through`),
    `aria-label={\`Add ${chip.text} back\`}`, `aria-pressed={true}`
  - `onClick={() => setExcluded((current) => toggleExcluded(current, chip.key))}`
  - `key={chip.key}`
  - minimum 44px touch height.
  - When `chips.length === 0`, render a muted placeholder line
    (`Items will appear here as you speak.`) instead of collapsing the area, so the layout does not
    jump the moment the first word lands.
- **Primary button**: `<Button type="button" size="3" onClick={handleAdd} disabled={selection.count === 0}>{selection.buttonLabel}</Button>`,
  full width, last child of the sheet body. The count is announced through the label itself, which is
  the handoff's stated a11y approach — no separate live region.
- **No loading or error UI for `useIngredients()`.** It is only used to sharpen parsing; while it is
  loading or failed, `knownNames` is `[]` and every segment simply stays one item. Deliberate — state
  it in a code comment so a reviewer does not add a spinner.

### 6.2 `ShoppingPage` — the mic button

**Modified:** `webapp/src/pages/shopping/ShoppingPage.tsx`

Inside the existing `<BottomBar>`'s `form.shopping-page__quick-add`, between `<TextField.Root>` and
the submit `<Button>`:

```tsx
<Button asChild size="3" variant="soft" className="shopping-page__quick-add-mic">
  <SheetLink to="dictate" aria-label="Dictate items">
    <MicIcon />
  </SheetLink>
</Button>
```

- `SheetLink to="dictate"` is relative to `/plans/:planId/shopping`, the same way the empty state's
  `SheetLink to="add"` already works on this page. `SheetLink` is required (not a plain `<Link>`) so
  the usage reads as a sheet trigger, and a real anchor is what `useSheet`'s history-index heuristic
  needs.
- `aria-label` goes on `SheetLink` (the element that actually becomes the anchor).
- An `<a>` inside the `<form>` does not submit it. No `preventDefault` needed.
- `variant="soft"` (secondary) against the submit button's `variant="solid"` (primary) — the `+` stays
  the visually dominant action.
- The mic button is only reachable in the loaded state, since `<BottomBar>` only renders in
  `ShoppingListBody`. A deep link to `/plans/x/shopping/dictate` while the list is loading, not found
  or forbidden still mounts the sheet (every branch of `ShoppingPage` renders `<Outlet/>`); the adds
  would then fail with the hook's normal error toast. Acceptable; no extra handling.

**Modified:** `webapp/src/pages/shopping/ShoppingPage.css` — guarantee the input stays usable at 320px:

```css
.shopping-page__quick-add-input {
  flex: 1;
  min-width: 0;          /* NEW: without this a flex item refuses to shrink past its content */
}

/* NEW: neither icon button may be squeezed out of its 44px target. */
.shopping-page__quick-add-mic,
.shopping-page__quick-add > button[type='submit'] {
  flex: 0 0 auto;
}
```

At 320px: 32px of bar padding + 2 × 8px gap + two icon buttons ≈ 88px leaves ≈ 184px for the field.

### 6.3 `MicIcon`

**New:** `webapp/src/components/MicIcon.tsx` — its own file, one component per file (Fast Refresh).
15×15 to sit in the same box as the Radix icons beside it; stroke-based, like `PageLoader`'s icons.

```tsx
/** A microphone, sized and coloured like a @radix-ui/react-icons icon (which has no mic of its own). */
export function MicIcon() {
  return (
    <svg
      width="15" height="15" viewBox="0 0 15 15"
      fill="none" stroke="currentColor" strokeWidth="1.1"
      strokeLinecap="round" strokeLinejoin="round"
      aria-hidden="true" focusable="false"
    >
      <rect x="5.6" y="1.4" width="3.8" height="7.2" rx="1.9" />
      <path d="M3.6 7.1v.4a3.9 3.9 0 0 0 7.8 0v-.4" />
      <path d="M7.5 11.4v2.1" />
      <path d="M5.4 13.5h4.2" />
    </svg>
  );
}
```

`aria-hidden` on the SVG; the accessible name comes from the link's `aria-label`.

### 6.4 CSS — `DictateItemsSheet.css`

House rules: `--accent-*` / `--gray-*` tokens only, never a named scale or a hard-coded colour;
BEM-ish `dictate-sheet__*` class names; no `100vh` anywhere (`dvh` is acceptable for a cap inside a
sheet).

```css
.dictate-sheet { display: flex; flex-direction: column; gap: 12px; }

.dictate-sheet__input {
  width: 100%;
  box-sizing: border-box;
  resize: none;                 /* fixed at rows=3; it scrolls internally instead of growing */
  font: inherit;
  font-size: 16px;              /* < 16px makes iOS Safari zoom the page on focus */
  line-height: 1.4;
  padding: 10px 12px;
  border: 1px solid var(--gray-7);
  border-radius: var(--radius-3);
  background: var(--color-surface);
  color: var(--gray-12);
}
.dictate-sheet__input:focus-visible { outline: 2px solid var(--accent-9); outline-offset: 1px; }

.dictate-sheet__chips {
  display: flex; flex-wrap: wrap; gap: 8px;
  max-height: 26dvh;            /* the chip area scrolls; the Add button stays reachable */
  overflow-y: auto;
  padding: 2px;                 /* room for focus rings */
}

.dictate-sheet__chip {
  min-height: 44px; padding: 0 12px;
  border: none; border-radius: 999px; cursor: pointer;
  background: var(--accent-3); color: var(--accent-11);
}
.dictate-sheet__chip--excluded {
  background: var(--gray-3); color: var(--gray-9);
  text-decoration: line-through;
}
```

Layout budget with the sheet's existing `max-height: 90dvh`: header ~56px + textarea 3 rows ~76px +
hint ~18px + chips ≤26dvh + button 44px + padding. Comfortably under 90dvh with the keyboard up.
The textarea is capped by **not auto-growing** (no auto-resize effect — an auto-resize effect writes
to the element and is another way to disturb a focused field; do not add one).

Nested scrolling inside `.dictate-sheet__chips` is safe: it is a descendant of the sheet's own
`Dialog.Content`, which is `react-remove-scroll`'s shard, so touch-move there is treated as inside the
dialog (this is the same reason `useSheetContainer` exists).

---

## 7. File-by-file change list

### New files (7)

| Path | Contents |
| --- | --- |
| `webapp/src/lib/splitDictation.ts` | `splitDictation`, `itemKey`. Pure. §3. |
| `webapp/src/lib/splitDictation.test.ts` | Vitest suite, §8.1. |
| `webapp/src/lib/dictationSelection.ts` | `buildDictationSelection`, `toggleExcluded`, `addedToastMessage`, `DictationChip`, `DictationSelection`. Pure. §4. |
| `webapp/src/lib/dictationSelection.test.ts` | Vitest suite, §8.2. |
| `webapp/src/pages/shopping/DictateItemsSheet.tsx` | The sheet. §6.1. |
| `webapp/src/pages/shopping/DictateItemsSheet.css` | §6.4. |
| `webapp/src/components/MicIcon.tsx` | §6.3. |

### Modified files (5)

| Path | Change |
| --- | --- |
| `webapp/src/pages/shopping/index.ts` | `export { DictateItemsSheet } from './DictateItemsSheet';` |
| `webapp/src/router.tsx` | `const DictateItemsSheet = lazy(...)` beside the other shopping imports; `{ path: 'dictate', element: <DictateItemsSheet /> }` in the `plans/:planId/shopping` children. |
| `webapp/src/pages/shopping/ShoppingPage.tsx` | Import `MicIcon`; add the mic `<Button asChild><SheetLink to="dictate">` between the quick-add field and the `+` button inside `<BottomBar>`. Nothing else in this file changes. |
| `webapp/src/pages/shopping/ShoppingPage.css` | `min-width: 0` on `.shopping-page__quick-add-input`; `flex: 0 0 auto` on the two icon buttons. |
| `webapp/CLAUDE.md` | **doc-updater, last PR.** Add to "Non-obvious decisions": the never-mutate-a-dictating-field rule and why the chip set is an excluded-key set instead of a text edit; the "no per-call `mutate` callbacks when the component unmounts immediately" note. Add the `/plans/:planId/shopping/dictate` route to the structure/navigation notes. Update the "Tests" line: two new pure-function suites join `mealPlanSummary.test.ts`. |

### Cascade impact

**None.** This feature adds no fields to and changes no signature of any existing type. Verified
against the code on disk:

- `ManualShoppingItemResponse`, `IngredientResponse`, `ShoppingRow`, `ShoppingListResponse` — all read
  only, unchanged.
- `useAddManualShoppingItem(planId: string)` — called, not changed. Its one existing caller
  (`ShoppingPage`'s `handleQuickAdd`) is untouched.
- `useIngredients()` — called, not changed.
- `useSheet` / `Sheet` / `BottomBar` / `SheetLink` — used as-is, no new props.
- No new npm dependency, no `package.json` change, no `vite.config.ts` change.

---

## 8. Test contract

Both suites are plain Vitest, node environment, no DOM — matching `src/lib/mealPlanSummary.test.ts`'s
style (`import { describe, it, expect } from 'vitest'`, one `describe` per exported function, one
`it` per case with a sentence-style name).

Shared catalogue constant for the `splitDictation` suite:

```ts
const CATALOGUE = ['Milk', 'Eggs', 'Bread', 'Cheese', 'Avocado', 'Butter', 'Salt', 'Pepper', 'Jam'];
```

Cases that need a different catalogue state it in the row.

### 8.1 `splitDictation.test.ts` — required cases

`knownNames` column: `[]` means the empty array; `CATALOGUE` means the constant above;
`CATALOGUE + [...]` means the constant plus the listed names.

| # | Input | `knownNames` | Expected | What it pins down |
| --- | --- | --- | --- | --- |
| S1 | `'milk, eggs, bread'` | `[]` | `['Milk','Eggs','Bread']` | comma splits |
| S2 | `'milk\neggs\nbread'` | `[]` | `['Milk','Eggs','Bread']` | newline splits |
| S3 | `'milk\r\neggs'` | `[]` | `['Milk','Eggs']` | CRLF splits |
| S4 | `'milk; eggs'` | `[]` | `['Milk','Eggs']` | semicolon splits |
| S5 | `'Milk. Eggs. Bread.'` | `[]` | `['Milk','Eggs','Bread']` | period splits; trailing period yields no empty item |
| S6 | `'1.5 litres of milk'` | `CATALOGUE` | `['1.5 litres of milk']` | decimal point does NOT split; `litres` is unexplained so the run is abandoned; first char is a digit so capitalising is a no-op |
| S7 | `'2 litres of milk. Bread'` | `CATALOGUE` | `['2 litres of milk','Bread']` | a real full stop still splits alongside a number |
| S8 | `'1,5 litres of milk'` | `CATALOGUE` | `['1','5 litres of milk']` | **documented behaviour** (decision A2): a comma is always a separator, never a decimal |
| S9 | `'milk and eggs'` | `CATALOGUE` | `['Milk','Eggs']` | standalone `and` splits |
| S10 | `'mac and cheese'` | `CATALOGUE + ['Mac and cheese']` | `['Mac and cheese']` | protection runs before the `and` split |
| S11 | `'mac and cheese'` | `CATALOGUE` | `['Mac','Cheese']` | without the catalogue entry it splits — the handoff's paired case |
| S12 | `'salt and pepper and milk'` | `CATALOGUE + ['Salt and pepper']` | `['Salt and pepper','Milk']` | protection is per-occurrence, the remaining `and` still splits |
| S13 | `'bread and butter and jam'` | `['Bread and butter','Butter and jam','Jam']` | `['Bread and butter','Jam']` | **longest-name-first**; shortest-first would give `['Bread','Butter and jam']` |
| S14 | `'salt & pepper'` | `CATALOGUE` | `['Salt','Pepper']` | `&` splits like `and` |
| S15 | `'salt & pepper'` | `CATALOGUE + ['Salt & pepper']` | `['Salt & pepper']` | a name containing `&` is protected too |
| S16 | `'Milk, eggs, and bread.'` | `[]` | `['Milk','Eggs','Bread']` | iOS auto-punctuation: `, and ` and the trailing period |
| S17 | `'sand and gravel'` | `[]` | `['Sand','Gravel']` | `and` inside `sand` does not split |
| S18 | `'hot-and-sour soup'` | `[]` | `['Hot-and-sour soup']` | hyphenated `and` does not split; the run abandons on unknown words |
| S19 | `'milk eggs bread'` | `CATALOGUE` | `['Milk','Eggs','Bread']` | separator-less run, fully covered |
| S20 | `'milk eggs dragonfruit'` | `CATALOGUE` | `['Milk eggs dragonfruit']` | **all-or-nothing**: one unknown word leaves the whole segment as one item |
| S21 | `'milk eggs bread'` | `[]` | `['Milk eggs bread']` | empty catalogue (query loading or failed) — never guesses |
| S22 | `'milk two'` | `CATALOGUE` | `['Milk two']` | trailing quantity word with no name abandons the run |
| S23 | `'milk two eggs'` | `CATALOGUE` | `['Milk','Two eggs']` | a quantity prefix attaches to the name that follows it |
| S24 | `'two avocados and a dozen eggs'` | `CATALOGUE` | `['Two avocados','A dozen eggs']` | quantities + plural tolerance + the handoff's stated example; items stay free text (not `['Avocado','Egg']`) |
| S25 | `'a couple of apples'` | `CATALOGUE + ['Apple']` | `['A couple of apples']` | `couple` and `of` are quantity words |
| S26 | `'milk egg'` | `CATALOGUE` | `['Milk','Egg']` | catalogue is plural (`Eggs`), text singular — symmetric plural tolerance |
| S27 | `'milk tomatoes'` | `CATALOGUE + ['Tomato']` | `['Milk','Tomatoes']` | `-es` plural |
| S28 | `'milk green beans'` | `CATALOGUE + ['Green beans']` | `['Milk','Green beans']` | multi-token known name inside a run |
| S29 | `'milk green'` | `CATALOGUE + ['Green beans']` | `['Milk green']` | a partial multi-token name does not match; the run abandons |
| S30 | `'milk eggs and bread butter'` | `CATALOGUE` | `['Milk','Eggs','Bread','Butter']` | **decision A3**: the run split applies to each piece produced by the `and` split |
| S31 | `'also milk, plus bread, and butter'` | `[]` | `['Milk','Bread','Butter']` | leading filler `also` / `plus` / `and` stripped |
| S32 | `'also plus milk'` | `[]` | `['Milk']` | filler stripping repeats |
| S33 | `'some milk'` | `CATALOGUE` | `['Milk']` | **Revised:** `some` is a filler and is stripped (reversed at the owner's request) |
| S34 | `'milk, eggs, milk'` | `[]` | `['Milk','Eggs']` | de-duplication |
| S35 | `'MILK, milk'` | `[]` | `['MILK']` | dedup is case-insensitive and keeps the FIRST occurrence verbatim |
| S36 | `''` | `[]` | `[]` | empty input |
| S37 | `'   '` | `[]` | `[]` | whitespace-only input |
| S38 | `'\n\n'` | `[]` | `[]` | newlines only |
| S39 | `',,,'` | `[]` | `[]` | separators only |
| S40 | `'and'` | `[]` | `[]` | a lone conjunction produces nothing |
| S41 | `'milk,    eggs'` | `[]` | `['Milk','Eggs']` | whitespace around separators |
| S42 | `'greek   yoghurt'` | `[]` | `['Greek yoghurt']` | internal whitespace collapses; only the first character is capitalised |
| S43 | `'  milk  '` | `[]` | `['Milk']` | outer trim |
| S44 | `'MAC AND CHEESE'` | `CATALOGUE + ['Mac and cheese']` | `['MAC AND CHEESE']` | protection and matching are case-insensitive; the user's own casing is preserved |
| S45 | `'Milk'` | `['', '   ', 'Milk']` | `['Milk']` | blank catalogue entries are ignored, not treated as matching everything |

Plus a `describe('itemKey')` block: `itemKey('  Two  Avocados ')` → `'two avocados'`;
`itemKey('Milk') === itemKey('MILK')`.

### 8.2 `dictationSelection.test.ts` — required cases

| # | Call | Expected |
| --- | --- | --- |
| D1 | `buildDictationSelection('milk, eggs, bread', [], new Set())` | `chips.length === 3`, none excluded, `selected === ['Milk','Eggs','Bread']`, `count === 3`, `buttonLabel === 'Add 3 items'` |
| D2 | `buildDictationSelection('milk, eggs, bread', [], new Set(['eggs']))` | `chips.length === 3`; the `Eggs` chip has `excluded === true`; `selected === ['Milk','Bread']`; `count === 2`; `'Add 2 items'` |
| D3 | `buildDictationSelection('milk', [], new Set())` | `count === 1`, `buttonLabel === 'Add 1 item'` |
| D4 | `buildDictationSelection('', [], new Set())` | `chips === []`, `selected === []`, `count === 0`, `'Add items'` |
| D5 | `buildDictationSelection('milk', [], new Set(['milk']))` | `chips.length === 1` and still on screen, `selected === []`, `count === 0`, `'Add items'` |
| D6 | `buildDictationSelection('MILK', [], new Set(['milk']))` | the chip is excluded — the excluded set is keyed by `itemKey`, not display text |
| D7 | `buildDictationSelection('milk, eggs', [], new Set(['kumquat']))` | an excluded key matching nothing is inert; `count === 2` |
| D8 | `buildDictationSelection('milk, milk', [], new Set())` | `chips.length === 1` — dedup flows through from `splitDictation` |
| D9 | `buildDictationSelection('milk eggs', ['Milk','Eggs'], new Set())` | `count === 2` — `knownNames` is passed through to the parser |
| D10 | chip keys | every `chip.key === itemKey(chip.text)` and all keys are unique (safe as React keys) |
| D11 | `toggleExcluded(new Set(), 'milk')` | `new Set(['milk'])`; the input set is unchanged (`size === 0`) and a **different** object is returned |
| D12 | `toggleExcluded(new Set(['milk','eggs']), 'milk')` | `new Set(['eggs'])`; input unchanged |
| D13 | `addedToastMessage(1)` / `addedToastMessage(3)` | `'Added 1 item'` / `'Added 3 items'` |

### 8.3 What is pure-and-tested vs. only verifiable by hand

**Pure, exported from `lib/`, covered by the tables above:** every parsing rule, every filler/quantity/
plural/dedup decision, the excluded-set filtering, the button label, the toast message, and the
toggle's immutability. This is all of the feature's decision-making.

**Not covered by any automated test in this repo** (no jsdom, no component runner, by decision):
the React wiring itself — that `onChange` stores the raw value, that the chip's `onClick` calls
`toggleExcluded`, that `handleAdd` loops `mutate` and then closes, that the route renders the sheet,
and every visual/iOS behaviour in §9. These are kept trivially reviewable by making the component a
thin shell: **the code-reviewer verifies them by reading the file**, and the owner verifies the rest on
a phone.

---

## 9. iOS, accessibility and keyboard notes for the web-dev

**The one rule that outranks everything else:** never write to the textarea's value from code while it
is focused. On iOS, changing a focused field's value from script ends the dictation session. See
§6.1.1 for the three structural consequences (verbatim echo in `onChange`, `setText` called from
exactly one place, chip removal maintains an excluded key set instead of editing the text).

- **No auto-resize.** Do not add a "grow the textarea to fit" effect. It writes to the element, it
  fights the height cap, and it is the classic accidental violation of the rule above.
- **`font-size: 16px` on the textarea.** Anything smaller makes iOS Safari zoom the page on focus.
- **No `100vh` anywhere.** `26dvh` on the chip area is the only viewport unit in the new CSS, matching
  the existing sheet's `max-height: 90dvh`.
- **`lib/viewportGuard.ts` needs nothing new.** It already treats a focused `TEXTAREA` as "typing" and
  skips its reset, and it resets the leftover pan on `focusout`. Do not modify it.
- **Never call `element.scrollIntoView()`.** If the dev thinks the chip area needs to be scrolled,
  the only allowed helpers are `lib/scrollIntoContainer.ts` — but this design needs neither.
- **Enter inserts a newline.** Guaranteed structurally by having no `<form>`; `enterKeyHint="enter"`
  only relabels the key.
- **`autoFocus`** on the textarea, matching `NewIngredientSheet`'s autofocused input. `AppShell`'s
  route-change focus deliberately yields to a descendant's mount focus, so this works. Whether iOS
  actually raises the keyboard for a focus that happens after a route change is **unverified on a real
  device** (it is unverified for the existing sheets too) — if it does not, the user taps the textarea
  once. Do not work around it with a `setTimeout(focus)`.
- **Touch targets:** chips `min-height: 44px`; both quick-add icon buttons at Radix `size="3"`.
- **Accessible names:** mic link `aria-label="Dictate items"`; textarea `aria-label="Dictated items"`;
  chip `aria-label="Remove <item>"` when included and `"Add <item> back"` when excluded, with
  `aria-pressed` reflecting the excluded state; the `MicIcon` SVG is `aria-hidden`.
- **The item count is announced through the button's own label** (`Add 3 items`) — no extra live
  region, per the handoff.
- **Sheet rules:** `components/Sheet` + `useSheet(parentPath)` is the only way to close;
  no `canClose` (the adds are optimistic and the sheet closes immediately); the sheet never changes
  the tab; content inside `<Sheet>` inherits the nested `<Theme>` automatically. No `Select` or
  `DropdownMenu` is used, so `useSheetContainer` / `SheetSelectContent` are not needed.

---

## 10. What we decided NOT to do

- **No batch endpoint, no backend change of any kind.** The handoff verified that
  `useAddManualShoppingItem` already mints a unique `temp-` id per call, targets only its own temp row
  on success/error, and refetches once through `invalidateIfLast`. ~5–15 sequential `mutate` calls is
  exactly what it was built for. Re-verified against `queries/shopping.ts` on disk.
- **No Web Speech API / `SpeechRecognition`.** Rejected in the handoff: unreliable on iOS, stops on
  pauses, needs HTTPS to test on a phone. The keyboard's own mic is the input device.
- **No new dependency, and specifically no jsdom / `@testing-library/react`.** See the infrastructure
  decision in §2.
- **No quantity or unit parsing, no linking to ingredient ids.** Items are free text. The catalogue is
  used only to find boundaries. `"two avocados"` becomes the description `"Two avocados"`.
- **No auto-correcting the user's words.** A chip never shows the catalogue's spelling of a name, only
  what the user said.
- **No `canClose` guard and no "adding…" progress state.** The adds are optimistic; the rows appear on
  the list underneath before the sheet has finished animating out.
- **No `mic` entry point anywhere but the shopping list's quick-add bar.** Not on the plan page, not in
  the header.
- **The `+` quick-add button and its handler are untouched.** The mic is purely additive.

---

## 11. PR stack

Web-only feature; the db / client / lib / service layers do not apply. Every PR has one concern and
leaves `npm run build` (the authoritative type-check gate), `npm run lint` and `npm run test` green.

| # | Tag | Title | Contents |
| --- | --- | --- | --- |
| 1 | `[plan]` | `feat(voice-shopping-list): plan` | this file |
| 2 | `[lib]` | `feat(voice-shopping-list): dictation parsing and selection helpers` | `lib/splitDictation.ts`, `lib/dictationSelection.ts` |
| 3 | `[web]` | `feat(voice-shopping-list): dictate sheet, mic button and route` | `pages/shopping/DictateItemsSheet.tsx` + `.css`, `components/MicIcon.tsx`, `pages/shopping/index.ts`, `router.tsx`, `pages/shopping/ShoppingPage.tsx`, `pages/shopping/ShoppingPage.css` |
| 4 | `[test]` | `feat(voice-shopping-list): splitDictation and selection tests` | `lib/splitDictation.test.ts`, `lib/dictationSelection.test.ts` — the full tables in §8 |
| 5 | `[docs]` | `feat(voice-shopping-list): documentation updates` | `webapp/CLAUDE.md` per §7; `docs/voice-shopping-list/retro.md` |

Reviews: **code-reviewer on PRs 2 and 3, test-reviewer on PR 4. Neither may be skipped.**

**Working-tree warning for the orchestrator:** the branch is `meal-app-frontend` and the tree has ~50
uncommitted files owned by the user, including `ShoppingPage.tsx`, `ShoppingPage.css`,
`BottomBar.tsx`, `useSheet.ts` and `webapp/CLAUDE.md`. Every file path in this plan is described as it
exists on disk right now. Do not stash, reset, checkout or clean anything; check `git status` before
any stack operation that would move those files.

---

## 12. Manual verification — only the owner, on a real phone

Nothing in this list can be automated in this repo. Everything else is covered by §8.

1. **The core case.** Open the shopping list, tap the mic, tap the keyboard's own mic, say
   "milk, eggs, bread" — the chips appear as the words land, `Add 3 items` enables, tapping it adds
   three rows and shows `Added 3 items`.
2. **The rule this feature exists to respect:** the dictation session does **not** end when chips
   appear, when a chip is tapped, or when text is still streaming in. This is the single most
   important thing to check, and the only way to check it is out loud on an iPhone.
3. Tapping a chip mid-dictation dims it without disturbing the text or the mic; tapping it again
   restores it.
4. Does the keyboard open by itself when the sheet opens (the `autoFocus` question in §9)? If not,
   note it — the fallback is one tap on the textarea, and it would affect every other sheet equally.
5. The textarea, the chips and `Add N items` all stay above the on-screen keyboard; the chip area
   scrolls rather than pushing the button off-screen; nothing pans the frame or cuts off the tab bar.
6. iOS auto-punctuation: say a list naturally and confirm the produced text ("Milk, eggs, and bread.")
   parses to three items (the same string is already pinned by test S16).
7. Android separator-less dictation: "milk eggs bread" with all three in the catalogue splits into
   three; with one word unknown it stays one item the user can see and fix.
8. 320px width: the quick-add field is still usable with the mic button present.
9. `Add N items` with ~15 items: all rows appear, the list settles after one refetch, no toast pile-up.
10. A failing add (airplane mode for one item) removes its temp row and shows one error toast, with the
    sheet already closed.
11. Browser Back closes the sheet and returns to the list; the quick-add draft typed before opening the
    sheet is still there.
12. Reduced motion: the sheet opens and closes without animation, and `close()` still lands on the list.
