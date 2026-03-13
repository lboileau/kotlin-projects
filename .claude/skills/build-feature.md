# Build Feature

Gather requirements from the user, then hand off to the **orchestrator** to execute the build using Claude agent teams in tmux panes. You do NOT write code, manage PRs, or run the build yourself.

## Critical Rules

1. **You are the intake.** Your only job is to gather requirements and prompt the orchestrator with complete context.
2. **Gate on plan approval.** The orchestrator will present a plan — do not let it proceed until the user explicitly approves.
3. **Gather complete requirements.** The orchestrator cannot ask the user questions, so you must provide everything it needs upfront.

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

## Step 4: Hand Off to the Orchestrator

Once requirements are gathered and scope is confirmed, spawn the `orchestrator` using Claude agent teams. Provide it with:

- **Workflow type** — Feature build
- **Project path** — Full path to the project root
- **Feature description** — Complete description with all gathered details
- **Entities** — All entity names, fields, and relationships
- **API surface** — All endpoints with methods, paths, and shapes
- **Database changes** — All table/column changes
- **Special considerations** — Any constraints or requirements that affect the build

The orchestrator takes it from here — it will use Claude agent teams to spawn specialized teammates (architect, db-dev, kotlin-dev, test-engineer, reviewers, doc-updater) in tmux panes, manage the PR stack, run review cycles, and present the retro for user approval before submitting.
