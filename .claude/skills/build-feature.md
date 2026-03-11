# Build Feature Workflow

You gather requirements from the user and then spawn the **orchestrator** agent team to execute the build. You do NOT write code or manage PRs yourself.

## Critical Rules

1. **Use agent teams.** All features MUST be built using agent teams via the orchestrator.
2. **Gate on plan approval.** Never let the orchestrator proceed until the user explicitly approves the plan.
3. **Gather complete requirements.** The orchestrator needs clear, unambiguous context to work effectively.

---

## Step 1: Gather Requirements

Ask the user:

1. **Which project?** Look under the monorepo root for existing projects. Confirm the project path.
2. **What feature do you want to add?** Get a clear description of the desired functionality.
3. **What entities/resources are involved?** Names, fields, relationships between them.
4. **What API endpoints are needed?** HTTP methods, paths, request/response shapes.
5. **What database changes are needed?** New tables, columns, constraints, relationships.
6. **Any special considerations?** Authentication, permissions, real-time updates, external integrations.

Keep asking until you have enough detail for the orchestrator to produce a complete plan.

---

## Step 2: Scope Check

Evaluate the feature size. A feature is **too big** if it involves:
- More than 4 new tables with complex relationships
- More than 4 new client interfaces
- More than 10 new API endpoints
- Cross-service concerns

If too big, explain why and suggest a breakdown into smaller, independently shippable features. Do not proceed until scope is manageable.

---

## Step 3: Existing Feature Check

Before proceeding, search the project for:
- Existing entities with similar names or purposes
- Existing API endpoints that overlap
- Existing database tables that could be extended
- Existing clients that already handle related data

If overlap is found, present it to the user and ask whether to extend, reuse, or create new.

---

## Step 4: Spawn the Orchestrator

Once requirements are gathered and scope is confirmed, spawn the `orchestrator` agent team with:

- **Project path** — The full path to the project root
- **Feature description** — Complete description of the feature with all gathered details
- **Entities** — All entity names, fields, and relationships
- **API surface** — All endpoints with methods, paths, and shapes
- **Database changes** — All table/column changes
- **Special considerations** — Any constraints or requirements that affect the build

The orchestrator will:
1. Spawn the architect to create a plan
2. Present the plan for user approval
3. Execute the full PR stack with agent teams (db-dev, kotlin-dev, test-engineer)
4. Run review cycles after each PR (code-reviewer, test-reviewer)
5. Collect retros from all developers
6. Spawn doc-updater for retrospective and final report
7. Present the retro to the user before submitting

---

## Graphite Command Reference

The orchestrator uses these commands (provided here for reference):

```bash
gt checkout main                                    # Start from trunk
gt create -m "<commit message>" --no-interactive    # Create stacked branch
gt submit --no-interactive                          # Submit stack to GitHub
gt log                                              # View the stack
gt checkout <branch-name>                           # Move to branch
gt restack                                          # Restack after changes
git add -A && git commit --amend --no-edit          # Amend current branch
```
