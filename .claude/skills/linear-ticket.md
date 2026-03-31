---
name: linear-ticket
description: Fetch a Linear ticket's details and produce a handoff file for the orchestrator, routing to the appropriate workflow. Use when the user provides a Linear ticket.
user-invocable: true
---

# Linear Ticket

You take a Linear ticket link or identifier, fetch its details, and produce a handoff file for the orchestrator — routing to the appropriate workflow (feature build or bug fix).

## Critical Rules

1. **Always fetch the ticket first.** Use the `mcp__linear-server__get_issue` tool to retrieve full ticket details before proceeding.
2. **Include relations.** Set `includeRelations: true` to get blocking/related context.
3. **Route correctly.** Use ticket labels, title, and description to determine whether this is a feature or a bug fix.
4. **Pre-fill the handoff.** Extract as much information as possible from the ticket so the orchestrator has full context without re-asking the user.

---

## Step 1: Parse the Ticket Identifier

Extract the issue ID from the user's input. Supported formats:
- Full URL: `https://linear.app/<workspace>/issue/ABC-123/...` → extract `ABC-123`
- Short identifier: `ABC-123`
- Full ID (UUID): pass as-is

---

## Step 2: Fetch Ticket Details

Call `mcp__linear-server__get_issue` with:
- `id`: the extracted identifier
- `includeRelations`: `true`

Extract from the response:
- **Title** — issue title
- **Description** — full markdown description
- **Labels** — list of label names (used for routing)
- **Priority** — priority level
- **Relations** — blocking/related issues for context
- **Comments** — any discussion that adds context

---

## Step 3: Route to the Right Workflow

Determine the workflow based on these signals (in priority order):

**Route to `bug-fix`** if ANY of:
- A label contains "bug" (case-insensitive)
- The title starts with "fix", "bug", or "broken" (case-insensitive)
- The description explicitly describes incorrect/broken behavior

**Route to `feature-build`** if ANY of:
- A label contains "feature", "enhancement", or "improvement" (case-insensitive)
- The title starts with "add", "implement", "create", or "support" (case-insensitive)
- The description describes new functionality to build

**If ambiguous:** Ask the user whether this is a feature or a bug fix before proceeding.

---

## Step 4: Gather Additional Context

Present the ticket summary to the user and confirm the routing decision. Ask if there's any additional context not in the ticket that the orchestrator should know about (e.g., related code areas, past attempts, urgency).

---

## Step 5: Write Handoff File

Write a handoff file to `docs/<feature-or-area>/handoff.md` inside the project directory.

### For feature-build:

```markdown
# Orchestrator Handoff

## Workflow
feature-build

## Project Path
<absolute path to project root>

## Feature Name
<feature-name derived from ticket>

## Linear Ticket
<ticket identifier> — <ticket title>

## Plan
<path to plan doc if it already exists, or "to be created by architect">

## Feature Description
<from ticket title + description + any user-provided context>

## Entities
<any mentioned in the description, or "to be determined by architect">

## API Surface
<any mentioned in the description, or "to be determined by architect">

## Database Changes
<any mentioned in the description, or "to be determined by architect">

## Special Considerations
<from labels, priority, related issues, user context>

## Notes
<any additional context>
```

### For bug-fix:

```markdown
# Orchestrator Handoff

## Workflow
bug-fix

## Project Path
<absolute path to project root>

## Bug Area
<affected feature or area>

## Linear Ticket
<ticket identifier> — <ticket title>

## Bug Description
<from ticket title + description>

## Expected Behavior
<extracted from description if present>

## Reproduction Steps
<extracted from description if present>

## Logs / Stack Traces
<extracted from description or comments if present>

## Context
<from related issues, priority, labels, user context>

## Notes
<any additional context>
```

---

## Step 6: Tell the User to Continue

After writing the handoff file, tell the user:

> The handoff file is ready at `docs/<name>/handoff.md`. To start:
>
> 1. Run `/compact` or `/clear` to free up context
> 2. Then say: **"Start the orchestrator for `<name>` using the handoff at `docs/<name>/handoff.md`"**
>
> The orchestrator will read the handoff, create the agent team, and drive the workflow from there.
