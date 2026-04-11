# Orchestrator Handoff

## Workflow
feature-build

## Project Path
/Users/louisboileau/Development/kotlin-projects-worktrees/activity-selector/camper

## Feature Name
activity-ladder

## Plan
to be created by architect

## Feature Description

A standalone, public feature in the camper webapp for choosing a group activity via a double-elimination voting tournament ("ladder"). Any authenticated camper user can create a ladder, add activities, and share a public link. Anyone who visits the link joins the ladder, and users who are actively present on the page when the creator starts the ladder become **voters**. The ladder then runs globally-synchronized voting rounds: in each round, two random remaining activities are pitted against each other and every eligible voter must cast a vote before the round concludes. An activity that accumulates 2 losses is eliminated. Play continues until a single winning activity remains.

The feature is **standalone** — it is not tied to trip plans. It lives under a new top-level nav entry and a new route `/activities`. It uses existing camper user accounts.

### Core User Flow

1. **Create** — An authenticated user creates an activity ladder (title, and adds 2+ activities). Each activity has: `name`, `imageUrl`, `distanceMinutes`, `costPerPerson`. All four fields are purely informational and are displayed to voters.
2. **Share / View** — The ladder has a public URL. Any authenticated camper user can visit it. "Joining" is **purely implicit**: loading the ladder page opens a WebSocket connection and their presence is tracked in memory — there is no explicit join endpoint and no persisted participant record at this stage.
3. **Start** — The creator clicks "Start". At that moment, the server **snapshots the in-memory presence set** and persists those users as the ladder's voter list in `ladder_participants`. These users — and only these users — are eligible to vote for the rest of the ladder. There is no minimum count — the creator can start with any number of activities (≥2) and any number of present users (≥1, including just themselves).
4. **Voting Rounds** — The ladder selects a random pair of activities from the appropriate bracket and shows the matchup to everyone. Each eligible voter must vote (late joiners become **spectators** and cannot vote, but can watch). The round does not advance until every eligible voter has cast a vote, **including voters who have temporarily disconnected** — the game waits for them to reconnect.
5. **Tie Handling** — If the vote is a tie, neither activity records a loss. The pair is returned to the pool without a result and a new random pair is drawn (re-pairing is random; the same two activities could be drawn again).
6. **Elimination** — An activity with 2 losses is eliminated. Activities with 1 loss are in the **losers bracket**.
7. **Grand Final** — When only one activity remains in each of the winners/losers brackets, the UI labels the current round as the **Grand Final**. If the winners-bracket champion (0 losses) wins, they win the ladder. If the losers-bracket champion wins, the winners-bracket champion now also has 1 loss → a **Grand Final Reset** is played. The winner of that reset wins the ladder.
8. **Completion** — Once a single winner is determined, the ladder is marked `completed` and the winning activity is highlighted.
9. **Restart (creator only, at any time)** — The creator can restart the ladder from ACTIVE or COMPLETED status. This wipes the voter list, all votes, the current match, and all bracket/loss state, and returns the ladder to DRAFT with activities intact (name, imageUrl, distanceMinutes, costPerPerson preserved; losses reset to 0, bracket reset to WINNERS). From there the creator can optionally edit activities and click Start again to begin a fresh round of voting with whoever is currently present.

### Vote Reveal Rules

- Individual votes are **anonymous** — the UI never shows which user voted for which activity.
- While a round is open, the UI shows **which users have voted** (progress: "3 of 5 voted") but not their choices.
- When all eligible voters have voted, the round closes and **vote totals** are revealed (e.g., "Activity A: 3, Activity B: 2"). Individual attributions remain hidden.

### Presence & Voting Eligibility

- **Presence** is tracked via STOMP WebSocket connection lifecycle and is held **in memory only** — no persistent record exists for users who merely visit a ladder. A user is "present" as long as they have at least one active WebSocket session subscribed to the ladder's topic.
- **Eligible voters** = the set of users who were present the moment the creator clicked "Start". At Start time, the server reads the in-memory presence set and persists those users as rows in `ladder_participants`. This persisted set is frozen and is the sole source of truth for "who can vote".
- **Late joiners** (users who connect after Start, i.e., users NOT in `ladder_participants`) are **spectators** — they see the current matchup and all reveals via WebSocket, but cannot vote. No persistent record is created for them.
- **Temporary disconnects** by eligible voters do NOT remove them from the persisted voter set. The round waits for them to return and cast their vote. While they are disconnected, the in-memory presence set reflects that they are offline, and the UI should show "Waiting for <user> to reconnect" or similar.
- All presence changes (connect, disconnect, reconnect, vote cast) are broadcast via WebSocket in real time to everyone on the ladder topic.

