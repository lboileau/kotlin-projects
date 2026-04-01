---
name: run-dev
description: Start the full local dev environment (DB, API, webapp) with a clean build. Use when the user wants to run or test the app locally.
user-invocable: true
---

# Run Dev

Start the full local dev environment for the camper project. Kills stale processes, resets the DB, does a clean build, and starts all three services.

## What it does

1. Kills any existing processes on ports 8080 (API) and 3000 (webapp) — **including from other worktrees**
2. Ensures the Postgres container is running
3. Cleans and re-runs Flyway migrations (fresh schema)
4. Seeds dev data
5. Runs `./gradlew clean build -x test` (full rebuild, no stale artifacts)
6. Starts `bootRun` (API on :8080) and `npm run dev` (webapp on :3000)
7. Waits for health checks, then opens the browser

## How to run

Find the project root (the directory containing `run-dev.sh`) — this is the `camper/` directory inside whatever worktree the user is working in.

Run the script in the foreground so the user can see output and Ctrl+C to stop:

```bash
! cd <project-root> && ./run-dev.sh
```

Tell the user to run the command above with `!` prefix so it runs in this session. **Do not run it yourself** — it's a long-running foreground process that holds the terminal until Ctrl+C.

### Options

- `--skip-build` — Skip the Gradle clean build (use existing artifacts). Useful when only frontend or DB changes were made.
- `--keep-db` — Don't reset the database. Only runs pending migrations instead of clean + migrate. Useful when the user has test data they want to keep.

### Examples

```bash
# Full reset (default)
! cd camper && ./run-dev.sh

# Skip rebuild, just restart services with fresh DB
! cd camper && ./run-dev.sh --skip-build

# Keep existing data, just rebuild and restart
! cd camper && ./run-dev.sh --keep-db

# Quick restart (skip build + keep data)
! cd camper && ./run-dev.sh --skip-build --keep-db
```

## Troubleshooting

If the user reports 500 errors after starting:
1. Check which worktree the Java process is running from: `ps -p $(lsof -i :8080 -t) -o command | grep worktrees`
2. If it's the wrong worktree, the script wasn't run or port kill failed — re-run `./run-dev.sh`
3. If migrations failed, check for version conflicts: `ls databases/camper-db/migrations/V03*` for duplicates
