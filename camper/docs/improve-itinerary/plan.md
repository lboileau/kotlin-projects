# Feature Plan: improve-itinerary

## Summary

Enhance the existing itinerary feature with richer event metadata (category, estimated cost, location, end time) and a new `itinerary_event_links` child table. The API is updated with new fields on existing endpoints (no new endpoints). The frontend is overhauled from a basic timeline to a multi-column card layout with category icons, cost summaries, duration display, and inline link management. This is a non-breaking enhancement — new columns have defaults or are nullable, and API responses are additive.

## Entities

### Modified: `ItineraryEvent`

| Field | Type | Nullable | Default | Notes |
|---|---|---|---|---|
| id | UUID | no | gen_random_uuid() | PK (existing) |
| itineraryId | UUID | no | — | FK to itineraries (existing) |
| title | String | no | — | (existing) |
| description | String? | yes | — | (existing) |
| details | String? | yes | — | (existing) |
| eventAt | Instant | no | — | (existing) |
| **category** | **String** | **no** | **'other'** | **NEW — one of: travel, accommodation, activity, meal, other** |
| **estimatedCost** | **BigDecimal?** | **yes** | **null** | **NEW — cost in trip currency, >= 0** |
| **location** | **String?** | **yes** | **null** | **NEW — free text, max 500 chars** |
| **eventEndAt** | **Instant?** | **yes** | **null** | **NEW — must be after eventAt** |
| createdAt | Instant | no | now() | (existing) |
| updatedAt | Instant | no | now() | (existing) |

### New: `ItineraryEventLink`

| Field | Type | Nullable | Default | Notes |
|---|---|---|---|---|
| id | UUID | no | gen_random_uuid() | PK |
| eventId | UUID | no | — | FK to itinerary_events, CASCADE delete |
| url | String | no | — | TEXT, valid URL |
| label | String? | yes | — | VARCHAR(255), display text |
| createdAt | Instant | no | now() | — |

## API Surface

All endpoints remain at `/api/plans/{planId}/itinerary`. No new endpoints. Links are managed inline with event CRUD (nested writes).

### Updated Request/Response Shapes

#### AddEventRequest

```kotlin
data class AddEventRequest(
    val title: String,
    val description: String?,
    val details: String?,
    val eventAt: Instant,
    val category: String,              // required, default "other"
    val estimatedCost: BigDecimal?,    // optional, >= 0
    val location: String?,             // optional, max 500 chars
    val eventEndAt: Instant?,          // optional, must be after eventAt
    val links: List<LinkInput>?        // optional, max 10
)

data class LinkInput(
    val url: String,     // required, valid URL format
    val label: String?   // optional
)
```

#### UpdateEventRequest

```kotlin
data class UpdateEventRequest(
    val title: String,
    val description: String?,
    val details: String?,
    val eventAt: Instant,
    val category: String,
    val estimatedCost: BigDecimal?,
    val location: String?,
    val eventEndAt: Instant?,
    val links: List<LinkInput>?        // full replacement: delete existing, insert new
)
```

#### ItineraryEventResponse

```kotlin
data class ItineraryEventResponse(
    val id: UUID,
    val itineraryId: UUID,
    val title: String,
    val description: String?,
    val details: String?,
    val eventAt: Instant,
    val category: String,
    val estimatedCost: BigDecimal?,
    val location: String?,
    val eventEndAt: Instant?,
    val links: List<LinkResponse>,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class LinkResponse(
    val id: UUID,
    val url: String,
    val label: String?,
    val createdAt: Instant
)
```

#### ItineraryResponse

```kotlin
data class ItineraryResponse(
    val id: UUID,
    val planId: UUID,
    val events: List<ItineraryEventResponse>,
    val totalEstimatedCost: BigDecimal?,  // NEW: sum of all event costs, null if none have costs
    val createdAt: Instant,
    val updatedAt: Instant
)
```

### Validation Rules

| Rule | Layer |
|---|---|
| `title` must not be blank | Client + Service |
| `category` must be one of: travel, accommodation, activity, meal, other | Client + Service |
| `eventEndAt` must be after `eventAt` if provided | Client + Service |
| `estimatedCost` must be >= 0 if provided | Client + Service |
| `url` in links must not be blank | Client + Service |
| Max 10 links per event | Client + Service |