## Entities

### Ladder
- `id: UUID`
- `creatorId: UUID` (FK → users)
- `title: String` (non-empty, max 200)
- `status: Enum` — `DRAFT` | `ACTIVE` | `COMPLETED`
- `currentRoundNumber: Int` (nullable; null in DRAFT)
- `currentMatchActivityAId: UUID?` (nullable; FK → ladder_activities)
- `currentMatchActivityBId: UUID?` (nullable; FK → ladder_activities)
- `isFinalRound: Boolean` (true when only 1 activity remains in each bracket)
- `isGrandFinalReset: Boolean` (true for the second grand final, if it happens)
- `winnerActivityId: UUID?` (nullable; set when COMPLETED)
- `createdAt: Timestamp`
- `updatedAt: Timestamp`

### LadderActivity
- `id: UUID`
- `ladderId: UUID` (FK → ladders, ON DELETE CASCADE)
- `name: String` (non-empty, max 200)
- `imageUrl: String` (non-empty, max 2000)
- `distanceMinutes: Int` (≥ 0)
- `costPerPerson: Decimal(10,2)` (≥ 0)
- `losses: Int` (default 0) — materialized loss counter, driven by vote resolution
- `bracket: Enum` — `WINNERS` (0 losses) | `LOSERS` (1 loss) | `ELIMINATED` (2 losses)
- `displayOrder: Int` — creator-defined ordering for display before start
- `createdAt: Timestamp`

### LadderParticipant (voter snapshot)
- `id: UUID`
- `ladderId: UUID` (FK → ladders, ON DELETE CASCADE)
- `userId: UUID` (FK → users)
- `createdAt: Timestamp`
- Unique constraint: `(ladder_id, user_id)`
- **Semantics:** rows exist **only for eligible voters**, and **only ever get inserted once**: at the moment the creator clicks Start, the server snapshots the in-memory presence set and bulk-inserts one row per currently-connected user. No rows are written before Start. No rows are written for late joiners (they are spectators, tracked only in memory). If a participant in the persisted table happens to be disconnected, they are still a voter — their absence is an in-memory presence fact, not a participant-table fact.

### LadderVote
- `id: UUID`
- `ladderId: UUID` (FK → ladders, ON DELETE CASCADE)
- `roundNumber: Int`
- `userId: UUID`
- `votedForActivityId: UUID` (FK → ladder_activities)
- `createdAt: Timestamp`
- Unique constraint: `(ladder_id, round_number, user_id)` — one vote per user per round

## API Surface

All endpoints live under `/api/ladders`. Authentication required for all (existing camper session auth).

1. **`POST /api/ladders`** — Create a new ladder.
   - Request: `{ title: String, activities: [{ name, imageUrl, distanceMinutes, costPerPerson }] }`
   - Response: `{ ladder: LadderView }`
   - The ladder is created in `DRAFT` status.

2. **`GET /api/ladders`** — List all ladders (public; no filtering by creator).
   - Response: `{ ladders: [LadderSummaryView] }`

3. **`GET /api/ladders/{id}`** — Get full ladder detail including activities, participants, current match, and current round vote progress.
   - Response: `{ ladder: LadderDetailView }`

4. **`POST /api/ladders/{id}/activities`** — Add an activity (creator only; DRAFT only).
   - Request: `{ name, imageUrl, distanceMinutes, costPerPerson }`
   - Response: `{ activity: ActivityView }`

5. **`DELETE /api/ladders/{id}/activities/{activityId}`** — Remove an activity (creator only; DRAFT only).

6. **`POST /api/ladders/{id}/start`** — Creator starts the ladder.
   - Preconditions: caller is creator; status is DRAFT; at least 2 activities.
   - Side effects: reads the in-memory presence set for this ladder, bulk-inserts one `ladder_participants` row per currently-connected user (the frozen voter set), sets ladder status to `ACTIVE`, selects the first random matchup, broadcasts `{ resource: "ladder", action: "started" }`.

