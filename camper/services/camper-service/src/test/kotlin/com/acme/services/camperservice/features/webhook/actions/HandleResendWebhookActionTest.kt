package com.acme.services.camperservice.features.webhook.actions

import com.acme.clients.common.Result
import com.acme.clients.invitationclient.api.GetByResendEmailIdParam
import com.acme.clients.invitationclient.fake.FakeInvitationClient
import com.acme.clients.invitationclient.model.Invitation
import com.acme.services.camperservice.features.webhook.dto.ResendWebhookData
import com.acme.services.camperservice.features.webhook.dto.ResendWebhookEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class HandleResendWebhookActionTest {

    private val fakeInvitationClient = FakeInvitationClient()
    private val action = HandleResendWebhookAction(fakeInvitationClient)

    @BeforeEach
    fun setUp() {
        fakeInvitationClient.reset()
    }

    private fun seedInvitation(resendEmailId: String, status: String = "sent"): Invitation {
        val invitation = Invitation(
            id = UUID.randomUUID(),
            planId = UUID.randomUUID(),
            userId = UUID.randomUUID(),
            email = "test@example.com",
            inviterId = UUID.randomUUID(),
            resendEmailId = resendEmailId,
            status = status,
            sentAt = Instant.now(),
            updatedAt = Instant.now()
        )
        fakeInvitationClient.seed(invitation)
        return invitation
    }

    private fun event(type: String, emailId: String) = ResendWebhookEvent(
        type = type,
        createdAt = "2026-03-08T00:00:00Z",
        data = ResendWebhookData(emailId = emailId, from = null, to = null, subject = null)
    )

    @Test
    fun `updates status to delivered on email delivered event`() {
        val inv = seedInvitation("email-123")

        action.execute(event("email.delivered", "email-123"))

        val updated = (fakeInvitationClient.getByResendEmailId(GetByResendEmailIdParam("email-123")) as Result.Success).value
        assertThat(updated!!.status).isEqualTo("delivered")
    }

    @Test
    fun `updates status to bounced on email bounced event`() {
        val inv = seedInvitation("email-456")

        action.execute(event("email.bounced", "email-456"))

        val updated = (fakeInvitationClient.getByResendEmailId(GetByResendEmailIdParam("email-456")) as Result.Success).value
        assertThat(updated!!.status).isEqualTo("bounced")
    }

    @Test
    fun `updates status to delayed on email delivery_delayed event`() {
        seedInvitation("email-789")

        action.execute(event("email.delivery_delayed", "email-789"))

        val updated = (fakeInvitationClient.getByResendEmailId(GetByResendEmailIdParam("email-789")) as Result.Success).value
        assertThat(updated!!.status).isEqualTo("delayed")
    }

    @Test
    fun `updates status to complained on email complained event`() {
        seedInvitation("email-abc")

        action.execute(event("email.complained", "email-abc"))

        val updated = (fakeInvitationClient.getByResendEmailId(GetByResendEmailIdParam("email-abc")) as Result.Success).value
        assertThat(updated!!.status).isEqualTo("complained")
    }

    @Test
    fun `ignores unsupported event types`() {
        seedInvitation("email-xyz")

        action.execute(event("email.opened", "email-xyz"))

        // Status should remain unchanged
        val unchanged = (fakeInvitationClient.getByResendEmailId(GetByResendEmailIdParam("email-xyz")) as Result.Success).value
        assertThat(unchanged!!.status).isEqualTo("sent")
    }

    @Test
    fun `ignores events for unknown email IDs`() {
        // No exception thrown, just returns silently
        action.execute(event("email.delivered", "nonexistent-email-id"))
    }
}