## Database Changes

### Migration V030: Alter itinerary_events

File: `databases/camper-db/migrations/V030__add_itinerary_event_metadata.sql`

```sql
ALTER TABLE itinerary_events ADD COLUMN IF NOT EXISTS category VARCHAR(50) NOT NULL DEFAULT 'other';
ALTER TABLE itinerary_events ADD COLUMN IF NOT EXISTS estimated_cost DECIMAL(10,2);
ALTER TABLE itinerary_events ADD COLUMN IF NOT EXISTS location VARCHAR(500);
ALTER TABLE itinerary_events ADD COLUMN IF NOT EXISTS event_end_at TIMESTAMPTZ;

ALTER TABLE itinerary_events ADD CONSTRAINT ck_itinerary_events_category
    CHECK (category IN ('travel', 'accommodation', 'activity', 'meal', 'other'));
```

Rollback: `R030__remove_itinerary_event_metadata.sql`

```sql
ALTER TABLE itinerary_events DROP CONSTRAINT IF EXISTS ck_itinerary_events_category;
ALTER TABLE itinerary_events DROP COLUMN IF EXISTS category;
ALTER TABLE itinerary_events DROP COLUMN IF EXISTS estimated_cost;
ALTER TABLE itinerary_events DROP COLUMN IF EXISTS location;
ALTER TABLE itinerary_events DROP COLUMN IF EXISTS event_end_at;
```

### Migration V031: Create itinerary_event_links

File: `databases/camper-db/migrations/V031__create_itinerary_event_links.sql`

```sql
CREATE TABLE IF NOT EXISTS itinerary_event_links (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id   UUID        NOT NULL,
    url        TEXT        NOT NULL,
    label      VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_event_links_event FOREIGN KEY (event_id) REFERENCES itinerary_events (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_event_links_event_id ON itinerary_event_links (event_id);
```

Rollback: `R031__drop_itinerary_event_links.sql`

```sql
DROP TABLE IF EXISTS itinerary_event_links CASCADE;
```

### Schema Updates

Update `databases/camper-db/schema/tables/007_itinerary_events.sql` to reflect new columns.

New file: `databases/camper-db/schema/tables/007b_itinerary_event_links.sql`

### Seed Data Updates

Update `databases/camper-db/seed/dev_seed.sql`:
- Add `category`, `estimated_cost`, `location`, `event_end_at` to existing event inserts
- Add sample `itinerary_event_links` rows

```sql
-- Updated events with new columns:
-- "Arrive at campsite" → category='travel', location='Pine Ridge Campground, Site #14'
-- "Morning hike to Eagle Peak" → category='activity', estimated_cost=0, location='Eagle Peak Trailhead', event_end_at=2026-07-11 12:00:00+00
-- "Campfire dinner" → category='meal', estimated_cost=35.00, event_end_at=2026-07-11 20:30:00+00
-- "Pack up and head home" → category='travel'

-- Sample links:
-- Eagle Peak hike → AllTrails link
-- Campfire dinner → recipe link
```

## Client Layer

### Decision: Keep links in existing `ItineraryClient`

Links are a child of events and are managed inline (nested writes). They are fetched as part of `getEvents` and written as part of `addEvent`/`updateEvent`. Adding link operations to `ItineraryClient` keeps the interface cohesive — links are not an independent resource. This follows the same pattern as the existing itinerary→events relationship within a single client.

### Model Changes

#### Modified: `ItineraryEvent` — `clients/itinerary-client/src/main/kotlin/com/acme/clients/itineraryclient/model/ItineraryEvent.kt`

```kotlin
data class ItineraryEvent(
    val id: UUID,
    val itineraryId: UUID,
    val title: String,
    val description: String?,
    val details: String?,
    val eventAt: Instant,
    val category: String,              // NEW
    val estimatedCost: BigDecimal?,    // NEW
    val location: String?,             // NEW
    val eventEndAt: Instant?,          // NEW
    val createdAt: Instant,
    val updatedAt: Instant
)
```

#### New: `ItineraryEventLink` — `clients/itinerary-client/src/main/kotlin/com/acme/clients/itineraryclient/model/ItineraryEventLink.kt`

