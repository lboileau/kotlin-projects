# Activity Ladder — Implementation Plan

## 1. Feature Summary

A standalone feature in camper that lets any authenticated user create a double-elimination voting tournament ("ladder") to pick a group activity. The creator adds 2+ activities, shares a public URL, and when they click Start the server snapshots everyone currently subscribed to the ladder's STOMP topic and freezes that set as the eligible voter list. From there the server drives globally synchronized voting rounds (round does not advance until every frozen voter votes, even across disconnects), eliminating activities at 2 losses, running a Grand Final (with optional Reset) when both brackets collapse, and persisting a single winner. The feature is fully public within the camper user base, does not touch plans/trips, and is the first feature in the app to use live WebSocket presence tracking.

## 2. Entities

All four entities live in their own tables, all with UUID PKs. Nullability and FKs confirmed below.

### Ladder (table: `activity_ladders`)
| Field | Type | Null | Notes |
|---|---|---|---|
| `id` | UUID | no | PK, `gen_random_uuid()` default |
| `creatorId` | UUID | no | FK → `users.id` (ON DELETE CASCADE — matches test-fixture truncate pattern; see Decisions Log item 6) |
| `title` | String(200) | no | non-empty |
| `status` | enum(VARCHAR(16)) | no | `DRAFT` \| `ACTIVE` \| `COMPLETED`, default `DRAFT` |
| `currentRoundNumber` | Int | yes | null in DRAFT; set to 1 at Start |
| `currentMatchActivityAId` | UUID | yes | FK → `ladder_activities.id` (ON DELETE SET NULL) |
| `currentMatchActivityBId` | UUID | yes | FK → `ladder_activities.id` (ON DELETE SET NULL) |
| `isFinalRound` | Boolean | no | default `false` |
| `isGrandFinalReset` | Boolean | no | default `false` |
| `winnerActivityId` | UUID | yes | FK → `ladder_activities.id` (ON DELETE SET NULL); set only on COMPLETED |
| `createdAt` | Timestamp | no | `now()` |
| `updatedAt` | Timestamp | no | `now()` |

Rationale for `ON DELETE SET NULL` on the three FK columns that point at `ladder_activities`: the creator may restart or we may delete a stale activity in DRAFT and the `activity_ladders` row must survive. For `winnerActivityId`, on the (unlikely) event a winning activity row is somehow deleted, the ladder remains readable as "completed, winner lost" rather than failing.

### LadderActivity (table: `ladder_activities`)
| Field | Type | Null | Notes |
|---|---|---|---|
| `id` | UUID | no | PK |
| `ladderId` | UUID | no | FK → `activity_ladders.id` ON DELETE CASCADE |
| `name` | String(200) | no | non-empty |
| `imageUrl` | String(2000) | no | non-empty, must start with `http://` or `https://` |
| `distanceMinutes` | Int | no | `>= 0`, CHECK constraint |
| `costPerPerson` | Decimal(10,2) | no | `>= 0`, CHECK constraint |
| `losses` | Int | no | default 0; `>= 0 AND <= 2` CHECK constraint |
| `bracket` | enum(VARCHAR(16)) | no | `WINNERS` \| `LOSERS` \| `ELIMINATED`, default `WINNERS` |
| `displayOrder` | Int | no | creator-defined order; assigned as `MAX(displayOrder)+1` on insert |
| `createdAt` | Timestamp | no | |

No `updatedAt` — activities are only mutated by server-side round resolution (losses/bracket) and by creator add/remove in DRAFT. Per handoff, "restart" resets losses/bracket but keeps the activity rows intact.

### LadderParticipant (table: `ladder_participants`)
| Field | Type | Null | Notes |
|---|---|---|---|
| `id` | UUID | no | PK |
| `ladderId` | UUID | no | FK → `activity_ladders.id` ON DELETE CASCADE |
| `userId` | UUID | no | FK → `users.id` ON DELETE CASCADE |
| `createdAt` | Timestamp | no | |

Unique `(ladder_id, user_id)`. Rows exist **only** after Start for users who were in the in-memory presence set at that moment.

### LadderVote (table: `ladder_votes`)
| Field | Type | Null | Notes |
|---|---|---|---|
| `id` | UUID | no | PK |
| `ladderId` | UUID | no | FK → `activity_ladders.id` ON DELETE CASCADE |
| `roundNumber` | Int | no | |
| `userId` | UUID | no | FK → `users.id` ON DELETE CASCADE |
| `votedForActivityId` | UUID | no | FK → `ladder_activities.id` ON DELETE CASCADE |
| `createdAt` | Timestamp | no | |

Unique `(ladder_id, round_number, user_id)`. No `updatedAt` — votes are immutable once cast.

## 3. Database Changes

Verified: highest current migration is `V039__enable_fuzzystrmatch.sql`. Next number is **V040**. Four new migrations and four new schema files.

### Schema files (under `databases/camper-db/schema/tables/`)

- `010_activity_ladders.sql`
- `011_ladder_activities.sql`
- `012_ladder_participants.sql`
- `013_ladder_votes.sql`

