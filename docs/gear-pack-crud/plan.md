# Gear Pack CRUD — Feature Plan

## 1. Feature Summary

Extend the existing gear pack infrastructure with full CRUD (create, update, delete) operations. Currently gear packs are read-only templates seeded via migrations. This feature lets users create their own gear packs, manage items within them, and share them globally. Authorization ensures only the creator can edit or delete their packs. Deleting a gear pack cascades by ungrouping (not deleting) any plan items that reference it. Adding items to a pack includes a name-similarity search to prevent near-duplicates, following the same pattern used by recipe ingredients.

**Linear ticket:** LBO-18 — Gear Pack Management

---

## 2. Existing Infrastructure

### Database
- **`gear_packs`** — `id`, `name` (unique), `description`, `created_at`, `updated_at`. No `created_by` column yet.
- **`gear_pack_items`** — `id`, `gear_pack_id` (FK, CASCADE), `name`, `category`, `default_quantity`, `scalable`, `sort_order`, `created_at`, `updated_at`. No uniqueness constraint on name within a pack.
- **`items`** — has `gear_pack_id UUID` (FK to `gear_packs`, ON DELETE SET NULL). Cascade ungrouping is already handled at DB level.
- **Seed data:** One "Cooking Equipment" pack with 12 items (V036).
- **Latest migration:** V037. Next available: V038.

### Client (`gear-pack-client`)
- **Interface:** `getAll(GetAllGearPacksParam)`, `getById(GetGearPackByIdParam)` — read-only.
- **Models:** `GearPack` (id, name, description, items, createdAt, updatedAt), `GearPackItem` (id, gearPackId, name, category, defaultQuantity, scalable, sortOrder, createdAt, updatedAt).
- **Fake:** `FakeGearPackClient` with in-memory `ConcurrentHashMap<UUID, GearPack>` store.

### Service (`camper-service` gear pack feature)
- **Endpoints:** `GET /api/gear-packs`, `GET /api/gear-packs/{id}`, `POST /api/gear-packs/{id}/apply`.
- **Actions:** `ListGearPacksAction`, `GetGearPackAction`, `ApplyGearPackAction`.
- **Models:** Service-layer `GearPack`, `GearPackItem`, `ApplyGearPackResult`, `AppliedItem`.
- **Error:** `GearPackError` sealed class with `NotFound`, `Invalid`, `Forbidden`, `ApplyFailed`.
- **DTOs:** `GearPackSummaryResponse` (includes `itemCount`), `GearPackDetailResponse`, `GearPackItemResponse`, `ApplyGearPackRequest/Response`.
- **Config:** `GearPackClientConfig`, `GearPackServiceConfig` — wired via `@Configuration` beans.

### Webapp
- **`GearPacksPanel.tsx`** — Collapsible panel inside GearModal. Lists packs, previews items, applies to plan. Read-only.
- **`api/client.ts`** — Types: `GearPackSummary`, `GearPackDetail`, `GearPackItem`, `ApplyGearPackResponse`. Methods: `getGearPacks()`, `getGearPack(id)`, `applyGearPack(id, data)`.

### Recipe Pattern (reference for item search)
- `FindSimilarRecipes` uses `LOWER(name) LIKE LOWER('%' || :name || '%')` for substring matching — no actual Levenshtein function.
- `IngredientClient.findByName()` uses `LOWER(name) = LOWER(:name)` for exact case-insensitive match.
- Recipe ingredient resolution flow: `CONFIRM_MATCH`, `CREATE_NEW`, `SELECT_EXISTING` — orchestrated by frontend UX.

---

## 3. Entities

### `gear_packs` (modified)

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK, DEFAULT gen_random_uuid() | existing |
| name | VARCHAR(100) | NOT NULL, UNIQUE | existing |
| description | VARCHAR(500) | NOT NULL, DEFAULT '' | existing |
| **created_by** | **UUID** | **NULLABLE, FK → users(id) ON DELETE SET NULL** | **NEW — null = system-created (immutable)** |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() | existing |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() | existing |

**Design decision:** `created_by` is nullable. `NULL` means the pack was system-seeded and cannot be edited or deleted by any user. Only packs with a non-null `created_by` are editable, and only by that user.

### `gear_pack_items` (modified)

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK | existing |
| gear_pack_id | UUID | NOT NULL, FK → gear_packs(id) CASCADE | existing |
| name | VARCHAR(255) | NOT NULL | existing |
| category | VARCHAR(50) | NOT NULL | existing |
| default_quantity | INTEGER | NOT NULL, DEFAULT 1 | existing |
| scalable | BOOLEAN | NOT NULL, DEFAULT false | existing |
| sort_order | INTEGER | NOT NULL, DEFAULT 0 | existing |
| created_at | TIMESTAMPTZ | NOT NULL | existing |
| updated_at | TIMESTAMPTZ | NOT NULL | existing |

