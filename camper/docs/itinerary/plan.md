# Feature: Itinerary

## Summary

Add itinerary support to plans. Each plan can have exactly one itinerary, which serves as a container for time-ordered events. Events have a title, optional description and details, and a datetime. Any user can access the itinerary (no membership check). The itinerary is automatically created when the first event is added, keeping the API simple.

## Entities

### itineraries

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | `UUID` | PK, `DEFAULT gen_random_uuid()` |
| `plan_id` | `UUID` | `NOT NULL`, `UNIQUE`, FK → `plans(id) ON DELETE CASCADE` |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT now()` |
| `updated_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT now()` |

Indexes: `uq_itineraries_plan_id` unique on `plan_id`
Constraints: FK to plans with cascade delete (deleting a plan removes its itinerary)

### itinerary_events

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | `UUID` | PK, `DEFAULT gen_random_uuid()` |
| `itinerary_id` | `UUID` | `NOT NULL`, FK → `itineraries(id) ON DELETE CASCADE` |
| `title` | `VARCHAR(255)` | `NOT NULL` |
| `description` | `TEXT` | nullable |
| `details` | `TEXT` | nullable |
| `event_at` | `TIMESTAMPTZ` | `NOT NULL` |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT now()` |
| `updated_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT now()` |

Indexes: `idx_itinerary_events_itinerary_id` on `itinerary_id`
Constraints: FK to itineraries with cascade delete

## API Surface

| Method | Path | Description | Request Body | Response |
|--------|------|-------------|-------------|----------|
| `GET` | `/api/plans/{planId}/itinerary` | Get itinerary with all events (ordered by `event_at`) | — | `200 { id, planId, events[], createdAt, updatedAt }` or `404` if no itinerary |
| `DELETE` | `/api/plans/{planId}/itinerary` | Delete the itinerary and all its events | — | `204` or `404` |
| `POST` | `/api/plans/{planId}/itinerary/events` | Add an event (auto-creates itinerary if needed) | `{ title, description?, details?, eventAt }` | `201 { id, itineraryId, title, description, details, eventAt, createdAt, updatedAt }` |
| `PUT` | `/api/plans/{planId}/itinerary/events/{eventId}` | Update an event | `{ title, description?, details?, eventAt }` | `200 { id, itineraryId, title, description, details, eventAt, createdAt, updatedAt }` |
| `DELETE` | `/api/plans/{planId}/itinerary/events/{eventId}` | Delete an event | — | `204` or `404` |

## Database Changes

Two new tables: `itineraries` and `itinerary_events`. See entity definitions above.

- Migration V005: `CREATE TABLE itineraries`
- Migration V006: `CREATE TABLE itinerary_events`

## Client Interface

New module: `clients/itinerary-client`

### ItineraryClient

```kotlin
interface ItineraryClient {
    fun getByPlanId(param: GetByPlanIdParam): Result<Itinerary, AppError>
    fun create(param: CreateItineraryParam): Result<Itinerary, AppError>
    fun delete(param: DeleteItineraryParam): Result<Unit, AppError>
    fun getEvents(param: GetEventsParam): Result<List<ItineraryEvent>, AppError>
    fun addEvent(param: AddEventParam): Result<ItineraryEvent, AppError>
    fun updateEvent(param: UpdateEventParam): Result<ItineraryEvent, AppError>
    fun deleteEvent(param: DeleteEventParam): Result<Unit, AppError>
}
```

### Parameter Objects

```kotlin
data class GetByPlanIdParam(val planId: UUID)
data class CreateItineraryParam(val planId: UUID)
data class DeleteItineraryParam(val planId: UUID)
data class GetEventsParam(val itineraryId: UUID)
data class AddEventParam(val itineraryId: UUID, val title: String, val description: String?, val details: String?, val eventAt: Instant)
data class UpdateEventParam(val id: UUID, val title: String, val description: String?, val details: String?, val eventAt: Instant)
data class DeleteEventParam(val id: UUID)
```

### Models

```kotlin
data class Itinerary(val id: UUID, val planId: UUID, val createdAt: Instant, val updatedAt: Instant)
data class ItineraryEvent(val id: UUID, val itineraryId: UUID, val title: String, val description: String?, val details: String?, val eventAt: Instant, val createdAt: Instant, val updatedAt: Instant)
```

## Service Layer

New feature vertical: `features/itinerary/`

### Actions

| Action | Description |
|--------|-------------|
| `GetItineraryAction` | Get itinerary by planId, include events ordered by `event_at` |
| `DeleteItineraryAction` | Delete itinerary by planId |
| `AddEventAction` | Auto-create itinerary if needed, then add event |
| `UpdateEventAction` | Update event fields |
| `DeleteEventAction` | Delete a single event |

### Error Types

```kotlin
sealed class ItineraryError(override val message: String) : AppError {
    data class PlanNotFound(val planId: String) : ItineraryError("Plan not found: $planId")
    data class NotFound(val planId: String) : ItineraryError("Itinerary not found for plan: $planId")
    data class EventNotFound(val eventId: String) : ItineraryError("Itinerary event not found: $eventId")
    data class Invalid(val field: String, val reason: String) : ItineraryError("Invalid $field: $reason")
}
```

### DTOs

```kotlin
// Requests
data class AddEventRequest(val title: String, val description: String?, val details: String?, val eventAt: Instant)
data class UpdateEventRequest(val title: String, val description: String?, val details: String?, val eventAt: Instant)

// Responses
data class ItineraryResponse(val id: UUID, val planId: UUID, val events: List<ItineraryEventResponse>, val createdAt: Instant, val updatedAt: Instant)
data class ItineraryEventResponse(val id: UUID, val itineraryId: UUID, val title: String, val description: String?, val details: String?, val eventAt: Instant, val createdAt: Instant, val updatedAt: Instant)
```

## PR Stack

| # | Branch | Title | Description |
|---|--------|-------|-------------|
| 1 | `feat-itinerary-plan` | feat(itinerary): plan | This plan document |
| 2 | `feat-itinerary-db` | feat(itinerary): db contracts | Schema files and migration SQL for itineraries and itinerary_events |
| 3 | `feat-itinerary-client` | feat(itinerary): client contracts | ItineraryClient interface, params, models (no implementation) |
| 4 | `feat-itinerary-service` | feat(itinerary): service contracts | DTOs, error types, action signatures, controller routes (501s) |
| 5 | `feat-itinerary-db-impl` | feat(itinerary): db implementation | Seed data, verify migrations runnable |
| 6 | `feat-itinerary-client-impl` | feat(itinerary): client implementation | JDBI operations, adapters, factory, fake |
| 7 | `feat-itinerary-service-impl` | feat(itinerary): service implementation | Actions, service facade, controller wiring |
| 8 | `feat-itinerary-client-test` | feat(itinerary): client tests | Integration tests with Testcontainers |
| 9 | `feat-itinerary-service-test` | feat(itinerary): service tests | Unit tests with FakeItineraryClient |
| 10 | `feat-itinerary-acceptance` | feat(itinerary): acceptance tests | End-to-end API tests |
| 11 | `feat-itinerary-docs` | feat(itinerary): update documentation | Retrospective-driven doc and skill updates |

## Open Questions

None — all decisions captured above.