```kotlin
data class ItineraryEventLink(
    val id: UUID,
    val eventId: UUID,
    val url: String,
    val label: String?,
    val createdAt: Instant
)
```

### Interface Changes — `api/ItineraryClient.kt`

Add two new methods:

```kotlin
interface ItineraryClient {
    // ... existing methods unchanged ...

    /** Retrieve all links for a list of events. */
    fun getLinksByEventIds(param: GetLinksByEventIdsParam): Result<List<ItineraryEventLink>, AppError>

    /** Replace all links for an event (delete existing, insert new). */
    fun replaceEventLinks(param: ReplaceEventLinksParam): Result<List<ItineraryEventLink>, AppError>
}
```

### New Param Objects — `api/ItineraryClientParams.kt`

```kotlin
/** Parameter for retrieving links by event IDs. */
data class GetLinksByEventIdsParam(val eventIds: List<UUID>)

/** Input for a single link to create. */
data class LinkInput(val url: String, val label: String?)

/** Parameter for replacing all links on an event. */
data class ReplaceEventLinksParam(val eventId: UUID, val links: List<LinkInput>)
```

### Modified Param Objects — `api/ItineraryClientParams.kt`

```kotlin
data class AddEventParam(
    val itineraryId: UUID,
    val title: String,
    val description: String?,
    val details: String?,
    val eventAt: Instant,
    val category: String,              // NEW
    val estimatedCost: BigDecimal?,    // NEW
    val location: String?,             // NEW
    val eventEndAt: Instant?           // NEW
)

data class UpdateEventParam(
    val id: UUID,
    val title: String,
    val description: String?,
    val details: String?,
    val eventAt: Instant,
    val category: String,              // NEW
    val estimatedCost: BigDecimal?,    // NEW
    val location: String?,             // NEW
    val eventEndAt: Instant?           // NEW
)
```

### New Row Adapter — `internal/adapters/ItineraryEventLinkRowAdapter.kt`

```kotlin
object ItineraryEventLinkRowAdapter {
    fun fromResultSet(rs: ResultSet): ItineraryEventLink = ItineraryEventLink(
        id = rs.getObject("id", UUID::class.java),
        eventId = rs.getObject("event_id", UUID::class.java),
        url = rs.getString("url"),
        label = rs.getString("label"),
        createdAt = rs.getTimestamp("created_at").toInstant()
    )
}
```

### Modified Row Adapter — `internal/adapters/ItineraryEventRowAdapter.kt`

Add `category`, `estimatedCost`, `location`, `eventEndAt` fields:

```kotlin
object ItineraryEventRowAdapter {
    fun fromResultSet(rs: ResultSet): ItineraryEvent = ItineraryEvent(
        // ... existing fields ...
        category = rs.getString("category"),
        estimatedCost = rs.getBigDecimal("estimated_cost"),
        location = rs.getString("location"),
        eventEndAt = rs.getTimestamp("event_end_at")?.toInstant(),
        // ... existing timestamp fields ...
    )
}
```

### New Operations

#### `internal/operations/GetLinksByEventIds.kt`

Queries `SELECT * FROM itinerary_event_links WHERE event_id IN (<ids>) ORDER BY created_at ASC`. Returns empty list if no event IDs provided.

#### `internal/operations/ReplaceEventLinks.kt`

In a single transaction:
1. `DELETE FROM itinerary_event_links WHERE event_id = :eventId`
2. For each link in params, `INSERT INTO itinerary_event_links (id, event_id, url, label, created_at)` with new UUID and `Instant.now()`
3. Returns the newly inserted links

Validates via `ValidateReplaceEventLinks` first.

### Modified Operations

#### `AddItineraryEvent` — add `category`, `estimated_cost`, `location`, `event_end_at` columns to INSERT and to the returned entity.

#### `UpdateItineraryEvent` — add `category`, `estimated_cost`, `location`, `event_end_at` columns to UPDATE SET clause and to the returned entity.

#### `GetItineraryEvents` — add `category`, `estimated_cost`, `location`, `event_end_at` to SELECT columns.

### New Validations

#### `internal/validations/ValidateReplaceEventLinks.kt`

