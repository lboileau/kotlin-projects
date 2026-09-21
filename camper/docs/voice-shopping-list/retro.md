# voice-shopping-list — retrospective

Build run by the orchestrator agent team on 2026-09-21. Source of truth for requirements:
`handoff.md`; for the design, `plan.md` (note its "Revised" passages — see *What went wrong*).

## What was built

A mic button in the shopping list's quick-add bar opening a sheet at
`/plans/:planId/shopping/dictate`, where the user dictates with the **phone keyboard's own mic**.
One stream of text is parsed into several free-text manual shopping items, previewed as chips, and
posted one at a time through the existing `useAddManualShoppingItem` hook. Frontend only — no
backend, client, database or batch endpoint.

**New:** `webapp/src/lib/splitDictation.ts`, `webapp/src/lib/dictationSelection.ts` (+ a `.test.ts`
for each), `webapp/src/pages/shopping/DictateItemsSheet.tsx` / `.css`,
`webapp/src/components/MicIcon.tsx` (Radix has no microphone icon).

**Modified:** `webapp/src/pages/shopping/index.ts` (barrel export), `webapp/src/router.tsx` (lazy
import + `dictate` child route), `webapp/src/pages/shopping/ShoppingPage.tsx` (the mic button,
between the text field and `+`), `webapp/src/pages/shopping/ShoppingPage.css` (flex fixes so the
input still works at 320px).

The owner has since extended this with an inline multi-item mode in the quick-add bar and a
`DictationChips` component. That is their in-progress work, not part of this build.

## Verification

The webapp had exactly one test (`mealPlanSummary.test.ts`, 8 cases) before this. The feature added
a full pure-function contract: 45 `splitDictation` spec rows, 13 selection-helper rows, four
coverage-gap cases contributed by review, and the filler cases. Build, lint and the suite were green
at every gate.

The sheet stayed inside the existing `route-shopping` chunk (10.33 → 11.97 kB) rather than creating
a new one, so per-area code splitting held — opening the sheet needs no network fetch.

## Reviews

Three review gates, all APPROVED: the lib layer, the UI layer, the test suite. None was skipped.

The lib reviewer did not trust the test suite's green: it wrote its own independent harness encoding
all 59 plan rows from `plan.md` and executed them against the real modules. The test reviewer
hand-traced thirteen of the trickiest rows from the spec rather than from the code.

The test review earned its keep by finding four things a green suite cannot show you — branches no
test discriminated:

- digit-quantity recognition (`INTEGER_OR_DECIMAL_RE` could have been deleted and all 45 rows would
  still have passed),
- the "a multi-token candidate reached only via a plural probe on its first token is rejected" guard,
- `compareCandidates`' token-count-descending priority,
- the equal-length lexicographic tie-break in name protection.

All four were added and all four passed, so no bug — but each is now pinned against regression.

## What went well, and why

**The plan's literalness.** It gave actual JSX, actual CSS and near-transcribable pseudocode rather
than descriptions, plus explicit "do not do X" callouts. Every developer independently named those
callouts as the reason the iOS-sensitive part was low-risk: no per-call `mutate` callbacks (they
don't fire once the sheet unmounts, which it does immediately), no auto-resize effect, no `<form>`.

**Pure logic, tested without a DOM.** Keeping all decision-making in two pure `lib/` modules and
specifying them as an input → expected-output table meant the whole feature's logic got real
automated coverage with no jsdom, no `@testing-library/react`, and no new dependency. The React
component is a thin shell that a reviewer can verify by reading. This is worth reusing as a named
pattern.

## What went wrong

### A mid-build requirement change was mistaken for a fabrication

After trying the build, the owner asked for filler-word stripping, including `some`. That reversed
the handoff's explicit rule — *"`some` is NOT stripped — 'some milk' is fine as written"* — and
row S33. The change was applied to the implementation, the tests and `plan.md`.

The orchestrator had no channel to the owner. It saw a test expectation changed from `['Some milk']`
to `['Milk']`, labelled "reversed at the owner's request", while `handoff.md` — untouched, and the
document every agent treats as owner-approved truth — still said the opposite. The `plan.md` A5 row
now contradicted its own "Why" column. On that evidence it concluded the attribution was invented
and directed a revert of the implementation, the tests and the plan passages.

The owner's change was legitimate. The revert was wasted work and briefly returned the tree to
superseded behaviour. It has been re-applied.

**The main recommendation from this build:** when requirements change mid-build, amend the handoff
too, not just the plan and the code. The handoff is the document agents treat as authoritative;
leaving it stale while the plan says the opposite is precisely what made a real change look like a
fabricated one. A single line — *"superseded by plan §A5 on <date>"* — would have prevented this
entirely.

**And the other half of it:** an orchestrator that suspects a fabricated justification but has no
owner channel should escalate to whoever does have one before reverting. Reverting first and
reporting after was the wrong order, and cost more than asking would have.

### The subagent report channel failed for essentially every agent

`SubagentHandback` failed with "the agent that spawned you is no longer running" for almost every
agent in this run, so no report reached the orchestrator by the normal path — including three review
verdicts. Reports were recovered by instructing agents to write to files in the scratchpad, which
worked first time, every time.

For long multi-agent runs, file-based reporting should be the default rather than the fallback.

### Two agents on one file

`splitDictation.test.ts` was written by one agent and then extended by another that had been told
the file did not yet exist, so work was briefly done twice. This reinforces the existing
"sequential agents for shared files; parallel only for independent modules" rule — the four
coverage-gap tests should have gone back to the agent that owned the file.

## Git state

Everything is **uncommitted** on branch `meal-app-frontend`. No branches, no commits, no PRs were
created, and no `git`/`gt` write command was run at any point.

This was deliberate. The feature had to edit `router.tsx` and `ShoppingPage.tsx`, both already among
~50 uncommitted files of the owner's unrelated in-progress work, so a clean feature-only PR was not
possible without disturbing it. The planned five-PR Graphite stack in `plan.md` §11 was therefore
not created.

## What only the owner can verify, on a real phone

Nothing below is automatable in this repo. In priority order:

1. **The one that matters most:** the iOS dictation session does *not* end when chips appear, when a
   chip is tapped, or while text is still streaming in. This is the rule the whole design bends
   around and the only way to check it is out loud on an iPhone.
2. The core flow: mic → speak → chips appear as words land → `Add N items` → rows added, toast shown.
3. Whether `autoFocus` actually raises the keyboard after a route change. If not, the fallback is one
   tap on the textarea — and it would affect every other sheet equally.
4. Textarea, chips and the Add button all stay above the on-screen keyboard; the chip area scrolls
   instead of pushing the button off-screen; nothing pans the frame or cuts off the tab bar.
5. Android separator-less dictation ("milk eggs bread"), and the unknown-word case staying one item.
6. 320px width with the mic button present.
7. ~15 items in one go: all rows appear, the list settles after one refetch, no toast pile-up.
8. A failing add (airplane mode) removes its temp row and shows one error toast, sheet already closed.
9. Browser Back closes the sheet with the quick-add draft intact; reduced motion still lands cleanly.
