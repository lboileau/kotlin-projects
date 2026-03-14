# Orchestrator Handoff

## Workflow
feature-build

## Project Path
/Users/louisboileau/Development/kotlin-projects-worktrees/feat-camp-log-book/camper

## Feature Name
camp-log-book

## Linear Ticket
LBO-12 — FAQ / help section

## Plan
to be created by architect

## Feature Description
Add a camp log book feature to the campsite scene as a fifth clickable interactable item. The log book supports three main capabilities:

1. **FAQ / Help** — Common FAQs such as how to invite members, how meal planning works, gear checklist tips, etc. Anyone can ask questions; camp managers / plan owners can answer.
2. **General Camping Tips** — Curated content like leave no trace principles, bear safety, etc.
3. **Journal Entries** — Users can leave free-form journal entries with any text they want.

The interactable could use a question mark icon or a book icon in the campsite scene, opening a modal with the log book content.

## Entities
- FAQ entries (question + answer, authored by managers/owners)
- Journal entries (free-form text, authored by any user)
- General camping tips (curated content)

## API Surface
to be determined by architect

## Database Changes
to be determined by architect

## Special Considerations
- High priority ticket
- Must integrate with existing InteractableItem pattern in the campsite scene (tent, equipment, kitchen, map table)
- The log book should be a fifth interactable item in the campsite scene
- Role-based permissions: anyone can ask questions / write journal entries, but only camp managers / plan owners can answer FAQs

## Notes
- Git branch name from Linear: `louisboileau/lbo-12-faq-help-section`
- Current branch is already `feat-camp-log-book`
