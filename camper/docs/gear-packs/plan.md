# Gear Packs — Feature Plan

## Summary

Gear packs are predefined templates (e.g., "Cooking Equipment") that let users apply a bundle of items to a plan in one action. Applying a pack creates regular items via the existing item system, with optional quantity scaling based on group size. Packs are read-only templates — once applied, the created items are independent and can be edited/deleted like any other item.

## Entities

### GearPack (template definition)

| Field       | Type         | Notes                              |
|-------------|--------------|------------------------------------|
| id          | UUID         | PK, gen_random_uuid()              |
| name        | VARCHAR(100) | e.g., "Cooking Equipment"          |
| description | VARCHAR(500) | e.g., "Essential cooking gear..." |
| created_at  | TIMESTAMPTZ  | NOT NULL DEFAULT now()             |
| updated_at  | TIMESTAMPTZ  | NOT NULL DEFAULT now()             |

### GearPackItem (template item within a pack)

| Field            | Type         | Notes                                      |
|------------------|--------------|--------------------------------------------|
| id               | UUID         | PK, gen_random_uuid()                      |
| gear_pack_id     | UUID         | FK → gear_packs(id) ON DELETE CASCADE      |
| name             | VARCHAR(255) | e.g., "Cast Iron Pan"                      |
| category         | VARCHAR(50)  | Must match existing item categories         |
| default_quantity | INTEGER      | Base quantity (before scaling)              |
| scalable         | BOOLEAN      | If true, quantity scales with group size    |
| sort_order       | INTEGER      | Display order within pack                   |
| created_at       | TIMESTAMPTZ  | NOT NULL DEFAULT now()                      |
| updated_at       | TIMESTAMPTZ  | NOT NULL DEFAULT now()                      |

### Scaling Logic

When applying a pack with `groupSize`:
- **scalable = false** → `finalQuantity = defaultQuantity` (e.g., 1 cast iron pan regardless)
- **scalable = true** → `finalQuantity = defaultQuantity * groupSize` (e.g., 1 plate × 4 people = 4 plates)

## API Surface

| Method | Path                          | Description                        | Auth          |
|--------|-------------------------------|------------------------------------|---------------|
| GET    | `/api/gear-packs`             | List all available gear packs      | X-User-Id     |
| GET    | `/api/gear-packs/{id}`        | Get pack details with items        | X-User-Id     |
| POST   | `/api/gear-packs/{id}/apply`  | Apply pack to a plan (bulk create) | X-User-Id     |

### GET /api/gear-packs

**Response:** `200 OK`
```json
[
  {
    "id": "uuid",
    "name": "Cooking Equipment",
    "description": "Essential cooking gear for campfire meals.",
    "itemCount": 12,
    "createdAt": "2026-01-01T00:00:00Z",
    "updatedAt": "2026-01-01T00:00:00Z"
  }
]
```

### GET /api/gear-packs/{id}

**Response:** `200 OK`
```json
{
  "id": "uuid",
  "name": "Cooking Equipment",
  "description": "Essential cooking gear for campfire meals.",
  "items": [
    {
      "id": "uuid",
      "name": "Cast Iron Pan",
      "category": "kitchen",
      "defaultQuantity": 1,
      "scalable": false,
      "sortOrder": 1
    },
    {
      "id": "uuid",
      "name": "Plates",
      "category": "kitchen",
      "defaultQuantity": 1,
      "scalable": true,
      "sortOrder": 5
    }
  ],
  "createdAt": "2026-01-01T00:00:00Z",
  "updatedAt": "2026-01-01T00:00:00Z"
}
```

**Error:** `404` if pack not found.

### POST /api/gear-packs/{id}/apply

**Request:**
```json
{
  "planId": "uuid",
  "groupSize": 4
}
```

**Response:** `201 Created`
```json
{
  "appliedCount": 12,
  "items": [
    {
      "id": "uuid",
      "planId": "uuid",
      "userId": null,
      "name": "Cast Iron Pan",
      "category": "kitchen",
      "quantity": 1,
      "packed": false,
      "createdAt": "...",
      "updatedAt": "..."
    }
  ]
}
```

**Errors:** `404` if pack not found, `400` if planId missing or groupSize < 1, `403` if user not OWNER/MANAGER of plan.

## Database Changes

### New Tables

#### gear_packs

```sql
CREATE TABLE IF NOT EXISTS gear_packs (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500) NOT NULL DEFAULT '',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_gear_packs_name UNIQUE (name)
);
```