**New constraint:** `UNIQUE (gear_pack_id, LOWER(name))` — prevents duplicate item names within the same pack (case-insensitive).

### `items` (unchanged)
- `gear_pack_id UUID` FK to `gear_packs(id)` ON DELETE SET NULL — already handles cascade ungrouping.

---

## 4. API Surface

### Existing Endpoints (unchanged)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/gear-packs` | List all gear packs (summary with itemCount) |
| GET | `/api/gear-packs/{id}` | Get gear pack detail with items |
| POST | `/api/gear-packs/{id}/apply` | Apply gear pack to a plan |

### New Endpoints

| Method | Path | Auth | Description | Request Body | Response |
|--------|------|------|-------------|-------------|----------|
| POST | `/api/gear-packs` | X-User-Id | Create a gear pack | `CreateGearPackRequest` | 201 `GearPackDetailResponse` |
| PUT | `/api/gear-packs/{id}` | X-User-Id (creator) | Update gear pack name/description | `UpdateGearPackRequest` | 200 `GearPackDetailResponse` |
| DELETE | `/api/gear-packs/{id}` | X-User-Id (creator) | Delete gear pack (cascades ungroup) | — | 204 |
| POST | `/api/gear-packs/{id}/items` | X-User-Id (creator) | Add item to gear pack | `AddGearPackItemRequest` | 201 `GearPackItemResponse` |
| PUT | `/api/gear-packs/{id}/items/{itemId}` | X-User-Id (creator) | Update item in gear pack | `UpdateGearPackItemRequest` | 200 `GearPackItemResponse` |
| DELETE | `/api/gear-packs/{id}/items/{itemId}` | X-User-Id (creator) | Remove item from gear pack | — | 204 |
| GET | `/api/gear-pack-items/search?q={query}` | X-User-Id | Search items by name (similarity) | — | 200 `List<GearPackItemSearchResult>` |

### Request/Response Shapes

```kotlin
// Requests
data class CreateGearPackRequest(
    val name: String,
    val description: String = "",
)

data class UpdateGearPackRequest(
    val name: String? = null,
    val description: String? = null,
)

data class AddGearPackItemRequest(
    val name: String,
    val category: String,
    val defaultQuantity: Int = 1,
    val scalable: Boolean = false,
)

data class UpdateGearPackItemRequest(
    val name: String? = null,
    val category: String? = null,
    val defaultQuantity: Int? = null,
    val scalable: Boolean? = null,
)

// Responses (updated)
data class GearPackSummaryResponse(
    val id: UUID,
    val name: String,
    val description: String,
    val itemCount: Int,
    val createdBy: UUID?,       // NEW
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class GearPackDetailResponse(
    val id: UUID,
    val name: String,
    val description: String,
    val items: List<GearPackItemResponse>,
    val createdBy: UUID?,       // NEW
    val createdAt: Instant,
    val updatedAt: Instant,
)

// GearPackItemResponse — unchanged

data class GearPackItemSearchResult(
    val id: UUID,
    val gearPackId: UUID,
    val gearPackName: String,
    val name: String,
    val category: String,
    val defaultQuantity: Int,
    val scalable: Boolean,
)
```

---

## 5. Database Changes

### Migration V038 — Add `created_by` to gear_packs + unique item names

```sql
-- Add created_by column (nullable — NULL means system-created/immutable)
ALTER TABLE gear_packs ADD COLUMN IF NOT EXISTS created_by UUID;

ALTER TABLE gear_packs ADD CONSTRAINT fk_gear_packs_created_by
    FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_gear_packs_created_by ON gear_packs (created_by);

-- Enforce unique item names within a pack (case-insensitive)
CREATE UNIQUE INDEX IF NOT EXISTS uq_gear_pack_items_pack_name
    ON gear_pack_items (gear_pack_id, LOWER(name));
```

### Rollback R038

```sql
DROP INDEX IF EXISTS uq_gear_pack_items_pack_name;
DROP INDEX IF EXISTS idx_gear_packs_created_by;
ALTER TABLE gear_packs DROP CONSTRAINT IF EXISTS fk_gear_packs_created_by;
ALTER TABLE gear_packs DROP COLUMN IF EXISTS created_by;
```

### Schema Updates
- `schema/tables/030_gear_packs.sql` — add `created_by UUID` column + FK + index.
- `schema/tables/031_gear_pack_items.sql` — add unique index on `(gear_pack_id, LOWER(name))`.

---

## 6. Client Interface

### Model Changes

**`GearPack`** (modified — adds `createdBy`):
```kotlin
data class GearPack(
    val id: UUID,
    val name: String,
    val description: String,
    val items: List<GearPackItem>,
    val createdBy: UUID?,    // NEW — null = system-created
    val createdAt: Instant,
    val updatedAt: Instant,
)
```

**`GearPackItem`** — unchanged.

**New model: `GearPackItemSearchResult`**:
```kotlin
data class GearPackItemSearchResult(
    val id: UUID,
    val gearPackId: UUID,
    val gearPackName: String,
    val name: String,
    val category: String,
    val defaultQuantity: Int,
    val scalable: Boolean,
)
```

