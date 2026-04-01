# Orchestrator Handoff

## Workflow
feature-build

## Project Path
/Users/louisboileau/Development/kotlin-projects-worktrees/item-packs/camper

## Feature Name
gear-pack-grouping

## Plan
to be created by architect

## Feature Description
When gear pack items are applied to a plan, they should visually group together in the gear/items UI. Items within a gear pack group remain fully editable. Users can also add new items directly into an existing gear pack group via the category dropdown.

This is a lightweight enhancement to the existing gear pack "apply" flow — no new CRUD for gear packs themselves.

## Entities

### items (existing table — modify)
Add a nullable `gear_pack_id` column:
- `gear_pack_id UUID NULL` — FK to `gear_packs(id) ON DELETE SET NULL`
- When a gear pack is applied, each created item gets `gear_pack_id` set to the pack's ID
- When a user manually creates an item and selects a gear pack as the "category", set `gear_pack_id` on that item
- When a user changes an item's category to something other than the gear pack, set `gear_pack_id = NULL` (removes it from the group)

### gear_packs (existing table — no changes)
Used read-only to look up pack names for UI labels and to populate the category dropdown.

## API Surface

### Modified endpoints

**`POST /api/plans/{planId}/items`** (create item)
- Add optional `gearPackId: UUID?` to request body
- Pass through to item client

**`PUT /api/plans/{planId}/items/{itemId}`** (update item)
- Add optional `gearPackId: UUID?` to request body
- When category changes and `gearPackId` was set, clear `gearPackId` to `null` (backend should enforce this: if the new category != the gear pack's name, set `gear_pack_id = NULL`)

**`GET /api/plans/{planId}/items`** (list items)
- Include `gearPackId` and `gearPackName` in item responses so the frontend can group them

**`POST /api/gear-packs/{id}/apply`** (apply gear pack — already exists)
- Already creates items via itemClient. Just needs to set `gear_pack_id` on each created item.

### No new endpoints needed

## Database Changes

### Migration: Add gear_pack_id to items
```sql
ALTER TABLE items ADD COLUMN gear_pack_id UUID;
ALTER TABLE items ADD CONSTRAINT fk_items_gear_pack FOREIGN KEY (gear_pack_id) REFERENCES gear_packs (id) ON DELETE SET NULL;
CREATE INDEX idx_items_gear_pack_id ON items (gear_pack_id);
```

## Special Considerations

### Webapp UI behavior
- **Grouping**: Items with the same `gear_pack_id` are grouped together visually with a light border and a light label showing the gear pack name
- **Ungrouped items**: Items with `gear_pack_id = NULL` display exactly as they do today (no border, no label)
- **Empty groups**: If all items in a gear pack group are deleted, the group simply disappears from the UI (no empty state needed)
- **Category dropdown**: When adding a new item, the category list should include existing gear packs (from the applied packs on the plan, looked up from distinct `gear_pack_id` values on current items, plus the gear pack name). Selecting a gear pack category sets `gear_pack_id` on the new item.
- **Category change removes from pack**: If a user edits an item and changes its category to something different, the item leaves the gear pack group (`gear_pack_id` cleared)
- **Items remain fully editable**: Name, quantity, packed status, delete — all work as today within a gear pack group

### Real-time updates
Existing WebSocket plan event system should cover this — item mutations already publish updates.

### Gear pack name resolution
The item response should include `gearPackName` (resolved via JOIN or lookup) so the frontend doesn't need a separate call to get pack names for labels.

## Notes
- The gear-packs feature (read-only packs + apply) already exists on branch `03-16-fix_gear-packs_retro_improvements_agent_lifecycle_and_workflow_fixes` but has NOT been merged to main yet. The orchestrator should build on top of main and include the gear pack tables (gear_packs, gear_pack_items) as prerequisites.
- The existing `ApplyGearPackAction` creates items via `itemClient.create()` — it just needs to pass `gearPackId` through.
- Keep changes minimal — this is a grouping/display enhancement, not a gear pack CRUD feature.