#### gear_pack_items

```sql
CREATE TABLE IF NOT EXISTS gear_pack_items (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    gear_pack_id     UUID         NOT NULL,
    name             VARCHAR(255) NOT NULL,
    category         VARCHAR(50)  NOT NULL,
    default_quantity INTEGER      NOT NULL DEFAULT 1,
    scalable         BOOLEAN      NOT NULL DEFAULT false,
    sort_order       INTEGER      NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT fk_gear_pack_items_gear_pack FOREIGN KEY (gear_pack_id)
        REFERENCES gear_packs (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_gear_pack_items_gear_pack_id ON gear_pack_items (gear_pack_id);
```

### Migration Files

- `V030__create_gear_packs.sql` — creates `gear_packs` table
- `V031__create_gear_pack_items.sql` — creates `gear_pack_items` table
- `V032__seed_cooking_equipment_pack.sql` — inserts the cooking equipment gear pack

### Schema Files

- `databases/camper-db/schema/tables/030_gear_packs.sql`
- `databases/camper-db/schema/tables/031_gear_pack_items.sql`

### Seed Data — Cooking Equipment Pack

```sql
-- Cooking Equipment Gear Pack
INSERT INTO gear_packs (id, name, description)
VALUES ('cc000000-0001-4000-8000-000000000001', 'Cooking Equipment', 'Essential cooking gear for campfire meals. Includes pots, pans, utensils, and tableware.')
ON CONFLICT (id) DO NOTHING;

INSERT INTO gear_pack_items (id, gear_pack_id, name, category, default_quantity, scalable, sort_order)
VALUES
    -- Cooking surfaces (non-scalable)
    ('cc000000-0001-4000-8000-000000000101', 'cc000000-0001-4000-8000-000000000001', 'Cast Iron Pan', 'kitchen', 1, false, 1),
    ('cc000000-0001-4000-8000-000000000102', 'cc000000-0001-4000-8000-000000000001', 'Grill Grate', 'kitchen', 1, false, 2),
    ('cc000000-0001-4000-8000-000000000103', 'cc000000-0001-4000-8000-000000000001', 'Large Pot', 'kitchen', 1, false, 3),
    -- Utensils (non-scalable)
    ('cc000000-0001-4000-8000-000000000104', 'cc000000-0001-4000-8000-000000000001', 'Spatula', 'kitchen', 1, false, 4),
    ('cc000000-0001-4000-8000-000000000105', 'cc000000-0001-4000-8000-000000000001', 'Tongs', 'kitchen', 1, false, 5),
    ('cc000000-0001-4000-8000-000000000106', 'cc000000-0001-4000-8000-000000000001', 'Cooking Knife', 'kitchen', 1, false, 6),
    ('cc000000-0001-4000-8000-000000000107', 'cc000000-0001-4000-8000-000000000001', 'Cutting Board', 'kitchen', 1, false, 7),
    ('cc000000-0001-4000-8000-000000000108', 'cc000000-0001-4000-8000-000000000001', 'Can Opener', 'kitchen', 1, false, 8),
    -- Tableware (scalable per person)
    ('cc000000-0001-4000-8000-000000000109', 'cc000000-0001-4000-8000-000000000001', 'Plates', 'kitchen', 1, true, 9),
    ('cc000000-0001-4000-8000-000000000110', 'cc000000-0001-4000-8000-000000000001', 'Cups', 'kitchen', 1, true, 10),
    ('cc000000-0001-4000-8000-000000000111', 'cc000000-0001-4000-8000-000000000001', 'Bowls', 'kitchen', 1, true, 11),
    ('cc000000-0001-4000-8000-000000000112', 'cc000000-0001-4000-8000-000000000001', 'Cutlery Set', 'kitchen', 1, true, 12)
ON CONFLICT (id) DO NOTHING;
```

This data also goes in `databases/camper-db/seed/dev_seed.sql` (appended).

## Client Interface

### Module: `clients/gear-pack-client`

**Package:** `com.acme.clients.gearpackclient`

Register in `settings.gradle.kts`:
```kotlin
include(":clients:gear-pack-client")
```

### Interface

```kotlin
package com.acme.clients.gearpackclient.api

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.gearpackclient.model.GearPack

interface GearPackClient {

    /** Retrieve all available gear packs (without items). */
    fun getAll(param: GetAllGearPacksParam): Result<List<GearPack>, AppError>

    /** Retrieve a gear pack by ID with its items. */
    fun getById(param: GetGearPackByIdParam): Result<GearPack, AppError>
}
```