### New Interface Methods

```kotlin
interface GearPackClient {
    // Existing
    fun getAll(param: GetAllGearPacksParam): Result<List<GearPack>, AppError>
    fun getById(param: GetGearPackByIdParam): Result<GearPack, AppError>

    // NEW
    /** Create a new gear pack. */
    fun create(param: CreateGearPackParam): Result<GearPack, AppError>

    /** Update an existing gear pack. Null fields are left unchanged. */
    fun update(param: UpdateGearPackParam): Result<GearPack, AppError>

    /** Delete a gear pack by ID. Returns NotFoundError if not found. */
    fun delete(param: DeleteGearPackParam): Result<Unit, AppError>

    /** Add an item to a gear pack. */
    fun addItem(param: AddGearPackItemParam): Result<GearPackItem, AppError>

    /** Update an item within a gear pack. Null fields are left unchanged. */
    fun updateItem(param: UpdateGearPackItemParam): Result<GearPackItem, AppError>

    /** Remove an item from a gear pack. */
    fun removeItem(param: RemoveGearPackItemParam): Result<Unit, AppError>

    /** Search gear pack items by name (case-insensitive substring match). */
    fun searchItems(param: SearchGearPackItemsParam): Result<List<GearPackItemSearchResult>, AppError>
}
```

### New Parameter Objects

```kotlin
data class CreateGearPackParam(
    val name: String,
    val description: String = "",
    val createdBy: UUID,
)

data class UpdateGearPackParam(
    val id: UUID,
    val name: String? = null,
    val description: String? = null,
)

data class DeleteGearPackParam(val id: UUID)

data class AddGearPackItemParam(
    val gearPackId: UUID,
    val name: String,
    val category: String,
    val defaultQuantity: Int = 1,
    val scalable: Boolean = false,
)

data class UpdateGearPackItemParam(
    val id: UUID,
    val gearPackId: UUID,
    val name: String? = null,
    val category: String? = null,
    val defaultQuantity: Int? = null,
    val scalable: Boolean? = null,
)

data class RemoveGearPackItemParam(
    val id: UUID,
    val gearPackId: UUID,
)

data class SearchGearPackItemsParam(val query: String)
```

### New Operations (1:1 with interface methods)

| Operation Class | Validation Class | Notes |
|----------------|-----------------|-------|
| `CreateGearPack` | `ValidateCreateGearPack` | Validates name not blank, length ≤ 100, description ≤ 500. Catches unique constraint violation → `ConflictError`. |
| `UpdateGearPack` | `ValidateUpdateGearPack` | Validates name length if provided. Gets existing first (→ `NotFoundError`). Catches unique constraint → `ConflictError`. |
| `DeleteGearPack` | — | Returns `NotFoundError` if 0 rows affected. |
| `AddGearPackItem` | `ValidateAddGearPackItem` | Validates name not blank, category not blank, defaultQuantity ≥ 1. Assigns `sort_order = MAX(sort_order) + 1`. Catches unique constraint → `ConflictError`. |
| `UpdateGearPackItem` | `ValidateUpdateGearPackItem` | Validates field lengths if provided. Gets existing first. Catches unique constraint → `ConflictError`. |
| `RemoveGearPackItem` | — | Returns `NotFoundError` if 0 rows affected. |
| `SearchGearPackItems` | `ValidateSearchGearPackItems` | Validates query not blank. SQL: `LOWER(gpi.name) LIKE LOWER('%' \|\| :query \|\| '%')` joined with `gear_packs` to get pack name. |

### Adapter Changes

**`GearPackRowAdapter`** — add `createdBy` field:
```kotlin
fun fromResultSet(rs: ResultSet): GearPack = GearPack(
    // ... existing fields ...
    createdBy = rs.getObject("created_by", UUID::class.java),  // NEW (nullable)
    // ...
)
```

**New adapter: `GearPackItemSearchResultAdapter`**:
```kotlin
object GearPackItemSearchResultAdapter {
    fun fromResultSet(rs: ResultSet): GearPackItemSearchResult = GearPackItemSearchResult(
        id = rs.getObject("id", UUID::class.java),
        gearPackId = rs.getObject("gear_pack_id", UUID::class.java),
        gearPackName = rs.getString("gear_pack_name"),
        name = rs.getString("name"),
        category = rs.getString("category"),
        defaultQuantity = rs.getInt("default_quantity"),
        scalable = rs.getBoolean("scalable"),
    )
}
```

### Fake Changes

`FakeGearPackClient` must implement all new methods. Key behaviors:
- `create` — add to store, check name uniqueness (case-insensitive), return `ConflictError` on duplicate.
- `update` — find in store, update non-null fields, check name uniqueness.
- `delete` — remove from store, return `NotFoundError` if not found.
- `addItem` — add to pack's items list, check name uniqueness within pack, assign sort_order.
- `updateItem` — find item in pack, update non-null fields, check name uniqueness within pack.
- `removeItem` — remove from pack's items, return `NotFoundError` if not found.
- `searchItems` — filter all items across all packs by case-insensitive substring match.

