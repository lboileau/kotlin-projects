package com.acme.clients.assignmentclient

import com.acme.clients.assignmentclient.api.AddAssignmentMemberParam
import com.acme.clients.assignmentclient.api.AssignmentClient
import com.acme.clients.assignmentclient.api.CreateAssignmentParam
import com.acme.clients.assignmentclient.api.DeleteAssignmentParam
import com.acme.clients.assignmentclient.api.GetAssignmentMembersParam
import com.acme.clients.assignmentclient.api.GetByIdParam
import com.acme.clients.assignmentclient.api.GetByPlanIdParam
import com.acme.clients.assignmentclient.api.RemoveAssignmentMemberParam
import com.acme.clients.assignmentclient.api.TransferOwnershipParam
import com.acme.clients.assignmentclient.api.UpdateAssignmentParam
import com.acme.clients.assignmentclient.test.AssignmentTestDb
import com.acme.clients.common.Result
import com.acme.clients.common.error.ConflictError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.error.ValidationError
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
class JdbiAssignmentClientTest {

    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("camper_db")
            .withUsername("postgres")
            .withPassword("postgres")

        private lateinit var client: AssignmentClient
        private lateinit var jdbi: org.jdbi.v3.core.Jdbi

        @BeforeAll
        @JvmStatic
        fun setup() {
            AssignmentTestDb.cleanAndMigrate(postgres.jdbcUrl, postgres.username, postgres.password)

            System.setProperty("DB_URL", postgres.jdbcUrl)
            System.setProperty("DB_USER", postgres.username)
            System.setProperty("DB_PASSWORD", postgres.password)
            client = createAssignmentClient()
            jdbi = org.jdbi.v3.core.Jdbi.create(postgres.jdbcUrl, postgres.username, postgres.password)
        }
    }

    private lateinit var ownerId: UUID
    private lateinit var otherUserId: UUID
    private lateinit var planId: UUID

    @BeforeEach
    fun truncateAndSeed() {
        jdbi.withHandle<Unit, Exception> { handle ->
            handle.createUpdate("TRUNCATE TABLE assignment_members, assignments, plan_members, plans, users CASCADE").execute()
        }
        ownerId = UUID.randomUUID()
        otherUserId = UUID.randomUUID()
        planId = UUID.randomUUID()
        jdbi.withHandle<Unit, Exception> { handle ->
            handle.createUpdate("INSERT INTO users (id, email, username) VALUES (:id, :email, :username)")
                .bind("id", ownerId).bind("email", "owner@example.com").bind("username", "owner").execute()
            handle.createUpdate("INSERT INTO users (id, email, username) VALUES (:id, :email, :username)")
                .bind("id", otherUserId).bind("email", "other@example.com").bind("username", "other").execute()
            handle.createUpdate("INSERT INTO plans (id, name, visibility, owner_id) VALUES (:id, :name, :visibility, :ownerId)")
                .bind("id", planId).bind("name", "Test Plan").bind("visibility", "private").bind("ownerId", ownerId).execute()
        }
    }

    @Nested
    inner class Create {
        @Test
        fun `create returns created assignment`() {
            val result = client.create(CreateAssignmentParam(
                planId = planId, name = "Big Tent", type = "tent", maxOccupancy = 4, ownerId = ownerId
            ))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val assignment = (result as Result.Success).value
            assertThat(assignment.name).isEqualTo("Big Tent")
            assertThat(assignment.type).isEqualTo("tent")
            assertThat(assignment.maxOccupancy).isEqualTo(4)
            assertThat(assignment.planId).isEqualTo(planId)
            assertThat(assignment.ownerId).isEqualTo(ownerId)
            assertThat(assignment.id).isNotNull()
            assertThat(assignment.createdAt).isNotNull()
        }

        @Test
        fun `create returns ConflictError for duplicate name and type in same plan`() {
            client.create(CreateAssignmentParam(
                planId = planId, name = "Tent A", type = "tent", maxOccupancy = 2, ownerId = ownerId
            ))
            val result = client.create(CreateAssignmentParam(
                planId = planId, name = "Tent A", type = "tent", maxOccupancy = 3, ownerId = ownerId
            ))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ConflictError::class.java)
        }

        @Test
        fun `create returns ValidationError for blank name`() {
            val result = client.create(CreateAssignmentParam(
                planId = planId, name = "", type = "tent", maxOccupancy = 2, ownerId = ownerId
            ))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
            assertThat((error as ValidationError).field).isEqualTo("name")
        }

        @Test
        fun `create returns ValidationError for invalid type`() {
            val result = client.create(CreateAssignmentParam(
                planId = planId, name = "Raft", type = "raft", maxOccupancy = 2, ownerId = ownerId
            ))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
            assertThat((error as ValidationError).field).isEqualTo("type")
        }
    }

    @Nested
    inner class GetById {
        @Test
        fun `getById returns assignment when it exists`() {
            val created = (client.create(CreateAssignmentParam(
                planId = planId, name = "Canoe 1", type = "canoe", maxOccupancy = 2, ownerId = ownerId
            )) as Result.Success).value

            val result = client.getById(GetByIdParam(created.id))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val found = (result as Result.Success).value
            assertThat(found.name).isEqualTo("Canoe 1")
            assertThat(found.type).isEqualTo("canoe")
            assertThat(found.planId).isEqualTo(planId)
        }

        @Test
        fun `getById returns NotFoundError when assignment does not exist`() {
            val result = client.getById(GetByIdParam(UUID.randomUUID()))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(NotFoundError::class.java)
        }
    }

    @Nested
    inner class GetByPlanId {
        @Test
        fun `getByPlanId returns all assignments for a plan`() {
            client.create(CreateAssignmentParam(
                planId = planId, name = "Tent A", type = "tent", maxOccupancy = 4, ownerId = ownerId
            ))
            client.create(CreateAssignmentParam(
                planId = planId, name = "Canoe A", type = "canoe", maxOccupancy = 2, ownerId = ownerId
            ))

            val result = client.getByPlanId(GetByPlanIdParam(planId))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val assignments = (result as Result.Success).value
            assertThat(assignments).hasSize(2)
        }

        @Test
        fun `getByPlanId filters by type`() {
            client.create(CreateAssignmentParam(
                planId = planId, name = "Tent A", type = "tent", maxOccupancy = 4, ownerId = ownerId
            ))
            client.create(CreateAssignmentParam(
                planId = planId, name = "Canoe A", type = "canoe", maxOccupancy = 2, ownerId = ownerId
            ))

            val result = client.getByPlanId(GetByPlanIdParam(planId, type = "tent"))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val assignments = (result as Result.Success).value
            assertThat(assignments).hasSize(1)
            assertThat(assignments[0].type).isEqualTo("tent")
        }

        @Test
        fun `getByPlanId returns empty list when plan has no assignments`() {
            val result = client.getByPlanId(GetByPlanIdParam(planId))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val assignments = (result as Result.Success).value
            assertThat(assignments).isEmpty()
        }
    }

    @Nested
    inner class Update {
        @Test
        fun `update returns updated assignment with name changed`() {
            val created = (client.create(CreateAssignmentParam(
                planId = planId, name = "Old Name", type = "tent", maxOccupancy = 4, ownerId = ownerId
            )) as Result.Success).value

            val result = client.update(UpdateAssignmentParam(id = created.id, name = "New Name", maxOccupancy = null))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val updated = (result as Result.Success).value
            assertThat(updated.name).isEqualTo("New Name")
            assertThat(updated.maxOccupancy).isEqualTo(4)
        }

        @Test
        fun `update returns updated assignment with maxOccupancy changed`() {
            val created = (client.create(CreateAssignmentParam(
                planId = planId, name = "Tent A", type = "tent", maxOccupancy = 4, ownerId = ownerId
            )) as Result.Success).value

            val result = client.update(UpdateAssignmentParam(id = created.id, name = null, maxOccupancy = 6))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val updated = (result as Result.Success).value
            assertThat(updated.name).isEqualTo("Tent A")
            assertThat(updated.maxOccupancy).isEqualTo(6)
        }

        @Test
        fun `update returns updated assignment with both fields changed`() {
            val created = (client.create(CreateAssignmentParam(
                planId = planId, name = "Old", type = "canoe", maxOccupancy = 2, ownerId = ownerId
            )) as Result.Success).value

            val result = client.update(UpdateAssignmentParam(id = created.id, name = "New", maxOccupancy = 4))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val updated = (result as Result.Success).value
            assertThat(updated.name).isEqualTo("New")
            assertThat(updated.maxOccupancy).isEqualTo(4)
        }

        @Test
        fun `update returns NotFoundError when assignment does not exist`() {
            val result = client.update(UpdateAssignmentParam(id = UUID.randomUUID(), name = "Nope", maxOccupancy = null))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(NotFoundError::class.java)
        }

        @Test
        fun `update returns ConflictError for duplicate name and type`() {
            client.create(CreateAssignmentParam(
                planId = planId, name = "Tent A", type = "tent", maxOccupancy = 4, ownerId = ownerId
            ))
            val created2 = (client.create(CreateAssignmentParam(
                planId = planId, name = "Tent B", type = "tent", maxOccupancy = 2, ownerId = ownerId
            )) as Result.Success).value

            val result = client.update(UpdateAssignmentParam(id = created2.id, name = "Tent A", maxOccupancy = null))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ConflictError::class.java)
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `delete returns success when assignment exists`() {
            val created = (client.create(CreateAssignmentParam(
                planId = planId, name = "Doomed", type = "tent", maxOccupancy = 2, ownerId = ownerId
            )) as Result.Success).value

            val result = client.delete(DeleteAssignmentParam(created.id))
            assertThat(result).isInstanceOf(Result.Success::class.java)

            val getResult = client.getById(GetByIdParam(created.id))
            assertThat(getResult).isInstanceOf(Result.Failure::class.java)
        }

        @Test
        fun `delete returns NotFoundError when assignment does not exist`() {
            val result = client.delete(DeleteAssignmentParam(UUID.randomUUID()))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(NotFoundError::class.java)
        }
    }

    @Nested
    inner class AddMember {
        @Test
        fun `addMember returns created member`() {
            val assignment = (client.create(CreateAssignmentParam(
                planId = planId, name = "Tent A", type = "tent", maxOccupancy = 4, ownerId = ownerId
            )) as Result.Success).value

            val result = client.addMember(AddAssignmentMemberParam(
                assignmentId = assignment.id, userId = ownerId, planId = planId, assignmentType = "tent"
            ))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val member = (result as Result.Success).value
            assertThat(member.assignmentId).isEqualTo(assignment.id)
            assertThat(member.userId).isEqualTo(ownerId)
            assertThat(member.planId).isEqualTo(planId)
            assertThat(member.assignmentType).isEqualTo("tent")
        }

        @Test
        fun `addMember returns ConflictError when already member of same assignment`() {
            val assignment = (client.create(CreateAssignmentParam(
                planId = planId, name = "Tent A", type = "tent", maxOccupancy = 4, ownerId = ownerId
            )) as Result.Success).value

            client.addMember(AddAssignmentMemberParam(
                assignmentId = assignment.id, userId = ownerId, planId = planId, assignmentType = "tent"
            ))
            val result = client.addMember(AddAssignmentMemberParam(
                assignmentId = assignment.id, userId = ownerId, planId = planId, assignmentType = "tent"
            ))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ConflictError::class.java)
        }

        @Test
        fun `addMember returns ConflictError when user already assigned to same type in same plan`() {
            val assignment1 = (client.create(CreateAssignmentParam(
                planId = planId, name = "Tent A", type = "tent", maxOccupancy = 4, ownerId = ownerId
            )) as Result.Success).value
            val assignment2 = (client.create(CreateAssignmentParam(
                planId = planId, name = "Tent B", type = "tent", maxOccupancy = 4, ownerId = ownerId
            )) as Result.Success).value

            client.addMember(AddAssignmentMemberParam(
                assignmentId = assignment1.id, userId = otherUserId, planId = planId, assignmentType = "tent"
            ))
            val result = client.addMember(AddAssignmentMemberParam(
                assignmentId = assignment2.id, userId = otherUserId, planId = planId, assignmentType = "tent"
            ))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ConflictError::class.java)
        }
    }

    @Nested
    inner class RemoveMember {
        @Test
        fun `removeMember removes the member`() {
            val assignment = (client.create(CreateAssignmentParam(
                planId = planId, name = "Tent A", type = "tent", maxOccupancy = 4, ownerId = ownerId
            )) as Result.Success).value

            client.addMember(AddAssignmentMemberParam(
                assignmentId = assignment.id, userId = ownerId, planId = planId, assignmentType = "tent"
            ))
            val result = client.removeMember(RemoveAssignmentMemberParam(assignmentId = assignment.id, userId = ownerId))
            assertThat(result).isInstanceOf(Result.Success::class.java)

            val membersResult = client.getMembers(GetAssignmentMembersParam(assignment.id))
            assertThat((membersResult as Result.Success).value).isEmpty()
        }

        @Test
        fun `removeMember returns NotFoundError when membership does not exist`() {
            val assignment = (client.create(CreateAssignmentParam(
                planId = planId, name = "Tent A", type = "tent", maxOccupancy = 4, ownerId = ownerId
            )) as Result.Success).value

            val result = client.removeMember(RemoveAssignmentMemberParam(assignmentId = assignment.id, userId = otherUserId))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(NotFoundError::class.java)
        }
    }

    @Nested
    inner class GetMembers {
        @Test
        fun `getMembers returns members of an assignment`() {
            val assignment = (client.create(CreateAssignmentParam(
                planId = planId, name = "Tent A", type = "tent", maxOccupancy = 4, ownerId = ownerId
            )) as Result.Success).value

            client.addMember(AddAssignmentMemberParam(
                assignmentId = assignment.id, userId = ownerId, planId = planId, assignmentType = "tent"
            ))
            client.addMember(AddAssignmentMemberParam(
                assignmentId = assignment.id, userId = otherUserId, planId = planId, assignmentType = "tent"
            ))

            val result = client.getMembers(GetAssignmentMembersParam(assignment.id))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val members = (result as Result.Success).value
            assertThat(members).hasSize(2)
        }

        @Test
        fun `getMembers returns empty list when assignment has no members`() {
            val assignment = (client.create(CreateAssignmentParam(
                planId = planId, name = "Empty Tent", type = "tent", maxOccupancy = 4, ownerId = ownerId
            )) as Result.Success).value

            val result = client.getMembers(GetAssignmentMembersParam(assignment.id))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            assertThat((result as Result.Success).value).isEmpty()
        }
    }

    @Nested
    inner class TransferOwnership {
        @Test
        fun `transferOwnership returns assignment with new owner`() {
            val created = (client.create(CreateAssignmentParam(
                planId = planId, name = "Tent A", type = "tent", maxOccupancy = 4, ownerId = ownerId
            )) as Result.Success).value

            val result = client.transferOwnership(TransferOwnershipParam(id = created.id, newOwnerId = otherUserId))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val updated = (result as Result.Success).value
            assertThat(updated.ownerId).isEqualTo(otherUserId)
            assertThat(updated.name).isEqualTo("Tent A")
        }

        @Test
        fun `transferOwnership returns NotFoundError when assignment does not exist`() {
            val result = client.transferOwnership(TransferOwnershipParam(id = UUID.randomUUID(), newOwnerId = otherUserId))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(NotFoundError::class.java)
        }
    }
}
