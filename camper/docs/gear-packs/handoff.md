# Orchestrator Handoff

## Workflow
feature-build

## Project Path
/Users/louisboileau/Development/kotlin-projects-worktrees/feat-gear-packs/camper

## Feature Name
gear-packs

## Linear Ticket
LBO-13 — Cooking Equipment Gear Templates

## Plan
to be created by architect

## Feature Description
Add a gear pack (template/preset) system that lets users apply pre-built bundles of items to a plan in one action, instead of adding items one by one. The first pack is "Cooking Equipment" (cast iron pan, grill grate, spatula, tongs, plates, cups, bowls, cutlery set, etc.), but the system must be generic enough to support arbitrary gear packs (e.g., sleeping gear, first aid, etc.).

Key requirements:
- Gear packs are predefined templates containing a list of items (name, category, default quantity)
- Applying a gear pack to a plan creates the items in bulk via the existing item system
- Quantity scaling based on group size (e.g., plates/cups scale with number of plan members)
- The kitchen category already exists in the items system — cooking pack items should use it

## Entities
- GearPack (template definition: id, name, description, items list)
- GearPackItem (template item: name, category, default quantity, scalable flag)
- Items are created via the existing Item entity when a pack is applied

## API Surface
- `GET /api/gear-packs` — list all available gear packs
- `GET /api/gear-packs/{id}` — get gear pack details with items
- `POST /api/gear-packs/{id}/apply` — apply a gear pack to a plan (body: planId, userId, groupSize)
- to be determined by architect

## Database Changes
- New `gear_packs` table (id, name, description, created_at, updated_at)
- New `gear_pack_items` table (id, gear_pack_id FK, name, category, default_quantity, scalable, created_at, updated_at)
- Seed data for the cooking equipment pack
- Existing `items` table is reused when packs are applied (no schema change needed)

## Special Considerations
- Must be generic/extensible — cooking is the first pack but the system should support any pack type
- Quantity scaling: some items scale with group size (plates, cups, bowls, cutlery) while others don't (grill grate, spatula)
- Gear packs are read-only templates — applying one creates regular items that can then be edited/deleted independently
- The existing item client already supports batch-friendly operations (create takes individual params, but the service can loop)
- WebSocket notifications: applying a pack should trigger a plan update event so other members see the new items in real time
- Frontend: needs a UI to browse/preview packs and apply them (webapp uses React + TypeScript)

## Notes
- The existing `gearsync` feature syncs gear between plans — gear packs are orthogonal (templates vs sync)
- Item categories are free-text strings today (e.g., "kitchen") — gear pack items should use consistent category values
