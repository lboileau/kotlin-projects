# Gear Sync

Automatically synchronize plan gear items based on the current assignment (tent/canoe) configuration.

## Summary

When a user closes the assignments modal, the frontend calls a gear-sync endpoint. The backend reads all assignments and members for the plan and idempotently sets the correct gear quantities. This replaces manual gear tracking for assignment-derived items.

## Behavior

The sync reads the full assignment state and writes the correct gear:

| Assignment State | Gear Item | Category | Quantity |
|------------------|-----------|----------|----------|
| N tent assignments | "Tents" | `camp` | N |
| M canoe assignments | "Canoes" | `canoe` | M |
| P total canoe members | "Paddles" | `canoe` | P |
| P total canoe members | "Life Jackets" | `canoe` | P |

- If quantity > 0: create item if missing, update quantity if changed
- If quantity = 0: delete item if it exists
- Items are plan-level (shared gear), not personal
- The sync is idempotent — calling it multiple times produces the same result

## API Surface

| Method | Path | Description | Request | Response |
|--------|------|-------------|---------|----------|
| POST | `/api/plans/{planId}/gear-sync` | Sync gear from assignments | — | 200 with synced item summary |

**Response:**
```json
{
  "items": [
    { "name": "Tents", "category": "camp", "quantity": 2 },
    { "name": "Canoes", "category": "canoe", "quantity": 1 },
    { "name": "Paddles", "category": "canoe", "quantity": 3 },
    { "name": "Life Jackets", "category": "canoe", "quantity": 3 }
  ]
}
```

## Frontend Integration

- The `AssignmentsModal` component calls `POST /api/plans/{planId}/gear-sync` on modal close
- No call on individual assignment mutations — only on modal dismiss

## Service Layer

### GearSyncAction

Single action that orchestrates the sync:

1. Call `AssignmentClient.getByPlanId(planId)` — get all assignments
2. For each canoe assignment, call `AssignmentClient.getMembers(assignmentId)` — count members
3. Calculate target quantities:
   - Tents = count of tent assignments
   - Canoes = count of canoe assignments
   - Paddles = sum of canoe member counts
   - Life Jackets = sum of canoe member counts
4. Call `ItemClient.getByPlanId(planId)` — get current gear
5. For each gear type, find existing item by name:
   - Missing + qty > 0 → create
   - Exists + qty changed → update
   - Exists + qty = 0 → delete
   - Exists + qty unchanged → no-op
6. Return summary of synced items

### Error Handling

- Plan not found → 404
- Requester not a plan member → 403

## PR Stack

1. `feat(gear-sync): plan` — this document
2. `feat(gear-sync): service contracts` — GearSyncAction signature, DTO, controller route (501)
3. `feat(gear-sync): service implementation` — sync logic using AssignmentClient + ItemClient
4. `feat(gear-sync): frontend` — call gear-sync on AssignmentsModal close
5. `feat(gear-sync): service tests` — unit tests with fake clients
6. `feat(gear-sync): acceptance tests` — end-to-end API tests

## Open Questions

None.