### Cascade Impact of `GearPack.createdBy` Addition

Adding `createdBy` to `GearPack` is a **breaking constructor change**. All call sites must be updated:

| File | Reason |
|------|--------|
| `gear-pack-client/.../model/GearPack.kt` | Model definition |
| `gear-pack-client/.../adapters/GearPackRowAdapter.kt` | Row mapping |
| `gear-pack-client/.../operations/GetAllGearPacks.kt` | Query needs `created_by` column |
| `gear-pack-client/.../operations/GetGearPackById.kt` | Query needs `created_by` column |
| `gear-pack-client/.../fake/FakeGearPackClient.kt` | Store and seed method |
| `gear-pack-client/.../GearPackClientIntegrationTest.kt` | `insertPack` helper, assertions |
| `camper-service/.../gearpack/model/GearPack.kt` | Service model |
| `camper-service/.../gearpack/mapper/GearPackMapper.kt` | `fromClient()` mapping |
| `camper-service/.../gearpack/dto/GearPackResponse.kt` | Summary + detail responses |
| `camper-service/.../gearpack/acceptance/fixture/GearPackFixture.kt` | Test fixture |
| `camper-service/.../gearpack/service/GearPackServiceTest.kt` | Test data construction |
| `camper-service/.../gearpack/acceptance/GearPackAcceptanceTest.kt` | Assertions on response shape |

---

## 7. Service Layer

### New Service Params

```kotlin
// In features/gearpack/params/GearPackServiceParams.kt

data class CreateGearPackParam(
    val name: String,
    val description: String,
    val requestingUserId: UUID,
)

data class UpdateGearPackParam(
    val id: UUID,
    val name: String?,
    val description: String?,
    val requestingUserId: UUID,
)

data class DeleteGearPackParam(
    val id: UUID,
    val requestingUserId: UUID,
)

data class AddGearPackItemParam(
    val gearPackId: UUID,
    val name: String,
    val category: String,
    val defaultQuantity: Int,
    val scalable: Boolean,
    val requestingUserId: UUID,
)

data class UpdateGearPackItemParam(
    val id: UUID,
    val gearPackId: UUID,
    val name: String?,
    val category: String?,
    val defaultQuantity: Int?,
    val scalable: Boolean?,
    val requestingUserId: UUID,
)

data class RemoveGearPackItemParam(
    val id: UUID,
    val gearPackId: UUID,
    val requestingUserId: UUID,
)

data class SearchGearPackItemsParam(
    val query: String,
    val requestingUserId: UUID,
)
```

### New Error Types

Add to existing `GearPackError` sealed class:

```kotlin
sealed class GearPackError(override val message: String) : AppError {
    // Existing
    data class NotFound(val packId: UUID) : GearPackError(...)
    data class Invalid(val field: String, val reason: String) : GearPackError(...)
    data class Forbidden(val planId: String, val userId: String) : GearPackError(...)
    data class ApplyFailed(val packName: String, val reason: String) : GearPackError(...)

    // NEW
    data class NotCreator(val packId: UUID, val userId: UUID) :
        GearPackError("User $userId is not the creator of gear pack $packId")
    data class SystemPack(val packId: UUID) :
        GearPackError("Gear pack $packId is a system pack and cannot be modified")
    data class DuplicateName(val name: String) :
        GearPackError("Gear pack name already exists: $name")
    data class DuplicateItemName(val name: String, val packName: String) :
        GearPackError("Item '$name' already exists in pack '$packName'")
    data class ItemNotFound(val itemId: UUID) :
        GearPackError("Gear pack item not found: $itemId")

    companion object {
        fun fromClientError(error: AppError): GearPackError = when (error) {
            is NotFoundError -> NotFound(UUID.fromString(error.id))
            is ValidationError -> Invalid(error.field, error.reason)
            is ConflictError -> DuplicateName(error.detail)  // NEW
            else -> Invalid("unknown", error.message)
        }
    }
}
```

### New Actions (1:1 with service methods)

| Action | Validation | Logic |
|--------|-----------|-------|
| `CreateGearPackAction` | `ValidateCreateGearPack` | Validate → call client.create(createdBy=userId) → map to service model |
| `UpdateGearPackAction` | `ValidateUpdateGearPack` | Validate → getById → check createdBy (NotCreator/SystemPack) → call client.update → map |
| `DeleteGearPackAction` | `ValidateDeleteGearPack` | getById → check createdBy (NotCreator/SystemPack) → call client.delete |
| `AddGearPackItemAction` | `ValidateAddGearPackItem` | Validate → getById → check createdBy → call client.addItem → map |
| `UpdateGearPackItemAction` | `ValidateUpdateGearPackItem` | Validate → getById → check createdBy → call client.updateItem → map |
| `RemoveGearPackItemAction` | `ValidateRemoveGearPackItem` | getById → check createdBy → call client.removeItem |
| `SearchGearPackItemsAction` | `ValidateSearchGearPackItems` | Validate query not blank → call client.searchItems → map |

