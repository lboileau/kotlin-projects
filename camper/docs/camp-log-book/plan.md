# Camp Log Book — Feature Plan

## Feature Summary

Add a camp log book as a fifth interactable item in the campsite scene, positioned on a stool beside the tent. The log book is a per-plan resource that provides two capabilities: **FAQ/Help** (anyone can ask questions, managers/owners can answer) and **Journal Entries** (any plan member can write and edit free-form entries displayed as flippable pages). The UI shows FAQs on the first "page", then each journal entry is its own flippable page ordered by `page_number`. The backend exposes a single `log-book-client` for data access, and the service layer enforces role-based permissions using the existing `PlanRoleAuthorizer`.

---

## Entities

### LogBookFaq

| Field        | Type           | Constraints                                |
|--------------|----------------|--------------------------------------------|
| id           | UUID           | PK, generated                              |
| planId       | UUID           | FK → plans.id (CASCADE), NOT NULL          |
| question     | String (TEXT)  | NOT NULL                                   |
| askedById    | UUID           | FK → users.id, NOT NULL                    |
| answer       | String (TEXT)  | Nullable (unanswered until manager replies) |
| answeredById | UUID           | FK → users.id, Nullable                    |
| createdAt    | Instant        | NOT NULL, DEFAULT now()                    |
| updatedAt    | Instant        | NOT NULL, DEFAULT now()                    |

### LogBookJournalEntry

| Field      | Type           | Constraints                       |
|------------|----------------|-----------------------------------|
| id         | UUID           | PK, generated                     |
| planId     | UUID           | FK → plans.id (CASCADE), NOT NULL |
| userId     | UUID           | FK → users.id, NOT NULL           |
| pageNumber | Int            | NOT NULL, auto-increment per plan |
| content    | String (TEXT)  | NOT NULL                          |
| createdAt  | Instant        | NOT NULL, DEFAULT now()           |
| updatedAt  | Instant        | NOT NULL, DEFAULT now()           |

The `pageNumber` provides stable ordering for the flippable page UI. When a journal entry is deleted, that page is simply removed — no renumbering occurs. New entries are assigned the next available page number (`MAX(page_number) + 1` within the plan, or `1` if none exist).

---

## Database Changes

### Table: `log_book_faqs`

```sql
CREATE TABLE IF NOT EXISTS log_book_faqs (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id         UUID            NOT NULL REFERENCES plans(id) ON DELETE CASCADE,
    question        TEXT            NOT NULL,
    asked_by_id     UUID            NOT NULL REFERENCES users(id),
    answer          TEXT,
    answered_by_id  UUID            REFERENCES users(id),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_log_book_faqs_plan_id ON log_book_faqs(plan_id);
```

**Migration:** `V025__create_log_book_faqs.sql`

### Table: `log_book_journal_entries`

```sql
CREATE TABLE IF NOT EXISTS log_book_journal_entries (
    id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id     UUID            NOT NULL REFERENCES plans(id) ON DELETE CASCADE,
    user_id     UUID            NOT NULL REFERENCES users(id),
    page_number INTEGER         NOT NULL,
    content     TEXT            NOT NULL,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_log_book_journal_entries_plan_id ON log_book_journal_entries(plan_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_log_book_journal_entries_plan_page ON log_book_journal_entries(plan_id, page_number);
```

**Migration:** `V026__create_log_book_journal_entries.sql`

The `page_number` is assigned in the `CreateJournalEntry` operation by computing `COALESCE(MAX(page_number), 0) + 1` for the given `plan_id`. The unique index on `(plan_id, page_number)` guarantees no duplicates.

---

## Client Interface

**Module:** `clients/log-book-client`
**Package:** `com.acme.clients.logbookclient`

### Models

```kotlin
// model/LogBookFaq.kt
data class LogBookFaq(
    val id: UUID,
    val planId: UUID,
    val question: String,
    val askedById: UUID,
    val answer: String?,
    val answeredById: UUID?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

// model/LogBookJournalEntry.kt
data class LogBookJournalEntry(
    val id: UUID,
    val planId: UUID,
    val userId: UUID,
    val pageNumber: Int,
    val content: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)
```

### Interface

