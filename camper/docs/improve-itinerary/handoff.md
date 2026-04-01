# Orchestrator Handoff

## Workflow
feature-build

## Project Path
/Users/louisboileau/Development/kotlin-projects-worktrees/improve-itinerary/camper

## Feature Name
improve-itinerary

## Plan
to be created by architect

## Feature Description
Significantly improve the existing itinerary feature to support richer event data and a much better reading/editing experience. The itinerary currently stores events with title, description, details, and a timestamp. The modal UI shows a basic timeline grouped by date. This upgrade adds structured metadata to events (category, cost, location, duration, links) and overhauls the frontend for readability and efficient data entry.

Goals:
1. **Rich event data** — each event can have a category, estimated cost, location, end time (duration), and multiple related links
2. **Better overview readability** — multi-column card layout with visual hierarchy, category icons, cost summaries per day and for the whole trip
3. **Faster event creation** — date picker auto-selects the latest date in the itinerary (append-to-end behavior), category picker, inline link management
4. **Time ordering** — events within a day are already ordered by event_at; ensure this is visually clear with time range display when end time is provided

## Entities

### Modified: `itinerary_events` table
Add columns:
- `category VARCHAR(50) NOT NULL DEFAULT 'other'` — one of: travel, accommodation, activity, meal, other
- `estimated_cost DECIMAL(10,2)` — nullable, cost in the trip's currency
- `location VARCHAR(500)` — nullable, free text location name
- `event_end_at TIMESTAMPTZ` — nullable, end time for duration display

### New: `itinerary_event_links` table
- `id UUID PRIMARY KEY DEFAULT gen_random_uuid()`
- `event_id UUID NOT NULL` — FK to itinerary_events(id) ON DELETE CASCADE
- `url TEXT NOT NULL`
- `label VARCHAR(255)` — nullable, display text for the link
- `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`
- Index on event_id

### Updated models (all layers)
`ItineraryEvent` gains: category (String), estimatedCost (BigDecimal?), location (String?), eventEndAt (Instant?)
New model: `ItineraryEventLink` with id, eventId, url, label, createdAt

## API Surface

All existing endpoints remain at `/api/plans/{planId}/itinerary`. No new endpoints needed — links are managed inline with events.

### Updated request/response shapes

**AddEventRequest** — add fields:
- `category: String` (required, default "other")
- `estimatedCost: BigDecimal?` (optional)
- `location: String?` (optional)
- `eventEndAt: Instant?` (optional, must be after eventAt if provided)
- `links: List<LinkInput>?` (optional) where `LinkInput = { url: String, label: String? }`

**UpdateEventRequest** — same new fields as AddEventRequest

**ItineraryEventResponse** — add fields:
- `category: String`
- `estimatedCost: BigDecimal?`
- `location: String?`
- `eventEndAt: Instant?`
- `links: List<LinkResponse>` where `LinkResponse = { id: UUID, url: String, label: String?, createdAt: Instant }`

**ItineraryResponse** — add computed field:
- `totalEstimatedCost: BigDecimal?` (sum of all event costs, null if no events have costs)

### Validation rules
- `category` must be one of: travel, accommodation, activity, meal, other
- `eventEndAt` must be after `eventAt` if provided
- `estimatedCost` must be >= 0 if provided
- `url` in links must be a valid URL format
- Max 10 links per event

## Database Changes

### Migration: alter itinerary_events
```sql
ALTER TABLE itinerary_events ADD COLUMN category VARCHAR(50) NOT NULL DEFAULT 'other';
ALTER TABLE itinerary_events ADD COLUMN estimated_cost DECIMAL(10,2);
ALTER TABLE itinerary_events ADD COLUMN location VARCHAR(500);
ALTER TABLE itinerary_events ADD COLUMN event_end_at TIMESTAMPTZ;
```

### Migration: create itinerary_event_links
```sql
CREATE TABLE itinerary_event_links (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id   UUID        NOT NULL,
    url        TEXT        NOT NULL,
    label      VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_event_links_event FOREIGN KEY (event_id) REFERENCES itinerary_events (id) ON DELETE CASCADE
);
CREATE INDEX idx_event_links_event_id ON itinerary_event_links (event_id);
```

### Seed data
Update existing itinerary seed data to include categories, some costs, locations, and sample links for realistic demo data.

## Special Considerations

### Frontend overhaul (ItineraryModal.tsx + ItineraryModal.css)
1. **Layout** — Replace simple timeline with multi-column card layout per day:
   - Left: time range (start – end if applicable)
   - Center: category icon + title + location + description preview
   - Right: estimated cost
   - Expandable: full details, links as clickable chips, edit/delete actions
2. **Category icons** — Use simple emoji or SVG icons: travel (car/plane), accommodation (house), activity (hiking), meal (utensils), other (pin)
3. **Cost summary** — Show per-day subtotal at bottom of each day group + trip total in header/footer
4. **Date picker auto-select** — When opening the add form, pre-fill `eventAt` with the latest date from existing events (preserving time as noon or next reasonable slot)
5. **Link management in form** — Add/remove links inline with URL + optional label fields
6. **Category selector** — Icon-based toggle group or dropdown in the form
7. **Responsive** — Cards should stack cleanly on narrow viewports

### WebSocket
Existing STOMP publish pattern continues to work — no changes needed to the live update mechanism. The frontend already refetches the full itinerary on updates.

### Backward compatibility
- New DB columns have defaults or are nullable, so existing data migrates cleanly
- API responses gain new fields (additive, non-breaking for any consumers)

## Notes
- The itinerary feature already exists end-to-end. This is an enhancement, not a greenfield build.
- Follow the existing action/validation/mapper patterns in the service layer.
- Links are managed as part of event CRUD (nested writes), not as a separate top-level resource.
- The client layer needs a new `ItineraryEventLinkClient` or the link operations can be added to the existing `ItineraryClient` interface — architect should decide based on complexity.
