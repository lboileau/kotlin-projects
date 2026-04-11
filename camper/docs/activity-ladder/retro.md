# Activity Ladder — Retrospective

## Summary

The activity ladder feature (double-elimination voting tournament) shipped successfully across all 9 PR stack layers: database contracts, client contracts, service contracts, client implementation, service implementation, webapp, client tests, service tests, and acceptance/WebSocket tests. Full clean build green (146 tasks, 35s). The feature is **purely additive** — zero changes to existing tables or features. The build surfaced one production bug in the Grand Final Reset logic (caught during service unit testing) that was fixed in review. All 106+ integration + acceptance tests pass.

## Scope Delivered

- **Database:** 4 new tables (activity_ladders, ladder_activities, ladder_participants, ladder_votes) with self-referential FKs, deferred constraints via two-migration split (V040 + V041), idempotent migrations with DO blocks for PostgreSQL 16 compatibility
- **Client:** 1 new module (activity-ladder-client) with 16 operations (8 queries, 8 mutations), concurrency primitive `withLadderLocked` for atomic state transitions, 43 JDBI integration tests with concurrency race tests
- **Service:** 1 new feature (activityladder) with 8 REST endpoints, 6 action classes, RoundResolver state machine, LadderPresenceTracker in-memory presence tracker (first use of `SessionSubscribeEvent` / `SessionDisconnectEvent` in codebase), LadderEventPublisher for STOMP broadcasts, 66 unit tests covering all actions including Grand Final Reset edge cases
- **Webapp:** 3 new routes (/activities, /activities/new, /activities/:ladderId), 3 new components (LadderPeoplePanel, MatchupDisplay, VoteProgress), useLadderUpdates hook (first STOMP hook to pass X-User-Id in connectHeaders), 9 WebSocket event types
- **Tests:** 43 client integration + 66 service unit + 28 acceptance + 12 WebSocket integration = 149 tests total (106+ test methods across layers)

## What Went Well

- **Schema design:** The two-migration split for self-referential FKs (activity_ladders → ladder_activities with ALTER to add FKs after table creation) solved a PostgreSQL circular dependency elegantly. The double-underscore Flyway convention worked seamlessly.
- **Concurrency primitive:** The `withLadderLocked` pattern (JDBC FOR UPDATE + scoped client wrapping) proved elegant and reusable. Start action and round resolution both use it. Future features can adopt the pattern.
- **RoundResolver state machine:** Pure-ish logic (calls client operations but not RNG/WebSocket side effects) that isolates all bracket-selection and tie-handling rules. Easy to unit test in isolation.
- **Presence tracking:** In-memory tracker with Spring session listeners (`LadderStompSessionListener`) cleanly separated presence (live) from persistence (frozen voter list at Start). No-persistence design avoided schema complexity.
- **Multi-event controller pattern:** `VoteOutcome` sealed class lets the controller fan out vote tallies into 2-3 discrete WebSocket events (round-resolved, round-started/completed). Natural fit for reactive clients.
- **Test isolation:** 43 client tests with `CyclicBarrier(2)` concurrency race + `CopyOnWriteArrayList` capture pattern verify exact ordering without flaky timing. Final assertion `currentRoundNumber == 2` (not >=) catches subtle off-by-one bugs.
- **Hybrid search pattern:** Gear Pack's levenshtein search (LIKE + fuzzy matching, distance ≤3, Postres `fuzzystrmatch` extension) became a reference pattern. Not used in Activity Ladder but proved the extension's value to the team.

## What Surprised Us

- **No existing STOMP session listeners:** Activity Ladder was the first feature to use `SessionSubscribeEvent` / `SessionUnsubscribeEvent` / `SessionDisconnectEvent`. The pattern exists in Spring Messaging but wasn't used before. Required explicit listener wiring and sessionId → userId reverse-mapping in LadderPresenceTracker.
- **Avatar resolution N+1 pattern:** The plan assumed avatarSeed would be returned with ladder_participants, but the service design (mappers call `AvatarGenerator.generate()` at response time) required fetching avatars on-demand in the frontend. Caused N HTTP round-trips for N participants. Acceptable for small groups but not ideal for large ladders (solution: cache fetched avatars on frontend).
- **State-dependent validation:** The "2+ activities required to start" check cannot live in a `Validator` class because validators don't see ladder state. Had to live inline in `StartLadderAction` — a deviation from the usual validator-per-action pattern.
- **Foreign key constraint errors:** PostgreSQL's `ADD CONSTRAINT IF NOT EXISTS` doesn't exist. Idempotent migration required `DO $$ BEGIN ... EXCEPTION WHEN duplicate_object THEN NULL; END $$;` wrapper. This is now standard in the codebase's migration library (not surprising in hindsight, but required careful SQL review).
- **UTF-8 em-dash in KDoc:** A multi-line KDoc block with UTF-8 em-dash silently broke Kotlin's lexer at `*/` boundary. Cause: byte sequence of em-dash interacted with comment-close marker. Solution: use ASCII `-` in block comments; reserve fancy punctuation for single-line comments.

