# photo-storage-client

Where photo bytes live. The service stores a key per photo (`recipe_photos.storage_key`) and talks to
this interface; it never sees S3.

## Package
`com.acme.clients.photostorageclient`

## Interface (`api/PhotoStorageClient`)
`put(key, mediaType, bytes)`, `get(key): StoredObject?` (null when absent), `delete(key)` (idempotent),
`urlFor(key, minValiditySeconds = 3600): String` — a URL a browser can load with no credentials and no
headers, since an `<img>` can send neither.

## Implementations
- **`S3PhotoStorageClient`** — any S3-compatible bucket via AWS SDK for Java v2 (`software.amazon.awssdk:s3`).
  Production is the Railway Storage Bucket, which is **private only** (Railway has no public buckets), so
  `urlFor` returns a **presigned GET URL**. It is signed for *twice* the validity asked for and memoised
  per key while at least the asked-for validity remains: a recipe page that refetches (window focus, a
  mutation settling) gets the same URL back for ~an hour, so the browser cache holds instead of every
  photo re-downloading. `delete` drops the memo entry. Bounded by the number of photos ever served.
- **`LocalFilePhotoStorageClient(dir, publicBaseUrl)`** — local development with no bucket: files under
  `dir/<key>`, `urlFor` = `publicBaseUrl/<key>` (the service's `/api/photo-store/**` route serves them from
  the same store). Keys are checked against a strict pattern so nothing can escape `dir`. Media type comes
  from the key's extension (files carry none).
- **`FakePhotoStorageClient`** (testFixtures) — in-memory; `nextPutResult` / `nextDeleteResult` inject a
  failure for cleanup tests; `urlFor` = `fake://<key>`.

## Factory (`PhotoStorageClientFactory.kt`)
`createS3PhotoStorageClient()` reads the standard AWS names — set on `camper-service` in Railway from the
bucket's variables — **all explicitly** (system property, then env, like `ANTHROPIC_API_KEY`), so a missing
one fails at startup by name; the SDK's default chain would find the credentials but not the endpoint,
and it looks for `AWS_REGION`, not `AWS_DEFAULT_REGION`:

| Variable | Meaning |
|---|---|
| `AWS_S3_BUCKET_NAME` | bucket name |
| `AWS_ENDPOINT_URL` | S3 API endpoint (`https://t3.storageapi.dev` on Railway) |
| `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` | credentials |
| `AWS_DEFAULT_REGION` | `auto` on Railway |
| `AWS_S3_PATH_STYLE` | optional; `true` for buckets needing path-style URLs (MinIO; older Railway buckets). Default virtual-hosted |

`isS3PhotoStorageConfigured()` is what the service checks to pick S3 vs the local store.
`createLocalPhotoStorageClient(dir, publicBaseUrl)`.

## Testing
- `S3PhotoStorageClientTest` — Testcontainers **MinIO** (`quay.io/minio/minio`; MinIO left Docker Hub) with
  path-style on: put/get/delete, a real HTTP GET of the presigned URL with no credentials, and the memo
  (asserted by counting presigner calls with an injected clock — the presigner stamps wall-clock time, so
  two signings in one second produce identical URLs and comparing strings proves nothing).
- `LocalFilePhotoStorageClientTest` — round trip in a temp dir, unsafe keys refused, extension → media type.
- `src/test/resources/docker-java.properties` (`api.version=1.44`) is needed for Testcontainers to talk to
  Docker Desktop here, same as `recipe-client`.
