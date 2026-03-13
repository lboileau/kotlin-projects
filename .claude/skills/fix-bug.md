# Fix Bug

Gather bug details from the user, then hand off to the **orchestrator** to diagnose, fix, and verify the bug using Claude agent teams in tmux panes. You do NOT write code, manage PRs, or run the build yourself.

## Critical Rules

1. **You are the intake.** Your only job is to gather bug details and prompt the orchestrator with complete context.
2. **Gate on diagnosis approval.** The orchestrator will present a diagnosis — do not let it proceed until the user explicitly approves.
3. **Regression test required.** Every bug fix must include a test that fails before the fix and passes after.
4. **Minimal change.** Fix the bug, nothing else. No refactoring, no "while I'm here" improvements.

---

## Step 1: Gather Bug Details

Ask the user:

1. **Which project?** Look under the monorepo root for existing projects. Confirm the project path.
2. **What is the bug?** Clear description of the incorrect behavior.
3. **What is the expected behavior?** What should happen instead.
4. **How to reproduce?** Steps, API calls, specific inputs, error messages.
5. **Any logs or stack traces?** Paste or point to them.
6. **When did it start?** Was it working before? Any recent changes that might be related?

Keep asking until you have enough detail for the orchestrator to diagnose effectively without needing to come back with questions.

---

## Step 2: Hand Off to the Orchestrator

Once bug details are gathered, spawn the `orchestrator` using Claude agent teams. Provide it with:

- **Workflow type** — Bug fix
- **Project path** — Full path to the project root
- **Bug description** — Complete description of the incorrect behavior
- **Expected behavior** — What should happen
- **Reproduction steps** — How to trigger the bug
- **Logs/stack traces** — Any error output
- **Context** — When it started, recent changes, suspected area

The orchestrator takes it from here — it will use Claude agent teams to spawn the architect for diagnosis, developers for the fix, test-engineer for regression tests, and reviewers for quality checks, all in tmux panes. It manages the PR stack, collects retros, and presents the final report for user approval before submitting.
