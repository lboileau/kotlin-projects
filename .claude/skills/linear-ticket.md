# Linear Ticket

You take a Linear ticket link or identifier, fetch its details, and then route to the appropriate workflow skill (`/build-feature` or `/fix-bug`).

## Critical Rules

1. **Always fetch the ticket first.** Use the `mcp__linear-server__get_issue` tool to retrieve full ticket details before proceeding.
2. **Include relations.** Set `includeRelations: true` to get blocking/related context.
3. **Route correctly.** Use ticket labels, title, and description to determine whether this is a feature or a bug fix.
4. **Pre-fill requirements.** Extract as much information as possible from the ticket so the downstream skill has context without re-asking the user.

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

**Route to `/fix-bug`** if ANY of:
- A label contains "bug" (case-insensitive)
- The title starts with "fix", "bug", or "broken" (case-insensitive)
- The description explicitly describes incorrect/broken behavior

**Route to `/build-feature`** if ANY of:
- A label contains "feature", "enhancement", or "improvement" (case-insensitive)
- The title starts with "add", "implement", "create", or "support" (case-insensitive)
- The description describes new functionality to build

**If ambiguous:** Ask the user whether this is a feature or a bug fix before proceeding.

---

## Step 4: Hand Off to the Downstream Skill

Present the ticket summary to the user, confirm the routing decision, then invoke the chosen skill.

### For `/build-feature`, pre-fill:
- **Feature description** — from ticket title + description
- **Entities/resources** — any mentioned in the description
- **API endpoints** — any mentioned in the description
- **Database changes** — any mentioned in the description
- **Special considerations** — from labels, priority, related issues

### For `/fix-bug`, pre-fill:
- **Bug description** — from ticket title + description
- **Expected behavior** — extract from description if present
- **Reproduction steps** — extract from description if present
- **Logs/stack traces** — extract from description or comments if present
- **Context** — from related issues, priority, labels

After presenting the extracted information, ask the user to confirm or supplement before spawning the orchestrator.
