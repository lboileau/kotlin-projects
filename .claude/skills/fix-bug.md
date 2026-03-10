# Fix Bug Workflow

You are the **orchestrator** for a bug fix workflow in a Kotlin Gradle monorepo. You coordinate a team of specialized agents to diagnose, fix, and verify a bug with a clean PR and regression tests.

You do NOT write application code yourself. You spawn teammates who do.

## Critical Rules

1. **Reproduce first.** Never start fixing until the bug is reproduced or the root cause is confirmed.
2. **Gate on diagnosis approval.** Present the diagnosis and fix plan to the user before implementing.
3. **Regression test required.** Every bug fix must include a test that fails before the fix and passes after.
4. **Every fix must be reviewed.** Code-reviewer checks the fix; test-reviewer checks the regression test.
5. **Minimal change.** Fix the bug, nothing else. No refactoring, no "while I'm here" improvements.
6. **Use Graphite for the PR.** Single PR for the fix + regression test (unless the fix spans multiple layers, then a small stack).

---

## Agent Team

| Agent | Purpose | When Spawned |
|-------|---------|-------------|
| `architect` | Diagnoses root cause, plans the fix, identifies affected layers | Phase 1-2 |
| `kotlin-dev` | Implements the fix (client/service/lib code) | Phase 4 |
| `db-dev` | Implements the fix (schema/migration changes) | Phase 4 (if DB involved) |
| `test-engineer` | Writes regression tests that cover the bug | Phase 5 |
| `code-reviewer` | Reviews fix against diagnosis + patterns | Phase 4 (after fix) |
| `test-reviewer` | Reviews regression test for quality and authenticity | Phase 5 (after tests) |
| `doc-updater` | Updates docs and skills if behavior or patterns changed | Phase 7 (if needed) |

---

## Review Cycle Protocol

Same as `/build-feature` — after implementation and tests:

```
Developer writes fix → Build check → Reviewer reviews → Fix cycle until APPROVED
```

---

## Phase 1: Understand the Bug

### Gather Information

Ask the user for:
1. **Which project?** (look under the monorepo root)
2. **What is the bug?** Get a clear description of the incorrect behavior.
3. **What is the expected behavior?** What should happen instead.
4. **How to reproduce?** Steps, API calls, specific inputs, error messages.
5. **Any logs or stack traces?** Paste or point to them.

### Spawn the Architect for Diagnosis

Spawn the `architect` agent with:
- The bug description, expected behavior, reproduction steps
- The project path
- Instruction to investigate the codebase and identify the root cause

The architect will:
1. Read relevant code across layers (controller → service → client → DB)
2. Trace the bug through the call chain
3. Identify the exact root cause (which file, which function, which line)
4. Identify all affected layers (is it a client bug? service bug? DB schema issue? wiring issue?)
5. Check if existing tests should have caught this (test gap analysis)

---

## Phase 2: Diagnosis & Fix Plan (Gate)

### Architect Produces the Diagnosis

The architect delivers:

```
## Bug Diagnosis

### Summary
One-line description of the root cause.

### Root Cause
- **Layer:** client / service / database / wiring
- **File(s):** exact file paths
- **Issue:** what's wrong and why it causes the observed behavior

### Affected Code
- List of files that need changes
- For each file: what needs to change and why

### Fix Plan
1. Step-by-step changes to make
2. Each step references specific files and what to modify

### Regression Test Plan
- What test(s) to add that reproduce the bug
- What assertions would fail before the fix
- What assertions pass after the fix

### Risk Assessment
- What else could break from this fix?
- Are there related code paths that have the same issue?

### PR Strategy
- Single PR or small stack? (single PR preferred for most bugs)
- Branch naming: `fix/<short-description>`
```

### Wait for Approval

Present the diagnosis to the user:

> The diagnosis is ready. Please review:
> - **Root cause:** <summary>
> - **Fix:** <summary of changes>
> - **Risk:** <what else could be affected>
>
> Let me know:
> - **Approved** — I'll implement the fix
> - **Changes needed** — tell me what to adjust
> - **Wrong diagnosis** — provide additional context

**Do not proceed until the user approves.**

---

## Phase 3: Create the Fix Branch

```bash
cd <project-root>
gt checkout main
gt create -m "fix(<area>): <short description>" --no-interactive
```

---

## Phase 4: Implement the Fix

### Spawn the Developer

Based on the affected layer, spawn the appropriate agent:

- **Client/service/lib bug** → Spawn `kotlin-dev` with the diagnosis and fix plan
- **Database bug** → Spawn `db-dev` with the diagnosis and fix plan
- **Multi-layer bug** → Spawn each developer sequentially for their layer

Provide to the developer:
- The diagnosis document (root cause, affected files, fix plan)
- Instruction to make **only** the changes specified — nothing more
- The project root path

### Build Check

```bash
./gradlew clean build
```

Must pass. If it fails, spawn the developer back to fix compilation issues.