### Param Types

```kotlin
package com.acme.clients.gearpackclient.api

/** Parameter for listing all gear packs. */
class GetAllGearPacksParam

/** Parameter for retrieving a gear pack by ID. */
data class GetGearPackByIdParam(val id: java.util.UUID)
```

### Model Types

```kotlin
package com.acme.clients.gearpackclient.model

import java.time.Instant
import java.util.UUID

data class GearPack(
    val id: UUID,
    val name: String,
    val description: String,
    val items: List<GearPackItem>,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class GearPackItem(
    val id: UUID,
    val gearPackId: UUID,
    val name: String,
    val category: String,
    val defaultQuantity: Int,
    val scalable: Boolean,
    val sortOrder: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
)
```

### Internal Operations

| Class | Param | Returns | Notes |
|-------|-------|---------|-------|
| `GetAllGearPacks` | `GetAllGearPacksParam` | `Result<List<GearPack>, AppError>` | Fetches packs without items (items list empty) |
| `GetGearPackById` | `GetGearPackByIdParam` | `Result<GearPack, AppError>` | Fetches pack + items joined, ordered by sort_order |

### Internal Validations

| Class | Param | Notes |
|-------|-------|-------|
| `ValidateGetAllGearPacks` | `GetAllGearPacksParam` | No-op (always succeeds) |
| `ValidateGetGearPackById` | `GetGearPackByIdParam` | No-op (always succeeds) |

### Row Adapters

```kotlin
package com.acme.clients.gearpackclient.internal.adapters

object GearPackRowAdapter {
    fun fromResultSet(rs: ResultSet): GearPack = GearPack(
        id = rs.getObject("id", UUID::class.java),
        name = rs.getString("name"),
        description = rs.getString("description"),
        items = emptyList(), // populated by operation
        createdAt = rs.getTimestamp("created_at").toInstant(),
        updatedAt = rs.getTimestamp("updated_at").toInstant(),
    )
}

object GearPackItemRowAdapter {
    fun fromResultSet(rs: ResultSet): GearPackItem = GearPackItem(
        id = rs.getObject("id", UUID::class.java),
        gearPackId = rs.getObject("gear_pack_id", UUID::class.java),
        name = rs.getString("name"),
        category = rs.getString("category"),
        defaultQuantity = rs.getInt("default_quantity"),
        scalable = rs.getBoolean("scalable"),
        sortOrder = rs.getInt("sort_order"),
        createdAt = rs.getTimestamp("created_at").toInstant(),
        updatedAt = rs.getTimestamp("updated_at").toInstant(),
    )
}
```

### Facade

```kotlin
package com.acme.clients.gearpackclient.internal

internal class JdbiGearPackClient(jdbi: Jdbi) : GearPackClient {
    private val getAllGearPacks = GetAllGearPacks(jdbi)
    private val getGearPackById = GetGearPackById(jdbi)

    override fun getAll(param: GetAllGearPacksParam) = getAllGearPacks.execute(param)
    override fun getById(param: GetGearPackByIdParam) = getGearPackById.execute(param)
}
```

### Factory

```kotlin
package com.acme.clients.gearpackclient

fun createGearPackClient(): GearPackClient {
    val url = System.getProperty("DB_URL") ?: System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5433/camper_db"
    val user = System.getProperty("DB_USER") ?: System.getenv("DB_USER") ?: "postgres"
    val password = System.getProperty("DB_PASSWORD") ?: System.getenv("DB_PASSWORD") ?: "postgres"
    val jdbi = Jdbi.create(url, user, password)
    return JdbiGearPackClient(jdbi)
}
```

### Fake (testFixtures)

```kotlin
package com.acme.clients.gearpackclient.fake

class FakeGearPackClient : GearPackClient {
    private val store = ConcurrentHashMap<UUID, GearPack>()

    override fun getAll(param: GetAllGearPacksParam): Result<List<GearPack>, AppError> {
        return success(store.values.sortedBy { it.name }.map { it.copy(items = emptyList()) })
    }

    override fun getById(param: GetGearPackByIdParam): Result<GearPack, AppError> {
        val pack = store[param.id] ?: return failure(NotFoundError("GearPack", param.id.toString()))
        return success(pack)
    }

    fun reset() = store.clear()

    fun seedGearPack(vararg packs: GearPack) {
        packs.forEach { store[it.id] = it }
    }
}
```

