# Fix Bug Workflow

You gather bug details from the user and then spawn the **orchestrator** agent team to diagnose, fix, and verify the bug. You do NOT write code or manage PRs yourself.

## Critical Rules

1. **Use agent teams.** All bug fixes MUST be built using agent teams via the orchestrator.
2. **Gate on diagnosis approval.** Never let the orchestrator proceed with a fix until the user approves the diagnosis.
3. **Regression test required.** Every bug fix must include a test that fails before the fix and passes after.
4. **Minimal change.** Fix the bug, nothing else. No refactoring, no "while I'm here" improvements.

---

## Step 1: Gather Bug Details

Ask the user:

1. **Which project?** Look under the monorepo root for existing projects. Confirm the project path.
2. **What is the bug?** Get a clear description of the incorrect behavior.
3. **What is the expected behavior?** What should happen instead.
4. **How to reproduce?** Steps, API calls, specific inputs, error messages.
5. **Any logs or stack traces?** Paste or point to them.
6. **When did it start?** Was it working before? Any recent changes that might be related?

Keep asking until you have enough detail for the orchestrator to diagnose effectively.

---

## Step 2: Spawn the Orchestrator

Once bug details are gathered, spawn the `orchestrator` agent team with:

- **Project path** — The full path to the project root
- **Workflow type** — Bug fix (not feature build)
- **Bug description** — Complete description of the incorrect behavior
- **Expected behavior** — What should happen
- **Reproduction steps** — How to trigger the bug
- **Logs/stack traces** — Any error output
- **Context** — When it started, recent changes, suspected area

### Bug Fix Workflow for the Orchestrator

The orchestrator will execute this workflow using agent teams:

```
1. Spawn architect → diagnose root cause → produce diagnosis report
2. Present diagnosis to user → USER APPROVAL GATE
3. Create fix branch (fix/<short-description>)
4. Spawn appropriate developer(s) → implement fix
   └─ build check → code-reviewer → fix cycle until APPROVED
5. Spawn test-engineer → regression tests
   └─ test check → test-reviewer → fix cycle until APPROVED
6. Final build: ./gradlew clean build
7. Collect retros from all developers
8. Spawn doc-updater (if behavior/patterns changed)
9. Present retro/final report to user → USER APPROVAL GATE
10. Submit PR
```

### PR Strategy

- **Single-layer bug (most common):** One PR with fix + regression test
- **Multi-layer bug:** Small stack if the fix spans layers needing separate review

### Post-Fix Assessment

After submission, the orchestrator flags to the user:
- Related code paths with the same issue (potential follow-up fixes)
- Test gaps that should be expanded
- Skill updates if the bug revealed a pattern that skills should teach differently