- If `links` is empty, return `success(Unit)` (deleting all links is valid)
- If `links.size > 10`, return `ValidationError("links", "maximum 10 links per event")`
- If any `link.url` is blank, return `ValidationError("url", "must not be blank")`

### Modified Validations

#### `ValidateAddEvent` — add:
- `category` must be one of: travel, accommodation, activity, meal, other
- `estimatedCost` must be >= 0 if provided
- `eventEndAt` must be after `eventAt` if provided

#### `ValidateUpdateEvent` — same new validations as above.

### JdbiItineraryClient Changes — `internal/JdbiItineraryClient.kt`

Add `getLinksByEventIds` and `replaceEventLinks` operations delegating to new operation classes.

### FakeItineraryClient Changes — `testFixtures/fake/FakeItineraryClient.kt`

- Add `linkStore: ConcurrentHashMap<UUID, ItineraryEventLink>`
- Update `ItineraryEvent` construction in `addEvent`/`updateEvent` to include new fields
- Implement `getLinksByEventIds`: filter linkStore by eventIds
- Implement `replaceEventLinks`: remove existing links for eventId, add new ones
- Update `deleteEvent` to also remove links for the event
- Update `delete` (itinerary) to also remove links for all events
- Add `seedLink(vararg entities: ItineraryEventLink)` helper
- Update `reset()` to also clear `linkStore`

## Service Layer

### Model Changes

#### Modified: `model/ItineraryEvent.kt`

```kotlin
data class ItineraryEvent(
    val id: UUID,
    val itineraryId: UUID,
    val title: String,
    val description: String?,
    val details: String?,
    val eventAt: Instant,
    val category: String,
    val estimatedCost: BigDecimal?,
    val location: String?,
    val eventEndAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant
)
```

#### New: `model/ItineraryEventLink.kt`

```kotlin
data class ItineraryEventLink(
    val id: UUID,
    val eventId: UUID,
    val url: String,
    val label: String?,
    val createdAt: Instant
)
```

### DTO Changes — `dto/ItineraryDtos.kt`

```kotlin
data class LinkInput(
    val url: String,
    val label: String?
)

data class LinkResponse(
    val id: UUID,
    val url: String,
    val label: String?,
    val createdAt: Instant
)

data class AddEventRequest(
    val title: String,
    val description: String?,
    val details: String?,
    val eventAt: Instant,
    val category: String,
    val estimatedCost: BigDecimal?,
    val location: String?,
    val eventEndAt: Instant?,
    val links: List<LinkInput>?
)

data class UpdateEventRequest(
    val title: String,
    val description: String?,
    val details: String?,
    val eventAt: Instant,
    val category: String,
    val estimatedCost: BigDecimal?,
    val location: String?,
    val eventEndAt: Instant?,
    val links: List<LinkInput>?
)

data class ItineraryEventResponse(
    val id: UUID,
    val itineraryId: UUID,
    val title: String,
    val description: String?,
    val details: String?,
    val eventAt: Instant,
    val category: String,
    val estimatedCost: BigDecimal?,
    val location: String?,
    val eventEndAt: Instant?,
    val links: List<LinkResponse>,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class ItineraryResponse(
    val id: UUID,
    val planId: UUID,
    val events: List<ItineraryEventResponse>,
    val totalEstimatedCost: BigDecimal?,
    val createdAt: Instant,
    val updatedAt: Instant
)
```

### Service Param Changes — `params/ItineraryServiceParams.kt`

```kotlin
data class AddEventParam(
    val planId: UUID,
    val title: String,
    val description: String?,
    val details: String?,
    val eventAt: Instant,
    val category: String,
    val estimatedCost: BigDecimal?,
    val location: String?,
    val eventEndAt: Instant?,
    val links: List<LinkInput>?
)

data class UpdateEventParam(
    val planId: UUID,
    val eventId: UUID,
    val title: String,
    val description: String?,
    val details: String?,
    val eventAt: Instant,
    val category: String,
    val estimatedCost: BigDecimal?,
    val location: String?,
    val eventEndAt: Instant?,
    val links: List<LinkInput>?
)

// LinkInput reuses the DTO type — no separate service-level type needed
// since it's a simple value object with no transformation
```

### Mapper Changes — `mapper/ItineraryMapper.kt`

