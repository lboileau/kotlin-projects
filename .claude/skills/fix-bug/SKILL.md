---
name: fix-bug
description: Gather bug details from the user, write a handoff file, then hand off to the orchestrator. Use when the user wants to fix a bug.
user-invocable: true
---

# Fix Bug

Gather bug details from the user, write a handoff file, then tell the user to clear context and start the orchestrator. You do NOT write code, manage PRs, or run the build yourself.

## Critical Rules

1. **You are the intake.** Your only job is to gather bug details and produce a handoff file for the orchestrator.
2. **Gather complete details.** The orchestrator cannot ask the user questions, so you must provide everything it needs upfront.
3. **Write the handoff file.** The handoff file is the single artifact that bridges this conversation and the orchestrator's fresh context.

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

## Step 2: Write Handoff File

Once bug details are gathered, write a handoff file to `docs/<feature-or-area>/handoff.md` inside the project directory. Use the bug area or affected feature as the directory name.

**Handoff file format:**

```markdown
# Orchestrator Handoff

## Workflow
bug-fix

## Project Path
<absolute path to project root>

## Bug Area
<affected feature or area name>

## Bug Description
<complete description of the incorrect behavior>

## Expected Behavior
<what should happen instead>

## Reproduction Steps
<step-by-step instructions to trigger the bug>

## Logs / Stack Traces
<any error output, or "none provided">

## Context
<when it started, recent changes, suspected area of code>

## Notes
<any additional context>
```

---

## Step 3: Tell the User to Continue

After writing the handoff file, tell the user:

> The handoff file is ready at `docs/<area>/handoff.md`. To start the fix:
>
> 1. Run `/compact` or `/clear` to free up context
> 2. Then say: **"Start the orchestrator for `<area>` using the handoff at `docs/<area>/handoff.md`"**
>
> The orchestrator will read the handoff, create the agent team, diagnose the bug, and drive the fix from there.