(Note: the schema/tables directory does NOT strictly mirror migration numbers in this repo — it restarts its own counter and groups tables. I'm using 010–013 as the next available prefixes after `009_assignment_members.sql`. db-dev should confirm and adjust if another feature has already claimed these slots.)

### Migrations

#### `V040__create_activity_ladders.sql`
```sql
CREATE TABLE IF NOT EXISTS activity_ladders (
    id                            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    creator_id                    UUID         NOT NULL,
    title                         VARCHAR(200) NOT NULL,
    status                        VARCHAR(16)  NOT NULL DEFAULT 'DRAFT',
    current_round_number          INT,
    current_match_activity_a_id   UUID,
    current_match_activity_b_id   UUID,
    is_final_round                BOOLEAN      NOT NULL DEFAULT false,
    is_grand_final_reset          BOOLEAN      NOT NULL DEFAULT false,
    winner_activity_id            UUID,
    created_at                    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at                    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT ck_activity_ladders_status CHECK (status IN ('DRAFT', 'ACTIVE', 'COMPLETED')),
    CONSTRAINT fk_activity_ladders_creator FOREIGN KEY (creator_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_activity_ladders_creator_id ON activity_ladders (creator_id);
CREATE INDEX IF NOT EXISTS idx_activity_ladders_status ON activity_ladders (status);
```

#### `V041__create_ladder_activities.sql`
```sql
CREATE TABLE IF NOT EXISTS ladder_activities (
    id                UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    ladder_id         UUID           NOT NULL,
    name              VARCHAR(200)   NOT NULL,
    image_url         VARCHAR(2000)  NOT NULL,
    distance_minutes  INT            NOT NULL,
    cost_per_person   DECIMAL(10,2)  NOT NULL,
    losses            INT            NOT NULL DEFAULT 0,
    bracket           VARCHAR(16)    NOT NULL DEFAULT 'WINNERS',
    display_order     INT            NOT NULL,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT ck_ladder_activities_distance CHECK (distance_minutes >= 0),
    CONSTRAINT ck_ladder_activities_cost CHECK (cost_per_person >= 0),
    CONSTRAINT ck_ladder_activities_losses CHECK (losses >= 0 AND losses <= 2),
    CONSTRAINT ck_ladder_activities_bracket CHECK (bracket IN ('WINNERS', 'LOSERS', 'ELIMINATED')),
    CONSTRAINT fk_ladder_activities_ladder FOREIGN KEY (ladder_id) REFERENCES activity_ladders (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_ladder_activities_ladder_id ON ladder_activities (ladder_id);
CREATE INDEX IF NOT EXISTS idx_ladder_activities_ladder_bracket ON ladder_activities (ladder_id, bracket);
```

Then — back-fill the two self-referential FKs on `activity_ladders` that point at `ladder_activities`. They cannot be declared in V040 (table doesn't exist yet). Add them at the end of V041:

```sql
ALTER TABLE activity_ladders
    ADD CONSTRAINT fk_activity_ladders_match_a
        FOREIGN KEY (current_match_activity_a_id) REFERENCES ladder_activities (id) ON DELETE SET NULL;
ALTER TABLE activity_ladders
    ADD CONSTRAINT fk_activity_ladders_match_b
        FOREIGN KEY (current_match_activity_b_id) REFERENCES ladder_activities (id) ON DELETE SET NULL;
ALTER TABLE activity_ladders
    ADD CONSTRAINT fk_activity_ladders_winner
        FOREIGN KEY (winner_activity_id) REFERENCES ladder_activities (id) ON DELETE SET NULL;
```

Wrap each `ADD CONSTRAINT` in a `DO $$ BEGIN ... EXCEPTION WHEN duplicate_object THEN NULL; END $$;` block or use `IF NOT EXISTS` via a `pg_constraint` lookup for idempotency.

#### `V042__create_ladder_participants.sql`
```sql
CREATE TABLE IF NOT EXISTS ladder_participants (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    ladder_id  UUID        NOT NULL,
    user_id    UUID        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_ladder_participants_ladder_user UNIQUE (ladder_id, user_id),
    CONSTRAINT fk_ladder_participants_ladder FOREIGN KEY (ladder_id) REFERENCES activity_ladders (id) ON DELETE CASCADE,
    CONSTRAINT fk_ladder_participants_user   FOREIGN KEY (user_id)   REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_ladder_participants_ladder_id ON ladder_participants (ladder_id);
CREATE INDEX IF NOT EXISTS idx_ladder_participants_user_id ON ladder_participants (user_id);
```

#### `V043__create_ladder_votes.sql`
```sql
CREATE TABLE IF NOT EXISTS ladder_votes (
    id                     UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    ladder_id              UUID        NOT NULL,
    round_number           INT         NOT NULL,
    user_id                UUID        NOT NULL,
    voted_for_activity_id  UUID        NOT NULL,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_ladder_votes_round_user UNIQUE (ladder_id, round_number, user_id),
    CONSTRAINT fk_ladder_votes_ladder   FOREIGN KEY (ladder_id) REFERENCES activity_ladders (id) ON DELETE CASCADE,
    CONSTRAINT fk_ladder_votes_user     FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_ladder_votes_activity FOREIGN KEY (voted_for_activity_id) REFERENCES ladder_activities (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_ladder_votes_ladder_round ON ladder_votes (ladder_id, round_number);
```

The `idx_ladder_votes_ladder_round` index supports the "count votes for current round" query that fires on every vote cast — the hot path.

### Rollbacks

- `R040__drop_activity_ladders.sql` — `DROP TABLE IF EXISTS activity_ladders CASCADE;`
- `R041__drop_ladder_activities.sql` — `DROP TABLE IF EXISTS ladder_activities CASCADE;`
- `R042__drop_ladder_participants.sql`
- `R043__drop_ladder_votes.sql`

### Seed data

Optional per handoff. Skip for initial PR. If desired later, a 3-activity demo ladder in DRAFT status against the existing seeded `demo@example.com` user.

### CLAUDE.md updates

`databases/camper-db/CLAUDE.md` gets four new schema blocks, four new entries in Relationships, and new invariants for each new constraint.

## 4. Client Module (`clients/activity-ladder-client`)

### Directory layout

```
clients/activity-ladder-client/
├── CLAUDE.md
├── build.gradle.kts
└── src/
    ├── main/kotlin/com/acme/clients/activityladderclient/
    │   ├── ActivityLadderClientFactory.kt
    │   ├── model/
    │   │   ├── Ladder.kt
    │   │   ├── LadderStatus.kt
    │   │   ├── LadderActivity.kt
    │   │   ├── LadderBracket.kt
    │   │   ├── LadderParticipant.kt
    │   │   └── LadderVote.kt
    │   ├── api/
    │   │   ├── ActivityLadderClient.kt
    │   │   └── ActivityLadderClientParams.kt
    │   └── internal/
    │       ├── JdbiActivityLadderClient.kt
    │       ├── adapters/
    │       │   ├── LadderRowAdapter.kt
    │       │   ├── LadderActivityRowAdapter.kt
    │       │   ├── LadderParticipantRowAdapter.kt
    │       │   └── LadderVoteRowAdapter.kt
    │       ├── operations/
    │       │   ├── CreateLadder.kt
    │       │   ├── GetLadderById.kt
    │       │   ├── GetLadderList.kt
    │       │   ├── AddLadderActivity.kt
    │       │   ├── RemoveLadderActivity.kt
    │       │   ├── GetLadderActivities.kt
    │       │   ├── UpdateLadderStatus.kt
    │       │   ├── UpdateLadderCurrentMatch.kt
    │       │   ├── BulkInsertParticipants.kt
    │       │   ├── GetParticipants.kt
    │       │   ├── CastVote.kt
    │       │   ├── GetVotesForRound.kt
    │       │   ├── CountVotesForRound.kt
    │       │   ├── UpdateActivityLossAndBracket.kt
    │       │   ├── SetLadderWinner.kt
    │       │   └── RestartLadder.kt
    │       └── validations/
    │           └── (1:1 with operations — see below)
    ├── testFixtures/kotlin/com/acme/clients/activityladderclient/
    │   ├── fake/
    │   │   └── FakeActivityLadderClient.kt
    │   └── test/
    │       └── ActivityLadderTestDb.kt
    └── test/kotlin/com/acme/clients/activityladderclient/
        └── JdbiActivityLadderClientTest.kt
```

### Model types

```kotlin
// model/LadderStatus.kt
enum class LadderStatus { DRAFT, ACTIVE, COMPLETED }

// model/LadderBracket.kt
enum class LadderBracket { WINNERS, LOSERS, ELIMINATED }

// model/Ladder.kt
data class Ladder(
    val id: UUID,
    val creatorId: UUID,
    val title: String,
    val status: LadderStatus,
    val currentRoundNumber: Int?,
    val currentMatchActivityAId: UUID?,
    val currentMatchActivityBId: UUID?,
    val isFinalRound: Boolean,
    val isGrandFinalReset: Boolean,
    val winnerActivityId: UUID?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

// model/LadderActivity.kt
data class LadderActivity(
    val id: UUID,
    val ladderId: UUID,
    val name: String,
    val imageUrl: String,
    val distanceMinutes: Int,
    val costPerPerson: BigDecimal,
    val losses: Int,
    val bracket: LadderBracket,
    val displayOrder: Int,
    val createdAt: Instant,
)

// model/LadderParticipant.kt
data class LadderParticipant(
    val id: UUID,
    val ladderId: UUID,
    val userId: UUID,
    val createdAt: Instant,
)

// model/LadderVote.kt
data class LadderVote(
    val id: UUID,
    val ladderId: UUID,
    val roundNumber: Int,
    val userId: UUID,
    val votedForActivityId: UUID,
    val createdAt: Instant,
)
```

### Interface (`api/ActivityLadderClient.kt`)

```kotlin
interface ActivityLadderClient {
    // Ladder CRUD
    /** Create a new ladder in DRAFT status. */
    fun createLadder(param: CreateLadderParam): Result<Ladder, AppError>

    /** Fetch a ladder by id. */
    fun getLadderById(param: GetLadderByIdParam): Result<Ladder, AppError>

    /** List all ladders, newest first. Caller-side pagination via limit/offset. */
    fun getLadderList(param: GetLadderListParam): Result<List<Ladder>, AppError>

    // Activity CRUD (within a ladder)
    /** Add an activity to a ladder; assigns display_order = MAX+1 atomically. */
    fun addActivity(param: AddLadderActivityParam): Result<LadderActivity, AppError>

    /** Delete an activity from a ladder. */
    fun removeActivity(param: RemoveLadderActivityParam): Result<Unit, AppError>

    /** List all activities for a ladder, ordered by display_order. */
    fun getActivities(param: GetLadderActivitiesParam): Result<List<LadderActivity>, AppError>

    // Ladder state mutation (used by round-resolution logic, all FOR-UPDATE-aware internally)
    /** Update status, currentRoundNumber, match, isFinalRound, isGrandFinalReset fields. */
    fun updateLadderState(param: UpdateLadderStateParam): Result<Ladder, AppError>

    /** Set the winner and mark COMPLETED in a single statement. */
    fun setLadderWinner(param: SetLadderWinnerParam): Result<Ladder, AppError>

    /**
     * Runs the entire restart operation inside a single transaction:
     * delete votes + participants, reset all activities to 0 losses / WINNERS bracket,
     * reset ladder state to DRAFT. Returns the updated ladder.
     */
    fun restartLadder(param: RestartLadderParam): Result<Ladder, AppError>

    // Activity state (driven by round resolution)
    /** Increment losses and set bracket on a single activity. */
    fun updateActivityLossAndBracket(param: UpdateActivityLossAndBracketParam): Result<LadderActivity, AppError>

    // Participants
    /**
     * Idempotent bulk insert of participants at Start time.
     * Uses INSERT ... ON CONFLICT (ladder_id, user_id) DO NOTHING so accidental re-runs
     * never corrupt an existing frozen voter set.
     */
    fun bulkInsertParticipants(param: BulkInsertParticipantsParam): Result<List<LadderParticipant>, AppError>

    /** List participants for a ladder. */
    fun getParticipants(param: GetParticipantsParam): Result<List<LadderParticipant>, AppError>

    // Votes
    /**
     * Cast a single vote. This method MUST be invoked by the caller inside a
     * transaction that has previously issued SELECT ... FOR UPDATE on the
     * activity_ladders row for this ladder. The implementation therefore performs
     * no locking of its own — it relies on the caller's transaction.
     *
     * Returns ConflictError on duplicate (ladder_id, round_number, user_id) violation.
     */
    fun castVote(param: CastVoteParam): Result<LadderVote, AppError>

    /** Fetch votes for a round (caller inside txn; used post-vote to tally). */
    fun getVotesForRound(param: GetVotesForRoundParam): Result<List<LadderVote>, AppError>

    /** Count votes for a round. Cheap check used before tallying. */
    fun countVotesForRound(param: CountVotesForRoundParam): Result<Int, AppError>

    /**
     * Runs `block` inside a single JDBI transaction that has acquired
     * `SELECT id FROM activity_ladders WHERE id = :id FOR UPDATE` on the target row.
     * The service's vote-resolution action is the sole caller.
     */
    fun <T> withLadderLocked(ladderId: UUID, block: (ActivityLadderClient) -> Result<T, AppError>): Result<T, AppError>
}
```

The `withLadderLocked` method is the pivotal concurrency primitive. It exists **on the client interface** (not as a service-level helper) because JDBI handles/transactions are client-scoped — the service has no JDBI handle to work with. The passed `block` receives a scoped `ActivityLadderClient` instance whose operations all share the transactional handle; any operation called on this scoped client is part of the same transaction that holds the lock on the ladder row. This mirrors how other features would use Jdbi's `inTransaction { handle -> ... }` but wrapped so the service never touches JDBI types.

### Parameter objects (`api/ActivityLadderClientParams.kt`)

```kotlin
data class CreateLadderParam(val creatorId: UUID, val title: String)
data class GetLadderByIdParam(val id: UUID)
data class GetLadderListParam(val limit: Int? = null, val offset: Int? = null)

data class AddLadderActivityParam(
    val ladderId: UUID,
    val name: String,
    val imageUrl: String,
    val distanceMinutes: Int,
    val costPerPerson: BigDecimal,
)
data class RemoveLadderActivityParam(val ladderId: UUID, val activityId: UUID)
data class GetLadderActivitiesParam(val ladderId: UUID)

data class UpdateLadderStateParam(
    val ladderId: UUID,
    val status: LadderStatus,
    val currentRoundNumber: Int?,
    val currentMatchActivityAId: UUID?,
    val currentMatchActivityBId: UUID?,
    val isFinalRound: Boolean,
    val isGrandFinalReset: Boolean,
)
data class SetLadderWinnerParam(val ladderId: UUID, val winnerActivityId: UUID)
data class RestartLadderParam(val ladderId: UUID)

data class UpdateActivityLossAndBracketParam(
    val activityId: UUID,
    val losses: Int,
    val bracket: LadderBracket,
)

data class BulkInsertParticipantsParam(val ladderId: UUID, val userIds: Set<UUID>)
data class GetParticipantsParam(val ladderId: UUID)

data class CastVoteParam(
    val ladderId: UUID,
    val roundNumber: Int,
    val userId: UUID,
    val votedForActivityId: UUID,
)
data class GetVotesForRoundParam(val ladderId: UUID, val roundNumber: Int)
data class CountVotesForRoundParam(val ladderId: UUID, val roundNumber: Int)
```

### Validation strategy

Validators are 1:1 with operations. All validators default to `success(Unit)` at the client layer — domain checks (title length, imageUrl format, activity count ≥ 2, etc.) happen at the service layer. The only client-level validation worth enforcing: `ValidateAddLadderActivity` checks `distanceMinutes >= 0` and `costPerPerson >= 0` as a last-line defense in case the service is bypassed.

### Factory

```kotlin
fun createActivityLadderClient(): ActivityLadderClient {
    val url = System.getProperty("DB_URL") ?: System.getenv("DB_URL")
        ?: "jdbc:postgresql://localhost:5433/camper_db"
    val user = System.getProperty("DB_USER") ?: System.getenv("DB_USER") ?: "postgres"
    val password = System.getProperty("DB_PASSWORD") ?: System.getenv("DB_PASSWORD") ?: "postgres"
    val jdbi = Jdbi.create(url, user, password)
    return JdbiActivityLadderClient(jdbi)
}
```

### FakeActivityLadderClient

- In-memory stores: `ConcurrentHashMap<UUID, Ladder>` + `ConcurrentHashMap<UUID, LadderActivity>` + `ConcurrentHashMap<Pair<UUID,UUID>, LadderParticipant>` + `ConcurrentHashMap<Triple<UUID,Int,UUID>, LadderVote>`.
- `withLadderLocked` is implemented as a `synchronized(ladderLock(ladderId))` block where `ladderLock` returns a per-ladder `Object` from a `ConcurrentHashMap<UUID, Object>`. This simulates the DB lock and lets tests exercise concurrent-vote scenarios deterministically.
- `reset()` clears all stores.
- `seed(ladder, activities, participants, votes)` helpers for service-layer tests.

### Row adapters

Straightforward `ResultSet` → model mappers for all four entity types; `LadderRowAdapter` handles the nullable columns (`currentRoundNumber`, `currentMatchActivity[AB]Id`, `winnerActivityId`). Enum columns are mapped via `LadderStatus.valueOf(rs.getString(...))`.

### Gradle `build.gradle.kts`

Mirrors `clients/gear-pack-client/build.gradle.kts`: `kotlin("jvm")` + `java-test-fixtures`, depends on `:clients:client-common`, JDBI, PostgreSQL JDBC, testFixtures depend on `:databases:camper-db`, Testcontainers.

### Settings wiring

Add to `camper/settings.gradle.kts`:
```kotlin
include(":clients:activity-ladder-client")
```

## 5. Service Feature (`services/camper-service/.../features/activityladder`)

### Directory layout

```
features/activityladder/
├── model/
│   ├── Ladder.kt                    # service-layer model, separate from client model
│   ├── LadderActivity.kt
│   ├── LadderParticipant.kt
│   ├── LadderVote.kt
│   ├── LadderDetail.kt              # aggregate: ladder + activities + participants + current-round vote count
│   ├── LadderSummary.kt             # for list endpoint
│   ├── LadderStatus.kt              # service enum (mirrors client enum)
│   └── LadderBracket.kt
├── error/
│   └── LadderError.kt
├── params/
│   └── ActivityLadderServiceParams.kt
├── validations/
│   ├── ValidateCreateLadder.kt
│   ├── ValidateListLadders.kt
│   ├── ValidateGetLadderDetail.kt
│   ├── ValidateAddActivity.kt
│   ├── ValidateRemoveActivity.kt
│   ├── ValidateStartLadder.kt
│   ├── ValidateCastVote.kt
│   └── ValidateRestartLadder.kt
├── actions/
│   ├── CreateLadderAction.kt
│   ├── ListLaddersAction.kt
│   ├── GetLadderDetailAction.kt
│   ├── AddActivityAction.kt
│   ├── RemoveActivityAction.kt
│   ├── StartLadderAction.kt
│   ├── CastVoteAction.kt
│   ├── RestartLadderAction.kt
│   └── roundresolution/
│       └── RoundResolver.kt        # pure class invoked by CastVoteAction inside the locked txn
├── service/
│   └── ActivityLadderService.kt
├── dto/
│   ├── CreateLadderRequest.kt
│   ├── AddActivityRequest.kt
│   ├── CastVoteRequest.kt
│   ├── LadderSummaryResponse.kt
│   ├── LadderDetailResponse.kt
│   ├── LadderActivityResponse.kt
│   ├── LadderParticipantResponse.kt
│   ├── CurrentRoundView.kt          # nested within LadderDetailResponse
│   └── CastVoteResponse.kt
├── mapper/
│   └── ActivityLadderMapper.kt
└── controller/
    └── ActivityLadderController.kt
```

### Service param objects

```kotlin
data class CreateLadderParam(val requestingUserId: UUID, val title: String, val activities: List<NewActivityInput>)
data class NewActivityInput(val name: String, val imageUrl: String, val distanceMinutes: Int, val costPerPerson: BigDecimal)

data class ListLaddersParam(val requestingUserId: UUID)
data class GetLadderDetailParam(val ladderId: UUID, val requestingUserId: UUID)

data class AddActivityParam(val ladderId: UUID, val requestingUserId: UUID, val name: String, val imageUrl: String, val distanceMinutes: Int, val costPerPerson: BigDecimal)
data class RemoveActivityParam(val ladderId: UUID, val activityId: UUID, val requestingUserId: UUID)

data class StartLadderParam(val ladderId: UUID, val requestingUserId: UUID)
data class CastVoteParam(val ladderId: UUID, val requestingUserId: UUID, val votedForActivityId: UUID)
data class RestartLadderParam(val ladderId: UUID, val requestingUserId: UUID)
```

### LadderError

```kotlin
sealed class LadderError(override val message: String) : AppError {
    data class NotFound(val ladderId: String) : LadderError("Ladder not found: $ladderId")
    data class ActivityNotFound(val activityId: String) : LadderError("Activity not found: $activityId")
    data class NotCreator(val ladderId: UUID, val userId: UUID) : LadderError("User $userId is not creator of ladder $ladderId")
    data class Invalid(val field: String, val reason: String) : LadderError("Invalid $field: $reason")
    data class IllegalState(val expected: String, val actual: String) : LadderError("Illegal state: expected $expected, was $actual")
    data class NotEligibleVoter(val userId: UUID) : LadderError("User $userId is not an eligible voter")
    data class AlreadyVoted(val userId: UUID, val roundNumber: Int) : LadderError("User $userId already voted in round $roundNumber")
    data class InvalidVoteTarget(val activityId: UUID) : LadderError("Activity $activityId is not in the current match")
    data class NotEnoughActivities(val count: Int) : LadderError("Cannot start ladder: need 2+ activities, have $count")
    data class NoPresentUsers(val ladderId: UUID) : LadderError("Cannot start ladder: no users currently present")

    companion object {
        fun fromClientError(error: AppError): LadderError = when (error) {
            is NotFoundError -> NotFound(error.id)
            is ConflictError -> Invalid(error.entity, error.detail)
            is ValidationError -> Invalid(error.field, error.reason)
            else -> Invalid("unknown", error.message)
        }
    }
}
```

### DTOs

```kotlin
data class CreateLadderRequest(val title: String, val activities: List<NewActivityPayload>)
data class NewActivityPayload(val name: String, val imageUrl: String, val distanceMinutes: Int, val costPerPerson: BigDecimal)

data class AddActivityRequest(val name: String, val imageUrl: String, val distanceMinutes: Int, val costPerPerson: BigDecimal)
data class CastVoteRequest(val votedForActivityId: UUID)

data class LadderSummaryResponse(
    val id: UUID,
    val title: String,
    val status: String,
    val creatorId: UUID,
    val activityCount: Int,
    val participantCount: Int,
    val winnerActivityId: UUID?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class LadderDetailResponse(
    val id: UUID,
    val title: String,
    val status: String,
    val creatorId: UUID,
    val activities: List<LadderActivityResponse>,
    val participants: List<LadderParticipantResponse>,
    val currentRound: CurrentRoundView?,
    val winnerActivityId: UUID?,
    val isFinalRound: Boolean,
    val isGrandFinalReset: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class LadderActivityResponse(
    val id: UUID,
    val name: String,
    val imageUrl: String,
    val distanceMinutes: Int,
    val costPerPerson: BigDecimal,
    val losses: Int,
    val bracket: String,
    val displayOrder: Int,
)

data class LadderParticipantResponse(
    val userId: UUID,
    val username: String?,
    val avatarSeed: String?,
)

data class CurrentRoundView(
    val roundNumber: Int,
    val activityAId: UUID,
    val activityBId: UUID,
    val votesCast: Int,
    val totalVoters: Int,
    val votedUserIds: List<UUID>,  // for "who has voted" indicator — anonymous re: which activity
)

data class CastVoteResponse(
    val voteCount: Int,
    val votersRemaining: Int,
)
```

### Cascade-impact: participant enrichment

`LadderParticipantResponse` requires `username` and `avatarSeed` from the `users` table. The service enriches participants at read time by calling `userClient.getById(userId)` for each participant — same pattern as `PlanMemberResponse` enrichment in `GetPlanMembersAction`. This means `ActivityLadderService` takes **both** `ActivityLadderClient` and `UserClient` in its constructor.

### Controller

```kotlin
@RestController
@RequestMapping("/api/ladders")
class ActivityLadderController(
    private val ladderService: ActivityLadderService,
    private val eventPublisher: LadderEventPublisher,
    private val presenceTracker: LadderPresenceTracker,
) {
    @PostMapping
    fun create(@RequestHeader("X-User-Id") userId: UUID, @RequestBody req: CreateLadderRequest): ResponseEntity<Any>

    @GetMapping
    fun list(@RequestHeader("X-User-Id") userId: UUID): ResponseEntity<Any>

    @GetMapping("/{id}")
    fun getDetail(@PathVariable id: UUID, @RequestHeader("X-User-Id") userId: UUID): ResponseEntity<Any>

    @PostMapping("/{id}/activities")
    fun addActivity(@PathVariable id: UUID, @RequestHeader("X-User-Id") userId: UUID, @RequestBody req: AddActivityRequest): ResponseEntity<Any>
    // → broadcasts { resource: "ladder", action: "activity-added" }

    @DeleteMapping("/{id}/activities/{activityId}")
    fun removeActivity(@PathVariable id: UUID, @PathVariable activityId: UUID, @RequestHeader("X-User-Id") userId: UUID): ResponseEntity<Any>
    // → broadcasts { resource: "ladder", action: "activity-removed" }

    @PostMapping("/{id}/start")
    fun start(@PathVariable id: UUID, @RequestHeader("X-User-Id") userId: UUID): ResponseEntity<Any>
    // Reads presenceTracker.snapshot(id) and passes to service. After success, broadcasts { action: "started" } then { action: "round-started" }.

    @PostMapping("/{id}/vote")
    fun vote(@PathVariable id: UUID, @RequestHeader("X-User-Id") userId: UUID, @RequestBody req: CastVoteRequest): ResponseEntity<Any>
    // Broadcasts depend on service outcome — see Round Resolution section.

    @PostMapping("/{id}/restart")
    fun restart(@PathVariable id: UUID, @RequestHeader("X-User-Id") userId: UUID): ResponseEntity<Any>
    // → broadcasts { action: "restarted" }
}
```

**Controller-vs-action WebSocket split:** The controller publishes simple events for simple endpoints (add/remove activity, restart). The vote endpoint is different — round resolution emits multiple events (`vote-cast`, then potentially `round-resolved` + `round-started` or `completed`). To keep the controller thin, `CastVoteAction` returns a sealed `VoteOutcome` result type that the controller maps to the correct sequence of `eventPublisher.publish*(...)` calls. The action itself never touches the publisher — same separation as in existing features where controllers publish events post-success.

```kotlin
sealed class VoteOutcome {
    data class VoteRecorded(val voteCount: Int, val votersRemaining: Int, val voterId: UUID) : VoteOutcome()
    data class RoundTied(val voteCount: Int, val newRoundNumber: Int, val nextMatchAId: UUID, val nextMatchBId: UUID) : VoteOutcome()
    data class RoundDecided(val winnerActivityId: UUID, val voteTotals: Map<UUID, Int>, val newRoundNumber: Int, val nextMatchAId: UUID, val nextMatchBId: UUID, val isFinalRound: Boolean, val isGrandFinalReset: Boolean) : VoteOutcome()
    data class LadderCompleted(val winnerActivityId: UUID, val voteTotals: Map<UUID, Int>) : VoteOutcome()
}
```

### Actions

| Action | Validates | Work |
|---|---|---|
| `CreateLadderAction` | title non-empty ≤200, activities.size ≥ 2, each activity name non-empty ≤200, imageUrl starts with http(s) ≤2000, distanceMinutes ≥ 0, costPerPerson ≥ 0 | creates ladder row → bulk adds activities via repeated `addActivity` calls → returns detail |
| `ListLaddersAction` | standard | `ladderClient.getLadderList` → for each summary fetch activities+participants (simple N+1; list is small) OR use dedicated client list-with-counts method — **see Decision log** |
| `GetLadderDetailAction` | standard | fetches ladder + activities + participants + (if ACTIVE) current round votes; enriches participants with user info via `userClient.getById` |
| `AddActivityAction` | creator check, status == DRAFT, field validations | `ladderClient.addActivity` |
| `RemoveActivityAction` | creator check, status == DRAFT | `ladderClient.removeActivity` |
| `StartLadderAction` | creator check, status == DRAFT, activities.size ≥ 2; takes a `presentUserIds: Set<UUID>` argument from the controller (obtained via `presenceTracker.snapshot(ladderId)`); `presentUserIds` must be non-empty (creator is always in it because the creator's own page opens a WS connection) | `bulkInsertParticipants`, `updateLadderState(status=ACTIVE, roundNumber=1, pick first match, isFinal=false, isReset=false)` — wraps in `withLadderLocked` for safety; broadcasts both `started` and `round-started` |
| `CastVoteAction` | participant check (is userId in `ladder_participants`?); status == ACTIVE; match activities are current; not already voted | `withLadderLocked { scopedClient -> ...` — inside the lock: `countVotesForRound`, verify user not in existing votes, `castVote`, re-count, if `count < totalVoters` return `VoteRecorded`, else tally → call `RoundResolver` → return the resolver's `VoteOutcome` |
| `RestartLadderAction` | creator check, status in (ACTIVE, COMPLETED) — if DRAFT return `IllegalState` | `ladderClient.restartLadder(ladderId)` — client does all the sub-operations in one txn |

### RoundResolver (pure class, unit-testable)

```kotlin
class RoundResolver(private val client: ActivityLadderClient) {

    /**
     * Precondition: caller has established a FOR UPDATE lock on the ladder row
     * (via `client.withLadderLocked`). All operations on `client` within this method
     * execute inside that transaction.
     */
    fun resolve(ladder: Ladder, roundNumber: Int): VoteOutcome {
        val votes = client.getVotesForRound(GetVotesForRoundParam(ladder.id, roundNumber)).getOrThrow()
        val tally = votes.groupingBy { it.votedForActivityId }.eachCount()
        val aCount = tally[ladder.currentMatchActivityAId!!] ?: 0
        val bCount = tally[ladder.currentMatchActivityBId!!] ?: 0

        return when {
            aCount == bCount -> handleTie(ladder, roundNumber)
            else -> {
                val (winnerId, loserId) = if (aCount > bCount) ladder.currentMatchActivityAId!! to ladder.currentMatchActivityBId!!
                                          else ladder.currentMatchActivityBId!! to ladder.currentMatchActivityAId!!
                handleWinDecision(ladder, roundNumber, winnerId, loserId, tally)
            }
        }
    }

    private fun handleTie(ladder: Ladder, roundNumber: Int): VoteOutcome { ... }
    private fun handleWinDecision(...): VoteOutcome { ... }
    private fun selectNextMatch(activities: List<LadderActivity>, ladder: Ladder): NextMatch { ... }
}
```

### Validations

All validators follow the existing pattern (execute → validate + log warn on failure). Domain-specific checks:
- `ValidateCreateLadder`: title non-empty, ≤200; activities.size ≥ 2; each activity's fields (see table above).
- `ValidateAddActivity`: all activity field checks.
- `ValidateStartLadder`: needs the activities count — architect decision: validator takes the precondition values (count) from the action after fetching, not from the param. Keep validator shape consistent with existing features (validator on param only) and hoist the "2+ activities" check into the action body. Document this deviation: single exception where a precondition requires data not in the param object. The action performs it as an early `return failure(...)` before calling the client.

### Spring wiring (`config/ActivityLadderClientConfig.kt` + `ActivityLadderServiceConfig.kt`)

```kotlin
@Configuration
class ActivityLadderClientConfig {
    @Bean
    fun activityLadderClient(): ActivityLadderClient = createActivityLadderClient()
}

@Configuration
class ActivityLadderServiceConfig {
    @Bean
    fun activityLadderService(
        ladderClient: ActivityLadderClient,
        userClient: UserClient,
    ): ActivityLadderService = ActivityLadderService(ladderClient, userClient)
}
```

`LadderEventPublisher` and `LadderPresenceTracker` are `@Component` classes (no explicit wiring needed).

### `ResultExtensions.kt` additions

```kotlin
fun LadderError.toResponseEntity(): ResponseEntity<Any> = when (this) {
    is LadderError.NotFound -> ResponseEntity.status(404).body(ApiResponse.ErrorBody("NOT_FOUND", message))
    is LadderError.ActivityNotFound -> ResponseEntity.status(404).body(ApiResponse.ErrorBody("NOT_FOUND", message))
    is LadderError.NotCreator -> ResponseEntity.status(403).body(ApiResponse.ErrorBody("FORBIDDEN", message))
    is LadderError.NotEligibleVoter -> ResponseEntity.status(403).body(ApiResponse.ErrorBody("FORBIDDEN", message))
    is LadderError.IllegalState -> ResponseEntity.status(409).body(ApiResponse.ErrorBody("CONFLICT", message))
    is LadderError.AlreadyVoted -> ResponseEntity.status(409).body(ApiResponse.ErrorBody("CONFLICT", message))
    is LadderError.NotEnoughActivities -> ResponseEntity.status(400).body(ApiResponse.ErrorBody("BAD_REQUEST", message))
    is LadderError.NoPresentUsers -> ResponseEntity.status(409).body(ApiResponse.ErrorBody("CONFLICT", message))
    is LadderError.Invalid -> ResponseEntity.status(400).body(ApiResponse.ErrorBody("BAD_REQUEST", message))
    is LadderError.InvalidVoteTarget -> ResponseEntity.status(400).body(ApiResponse.ErrorBody("BAD_REQUEST", message))
}

@JvmName("activityLadderResultToResponseEntity")
fun <T> Result<T, LadderError>.toResponseEntity(...) = ...
```

## 6. WebSocket Layer

### `websocket/LadderUpdateMessage.kt`

```kotlin
data class LadderUpdateMessage(
    val resource: String = "ladder",
    val action: String,
    val payload: Map<String, Any?>? = null,
)
```

Separate from `PlanUpdateMessage` per handoff — plans and ladders stay independent.

### `websocket/LadderEventPublisher.kt` (mirrors PlanEventPublisher)

```kotlin
@Component
class LadderEventPublisher(private val messagingTemplate: SimpMessagingTemplate) {
    private val logger = LoggerFactory.getLogger(LadderEventPublisher::class.java)

    private fun publish(ladderId: UUID, action: String, payload: Map<String, Any?>? = null) {
        val destination = "/topic/ladders/$ladderId"
        val message = LadderUpdateMessage(action = action, payload = payload)
        logger.debug("Publishing to {}: {}", destination, message)
        messagingTemplate.convertAndSend(destination, message)
    }

    fun presenceChanged(ladderId: UUID, presentUserIds: Set<UUID>) =
        publish(ladderId, "presence-changed", mapOf("presentUserIds" to presentUserIds.map { it.toString() }))

    fun activityAdded(ladderId: UUID) = publish(ladderId, "activity-added")
    fun activityRemoved(ladderId: UUID) = publish(ladderId, "activity-removed")
    fun started(ladderId: UUID) = publish(ladderId, "started")
    fun roundStarted(ladderId: UUID, roundNumber: Int, activityAId: UUID, activityBId: UUID, isFinal: Boolean, isReset: Boolean) =
        publish(ladderId, "round-started", mapOf(
            "roundNumber" to roundNumber,
            "activityAId" to activityAId.toString(),
            "activityBId" to activityBId.toString(),
            "isFinalRound" to isFinal,
            "isGrandFinalReset" to isReset,
        ))
    fun voteCast(ladderId: UUID, voterId: UUID, voteCount: Int, votersRemaining: Int) =
        publish(ladderId, "vote-cast", mapOf(
            "voterId" to voterId.toString(),
            "voteCount" to voteCount,
            "votersRemaining" to votersRemaining,
        ))
    fun roundResolved(ladderId: UUID, outcome: String, winnerActivityId: UUID?, voteTotals: Map<UUID, Int>?) =
        publish(ladderId, "round-resolved", mapOf(
            "outcome" to outcome,  // "tie" | "decided"
            "winnerActivityId" to winnerActivityId?.toString(),
            "voteTotals" to voteTotals?.mapKeys { it.key.toString() },
        ))
    fun completed(ladderId: UUID, winnerActivityId: UUID) =
        publish(ladderId, "completed", mapOf("winnerActivityId" to winnerActivityId.toString()))
    fun restarted(ladderId: UUID) = publish(ladderId, "restarted")
}
```

### `websocket/LadderPresenceTracker.kt`

```kotlin
@Component
class LadderPresenceTracker(private val eventPublisher: LadderEventPublisher) {
    private val logger = LoggerFactory.getLogger(LadderPresenceTracker::class.java)

    // ladderId -> (userId -> sessionCount). We track a count per user rather than a set of session IDs
    // because a user may have multiple browser tabs open; they should remain "present" as long as any tab stays.
    private val presence = ConcurrentHashMap<UUID, ConcurrentHashMap<UUID, Int>>()

    // sessionId -> (ladderId, userId) — so that disconnect events, which only know the session, can find what to decrement.
    private val sessions = ConcurrentHashMap<String, Pair<UUID, UUID>>()

    fun snapshot(ladderId: UUID): Set<UUID> =
        presence[ladderId]?.keys?.toSet() ?: emptySet()

    fun getPresent(ladderId: UUID): Set<UUID> = snapshot(ladderId)

    fun subscribed(sessionId: String, ladderId: UUID, userId: UUID) {
        val ladderMap = presence.computeIfAbsent(ladderId) { ConcurrentHashMap() }
        ladderMap.merge(userId, 1) { old, _ -> old + 1 }
        sessions[sessionId] = ladderId to userId
        eventPublisher.presenceChanged(ladderId, ladderMap.keys.toSet())
    }

    fun unsubscribed(sessionId: String) {
        val (ladderId, userId) = sessions.remove(sessionId) ?: return
        val ladderMap = presence[ladderId] ?: return
        ladderMap.compute(userId) { _, count ->
            val next = (count ?: 1) - 1
            if (next <= 0) null else next
        }
        if (ladderMap.isEmpty()) presence.remove(ladderId)
        eventPublisher.presenceChanged(ladderId, ladderMap.keys.toSet())
    }
}
```

### `websocket/LadderStompSessionListener.kt`

Spring event listener that hooks into STOMP session events:

```kotlin
@Component
class LadderStompSessionListener(private val presenceTracker: LadderPresenceTracker) {
    private val logger = LoggerFactory.getLogger(LadderStompSessionListener::class.java)
    private val LADDER_TOPIC_REGEX = Regex("""^/topic/ladders/([0-9a-fA-F-]{36})$""")

    @EventListener
    fun onSubscribe(event: SessionSubscribeEvent) {
        val accessor = StompHeaderAccessor.wrap(event.message)
        val destination = accessor.destination ?: return
        val match = LADDER_TOPIC_REGEX.matchEntire(destination) ?: return
        val ladderId = UUID.fromString(match.groupValues[1])
        val sessionId = accessor.sessionId ?: return
        val userId = extractUserId(accessor) ?: run {
            logger.warn("Subscribe to {} with no X-User-Id; ignoring", destination)
            return
        }
        presenceTracker.subscribed(sessionId, ladderId, userId)
    }

    @EventListener
    fun onUnsubscribe(event: SessionUnsubscribeEvent) = handleLeave(event.message)

    @EventListener
    fun onDisconnect(event: SessionDisconnectEvent) = handleLeave(event.message)

    private fun handleLeave(message: Message<*>) {
        val sessionId = StompHeaderAccessor.wrap(message).sessionId ?: return
        presenceTracker.unsubscribed(sessionId)
    }

    private fun extractUserId(accessor: StompHeaderAccessor): UUID? {
        // Read X-User-Id from STOMP connect headers (native headers)
        val headers = accessor.getNativeHeader("X-User-Id") ?: return null
        return headers.firstOrNull()?.let { runCatching { UUID.fromString(it) }.getOrNull() }
    }
}
```

### `config/WebSocketConfig.kt` — no change required

The existing `WebSocketConfig` uses `enableSimpleBroker("/topic")` which already covers `/topic/ladders/{id}`. The STOMP endpoint `/ws` is shared across plan and ladder topics.

### Frontend → Backend: how userId reaches STOMP session events

**Current wire protocol (`usePlanUpdates.ts`) does NOT send `X-User-Id` on the STOMP CONNECT frame.** Plan updates don't need it because the broker-side interceptor only reads destinations. But ladder presence DOES need the userId.

**Decision:** Extend the frontend STOMP hook (new `useLadderUpdates.ts`) to pass `X-User-Id` in STOMP `connectHeaders`. On the backend, `StompHeaderAccessor.getNativeHeader("X-User-Id")` reads it from the CONNECT frame headers (which are propagated into session attributes and available in both subscribe and disconnect events). This is a pragmatic trust-based pattern consistent with the existing `X-User-Id` header used on all HTTP endpoints. If the header is missing on subscribe, log warn and skip presence tracking for that session.

Note: SessionDisconnectEvent fires BEFORE the native headers are normally accessible; to make them available the listener reads from STOMP session attributes (`sessionAttributes["userId"]`) populated at connect time. We'll add a `StompChannelInterceptor` (also in the new file, or a separate `LadderConnectInterceptor.kt`) that captures `X-User-Id` from CONNECT headers into `sessionAttributes` so disconnect handlers can find it. Alternative cleaner path: the sessionId→(ladderId, userId) map in `LadderPresenceTracker` already solves this — disconnect just looks up the session, no headers needed at disconnect time.

### CLAUDE.md update

`services/camper-service/CLAUDE.md` gets a new "Activity Ladder" feature section, plus mentions the `LadderEventPublisher`, `LadderPresenceTracker`, and `LadderStompSessionListener` components alongside the existing `PlanEventPublisher` entry.

## 7. Round Resolution Logic

### Invariants

- **Round advancement rule:** a round advances only when `ladder_votes` has one row per `ladder_participants` row for that `(ladder_id, round_number)`.
- **Lock granularity:** the entire "record vote → check count → possibly resolve round → update ladder state" sequence runs inside a single transaction that holds `SELECT ... FOR UPDATE` on the `activity_ladders` row.

### Pseudocode — `CastVoteAction.execute`

```
validate param (via validator)
preload ladder (no lock) to do cheap 403/409 checks before grabbing the lock:
    - ladder status must be ACTIVE
    - requestingUserId must be in ladder_participants
    - votedForActivityId must equal currentMatchActivityAId or currentMatchActivityBId
    - no existing vote row for (ladderId, currentRoundNumber, userId)

ladderClient.withLadderLocked(ladderId) { scoped ->
    re-fetch ladder (now under FOR UPDATE)
    re-check status == ACTIVE  (else IllegalState)
    re-check currentRoundNumber and match ids haven't advanced (else return a VoteRecorded equivalent representing "stale" — see decision log)
    re-check: no existing vote row (guards against the duplicate case)

    scoped.castVote(CastVoteParam(ladderId, currentRoundNumber, userId, votedForActivityId))
        -> on ConflictError (unique violation): return LadderError.AlreadyVoted

    val totalVoters  = participantCount (cached in ladder or re-count via participants query)
    val votesCast    = scoped.countVotesForRound(...)
    val remaining    = totalVoters - votesCast
    if (remaining > 0) return VoteOutcome.VoteRecorded(...)

    // All votes in → resolve round
    val roundResolver = RoundResolver(scoped)
    val outcome = roundResolver.resolve(ladder, currentRoundNumber)
    outcome
}
```

### State machine for `RoundResolver.resolve`

```
load votes for (ladderId, currentRoundNumber)
tally by votedForActivityId -> aCount, bCount

IF aCount == bCount:
    # Tie
    increment currentRoundNumber (no loss recorded)
    reselect pair from full candidate pool (same bracket rules as below)
    updateLadderState(newRoundNumber, new match ids, isFinal unchanged, isReset unchanged)
    RETURN VoteOutcome.RoundTied

ELSE:
    winner = higher count
    loser  = lower count
    newLosses = loserActivity.losses + 1
    newBracket = if newLosses == 1 then LOSERS else ELIMINATED
    updateActivityLossAndBracket(loserId, newLosses, newBracket)

    # Refresh activities list after mutation
    activities = client.getActivities(ladderId)
    nonEliminated = activities.filter { bracket != ELIMINATED }
    winnersBracket = nonEliminated.filter { bracket == WINNERS }
    losersBracket  = nonEliminated.filter { bracket == LOSERS }

    IF nonEliminated.size == 1:
        winnerActivityId = nonEliminated[0].id
        client.setLadderWinner(ladderId, winnerActivityId)  # also sets status=COMPLETED, clears match, updatedAt
        RETURN VoteOutcome.LadderCompleted(winnerActivityId, voteTotals)

    # Grand Final logic
    IF winnersBracket.size == 1 AND losersBracket.size == 1:
        IF !ladder.isFinalRound:
            # First Grand Final
            nextA = winnersBracket[0]
            nextB = losersBracket[0]
            updateLadderState(status=ACTIVE, newRoundNumber=currentRoundNumber+1,
                              nextA.id, nextB.id, isFinalRound=true, isGrandFinalReset=ladder.isGrandFinalReset)
            RETURN VoteOutcome.RoundDecided(...)
        ELSE:
            # Completed the Grand Final round. Who won matters:
            IF winnerJustWon == winnersBracket[0]:
                # Winners-bracket champion won the Grand Final → they are the tournament winner
                # But we already moved the losers-bracket activity to ELIMINATED via updateActivityLossAndBracket above
                # Therefore nonEliminated.size == 1 path above already handled this; we won't actually hit this branch.
                unreachable
            ELSE IF ladder.isGrandFinalReset:
                # Losers-bracket champion won the Reset — but again, nonEliminated.size == 1 already handles it.
                unreachable
            ELSE:
                # Losers-bracket champion won the first Grand Final.
                # updateActivityLossAndBracket above gave the winners-bracket champion their first loss,
                # so winnersBracket is now empty and losersBracket has both.
                # Re-pair the same two activities for the Grand Final Reset.
                # Both remaining activities are in LOSERS bracket with 1 loss each.
                # Use the pair directly.
                nextA = losersBracket[0]; nextB = losersBracket[1]
                updateLadderState(..., newRoundNumber+1, nextA, nextB, isFinalRound=true, isGrandFinalReset=true)
                RETURN VoteOutcome.RoundDecided(..., isFinalRound=true, isGrandFinalReset=true)

    # Otherwise, pick next pairing using bracket rules
    IF winnersBracket.size >= 2 AND losersBracket.size >= 2:
        pickFrom = if Random.nextBoolean() then winnersBracket else losersBracket
    ELSE IF winnersBracket.size >= 2:
        pickFrom = winnersBracket
    ELSE IF losersBracket.size >= 2:
        pickFrom = losersBracket
    ELSE:
        # edge case: one bracket has 1, other has ≥2 — play the larger bracket (per handoff step 5)
        pickFrom = if winnersBracket.size > losersBracket.size then winnersBracket else losersBracket

    nextA, nextB = two distinct random picks from pickFrom
    updateLadderState(status=ACTIVE, newRoundNumber=currentRoundNumber+1, nextA.id, nextB.id, isFinalRound=false, isGrandFinalReset=ladder.isGrandFinalReset)
    RETURN VoteOutcome.RoundDecided(winnerId, voteTotals, newRoundNumber, nextA, nextB, isFinalRound=false, isGrandFinalReset=false)
```

### Concurrency guarantee

Two voters cast the final vote simultaneously. Both requests enter the scoped `withLadderLocked` block. Postgres's `SELECT ... FOR UPDATE` serializes them: the second vote will block at `SELECT id FROM activity_ladders WHERE id = :id FOR UPDATE` until the first transaction commits. When the second transaction unblocks, it re-fetches the ladder — which now has advanced to the next round, or to COMPLETED. The second action then:

- If the ladder has moved to a new round: the vote is stale. Re-validate the match (`votedForActivityId ∈ {currentMatchActivityAId, currentMatchActivityBId}`) → fails → return `LadderError.InvalidVoteTarget`. This is correct: the user's vote was for a match that already ended. The UI will refetch and show the new match.
- If the ladder is COMPLETED: status check fails → return `LadderError.IllegalState`.

**Result:** exactly one resolution per final vote. The losing voter sees an error and their UI refetches. This is the safest and simplest approach and matches the handoff's suggestion of `SELECT ... FOR UPDATE`.

### Alternative considered

"Use `advisory_lock(ladder_id)` instead of row lock" — rejected because the ladder row itself needs locking anyway (state updates), and `FOR UPDATE` is idiomatic Postgres. Advisory locks would add complexity without benefit.

## 8. Frontend (`webapp/src/...`)

### Routes (`App.tsx`)

Add three routes, all under `<ProtectedRoute>`:
- `/activities` → `<ActivitiesListPage />`
- `/activities/new` → `<NewActivityLadderPage />`
- `/activities/:ladderId` → `<ActivityLadderPage />`

### Files to create

```
webapp/src/
├── api/
│   └── client.ts                          (EDIT — add LadderSummary, LadderDetail, LadderActivity, LadderParticipant, CurrentRound types + api.ladders.*)
├── hooks/
│   └── useLadderUpdates.ts                (NEW — STOMP hook subscribing to /topic/ladders/:id, passes X-User-Id in connectHeaders)
├── pages/
│   ├── ActivitiesListPage.tsx             (NEW)
│   ├── ActivitiesListPage.css             (NEW)
│   ├── NewActivityLadderPage.tsx          (NEW)
│   ├── NewActivityLadderPage.css          (NEW)
│   ├── ActivityLadderPage.tsx             (NEW)
│   └── ActivityLadderPage.css             (NEW)
└── components/
    ├── activityladder/
    │   ├── LadderActivityCard.tsx         (NEW — single activity visual with image, name, distance, cost)
    │   ├── LadderActivityCard.css         (NEW)
    │   ├── LadderPeoplePanel.tsx          (NEW — people panel, DRAFT "In the room" vs ACTIVE/COMPLETED "Voters" + "Watching")
    │   ├── LadderPeoplePanel.css          (NEW)
    │   ├── LadderMatchupView.tsx          (NEW — two cards side by side with vote buttons)
    │   ├── LadderMatchupView.css          (NEW)
    │   ├── LadderVoteProgress.tsx         (NEW — "3 of 5 voted" progress bar)
    │   ├── LadderVoteProgress.css         (NEW)
    │   ├── LadderOutcomeBanner.tsx        (NEW — tie / decided / final round / grand final reset / winner banners)
    │   ├── LadderOutcomeBanner.css        (NEW)
    │   └── LadderDraftActivityList.tsx    (NEW — DRAFT-state list with add/remove for creator)
    └── SideNav.tsx                        (EDIT — add "Activities" nav entry pointing at /activities)
```

### State → UI map (in `ActivityLadderPage.tsx`)

```
status === DRAFT && isCreator
    → LadderDraftActivityList (editable) + LadderPeoplePanel (all present = "In the room") + StartButton
status === DRAFT && !isCreator
    → LadderDraftActivityList (read-only) + LadderPeoplePanel + "waiting for creator to start"
status === ACTIVE && isVoter && !hasVotedThisRound
    → LadderMatchupView (with vote buttons) + LadderVoteProgress + LadderPeoplePanel (voters/watching split) + RestartButton (if creator)
status === ACTIVE && isVoter && hasVotedThisRound
    → LadderMatchupView (buttons locked, "Waiting for others…") + LadderVoteProgress + LadderPeoplePanel + RestartButton
status === ACTIVE && !isVoter   (spectator — logged in but wasn't present at Start)
    → LadderMatchupView (view-only, clear "You are watching" banner) + LadderVoteProgress + LadderPeoplePanel + RestartButton
status === ACTIVE && roundResolvedBriefly  (UI holds on a resolved payload for ~2s before next round event)
    → LadderOutcomeBanner(tie | decided with totals) + LadderMatchupView (locked)
status === COMPLETED
    → LadderOutcomeBanner(winner) + LadderMatchupView (shows final matchup dimmed, winning card highlighted) + LadderPeoplePanel + RestartButton
```

### WebSocket message handling

```typescript
useLadderUpdates(ladderId, (msg) => {
    switch (msg.action) {
        case 'presence-changed': setPresentUserIds(msg.payload.presentUserIds); break;
        case 'activity-added':
        case 'activity-removed':
        case 'started':
        case 'restarted':
            refetchLadderDetail(); break;
        case 'round-started':
            refetchLadderDetail();
            setLastResolvedOutcome(null);
            break;
        case 'vote-cast':
            // optimistic: bump votedUserIds from payload
            setVotedUserIds(prev => [...prev, msg.payload.voterId]);
            // also refetch to get authoritative vote count
            refetchLadderDetail();
            break;
        case 'round-resolved':
            setLastResolvedOutcome(msg.payload);  // used to briefly show OutcomeBanner before next round-started arrives
            break;
        case 'completed':
            refetchLadderDetail();
            break;
    }
});
```

### People panel (`LadderPeoplePanel.tsx`)

- Props: `status`, `participants` (frozen voter list from ladder detail), `presentUserIds` (live from WS), `votedUserIds` (who voted this round), `currentUserId`.
- DRAFT: single "In the room" section — render every user in `presentUserIds`. Requires the webapp to fetch user info (username, avatarSeed) on demand for new present users; cache by userId. For initial version, render only the `participants[]` from the detail (empty in DRAFT) plus `presentUserIds`. Since DRAFT has no persisted participants, the component calls a new `api.users.getById` method per unknown userId and memoizes. Reuse `CamperAvatar` with `avatarSeed` + `seatIndex`.
- ACTIVE/COMPLETED:
  - **Voters** section: iterate `participants`. Each row → `CamperAvatar` + username + online dot (present? — check `presentUserIds`) + voted checkmark (check `votedUserIds` — not applicable in COMPLETED state). Dim offline voters, label them "offline — waiting".
  - **Watching** section: `presentUserIds.filter(id => !participants.some(p => p.userId === id))`. No voted indicator. Fetch user info on-demand as above.
- Reuses `components/CamperAvatar.tsx` and `lib/avatarConstants.ts`.

### API client additions (`src/api/client.ts`)

```typescript
export interface LadderSummary {
  id: string;
  title: string;
  status: 'DRAFT' | 'ACTIVE' | 'COMPLETED';
  creatorId: string;
  activityCount: number;
  participantCount: number;
  winnerActivityId: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface LadderActivity {
  id: string;
  name: string;
  imageUrl: string;
  distanceMinutes: number;
  costPerPerson: number;
  losses: number;
  bracket: 'WINNERS' | 'LOSERS' | 'ELIMINATED';
  displayOrder: number;
}

export interface LadderParticipant {
  userId: string;
  username: string | null;
  avatarSeed: string | null;
}

export interface CurrentRoundView {
  roundNumber: number;
  activityAId: string;
  activityBId: string;
  votesCast: number;
  totalVoters: number;
  votedUserIds: string[];
}

export interface LadderDetail {
  id: string;
  title: string;
  status: 'DRAFT' | 'ACTIVE' | 'COMPLETED';
  creatorId: string;
  activities: LadderActivity[];
  participants: LadderParticipant[];
  currentRound: CurrentRoundView | null;
  winnerActivityId: string | null;
  isFinalRound: boolean;
  isGrandFinalReset: boolean;
  createdAt: string;
  updatedAt: string;
}

api.ladders = {
  list(): Promise<LadderSummary[]>,
  get(id: string): Promise<LadderDetail>,
  create(payload: { title: string; activities: LadderActivityInput[] }): Promise<LadderDetail>,
  addActivity(id: string, payload: LadderActivityInput): Promise<LadderActivity>,
  removeActivity(id: string, activityId: string): Promise<void>,
  start(id: string): Promise<LadderDetail>,
  vote(id: string, payload: { votedForActivityId: string }): Promise<{ voteCount: number; votersRemaining: number }>,
  restart(id: string): Promise<LadderDetail>,
};
```

### `useLadderUpdates.ts`

Copy of `usePlanUpdates.ts` but:
- Destination `/topic/ladders/${ladderId}`.
- `brokerURL` same as existing.
- **Adds `connectHeaders: { 'X-User-Id': localStorage.getItem('camper.userId') ?? '' }`** so the backend listener can extract it.
- Message type: `LadderUpdateMessage = { resource: string; action: string; payload?: Record<string, unknown> }`.

### Visual language

Follow existing "Enchanted Expedition Journal" aesthetic: parchment-textured cards, night-sky or dusk parallax background on the list page, campfire-inspired border accents. Reuse `components/ui/Modal` for confirmation (restart, delete activity) and `components/ui/Button` for all buttons.

## 9. PR Stack

Order and file lists below. All PRs are on top of `main`. Each PR is a single concern and should compile independently.

### PR 1 — `[plan] feat(activity-ladder): plan`
**Files:**
- `camper/docs/activity-ladder/plan.md` (this document)

### PR 2 — `[db] feat(activity-ladder): db contracts`
Schema files only, plus migrations. Executable but nothing consumes them yet.
**Files:**
- `camper/databases/camper-db/schema/tables/010_activity_ladders.sql`
- `camper/databases/camper-db/schema/tables/011_ladder_activities.sql`
- `camper/databases/camper-db/schema/tables/012_ladder_participants.sql`
- `camper/databases/camper-db/schema/tables/013_ladder_votes.sql`
- `camper/databases/camper-db/migrations/V040__create_activity_ladders.sql`
- `camper/databases/camper-db/migrations/V041__create_ladder_activities.sql`
- `camper/databases/camper-db/migrations/V042__create_ladder_participants.sql`
- `camper/databases/camper-db/migrations/V043__create_ladder_votes.sql`
- `camper/databases/camper-db/migrations/rollback/R040__drop_activity_ladders.sql`
- `camper/databases/camper-db/migrations/rollback/R041__drop_ladder_activities.sql`
- `camper/databases/camper-db/migrations/rollback/R042__drop_ladder_participants.sql`
- `camper/databases/camper-db/migrations/rollback/R043__drop_ladder_votes.sql`
- `camper/databases/camper-db/CLAUDE.md` (EDIT — add schema blocks, relationships, invariants)

### PR 3 — `[client] feat(activity-ladder): client contracts`
All model types, param objects, interface, **stubbed** `JdbiActivityLadderClient` + `FakeActivityLadderClient` (all methods `TODO("Implementation in next PR")`). Build stays green.
**Files:**
- `camper/settings.gradle.kts` (EDIT — include `:clients:activity-ladder-client`)
- `camper/clients/activity-ladder-client/build.gradle.kts`
- `camper/clients/activity-ladder-client/CLAUDE.md`
- `camper/clients/activity-ladder-client/src/main/kotlin/com/acme/clients/activityladderclient/ActivityLadderClientFactory.kt`
- `camper/clients/activity-ladder-client/src/main/kotlin/com/acme/clients/activityladderclient/model/Ladder.kt`
- `.../model/LadderStatus.kt`
- `.../model/LadderBracket.kt`
- `.../model/LadderActivity.kt`
- `.../model/LadderParticipant.kt`
- `.../model/LadderVote.kt`
- `.../api/ActivityLadderClient.kt`
- `.../api/ActivityLadderClientParams.kt`
- `.../internal/JdbiActivityLadderClient.kt` (stub — `TODO` bodies, uses a constructor that takes `Jdbi`)
- `.../internal/validations/` (all 16 validator stubs, each returning `success(Unit)`)
- `.../internal/operations/` (empty files, declared but not implemented — OR all implemented skeletons calling throwing `TODO`; developer's choice)
- `src/testFixtures/kotlin/com/acme/clients/activityladderclient/fake/FakeActivityLadderClient.kt` (stub — `TODO` bodies)
- `src/testFixtures/kotlin/com/acme/clients/activityladderclient/test/ActivityLadderTestDb.kt` (wraps `MigrationRunner`)
- `src/test/resources/docker-java.properties`

### PR 4 — `[service] feat(activity-ladder): service contracts`
All service-layer types + controller stub. Compiles, wires up against stubbed client. Endpoints return a TODO-style error until PR 9.
**Files:**
- `camper/services/camper-service/build.gradle.kts` (EDIT — add dep on `:clients:activity-ladder-client`)
- `.../features/activityladder/model/` (all 8 model files)
- `.../features/activityladder/error/LadderError.kt`
- `.../features/activityladder/params/ActivityLadderServiceParams.kt`
- `.../features/activityladder/dto/` (all DTO files)
- `.../features/activityladder/mapper/ActivityLadderMapper.kt` (skeleton)
- `.../features/activityladder/validations/` (all 8 validators, default or with checks where applicable)
- `.../features/activityladder/actions/` (all 8 action skeletons — `TODO("impl")` bodies)
- `.../features/activityladder/actions/roundresolution/RoundResolver.kt` (skeleton)
- `.../features/activityladder/service/ActivityLadderService.kt` (facade)
- `.../features/activityladder/controller/ActivityLadderController.kt` (all endpoints wired, calling service — service throws `TODO` so endpoints 500; OK for contract PR because no tests run them yet)
- `.../websocket/LadderUpdateMessage.kt`
- `.../websocket/LadderEventPublisher.kt`
- `.../websocket/LadderPresenceTracker.kt`
- `.../websocket/LadderStompSessionListener.kt`
- `.../config/ActivityLadderClientConfig.kt`
- `.../config/ActivityLadderServiceConfig.kt`
- `.../common/error/ResultExtensions.kt` (EDIT — add `LadderError.toResponseEntity` + `@JvmName("activityLadderResultToResponseEntity")` overload)

### PR 5 — `[db-impl] feat(activity-ladder): db implementation`
No actual code — the migrations are already the full implementation. **Skip this PR** (merged into PR 2).

### PR 6 — `[client-impl] feat(activity-ladder): client implementation`
Replace all `TODO` bodies in client, operations, adapters, and fake.
**Files (modified from PR 3):**
- All files under `clients/activity-ladder-client/src/main/kotlin/com/acme/clients/activityladderclient/internal/operations/`
- All files under `.../internal/adapters/`
- `.../internal/JdbiActivityLadderClient.kt` (real facade wiring)
- `.../internal/operations/RestartLadder.kt` (in-transaction composite operation)
- `src/testFixtures/.../fake/FakeActivityLadderClient.kt` (real in-memory impl with per-ladder lock)
- `src/main/kotlin/.../ActivityLadderClientFactory.kt` (finalize)

### PR 7 — `[service-impl] feat(activity-ladder): service implementation`
Replace all service-side `TODO` bodies: all 8 actions, `RoundResolver`, controller logic (event publishing sequence), mapper.
**Files (modified from PR 4):**
- All files under `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/activityladder/actions/`
- `.../features/activityladder/actions/roundresolution/RoundResolver.kt`
- `.../features/activityladder/controller/ActivityLadderController.kt` (concrete event publishing from VoteOutcome)
- `.../features/activityladder/mapper/ActivityLadderMapper.kt` (concrete)
- `.../features/activityladder/service/ActivityLadderService.kt` (concrete facade)

### PR 8 — `[webapp] feat(activity-ladder): frontend implementation`
**Files:**
- `camper/webapp/src/App.tsx` (EDIT — add three routes)
- `camper/webapp/src/components/SideNav.tsx` (EDIT — add "Activities" nav entry)
- `camper/webapp/src/api/client.ts` (EDIT — add types and api.ladders)
- `camper/webapp/src/hooks/useLadderUpdates.ts` (NEW)
- `camper/webapp/src/pages/ActivitiesListPage.tsx` + `.css`
- `camper/webapp/src/pages/NewActivityLadderPage.tsx` + `.css`
- `camper/webapp/src/pages/ActivityLadderPage.tsx` + `.css`
- `camper/webapp/src/components/activityladder/LadderActivityCard.tsx` + `.css`
- `camper/webapp/src/components/activityladder/LadderPeoplePanel.tsx` + `.css`
- `camper/webapp/src/components/activityladder/LadderMatchupView.tsx` + `.css`
- `camper/webapp/src/components/activityladder/LadderVoteProgress.tsx` + `.css`
- `camper/webapp/src/components/activityladder/LadderOutcomeBanner.tsx` + `.css`
- `camper/webapp/src/components/activityladder/LadderDraftActivityList.tsx`

### PR 9 — `[client-test] feat(activity-ladder): client tests`
**Files:**
- `camper/clients/activity-ladder-client/src/test/kotlin/com/acme/clients/activityladderclient/JdbiActivityLadderClientTest.kt` — Testcontainers-backed integration tests for each operation + `withLadderLocked` concurrency test (two threads racing to be the last voter).

### PR 10 — `[service-test] feat(activity-ladder): service tests`
**Files:**
- `camper/services/camper-service/src/test/kotlin/com/acme/services/camperservice/features/activityladder/service/ActivityLadderServiceTest.kt` — unit tests using `FakeActivityLadderClient` + `FakeUserClient`. Covers every action + every `RoundResolver` branch (tie, decided, bracket transition, grand final, grand final reset, completion, restart from ACTIVE and COMPLETED).
- `.../actions/roundresolution/RoundResolverTest.kt` — pure-logic tests against the fake client.
- `.../websocket/LadderPresenceTrackerTest.kt` — unit tests of subscribed/unsubscribed sequences, multi-tab counting, broadcasts.

### PR 11 — `[acceptance] feat(activity-ladder): acceptance tests`
**Files:**
- `.../features/activityladder/acceptance/fixture/ActivityLadderFixture.kt` — inserts ladders, activities, participants, votes via JDBC; truncates `ladder_votes, ladder_participants, ladder_activities, activity_ladders, users CASCADE`.
- `.../features/activityladder/acceptance/ActivityLadderAcceptanceTest.kt` — full API scenarios:
  - create → add/remove activity → start → vote progression → complete
  - tie reshuffling
  - late-joiner spectator blocked from voting
  - waiting-for-disconnected-voter (simulated via absent vote)
  - restart from ACTIVE and from COMPLETED
  - creator-only enforcement on add/remove/start/restart
  - 403 for non-participant voting
- `.../websocket/LadderWebSocketIntegrationTest.kt` — verifies `started`, `vote-cast`, `round-resolved`, `round-started`, `completed`, `restarted` events reach broker channel. Uses the same channel-interceptor pattern as `WebSocketIntegrationTest.kt`. Also exercises `presence-changed` by simulating STOMP subscribe/unsubscribe events directly against the `LadderStompSessionListener`.

### PR 12 — `[docs] feat(activity-ladder): documentation updates`
**Files:**
- `camper/services/camper-service/CLAUDE.md` (EDIT — new Activity Ladder feature section)
- `camper/webapp/CLAUDE.md` (EDIT — new routes, hook, components, API table entries)
- `camper/CLAUDE.md` (EDIT — add `activity-ladder-client` to Project Structure)
- `camper/clients/activity-ladder-client/CLAUDE.md` (finalize if not completed in PR 3)

## 10. Constructor-Call-Site Impact Analysis

**No existing data classes are modified.** The feature is purely additive:
- No change to `User`, `Plan`, `PlanMember`, `Item`, etc.
- No change to `UserClient`, `PlanClient`, or any other existing client interface.
- No change to `PlanEventPublisher` or `PlanUpdateMessage` (`LadderEventPublisher` and `LadderUpdateMessage` are brand-new).

The only existing files that change are:
- `camper/settings.gradle.kts` — add new module include (non-breaking).
- `services/camper-service/build.gradle.kts` — add new client dependency.
- `common/error/ResultExtensions.kt` — append new overload with unique `@JvmName` (non-breaking — existing overloads untouched).
- `services/camper-service/CLAUDE.md`, `webapp/CLAUDE.md`, `webapp/src/App.tsx`, `webapp/src/components/SideNav.tsx`, `webapp/src/api/client.ts` — all additive.

**No grep-then-update cascade is needed.** This keeps the contract PRs very low risk.

## 11. Test Fixture Impact

Existing acceptance-test fixtures all truncate `users CASCADE` in `@BeforeEach`. New tables with FKs to `users` (`activity_ladders.creator_id`, `ladder_participants.user_id`, `ladder_votes.user_id`) will be transitively truncated by CASCADE, but only if the other test's truncate happens to cascade through them.

**Concrete risk:** when the existing `WebSocketIntegrationTest`, `ItineraryFixture`, `RecipeFixture`, `UserFixture`, `LogBookFixture`, `ItemFixture`, `AssignmentFixture`, `GearPackFixture`, `MealPlanFixture`, or `GearSyncFixture` truncates `users CASCADE`, Postgres will fail UNLESS all tables FK-ing to `users` participate in the cascade path. Adding `activity_ladders` (with FK `creator_id` → `users.id` ON DELETE RESTRICT) **would break every one of these tests** because RESTRICT blocks user deletion even via CASCADE.

**Decision:** Change `activity_ladders.creator_id` FK to `ON DELETE CASCADE` — consistent with how gear packs treat the `created_by` nullable column (though gear_packs is nullable, ladders are not). Rationale: if we delete a test user, we want ladder rows to go away too. This also matches the production intent — if a user account is deleted, their orphaned ladders serve no purpose.

**Rollback consequence:** if CASCADE is chosen, update the entity table above and `V040` migration to use `ON DELETE CASCADE` (not `RESTRICT`).

### Fixtures that MUST be updated to also truncate the new tables

Only **`WebSocketIntegrationTest.setUp()`** currently truncates `users CASCADE` without listing all descendants. Adding `ladder_votes, ladder_participants, ladder_activities, activity_ladders` to that TRUNCATE list in the test is the safest way to ensure the table list stays explicit:

```
TRUNCATE TABLE ladder_votes, ladder_participants, ladder_activities, activity_ladders,
               assignment_members, assignments, itinerary_events, itineraries,
               items, plan_members, plans, users CASCADE
```

The other fixtures rely on CASCADE and will work transitively as long as the FK is `ON DELETE CASCADE`. However, for explicitness and to avoid ordering surprises in Postgres, **the new `ActivityLadderFixture.truncateAll()` should also include all four new tables explicitly**, and each existing fixture that lists `users` in its TRUNCATE should be updated to also prepend the ladder tables — but only if they might conflict. Since each test in its own feature uses its own fixture, the only cross-fixture risk is `WebSocketIntegrationTest`. **Action: update `WebSocketIntegrationTest.setUp()` TRUNCATE list in PR 11.**

The other nine fixtures work fine without modification because:
- Their tests don't create ladder rows.
- CASCADE on user deletion removes any ladder rows transitively.
- `ladder_activities`, `ladder_participants`, `ladder_votes` all CASCADE from `activity_ladders`.

## 12. Open Questions

1. **Creator auto-subscribe to WS?** The handoff says the creator clicks Start "at which moment the server snapshots the in-memory presence set". This presumes the creator's browser has opened a WS subscription to `/topic/ladders/{id}`. The plan assumes the `ActivityLadderPage` always opens a WS subscription on mount for any viewer (including the creator). **Assumed yes.** The alternative — where the creator must manually "enter the room" before Start works — is rejected as un-intuitive.
2. **`LadderPresenceTracker` scope:** I chose `@Component` (singleton). This is correct for in-memory tracking across requests. Flagged in Decisions log.
3. **Do we broadcast `presence-changed` in COMPLETED state?** Yes — the people panel still shows who's connected; this is cosmetic and lets the panel update when users leave the winner's celebration page.
4. **Should `GET /api/ladders` (list) include any filtering?** Handoff says "no filtering by creator". Plan implements it as unfiltered. Pagination supported via `limit`/`offset` query params — TBD whether the webapp uses them. **Assumption:** no pagination in MVP; the list is small.
5. **UI detail: `/activities/new` standalone page vs a modal on `/activities`?** Handoff lists it as a route. Plan sticks with a dedicated route. The create page is simple enough to not need a modal overlay.

## 13. Decisions Log

1. **Bracket selection when both brackets have ≥ 2:** random (via `kotlin.random.Random.nextBoolean()`). Not reproducible (no seed stored). Sufficient per handoff's explicit permission.
2. **No `round_log` table.** Per handoff.
3. **Index on `ladder_votes(ladder_id, round_number)`:** YES, added as `idx_ladder_votes_ladder_round`. Justification: the hot path "count votes for current round" runs on every vote cast.
4. **`LadderPresenceTracker` is a Spring `@Component` singleton.** Not scoped per request or per session — it's an in-process map shared across all requests and WS sessions on this node.
5. **Single-node deployment assumption.** Presence tracking is in-process. This feature will NOT work correctly across multiple service instances. Acceptable per handoff (standalone MVP feature, not multi-instance).
6. **`activity_ladders.creator_id` FK uses `ON DELETE CASCADE`**, not `RESTRICT`, for test-fixture compatibility and cleaner user-deletion semantics.
7. **The client interface exposes `withLadderLocked`** rather than hiding the lock inside an action helper. Reason: JDBI transaction scoping lives inside the client module; the service must never see `Jdbi` or `Handle` types.
8. **`ValidateStartLadder` does NOT validate activity count** — that check requires data outside the param object. Documented as a deliberate deviation from the 1:1-with-actions validator rule. The check is instead inline in `StartLadderAction.execute` after fetching the activities list. Returns `LadderError.NotEnoughActivities`.
9. **Ladder list counts:** `LadderSummaryResponse.activityCount` and `participantCount` are computed by the service via a follow-up `getActivities` + `getParticipants` per ladder (N+1) rather than via SQL COUNT subqueries. Acceptable because the list is small (MVP). If the list grows, introduce a `getLadderList` variant that returns `(Ladder, activityCount, participantCount)` via a single JOIN+COUNT query — out of scope for v1.
10. **Stale vote handling:** if a vote arrives after the round has advanced (losing race condition), return `LadderError.InvalidVoteTarget` rather than silently accepting the stale vote. The frontend will refetch and render the new match.
11. **Frontend sends `X-User-Id` in STOMP CONNECT frame's native headers** so the backend listener can populate the presence tracker with the user's identity. This is an addition to the existing trust-based auth pattern.
12. **Event envelope reuse:** `LadderUpdateMessage` is a sibling of `PlanUpdateMessage` — NOT a shared base class. Keeps ladders and plans fully independent per handoff.
13. **`LadderVote` has no `updatedAt`.** Votes are immutable once cast.
14. **`LadderActivity` has no `updatedAt`.** Losses/bracket mutations during round resolution don't require timestamping.

---

## Retro Report

### Surprises

- The existing `WebSocketConfig` uses `enableSimpleBroker` (in-memory broker) — perfect for this feature. No external STOMP broker to worry about.
- No existing STOMP session-event listener exists in the codebase. This feature will be the first to introduce `SessionSubscribeEvent` / `SessionDisconnectEvent` handling. The pattern has to be established fresh rather than copied.
- The existing `usePlanUpdates.ts` frontend hook does NOT pass `X-User-Id` in STOMP `connectHeaders`, because plan WS updates only use broadcast (server-to-client), never presence. Ladders need the reverse direction (client identifies itself to the server), which forces the new `useLadderUpdates.ts` to add connect headers. This is a small but real extension of the existing WS pattern.
- The `ladder_activities` self-referential FK from `activity_ladders` forced a two-migration split (V040 creates `activity_ladders` without the three match/winner FKs, V041 creates `ladder_activities` and adds the FKs via `ALTER TABLE`). The schema couldn't be collapsed into a single migration because the tables mutually reference each other.
- None of the existing fixture TRUNCATE lists are shared — every fixture has its own hand-maintained list. Adding a new top-level table requires the architect to check every fixture for TRUNCATE conflicts. Only `WebSocketIntegrationTest` will need a manual edit; the other nine fixtures are unaffected because their tests don't create ladder data.

### Patterns discovered

- **Locking via client-scoped closure.** The `withLadderLocked` pattern — where the client exposes a `(T) -> Result<T, AppError>` entry point that runs inside a locked JDBI transaction — is new to this codebase. It cleanly avoids leaking `Jdbi.Handle` into the service layer. Worth documenting in `service-manager` SKILL.md once it proves itself.
- **VoteOutcome sealed class returned from an action.** Most actions in the codebase return `Result<Model, Error>`. This feature needs actions that return richer shapes (sealed `VoteOutcome`) because the controller needs to emit different WS events depending on which branch fired. Clean alternative to having the action touch the publisher directly.
- **Multi-event controller methods.** The vote endpoint may emit 2–3 WebSocket events (`vote-cast` → `round-resolved` → `round-started` OR `completed`). Existing controllers emit exactly one. Worth noting so reviewers don't flag it.

### Pain points

- **Mutually referential FKs** (`activity_ladders` ↔ `ladder_activities`) required a two-step migration. Not complex, but fiddly. Idempotent `ALTER TABLE ... ADD CONSTRAINT` needs a `DO $$ ... EXCEPTION WHEN duplicate_object THEN NULL; END $$;` wrapper, which is ugly.
- **Presence tracking on disconnect** needs a sessionId→(ladderId, userId) reverse map because `SessionDisconnectEvent` only carries the sessionId, not the subscribed destinations. This is documented but easy to miss.
- **Validation at the service-action boundary cannot always live in a validator class.** The "2+ activities required to start" check needs data that's not in the `StartLadderParam`. I punted by doing the check inline in the action. The pattern treats validators as pure param checks; they can't enforce preconditions that depend on state. Worth noting in `service-manager` skill.

### Recommendations

- Update `service-manager` SKILL.md with a note on **state-dependent preconditions belonging in the action body**, not the validator. Today's guidance implies all checks go in the validator.
- Add a small section to the SKILL on **client-layer transactional helpers** (the `withLocked`/`inTransaction` pattern) as an optional escape hatch for features that need atomic multi-operation sequences. Currently the skill is silent on this.
- Frontend WebSocket presence tracking is a reusable pattern. Consider factoring the next iteration into a generic `useStompPresence(topicPrefix, resourceId, userId)` hook that both ladders and future live-collab features can use.
- The TRUNCATE list in existing fixtures is a fragile maintenance burden. Long-term, a shared test utility that introspects `information_schema` and TRUNCATE-CASCADEs every table in the test DB would eliminate the "which fixture needs updating" problem. Out of scope here.

No issues were catastrophic. The plan was smooth overall because (a) the handoff was extraordinarily detailed, (b) the codebase has very consistent patterns in itinerary/gearpack for me to mirror, and (c) the feature is cleanly additive — no existing data classes are touched. The only substantive design decisions were around concurrency (FOR UPDATE vs advisory lock, where the lock helper lives) and state-dependent validation placement, both of which are resolved in the Decisions Log.
