# Feature: Invite Emails

## Summary

When a member is added to a plan, send them a colourful, adventure-themed invitation email via [Resend](https://resend.com). Track each invitation's status in a new `invitations` table, updated in real-time via Resend webhooks. Surface invitation status on the frontend — pending avatars show a failed/bounced state with a hover tooltip when the email failed to send or was rejected.

In local development, the email client is faked out: no real emails are sent, and invitations are recorded as `delivered` immediately.

## Approach

**Full delivery tracking via webhooks.** The Resend `send()` call returns an email ID synchronously. A webhook endpoint (`POST /api/webhooks/resend`) receives delivery events from Resend to update the invitation status as the email progresses through the delivery pipeline.

### Status Lifecycle

```
upsert invitation  →  status: "pending"
                          ↓
send() succeeds    →  status: "sent"
                          ↓
              webhook: email.delivered  →  status: "delivered"
              webhook: email.bounced    →  status: "bounced"
              webhook: email.complained →  status: "complained"
              webhook: email.delivery_delayed → status: "delayed"

send() fails       →  status: "failed"
```

## Entities

### Invitation

| Field | Type | Notes |
|-------|------|-------|
| id | UUID | PK |
| plan_id | UUID | FK → plans, ON DELETE CASCADE |
| user_id | UUID | FK → users, ON DELETE CASCADE |
| email | VARCHAR(255) | Recipient email (denormalized for display) |
| inviter_id | UUID | FK → users |
| resend_email_id | VARCHAR(255) | Nullable, returned by Resend on success |
| status | VARCHAR(20) | `pending`, `sent`, `delivered`, `bounced`, `failed`, `complained`, `delayed` |
| sent_at | TIMESTAMPTZ | When the send was attempted |
| updated_at | TIMESTAMPTZ | |

Constraints:
- `UNIQUE(plan_id, user_id)` — one invitation record per member per plan
- Index on `resend_email_id` — for fast webhook lookups

## Email Content

**From:** Configurable via `EMAIL_FROM` env var (e.g., `Camper <invites@yourdomain.com>`)

**Subject:** `You're invited to {planName}!`

**Body (HTML):** Colourful, adventure-themed email:
- Headline: `{inviterName} is inviting you to join them on {planName}!`
- Subtext: `Join them to see all the action.`
- CTA button linking to the app (plan page)
- Footer: `This email is from Louis. If you have any concerns, please contact him directly.`

The email should use warm, outdoorsy colours (greens, oranges, earth tones) and feel fun and inviting — matching the camping/adventure tone of the app.

## API Changes

### Modified Endpoints

**`POST /api/plans/{planId}/members`** — No changes to the request/response contract. The email is sent as a side effect after the member is added. If the email fails, the member is still added but the invitation status is `failed`.

**`GET /api/plans/{planId}/members`** — Add `invitationStatus: String?` to `PlanMemberResponse`. Values: `sent`, `delivered`, `bounced`, `failed`, `complained`, `delayed`, or `null` (for members added before this feature / the plan owner).

### New Endpoint

**`POST /api/webhooks/resend`** — Receives Resend webhook events. No authentication header required (Resend calls this). Verifies the event via `svix-id` header for idempotency.

Request body (from Resend):
```json
{
  "type": "email.delivered",
  "created_at": "2024-02-22T23:41:12.126Z",
  "data": {
    "email_id": "56761188-7520-42d8-8898-ff6fc54ce618",
    "from": "Camper <invites@example.com>",
    "to": ["user@example.com"],
    "subject": "You're invited to Summer Trip!"
  }
}
```

Supported event types: `email.sent`, `email.delivered`, `email.bounced`, `email.delivery_delayed`, `email.complained`.

Flow:
1. Parse event type and extract `email_id` from `data.email_id`
2. Look up invitation by `resend_email_id`
3. Update invitation status based on event type
4. Return 200 OK (always — even if invitation not found, to avoid Resend retries)

## Client Interfaces

### EmailClient (new)

```kotlin
interface EmailClient {
    fun send(param: SendEmailParam): Result<SendEmailResult, AppError>
}

data class SendEmailParam(
    val to: String,
    val subject: String,
    val html: String
)

data class SendEmailResult(
    val emailId: String
)
```

- **Real implementation:** Wraps Resend Java SDK (`com.resend:resend-java`)
- **Fake (testFixtures + local dev):** Records calls, returns success with a fake email ID, no actual sending

### InvitationClient (new)

```kotlin
interface InvitationClient {
    fun upsert(param: UpsertInvitationParam): Result<Invitation, AppError>
    fun getByPlanId(param: GetByPlanIdParam): Result<List<Invitation>, AppError>
    fun getByPlanIdAndUserId(param: GetByPlanIdAndUserIdParam): Result<Invitation?, AppError>
    fun getByResendEmailId(param: GetByResendEmailIdParam): Result<Invitation, AppError>
    fun updateStatus(param: UpdateStatusParam): Result<Invitation, AppError>
}
```

`upsert` uses `INSERT ... ON CONFLICT (plan_id, user_id) DO UPDATE` to handle both create and re-send cases in a single operation.

## Invitation Deduplication

When a member is added to a plan, check for an existing invitation record before sending:

| Existing Status | Action |
|----------------|--------|
| _(no record)_ | Send email, create invitation with status `sent`/`failed` |
| `sent` | Skip — email already in flight |
| `delayed` | Skip — email still being retried by Resend |
| `delivered` | Skip — email already delivered successfully |
| `complained` | Skip — recipient marked as spam, do not re-send |
| `failed` | Re-send — previous attempt failed |
| `bounced` | Re-send — maybe a transient issue, worth retrying |

On re-send: update the existing record's `resend_email_id`, `status`, `inviter_id`, and `sent_at`. No duplicate rows.

## Service Changes

### AddPlanMemberAction (modified)

Updated flow:
1. Validate input
2. Get or create user by email
3. Add member to plan (handles `AlreadyMember` — if already a member, still proceed to check invitation status)
4. Upsert invitation record with status `pending`
5. If previous invitation exists and status is `sent`, `delayed`, `delivered`, or `complained` → skip email, return member
6. Fetch plan name (via `planClient.getById`)
7. Fetch inviter name (via `userClient.getById`)
8. Send invitation email (via `emailClient.send`)
9. Update invitation status to `sent` with `resend_email_id` (or `failed` if send throws)
10. Return member (regardless of email success/failure)

### GetPlanMembersAction (modified)

Updated flow:
1. Fetch members (existing)
2. Fetch invitations for the plan (new)
3. Enrich each member with invitation status

### AddPlanMemberParam (updated)

Add `requestingUserId: UUID` — needed to look up the inviter's name for the email.

### HandleResendWebhookAction (new)

New action in a `webhook` feature:
1. Parse the Resend event payload
2. Map event type to invitation status (`email.delivered` → `delivered`, `email.bounced` → `bounced`, etc.)
3. Look up invitation by `resend_email_id` via `invitationClient.getByResendEmailId`
4. Update status via `invitationClient.updateStatus`
5. Return success (or silently ignore if invitation not found)

### WebhookController (new)

- `POST /api/webhooks/resend` — receives Resend events, delegates to `HandleResendWebhookAction`
- Always returns 200 OK
- No `X-User-Id` header required

## Frontend Changes

### PlanMember TypeScript type

Add `invitationStatus: string | null` field.

### CamperAvatar

Add a `failed` invitation state:
- When `invitationStatus` is `failed`, `bounced`, or `complained`: show a visual indicator (e.g., red tint or warning icon on the pending ghost avatar)
- Hover tooltip: `Failed to invite {email}`

### PlanPage

Pass `invitationStatus` and `email` through to `CamperAvatar`.

## Configuration

| Env Var | Description | Local Default |
|---------|-------------|---------------|
| `RESEND_API_KEY` | Resend API key | not set (fake client used) |
| `EMAIL_FROM` | Sender address | `Camper <noreply@example.com>` |
| `APP_BASE_URL` | Base URL for links in emails | `http://localhost:5173` |

When `RESEND_API_KEY` is not set, the app uses `FakeEmailClient` (logs + returns success). In production, the real Resend client is used.

**Resend Dashboard setup (manual):** Register the webhook URL `https://camper-service-production.up.railway.app/api/webhooks/resend` and subscribe to `email.sent`, `email.delivered`, `email.bounced`, `email.delivery_delayed`, `email.complained`.

## PR Stack

```
1.  [plan]           feat(invite-email): plan — this document
2.  [db]             feat(invite-email): db — invitations table schema + migration
3.  [client]         feat(invite-email): email client contracts — interface, params, model, fake
4.  [client]         feat(invite-email): invitation client contracts — interface, params, model, fake
5.  [service]        feat(invite-email): service contracts — updated DTOs, params, action signatures, webhook controller
6.  [db-impl]        feat(invite-email): db implementation — seed data
7.  [client-impl]    feat(invite-email): email client implementation — Resend SDK
8.  [client-impl]    feat(invite-email): invitation client implementation — JDBI operations
9.  [service-impl]   feat(invite-email): service implementation — send email on invite, enrich members, webhook handler
10. [frontend]       feat(invite-email): frontend — failed invitation status on avatars
11. [client-test]    feat(invite-email): invitation client tests
12. [service-test]   feat(invite-email): service tests
13. [acceptance]     feat(invite-email): acceptance tests
14. [docs]           feat(invite-email): update documentation
```

## Open Questions

None — ready for implementation.