### Authorization Pattern

All mutating actions share a common check:
```kotlin
// In each mutating action:
val pack = when (val result = gearPackClient.getById(GetGearPackByIdParam(param.gearPackId))) {
    is Result.Success -> result.value
    is Result.Failure -> return failure(GearPackError.fromClientError(result.error))
}

if (pack.createdBy == null) {
    return failure(GearPackError.SystemPack(pack.id))
}
if (pack.createdBy != param.requestingUserId) {
    return failure(GearPackError.NotCreator(pack.id, param.requestingUserId))
}
```

### Service Facade Updates

```kotlin
class GearPackService(
    gearPackClient: GearPackClient,
    itemClient: ItemClient,
    planRoleAuthorizer: PlanRoleAuthorizer,
) {
    // Existing
    fun list(param: ListGearPacksParam) = listGearPacks.execute(param)
    fun getById(param: GetGearPackParam) = getGearPack.execute(param)
    fun apply(param: ApplyGearPackParam) = applyGearPack.execute(param)

    // NEW
    fun create(param: CreateGearPackParam) = createGearPack.execute(param)
    fun update(param: UpdateGearPackParam) = updateGearPack.execute(param)
    fun delete(param: DeleteGearPackParam) = deleteGearPack.execute(param)
    fun addItem(param: AddGearPackItemParam) = addGearPackItem.execute(param)
    fun updateItem(param: UpdateGearPackItemParam) = updateGearPackItem.execute(param)
    fun removeItem(param: RemoveGearPackItemParam) = removeGearPackItem.execute(param)
    fun searchItems(param: SearchGearPackItemsParam) = searchGearPackItems.execute(param)
}
```

### Controller Routes

Add to existing `GearPackController`:

```kotlin
@PostMapping("/api/gear-packs")
fun create(@RequestHeader("X-User-Id") userId: UUID, @RequestBody request: CreateGearPackRequest): ResponseEntity<Any>

@PutMapping("/api/gear-packs/{id}")
fun update(@PathVariable id: UUID, @RequestHeader("X-User-Id") userId: UUID, @RequestBody request: UpdateGearPackRequest): ResponseEntity<Any>

@DeleteMapping("/api/gear-packs/{id}")
fun delete(@PathVariable id: UUID, @RequestHeader("X-User-Id") userId: UUID): ResponseEntity<Any>

@PostMapping("/api/gear-packs/{id}/items")
fun addItem(@PathVariable id: UUID, @RequestHeader("X-User-Id") userId: UUID, @RequestBody request: AddGearPackItemRequest): ResponseEntity<Any>

@PutMapping("/api/gear-packs/{id}/items/{itemId}")
fun updateItem(@PathVariable id: UUID, @PathVariable itemId: UUID, @RequestHeader("X-User-Id") userId: UUID, @RequestBody request: UpdateGearPackItemRequest): ResponseEntity<Any>

@DeleteMapping("/api/gear-packs/{id}/items/{itemId}")
fun removeItem(@PathVariable id: UUID, @PathVariable itemId: UUID, @RequestHeader("X-User-Id") userId: UUID): ResponseEntity<Any>

@GetMapping("/api/gear-pack-items/search")
fun searchItems(@RequestParam q: String, @RequestHeader("X-User-Id") userId: UUID): ResponseEntity<Any>
```

**Note:** The search endpoint is on a different path (`/api/gear-pack-items/search`) so it needs its own controller class or to be added to the existing controller with a different mapping base.

### Mapper Updates

Add to `GearPackMapper`:

```kotlin
object GearPackMapper {
    // Existing (update to include createdBy)
    fun fromClient(client: ClientGearPack): GearPack = GearPack(
        id = client.id,
        name = client.name,
        description = client.description,
        items = client.items.map { fromClientItem(it) },
        createdBy = client.createdBy,  // NEW
        createdAt = client.createdAt,
        updatedAt = client.updatedAt,
    )

    fun toSummaryResponse(pack: GearPack): GearPackSummaryResponse = GearPackSummaryResponse(
        // ... existing fields ...
        createdBy = pack.createdBy,  // NEW
    )

    fun toDetailResponse(pack: GearPack): GearPackDetailResponse = GearPackDetailResponse(
        // ... existing fields ...
        createdBy = pack.createdBy,  // NEW
    )

    // NEW
    fun toItemResponse(item: GearPackItem): GearPackItemResponse = GearPackItemResponse(
        id = item.id,
        name = item.name,
        category = item.category,
        defaultQuantity = item.defaultQuantity,
        scalable = item.scalable,
        sortOrder = item.sortOrder,
    )

    fun toSearchResultResponse(result: ClientGearPackItemSearchResult): GearPackItemSearchResultResponse =
        GearPackItemSearchResultResponse(
            id = result.id,
            gearPackId = result.gearPackId,
            gearPackName = result.gearPackName,
            name = result.name,
            category = result.category,
            defaultQuantity = result.defaultQuantity,
            scalable = result.scalable,
        )
}
```