```kotlin
// api/LogBookClient.kt
interface LogBookClient {

    /** Create a new FAQ question (unanswered). */
    fun createFaq(param: CreateFaqParam): Result<LogBookFaq, AppError>

    /** Answer or update the answer to an existing FAQ. */
    fun answerFaq(param: AnswerFaqParam): Result<LogBookFaq, AppError>

    /** List all FAQs for a plan, ordered by created_at desc. */
    fun getFaqsByPlanId(param: GetFaqsByPlanIdParam): Result<List<LogBookFaq>, AppError>

    /** Delete a FAQ. */
    fun deleteFaq(param: DeleteFaqParam): Result<Unit, AppError>

    /** Create a journal entry. Page number is auto-assigned. */
    fun createJournalEntry(param: CreateJournalEntryParam): Result<LogBookJournalEntry, AppError>

    /** Update a journal entry's content. */
    fun updateJournalEntry(param: UpdateJournalEntryParam): Result<LogBookJournalEntry, AppError>

    /** List all journal entries for a plan, ordered by page_number asc. */
    fun getJournalEntriesByPlanId(param: GetJournalEntriesByPlanIdParam): Result<List<LogBookJournalEntry>, AppError>

    /** Delete a journal entry. */
    fun deleteJournalEntry(param: DeleteJournalEntryParam): Result<Unit, AppError>
}
```

### Params

```kotlin
// api/LogBookClientParams.kt
data class CreateFaqParam(
    val planId: UUID,
    val question: String,
    val askedById: UUID,
)

data class AnswerFaqParam(
    val id: UUID,
    val answer: String,
    val answeredById: UUID,
)

data class GetFaqsByPlanIdParam(val planId: UUID)

data class DeleteFaqParam(val id: UUID)

data class CreateJournalEntryParam(
    val planId: UUID,
    val userId: UUID,
    val content: String,
)

data class UpdateJournalEntryParam(
    val id: UUID,
    val content: String,
)

data class GetJournalEntriesByPlanIdParam(val planId: UUID)

data class DeleteJournalEntryParam(val id: UUID)
```

### Internal Structure

```
internal/
├── JdbiLogBookClient.kt              # Facade delegating to operations
├── operations/
│   ├── CreateFaq.kt
│   ├── AnswerFaq.kt
│   ├── GetFaqsByPlanId.kt
│   ├── DeleteFaq.kt
│   ├── CreateJournalEntry.kt          # Assigns page_number via MAX+1 query
│   ├── UpdateJournalEntry.kt
│   ├── GetJournalEntriesByPlanId.kt   # ORDER BY page_number ASC
│   └── DeleteJournalEntry.kt
├── validations/
│   ├── ValidateCreateFaq.kt           # question must not be blank
│   ├── ValidateAnswerFaq.kt           # answer must not be blank
│   ├── ValidateGetFaqsByPlanId.kt     # default (success)
│   ├── ValidateDeleteFaq.kt           # default (success)
│   ├── ValidateCreateJournalEntry.kt  # content must not be blank
│   ├── ValidateUpdateJournalEntry.kt  # content must not be blank
│   ├── ValidateGetJournalEntriesByPlanId.kt  # default (success)
│   └── ValidateDeleteJournalEntry.kt  # default (success)
└── adapters/
    ├── LogBookFaqRowAdapter.kt
    └── LogBookJournalEntryRowAdapter.kt
```

### Factory

```kotlin
// LogBookClientFactory.kt
fun createLogBookClient(): LogBookClient {
    val url = System.getProperty("DB_URL") ?: System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5433/camper_db"
    val user = System.getProperty("DB_USER") ?: System.getenv("DB_USER") ?: "postgres"
    val password = System.getProperty("DB_PASSWORD") ?: System.getenv("DB_PASSWORD") ?: "postgres"
    val jdbi = Jdbi.create(url, user, password)
    return JdbiLogBookClient(jdbi)
}
```

### Fake (testFixtures)

```kotlin
// fake/FakeLogBookClient.kt
class FakeLogBookClient : LogBookClient {
    private val faqStore = ConcurrentHashMap<UUID, LogBookFaq>()
    private val journalStore = ConcurrentHashMap<UUID, LogBookJournalEntry>()
    // Reuses validators from internal/validations/
    // ... in-memory implementation of all methods
    // createJournalEntry assigns pageNumber = max(existing pages for plan) + 1
    fun reset() { faqStore.clear(); journalStore.clear() }
    fun seedFaq(vararg entities: LogBookFaq) { ... }
    fun seedJournalEntry(vararg entities: LogBookJournalEntry) { ... }
}
```

