# Live Updates via WebSocket

## Summary

Add STOMP-over-WebSocket support so that when any resource changes (plans, members, items, itinerary, assignments), all clients currently viewing the affected plan receive a lightweight notification. The client then refetches the relevant data via existing REST endpoints. If the user has a modal open, refetches are deferred until the modal closes.

## Architecture

### Backend

**Dependencies:** `spring-boot-starter-websocket`

**WebSocket Configuration:**
- STOMP endpoint: `/ws` (with SockJS fallback)
- Application destination prefix: `/app`
- Topic broker prefix: `/topic`

**Topic structure:**
- `/topic/plans/{planId}` — single topic per plan

**Message format:**
```json
{
  "resource": "items",
  "action": "updated"
}
```

Resource types: `plan`, `members`, `items`, `itinerary`, `assignments`
Action types: `created`, `updated`, `deleted`

**Event Publisher:**
- `PlanEventPublisher` — Spring `@Component` that injects `SimpMessagingTemplate`
- Single method: `publishUpdate(planId, resource, action)`
- Called from controllers after successful mutations

### Frontend

**Dependencies:** `@stomp/stompjs` (modern STOMP client, no SockJS needed for modern browsers — we'll include SockJS fallback on the backend for compatibility)

**Hook:** `usePlanUpdates(planId, onUpdate)` — custom React hook that:
1. Connects to `/ws` via STOMP
2. Subscribes to `/topic/plans/{planId}`
3. Calls `onUpdate(resource, action)` when a message arrives
4. Disconnects on unmount or planId change

**Modal-aware refetch in PlanPage:**
- Track `activeModal` state (already exists)
- When a WebSocket message arrives:
  - If no modal is open → refetch immediately
  - If a modal is open → queue the resource for refetch
- When a modal closes → flush the queue and refetch all queued resources

## Controllers and Their Broadcast Events

| Controller | Endpoint | Resource | Action |
|------------|----------|----------|--------|
| PlanController | `PUT /api/plans/{planId}` | `plan` | `updated` |
| PlanController | `DELETE /api/plans/{planId}` | `plan` | `deleted` |
| PlanController | `POST /api/plans/{planId}/members` | `members` | `updated` |
| PlanController | `DELETE /api/plans/{planId}/members/{memberId}` | `members` | `updated` |
| ItemController | `POST /api/items` | `items` | `updated` |
| ItemController | `PUT /api/items/{id}` | `items` | `updated` |
| ItemController | `DELETE /api/items/{id}` | `items` | `updated` |
| ItineraryController | `DELETE /api/plans/{planId}/itinerary` | `itinerary` | `updated` |
| ItineraryController | `POST /api/plans/{planId}/itinerary/events` | `itinerary` | `updated` |
| ItineraryController | `PUT /api/plans/{planId}/itinerary/events/{eventId}` | `itinerary` | `updated` |
| ItineraryController | `DELETE /api/plans/{planId}/itinerary/events/{eventId}` | `itinerary` | `updated` |
| AssignmentController | `POST /api/plans/{planId}/assignments` | `assignments` | `updated` |
| AssignmentController | `PUT /api/plans/{planId}/assignments/{assignmentId}` | `assignments` | `updated` |
| AssignmentController | `DELETE /api/plans/{planId}/assignments/{assignmentId}` | `assignments` | `updated` |
| AssignmentController | `POST .../assignments/{assignmentId}/members` | `assignments` | `updated` |
| AssignmentController | `DELETE .../assignments/{assignmentId}/members/{userId}` | `assignments` | `updated` |
| AssignmentController | `PUT .../assignments/{assignmentId}/owner` | `assignments` | `updated` |

**Note on Items:** Items use `ownerType`/`ownerId` instead of `planId` in the path. For plan-owned items, `ownerId` is the `planId`. For user-owned items within a plan, the item has a `planId` field on the model. The controller will resolve the `planId` from the item's data to broadcast to the correct topic.

**Note on Plan creation:** `POST /api/plans` does not broadcast — there's no one subscribed to a plan that doesn't exist yet. The creator's UI handles it locally.

## PR Stack

```
Stack (bottom → top):

1. [plan]           feat(live-updates): plan — description and breakdown
2. [ws-infra]       feat(live-updates): backend websocket infrastructure — dependency, config, event publisher
3. [ws-integration] feat(live-updates): backend integration — publish events from controllers
4. [fe-infra]       feat(live-updates): frontend websocket infrastructure — STOMP client, usePlanUpdates hook
5. [fe-integration] feat(live-updates): frontend integration — wire into PlanPage with modal-aware refetch
6. [tests]          feat(live-updates): tests — WebSocket integration tests
7. [docs]           feat(live-updates): update documentation
```

## Decisions

1. **No SockJS fallback** — native WebSocket only, both backend and frontend.
2. **No user filtering** — the user who made the change also receives the broadcast. Simpler, and harmless since the UI already updated.
