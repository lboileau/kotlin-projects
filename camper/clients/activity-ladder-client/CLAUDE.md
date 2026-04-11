# activity-ladder-client

JDBI data access client for the activity ladder feature — double-elimination voting tournaments.

## Package
`com.acme.clients.activityladderclient`

## Public API (`ActivityLadderClient` interface)

### Queries
- `createLadder(param: CreateLadderParam): Result<Ladder, AppError>` — Create a new ladder in DRAFT status, auto-assigned creator_id
- `getLadderById(param: GetLadderByIdParam): Result<Ladder, AppError>` — Fetch a ladder by ID with all state fields
- `getLadderList(param: GetLadderListParam): Result<List<Ladder>, AppError>` — List all ladders, newest first, with limit/offset pagination (MAX_LIMIT 100)
- `getActivities(param: GetActivitiesParam): Result<List<LadderActivity>, AppError>` — List all activities for a ladder, ordered by display_order

### Mutations (activity management — DRAFT only)
- `addActivity(param: AddActivityParam): Result<LadderActivity, AppError>` — Add an activity; assigns display_order = MAX+1 atomically, returns created activity with computed order
- `removeActivity(param: RemoveActivityParam): Result<Unit, AppError>` — Delete an activity from a ladder (returns NotFoundError if not found)

### Mutations (ladder state)
- `updateLadderState(param: UpdateLadderStateParam): Result<Ladder, AppError>` — Update status, currentRoundNumber, current match activities (A/B), isFinalRound, isGrandFinalReset; returns updated ladder
- `setLadderWinner(param: SetLadderWinnerParam): Result<Ladder, AppError>` — Atomically set winner_activity_id and mark status COMPLETED
- `updateActivityLossAndBracket(param: UpdateActivityLossAndBracketParam): Result<Unit, AppError>` — Increment losses and set bracket on a single activity (called by RoundResolver after each vote tally)

### Mutations (voting round & participants)
- `bulkInsertParticipants(param: BulkInsertParticipantsParam): Result<Int, AppError>` — Idempotent bulk insert of participants at Start time (ON CONFLICT DO NOTHING); returns count inserted
- `castVote(param: CastVoteParam): Result<Unit, AppError>` — Cast a single vote for current round (called inside withLadderLocked transaction); returns ConflictError if vote already cast for this round
- `getParticipants(param: GetParticipantsParam): Result<List<LadderParticipant>, AppError>` — List all participants (eligible voters) for a ladder
- `getVotesForRound(param: GetVotesForRoundParam): Result<List<LadderVote>, AppError>` — Fetch all votes cast in a given round
- `countVotesForRound(param: CountVotesForRoundParam): Result<Int, AppError>` — Count votes for a round (cheap check before resolution)

### Mutations (restart)
- `restartLadder(param: RestartLadderParam): Result<Unit, AppError>` — Transactional restart: delete all votes + participants, reset all activities (losses=0, bracket=WINNERS, displayOrder unchanged), reset ladder state (status=DRAFT, currentRoundNumber/matches/flags all cleared)

### Concurrency primitive
- `withLadderLocked<T>(ladderId: UUID, block: suspend HandleScopedActivityLadderClient.() -> Result<T, AppError>): Result<T, AppError>` — Execute a block with an exclusive FOR UPDATE lock on the ladder row. The block receives a scoped client (`HandleScopedActivityLadderClient`) that shares the same transaction/handle as the lock, ensuring all operations run atomically. All vote casting and round resolution must happen inside this lock.

## Model Types

### `Ladder`
- `id: UUID`
- `creatorId: UUID`
- `title: String`
- `status: LadderStatus` — `DRAFT`, `ACTIVE`, `COMPLETED`
- `currentRoundNumber: Int?` — null in DRAFT; set to 1 at Start
- `currentMatchActivityAId: UUID?`
- `currentMatchActivityBId: UUID?`
- `isFinalRound: Boolean` — true when only 1 activity per bracket remains
- `isGrandFinalReset: Boolean` — true if Grand Final Reset is in progress
- `winnerActivityId: UUID?` — set when COMPLETED
- `createdAt: Instant`
- `updatedAt: Instant`

### `LadderStatus` enum
- `DRAFT` — ladder under construction; activities can be added/removed
- `ACTIVE` — voting in progress; participants frozen
- `COMPLETED` — winner determined

### `LadderActivity`
- `id: UUID`
- `ladderId: UUID`
- `name: String`
- `imageUrl: String`
- `distanceMinutes: Int`
- `costPerPerson: BigDecimal`
- `losses: Int` — default 0; incremented by RoundResolver
- `bracket: LadderBracket` — `WINNERS`, `LOSERS`, `ELIMINATED`; driven by losses counter
- `displayOrder: Int` — assigned atomically at insert time; immutable
- `createdAt: Instant`

### `LadderBracket` enum
- `WINNERS` — 0 losses
- `LOSERS` — 1 loss
- `ELIMINATED` — 2 losses