---

## Service Layer

**Feature path:** `services/camper-service/.../features/logbook/`

### Service Model

```kotlin
// model/LogBookFaq.kt (service-level, separate from client model)
data class LogBookFaq(
    val id: UUID,
    val planId: UUID,
    val question: String,
    val askedById: UUID,
    val answer: String?,
    val answeredById: UUID?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

// model/LogBookJournalEntry.kt
data class LogBookJournalEntry(
    val id: UUID,
    val planId: UUID,
    val userId: UUID,
    val pageNumber: Int,
    val content: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)
```

### Service Params

```kotlin
// params/LogBookServiceParams.kt
data class AskFaqParam(
    val planId: UUID,
    val question: String,
    val requestingUserId: UUID,
)

data class AnswerFaqParam(
    val faqId: UUID,
    val planId: UUID,
    val answer: String,
    val requestingUserId: UUID,
)

data class GetFaqsParam(
    val planId: UUID,
    val requestingUserId: UUID,
)

data class DeleteFaqParam(
    val faqId: UUID,
    val planId: UUID,
    val requestingUserId: UUID,
)

data class CreateJournalEntryParam(
    val planId: UUID,
    val content: String,
    val requestingUserId: UUID,
)

data class UpdateJournalEntryParam(
    val entryId: UUID,
    val planId: UUID,
    val content: String,
    val requestingUserId: UUID,
)

data class GetJournalEntriesParam(
    val planId: UUID,
    val requestingUserId: UUID,
)

data class DeleteJournalEntryParam(
    val entryId: UUID,
    val planId: UUID,
    val requestingUserId: UUID,
)
```

### Actions

| Action                     | Auth Required                     | Description                                       |
|----------------------------|-----------------------------------|---------------------------------------------------|
| `AskFaqAction`             | Any plan member                   | Creates a new FAQ question (unanswered)            |
| `AnswerFaqAction`          | OWNER or MANAGER                  | Sets the answer on an existing FAQ                 |
| `GetFaqsAction`            | Any plan member                   | Lists all FAQs for the plan                        |
| `DeleteFaqAction`          | OWNER, MANAGER, or the asker      | Deletes a FAQ                                      |
| `CreateJournalEntryAction` | Any plan member                   | Creates a new journal entry (auto page number)     |
| `UpdateJournalEntryAction` | Original author only              | Updates journal entry content                      |
| `GetJournalEntriesAction`  | Any plan member                   | Lists all journal entries ordered by page_number   |
| `DeleteJournalEntryAction` | OWNER, MANAGER, or the author     | Deletes a journal entry                            |

### Validations (1:1 with actions)

| Validation                        | Rules                                     |
|-----------------------------------|-------------------------------------------|
| `ValidateAskFaq`                  | question must not be blank                |
| `ValidateAnswerFaq`               | answer must not be blank                  |
| `ValidateGetFaqs`                 | default (success)                         |
| `ValidateDeleteFaq`               | default (success)                         |
| `ValidateCreateJournalEntry`      | content must not be blank                 |
| `ValidateUpdateJournalEntry`      | content must not be blank                 |
| `ValidateGetJournalEntries`       | default (success)                         |
| `ValidateDeleteJournalEntry`      | default (success)                         |

### Error Type

```kotlin
// error/LogBookError.kt
sealed class LogBookError(override val message: String) : AppError {
    data class NotFound(val entityId: UUID) : LogBookError("Log book entry not found: $entityId")
    data class Invalid(val field: String, val reason: String) : LogBookError("Invalid log book $field: $reason")
    data class Forbidden(val planId: UUID, val userId: UUID) : LogBookError("User $userId is not authorized for this action in plan $planId")

    companion object {
        fun fromClientError(error: AppError): LogBookError = when (error) {
            is NotFoundError -> NotFound(UUID.fromString(error.id))
            is ValidationError -> Invalid(error.field, error.reason)
            else -> Invalid("unknown", error.message)
        }
    }
}
```

### DTOs