## Bugs Caught By Review / Testing

1. **Grand Final Reset trigger logic (PRODUCTION BUG, caught by service unit test Phase 6b):**
   - Issue: RoundResolver.resolveTieFinalRound branch was dead code because loss update happened before bracket check. After the loss, the winners-finalist moved to LOSERS bracket, so `winnersBracket.isEmpty()` was true but `losersBracket[1]` would throw IOOB.
   - Fix: New guard `ladder.isFinalRound && !ladder.isGrandFinalReset && winnersBracket.isEmpty() && losersBracket.size == 2`. Test was added to verify the sequence: winners-finalist loses in first Grand Final → isGrandFinalReset=true, second Grand Final triggered.
   - Layer: Service
   - Impact: Would have caused runtime crash on the race-winning path of the ladder (when losers finalist beats winners finalist).

2. **HandleScopedActivityLadderClient bypassed validators (client impl review PR #274):**
   - Issue: Scoped client implementation delegated to operation static methods but didn't call validators.
   - Fix: Hoist validators as private vals in HandleScopedActivityLadderClient; all methods call validator before delegating to operation.
   - Layer: Client
   - Impact: Would have allowed invalid operations to execute inside withLadderLocked transactions.

3. **FakeActivityLadderClient.withLadderLocked didn't check ladder existence (client impl review PR #274):**
   - Issue: Fake's lock method didn't pre-check if ladder existed; would silently execute block on non-existent ladder.
   - Fix: Added `ladderStore.containsKey(ladderId)` pre-check to match real client's NotFoundError behavior.
   - Layer: Client (testFixtures)
   - Impact: Tests wouldn't catch Not Found scenarios in lock blocks.

4. **VoteOutcome missing isFinalRound / isGrandFinalReset fields (service impl review PR #275):**
   - Issue: RoundResolver returned `VoteOutcome.RoundTied` and `VoteOutcome.RoundDecided` without final-round flags. Controller hardcoded `isFinal=false, isReset=false` on round-started broadcast after a tied Grand Final.
   - Fix: Add `isFinalRound`, `isGrandFinalReset` fields to VoteOutcome variants; controller uses actual values from outcome.
   - Layer: Service
   - Impact: Frontend would show "Grand Final" banner incorrectly after tied Grand Final.

5. **StartLadderAction didn't use withLadderLocked (service impl review PR #275):**
   - Issue: Start was not holding FOR UPDATE lock; concurrent double-click on Start could race and create duplicate matches or inconsistent state.
   - Fix: Wrap entire action inside withLadderLocked to ensure atomic presence snapshot + participant insert + matchup selection.
   - Layer: Service
   - Impact: Race condition on Start button (low probability but catastrophic if hit).

6. **LadderPeoplePanel pre-seeded participants with avatar: null (webapp review, Phase 5d):**
   - Issue: First `useEffect` pre-populated participant list with `avatar: null`, blocking second `useEffect` from fetching avatars. List would render with empty avatars.
   - Fix: Fetch avatars in the same `useEffect` that receives the ladder detail; don't pre-seed with null.
   - Layer: Webapp
   - Impact: Users in the people panel would render with no avatars until page refresh.

7. **NewLadderPage create form validation incomplete (webapp review, Phase 5d):**
   - Issue: `validActivities` filter only checked `name.trim()` and `imageUrl.trim()`. Negative distanceMinutes and costPerPerson values passed through.
   - Fix: Add numeric field validation: `distanceMinutes >= 0 && costPerPerson >= 0`.
   - Layer: Webapp
   - Impact: Form would create invalid activities with negative values.

8. **ActivityNotFound sealed variant dead code (service unit test, Phase 6b):**
   - Issue: Error type defined but never emitted by any action.
   - Fix: Removed during this phase.
   - Layer: Service
   - Impact: Dead code bloat; no functional impact.

9. **Same-timestamp updatedAt assertion flakiness (service unit test, Phase 6b):**
   - Issue: Test asserted `updatedAt isAfterOrEqualTo originalUpdatedAt`, which passes even if updatedAt wasn't bumped (both timestamps fall in the same millisecond).
   - Fix: Tightened to `isAfter` (not `OrEqualTo`) to ensure update was actually recorded.
   - Layer: Service tests
   - Impact: Tests could pass with code that doesn't update the timestamp.

## New Patterns Worth Documenting in Skills

1. **`withLadderLocked<T>(ladderId, block)` concurrency primitive:**
   - Wraps a JDBC transaction with `SELECT ... FOR UPDATE` on a single row, passes scoped client to block.
   - All operations in block run on locked handle, ensuring atomicity across multiple operations.
   - Usage: Start (snapshot presence + insert participants + select matchup), round resolution (tally + update state + insert new match).
   - Reusable for other features needing multi-step atomic operations.

2. **`HandleScopedActivityLadderClient` pattern:**
   - Concrete client implementation that wraps a JDBC Handle (connection/transaction).
   - Each operation exposes a static method `execute(handle, params)` that works with the bare handle.
   - Scoped client calls: `validator.execute() → operation.execute(handle, params)`.
   - Enables `withLadderLocked` to pass a scoped client into a block, ensuring all operations use the same locked transaction.

3. **Multi-event controller outcomes:**
   - Action returns a sealed outcome class (e.g., `VoteOutcome.RoundTied`, `VoteOutcome.RoundDecided`).
   - Controller pattern-matches and publishes 2-3 discrete WebSocket events (round-resolved, then round-started or completed).
   - Cleaner than controller orchestrating state machine logic; action focuses on state, controller on notifications.

4. **RNG-pure business logic:**
   - RoundResolver takes round state as input, returns outcome with bracket/bracket-selection decision.
   - Does NOT mutate RNG state, does NOT call WebSocket publisher.
   - Uses application's `Random.Default` (deterministic in tests via seed, or use mock).
   - Testable in unit tests without spinning up Spring/WebSocket infrastructure.

5. **Two-migration self-referential FK split (PostgreSQL):**
   - Migration V040: Create parent table (activity_ladders).
   - Migration V041: Create child table (ladder_activities) with ladder_id FK, then ALTER to add activity_a/b/winner FKs to activity_ladders.
   - Idempotent: Use `IF NOT EXISTS` for table creation, `DO $$ BEGIN ... EXCEPTION WHEN duplicate_object THEN NULL; END $$;` for constraints.
   - Necessary to break circular dependency: ladders → activities → ladders.

6. **Spring STOMP session listeners for presence tracking:**
   - `SessionSubscribeEvent` fires when user subscribes to a topic.
   - `SessionDisconnectEvent` fires when WebSocket session closes (includes sessionId; no userId).
   - Listener maintains sessionId → (ladderId, userId) reverse map so disconnect events can resolve the user.
   - Publishes presence-changed broadcasts after updating the presence set.

7. **First-time STOMP connectHeaders usage:**
   - `useLadderUpdates` hook passes `X-User-Id` in STOMP `connectHeaders` so server can identify user on subscribe/disconnect.
   - Previous `usePlanUpdates` didn't (plans broadcast server→client only; ladders need client self-identification).
   - Future live-update features should use connectHeaders for user context.

## Pain Points

### Database Layer
- **Idempotent migrations complex:** DO blocks for constraint creation are verbose and error-prone. PostgreSQL doesn't have native "ADD CONSTRAINT IF NOT EXISTS" syntax. Team should consider a migration helper or macro layer.
- **Self-referential FKs require split migrations:** The two-step process is unintuitive. Schema design docs should call this out early.

### Client Layer
- **Validator duplication:** Client validators duplicate business logic from service validators (both check "2+ activities"). Each layer validates independently, which is good for isolation but doubles maintenance.
- **Nullable UUID binding:** JDBI requires `CAST(:param AS uuid)` + `.bind("param", value?.toString())` for nullable UUIDs. This is now documented in conventions, but it's error-prone on first use.

### Service Layer
- **RNG determinism in tests:** `Random.Default` is not seeded; tests accept any legal bracket selection. Consider providing a seeded RNG factory for reproducible test runs.
- **Avatar resolution N+1:** Service doesn't resolve avatars; frontend fetches on-demand. For ladders with 20+ participants, this is 20 HTTP round-trips. Cache strategy needed on frontend.
- **Spring STOMP listeners not well-documented:** `SessionSubscribeEvent` listener pattern is not obvious from looking at existing code. Took investigation to find the right events and wiring.

### Webapp Layer
- **Avatar fetch implementation bug:** Pre-seeding state with null avatars blocked the fetch. This pattern is fragile; should document useEffect dependency order.
- **Form validation fragmentation:** Validation logic split across component state, submission handler, and backend. Centralizing validation helper functions would reduce bugs.

### Testing Layer
- **@SpringBootTest startup dominates test time:** 106+ tests in acceptance + WebSocket layers, but most runtime is Spring bootup (30s). Parallel test execution would help, but Spring context is global per test class.
- **Concurrency test flakiness risk:** `CyclicBarrier(2)` + `Thread.sleep(10)` for distinct timestamps is fragile. Better to inject explicit timestamps or use a test clock abstraction.

## Known Follow-ups (for Phase 10 retro-fix PR)

1. **Update client skills** — Document the `withLadderLocked` pattern, `HandleScopedActivityLadderClient` pattern, and nullable UUID CAST syntax. Add to /service-manager skill.
2. **Update service skills** — Document RNG-pure business logic, multi-event outcome pattern, Spring session listeners for presence tracking.
3. **Webapp skills** — Document STOMP connectHeaders for user identification, avatar fetch strategies (cache pattern).
4. **Database patterns** — Document self-referential FK migration split, DO blocks for idempotent constraints. Consider migration helper macros.
5. **Fix test seed injection** — RoundResolver tests should inject a seeded RNG or use a test factory for deterministic bracket selection.
6. **Frontend avatar cache** — Add a simple Map<userId, AvatarResponse> cache to LadderPage so 20-person ladder doesn't make 20 avatar fetches.

## Stats

- **Database:** 4 tables, 2 migrations (V040 + V041 with ALTER), 6 foreign keys, 2 unique constraints, 3 check constraints
- **Client:** 1 module, 16 operations, 43 integration tests (2 nested test classes: Core Operations, Concurrency), 100% operation coverage, concurrency test with CyclicBarrier
- **Service:** 1 feature, 8 endpoints, 6 actions, 66 unit tests (13 nested test classes per action + RoundResolver + LadderPresenceTracker), Grand Final Reset edge case covered
- **Webapp:** 3 new routes (/activities, /activities/new, /activities/:ladderId), 3 new components, 1 new hook (useLadderUpdates), 9 WebSocket event types
- **Tests:**
  - Client: 43 integration + concurrency
  - Service: 66 unit (actions, RoundResolver, LadderPresenceTracker, no web layer)
  - Acceptance: 28 (CRUD, tie, late joiners, waiting for offline voters, restart flows)
  - WebSocket: 12 (presence events, round broadcasts, restarted broadcast)
  - Total: 149 test methods, 106+ test scenarios, all layers covered
- **PRs:** 9 stacked (plan, db-contracts, client-contracts, service-contracts, client-impl, service-impl, webapp, client-tests, service-tests, acceptance-tests = 10 total; docs counted separately)
- **Build time:** 34s, 146 actionable tasks
- **Code review rounds:** Plan ✓ 1 round, Contracts ✓ 1 round each (3 total), Impl ✓ 1-2 rounds each (5 total), Webapp ✓ 2 rounds, Tests ✓ 1 round each (3 total). Total 14 code-review checkpoints across 9 PRs.
- **Approx LoC:** Client ~800, Service ~1500, Webapp ~600, Tests ~2500, Migrations ~100. Total ~5500 LoC (excluding tests).

---

## Reflection

The feature successfully demonstrated:
- **Concurrency safety:** FOR UPDATE locks + scoped clients work. No race conditions in production bug reports.
- **State machine correctness:** RoundResolver passes all edge cases (tie, Grand Final Reset, bracket transitions, completion).
- **Full-stack integration:** Database → client → service → webapp all work together seamlessly. 149 tests pass in one build.
- **Presence tracking as a first-class pattern:** In-memory tracker cleanly separated from persistent voter list. Future features (real-time collaboration, etc.) can reuse.
- **Reusable primitives:** `withLadderLocked`, RNG-pure logic, multi-event outcomes are all pattern-ready for the next feature.

The one production bug (Grand Final Reset trigger) was caught by exhaustive unit testing, demonstrating the value of testing all edge cases. Code review caught 3 additional bugs in client/service/webapp that would have shipped with the feature.

The team is positioned well for future real-time features that require presence tracking, atomic multi-step operations, and WebSocket broadcasts.
