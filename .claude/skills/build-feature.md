# Build Feature

Gather requirements from the user, write a handoff file, then tell the user to clear context and start the orchestrator. You do NOT write code, manage PRs, or run the build yourself.

## Critical Rules

1. **You are the intake.** Your only job is to gather requirements and produce a handoff file for the orchestrator.
2. **Gather complete requirements.** The orchestrator cannot ask the user questions, so you must provide everything it needs upfront.
3. **Write the handoff file.** The handoff file is the single artifact that bridges this conversation and the orchestrator's fresh context.

---

## Step 1: Gather Requirements

Ask the user:

1. **Which project?** Look under the monorepo root for existing projects. Confirm the project path.
2. **What feature do you want to add?** Get a clear description of the desired functionality.
3. **What entities/resources are involved?** Names, fields, relationships between them.
4. **What API endpoints are needed?** HTTP methods, paths, request/response shapes.
5. **What database changes are needed?** New tables, columns, constraints, relationships.
6. **Any special considerations?** Authentication, permissions, real-time updates, external integrations.

Keep asking until you have enough detail for the orchestrator to produce a complete plan without needing to come back with questions.

---

## Step 2: Scope Check

A feature is **too big** if it involves:
- More than 4 new tables with complex relationships
- More than 4 new client interfaces
- More than 10 new API endpoints
- Cross-service concerns

If too big, suggest a breakdown into smaller, independently shippable features. Do not proceed until scope is manageable.

---

## Step 3: Existing Feature Check

Before proceeding, search the project for:
- Existing entities with similar names or purposes
- Existing API endpoints that overlap
- Existing database tables that could be extended
- Existing clients that already handle related data

If overlap is found, present it to the user and ask whether to extend, reuse, or create new.

---

## Step 4: Write Handoff File

Once requirements are gathered and scope is confirmed, write a handoff file to `docs/<feature>/handoff.md` inside the project directory.

**Handoff file format:**

```markdown
# Orchestrator Handoff

## Workflow
feature-build

## Project Path
<absolute path to project root>

## Feature Name
<feature-name>

## Plan
<path to plan doc if it already exists, or "to be created by architect">

## Feature Description
<complete description of the feature with all gathered details>

## Entities
<all entity names, fields, and relationships>

## API Surface
<all endpoints with methods, paths, and shapes>

## Database Changes
<all table/column changes>

## Special Considerations
<any constraints, permissions, real-time requirements, integrations>

## Notes
<any additional context — e.g., "plan already exists and was reviewed", "resuming from phase X">
```

---

## Step 5: Tell the User to Continue

After writing the handoff file, tell the user:

> The handoff file is ready at `docs/<feature>/handoff.md`. To start the build:
>
> 1. Run `/compact` or `/clear` to free up context
> 2. Then say: **"Start the orchestrator for `<feature>` using the handoff at `docs/<feature>/handoff.md`"**
>
> The orchestrator will read the handoff, create the agent team, and drive the build from there.