```kotlin
// dto/LogBookRequest.kt
data class AskFaqRequest(val question: String)
data class AnswerFaqRequest(val answer: String)
data class CreateJournalEntryRequest(val content: String)
data class UpdateJournalEntryRequest(val content: String)

// dto/LogBookResponse.kt
data class LogBookFaqResponse(
    val id: UUID,
    val planId: UUID,
    val question: String,
    val askedById: UUID,
    val answer: String?,
    val answeredById: UUID?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class LogBookJournalEntryResponse(
    val id: UUID,
    val planId: UUID,
    val userId: UUID,
    val pageNumber: Int,
    val content: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)
```

### Mapper

```kotlin
// mapper/LogBookMapper.kt
object LogBookMapper {
    fun fromClient(faq: ClientLogBookFaq): LogBookFaq = ...
    fun fromClient(entry: ClientLogBookJournalEntry): LogBookJournalEntry = ...
    fun toResponse(faq: LogBookFaq): LogBookFaqResponse = ...
    fun toResponse(entry: LogBookJournalEntry): LogBookJournalEntryResponse = ...
}
```

### Service

```kotlin
// service/LogBookService.kt
class LogBookService(
    logBookClient: LogBookClient,
    planRoleAuthorizer: PlanRoleAuthorizer,
) {
    private val askFaq = AskFaqAction(logBookClient, planRoleAuthorizer)
    private val answerFaq = AnswerFaqAction(logBookClient, planRoleAuthorizer)
    private val getFaqs = GetFaqsAction(logBookClient, planRoleAuthorizer)
    private val deleteFaq = DeleteFaqAction(logBookClient, planRoleAuthorizer)
    private val createJournalEntry = CreateJournalEntryAction(logBookClient, planRoleAuthorizer)
    private val updateJournalEntry = UpdateJournalEntryAction(logBookClient, planRoleAuthorizer)
    private val getJournalEntries = GetJournalEntriesAction(logBookClient, planRoleAuthorizer)
    private val deleteJournalEntry = DeleteJournalEntryAction(logBookClient, planRoleAuthorizer)

    fun askFaq(param: AskFaqParam): Result<LogBookFaq, LogBookError> = askFaq.execute(param)
    fun answerFaq(param: AnswerFaqParam): Result<LogBookFaq, LogBookError> = answerFaq.execute(param)
    fun getFaqs(param: GetFaqsParam): Result<List<LogBookFaq>, LogBookError> = getFaqs.execute(param)
    fun deleteFaq(param: DeleteFaqParam): Result<Unit, LogBookError> = deleteFaq.execute(param)
    fun createJournalEntry(param: CreateJournalEntryParam): Result<LogBookJournalEntry, LogBookError> = createJournalEntry.execute(param)
    fun updateJournalEntry(param: UpdateJournalEntryParam): Result<LogBookJournalEntry, LogBookError> = updateJournalEntry.execute(param)
    fun getJournalEntries(param: GetJournalEntriesParam): Result<List<LogBookJournalEntry>, LogBookError> = getJournalEntries.execute(param)
    fun deleteJournalEntry(param: DeleteJournalEntryParam): Result<Unit, LogBookError> = deleteJournalEntry.execute(param)
}
```

### API Surface

| Method   | Path                                               | Description                        | Request Body                | Response                           | Auth                  |
|----------|----------------------------------------------------|------------------------------------|-----------------------------|------------------------------------|----------------------|
| `POST`   | `/api/plans/{planId}/log-book/faqs`                | Ask a new FAQ question             | `AskFaqRequest`             | `LogBookFaqResponse` (201)         | Any member           |
| `PUT`    | `/api/plans/{planId}/log-book/faqs/{faqId}/answer` | Answer a FAQ                       | `AnswerFaqRequest`          | `LogBookFaqResponse`               | OWNER, MANAGER       |
| `GET`    | `/api/plans/{planId}/log-book/faqs`                | List all FAQs for a plan           | —                           | `List<LogBookFaqResponse>`         | Any member           |
| `DELETE` | `/api/plans/{planId}/log-book/faqs/{faqId}`        | Delete a FAQ                       | —                           | 204                                | OWNER, MANAGER, asker|
| `POST`   | `/api/plans/{planId}/log-book/journal`             | Create a journal entry             | `CreateJournalEntryRequest` | `LogBookJournalEntryResponse` (201)| Any member           |
| `PUT`    | `/api/plans/{planId}/log-book/journal/{entryId}`   | Update a journal entry             | `UpdateJournalEntryRequest` | `LogBookJournalEntryResponse`      | Original author only |
| `GET`    | `/api/plans/{planId}/log-book/journal`             | List journal entries (by page order)| —                          | `List<LogBookJournalEntryResponse>`| Any member           |
| `DELETE` | `/api/plans/{planId}/log-book/journal/{entryId}`   | Delete a journal entry             | —                           | 204                                | OWNER, MANAGER, author|

