# Feature: Recipe Instructions & Photos

## Summary

Recipes gain **instructions** (an ordered list of steps) and **photos** (images attached to the recipe). Both imports fill them: the URL import keeps the page's `recipeInstructions` (today the extractor drops it), and the photo import takes **two named photos — one of the ingredient list, one of the method** — reads steps from the second, and attaches both source photos to the draft. Users can also add and remove photos by hand and edit steps on the New/Edit forms. Photo bytes live in the **Railway S3 bucket** (private, SigV4, presigned reads); Postgres stores only metadata and the object key. The recipe page becomes three tabs: **Ingredients · Instructions · Photos**.

## Entities

### Recipe Step

One row per step, ordered. Steps are plain text, never reviewed, and always written as a whole list (replace-all), so they need no per-step endpoints or review state.

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| recipe_id | UUID | FK → recipes, `ON DELETE CASCADE` |
| position | INT | 0-based, `UNIQUE (recipe_id, position)` |
| text | TEXT | non-blank, ≤ 2000 chars (validated in Kotlin) |
| created_at | TIMESTAMPTZ | |

### Recipe Photo

Metadata only; the bytes are an object in the bucket at `storage_key`.

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| recipe_id | UUID | FK → recipes, `ON DELETE CASCADE` (the object is deleted by the service before the row) |
| position | INT | 0-based display order, `UNIQUE (recipe_id, position)` |
| storage_key | TEXT | `recipes/{recipeId}/{photoId}.{ext}` — unique |
| media_type | VARCHAR(50) | `image/jpeg` in practice (the webapp re-encodes); `png`/`gif`/`webp` accepted |
| byte_size | INT | as stored |
| width, height | INT NULL | from `ImageIO` when it can read the format; null otherwise |
| source | VARCHAR(20) | `upload` (added by a person) or `import` (the photo the import was read from) |
| role | VARCHAR(20) NULL | for `import` photos: `ingredients` or `instructions`; null for uploads |
| created_by | UUID | FK → users `ON DELETE RESTRICT` |
| created_at | TIMESTAMPTZ | |