### Service Model Updates

```kotlin
// In features/gearpack/model/GearPack.kt — add createdBy
data class GearPack(
    val id: UUID,
    val name: String,
    val description: String,
    val items: List<GearPackItem>,
    val createdBy: UUID?,    // NEW
    val createdAt: Instant,
    val updatedAt: Instant,
)
```

---

## 8. Webapp

### API Client Updates (`api/client.ts`)

**New types:**
```typescript
// Updated existing types — add createdBy
export interface GearPackSummary {
  id: string;
  name: string;
  description: string;
  itemCount: number;
  createdBy: string | null;  // NEW
  createdAt: string;
  updatedAt: string;
}

export interface GearPackDetail {
  id: string;
  name: string;
  description: string;
  items: GearPackItem[];
  createdBy: string | null;  // NEW
  createdAt: string;
  updatedAt: string;
}

// New types
export interface CreateGearPackRequest {
  name: string;
  description?: string;
}

export interface UpdateGearPackRequest {
  name?: string;
  description?: string;
}

export interface AddGearPackItemRequest {
  name: string;
  category: string;
  defaultQuantity?: number;
  scalable?: boolean;
}

export interface UpdateGearPackItemRequest {
  name?: string;
  category?: string;
  defaultQuantity?: number;
  scalable?: boolean;
}

export interface GearPackItemSearchResult {
  id: string;
  gearPackId: string;
  gearPackName: string;
  name: string;
  category: string;
  defaultQuantity: number;
  scalable: boolean;
}
```

**New API methods:**
```typescript
export const api = {
  // ... existing ...

  // NEW
  createGearPack(data: CreateGearPackRequest): Promise<GearPackDetail>,
  updateGearPack(id: string, data: UpdateGearPackRequest): Promise<GearPackDetail>,
  deleteGearPack(id: string): Promise<void>,
  addGearPackItem(packId: string, data: AddGearPackItemRequest): Promise<GearPackItem>,
  updateGearPackItem(packId: string, itemId: string, data: UpdateGearPackItemRequest): Promise<GearPackItem>,
  removeGearPackItem(packId: string, itemId: string): Promise<void>,
  searchGearPackItems(query: string): Promise<GearPackItemSearchResult[]>,
};
```

### UI Changes

#### GearPacksPanel.tsx Enhancements

1. **"Create Pack" button** — shown at top of expanded panel. Opens inline form for name + description.
2. **Edit/Delete buttons per pack card** — shown only when `pack.createdBy === currentUserId`. Edit opens inline edit form. Delete shows confirmation dialog.
3. **Item management view** — when a user-created pack is expanded for editing, show the item list with:
   - **Add item form** with name input that triggers search-as-you-type against `/api/gear-pack-items/search?q=...`.
   - **Suggestion dropdown** showing matching items from other packs. Selecting a suggestion copies name + category.
   - **Remove button** per item.
4. **Confirmation dialog** for delete — "This will ungroup all items from plans that use this pack. Are you sure?"

#### Props Updates

`GearPacksPanel` needs `currentUserId: string` prop to determine edit/delete visibility.

`GearPacksPanelProps`:
```typescript
interface GearPacksPanelProps {
  planId: string;
  memberCount: number;
  canEdit: boolean;
  onItemsChanged: () => void;
  currentUserId: string;  // NEW — for creator-only controls
}
```

The parent `GearModal` / `ChecklistModal` already has `currentUserId` available and can pass it through.

---

## 9. PR Stack

### PR 1 — `feat(gear-pack-crud): plan`
- `docs/gear-pack-crud/plan.md`
- `docs/gear-pack-crud/handoff.md`

### PR 2 — `feat(gear-pack-crud): database`
**Migration + schema updates for created_by and unique item names.**

Files:
- `camper/databases/camper-db/migrations/V038__gear_pack_crud.sql` (new)
- `camper/databases/camper-db/migrations/rollback/R038__gear_pack_crud.sql` (new)
- `camper/databases/camper-db/schema/tables/030_gear_packs.sql` (update: add `created_by`)
- `camper/databases/camper-db/schema/tables/031_gear_pack_items.sql` (update: add unique index)

### PR 3 — `feat(gear-pack-crud): client contracts`
**New interface methods, params, models. Update existing model + queries for `createdBy`.**

Files:
- `camper/clients/gear-pack-client/src/main/kotlin/com/acme/clients/gearpackclient/api/GearPackClient.kt` (update: add 7 new methods)
- `camper/clients/gear-pack-client/src/main/kotlin/com/acme/clients/gearpackclient/api/GearPackClientParams.kt` (update: add 7 new param classes)
- `camper/clients/gear-pack-client/src/main/kotlin/com/acme/clients/gearpackclient/model/GearPack.kt` (update: add `createdBy` to `GearPack`, add `GearPackItemSearchResult`)