## Service Layer

### Feature: `features/gearpack/`

Located in `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/gearpack/`.

### Service Params

```kotlin
package com.acme.services.camperservice.features.gearpack.params

import java.util.UUID

data class ListGearPacksParam(val requestingUserId: UUID)

data class GetGearPackParam(val id: UUID, val requestingUserId: UUID)

data class ApplyGearPackParam(
    val gearPackId: UUID,
    val planId: UUID,
    val groupSize: Int,
    val requestingUserId: UUID,
)
```

### Service Model

```kotlin
package com.acme.services.camperservice.features.gearpack.model

import java.time.Instant
import java.util.UUID

data class GearPack(
    val id: UUID,
    val name: String,
    val description: String,
    val items: List<GearPackItem>,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class GearPackItem(
    val id: UUID,
    val name: String,
    val category: String,
    val defaultQuantity: Int,
    val scalable: Boolean,
    val sortOrder: Int,
)

data class ApplyGearPackResult(
    val appliedCount: Int,
    val items: List<AppliedItem>,
)

data class AppliedItem(
    val id: UUID,
    val planId: UUID,
    val name: String,
    val category: String,
    val quantity: Int,
    val packed: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)
```

### Error Types

```kotlin
package com.acme.services.camperservice.features.gearpack.error

import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.error.ValidationError
import java.util.UUID

sealed class GearPackError(override val message: String) : AppError {
    data class NotFound(val packId: UUID) : GearPackError("Gear pack not found: $packId")
    data class Invalid(val field: String, val reason: String) : GearPackError("Invalid $field: $reason")
    data class Forbidden(val planId: String, val userId: String) : GearPackError("User $userId is not authorized to apply gear packs to plan $planId")
    data class ApplyFailed(val packName: String, val reason: String) : GearPackError("Failed to apply gear pack '$packName': $reason")

    companion object {
        fun fromClientError(error: AppError): GearPackError = when (error) {
            is NotFoundError -> NotFound(UUID.fromString(error.id))
            is ValidationError -> Invalid(error.field, error.reason)
            else -> Invalid("unknown", error.message)
        }
    }
}
```

### Request/Response DTOs

```kotlin
package com.acme.services.camperservice.features.gearpack.dto

import java.time.Instant
import java.util.UUID

// Responses

data class GearPackSummaryResponse(
    val id: UUID,
    val name: String,
    val description: String,
    val itemCount: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class GearPackDetailResponse(
    val id: UUID,
    val name: String,
    val description: String,
    val items: List<GearPackItemResponse>,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class GearPackItemResponse(
    val id: UUID,
    val name: String,
    val category: String,
    val defaultQuantity: Int,
    val scalable: Boolean,
    val sortOrder: Int,
)

data class ApplyGearPackRequest(
    val planId: UUID,
    val groupSize: Int,
)

data class ApplyGearPackResponse(
    val appliedCount: Int,
    val items: List<AppliedItemResponse>,
)

data class AppliedItemResponse(
    val id: UUID,
    val planId: UUID,
    val userId: UUID?,
    val name: String,
    val category: String,
    val quantity: Int,
    val packed: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)
```

### Mapper

```kotlin
package com.acme.services.camperservice.features.gearpack.mapper

object GearPackMapper {
    fun fromClient(client: ClientGearPack): GearPack = ...
    fun fromClientItem(client: ClientGearPackItem): GearPackItem = ...
    fun toSummaryResponse(pack: GearPack): GearPackSummaryResponse = ...
    fun toDetailResponse(pack: GearPack): GearPackDetailResponse = ...
    fun toApplyResponse(result: ApplyGearPackResult): ApplyGearPackResponse = ...
    fun appliedItemToResponse(item: AppliedItem): AppliedItemResponse = ...
}
```

### Actions

| Action | Param | Returns | Dependencies |
|--------|-------|---------|-------------|
| `ListGearPacksAction` | `ListGearPacksParam` | `Result<List<GearPack>, GearPackError>` | `GearPackClient` |
| `GetGearPackAction` | `GetGearPackParam` | `Result<GearPack, GearPackError>` | `GearPackClient` |
| `ApplyGearPackAction` | `ApplyGearPackParam` | `Result<ApplyGearPackResult, GearPackError>` | `GearPackClient`, `ItemClient`, `PlanRoleAuthorizer` |

