package com.acme.clients.invitationclient

import com.acme.clients.common.Result
import com.acme.clients.invitationclient.api.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID

@Testcontainers
class JdbiInvitationClientTest {

    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("camper_db")
            .withUsername("postgres")
            .withPassword("postgres")

        private lateinit var client: InvitationClient

        @BeforeAll
        @JvmStatic
        fun setup() {
            com.acme.clients.invitationclient.test.InvitationTestDb.cleanAndMigrate(
                postgres.jdbcUrl, postgres.username, postgres.password
            )
            System.setProperty("DB_URL", postgres.jdbcUrl)
            System.setProperty("DB_USER", postgres.username)
            System.setProperty("DB_PASSWORD", postgres.password)
            client = createInvitationClient()
        }
    }

    private val planId = UUID.randomUUID()
    private val userId = UUID.randomUUID()
    private val inviterId = UUID.randomUUID()

    @BeforeEach
    fun truncate() {
        val jdbi = org.jdbi.v3.core.Jdbi.create(postgres.jdbcUrl, postgres.username, postgres.password)
        jdbi.withHandle<Unit, Exception> { handle ->
            handle.createUpdate("TRUNCATE TABLE invitations CASCADE").execute()
            // Seed required FK references
            handle.createUpdate("DELETE FROM users WHERE id IN (:userId, :inviterId)")
                .bind("userId", userId)
                .bind("inviterId", inviterId)
                .execute()
            handle.createUpdate("INSERT INTO users (id, email) VALUES (:id, :email) ON CONFLICT DO NOTHING")
                .bind("id", userId)
                .bind("email", "user-${userId}@example.com")
                .execute()
            handle.createUpdate("INSERT INTO users (id, email) VALUES (:id, :email) ON CONFLICT DO NOTHING")
                .bind("id", inviterId)
                .bind("email", "inviter-${inviterId}@example.com")
                .execute()
            handle.createUpdate("INSERT INTO plans (id, name, owner_id) VALUES (:id, :name, :ownerId) ON CONFLICT DO NOTHING")
                .bind("id", planId)
                .bind("name", "Test Plan")
                .bind("ownerId", inviterId)
                .execute()
        }
    }

    @Nested
    inner class Upsert {
        @Test
        fun `upsert creates new invitation`() {
            val result = client.upsert(
                UpsertInvitationParam(
                    planId = planId, userId = userId, email = "test@example.com",
                    inviterId = inviterId, resendEmailId = null, status = "pending"
                )
            )

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val invitation = (result as Result.Success).value
            assertThat(invitation.planId).isEqualTo(planId)
            assertThat(invitation.userId).isEqualTo(userId)
            assertThat(invitation.email).isEqualTo("test@example.com")
            assertThat(invitation.status).isEqualTo("pending")
            assertThat(invitation.resendEmailId).isNull()
        }

        @Test
        fun `upsert updates existing invitation on conflict`() {
            client.upsert(
                UpsertInvitationParam(
                    planId = planId, userId = userId, email = "test@example.com",
                    inviterId = inviterId, resendEmailId = null, status = "pending"
                )
            )

            val result = client.upsert(
                UpsertInvitationParam(
                    planId = planId, userId = userId, email = "test@example.com",
                    inviterId = inviterId, resendEmailId = "resend-123", status = "sent"
                )
            )

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val invitation = (result as Result.Success).value
            assertThat(invitation.status).isEqualTo("sent")
            assertThat(invitation.resendEmailId).isEqualTo("resend-123")
        }
    }

    @Nested
    inner class GetByPlanId {
        @Test
        fun `getByPlanId returns invitations for plan`() {
            client.upsert(
                UpsertInvitationParam(
                    planId = planId, userId = userId, email = "test@example.com",
                    inviterId = inviterId, resendEmailId = null, status = "sent"
                )
            )

            val result = client.getByPlanId(GetByPlanIdParam(planId))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val invitations = (result as Result.Success).value
            assertThat(invitations).hasSize(1)
            assertThat(invitations[0].email).isEqualTo("test@example.com")
        }

        @Test
        fun `getByPlanId returns empty for unknown plan`() {
            val result = client.getByPlanId(GetByPlanIdParam(UUID.randomUUID()))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            assertThat((result as Result.Success).value).isEmpty()
        }
    }

    @Nested
    inner class GetByPlanIdAndUserId {
        @Test
        fun `getByPlanIdAndUserId returns invitation when exists`() {
            client.upsert(
                UpsertInvitationParam(
                    planId = planId, userId = userId, email = "test@example.com",
                    inviterId = inviterId, resendEmailId = null, status = "sent"
                )
            )

            val result = client.getByPlanIdAndUserId(GetByPlanIdAndUserIdParam(planId, userId))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val invitation = (result as Result.Success).value
            assertThat(invitation).isNotNull
            assertThat(invitation!!.email).isEqualTo("test@example.com")
        }

        @Test
        fun `getByPlanIdAndUserId returns null when not found`() {
            val result = client.getByPlanIdAndUserId(GetByPlanIdAndUserIdParam(planId, UUID.randomUUID()))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            assertThat((result as Result.Success).value).isNull()
        }
    }

    @Nested
    inner class GetByResendEmailId {
        @Test
        fun `getByResendEmailId returns invitation when exists`() {
            client.upsert(
                UpsertInvitationParam(
                    planId = planId, userId = userId, email = "test@example.com",
                    inviterId = inviterId, resendEmailId = "resend-abc", status = "sent"
                )
            )

            val result = client.getByResendEmailId(GetByResendEmailIdParam("resend-abc"))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val invitation = (result as Result.Success).value
            assertThat(invitation).isNotNull
            assertThat(invitation!!.planId).isEqualTo(planId)
        }

        @Test
        fun `getByResendEmailId returns null when not found`() {
            val result = client.getByResendEmailId(GetByResendEmailIdParam("nonexistent"))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            assertThat((result as Result.Success).value).isNull()
        }
    }

    @Nested
    inner class UpdateStatus {
        @Test
        fun `updateStatus updates status and resendEmailId`() {
            val created = (client.upsert(
                UpsertInvitationParam(
                    planId = planId, userId = userId, email = "test@example.com",
                    inviterId = inviterId, resendEmailId = null, status = "pending"
                )
            ) as Result.Success).value

            val result = client.updateStatus(
                UpdateStatusParam(id = created.id, status = "sent", resendEmailId = "resend-xyz")
            )

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val updated = (result as Result.Success).value
            assertThat(updated.status).isEqualTo("sent")
            assertThat(updated.resendEmailId).isEqualTo("resend-xyz")
        }

        @Test
        fun `updateStatus returns NotFound for unknown id`() {
            val result = client.updateStatus(
                UpdateStatusParam(id = UUID.randomUUID(), status = "delivered")
            )

            assertThat(result).isInstanceOf(Result.Failure::class.java)
        }
    }
}