```kotlin
object ItineraryMapper {

    fun fromClient(clientEvent: ClientItineraryEvent): ItineraryEvent = ItineraryEvent(
        // ... existing fields ...
        category = clientEvent.category,
        estimatedCost = clientEvent.estimatedCost,
        location = clientEvent.location,
        eventEndAt = clientEvent.eventEndAt,
        // ... existing timestamp fields ...
    )

    fun fromClient(clientLink: ClientItineraryEventLink): ItineraryEventLink = ItineraryEventLink(
        id = clientLink.id,
        eventId = clientLink.eventId,
        url = clientLink.url,
        label = clientLink.label,
        createdAt = clientLink.createdAt
    )

    fun toResponse(
        itinerary: Itinerary,
        events: List<ItineraryEvent>,
        linksByEventId: Map<UUID, List<ItineraryEventLink>>
    ): ItineraryResponse = ItineraryResponse(
        id = itinerary.id,
        planId = itinerary.planId,
        events = events.map { toResponse(it, linksByEventId[it.id] ?: emptyList()) },
        totalEstimatedCost = events.mapNotNull { it.estimatedCost }.takeIf { it.isNotEmpty() }?.sumOf { it },
        createdAt = itinerary.createdAt,
        updatedAt = itinerary.updatedAt
    )

    fun toResponse(event: ItineraryEvent, links: List<ItineraryEventLink>): ItineraryEventResponse =
        ItineraryEventResponse(
            // ... all event fields ...
            links = links.map { toResponse(it) },
            // ... timestamps ...
        )

    fun toResponse(link: ItineraryEventLink): LinkResponse = LinkResponse(
        id = link.id,
        url = link.url,
        label = link.label,
        createdAt = link.createdAt
    )
}
```

### Action Changes

#### `GetItineraryAction` — After fetching events, also fetch links:

```kotlin
// After fetching events...
val eventIds = events.map { it.id }
val links = if (eventIds.isNotEmpty()) {
    when (val linkResult = itineraryClient.getLinksByEventIds(
        ClientGetLinksByEventIdsParam(eventIds = eventIds)
    )) {
        is Result.Success -> linkResult.value.map { ItineraryMapper.fromClient(it) }
        is Result.Failure -> return Result.Failure(ItineraryError.fromClientError(linkResult.error))
    }
} else emptyList()

val linksByEventId = links.groupBy { it.eventId }
// Return type changes to: Triple<Itinerary, List<ItineraryEvent>, Map<UUID, List<ItineraryEventLink>>>
```

**Return type change:** `Pair<Itinerary, List<ItineraryEvent>>` → `Triple<Itinerary, List<ItineraryEvent>, Map<UUID, List<ItineraryEventLink>>>`

#### `AddEventAction` — After adding event, replace links if provided:

```kotlin
// After successfully adding event...
val links = if (!param.links.isNullOrEmpty()) {
    when (val linkResult = itineraryClient.replaceEventLinks(
        ClientReplaceEventLinksParam(
            eventId = event.id,
            links = param.links.map { ClientLinkInput(url = it.url, label = it.label) }
        )
    )) {
        is Result.Success -> linkResult.value.map { ItineraryMapper.fromClient(it) }
        is Result.Failure -> return Result.Failure(ItineraryError.fromClientError(linkResult.error))
    }
} else emptyList()
// Return type changes to: Pair<ItineraryEvent, List<ItineraryEventLink>>
```

**Return type change:** `ItineraryEvent` → `Pair<ItineraryEvent, List<ItineraryEventLink>>`

#### `UpdateEventAction` — After updating event, replace links:

```kotlin
// After successfully updating event...
val links = if (param.links != null) {
    when (val linkResult = itineraryClient.replaceEventLinks(
        ClientReplaceEventLinksParam(
            eventId = param.eventId,
            links = param.links.map { ClientLinkInput(url = it.url, label = it.label) }
        )
    )) {
        is Result.Success -> linkResult.value.map { ItineraryMapper.fromClient(it) }
        is Result.Failure -> return Result.Failure(ItineraryError.fromClientError(linkResult.error))
    }
} else {
    // links is null → don't touch existing links, fetch current ones
    when (val linkResult = itineraryClient.getLinksByEventIds(
        ClientGetLinksByEventIdsParam(eventIds = listOf(param.eventId))
    )) {
        is Result.Success -> linkResult.value.map { ItineraryMapper.fromClient(it) }
        is Result.Failure -> return Result.Failure(ItineraryError.fromClientError(linkResult.error))
    }
}
// Return type changes to: Pair<ItineraryEvent, List<ItineraryEventLink>>
```