### Validations (1:1 with actions)

| Class | Param | Rules |
|-------|-------|-------|
| `ValidateListGearPacks` | `ListGearPacksParam` | No-op |
| `ValidateGetGearPack` | `GetGearPackParam` | No-op |
| `ValidateApplyGearPack` | `ApplyGearPackParam` | groupSize must be > 0 |

### ApplyGearPackAction — Detailed Flow

```
1. Validate param (groupSize > 0)
2. Authorize: PlanRoleAuthorizer.authorize(planId, userId, {OWNER, MANAGER})
3. Fetch gear pack by ID via GearPackClient.getById()
4. For each GearPackItem in pack:
   a. Compute finalQuantity = if (scalable) defaultQuantity * groupSize else defaultQuantity
   b. Call ItemClient.create(CreateItemParam(
        planId = param.planId,
        userId = null,           // shared gear
        name = item.name,
        category = item.category,
        quantity = finalQuantity,
        packed = false,
      ))
   c. Collect created items; if any fails, short-circuit with ApplyFailed error
5. Return ApplyGearPackResult(appliedCount, items)
```

### Service Facade

```kotlin
package com.acme.services.camperservice.features.gearpack.service

class GearPackService(
    gearPackClient: GearPackClient,
    itemClient: ItemClient,
    planRoleAuthorizer: PlanRoleAuthorizer,
) {
    private val listGearPacks = ListGearPacksAction(gearPackClient)
    private val getGearPack = GetGearPackAction(gearPackClient)
    private val applyGearPack = ApplyGearPackAction(gearPackClient, itemClient, planRoleAuthorizer)

    fun list(param: ListGearPacksParam) = listGearPacks.execute(param)
    fun getById(param: GetGearPackParam) = getGearPack.execute(param)
    fun apply(param: ApplyGearPackParam) = applyGearPack.execute(param)
}
```

### Controller

```kotlin
package com.acme.services.camperservice.features.gearpack.controller

@RestController
@RequestMapping("/api/gear-packs")
class GearPackController(
    private val gearPackService: GearPackService,
    private val eventPublisher: PlanEventPublisher,
) {
    @GetMapping
    fun list(@RequestHeader("X-User-Id") userId: UUID): ResponseEntity<Any>

    @GetMapping("/{id}")
    fun getById(@PathVariable id: UUID, @RequestHeader("X-User-Id") userId: UUID): ResponseEntity<Any>

    @PostMapping("/{id}/apply")
    fun apply(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: ApplyGearPackRequest,
    ): ResponseEntity<Any>
    // On success: eventPublisher.publishUpdate(request.planId, "items", "updated")
}
```

### Spring Configuration

```kotlin
// config/GearPackClientConfig.kt
@Configuration
class GearPackClientConfig {
    @Bean
    fun gearPackClient(): GearPackClient = createGearPackClient()
}

// config/GearPackServiceConfig.kt
@Configuration
class GearPackServiceConfig {
    @Bean
    fun gearPackService(
        gearPackClient: GearPackClient,
        itemClient: ItemClient,
        planRoleAuthorizer: PlanRoleAuthorizer,
    ): GearPackService = GearPackService(gearPackClient, itemClient, planRoleAuthorizer)
}
```

### ResultExtensions Addition

Add to `common/error/ResultExtensions.kt`:

```kotlin
fun GearPackError.toResponseEntity(): ResponseEntity<Any> = when (this) {
    is GearPackError.NotFound -> ResponseEntity.status(404)
        .body(ApiResponse.ErrorBody("NOT_FOUND", message))
    is GearPackError.Invalid -> ResponseEntity.status(400)
        .body(ApiResponse.ErrorBody("BAD_REQUEST", message))
    is GearPackError.Forbidden -> ResponseEntity.status(403)
        .body(ApiResponse.ErrorBody("FORBIDDEN", message))
    is GearPackError.ApplyFailed -> ResponseEntity.status(500)
        .body(ApiResponse.ErrorBody("INTERNAL_ERROR", message))
}

@JvmName("gearPackResultToResponseEntity")
fun <T> Result<T, GearPackError>.toResponseEntity(
    successStatus: Int = 200,
    transform: (T) -> Any = { it as Any },
): ResponseEntity<Any> = when (this) {
    is Result.Success -> ResponseEntity.status(successStatus).body(transform(value))
    is Result.Failure -> error.toResponseEntity()
}
```