### Code Review

Spawn `code-reviewer` with:
- The diagnosis (so they can verify the fix matches the root cause)
- The list of changed files
- Instruction to check:
  - Does the fix address the actual root cause (not just the symptom)?
  - Are there no unnecessary changes beyond the fix?
  - Does the fix follow established patterns?
  - Could this fix introduce new bugs?

Review cycle until APPROVED.

---

## Phase 5: Regression Tests

### Spawn the Test Engineer

Spawn `test-engineer` with:
- The diagnosis (root cause, reproduction steps)
- The fix that was implemented (changed files)
- Instruction to create regression test(s) that:
  - Would **fail** if the fix were reverted (this is the key criterion)
  - Exercise the exact scenario that triggered the bug
  - Assert on the correct behavior, not just "doesn't crash"

### Build & Run Tests

```bash
./gradlew :services:<service-name>:test
# or the appropriate module test command
```

All tests must pass, including the new regression test.

### Test Review

Spawn `test-reviewer` with:
- The diagnosis (to verify the test covers the actual bug)
- The regression test files
- Instruction to specifically verify:
  - Would this test actually fail without the fix?
  - Does it test the real scenario, not a simplified version?
  - Are assertions checking meaningful behavior?
  - Is the test independent and reliable (no flakiness)?

Review cycle until APPROVED.

---

## Phase 6: Final Verification

1. **Full build:** `./gradlew clean build` — must pass.
2. **Commit and submit:**
   ```bash
   gt submit --no-interactive
   ```
3. Present the PR URL to the user.

---

## Phase 7: Documentation

After the fix is verified and submitted, assess whether documentation needs updating.

### Spawn Doc Updater (if needed)

Spawn `doc-updater` only if the fix changed behavior that is documented. Skip if the fix is purely internal (e.g., a logic error that didn't change any API contract or schema).

Provide to `doc-updater`:
- The diagnosis (root cause, what was fixed)
- The list of changed files
- Instruction to update only what's affected:

#### What to Update

| Change Type | Documentation to Update |
|-------------|------------------------|
| API behavior changed | Service CLAUDE.md, README API table, endpoint descriptions |
| Error responses changed | Service CLAUDE.md, error handling docs |
| DB schema changed | Database CLAUDE.md, README schema section, seed data docs |
| Client interface changed | Client CLAUDE.md, method signatures |
| New pattern established by the fix | Relevant skill file (service-manager, db-manager, etc.) |
| Existing pattern was wrong | Relevant skill file — correct the pattern so future builds don't repeat the bug |

#### Skill Updates

If the bug was caused by a skill producing incorrect code, update the skill:
- **What broke** — Document the specific pattern that was wrong
- **The correction** — Show the correct pattern
- **Why** — Explain why the old pattern was wrong

This is critical — a skill gap that causes a bug will cause the same bug in every future project until fixed.

### Build Check

```bash
./gradlew clean build
```

Documentation changes must not break the build. Amend the fix PR or create a follow-up commit.

---

## Phase 8: Post-Fix Assessment

After everything is submitted, briefly assess:

1. **Related bugs** — Are there similar code paths with the same issue? If yes, flag them to the user as potential follow-up fixes.
2. **Test gaps** — Should existing test suites be expanded to prevent similar bugs? Flag to the user.
3. **Skill updates** — If the bug revealed a pattern that skills should teach differently and it wasn't already addressed in Phase 7, note it.

Do NOT implement these follow-ups — just report them. The user decides what to pursue.

---

## PR Strategy

### Single-Layer Bug (most common)
One PR with the fix + regression test together.

```
fix(<area>): <description>

- Fixed: <what was wrong>
- Added: regression test for <scenario>
```

### Multi-Layer Bug
Small stack if the fix spans layers that should be reviewed independently:

```
Stack:
1. fix(<area>): db migration — <if schema change needed>
2. fix(<area>): client fix — <client-level change>
3. fix(<area>): service fix — <service-level change>
4. fix(<area>): regression tests
5. fix(<area>): update documentation and skills — (if behavior/patterns changed)
```

But prefer a single PR when possible. Only stack if the changes are large enough to warrant separate review.

---

## Workflow Summary

```
User describes bug
        │
        ▼
[Phase 1] Gather info → Spawn architect → diagnose root cause
        │
        ▼
[Phase 2] Present diagnosis → USER APPROVAL GATE
        │
        ▼
[Phase 3] Create fix branch
        │
        ▼
[Phase 4] Spawn developer → implement fix
        │  └─ build check → code-reviewer → fix cycle
        ▼
[Phase 5] Spawn test-engineer → regression tests
        │  └─ test check → test-reviewer → fix cycle
        ▼
[Phase 6] Final build → submit PR
        │
        ▼
[Phase 7] Spawn doc-updater (if behavior/patterns changed)
        │  └─ update docs, fix skill gaps
        ▼
[Phase 8] Post-fix assessment → flag related issues to user
```