### PR 4 — `feat(gear-pack-crud): service contracts`
**New DTOs, error types, params, service model updates.**

Files:
- `camper/services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/gearpack/dto/GearPackRequest.kt` (update: add 4 new request classes)
- `camper/services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/gearpack/dto/GearPackResponse.kt` (update: add `createdBy` to responses, add `GearPackItemSearchResultResponse`)
- `camper/services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/gearpack/error/GearPackError.kt` (update: add 5 new error types, update companion)
- `camper/services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/gearpack/params/GearPackServiceParams.kt` (update: add 7 new param classes)
- `camper/services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/gearpack/model/GearPack.kt` (update: add `createdBy` to service `GearPack`)

### PR 5 — `feat(gear-pack-crud): client implementation`
**Operation classes, validations, adapters, facade, fake.**

Files:
- `camper/clients/gear-pack-client/src/main/kotlin/com/acme/clients/gearpackclient/internal/JdbiGearPackClient.kt` (update: add 7 new delegations)
- `camper/clients/gear-pack-client/src/main/kotlin/com/acme/clients/gearpackclient/internal/adapters/GearPackRowAdapter.kt` (update: add `createdBy`)
- `camper/clients/gear-pack-client/src/main/kotlin/com/acme/clients/gearpackclient/internal/adapters/GearPackItemSearchResultAdapter.kt` (new)
- `camper/clients/gear-pack-client/src/main/kotlin/com/acme/clients/gearpackclient/internal/operations/CreateGearPack.kt` (new)
- `camper/clients/gear-pack-client/src/main/kotlin/com/acme/clients/gearpackclient/internal/operations/UpdateGearPack.kt` (new)
- `camper/clients/gear-pack-client/src/main/kotlin/com/acme/clients/gearpackclient/internal/operations/DeleteGearPack.kt` (new)
- `camper/clients/gear-pack-client/src/main/kotlin/com/acme/clients/gearpackclient/internal/operations/AddGearPackItem.kt` (new)
- `camper/clients/gear-pack-client/src/main/kotlin/com/acme/clients/gearpackclient/internal/operations/UpdateGearPackItem.kt` (new)
- `camper/clients/gear-pack-client/src/main/kotlin/com/acme/clients/gearpackclient/internal/operations/RemoveGearPackItem.kt` (new)
- `camper/clients/gear-pack-client/src/main/kotlin/com/acme/clients/gearpackclient/internal/operations/SearchGearPackItems.kt` (new)
- `camper/clients/gear-pack-client/src/main/kotlin/com/acme/clients/gearpackclient/internal/operations/GetAllGearPacks.kt` (update: add `created_by` to SELECT)
- `camper/clients/gear-pack-client/src/main/kotlin/com/acme/clients/gearpackclient/internal/operations/GetGearPackById.kt` (update: add `created_by` to SELECT)
- `camper/clients/gear-pack-client/src/main/kotlin/com/acme/clients/gearpackclient/internal/validations/ValidateCreateGearPack.kt` (new)
- `camper/clients/gear-pack-client/src/main/kotlin/com/acme/clients/gearpackclient/internal/validations/ValidateUpdateGearPack.kt` (new)
- `camper/clients/gear-pack-client/src/main/kotlin/com/acme/clients/gearpackclient/internal/validations/ValidateAddGearPackItem.kt` (new)
- `camper/clients/gear-pack-client/src/main/kotlin/com/acme/clients/gearpackclient/internal/validations/ValidateUpdateGearPackItem.kt` (new)
- `camper/clients/gear-pack-client/src/main/kotlin/com/acme/clients/gearpackclient/internal/validations/ValidateSearchGearPackItems.kt` (new)
- `camper/clients/gear-pack-client/src/testFixtures/kotlin/com/acme/clients/gearpackclient/fake/FakeGearPackClient.kt` (update: implement all new methods)

### PR 6 — `feat(gear-pack-crud): service implementation`
**Actions, validations, mapper, controller, config updates.**