Limits: **6 photos per recipe**, **5 MB decoded per photo** (the existing import validator's cap; the webapp sends ~300–500 KB after its 1568px downscale).

## Storage

**Bucket:** Railway Storage Bucket linked to `camper-service`. S3-compatible, virtual-hosted-style URLs, private (no public buckets), SigV4. The service reads the standard AWS names, set on `camper-service` in Railway (the bucket's own `BUCKET`/`ENDPOINT`/… variables are referenced under these names):

| Variable | Meaning |
|---|---|
| `AWS_S3_BUCKET_NAME` | bucket name |
| `AWS_ENDPOINT_URL` | S3 API endpoint, e.g. `https://t3.storageapi.dev` |
| `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` | credentials (the SDK's default provider chain also reads these) |
| `AWS_DEFAULT_REGION` | e.g. `auto` (read explicitly — SDK v2 looks for `AWS_REGION`, not `AWS_DEFAULT_REGION`) |

The factory reads all five explicitly (system property, then env — the same order as `ANTHROPIC_API_KEY`) rather than relying on the SDK's chain, so a missing one fails at startup with its name in the message. `AWS_S3_BUCKET_NAME` unset selects the local file store.

**Reading photos in the browser:** the bucket is private and an `<img>` tag can send neither `X-User-Id` nor S3 auth, so the detail response carries a **presigned GET URL per photo** (`photos[].url`), valid 1 hour and **memoised server-side per key for 50 minutes** so a refetch of the recipe (window focus, a mutation settling) returns the same URL and the browser's cache hits instead of re-downloading every photo. `<img>` loads need no CORS.

**Local development without a bucket:** when `AWS_S3_BUCKET_NAME` is unset, a **filesystem store** writes under `camper/.photos/` (gitignored) and `url` points at the API's own proxy route (below), so the whole feature works locally with nothing extra running. (Same idea as the scraper's NoOp/relay clients.)

**Proxy route** `GET /api/recipes/{id}/photos/{photoId}/content` streams the object through the API with `Cache-Control: private, max-age=31536000, immutable` (a photo id's content never changes). It is what the local store's URLs point at; in production the presigned URL is used instead, and the route stays as a fallback. It does **not** require `X-User-Id` (an `<img>` can't send it) — the same trust-by-unguessable-UUID that `GET /api/recipes/{id}` already relies on.

## API Surface

### Changed responses

- `RecipeDetailResponse` gains `steps: string[]` and `photos: RecipePhotoResponse[]` (required mapper params, no defaults — same rule as `favoriteCount`).
- `RecipeResponse` (list) is unchanged — no steps, no photos, no counts. (A cover thumbnail on list rows is a possible follow-up, not this feature.)

### New / changed endpoints (all under `/api/recipes`, `X-User-Id` required unless noted)

| Endpoint | Status | Notes |
|---|---|---|
| `POST /api/recipes` | 201 | `CreateRecipeRequest` gains optional `steps: string[]` |
| `PUT /api/recipes/{id}/steps` | 200 | body `{ steps: string[] }`, replace-all; returns `{ steps }`. Blank entries rejected (400), ≤ 100 steps, each ≤ 2000 chars |
| `POST /api/recipes/{id}/photos` | 201 | body `{ mediaType, data }` (raw base64, same shape and validator as import) → `RecipePhotoResponse`; 409 `PHOTO_LIMIT` at 6 |
| `DELETE /api/recipes/{id}/photos/{photoId}` | 204 | deletes the object then the row; idempotent |
| `GET /api/recipes/{id}/photos/{photoId}/content` | 200 | bytes; **no `X-User-Id`**; immutable cache headers |
| `POST /api/recipes/import-images` | 201 | `images[]` entries gain `role: "ingredients" \| "instructions"`; 1–2 images; exactly one `ingredients` photo required |

`RecipePhotoResponse`: `{ id, url, mediaType, width, height, byteSize, source, role, position, createdAt }`.

### Authorization

Same as the rest of the recipe feature: no server-side ownership enforcement on edits (published recipes are a shared library; the webapp hides edit on someone else's draft). `import`-sourced photos are deletable like any other.

## Database Changes

- `V045__create_recipe_steps.sql` + `rollback/R045__drop_recipe_steps.sql`
- `V046__create_recipe_photos.sql` + `rollback/R046__drop_recipe_photos.sql`
- `dev_seed.sql`: 3–5 steps for each of the three seeded recipes (id prefix `aa170000`, `ON CONFLICT (id) DO NOTHING`). No seeded photos.

## Client Interfaces

### `clients/photo-storage-client` (new)

Object storage behind an interface, so the service never sees S3.

```kotlin
interface PhotoStorageClient {
    fun put(param: PutObjectParam): Result<Unit, AppError>      // key, mediaType, bytes
    fun get(param: GetObjectParam): Result<StoredObject?, AppError>
    fun delete(param: DeleteObjectParam): Result<Unit, AppError>  // idempotent
    /** A URL a browser can load the object from without credentials, for at least [minValidity]. */
    fun urlFor(param: UrlForParam): Result<String, AppError>
}
```

- `S3PhotoStorageClient` — AWS SDK for Java v2 (`software.amazon.awssdk:s3`), `endpointOverride(AWS_ENDPOINT_URL)`, static credentials, `S3Presigner` for `urlFor` (1 h) with a 50-minute in-memory memo per key. Factory reads `AWS_S3_BUCKET_NAME`/`AWS_ENDPOINT_URL`/`AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY`/`AWS_DEFAULT_REGION`.
- `LocalFilePhotoStorageClient(dir)` — files under `dir/<key>`; `urlFor` returns the API proxy path (`/api/recipes/{recipeId}/photos/{photoId}/content` — the key encodes both ids).
- `FakePhotoStorageClient` (testFixtures) — in-memory map; `urlFor` returns `fake://<key>`.
- Integration test: Testcontainers **MinIO** round-trip (put → get → presign → delete) so the S3 client is exercised without Railway.

### `clients/recipe-client` (extended)

Models `RecipeStep(id, recipeId, position, text, createdAt)`, `RecipePhoto(id, recipeId, position, storageKey, mediaType, byteSize, width?, height?, source, role?, createdBy, createdAt)`.

New methods: `getSteps(recipeId)`, `replaceSteps(recipeId, texts)` (delete + insert in one transaction, positions from list order), `getPhotos(recipeId)` (ordered by position), `getPhotoById(photoId)`, `addPhoto(param)` (position = max+1), `removePhoto(photoId)`. `CreateRecipeParam` unchanged (steps are written by the service after create). Fake updated; `getSteps`/`getPhotos` order by position.

### `clients/recipe-scraper-client` (extended)

- `ScrapedRecipe` gains `steps: List<String>`; `ScrapedRecipeJson` + `scrapedRecipeSchema()` gain a required `steps` array of strings.
- **Extractor** keeps `recipeInstructions` and **flattens it to plain strings before sending** (schema.org allows a string, `string[]`, `HowToStep[]`, or `HowToSection[]` with nested `itemListElement`; keep only each step's `text`, drop `name`/`url`/`image`). The visible-text fallback needs no change — the model reads steps from the text.
- **System prompt** gains a `steps` rule: the method as an ordered list, one string per step, as written but with step numbers/labels stripped, empty when the source has none.
- **Photo import**: `RecipeImage` gains `role`. `buildForImages` labels each block `Ingredients photo:` / `Instructions photo:` and the user message says explicitly which is which; with only an ingredients photo it says the method may be absent and `steps` should be `[]`. "Unreadable" is still decided by the ingredient list alone.
- Relay client and offline harness write/label the roles too.

## Service Layer

### Actions

- `GetRecipeAction`: also loads steps and photos (two client calls) and presigns each photo's URL via `PhotoStorageClient.urlFor`.
- `CreateRecipeAction`: writes `steps` after creating the recipe when present.
- `ReplaceRecipeStepsAction` (new): validates, `replaceSteps`, returns the list.
- `AddRecipePhotoAction` (new): validates like the import (count ≤ 6, media type, base64, ≤ 5 MB), reads dimensions with `ImageIO` (null if unreadable), `put` to storage under `recipes/{recipeId}/{photoId}.{ext}`, then inserts the row; if the row insert fails, deletes the object.
- `RemoveRecipePhotoAction` (new): `delete` the object (ignore not-found), then the row.
- `GetRecipePhotoContentAction` (new): row → `get` → bytes + media type.
- `ScrapedRecipeDrafter.createDraft`: also `replaceSteps(scraped.steps)`.
- `ImportRecipeFromImagesAction`: validates roles (1–2 images, exactly one `ingredients`), and after the draft exists stores each source image as a photo with `source = import` and its role, via the same code path as `AddRecipePhotoAction`. A storage failure here **does not fail the import** — it's logged; the draft is still worth more than the photo.
- `DeleteRecipeAction`: deletes the recipe's objects from storage before the row (the DB cascade removes the metadata).

### Error types

`RecipeError.PhotoNotFound(photoId)` → 404; `RecipeError.PhotoLimit(recipeId)` → 409 `PHOTO_LIMIT`; `RecipeError.StorageFailed(reason)` → 502 `STORAGE_FAILED`. Steps validation reuses `Invalid`.

### Configuration

`PhotoStorageClientConfig`: `AWS_S3_BUCKET_NAME` set → S3 client; else local file store at `PHOTO_STORE_DIR` (default `./.photos`). Logged at startup like the scraper choice. `RecipeServiceConfig` gains the storage client.

## Import Flow (what changes)

**URL:** extractor keeps and flattens `recipeInstructions` → model returns `steps` → drafter writes them. Token cost: a typical 8–12 step method adds ~300–600 output tokens (≈ +$0.005 on Sonnet 5).

**Photos:**
1. Sheet: two slots. *Ingredients photo* (required): "Show the full ingredient list, quantities included." *Instructions photo* (optional): "Show the method/steps. Skip it if the card has none." Each slot has its own picker, thumbnail and remove; both shrink to chips while importing.
2. Request: `images: [{ mediaType, data, role: "ingredients" }, { …, role: "instructions" }]`.
3. Prompt: each image preceded by its role label; user message names them and asks for `steps` from the instructions photo (or `[]`).
4. Draft created with lines (review as today) **and** steps; both photos attached with `source = import`.
5. Review screen shows the Instructions and Photos tabs alongside the lines.

Cost: two photos ≈ 4.7k image tokens + text ≈ 10k in / 1.5–2k out ≈ **$0.04–0.05** per two-photo import.

## Frontend

- **Recipe page** (`RecipeDetailPage`): Radix `Tabs` under the hero — **Ingredients · Instructions · Photos** — with the active tab in `?tab=` (replace-navigated, default `ingredients`), so every state has a URL and Back leaves the page rather than cycling tabs. The draft-review UI stays inside the Ingredients tab. Instructions: an `<ol>` of steps; empty state with an "Add instructions" link to the edit page when `mayEdit`. Photos: a 3-column grid of `<img src={photo.url}>` (lazy), tapping opens a full-height `Sheet` at `/recipes/:id/photos/:photoId` with the image and a "Remove photo" button; an "Add photo" button on the tab uploads immediately through the existing `preparePhoto` pipeline (a create, like the rapid ingredient add — not part of the form), with a toast on success. `import` photos show a small "From import" badge.
- **New/Edit forms**: `RecipeFormValues` gains `steps: string[]`; a `StepsEditor` (one `TextArea` per step, add, remove, move up/down — no drag). New sends `steps` in the create request; Edit diffs and, if changed, one `PUT …/steps` inside `useSaveRecipeEdits`. `newRecipeDraft` persists `steps`.
- **Import sheet**: the two named slots above; the verbose helper copy is deliberate.
- **Types**: `RecipeDetailResponse` gains `steps`, `photos`; `api/recipes.ts` gains `replaceRecipeSteps`, `addRecipePhoto`, `removeRecipePhoto`; hooks `useAddRecipePhoto`/`useRemoveRecipePhoto` invalidate `recipeKey(id)` (no optimistic update — the URL only exists after upload).
- **Pure/tested**: `lib/recipeSteps.ts` (`normaliseSteps`: trim, drop blanks, strip leading "1." / "Step 1:" labels; `stepsChanged`), extending `lib/importPhotos.ts` for the two-slot selection.

## PR Stack

| # | Branch | Title | Description |
|---|---|---|---|
| 1 | `feat-recipe-steps-photos-db-client` | feat(recipes): steps & photos schema, storage client, recipe-client methods | V045/V046 + rollbacks + seed; new `photo-storage-client` (S3, local file, fake, MinIO test); recipe-client models/ops/fake + integration tests |
| 2 | `feat-recipe-steps-photos-service` | feat(recipes): steps and photos endpoints | Detail response, create-with-steps, replace steps, add/remove/serve photos, delete-recipe cleanup, config; unit + acceptance tests |
| 3 | `feat-scraper-instructions` | feat(scraper): read instructions from pages and photos; photo roles | Extractor flattening, schema `steps`, prompt rules, roles in image prompt, relay/harness, tests |
| 4 | `feat-import-steps-photos` | feat(recipes): imports store steps and attach source photos | Drafter writes steps; image import validates roles and attaches photos; tests |
| 5 | `feat-webapp-recipe-tabs` | feat(webapp): recipe tabs — instructions and photos, steps editor | Tabs, Instructions/Photos tabs, photo viewer sheet, add/remove photo, StepsEditor on New/Edit, draft persistence, lib tests |
| 6 | `feat-webapp-import-two-photos` | feat(webapp): two-photo import (ingredients + instructions) | Two-slot picker, roles in the request, compact chips; CLAUDE.md updates across modules |

1 → 2 → 4 depend in order; 3 is independent of 2; 5 and 6 need 2 (and 6 needs 4) deployed for a real run. Each PR runs the same local checks as PR #323 (module tests, acceptance with Testcontainers, `npm run build`/`test`), plus a relay-driven end-to-end for 4 and 6.

## Open Questions

1. **Photo cap of 6 per recipe** — fine, or higher?
2. **Local dev store on the filesystem vs. MinIO in `docker-compose`** — the plan uses the filesystem (nothing new to run); MinIO stays a test-only dependency. Say if you'd rather run against a real S3 API locally.
3. **Presigned URL lifetime 1 h / memo 50 min** — long enough for a cooking session without re-fetch; a photo won't be viewable from a tab left open for hours until the recipe refetches (which it does on focus). OK?

Resolved: the service reads the `AWS_*` variables already set on `camper-service` in Railway (2026-09-22).