### `LadderParticipant`
- `id: UUID`
- `ladderId: UUID`
- `userId: UUID`
- `createdAt: Instant`
- **Semantics:** rows exist only after Start and represent the frozen voter set; one row per eligible voter

### `LadderVote`
- `id: UUID`
- `ladderId: UUID`
- `roundNumber: Int`
- `userId: UUID`
- `votedForActivityId: UUID`
- `createdAt: Instant`

## Parameter Objects

### Queries
- `CreateLadderParam(creatorId: UUID, title: String)` — creates ladder with generated UUID
- `GetLadderByIdParam(id: UUID)`
- `GetLadderListParam(limit: Int? = null, offset: Int? = null)` — defaults to MAX_LIMIT (100)
- `GetActivitiesParam(ladderId: UUID)`
- `GetParticipantsParam(ladderId: UUID)`
- `GetVotesForRoundParam(ladderId: UUID, roundNumber: Int)`
- `CountVotesForRoundParam(ladderId: UUID, roundNumber: Int)`

### Mutations
- `AddActivityParam(ladderId: UUID, name: String, imageUrl: String, distanceMinutes: Int, costPerPerson: BigDecimal)`
- `RemoveActivityParam(activityId: UUID)` — caller checks ladder state validity; client just deletes
- `UpdateLadderStateParam(ladderId: UUID, status: LadderStatus, currentRoundNumber: Int? = null, currentMatchActivityAId: UUID? = null, currentMatchActivityBId: UUID? = null, isFinalRound: Boolean = false, isGrandFinalReset: Boolean = false)`
- `SetLadderWinnerParam(ladderId: UUID, winnerActivityId: UUID)`
- `UpdateActivityLossAndBracketParam(activityId: UUID, newLosses: Int, newBracket: LadderBracket)`
- `BulkInsertParticipantsParam(ladderId: UUID, userIds: Set<UUID>)`
- `CastVoteParam(ladderId: UUID, roundNumber: Int, userId: UUID, votedForActivityId: UUID)`
- `RestartLadderParam(ladderId: UUID)`

## Database
- Database: `camper-db` (port 5433, database `camper_db`)
- Tables: `activity_ladders`, `ladder_activities`, `ladder_participants`, `ladder_votes`
- Key constraints: unique `(ladder_id, user_id)` on participants; unique `(ladder_id, round_number, user_id)` on votes

## Architecture

### Facade Pattern
- `JdbiActivityLadderClient` delegates to individual operation classes in `internal/operations/`
- All operations take their corresponding param type and return `Result<T, AppError>`

### Validation Classes
- 1:1 with operations in `internal/validations/`
- Each operation instantiates its validator and checks result before executing

### Row Adapters
- `LadderRowAdapter` — ResultSet → Ladder
- `LadderActivityRowAdapter` — ResultSet → LadderActivity
- `LadderParticipantRowAdapter` — ResultSet → LadderParticipant
- `LadderVoteRowAdapter` — ResultSet → LadderVote

### Concurrency Primitive: `withLadderLocked`
- Implemented as a scoped client wrapper that holds an exclusive transaction lock on the ladder row
- All operations passed to the block execute on the same JDBC Handle (transaction), ensuring atomicity
- Used by `StartLadderAction` and round resolution logic (via `RoundResolver`) to prevent concurrent state mutations
- Model: `HandleScopedActivityLadderClient` — a `ActivityLadderClient` implementation that delegates all operations to the same JDBC Handle, so the FOR UPDATE lock is held across the entire block

### Factory
- `createActivityLadderClient(): ActivityLadderClient` — reads `DB_URL`, `DB_USER`, `DB_PASSWORD` from env vars / system properties; creates JDBI instance

## Error Handling
- Returns `Result<T, AppError>` — never throws for expected failures
- `NotFoundError` — ladder, activity, or participant not found
- `ConflictError` — duplicate vote cast for same user/round; duplicate participant row (should not occur with idempotent ON CONFLICT)

## Testing

### Integration Tests
- Testcontainers PostgreSQL + migrations via `ActivityLadderTestDb`
- 43 tests covering CRUD operations, concurrency (two-thread race with CyclicBarrier), ordering, and vote ledger immutability
- Seeding uses `CAST(:id AS uuid)` pattern for nullable UUID columns
- Truncate order: `ladder_votes, ladder_participants, ladder_activities, activity_ladders, users` (child-first)
- Concurrency test: `CyclicBarrier(2)` + `CopyOnWriteArrayList` for two-thread race on getLadderList ordering with distinct timestamps

### Fake Implementation
- `FakeActivityLadderClient` (in testFixtures) — in-memory storage via `ConcurrentHashMap<UUID, Ladder>`
- References actual validation classes from `internal/validations/`
- `withLadderLocked` uses per-ladder `synchronized` block to simulate exclusive lock
- `reset()` and `seed()` helpers for test setup
- All 16 operations implemented with proper error handling (NotFound, Conflict)

### Test DB Helper
- `ActivityLadderTestDb` (in testFixtures) — thin wrapper around `MigrationRunner` from `databases/camper-db`
