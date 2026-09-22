# Deploy plan — recipe instructions & photos (PR #324)

Deploys are manual (`railway up` from `main`; nothing auto-deploys on merge). Flyway runs on service
startup from the migrations baked into the jar, so **the deploy is the migration run** — there is no
separate "migrate first" step, and none is needed here: `V045__create_recipe_steps` and
`V046__create_recipe_photos` only create tables, so the instance that's still serving during the
swap is unaffected. The webapp ships inside the same container, so frontend and backend switch
together (and the webapp defaults `steps`/`photos` to empty anyway).

Prod reads and pushes are run by the owner with `!` commands (the assistant's auto-mode blocks them);
the assistant prepares scripts in its scratchpad.

## 0. Pre-flight (5 min)

- [ ] PR #324 merged; local `main` pulled.
- [ ] Variables on `camper-service` in Railway — confirm all present:
  `AWS_S3_BUCKET_NAME`, `AWS_ENDPOINT_URL`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_DEFAULT_REGION`
  (photos), plus the existing `DB_*`, `ANTHROPIC_API_KEY` (imports), `RESEND_*`, `APP_BASE_URL`.
  ```
  ! cd /Users/louisboileau/Development/kotlin-projects/camper && railway service camper-service && railway variables | grep -E "AWS_|ANTHROPIC|DB_URL"
  ```
  Without `AWS_S3_BUCKET_NAME` the service would fall back to a **local file store inside the
  container** — photos would vanish on the next deploy. The startup log line must say
  `storing recipe photos in the bucket`.

## 1. Back up prod Postgres (2 min)

Postgres 17 on Railway (`Postgres-MuMx`); dump with a 17 client in plain SQL:

```
! cd /Users/louisboileau/Development/kotlin-projects/camper && railway service Postgres-MuMx && URL=$(railway variables --json | python3 -c "import json,sys; print(json.load(sys.stdin)['DATABASE_PUBLIC_URL'])") && docker run --rm postgres:17-alpine pg_dump "$URL" --no-owner --no-acl > /tmp/camper-prod-$(date +%Y%m%d-%H%M).sql && ls -la /tmp/camper-prod-*.sql
```

## 2. Rehearse the migrations on a clone (5 min, recommended)

Restore the dump into the local `docker-compose` Postgres (fresh DB), then run Flyway against it —
proves V045/V046 apply cleanly on prod's real data before prod sees them:

```
! cd /Users/louisboileau/Development/kotlin-projects/camper/databases/camper-db && docker-compose up -d && PGPASSWORD=postgres psql -h localhost -p 5433 -U postgres -c "DROP DATABASE IF EXISTS camper_clone" -c "CREATE DATABASE camper_clone" && PGPASSWORD=postgres psql -h localhost -p 5433 -U postgres -d camper_clone -q -f /tmp/camper-prod-*.sql 2>&1 | grep -v transaction_timeout; flyway -url=jdbc:postgresql://localhost:5433/camper_clone -user=postgres -password=postgres -locations=filesystem:migrations migrate | tail -5
```

Expect `Successfully applied 2 migrations … now at version v046`. (One harmless
`transaction_timeout` error on load is the 17→16 difference.)

## 3. Deploy (5–8 min)

```
! cd /Users/louisboileau/Development/kotlin-projects/camper && railway service camper-service && ./deploy.sh
```

`deploy.sh` does a clean local build first, then `railway up`. Watch the deploy logs for, in order:

1. `Migrating schema "public" to version "045 - create recipe steps"` / `"046 - create recipe photos"` and
   `Successfully applied 2 migrations`
2. `AWS_S3_BUCKET_NAME set, storing recipe photos in the bucket`
3. `ANTHROPIC_API_KEY set, using Anthropic recipe scraper client`
4. `Started CamperServiceApplicationKt`

If (1) fails the container exits and Railway keeps the previous deployment serving — nothing to undo.

## 4. Smoke test in prod (5 min)

On https://camper-service-production.up.railway.app:

- [ ] Open any recipe → three tabs; Instructions and Photos show their empty states.
- [ ] Photos → **Add photo** with a phone photo → thumbnail appears; its URL starts with the bucket
      endpoint (`https://…storageapi.dev/…?X-Amz-…`), not `/api/photo-store/`. Open it, **Remove** it.
- [ ] Import → Link with a recipe URL → the draft has steps on the Instructions tab.
- [ ] Import → Photos with an ingredients + instructions pair → draft with lines, steps and both photos
      attached (role badges). Check the logs for `Scrape of images=2 … inputTokens=… outputTokens=…`.
- [ ] Edit a recipe: add/drag a step, Save → persists.

## 5. Backfill instructions on prod (15–20 min, after the smoke test)

Prod recipes have no steps until this runs. Same procedure as the local run in
`docs/recipe-scraper/instructions-backfill-2026-09-22.md`, with prod's data:

1. Owner fetches prod's recipe list and ingredient catalogue (`!` commands the assistant prepares) into
   the assistant's scratchpad: `urls.txt`, `recipes.json` (url → id), `ingredients.json`.
2. Assistant runs the offline harness `prompt` mode (fetches each page), answers every case as the
   model, runs `parse`, and writes `apply-steps.py` that `PUT`s `/api/recipes/{id}/steps` per recipe.
3. Owner runs `! python3 …/apply-steps.py` against prod (any `X-User-Id` works — the API is trust-based).
4. Verify by reading back a few recipes; the run is idempotent (replace-whole-list), so it can be re-run.

Expect the same 5 bot-blocked sites to stay unfilled; add their steps by hand or via photo import.

## Rollback

- **App:** Railway → camper-service → Deployments → redeploy the previous one (or `railway rollback`).
  The two new tables are harmless to an older build, so leave them. If you want them gone too:
  `flyway -configFiles=… -locations=filesystem:migrations/rollback` is *not* wired; run
  `R046__drop_recipe_photos.sql` then `R045__drop_recipe_steps.sql` by hand with `psql`.
- **Photos:** objects for removed rows just sit in the bucket; nothing references them. Clean up later
  if ever needed by listing keys under `recipes/` that have no `recipe_photos` row.
- **Backfill:** `PUT /steps` with `[]` per recipe (the apply script takes `--clear`).
