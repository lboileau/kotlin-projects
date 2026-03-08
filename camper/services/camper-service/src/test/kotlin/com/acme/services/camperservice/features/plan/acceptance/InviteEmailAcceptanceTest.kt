package com.acme.services.camperservice.features.plan.acceptance

import com.acme.clients.emailclient.api.EmailClient
import com.acme.clients.emailclient.fake.FakeEmailClient
import com.acme.clients.invitationclient.api.InvitationClient
import com.acme.clients.invitationclient.fake.FakeInvitationClient
import com.acme.services.camperservice.config.TestContainerConfig
import com.acme.services.camperservice.features.plan.acceptance.fixture.PlanFixture
import com.acme.services.camperservice.features.plan.dto.AddMemberRequest
import com.acme.services.camperservice.features.plan.dto.PlanMemberResponse
import com.acme.services.camperservice.features.webhook.dto.ResendWebhookData
import com.acme.services.camperservice.features.webhook.dto.ResendWebhookEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Import
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestContainerConfig::class)
class InviteEmailAcceptanceTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    lateinit var emailClient: EmailClient

    @Autowired
    lateinit var invitationClient: InvitationClient

    private val fakeEmailClient get() = emailClient as FakeEmailClient
    private val fakeInvitationClient get() = invitationClient as FakeInvitationClient

    private lateinit var fixture: PlanFixture
    private lateinit var ownerId: UUID
    private lateinit var otherUserId: UUID

    @BeforeEach
    fun setUp() {
        fixture = PlanFixture(jdbcTemplate)
        fixture.truncateAll()
        fakeEmailClient.reset()
        fakeInvitationClient.reset()
        ownerId = fixture.insertUser(email = "owner@example.com", username = "owner")
        otherUserId = fixture.insertUser(email = "other@example.com", username = "other")
    }

    @Nested
    inner class AddMemberSendsEmail {

        @Test
        fun `adding member sends invitation email`() {
            val planId = fixture.insertPlan(name = "Camping Trip", ownerId = ownerId)

            val response = restTemplate.exchange(
                "/api/plans/$planId/members",
                HttpMethod.POST,
                entityWithUser(AddMemberRequest(email = "other@example.com"), ownerId),
                PlanMemberResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            assertThat(fakeEmailClient.sentEmails).hasSize(1)
            val sent = fakeEmailClient.sentEmails[0]
            assertThat(sent.to).isEqualTo("other@example.com")
            assertThat(sent.subject).contains("Camping Trip")
            assertThat(sent.html).contains("owner")
        }

        @Test
        fun `getMembers returns invitation status after adding member`() {
            val planId = fixture.insertPlan(name = "Trip", ownerId = ownerId)

            restTemplate.exchange(
                "/api/plans/$planId/members",
                HttpMethod.POST,
                entityWithUser(AddMemberRequest(email = "other@example.com"), ownerId),
                PlanMemberResponse::class.java
            )

            val membersResponse = restTemplate.exchange(
                "/api/plans/$planId/members",
                HttpMethod.GET,
                entityWithUser(null, ownerId),
                Array<PlanMemberResponse>::class.java
            )

            assertThat(membersResponse.statusCode).isEqualTo(HttpStatus.OK)
            val invited = membersResponse.body!!.find { it.userId == otherUserId }
            assertThat(invited).isNotNull
            assertThat(invited!!.invitationStatus).isEqualTo("sent")
        }
    }

    @Nested
    inner class ResendWebhook {

        @Test
        fun `POST webhook returns 200 OK`() {
            val event = ResendWebhookEvent(
                type = "email.delivered",
                createdAt = "2026-03-08T00:00:00Z",
                data = ResendWebhookData(emailId = "nonexistent", from = null, to = null, subject = null)
            )

            val response = restTemplate.exchange(
                "/api/webhooks/resend",
                HttpMethod.POST,
                HttpEntity(event),
                Void::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        }

        @Test
        fun `webhook updates invitation status via full flow`() {
            val planId = fixture.insertPlan(name = "Trip", ownerId = ownerId)

            // Add member (creates invitation with status "sent")
            restTemplate.exchange(
                "/api/plans/$planId/members",
                HttpMethod.POST,
                entityWithUser(AddMemberRequest(email = "other@example.com"), ownerId),
                PlanMemberResponse::class.java
            )

            // Get the emailId from the fake invitation client
            val invitations = (fakeInvitationClient.getByPlanId(
                com.acme.clients.invitationclient.api.GetByPlanIdParam(planId)
            ) as com.acme.clients.common.Result.Success).value
            assertThat(invitations).hasSize(1)
            val emailId = invitations[0].resendEmailId!!

            // Send webhook to mark as delivered
            val event = ResendWebhookEvent(
                type = "email.delivered",
                createdAt = "2026-03-08T00:00:00Z",
                data = ResendWebhookData(emailId = emailId, from = null, to = null, subject = null)
            )
            val webhookResponse = restTemplate.exchange(
                "/api/webhooks/resend",
                HttpMethod.POST,
                HttpEntity(event),
                Void::class.java
            )
            assertThat(webhookResponse.statusCode).isEqualTo(HttpStatus.OK)

            // Verify invitation status updated
            val updated = (fakeInvitationClient.getByPlanId(
                com.acme.clients.invitationclient.api.GetByPlanIdParam(planId)
            ) as com.acme.clients.common.Result.Success).value
            assertThat(updated[0].status).isEqualTo("delivered")
        }
    }

    private fun entityWithUser(body: Any?, userId: UUID): HttpEntity<Any?> {
        val headers = HttpHeaders()
        headers.set("X-User-Id", userId.toString())
        return HttpEntity(body, headers)
    }
}
