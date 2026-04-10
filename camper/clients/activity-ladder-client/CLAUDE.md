# activity-ladder-client

JDBI data access client for the activity ladder feature — double-elimination voting tournaments.

## Package
`com.acme.clients.activityladderclient`

## Public API (`ActivityLadderClient` interface)
- `createLadder` — Create a new ladder in DRAFT status
- `getLadderById` — Fetch a ladder by ID
- `getLadderList` — List all ladders, newest first, with limit/offset pagination
- `addActivity` — Add an activity to a ladder; assigns display_order = MAX+1 atomically
- `removeActivity` — Delete an activity from a ladder
- `getActivities` — List all activities for a ladder, ordered by display_order
- `updateLadderState` — Update status, round, match, and flag fields on a ladder
- `setLadderWinner` — Set the winner and mark COMPLETED in a single statement
- `restartLadder` — Transactional restart: delete votes + participants, reset activities and ladder state
- `updateActivityLossAndBracket` — Increment losses and set bracket on a single activity
- `bulkInsertParticipants` — Idempotent bulk insert of participants at Start time (ON CONFLICT DO NOTHING)
- `getParticipants` — List participants for a ladder
- `castVote` — Cast a single vote (caller must hold FOR UPDATE lock on ladder row)
- `getVotesForRound` — Fetch all votes for a round
- `countVotesForRound` — Count votes for a round (cheap check before tallying)
- `withLadderLocked` — Run a block inside a transaction that holds FOR UPDATE on the ladder row

## Model
- `Ladder` — mirrors `activity_ladders` table (UUID PK, status enum, nullable match/winner UUID refs, round number, boolean flags)
- `LadderStatus` — enum: `DRAFT`, `ACTIVE`, `COMPLETED`
- `LadderActivity` — mirrors `ladder_activities` table (BigDecimal cost, Int losses/distanceMinutes/displayOrder, bracket enum)
- `LadderBracket` — enum: `WINNERS`, `LOSERS`, `ELIMINATED`
- `LadderParticipant` — mirrors `ladder_participants` table (ladderId + userId, frozen at Start time)
- `LadderVote` — mirrors `ladder_votes` table (ladderId + roundNumber + userId + votedForActivityId, immutable)

## Database
- Database: `camper-db` (port 5433, database `camper_db`)
- Tables: `activity_ladders`, `ladder_activities`, `ladder_participants`, `ladder_votes`
- Key constraints: unique `(ladder_id, user_id)` on participants; unique `(ladder_id, round_number, user_id)` on votes

## Architecture
- **Facade pattern:** `JdbiActivityLadderClient` delegates to individual operation classes
- **Validation classes:** 1:1 with operations in `internal/validations/`
- **Parameter objects:** All methods take dedicated data class params
- **Row adapters:** `*RowAdapter` objects in `internal/adapters/` map ResultSet to model types
- **Factory:** `createActivityLadderClient()` creates the client (reads DB config from env vars)

## Error Handling
- Returns `Result<T, AppError>` — never throws for expected failures
- `NotFoundError` for missing entities
- `ConflictError` for duplicate constraint violations (e.g., duplicate vote)

## Testing
- Integration tests: Testcontainers PostgreSQL + migrations via `ActivityLadderTestDb`
- `FakeActivityLadderClient` (testFixtures) for consumer testing — references actual validators
- `ActivityLadderTestDb` (testFixtures) wraps `MigrationRunner` from database module