**Return type change:** `ItineraryEvent` → `Pair<ItineraryEvent, List<ItineraryEventLink>>`

### Validation Changes

#### `ValidateAddEvent` — add:
- `category` must be one of: travel, accommodation, activity, meal, other → `ItineraryError.Invalid("category", "must be one of: travel, accommodation, activity, meal, other")`
- `estimatedCost` must be >= 0 if provided → `ItineraryError.Invalid("estimatedCost", "must be >= 0")`
- `eventEndAt` must be after `eventAt` if provided → `ItineraryError.Invalid("eventEndAt", "must be after eventAt")`
- `links` size must be <= 10 if provided → `ItineraryError.Invalid("links", "maximum 10 links per event")`
- Each `link.url` must not be blank → `ItineraryError.Invalid("url", "must not be blank")`

#### `ValidateUpdateEvent` — same new validations.

### Controller Changes — `controller/ItineraryController.kt`

Update `addEvent` to pass new fields (category, estimatedCost, location, eventEndAt, links) from request to service param.

Update `updateEvent` similarly.

Update `getItinerary` response mapping to use new `toResponse(itinerary, events, linksByEventId)` signature.

Update `addEvent`/`updateEvent` response mapping to use `toResponse(event, links)`.

### ItineraryService Changes — `service/ItineraryService.kt`

No structural changes. Return types update to match action return type changes. The service facade delegates as before.

## Webapp

### TypeScript Type Changes — `webapp/src/api/client.ts`

```typescript
interface ItineraryEvent {
  id: string;
  itineraryId: string;
  title: string;
  description: string | null;
  details: string | null;
  eventAt: string;
  category: string;              // NEW
  estimatedCost: number | null;  // NEW
  location: string | null;       // NEW
  eventEndAt: string | null;     // NEW
  links: LinkResponse[];         // NEW
  createdAt: string;
  updatedAt: string;
}

interface LinkResponse {
  id: string;
  url: string;
  label: string | null;
  createdAt: string;
}

interface LinkInput {
  url: string;
  label: string | null;
}

interface Itinerary {
  id: string;
  planId: string;
  events: ItineraryEvent[];
  totalEstimatedCost: number | null;  // NEW
  createdAt: string;
  updatedAt: string;
}
```

Update `addEvent` and `updateEvent` API methods to include new fields in request body.

### ItineraryModal.tsx Changes

#### Form State

Add to form state:
- `category: string` (default 'other')
- `estimatedCost: string` (empty string for null — input as text, parse to number)
- `location: string` (empty string for null)
- `eventEndAt: string` (empty string for null — datetime-local input)
- `links: Array<{ url: string, label: string }>` (default empty array)

#### Form UI

1. **Category selector** — Icon-based toggle group with 5 options:
   - travel (car icon), accommodation (house), activity (hiking), meal (utensils), other (pin)
   - Pre-selected: 'other' for new events, event's category for edits

2. **Location input** — Text input below category, optional, placeholder "Location"

3. **End time input** — Datetime-local input next to the existing eventAt, optional, labeled "End time"

4. **Estimated cost input** — Number input with step="0.01", optional, placeholder "Estimated cost"

5. **Link management** — Below the details textarea:
   - List of URL + label pairs with remove button
   - "Add link" button that appends a new empty row
   - Max 10 links (hide "Add link" when at limit)

6. **Date picker auto-select** — When opening add form, set `eventAt` to the latest date from existing events (time set to 12:00)

#### Card Layout (read view)

Replace simple timeline with card-based layout per day:

```
┌─── Day Header: "Fri, July 10" ──── Day Cost: $35.00 ───┐
│                                                          │
│  ┌────────────────────────────────────────────────┐     │
│  │ 🚗  3:00 PM          Arrive at campsite  $—   │     │
│  │      Pine Ridge Campground, Site #14           │     │
│  │      ▸ Set up tents and organize...            │     │
│  └────────────────────────────────────────────────┘     │
│                                                          │
│  ┌────────────────────────────────────────────────┐     │
│  │ 🥾  8:00 AM – 12:00 PM  Morning hike     $0   │     │
│  │      Eagle Peak Trailhead                      │     │
│  │      ▸ A moderate 5-mile loop trail...         │     │
│  │      🔗 AllTrails                              │     │
│  └────────────────────────────────────────────────┘     │
│                                                          │
└──────────────────────────────────────────────────────────┘
                             Trip Total: $35.00
```

Each card shows:
- **Left**: Category icon + time range (start – end if applicable)
- **Center**: Title + location (if set) + description preview (truncated)
- **Right**: Estimated cost (or dash if none)
- **Expandable**: Full description, details (in note box), links as clickable chips
- **Owner actions**: Edit/delete buttons (revealed on hover, same as current pattern)

#### Cost Summary

- Per-day subtotal at bottom of each day group
- Trip total in the modal footer (from `totalEstimatedCost` response field)
- Display "—" when no costs are set

#### Category Icons

Map categories to simple emoji/text icons:
- `travel` → 🚗
- `accommodation` → 🏠
- `activity` → 🥾
- `meal` → 🍽️
- `other` → 📌

## PR Stack

### 1. [plan] feat(itinerary): improve-itinerary plan
- `camper/docs/improve-itinerary/plan.md`

### 2. [db] feat(itinerary): database migrations for event metadata and links
- `databases/camper-db/migrations/V030__add_itinerary_event_metadata.sql`
- `databases/camper-db/migrations/V031__create_itinerary_event_links.sql`
- `databases/camper-db/migrations/rollback/R030__remove_itinerary_event_metadata.sql`
- `databases/camper-db/migrations/rollback/R031__drop_itinerary_event_links.sql`
- `databases/camper-db/schema/tables/007_itinerary_events.sql` (updated)
- `databases/camper-db/schema/tables/007b_itinerary_event_links.sql` (new)
- `databases/camper-db/seed/dev_seed.sql` (updated itinerary section)
- `databases/camper-db/CLAUDE.md` (updated schema docs)

### 3. [client] feat(itinerary): client contracts for event metadata and links
- `clients/itinerary-client/src/main/kotlin/com/acme/clients/itineraryclient/model/ItineraryEvent.kt` (add fields)
- `clients/itinerary-client/src/main/kotlin/com/acme/clients/itineraryclient/model/ItineraryEventLink.kt` (new)
- `clients/itinerary-client/src/main/kotlin/com/acme/clients/itineraryclient/api/ItineraryClient.kt` (add methods)
- `clients/itinerary-client/src/main/kotlin/com/acme/clients/itineraryclient/api/ItineraryClientParams.kt` (add/modify params)

### 4. [service] feat(itinerary): service contracts for event metadata and links
- `services/camper-service/src/main/kotlin/.../itinerary/model/ItineraryEvent.kt` (add fields)
- `services/camper-service/src/main/kotlin/.../itinerary/model/ItineraryEventLink.kt` (new)
- `services/camper-service/src/main/kotlin/.../itinerary/dto/ItineraryDtos.kt` (update all DTOs)
- `services/camper-service/src/main/kotlin/.../itinerary/params/ItineraryServiceParams.kt` (add fields)
- `services/camper-service/src/main/kotlin/.../itinerary/error/ItineraryError.kt` (no changes needed)