7. **`POST /api/ladders/{id}/vote`** — Cast a vote on the current match.
   - Request: `{ votedForActivityId: UUID }`
   - Preconditions: caller has a row in `ladder_participants` for this ladder (i.e., is an eligible voter); status is ACTIVE; target activity is one of the two in the current match; caller has not already voted this round.
   - Response: `{ voteCount: Int, votersRemaining: Int }`
   - Side effects: persists vote; if all voters have now voted, resolves the round (see resolution logic below) and advances state accordingly; broadcasts appropriate events.

8. **`POST /api/ladders/{id}/restart`** — Creator resets the ladder back to a fresh DRAFT state.
   - Preconditions: caller is the creator; status is ACTIVE or COMPLETED (restarting a DRAFT ladder is a no-op and should return 409 or succeed idempotently — architect's call).
   - Side effects (all within a single transaction):
     - Delete all rows in `ladder_votes` for this ladder.
     - Delete all rows in `ladder_participants` for this ladder.
     - For every row in `ladder_activities` for this ladder, set `losses = 0` and `bracket = WINNERS`.
     - On the `activity_ladders` row: set `status = DRAFT`, `currentRoundNumber = null`, `currentMatchActivityAId = null`, `currentMatchActivityBId = null`, `isFinalRound = false`, `isGrandFinalReset = false`, `winnerActivityId = null`, bump `updatedAt`.
   - Broadcasts `{ resource: "ladder", action: "restarted" }`. All connected clients refetch the ladder; the in-memory presence set is untouched (users who are currently connected remain connected, they are just no longer "voters" until a fresh Start is clicked).
   - The creator is free to add/remove activities and then click Start again to begin fresh voting.

**Target: 8 REST endpoints.** Well under the 10-endpoint cap. Note: there is **no** `POST /join` endpoint — opening a WebSocket connection to `/topic/ladders/{id}` is the only "join" action, and it creates no persistent state until (and unless) the creator clicks Start.

### Round Resolution Logic (server-side, after final vote cast)

1. Count votes for A and B.
2. **Tie:** record no losses; pick a new random pair from the current pool (using the same bracket selection rules as below); increment `currentRoundNumber`; do NOT persist tie votes as a "result" (votes are still persisted in `ladder_votes`, but no activity's `losses` counter changes); broadcast `{ resource: "ladder", action: "round-resolved", outcome: "tie" }`, then `{ action: "round-started" }`.
3. **Winner decided:** increment loser's `losses`. If `losses == 1`, move loser to `LOSERS` bracket. If `losses == 2`, move loser to `ELIMINATED`.
4. **Check for ladder completion:** if only one non-eliminated activity remains, mark ladder `COMPLETED`, set `winnerActivityId`, broadcast `{ action: "completed" }` and stop.
5. **Otherwise, select next pairing:**
   - If both brackets have ≥2 activities, randomly pick which bracket plays this round (or alternate; the architect can decide a deterministic scheme — random is acceptable).
   - If one bracket has 1 and the other has ≥2, play the larger bracket.
   - If both brackets have exactly 1 activity each → **Grand Final** (or **Grand Final Reset** if `isGrandFinalReset` should now apply): set `isFinalRound = true`, pair the two finalists. If the losers-bracket finalist wins the Grand Final (giving the winners-bracket finalist their first loss), set `isGrandFinalReset = true` and re-pair the same two activities for one more round. If the winners-bracket finalist wins the Grand Final, they win the ladder immediately.
6. Broadcast `{ action: "round-resolved", outcome: "decided", winnerActivityId, voteTotals: {...} }` followed by `{ action: "round-started" }`.

## Database Changes

Four new tables, all with UUID PKs and `created_at` / `updated_at` where appropriate. New migration files under `databases/camper-db/migrations/` starting at the next available version number (V040 and onward).

1. **`activity_ladders`** — ladder metadata and current round state (columns match the `Ladder` entity above).
2. **`ladder_activities`** — activities belonging to a ladder, with losses counter and bracket enum.
3. **`ladder_participants`** — the frozen voter snapshot, populated only at Start time from the in-memory presence set. Unique `(ladder_id, user_id)`. Empty for any ladder still in DRAFT.
4. **`ladder_votes`** — one row per vote cast. Unique `(ladder_id, round_number, user_id)`.

All FKs to `activity_ladders.id` should be `ON DELETE CASCADE`. No changes to existing tables. Seed data is optional (a small demo ladder in `dev_seed.sql` would be nice-to-have but not required).

## Special Considerations

### WebSocket / Real-Time
- Add a new STOMP topic: `/topic/ladders/{ladderId}`. Model after the existing `PlanEventPublisher` pattern but in a new `LadderEventPublisher` class so ladders and plans remain independent.
- **Presence bookkeeping is in-memory only** and is the sole reason an in-memory tracker exists. Use a `ConcurrentHashMap<UUID, Set<UUID>>` (ladderId → currently-connected userIds) encapsulated in a `LadderPresenceTracker` component. This tracker serves two purposes and two only:
  1. Power the "who is currently on the page" UI in both DRAFT and ACTIVE states (via `presence-changed` WebSocket broadcasts).
  2. Provide the snapshot that gets read at Start time to populate `ladder_participants`.
- Nothing about a user visiting a ladder is persisted until the creator clicks Start.
- Listen for Spring `SessionSubscribeEvent` / `SessionUnsubscribeEvent` / `SessionDisconnectEvent` to maintain the presence set. Model after how the existing codebase handles WS session events (check `services/camper-service/src/main/kotlin/com/acme/services/camperservice/websocket/` for patterns).
- Every mutation (connect, disconnect, activity added/removed in draft, start, vote cast, round resolved, completed) must publish a `{ resource, action, payload? }` envelope to the ladder topic. The frontend subscribes and refetches / updates accordingly.
- The backend must **never advance a round until every row in `ladder_participants` has a corresponding vote in `ladder_votes` for the current round**, regardless of whether those users are currently connected. A disconnected voter still blocks round advancement; when they reconnect, the UI must show them the currently open match so they can cast their delayed vote.
- The presence tracker must broadcast `presence-changed` events as users connect/disconnect so all clients can update their "present / waiting for" UI. In ACTIVE state the client combines the persisted voter list (from the ladder detail endpoint) with live presence (from WebSocket) to render "waiting for <user>" indicators.

### Auth & Permissions
- All endpoints require an authenticated camper user session. Connecting to the WebSocket topic also requires an authenticated session.
- Only the creator can: add/remove activities in DRAFT, start the ladder, restart the ladder.
- Only users with a row in `ladder_participants` for the ladder can vote; all other connected users are spectators.
- Anyone authenticated can: list ladders, view any ladder's detail, and connect to any ladder's WebSocket topic. Ladders are fully public within the camper user base.

### Validation Rules (per existing patterns: validations 1:1 with actions)
- Ladder title: non-empty, max 200 chars.
- Activity name: non-empty, max 200 chars.
- Activity imageUrl: non-empty, max 2000 chars (format validation: must look URL-ish; the architect can pick exact rules).
- Activity distanceMinutes: integer, ≥ 0.
- Activity costPerPerson: decimal, ≥ 0.
- At least 2 activities required to start a ladder.
- Vote target must be one of the two activities in the current match.
- Vote may only be cast once per user per round.

### Concurrency
- Round resolution must be atomic. Two voters casting the final vote simultaneously could both trigger "last vote → resolve round". Use a transactional DB check (e.g., count votes and compare to voter count under `SELECT ... FOR UPDATE` on the ladder row) so resolution runs exactly once.

### Frontend (webapp)
- New top-level nav item: **Activities** (route `/activities`).
- `/activities` — list view of all ladders with status badge.
- `/activities/new` — create form (title + add/remove activities inline).
- `/activities/:id` — the live ladder page. Subscribes to `/topic/ladders/:id` over STOMP. States:
  - **DRAFT (creator):** show activity list with add/remove, "Start Ladder" button, the people panel (see below).
  - **DRAFT (non-creator):** show activity list (read-only), the people panel, "waiting for creator to start".
  - **ACTIVE (voter, round open, not yet voted):** show current matchup, two activity cards, vote buttons, voter progress (N of M voted), "Final Round" / "Grand Final Reset" banner when applicable, the people panel, and the creator's "Restart Ladder" button.
  - **ACTIVE (voter, round open, already voted):** show same matchup but voting is locked; show progress; people panel; creator's restart button.
  - **ACTIVE (spectator):** show current matchup, progress, cannot vote; clearly labeled "You are watching — voting was locked at start"; people panel; creator's restart button.
  - **ACTIVE (round resolved, before next round starts):** briefly show vote totals + outcome (or "tie — reshuffling") before the next matchup appears.
  - **COMPLETED:** show winning activity highlighted; people panel; creator's "Restart Ladder" button remains available.
- Follow existing webapp patterns (see `webapp/src/components/ItineraryModal.tsx`, `webapp/src/pages/PlanPage.tsx`, and related WebSocket-subscribing components for prior art).

#### People Panel

A persistent panel on the ladder page (sidebar or header strip — architect's call) that shows user avatars + names, modeled on the member list already used on `PlanPage.tsx`. Reuse the existing `CamperAvatar` component and the `avatar-generator` lib so avatars render the same way as elsewhere in the app. The panel's contents depend on ladder status:

- **DRAFT:** A single section labeled "In the room" containing every user currently connected to the ladder WebSocket topic (sourced from live `presence-changed` events). No persistent distinction between users at this stage — everyone here is a candidate voter.

- **ACTIVE / COMPLETED:** Two visually distinct groups:
  1. **Voters** — the users listed in `ladder_participants` (the frozen Start-time snapshot). Each voter is rendered with their avatar + name and an **online/offline indicator** driven by the in-memory presence set, plus a per-round **voted / not-yet-voted** indicator. Voters who are currently disconnected render dimmed with an "offline — waiting" marker so the "Waiting for <user>" situation is immediately legible. Voters who have voted in the current round show a ✓ (without revealing which activity they picked).
  2. **Watching** — any currently-connected user who is NOT in `ladder_participants`. These are late joiners / spectators. Rendered in a separate sub-section so they are obviously distinct from voters. They have no voted/not-voted indicator. When they disconnect they simply vanish from this list (no persistence).

- **After Restart:** the ladder returns to DRAFT, so the panel collapses back to the single "In the room" section based purely on live presence. Users who were voters a moment ago lose their voter styling immediately (because `ladder_participants` is now empty).

The panel must update live via WebSocket events — both `presence-changed` (connect/disconnect) and vote/round events.

### Testing
- Client unit tests with a `FakeActivityLadderClient`.
- Service unit tests for each action, including round-resolution logic with every interesting case: tie, decided, bracket transitions, grand final, grand final reset, concurrent final votes, and **restart from both ACTIVE and COMPLETED**.
- Acceptance tests with `@SpringBootTest` + Testcontainers covering the full create → connect → start → vote → complete flow, plus tie reshuffling, plus late-joiner spectator enforcement, plus waiting-for-disconnected-voter behavior, plus restart-then-replay (restart an in-flight ladder, confirm state is reset, Start again, complete a fresh ladder with possibly different voters).
- WebSocket integration test verifying presence tracking and event broadcasts (see existing `WebSocketIntegrationTest.kt`), including that `presence-changed` fires on connect and disconnect and that `restarted` broadcasts reach all connected clients.

## Notes

- This feature is **standalone**: it is not attached to plans, trips, or any existing camper entity. It only depends on the existing `users` table for identity.
- The branch name is `activity-selector` but the feature/directory name is `activity-ladder` (chosen because "ladder" is the user's term of art for this tournament-style flow). The route is `/activities` per the user's request.
- The architect should determine: exact bracket-selection scheme when both brackets have ≥2 activities (random is fine), whether to persist the bracket-selection RNG seed for reproducibility (probably not needed), and whether to store a `round_log` for replay/debugging (out of scope — user said no history required, and materialized `losses` + `bracket` on activities is sufficient state).
- Scope: 4 new tables, 1 new client (`activity-ladder-client`), 1 new service feature (`activityladder`), 8 REST endpoints, 1 new WebSocket topic + publisher + presence tracker, ~6 new frontend routes/components (including the people panel which reuses `CamperAvatar`). Confirmed within the /build-feature scope cap.
- The architect/kotlin-dev/db-dev/web-dev teams should closely follow the patterns in existing features (`itinerary` and `gearpack` are the most recently-built and most relevant references for validations + WebSocket live updates).