### Controller

```kotlin
// controller/LogBookController.kt
@RestController
@RequestMapping("/api/plans/{planId}/log-book")
class LogBookController(
    private val logBookService: LogBookService,
    private val eventPublisher: PlanEventPublisher,
) { ... }
```

### Configuration

```kotlin
// config/LogBookClientConfig.kt
@Configuration
class LogBookClientConfig {
    @Bean
    fun logBookClient(): LogBookClient = createLogBookClient()
}

// config/LogBookServiceConfig.kt
@Configuration
class LogBookServiceConfig {
    @Bean
    fun logBookService(logBookClient: LogBookClient, planRoleAuthorizer: PlanRoleAuthorizer): LogBookService =
        LogBookService(logBookClient, planRoleAuthorizer)
}
```

### ResultExtensions

Add to `ResultExtensions.kt`:

```kotlin
fun LogBookError.toResponseEntity(): ResponseEntity<Any> = when (this) {
    is LogBookError.NotFound -> ResponseEntity.status(404)
        .body(ApiResponse.ErrorBody("NOT_FOUND", message))
    is LogBookError.Invalid -> ResponseEntity.status(400)
        .body(ApiResponse.ErrorBody("BAD_REQUEST", message))
    is LogBookError.Forbidden -> ResponseEntity.status(403)
        .body(ApiResponse.ErrorBody("FORBIDDEN", message))
}

@JvmName("logBookResultToResponseEntity")
fun <T> Result<T, LogBookError>.toResponseEntity(
    successStatus: Int = 200,
    transform: (T) -> Any = { it as Any }
): ResponseEntity<Any> = when (this) {
    is Result.Success -> ResponseEntity.status(successStatus).body(transform(value))
    is Result.Failure -> error.toResponseEntity()
}
```

### WebSocket Events

The `LogBookController` publishes events after successful mutations:
- `eventPublisher.publishUpdate(planId, "log-book-faqs", "updated")`
- `eventPublisher.publishUpdate(planId, "log-book-journal", "updated")`

---

## Frontend Notes

The log book interactable should be positioned on a stool beside the tent in the campsite scene. It uses the existing `InteractableItem` component pattern. The UI opens a modal showing FAQs on the first "page", then each journal entry as its own flippable page ordered by `page_number`. Deleted pages are simply removed (no renumbering).

---

## PR Stack

| #  | Tag             | Title                                          | Description                                                      |
|----|-----------------|------------------------------------------------|------------------------------------------------------------------|
| 1  | `[plan]`        | feat(log-book): plan                           | This plan document                                               |
| 2  | `[db]`          | feat(log-book): db contracts                   | Schema files + migrations V025, V026                             |
| 3  | `[client]`      | feat(log-book): client contracts               | `LogBookClient` interface, params, models, factory, fake          |
| 4  | `[service]`     | feat(log-book): service contracts              | Service, actions, validations, error, DTOs, mapper, controller, config, ResultExtensions |
| 5  | `[db-impl]`     | feat(log-book): db implementation              | Migration SQL files (already complete in db contracts)            |
| 6  | `[client-impl]` | feat(log-book): client implementation          | `JdbiLogBookClient`, operations, adapters, validations            |
| 7  | `[service-impl]`| feat(log-book): service implementation         | Action implementations, controller routing                        |
| 8  | `[client-test]` | feat(log-book): client tests                   | Integration tests for `JdbiLogBookClient` with Testcontainers     |
| 9  | `[service-test]`| feat(log-book): service tests                  | Unit tests for `LogBookService` with `FakeLogBookClient`          |
| 10 | `[acceptance]`  | feat(log-book): acceptance tests               | `@SpringBootTest` + `TestRestTemplate` acceptance tests           |
| 11 | `[docs]`        | feat(log-book): documentation updates          | Update CLAUDE.md files with log-book feature docs                 |

**Note:** No `[lib]` PR needed — no pure-logic library is required for this feature.
