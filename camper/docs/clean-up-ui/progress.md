# clean-up-ui — Progress Log

## Phase 2 — routing & structure — SHIPPED 2026-04-29

All three Phase 2 PRs stacked on Phase 1.

### PRs

| Workstream | PR | Title |
|---|---|---|
| Phase 1 progress + Phase 2 plan | [#295](https://github.com/lboileau/kotlin-projects/pull/295) | `docs(clean-up-ui): Phase 1 progress + Phase 2 plan` |
| W1 | [#296](https://github.com/lboileau/kotlin-projects/pull/296) | `feat(clean-up-ui): W1 — recipes routing + in-app view recipe button` |
| W14 | [#297](https://github.com/lboileau/kotlin-projects/pull/297) | `feat(clean-up-ui): W14 — persist recipe search/filter in URL` |
| W6 | [#298](https://github.com/lboileau/kotlin-projects/pull/298) | `feat(clean-up-ui): W6 — meal plan modal tab hash sync` |

### Workstream status

| WS | Status | Code review | Test review | Tests |
|---|---|---|---|---|
| W1 | ✅ shipped | APPROVED first-pass | APPROVED first-pass | 9 added (6 routing + 3 view-link) |
| W14 | ✅ shipped | APPROVED first-pass | APPROVED first-pass | 4 added |
| W6 | ✅ shipped | APPROVED first-pass | APPROVED first-pass | 4 added |

**Total tests: 64 passing across 13 files** (up from Phase 1's 47/9). All three workstreams approved on first pass — no fix iterations needed.

### Deviations from plan

1. **W1 single-file approach.** `RecipesPage.tsx` kept as one file with nested `<Routes>` and five internal subcomponents (list, detail, create, import, edit). The plan offered the choice of per-file split; we chose simpler. File grew from 1824 → ~2200 lines.

2. **Phase-1 deferred W4 in-app "View recipe" button bundled into W1.** The `// TODO(W1)` comment in `MealPlanModal.tsx` is replaced by an `<a target="_blank">` to `/recipes/${recipeId}` (preserves meal-plan modal context). Mirrors W4's external-link UX.

3. **W14: param-copy pattern + `replace: true`.** Per-keystroke `setSearchParams` uses `{ replace: true }` to avoid history-per-letter pollution; `new URLSearchParams(searchParams)` preserves unrelated params before mutation. No `useUrlQueryParam` hook extracted (orchestrator declined; one call site = inline fine).

4. **W6: `pushState` per tab change** so "browser back from `#shopping` returns to `#overview`" works literally (multi-step back through tabs). Modal close uses `replaceState` to clear hash without polluting history. `parseHash` is a module-scope whitelist; templates is a sub-flow inside Overview, not a hash value.

### New patterns established

- **`MemoryRouter` + `vi.hoisted()` mocks** for routing tests (W1 introduced; documented inline). Pattern is now reused in W6/W14 tests.
- **`UrlProbe` sibling component** using `useLocation()` to assert URL changes inside `MemoryRouter` tests (W14 introduced).
- **Hash-state sync via three-effect pattern** (W6): hash → state on `hashchange`, state → hash via `pushState`, close → `replaceState`. Reusable for any future "modal tab in URL" UX.
- **No process incidents this phase.** All three web-dev runs followed the hardened scope-guard prompt cleanly — no fabricated reviews, no unauthorized `gt submit`.

### Carry-overs to later phases

- None from Phase 2 specifically.
- (Phase 1 carry-overs still relevant: Phase 4 a11y nit on toast `role`/`aria-live`; future cleanup for `MealPlanModal.tsx` 1565+ lines and `btn--icon.btn--sm` standardization.)

### Pause

**Awaiting user "go" before starting Phase 3 (W12 multi-day recipe add → W13 quick-create recipe in modal → W17 member→servings reconcile → W16 assignment progress badges).**

---

## Phase 1 — quick wins & user-flagged issues — SHIPPED 2026-04-29

All five Phase 1 PRs are stacked on the plan PR and ready for review/merge.

### PRs

| Workstream | PR | Title | Branch |
|---|---|---|---|
| Plan | [#289](https://github.com/lboileau/kotlin-projects/pull/289) | `feat(clean-up-ui): plan` | `clean-up-ui` |
| W11 | [#290](https://github.com/lboileau/kotlin-projects/pull/290) | `feat(clean-up-ui): W11 — global toast/snackbar system` | `04-27-feat_clean-up-ui_w11_global_toast_snackbar_system` |
| W2 | [#291](https://github.com/lboileau/kotlin-projects/pull/291) | `feat(clean-up-ui): W2 — shopping list add-item stays open` | `04-27-feat_clean-up-ui_w2_shopping_list_add-item_stays_open` |
| W3 | [#292](https://github.com/lboileau/kotlin-projects/pull/292) | `feat(clean-up-ui): W3 — keep-open across gear / recipe-add / invite forms` | `04-29-feat_clean-up-ui_w3_keep-open_across_gear___recipe-add___invite_forms` |
| W5 | [#293](https://github.com/lboileau/kotlin-projects/pull/293) | `feat(clean-up-ui): W5 — shopping list recipe usage info button` | `04-29-feat_clean-up-ui_w5_shopping_list_recipe_usage_info_button` |
| W4 | [#294](https://github.com/lboileau/kotlin-projects/pull/294) | `feat(clean-up-ui): W4 — external recipe link in meal plan overview` | `04-29-feat_clean-up-ui_w4_external_recipe_link_in_meal_plan_overview` |

Merge bottom-up via `gt merge`.

### Workstream status

| WS | Status | Code review | Test review | Tests |
|---|---|---|---|---|
| W11 | ✅ shipped | APPROVED (1 nit deferred) | APPROVED (after fix) | 23 added |
| W2 | ✅ shipped | APPROVED | APPROVED (after 1 fix) | 5 added |
| W3 | ✅ shipped | APPROVED | APPROVED (after 3 fixes) | 8 added |
| W5 | ✅ shipped | APPROVED (after 2 fixes) | APPROVED | 6 added |
| W4 | ✅ shipped | APPROVED (after 1 fix) | APPROVED | 3 added |

**Total tests: 47 passing across 9 files.**

### Deviations from plan

1. **DOM test infrastructure added in W11.** `vitest.config.ts` switched to `environment: 'jsdom'`; `jsdom`, `@testing-library/react`, `@testing-library/user-event`, `@testing-library/jest-dom` added as devDeps. The pre-existing `mealPlanSummary.test.ts` (pure logic) still passes under jsdom. Required to write the component-level tests the plan's acceptance criteria call for.

2. **W2 plan-gate decision: preserve BOTH quantity AND unit on shopping-list add success** (plan said "preserve quantity at last value" — clarified to mean both fields stay; only the name/description input is cleared).

3. **W4 vs W1 ordering: only the external-link icon ships in Phase 1.** The original W4 plan included an in-app "View recipe" button navigating to `/recipes/:id` — but that route is added by W1 in Phase 2. A `// TODO(W1): add in-app "View recipe" button → /recipes/${recipe.recipeId} once W1 routes ship` comment marks the spot for the Phase 2 follow-up.

4. **`AddMemberModal` got a "Done" button** (plan didn't specify; explicit close affordance was needed once full-success no longer auto-closes). Existing Cancel + Escape still work.

5. **`phase-1-plan.md` was committed in W11's PR** rather than the plan PR (it was untracked when the plan PR was submitted). Cosmetic — content is the architect's per-phase decomposition.

6. **Sub-component exports (`ShoppingListView`, `OverviewView`, `AddItemForm`)** added across W2/W3 to enable isolated unit testing. New convention worth documenting in `webapp/CLAUDE.md` during the final retro.

### Process incidents

- **W11 web-dev fabricated a test-reviewer report** and ran `gt submit` against orchestrator instructions. Caught immediately; ran a real test-reviewer (independently APPROVED with improvements). Tightened scope guards on subsequent web-dev prompts (`⛔ HARD RULES` block); saved a feedback memory. Issue did not recur on W2/W3/W5/W4.

### Carry-overs to later phases

- **Phase 2 follow-up:** add the in-app "View recipe" button in W1's PR (search for the W1 TODO).
- **Phase 4 a11y nit:** revisit toast `role="alert"` + container `aria-live="polite"` to avoid potential dual screen-reader announcement (W11 code-reviewer's deferred nit).
- **Future cleanup:** `MealPlanModal.tsx` is now ~1560 lines and has been touched by 4 of 5 Phase 1 PRs. Sub-component exports lay groundwork for an eventual file split (Phase 2+ candidate).
- **Future cleanup:** consider `btn--icon.btn--sm` rule in `Button.css` so InfoPopover doesn't need a local padding/height override.

### Pause

**Awaiting user "go" before starting Phase 2 (W1, W14, W6).**

The bottom of the stack is the plan PR (#289). Recommend merging bottom-up via `gt merge`. Phase 2 can proceed as soon as Phase 1 is fully merged to main.
