package com.acme.clients.planclient

import com.acme.clients.common.Result
import com.acme.clients.common.error.ConflictError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.error.ValidationError
import com.acme.clients.planclient.api.AddMemberParam
import com.acme.clients.planclient.api.CreatePlanParam
import com.acme.clients.planclient.api.DeletePlanParam
import com.acme.clients.planclient.api.GetByIdParam
import com.acme.clients.planclient.api.GetByUserIdParam
import com.acme.clients.planclient.api.GetMembersParam
import com.acme.clients.planclient.api.GetPublicPlansParam
import com.acme.clients.planclient.api.PlanClient
import com.acme.clients.planclient.api.RemoveMemberParam
import com.acme.clients.planclient.api.UpdatePlanParam
import com.acme.clients.planclient.test.PlanTestDb
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
class JdbiPlanClientTest {

    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("camper_db")
            .withUsername("postgres")
            .withPassword("postgres")

        private lateinit var client: PlanClient
        private lateinit var jdbi: org.jdbi.v3.core.Jdbi

        @BeforeAll
        @JvmStatic
        fun setup() {
            PlanTestDb.cleanAndMigrate(postgres.jdbcUrl, postgres.username, postgres.password)

            System.setProperty("DB_URL", postgres.jdbcUrl)
            System.setProperty("DB_USER", postgres.username)
            System.setProperty("DB_PASSWORD", postgres.password)
            client = createPlanClient()
            jdbi = org.jdbi.v3.core.Jdbi.create(postgres.jdbcUrl, postgres.username, postgres.password)
        }
    }

    private lateinit var ownerId: UUID
    private lateinit var otherUserId: UUID

    @BeforeEach
    fun truncateAndSeed() {
        jdbi.withHandle<Unit, Exception> { handle ->
            handle.createUpdate("TRUNCATE TABLE plan_members, plans, users CASCADE").execute()
        }
        ownerId = UUID.randomUUID()
        otherUserId = UUID.randomUUID()
        jdbi.withHandle<Unit, Exception> { handle ->
            handle.createUpdate("INSERT INTO users (id, email, username) VALUES (:id, :email, :username)")
                .bind("id", ownerId).bind("email", "owner@example.com").bind("username", "owner").execute()
            handle.createUpdate("INSERT INTO users (id, email, username) VALUES (:id, :email, :username)")
                .bind("id", otherUserId).bind("email", "other@example.com").bind("username", "other").execute()
        }
    }

    @Nested
    inner class GetById {
        @Test
        fun `getById returns plan when it exists`() {
            val created = (client.create(CreatePlanParam(name = "Trip", visibility = "private", ownerId = ownerId)) as Result.Success).value

            val result = client.getById(GetByIdParam(created.id))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val found = (result as Result.Success).value
            assertThat(found.name).isEqualTo("Trip")
            assertThat(found.visibility).isEqualTo("private")
            assertThat(found.ownerId).isEqualTo(ownerId)
        }

        @Test
        fun `getById returns NotFoundError when plan does not exist`() {
            val result = client.getById(GetByIdParam(UUID.randomUUID()))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(NotFoundError::class.java)
        }
    }

    @Nested
    inner class GetByUserId {
        @Test
        fun `getByUserId returns plans the user is a member of`() {
            val plan = (client.create(CreatePlanParam(name = "Camp", visibility = "private", ownerId = ownerId)) as Result.Success).value
            client.addMember(AddMemberParam(planId = plan.id, userId = ownerId))

            val result = client.getByUserId(GetByUserIdParam(ownerId))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val plans = (result as Result.Success).value
            assertThat(plans).hasSize(1)
            assertThat(plans[0].name).isEqualTo("Camp")
        }

        @Test
        fun `getByUserId returns empty list when user has no plans`() {
            val result = client.getByUserId(GetByUserIdParam(UUID.randomUUID()))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val plans = (result as Result.Success).value
            assertThat(plans).isEmpty()
        }
    }

    @Nested
    inner class GetPublicPlans {
        @Test
        fun `getPublicPlans returns only public plans`() {
            client.create(CreatePlanParam(name = "Private Trip", visibility = "private", ownerId = ownerId))
            client.create(CreatePlanParam(name = "Public Trip", visibility = "public", ownerId = ownerId))

            val result = client.getPublicPlans(GetPublicPlansParam())
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val plans = (result as Result.Success).value
            assertThat(plans).hasSize(1)
            assertThat(plans[0].name).isEqualTo("Public Trip")
        }

        @Test
        fun `getPublicPlans returns empty list when no public plans exist`() {
            client.create(CreatePlanParam(name = "Private Only", visibility = "private", ownerId = ownerId))

            val result = client.getPublicPlans(GetPublicPlansParam())
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val plans = (result as Result.Success).value
            assertThat(plans).isEmpty()
        }
    }

    @Nested
    inner class Create {
        @Test
        fun `create returns created plan`() {
            val result = client.create(CreatePlanParam(name = "Beach Trip", visibility = "private", ownerId = ownerId))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val plan = (result as Result.Success).value
            assertThat(plan.name).isEqualTo("Beach Trip")
            assertThat(plan.visibility).isEqualTo("private")
            assertThat(plan.ownerId).isEqualTo(ownerId)
            assertThat(plan.id).isNotNull()
            assertThat(plan.createdAt).isNotNull()
        }

        @Test
        fun `create returns ValidationError for blank name`() {
            val result = client.create(CreatePlanParam(name = "", visibility = "private", ownerId = ownerId))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
            assertThat((error as ValidationError).field).isEqualTo("name")
        }

        @Test
        fun `create returns ValidationError for invalid visibility`() {
            val result = client.create(CreatePlanParam(name = "Trip", visibility = "hidden", ownerId = ownerId))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
            assertThat((error as ValidationError).field).isEqualTo("visibility")
        }
    }

    @Nested
    inner class Update {
        @Test
        fun `update returns updated plan`() {
            val created = (client.create(CreatePlanParam(name = "Old Name", visibility = "private", ownerId = ownerId)) as Result.Success).value

            val result = client.update(UpdatePlanParam(id = created.id, name = "New Name"))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val updated = (result as Result.Success).value
            assertThat(updated.name).isEqualTo("New Name")
        }

        @Test
        fun `update returns NotFoundError when plan does not exist`() {
            val result = client.update(UpdatePlanParam(id = UUID.randomUUID(), name = "Nope"))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(NotFoundError::class.java)
        }

        @Test
        fun `update returns ValidationError for blank name`() {
            val created = (client.create(CreatePlanParam(name = "Trip", visibility = "private", ownerId = ownerId)) as Result.Success).value

            val result = client.update(UpdatePlanParam(id = created.id, name = ""))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `delete returns success when plan exists`() {
            val created = (client.create(CreatePlanParam(name = "Doomed", visibility = "private", ownerId = ownerId)) as Result.Success).value

            val result = client.delete(DeletePlanParam(created.id))
            assertThat(result).isInstanceOf(Result.Success::class.java)

            val getResult = client.getById(GetByIdParam(created.id))
            assertThat(getResult).isInstanceOf(Result.Failure::class.java)
        }

        @Test
        fun `delete returns NotFoundError when plan does not exist`() {
            val result = client.delete(DeletePlanParam(UUID.randomUUID()))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(NotFoundError::class.java)
        }

        @Test
        fun `delete cascades to plan members`() {
            val plan = (client.create(CreatePlanParam(name = "Cascade", visibility = "private", ownerId = ownerId)) as Result.Success).value
            client.addMember(AddMemberParam(planId = plan.id, userId = ownerId))

            client.delete(DeletePlanParam(plan.id))

            val members = client.getMembers(GetMembersParam(plan.id))
            assertThat(members).isInstanceOf(Result.Success::class.java)
            assertThat((members as Result.Success).value).isEmpty()
        }
    }

    @Nested
    inner class Members {
        @Test
        fun `addMember and getMembers work together`() {
            val plan = (client.create(CreatePlanParam(name = "Group Trip", visibility = "private", ownerId = ownerId)) as Result.Success).value

            val addResult = client.addMember(AddMemberParam(planId = plan.id, userId = ownerId))
            assertThat(addResult).isInstanceOf(Result.Success::class.java)
            val member = (addResult as Result.Success).value
            assertThat(member.planId).isEqualTo(plan.id)
            assertThat(member.userId).isEqualTo(ownerId)

            val membersResult = client.getMembers(GetMembersParam(plan.id))
            assertThat(membersResult).isInstanceOf(Result.Success::class.java)
            val members = (membersResult as Result.Success).value
            assertThat(members).hasSize(1)
            assertThat(members[0].userId).isEqualTo(ownerId)
        }

        @Test
        fun `addMember returns ConflictError for duplicate membership`() {
            val plan = (client.create(CreatePlanParam(name = "Dup", visibility = "private", ownerId = ownerId)) as Result.Success).value
            client.addMember(AddMemberParam(planId = plan.id, userId = ownerId))

            val result = client.addMember(AddMemberParam(planId = plan.id, userId = ownerId))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ConflictError::class.java)
        }

        @Test
        fun `removeMember removes the member`() {
            val plan = (client.create(CreatePlanParam(name = "Leave", visibility = "private", ownerId = ownerId)) as Result.Success).value
            client.addMember(AddMemberParam(planId = plan.id, userId = ownerId))

            val result = client.removeMember(RemoveMemberParam(planId = plan.id, userId = ownerId))
            assertThat(result).isInstanceOf(Result.Success::class.java)

            val membersResult = client.getMembers(GetMembersParam(plan.id))
            assertThat((membersResult as Result.Success).value).isEmpty()
        }

        @Test
        fun `removeMember returns NotFoundError when membership does not exist`() {
            val plan = (client.create(CreatePlanParam(name = "NoMember", visibility = "private", ownerId = ownerId)) as Result.Success).value

            val result = client.removeMember(RemoveMemberParam(planId = plan.id, userId = otherUserId))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(NotFoundError::class.java)
        }

        @Test
        fun `getMembers returns empty list when plan has no members`() {
            val plan = (client.create(CreatePlanParam(name = "Empty", visibility = "private", ownerId = ownerId)) as Result.Success).value

            val result = client.getMembers(GetMembersParam(plan.id))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            assertThat((result as Result.Success).value).isEmpty()
        }
    }
}
