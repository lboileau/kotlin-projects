package com.acme.services.camperservice.features.assignment

import com.acme.clients.assignmentclient.fake.FakeAssignmentClient
import com.acme.clients.assignmentclient.model.Assignment as ClientAssignment
import com.acme.clients.assignmentclient.model.AssignmentMember as ClientAssignmentMember
import com.acme.clients.common.Result
import com.acme.clients.planclient.fake.FakePlanClient
import com.acme.clients.planclient.model.Plan
import com.acme.clients.userclient.fake.FakeUserClient
import com.acme.clients.gearsyncclient.fake.FakeGearSyncClient
import com.acme.clients.userclient.model.User
import com.acme.services.camperservice.features.assignment.error.AssignmentError
import com.acme.services.camperservice.features.assignment.params.*
import com.acme.services.camperservice.features.assignment.service.AssignmentService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class AssignmentServiceTest {

    private val fakeAssignmentClient = FakeAssignmentClient()
    private val fakePlanClient = FakePlanClient()
    private val fakeUserClient = FakeUserClient()
    private val fakeGearSyncClient = FakeGearSyncClient()
    private val assignmentService = AssignmentService(fakeAssignmentClient, fakePlanClient, fakeUserClient, fakeGearSyncClient)

    private val planOwnerId = UUID.randomUUID()
    private val assignmentOwnerId = UUID.randomUUID()
    private val otherUserId = UUID.randomUUID()
    private val planId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        fakeAssignmentClient.reset()
        fakePlanClient.reset()
        fakeUserClient.reset()

        fakeUserClient.seed(
            User(id = planOwnerId, email = "planowner@example.com", username = "planowner", createdAt = Instant.now(), updatedAt = Instant.now()),
            User(id = assignmentOwnerId, email = "assignowner@example.com", username = "assignowner", createdAt = Instant.now(), updatedAt = Instant.now()),
            User(id = otherUserId, email = "other@example.com", username = "other", createdAt = Instant.now(), updatedAt = Instant.now())
        )

        fakePlanClient.seedPlan(
            Plan(id = planId, name = "Camping Trip", visibility = "private", ownerId = planOwnerId, createdAt = Instant.now(), updatedAt = Instant.now())
        )
    }

    @Nested
    inner class CreateAssignment {
        @Test
        fun `create returns success for tent with default capacity`() {
            val result = assignmentService.create(
                CreateAssignmentParam(planId = planId, name = "Tent A", type = "tent", maxOccupancy = null, userId = assignmentOwnerId)
            )

            assertThat(result.isSuccess).isTrue()
            val response = (result as Result.Success).value
            assertThat(response.name).isEqualTo("Tent A")
            assertThat(response.type).isEqualTo("tent")
            assertThat(response.maxOccupancy).isEqualTo(4)
            assertThat(response.ownerId).isEqualTo(assignmentOwnerId)
            assertThat(response.planId).isEqualTo(planId)
        }

        @Test
        fun `create returns success for canoe with custom capacity`() {
            val result = assignmentService.create(
                CreateAssignmentParam(planId = planId, name = "Canoe 1", type = "canoe", maxOccupancy = 3, userId = assignmentOwnerId)
            )

            assertThat(result.isSuccess).isTrue()
            val response = (result as Result.Success).value
            assertThat(response.name).isEqualTo("Canoe 1")
            assertThat(response.type).isEqualTo("canoe")
            assertThat(response.maxOccupancy).isEqualTo(3)
        }

        @Test
        fun `create returns Invalid when name is blank`() {
            val result = assignmentService.create(
                CreateAssignmentParam(planId = planId, name = "", type = "tent", maxOccupancy = null, userId = assignmentOwnerId)
            )

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(AssignmentError.Invalid::class.java)
        }

        @Test
        fun `create returns Invalid when type is invalid`() {
            val result = assignmentService.create(
                CreateAssignmentParam(planId = planId, name = "Hammock", type = "hammock", maxOccupancy = null, userId = assignmentOwnerId)
            )

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(AssignmentError.Invalid::class.java)
        }

        @Test
        fun `create returns PlanNotFound when plan does not exist`() {
            val result = assignmentService.create(
                CreateAssignmentParam(planId = UUID.randomUUID(), name = "Tent B", type = "tent", maxOccupancy = null, userId = assignmentOwnerId)
            )

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(AssignmentError.PlanNotFound::class.java)
        }
    }

    @Nested
    inner class GetAssignments {
        @Test
        fun `getAssignments returns all assignments for a plan`() {
            assignmentService.create(CreateAssignmentParam(planId = planId, name = "Tent A", type = "tent", maxOccupancy = null, userId = assignmentOwnerId))
            assignmentService.create(CreateAssignmentParam(planId = planId, name = "Canoe 1", type = "canoe", maxOccupancy = null, userId = assignmentOwnerId))

            val result = assignmentService.getAssignments(GetAssignmentsParam(planId = planId))

            assertThat(result.isSuccess).isTrue()
            val assignments = (result as Result.Success).value
            assertThat(assignments).hasSize(2)
            assertThat(assignments.map { it.name }).containsExactlyInAnyOrder("Tent A", "Canoe 1")
        }

        @Test
        fun `getAssignments returns filtered by type`() {
            assignmentService.create(CreateAssignmentParam(planId = planId, name = "Tent A", type = "tent", maxOccupancy = null, userId = assignmentOwnerId))
            assignmentService.create(CreateAssignmentParam(planId = planId, name = "Canoe 1", type = "canoe", maxOccupancy = null, userId = assignmentOwnerId))

            val result = assignmentService.getAssignments(GetAssignmentsParam(planId = planId, type = "tent"))

            assertThat(result.isSuccess).isTrue()
            val assignments = (result as Result.Success).value
            assertThat(assignments).hasSize(1)
            assertThat(assignments[0].name).isEqualTo("Tent A")
        }

        @Test
        fun `getAssignments returns empty list when no assignments exist`() {
            val result = assignmentService.getAssignments(GetAssignmentsParam(planId = planId))

            assertThat(result.isSuccess).isTrue()
            assertThat((result as Result.Success).value).isEmpty()
        }
    }

    @Nested
    inner class GetAssignment {
        @Test
        fun `getAssignment returns assignment with members enriched with username`() {
            val created = (assignmentService.create(
                CreateAssignmentParam(planId = planId, name = "Tent A", type = "tent", maxOccupancy = 4, userId = assignmentOwnerId)
            ) as Result.Success).value

            assignmentService.addMember(AddAssignmentMemberParam(assignmentId = created.id, memberUserId = assignmentOwnerId, userId = assignmentOwnerId))

            val result = assignmentService.getAssignment(GetAssignmentParam(assignmentId = created.id))

            assertThat(result.isSuccess).isTrue()
            val detail = (result as Result.Success).value
            assertThat(detail.name).isEqualTo("Tent A")
            assertThat(detail.members).hasSize(1)
            assertThat(detail.members[0].username).isEqualTo("assignowner")
            assertThat(detail.members[0].userId).isEqualTo(assignmentOwnerId)
        }

        @Test
        fun `getAssignment returns NotFound when assignment does not exist`() {
            val result = assignmentService.getAssignment(GetAssignmentParam(assignmentId = UUID.randomUUID()))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(AssignmentError.NotFound::class.java)
        }
    }

    @Nested
    inner class UpdateAssignment {
        @Test
        fun `update returns success when owner updates`() {
            val created = (assignmentService.create(
                CreateAssignmentParam(planId = planId, name = "Old Name", type = "tent", maxOccupancy = null, userId = assignmentOwnerId)
            ) as Result.Success).value

            val result = assignmentService.update(
                UpdateAssignmentParam(assignmentId = created.id, name = "New Name", maxOccupancy = null, userId = assignmentOwnerId)
            )

            assertThat(result.isSuccess).isTrue()
            assertThat((result as Result.Success).value.name).isEqualTo("New Name")
        }

        @Test
        fun `update returns success when plan owner updates`() {
            val created = (assignmentService.create(
                CreateAssignmentParam(planId = planId, name = "Tent A", type = "tent", maxOccupancy = null, userId = assignmentOwnerId)
            ) as Result.Success).value

            val result = assignmentService.update(
                UpdateAssignmentParam(assignmentId = created.id, name = "Renamed", maxOccupancy = null, userId = planOwnerId)
            )

            assertThat(result.isSuccess).isTrue()
            assertThat((result as Result.Success).value.name).isEqualTo("Renamed")
        }

        @Test
        fun `update returns NotOwner when non-owner updates`() {
            val created = (assignmentService.create(
                CreateAssignmentParam(planId = planId, name = "Tent A", type = "tent", maxOccupancy = null, userId = assignmentOwnerId)
            ) as Result.Success).value

            val result = assignmentService.update(
                UpdateAssignmentParam(assignmentId = created.id, name = "Hijacked", maxOccupancy = null, userId = otherUserId)
            )

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(AssignmentError.NotOwner::class.java)
        }

        @Test
        fun `update returns NotFound when assignment does not exist`() {
            val result = assignmentService.update(
                UpdateAssignmentParam(assignmentId = UUID.randomUUID(), name = "Nope", maxOccupancy = null, userId = assignmentOwnerId)
            )

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(AssignmentError.NotFound::class.java)
        }
    }

    @Nested
    inner class DeleteAssignment {
        @Test
        fun `delete returns success when owner deletes`() {
            val created = (assignmentService.create(
                CreateAssignmentParam(planId = planId, name = "Doomed", type = "tent", maxOccupancy = null, userId = assignmentOwnerId)
            ) as Result.Success).value

            val result = assignmentService.delete(
                DeleteAssignmentParam(assignmentId = created.id, userId = assignmentOwnerId)
            )

            assertThat(result.isSuccess).isTrue()
        }

        @Test
        fun `delete returns success when plan owner deletes`() {
            val created = (assignmentService.create(
                CreateAssignmentParam(planId = planId, name = "Also Doomed", type = "canoe", maxOccupancy = null, userId = assignmentOwnerId)
            ) as Result.Success).value

            val result = assignmentService.delete(
                DeleteAssignmentParam(assignmentId = created.id, userId = planOwnerId)
            )

            assertThat(result.isSuccess).isTrue()
        }

        @Test
        fun `delete returns NotOwner when non-owner deletes`() {
            val created = (assignmentService.create(
                CreateAssignmentParam(planId = planId, name = "Protected", type = "tent", maxOccupancy = null, userId = assignmentOwnerId)
            ) as Result.Success).value

            val result = assignmentService.delete(
                DeleteAssignmentParam(assignmentId = created.id, userId = otherUserId)
            )

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(AssignmentError.NotOwner::class.java)
        }
    }

    @Nested
    inner class AddMember {
        @Test
        fun `addMember returns success`() {
            val created = (assignmentService.create(
                CreateAssignmentParam(planId = planId, name = "Tent A", type = "tent", maxOccupancy = 4, userId = assignmentOwnerId)
            ) as Result.Success).value

            val result = assignmentService.addMember(
                AddAssignmentMemberParam(assignmentId = created.id, memberUserId = otherUserId, userId = otherUserId)
            )

            assertThat(result.isSuccess).isTrue()
            val member = (result as Result.Success).value
            assertThat(member.userId).isEqualTo(otherUserId)
            assertThat(member.assignmentId).isEqualTo(created.id)
            assertThat(member.username).isEqualTo("other")
        }

        @Test
        fun `addMember returns AtCapacity when assignment is full`() {
            val created = (assignmentService.create(
                CreateAssignmentParam(planId = planId, name = "Tiny Tent", type = "tent", maxOccupancy = 1, userId = assignmentOwnerId)
            ) as Result.Success).value

            assignmentService.addMember(AddAssignmentMemberParam(assignmentId = created.id, memberUserId = assignmentOwnerId, userId = assignmentOwnerId))

            val result = assignmentService.addMember(
                AddAssignmentMemberParam(assignmentId = created.id, memberUserId = otherUserId, userId = otherUserId)
            )

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(AssignmentError.AtCapacity::class.java)
        }

        @Test
        fun `addMember returns AlreadyMember when user is already assigned`() {
            val created = (assignmentService.create(
                CreateAssignmentParam(planId = planId, name = "Tent A", type = "tent", maxOccupancy = 4, userId = assignmentOwnerId)
            ) as Result.Success).value

            assignmentService.addMember(AddAssignmentMemberParam(assignmentId = created.id, memberUserId = otherUserId, userId = otherUserId))

            val result = assignmentService.addMember(
                AddAssignmentMemberParam(assignmentId = created.id, memberUserId = otherUserId, userId = otherUserId)
            )

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(AssignmentError.AlreadyMember::class.java)
        }
    }

    @Nested
    inner class RemoveMember {
        @Test
        fun `removeMember allows self-remove`() {
            val created = (assignmentService.create(
                CreateAssignmentParam(planId = planId, name = "Tent A", type = "tent", maxOccupancy = 4, userId = assignmentOwnerId)
            ) as Result.Success).value

            assignmentService.addMember(AddAssignmentMemberParam(assignmentId = created.id, memberUserId = otherUserId, userId = otherUserId))

            val result = assignmentService.removeMember(
                RemoveAssignmentMemberParam(assignmentId = created.id, memberUserId = otherUserId, userId = otherUserId)
            )

            assertThat(result.isSuccess).isTrue()
        }

        @Test
        fun `removeMember allows assignment owner to remove other`() {
            val created = (assignmentService.create(
                CreateAssignmentParam(planId = planId, name = "Tent A", type = "tent", maxOccupancy = 4, userId = assignmentOwnerId)
            ) as Result.Success).value

            assignmentService.addMember(AddAssignmentMemberParam(assignmentId = created.id, memberUserId = otherUserId, userId = otherUserId))

            val result = assignmentService.removeMember(
                RemoveAssignmentMemberParam(assignmentId = created.id, memberUserId = otherUserId, userId = assignmentOwnerId)
            )

            assertThat(result.isSuccess).isTrue()
        }

        @Test
        fun `removeMember allows plan owner to remove other`() {
            val created = (assignmentService.create(
                CreateAssignmentParam(planId = planId, name = "Tent A", type = "tent", maxOccupancy = 4, userId = assignmentOwnerId)
            ) as Result.Success).value

            assignmentService.addMember(AddAssignmentMemberParam(assignmentId = created.id, memberUserId = otherUserId, userId = otherUserId))

            val result = assignmentService.removeMember(
                RemoveAssignmentMemberParam(assignmentId = created.id, memberUserId = otherUserId, userId = planOwnerId)
            )

            assertThat(result.isSuccess).isTrue()
        }

        @Test
        fun `removeMember returns NotOwner when non-owner non-self tries to remove`() {
            val created = (assignmentService.create(
                CreateAssignmentParam(planId = planId, name = "Tent A", type = "tent", maxOccupancy = 4, userId = assignmentOwnerId)
            ) as Result.Success).value

            val fourthUserId = UUID.randomUUID()
            fakeUserClient.seed(
                User(id = fourthUserId, email = "fourth@example.com", username = "fourth", createdAt = Instant.now(), updatedAt = Instant.now())
            )

            assignmentService.addMember(AddAssignmentMemberParam(assignmentId = created.id, memberUserId = assignmentOwnerId, userId = assignmentOwnerId))
            assignmentService.addMember(AddAssignmentMemberParam(assignmentId = created.id, memberUserId = otherUserId, userId = otherUserId))

            val result = assignmentService.removeMember(
                RemoveAssignmentMemberParam(assignmentId = created.id, memberUserId = assignmentOwnerId, userId = otherUserId)
            )

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(AssignmentError.NotOwner::class.java)
        }
    }

    @Nested
    inner class TransferOwnership {
        @Test
        fun `transferOwnership returns success when owner transfers`() {
            val created = (assignmentService.create(
                CreateAssignmentParam(planId = planId, name = "Tent A", type = "tent", maxOccupancy = null, userId = assignmentOwnerId)
            ) as Result.Success).value

            val result = assignmentService.transferOwnership(
                TransferOwnershipParam(assignmentId = created.id, newOwnerId = otherUserId, userId = assignmentOwnerId)
            )

            assertThat(result.isSuccess).isTrue()
            assertThat((result as Result.Success).value.ownerId).isEqualTo(otherUserId)
        }

        @Test
        fun `transferOwnership returns success when plan owner transfers`() {
            val created = (assignmentService.create(
                CreateAssignmentParam(planId = planId, name = "Tent A", type = "tent", maxOccupancy = null, userId = assignmentOwnerId)
            ) as Result.Success).value

            val result = assignmentService.transferOwnership(
                TransferOwnershipParam(assignmentId = created.id, newOwnerId = otherUserId, userId = planOwnerId)
            )

            assertThat(result.isSuccess).isTrue()
            assertThat((result as Result.Success).value.ownerId).isEqualTo(otherUserId)
        }

        @Test
        fun `transferOwnership returns NotOwner when non-owner transfers`() {
            val created = (assignmentService.create(
                CreateAssignmentParam(planId = planId, name = "Tent A", type = "tent", maxOccupancy = null, userId = assignmentOwnerId)
            ) as Result.Success).value

            val result = assignmentService.transferOwnership(
                TransferOwnershipParam(assignmentId = created.id, newOwnerId = planOwnerId, userId = otherUserId)
            )

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(AssignmentError.NotOwner::class.java)
        }
    }
}
