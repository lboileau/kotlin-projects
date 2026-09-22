# recipe-favorites — retrospective

2026-09-21. Favourite a recipe, see how many people did and who, filter the list by Favourites / My favourites; plus category chips on the Ingredients list. One table, three endpoints, two response fields, a webapp pass that was redesigned three times while it was being built. Delivered as one PR.

## What went well

- **The plan was executable.** The client and DB sections were pasteable code that compiled unchanged; every implementing agent said so. Literal file contents plus the *reason* for each choice (why CASCADE, why no `recipe_id` index) is what let agents verify instead of trust.
- **Two design decisions in the plan prevented real bugs.** Mapper parameters for `favoriteCount` / `favoritedByMe` are required, not defaulted, so update / publish / resolve-duplicate could not silently return `0 / false` and poison the frontend cache — and the tests prove it (the publish test asserts `favoritedByMe` is `true`, not just the count). The optimistic cache edits are guarded and idempotent, so a rollback is `apply(current, !intended)` on the live cache and a double-decrement is structurally impossible.
- **Reviews found things.** The seed's `ON CONFLICT (id)` didn't guard the natural key (fixed to `(recipe_id, user_id)`); a soft pill that vanished into the row's hover tint; a publish regression test that asserted only half the regression; a duplicate unit test that never started from a favourite. None was caught by a green build.
- **No production bug was found by the test PRs** — 22 client integration tests, 28 service unit tests, 25 acceptance tests, all against the real contracts. Full suite: 1,497+ backend tests, 154 webapp tests.
- **Verification never touched the owner's data.** The migration was proven on a scratch database replaying all 44 migrations (`sort -V` matters: lexical order applies V010 before V002), then dropped.

## What went badly

- **The orchestrator could not be talked to.** It read its inbox only when one of its own agents finished, so owner decisions sat unread for up to 40 minutes — while the owner was iterating on the UI in real time. Its sub-agents' hand-backs frequently failed ("the agent that spawned you is no longer running") and landed with the session lead instead. The owner reasonably concluded it had crashed.
- **That produced two collisions.** (1) The lead, believing the orchestrator dead, started a second web-dev on the same files; stopped within two minutes, no damage. (2) Much worse: the lead built owner-requested UI changes directly (at the owner's request, for speed), the orchestrator — not having read any of the messages saying so — saw code that contradicted the written handoff, concluded an agent had invented an owner decision, and **dispatched an agent to revert the owner-approved UI**. It was caught mid-edit; the files were restored from the commit and the orchestrator was stopped.
- **The orchestrator was not wrong on the rule.** The handoff had not been amended before the code changed. "Amend the handoff first" exists precisely so a reader can tell an owner decision from an invention, and its reviewer flagged exactly that. The lead delegated the amendment to an agent that wasn't listening instead of writing it himself. Once the amendment was committed with the owner's own words, every "blocking" finding dissolved.
- **Eleven sequential PRs is the wrong shape for a feature whose UI is being designed live.** Backend was done and approved early; the frontend then needed four rounds of owner feedback, each of which was routed docs → agent → reviewer. The owner asked, fairly, why they couldn't just see the change. Direct edits followed by a single review of the settled design took minutes.
- **Shared working tree, several writers.** A commit landed on the wrong branch because the orchestrator switched branches between a status check and a commit; an amend swept another writer's uncommitted files into a commit. Survivable only because everything ships as one PR.

## What to change

1. **Whoever changes the code amends the handoff, in the same sitting.** Never delegate the amendment. Quote the owner. (Already the rule; this build shows the cost of bending it for speed.)
2. **Visual iteration happens outside the PR pipeline.** When the owner is looking at a dev server and reacting, edit directly, let them look, and review the settled design once. Run the staged workflow for the parts with contracts (DB, client, service, tests).
3. **An orchestrator must drain its inbox before every spawn and every git operation**, and must treat "code contradicts the handoff" as a question for the lead *and wait for the answer* — not announce a revert and start it in the same breath. Reverting owner-visible work is never the default resolution.
4. **One writer per working tree.** Parallel agents get worktrees, or strictly disjoint paths with explicit-path staging only — never `git add -A`, never `--amend` without checking `HEAD` first.
5. **Parallelise what is independent.** PR 9 and PR 10 (different files, no shared state) took a few minutes side by side; the sequential plan had them an hour apart.
6. **Skill updates worth making** (proposed, owner to decide): batched reads in service-manager (`bindList` + empty-list guard + "aggregates omit zero rows, so callers *and the fake* default"); required mapper params when a DTO gains a computed field; contract-PR build gates include the consuming service's `compileTestKotlin`; a scratch-database verification recipe in db-manager, and its TRUNCATE note corrected (`TRUNCATE … CASCADE` ignores `ON DELETE`; FK cascades are for real deletes); in web-manager, "no DOM test runner ⇒ logic lives in `lib/`" as a design rule, not a testing footnote.

## Known gaps, left on purpose

- `GET /api/recipes/{id}` still returns anyone's draft (the frontend's read-only draft callout depends on it), so the three new endpoints are stricter than the detail endpoint. Flagged in the plan; not changed here.
- The Recipes list's meal chips still carry their own copy of the chip markup and CSS rather than using `components/FilterChips`.
- The favourite toggle's `onSuccess` wiring is covered by review, not by a test: the webapp deliberately has no DOM or hook test runner, so only the pure cache edits are tested.
- `recipe-client`'s new integration harness covers favourites only; `create` / `update` / `findSimilarByName` and all of `ingredient-client` remain untested against a real Postgres.
- A pre-existing timing flake in `MealPlanClientIntegrationTest` (`updatedAt` from the JVM clock on insert vs the database clock on update) surfaced once under load during this work. Unrelated; worth a small fix of its own.
- `migrations/rollback/` is copied onto Flyway's production classpath; harmless today because `R044__…` is not a valid repeatable-migration name, but one line in `services/camper-service/build.gradle.kts` would close it for good.
- Not verified on a real phone: the favourite button's pop, the 360px fit of the dropdown and the title-line pill (checked in desktop Chrome at phone width only).
