# Orchestrator Handoff

## Workflow
feature-build

## Project Path
/Users/louisboileau/Development/kotlin-projects-worktrees/gear-pack-crud

## Feature Name
gear-pack-crud

## Linear Ticket
LBO-18 — Gear Pack Management

## Plan
to be created by architect

## Feature Description
Extend existing gear pack support with full CRUD (create, update, delete) operations. Gear packs are globally managed and available to all plans.

### Existing Infrastructure
- Two tables: `gear_packs` and `gear_pack_items`
- A `gear-pack-client` that supports `getAll` and `getById` methods
- A camper-service gear pack feature that supports listing gear packs, fetching details, and applying a gear pack to a plan's gear list

### New Functionality
1. **Create gear packs** — anyone can create a gear pack
2. **Update gear packs** — only the creator can edit their own gear pack
3. **Delete gear packs** — only the creator can delete their own gear pack
   - Deleting a gear pack should cascade: remove `gearPackId` from any items across plans that had previously included the gear pack (ungroup items, do NOT delete plan items)
   - A confirmation warning should be shown before deletion
4. **Canonical gear pack items** — item names should be unique
   - Adding items to a pack follows a pattern similar to recipes:
     - First, user selects from existing items
     - Adding a new item does a Levenshtein distance check and suggests close matches before creating
     - If the item doesn't exist, the user can create a new item inline and add it to the pack

## Entities
- `gear_packs` — existing table (may need `created_by` or similar field for authorization)
- `gear_pack_items` — existing table (canonical items with unique names)
- Plan items — existing; need to handle `gearPackId` ungrouping on gear pack deletion

## API Surface
- `POST /gear-packs` — create a gear pack
- `PUT /gear-packs/{id}` — update a gear pack (creator only)
- `DELETE /gear-packs/{id}` — delete a gear pack (creator only, with cascade ungrouping)
- `POST /gear-packs/{id}/items` — add item to gear pack (with Levenshtein matching)
- `DELETE /gear-packs/{id}/items/{itemId}` — remove item from gear pack
- Item search/suggestion endpoint for Levenshtein matching (to be determined by architect)

## Database Changes
- Existing tables may need schema updates (e.g., `created_by` column on `gear_packs` for authorization)
- Cascade logic for ungrouping plan items when a gear pack is deleted
- To be fully determined by architect after reviewing existing schema

## Special Considerations
- Authorization: creator-only edit/delete requires tracking who created each gear pack
- Delete cascade must ungroup items (remove `gearPackId`) but NOT delete plan items
- Levenshtein distance matching for near-duplicate item names — similar to existing recipe pattern
- Confirmation UX for deletion

## Notes
- This builds on top of existing gear pack infrastructure (tables, client, service feature)
- The architect should review existing `gear-pack-client`, `gear_packs`/`gear_pack_items` tables, and camper-service gear pack feature to understand the current state before planning