### 5. [client-impl] feat(itinerary): client implementation for event metadata and links
- `clients/itinerary-client/src/main/kotlin/.../internal/adapters/ItineraryEventRowAdapter.kt` (add fields)
- `clients/itinerary-client/src/main/kotlin/.../internal/adapters/ItineraryEventLinkRowAdapter.kt` (new)
- `clients/itinerary-client/src/main/kotlin/.../internal/operations/AddItineraryEvent.kt` (add columns)
- `clients/itinerary-client/src/main/kotlin/.../internal/operations/UpdateItineraryEvent.kt` (add columns)
- `clients/itinerary-client/src/main/kotlin/.../internal/operations/GetItineraryEvents.kt` (add columns)
- `clients/itinerary-client/src/main/kotlin/.../internal/operations/GetLinksByEventIds.kt` (new)
- `clients/itinerary-client/src/main/kotlin/.../internal/operations/ReplaceEventLinks.kt` (new)
- `clients/itinerary-client/src/main/kotlin/.../internal/validations/ValidateAddEvent.kt` (add validations)
- `clients/itinerary-client/src/main/kotlin/.../internal/validations/ValidateUpdateEvent.kt` (add validations)
- `clients/itinerary-client/src/main/kotlin/.../internal/validations/ValidateReplaceEventLinks.kt` (new)
- `clients/itinerary-client/src/main/kotlin/.../internal/JdbiItineraryClient.kt` (add operations)
- `clients/itinerary-client/src/testFixtures/kotlin/.../fake/FakeItineraryClient.kt` (update)

### 6. [service-impl] feat(itinerary): service implementation for event metadata and links
- `services/camper-service/src/main/kotlin/.../itinerary/mapper/ItineraryMapper.kt` (update all mappers)
- `services/camper-service/src/main/kotlin/.../itinerary/actions/GetItineraryAction.kt` (fetch links, change return type)
- `services/camper-service/src/main/kotlin/.../itinerary/actions/AddEventAction.kt` (pass new fields, manage links)
- `services/camper-service/src/main/kotlin/.../itinerary/actions/UpdateEventAction.kt` (pass new fields, manage links)
- `services/camper-service/src/main/kotlin/.../itinerary/validations/ValidateAddEvent.kt` (add validations)
- `services/camper-service/src/main/kotlin/.../itinerary/validations/ValidateUpdateEvent.kt` (add validations)
- `services/camper-service/src/main/kotlin/.../itinerary/service/ItineraryService.kt` (update return types)
- `services/camper-service/src/main/kotlin/.../itinerary/controller/ItineraryController.kt` (pass new fields, update response mapping)
- `services/camper-service/CLAUDE.md` (update itinerary feature docs)

### 7. [client-test] feat(itinerary): client tests for event metadata and links
- `clients/itinerary-client/src/test/kotlin/.../ItineraryClientTest.kt` (add/update tests)

### 8. [service-test] feat(itinerary): service unit tests for event metadata and links
- `services/camper-service/src/test/kotlin/.../itinerary/service/ItineraryServiceTest.kt` (add/update tests)

### 9. [acceptance] feat(itinerary): acceptance tests for event metadata and links
- `services/camper-service/src/test/kotlin/.../itinerary/acceptance/fixture/ItineraryFixture.kt` (update fixture inserts)
- `services/camper-service/src/test/kotlin/.../itinerary/acceptance/ItineraryAcceptanceTest.kt` (add/update tests)

### 10. [webapp] feat(itinerary): frontend overhaul with cards, categories, costs, and links
- `webapp/src/api/client.ts` (update types and API methods)
- `webapp/src/components/ItineraryModal.tsx` (full rewrite of layout and form)
- `webapp/src/components/ItineraryModal.css` (full rewrite of styles)

### 11. [docs] feat(itinerary): documentation updates
- `camper/CLAUDE.md` (update itinerary section if needed)
- `services/camper-service/CLAUDE.md` (update itinerary feature docs — DTOs, routes, models)
- `databases/camper-db/CLAUDE.md` (update schema docs for new columns + new table)

## Open Questions

1. **Link `url` validation strictness** — Should we validate URL format beyond "not blank"? The handoff says "valid URL format" but strict URL validation can reject legitimate URLs. Recommendation: validate that it starts with `http://` or `https://` (simple prefix check), not a full RFC 3986 parse.

2. **Cost currency** — The handoff mentions "cost in the trip's currency" but there's no currency field on plans. Should we add one, or assume a single currency? Recommendation: defer currency support — just store the number. Users understand their own currency context.

3. **UpdateEvent link semantics: replace vs. null-means-keep** — The plan specifies: if `links` is `null` in the update request, existing links are preserved (no-op); if `links` is an empty list `[]`, all links are deleted; if `links` is a populated list, existing links are fully replaced. This lets clients omit `links` for a simple title-only edit without accidentally clearing them. Confirm this is the desired behavior.
