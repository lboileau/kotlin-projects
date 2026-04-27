# Orchestrator Handoff

## Workflow
feature-build

## Project Path
/Users/louisboileau/Development/kotlin-projects-worktrees/clean-up-ui/camper

## Feature Name
clean-up-ui

## Plan
See `camper/docs/clean-up-ui/plan.md`. The plan is already written and vetted FE-only — do **not** ask the architect to re-author it. The architect's role here is limited to producing per-phase implementation plans that decompose each workstream into PR-sized chunks consistent with the existing patterns in `webapp/src/`.

## Feature Description
A wide-ranging frontend-only UX cleanup of the camper webapp. 29 workstreams (W1–W29) grouped into 6 phases. Phases ship sequentially as PR stacks. Within a phase, workstreams may ship in parallel where independent.

The plan covers:
- Routing fixes so browser back/forward works (W1, W6, W14)
- Forms that stay open for repetitive add (W2, W3)
- Surfacing data already on existing API payloads (W4 recipe link, W5 shopping list info button, W18 servings context, W19 pending member visuals)
- A global toast/snackbar system (W11) used by W9, W15, W17, W20
- Workflow improvements: multi-day recipe add (W12), quick-create recipe in modal (W13), member→servings reconciliation (W17), assignment progress badges (W16)
- Polish & consistency: themed confirm modal (W7), reusable Tabs primitive (W24), unified delete + invite patterns (W23)
- Discoverability: gear pack & import promotion (W21), ladder rules hint (W22), first-run onboarding (W27, W28), empty states (W10), restart-ladder copy (W29)
- Sizing & mobile baseline (W25, W26)
- Keyboard support (W8) and blur-save feedback (W9)

## Entities
**No new entities and no DB changes.** All workstreams operate on data already exposed by the existing API.

## API Surface
**No backend changes.** The plan has been vetted against `services/camper-service/src/main/kotlin/` and `webapp/src/api/client.ts`. Two workstreams (W5, W20) have explicit FE-only scoping notes about limitations that would otherwise require BE work — those limitations are accepted and documented.

If during execution any agent believes a workstream requires a BE change, **stop and surface that to the user before proceeding** — do not silently expand scope.

## Database Changes
None.

## Phasing & Checkpoints

The plan defines this ship order. Land each phase fully (all PRs merged to main) before starting the next:

**Phase 1 — quick wins & user-flagged issues**
1. W11 (toast system) — ship first; later workstreams import it.
2. W2 + W3 (keep-open forms)
3. W5 (recipe info button on shopping list)
4. W4 (recipe link in meal plan)

**Phase 2 — routing & structure**
5. W1 (recipes routing)
6. W14 (persist recipe search)
7. W6 (meal plan tab hash)

**Phase 3 — high-value workflow improvements**
8. W12 (multi-day recipe add)
9. W13 (quick-create recipe in modal)
10. W17 (member→servings reconcile)
11. W16 (assignment progress badges)

**Phase 4 — polish & consistency**
12. W7 (confirm modal)
13. W29 (restart ladder copy) — depends on W7
14. W23 (delete + invite consistency) — depends on W7
15. W24 (tabs primitive)
16. W18 (servings context)
17. W19 (pending member visuals)
18. W20 (pending invitations rollup)
19. W15 (gear pack apply summary + Undo) — depends on W11
20. W9 (blur-save feedback) — depends on W11

**Phase 5 — discoverability & guidance**
21. W21 (promote gear packs + import)
22. W22 (ladder rules hint)
23. W27 (first-run onboarding)
24. W28 (profile setup context)
25. W10 (empty states)

**Phase 6 — sizing & mobile**
26. W25 (modal sizing polish)
27. W26 (mobile baseline)
28. W8 (keyboard support across forms)

**Checkpoint behavior.** After each phase fully lands on main:
- Write a short progress note to `docs/clean-up-ui/progress.md` (workstream IDs shipped, PR numbers, any deviations from plan).
- **Pause** and surface the progress note to the user before starting the next phase. Continue automatically only if the user explicitly says "go" or equivalent.

## Special Considerations

- **Plan is the source of truth.** Each workstream in `plan.md` has a Problem / Change / Files / Acceptance section. Implementations must satisfy the Acceptance criteria.
- **Two scoped limitations** (W5, W20) are intentional. Don't try to "fix" them by adding BE changes. Re-read the plan's "FE-only note" on each.
- **Standard feature-build flow per workstream:** architect (per-phase plan only — global plan exists) → web-dev → test-engineer → code-reviewer → test-reviewer → doc-updater.
- **No db-dev or kotlin-dev work** for any workstream. If those agents are spawned, that's a sign scope has crept.
- **Build gate.** `npm run build` is the canonical type-check (per `webapp/CLAUDE.md`). `npm run test` for tests. Both must pass on every PR.
- **No backend file changes.** Code-reviewer must reject any PR that modifies `services/`, `clients/`, `databases/`, or `libs/`.
- **Shared UI primitives.** New components (`Toast`, `ConfirmModal`, `Tabs`) belong in `src/components/ui/` per the existing convention. Migrate ad-hoc usage in the same workstream that introduces the primitive.
- **Aesthetic.** Preserve the parchment / campsite visual language — no new color tokens, no new fonts. Reuse existing CSS variables in `src/styles/theme.css`.
- **Live updates.** Don't break existing STOMP refetch logic in PlanPage / MealPlanModal / AssignmentsModal.
- **Mobile (W26).** Time-boxed baseline only — "usable on iPhone SE", not pixel-perfect.
- **Retro.** After all phases ship, run `doc-updater` once to update `webapp/CLAUDE.md` with new components/patterns introduced and write `docs/clean-up-ui/retro.md`.

## Notes
- Plan has been vetted FE-only end-to-end. See the verification table at the top of `plan.md`.
- This is the largest single feature in the repo so far — phased checkpoints are mandatory, not optional.
- If any phase produces enough PRs that review becomes unwieldy, split that phase further rather than lowering quality bars.