Files:
- `camper/services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/gearpack/controller/GearPackController.kt` (update: add 7 new endpoints)
- `camper/services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/gearpack/service/GearPackService.kt` (update: add 7 new facade methods + action wiring)
- `camper/services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/gearpack/mapper/GearPackMapper.kt` (update: add `createdBy` mapping, new mappers)
- `camper/services/camper-service/src/main/kotlin/com/acme/services/camperservice/config/GearPackServiceConfig.kt` (update: no changes needed — service constructor unchanged)
- `camper/services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/gearpack/actions/CreateGearPackAction.kt` (new)
- `camper/services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/gearpack/actions/UpdateGearPackAction.kt` (new)
- `camper/services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/gearpack/actions/DeleteGearPackAction.kt` (new)
- `camper/services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/gearpack/actions/AddGearPackItemAction.kt` (new)
- `camper/services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/gearpack/actions/UpdateGearPackItemAction.kt` (new)
- `camper/services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/gearpack/actions/RemoveGearPackItemAction.kt` (new)
- `camper/services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/gearpack/actions/SearchGearPackItemsAction.kt` (new)
- `camper/services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/gearpack/validations/ValidateCreateGearPack.kt` (new)
- `camper/services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/gearpack/validations/ValidateUpdateGearPack.kt` (new)
- `camper/services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/gearpack/validations/ValidateDeleteGearPack.kt` (new)
- `camper/services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/gearpack/validations/ValidateAddGearPackItem.kt` (new)
- `camper/services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/gearpack/validations/ValidateUpdateGearPackItem.kt` (new)
- `camper/services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/gearpack/validations/ValidateRemoveGearPackItem.kt` (new)
- `camper/services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/gearpack/validations/ValidateSearchGearPackItems.kt` (new)

### PR 7 — `feat(gear-pack-crud): webapp`
**Frontend CRUD UI — create/edit/delete packs, manage items, search suggestions.**

Files:
- `camper/webapp/src/api/client.ts` (update: new types + API methods, add `createdBy` to existing types)
- `camper/webapp/src/components/GearPacksPanel.tsx` (update: add create form, edit/delete controls, item management, search)
- `camper/webapp/src/components/GearPacksPanel.css` (update: styles for new CRUD UI elements)
- `camper/webapp/src/components/GearModal.tsx` (update: pass `currentUserId` to GearPacksPanel)

### PR 8 — `feat(gear-pack-crud): client tests`
**Integration tests for all new client operations.**

Files:
- `camper/clients/gear-pack-client/src/test/kotlin/com/acme/clients/gearpackclient/GearPackClientIntegrationTest.kt` (update: add tests for create, update, delete, addItem, updateItem, removeItem, searchItems; update insertPack to include created_by)

### PR 9 — `feat(gear-pack-crud): service tests`
**Unit tests for all new service actions.**

Files:
- `camper/services/camper-service/src/test/kotlin/com/acme/services/camperservice/features/gearpack/service/GearPackServiceTest.kt` (update: add tests for create, update, delete, addItem, updateItem, removeItem, searchItems; update existing test data with createdBy)

### PR 10 — `feat(gear-pack-crud): acceptance tests`
**End-to-end tests with real DB.**

Files:
- `camper/services/camper-service/src/test/kotlin/com/acme/services/camperservice/features/gearpack/acceptance/GearPackAcceptanceTest.kt` (update: add tests for all new endpoints, authorization, cascade ungrouping, item search)
- `camper/services/camper-service/src/test/kotlin/com/acme/services/camperservice/features/gearpack/acceptance/fixture/GearPackFixture.kt` (update: add helpers for creating users, packs with created_by, items)

### PR 11 — `feat(gear-pack-crud): documentation`
**CLAUDE.md updates, retro.**

Files:
- `CLAUDE.md` (update if needed)
- `camper/CLAUDE.md` (update if needed)
- `camper/webapp/CLAUDE.md` (update: add new endpoints to API table)
- `docs/gear-pack-crud/retro.md` (new)

---

## 10. Open Questions

1. **Levenshtein vs. LIKE:** The handoff mentions "Levenshtein distance check" but the existing recipe pattern uses `LIKE` for substring matching. Should we enable PostgreSQL's `fuzzystrmatch` extension for true Levenshtein distance, or is `LIKE`-based substring matching sufficient? **Recommendation:** Start with `LIKE` (consistent with recipes), enhance to Levenshtein later if needed.

2. **Gear pack visibility:** Should user-created packs be visible to all users (globally shared) or only to the creator? The handoff says "globally managed and available to all plans." **Assumption:** All packs are globally visible; only mutations are creator-restricted.

3. **Sort order management:** When items are removed from a pack, should remaining items be re-sorted to close gaps? **Recommendation:** No — gaps are harmless and re-sorting is unnecessary complexity. New items get `MAX(sort_order) + 1`.

4. **Seed pack editability:** System-seeded packs (null `created_by`) cannot be edited by anyone. Should there be a "clone pack" feature so users can create their own copy of a system pack? **Recommendation:** Not in v1. Users can create a new pack and manually add the same items.

5. **Item update within a pack:** The handoff doesn't explicitly mention updating items (only add/remove). Should `PUT /api/gear-packs/{id}/items/{itemId}` be included? **Assumption:** Yes — it's natural to want to change an item's quantity, category, or scalable flag after adding it.

6. **Search endpoint path:** The search endpoint (`/api/gear-pack-items/search`) uses a different base path from the gear pack endpoints (`/api/gear-packs`). Should it be a query parameter on the main listing instead? **Recommendation:** Separate path is cleaner since it searches across all packs, not within one.
