# Not Yet Assigned — Unassigned Members Display

## Summary

Show unassigned plan members at the top of each tab in the Assignments Modal. The Tents tab shows members not assigned to any tent; the Canoes tab shows members not assigned to any canoe. Only active members (with usernames) are shown — pending invitees are excluded. Avatars use a scared face expression to convey they need a group.

## Changes

**Frontend only** — no backend, database, or client changes needed.

### AssignmentsModal.tsx
- Add `scared` prop to `MiniAvatar` component (wider eyes, raised brows, open mouth)
- Compute unassigned members per tab type from existing `members` and `assignments` data
- Render an "unassigned" section at the top of each tab with scared mini avatars + names

### AssignmentsModal.css
- New `.assign-unassigned` styles for the unassigned members section

## PR Stack
1. `feat(not-yet-assigned): plan` — this document
2. `feat(not-yet-assigned): frontend — show unassigned members in assignments modal`