## Frontend Plan

### New API Methods

Add to `webapp/src/api/client.ts`:

```typescript
// Types
export interface GearPackSummary {
  id: string;
  name: string;
  description: string;
  itemCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface GearPackDetail {
  id: string;
  name: string;
  description: string;
  items: GearPackItem[];
  createdAt: string;
  updatedAt: string;
}

export interface GearPackItem {
  id: string;
  name: string;
  category: string;
  defaultQuantity: number;
  scalable: boolean;
  sortOrder: number;
}

export interface ApplyGearPackResponse {
  appliedCount: number;
  items: Item[];
}

// API methods
getGearPacks(): Promise<GearPackSummary[]> {
  return request('/api/gear-packs');
},

getGearPack(id: string): Promise<GearPackDetail> {
  return request(`/api/gear-packs/${id}`);
},

applyGearPack(id: string, data: { planId: string; groupSize: number }): Promise<ApplyGearPackResponse> {
  return request(`/api/gear-packs/${id}/apply`, {
    method: 'POST',
    body: JSON.stringify(data),
  });
},
```

### UI Components

#### GearPacksPanel (new component in GearModal)

Add a "Gear Packs" section to the existing `GearModal.tsx` — a panel at the top of the shared gear section that shows available packs with an "Apply" action.

**Location:** `webapp/src/components/GearPacksPanel.tsx` + `GearPacksPanel.css`

**Layout:**
1. Collapsed by default — shows "Gear Packs" header with expand toggle
2. Expanded — shows a card per available pack with:
   - Pack name and description
   - Item count badge
   - "Preview" button → expands to show item list with scaled quantities
   - "Apply to Plan" button → applies with group size input
3. Group size input: number stepper, defaults to current member count

**Flow:**
1. GearModal loads gear packs on mount via `api.getGearPacks()`
2. User clicks "Preview" → fetches pack detail via `api.getGearPack(id)`
3. Preview shows items with quantities computed client-side based on group size
4. User clicks "Apply" → calls `api.applyGearPack(id, { planId, groupSize })`
5. On success, refetch items (WebSocket will also trigger refresh for other users)

**Integration with GearModal:**
- GearPacksPanel is rendered above the shared gear ChecklistSection
- Receives `planId`, `memberCount`, `canEdit` (only OWNER/MANAGER can apply)
- After applying, triggers `onItemsChanged` callback to refresh item list

### Webapp Routing

No new routes needed — gear packs are accessed within the existing GearModal on PlanPage.

## PR Stack

| # | Type | Branch | Title | Description |
|---|------|--------|-------|-------------|
| 1 | plan | `feat-gear-packs` | feat(gear-packs): plan | This plan document |
| 2 | db | `feat-gear-packs-db` | feat(gear-packs): database | Migrations, schema files, seed data for gear_packs and gear_pack_items tables |
| 3 | client | `feat-gear-packs-client` | feat(gear-packs): client contracts | GearPackClient interface, params, models, fake (testFixtures) |
| 4 | service | `feat-gear-packs-service` | feat(gear-packs): service contracts | Error types, params, DTOs, mapper stubs, action stubs, service stub, controller stub |
| 5 | client-impl | `feat-gear-packs-client-impl` | feat(gear-packs): client implementation | JDBI operations, validations, row adapters, factory, facade |
| 6 | service-impl | `feat-gear-packs-service-impl` | feat(gear-packs): service implementation | Action logic, validation logic, mapper, controller wiring, ResultExtensions, Spring config |
| 7 | webapp | `feat-gear-packs-webapp` | feat(gear-packs): webapp | GearPacksPanel component, API methods, GearModal integration |
| 8 | client-test | `feat-gear-packs-client-test` | feat(gear-packs): client tests | Integration tests for GearPackClient with Testcontainers |
| 9 | service-test | `feat-gear-packs-service-test` | feat(gear-packs): service tests | Unit tests for actions/validations with FakeGearPackClient + FakeItemClient |
| 10 | acceptance | `feat-gear-packs-acceptance` | feat(gear-packs): acceptance tests | @SpringBootTest + TestRestTemplate end-to-end tests |
| 11 | docs | `feat-gear-packs-docs` | feat(gear-packs): documentation | CLAUDE.md updates, webapp CLAUDE.md updates |

## Open Questions

None — the feature scope is well-defined and all patterns map cleanly to existing conventions.
