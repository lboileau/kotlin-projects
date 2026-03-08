package com.acme.services.camperservice.features.assignment.acceptance

import com.acme.services.camperservice.config.TestContainerConfig
import com.acme.services.camperservice.features.assignment.acceptance.fixture.AssignmentFixture
import com.acme.services.camperservice.features.assignment.dto.AddAssignmentMemberRequest
import com.acme.services.camperservice.features.assignment.dto.AssignmentDetailResponse
import com.acme.services.camperservice.features.assignment.dto.AssignmentMemberResponse
import com.acme.services.camperservice.features.assignment.dto.AssignmentResponse
import com.acme.services.camperservice.features.assignment.dto.CreateAssignmentRequest
import com.acme.services.camperservice.features.assignment.dto.TransferOwnershipRequest
import com.acme.services.camperservice.features.assignment.dto.UpdateAssignmentRequest
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
class AssignmentAcceptanceTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    private lateinit var fixture: AssignmentFixture
    private lateinit var planOwnerId: UUID
    private lateinit var assignmentOwnerId: UUID
    private lateinit var memberId: UUID
    private lateinit var otherUserId: UUID
    private lateinit var planId: UUID

    @BeforeEach
    fun setUp() {
        fixture = AssignmentFixture(jdbcTemplate)
        fixture.truncateAll()
        planOwnerId = fixture.insertUser(email = "planowner@example.com", username = "planowner")
        assignmentOwnerId = fixture.insertUser(email = "assignmentowner@example.com", username = "assignmentowner")
        memberId = fixture.insertUser(email = "member@example.com", username = "member")
        otherUserId = fixture.insertUser(email = "other@example.com", username = "other")
        planId = fixture.insertPlan(name = "Camping Trip", ownerId = planOwnerId)
        fixture.insertPlanMember(planId = planId, userId = planOwnerId)
        fixture.insertPlanMember(planId = planId, userId = assignmentOwnerId)
        fixture.insertPlanMember(planId = planId, userId = memberId)
    }

    @Nested
    inner class CreateAssignment {

        @Test
        fun `POST returns 201 with created tent assignment with default capacity 4`() {
            val response = restTemplate.exchange(
                "/api/plans/$planId/assignments",
                HttpMethod.POST,
                entityWithUser(CreateAssignmentRequest(name = "Big Tent", type = "tent", maxOccupancy = null), assignmentOwnerId),
                AssignmentResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            assertThat(response.body!!.name).isEqualTo("Big Tent")
            assertThat(response.body!!.type).isEqualTo("tent")
            assertThat(response.body!!.maxOccupancy).isEqualTo(4)
            assertThat(response.body!!.ownerId).isEqualTo(assignmentOwnerId)
            assertThat(response.body!!.planId).isEqualTo(planId)
        }

        @Test
        fun `POST returns 201 with canoe assignment with custom capacity`() {
            val response = restTemplate.exchange(
                "/api/plans/$planId/assignments",
                HttpMethod.POST,
                entityWithUser(CreateAssignmentRequest(name = "Red Canoe", type = "canoe", maxOccupancy = 3), assignmentOwnerId),
                AssignmentResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            assertThat(response.body!!.name).isEqualTo("Red Canoe")
            assertThat(response.body!!.type).isEqualTo("canoe")
            assertThat(response.body!!.maxOccupancy).isEqualTo(3)
        }

        @Test
        fun `POST returns 400 when name is blank`() {
            val response = restTemplate.exchange(
                "/api/plans/$planId/assignments",
                HttpMethod.POST,
                entityWithUser(CreateAssignmentRequest(name = "", type = "tent", maxOccupancy = null), assignmentOwnerId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        fun `POST returns 400 when type is invalid`() {
            val response = restTemplate.exchange(
                "/api/plans/$planId/assignments",
                HttpMethod.POST,
                entityWithUser(CreateAssignmentRequest(name = "Hammock", type = "hammock", maxOccupancy = null), assignmentOwnerId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        fun `POST returns 409 when duplicate name and type in same plan`() {
            fixture.createAssignment(planId = planId, name = "Big Tent", type = "tent", ownerId = assignmentOwnerId)

            val response = restTemplate.exchange(
                "/api/plans/$planId/assignments",
                HttpMethod.POST,
                entityWithUser(CreateAssignmentRequest(name = "Big Tent", type = "tent", maxOccupancy = null), assignmentOwnerId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        }
    }

    @Nested
    inner class GetAssignments {

        @Test
        fun `GET returns 200 with all assignments for plan`() {
            fixture.createAssignment(planId = planId, name = "Tent A", type = "tent", ownerId = assignmentOwnerId)
            fixture.createAssignment(planId = planId, name = "Canoe A", type = "canoe", maxOccupancy = 2, ownerId = assignmentOwnerId)

            val response = restTemplate.exchange(
                "/api/plans/$planId/assignments",
                HttpMethod.GET,
                entityWithUser(null, assignmentOwnerId),
                Array<AssignmentResponse>::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body).hasSize(2)
            assertThat(response.body!!.map { it.name }).containsExactlyInAnyOrder("Tent A", "Canoe A")
        }

        @Test
        fun `GET returns 200 filtered by type query param`() {
            fixture.createAssignment(planId = planId, name = "Tent A", type = "tent", ownerId = assignmentOwnerId)
            fixture.createAssignment(planId = planId, name = "Canoe A", type = "canoe", maxOccupancy = 2, ownerId = assignmentOwnerId)

            val response = restTemplate.exchange(
                "/api/plans/$planId/assignments?type=canoe",
                HttpMethod.GET,
                entityWithUser(null, assignmentOwnerId),
                Array<AssignmentResponse>::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body).hasSize(1)
            assertThat(response.body!![0].name).isEqualTo("Canoe A")
            assertThat(response.body!![0].type).isEqualTo("canoe")
        }

        @Test
        fun `GET returns 200 with empty list when no assignments`() {
            val response = restTemplate.exchange(
                "/api/plans/$planId/assignments",
                HttpMethod.GET,
                entityWithUser(null, assignmentOwnerId),
                Array<AssignmentResponse>::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body).isEmpty()
        }
    }

    @Nested
    inner class GetAssignment {

        @Test
        fun `GET returns 200 with assignment detail including members`() {
            val assignmentId = fixture.createAssignment(planId = planId, name = "Tent A", type = "tent", ownerId = assignmentOwnerId)
            fixture.addMember(assignmentId = assignmentId, userId = memberId, planId = planId, type = "tent")

            val response = restTemplate.exchange(
                "/api/plans/$planId/assignments/$assignmentId",
                HttpMethod.GET,
                entityWithUser(null, assignmentOwnerId),
                AssignmentDetailResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.name).isEqualTo("Tent A")
            assertThat(response.body!!.members).hasSize(1)
            assertThat(response.body!!.members[0].userId).isEqualTo(memberId)
            assertThat(response.body!!.members[0].username).isEqualTo("member")
        }

        @Test
        fun `GET returns 404 when assignment not found`() {
            val response = restTemplate.exchange(
                "/api/plans/$planId/assignments/${UUID.randomUUID()}",
                HttpMethod.GET,
                entityWithUser(null, assignmentOwnerId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @Nested
    inner class UpdateAssignment {

        @Test
        fun `PUT returns 200 when assignment owner updates`() {
            val assignmentId = fixture.createAssignment(planId = planId, name = "Old Name", type = "tent", ownerId = assignmentOwnerId)

            val response = restTemplate.exchange(
                "/api/plans/$planId/assignments/$assignmentId",
                HttpMethod.PUT,
                entityWithUser(UpdateAssignmentRequest(name = "New Name", maxOccupancy = null), assignmentOwnerId),
                AssignmentResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.name).isEqualTo("New Name")
        }

        @Test
        fun `PUT returns 200 when plan owner updates`() {
            val assignmentId = fixture.createAssignment(planId = planId, name = "Old Name", type = "tent", ownerId = assignmentOwnerId)

            val response = restTemplate.exchange(
                "/api/plans/$planId/assignments/$assignmentId",
                HttpMethod.PUT,
                entityWithUser(UpdateAssignmentRequest(name = "Plan Owner Rename", maxOccupancy = null), planOwnerId),
                AssignmentResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.name).isEqualTo("Plan Owner Rename")
        }

        @Test
        fun `PUT returns 403 when non-owner non-plan-owner updates`() {
            val assignmentId = fixture.createAssignment(planId = planId, name = "Tent", type = "tent", ownerId = assignmentOwnerId)

            val response = restTemplate.exchange(
                "/api/plans/$planId/assignments/$assignmentId",
                HttpMethod.PUT,
                entityWithUser(UpdateAssignmentRequest(name = "Renamed", maxOccupancy = null), otherUserId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        }

        @Test
        fun `PUT returns 404 when assignment not found`() {
            val response = restTemplate.exchange(
                "/api/plans/$planId/assignments/${UUID.randomUUID()}",
                HttpMethod.PUT,
                entityWithUser(UpdateAssignmentRequest(name = "Nope", maxOccupancy = null), assignmentOwnerId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @Nested
    inner class DeleteAssignment {

        @Test
        fun `DELETE returns 204 when assignment owner deletes`() {
            val assignmentId = fixture.createAssignment(planId = planId, name = "Doomed", type = "tent", ownerId = assignmentOwnerId)

            val response = restTemplate.exchange(
                "/api/plans/$planId/assignments/$assignmentId",
                HttpMethod.DELETE,
                entityWithUser(null, assignmentOwnerId),
                Void::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        }

        @Test
        fun `DELETE returns 204 when plan owner deletes`() {
            val assignmentId = fixture.createAssignment(planId = planId, name = "Doomed", type = "tent", ownerId = assignmentOwnerId)

            val response = restTemplate.exchange(
                "/api/plans/$planId/assignments/$assignmentId",
                HttpMethod.DELETE,
                entityWithUser(null, planOwnerId),
                Void::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        }

        @Test
        fun `DELETE returns 403 when non-owner deletes`() {
            val assignmentId = fixture.createAssignment(planId = planId, name = "Protected", type = "tent", ownerId = assignmentOwnerId)

            val response = restTemplate.exchange(
                "/api/plans/$planId/assignments/$assignmentId",
                HttpMethod.DELETE,
                entityWithUser(null, otherUserId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        }

        @Test
        fun `DELETE returns 404 when assignment not found`() {
            val response = restTemplate.exchange(
                "/api/plans/$planId/assignments/${UUID.randomUUID()}",
                HttpMethod.DELETE,
                entityWithUser(null, assignmentOwnerId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @Nested
    inner class AddMember {

        @Test
        fun `POST returns 201 with new member`() {
            val assignmentId = fixture.createAssignment(planId = planId, name = "Tent A", type = "tent", ownerId = assignmentOwnerId)

            val response = restTemplate.exchange(
                "/api/plans/$planId/assignments/$assignmentId/members",
                HttpMethod.POST,
                entityWithUser(AddAssignmentMemberRequest(userId = memberId), assignmentOwnerId),
                AssignmentMemberResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            assertThat(response.body!!.assignmentId).isEqualTo(assignmentId)
            assertThat(response.body!!.userId).isEqualTo(memberId)
            assertThat(response.body!!.username).isEqualTo("member")
        }

        @Test
        fun `POST returns 409 when already a member`() {
            val assignmentId = fixture.createAssignment(planId = planId, name = "Tent A", type = "tent", ownerId = assignmentOwnerId)
            fixture.addMember(assignmentId = assignmentId, userId = memberId, planId = planId, type = "tent")

            val response = restTemplate.exchange(
                "/api/plans/$planId/assignments/$assignmentId/members",
                HttpMethod.POST,
                entityWithUser(AddAssignmentMemberRequest(userId = memberId), assignmentOwnerId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        }

        @Test
        fun `POST returns 409 when already assigned to same type in same plan`() {
            val tentA = fixture.createAssignment(planId = planId, name = "Tent A", type = "tent", ownerId = assignmentOwnerId)
            fixture.addMember(assignmentId = tentA, userId = memberId, planId = planId, type = "tent")

            val tentB = fixture.createAssignment(planId = planId, name = "Tent B", type = "tent", ownerId = assignmentOwnerId)

            val response = restTemplate.exchange(
                "/api/plans/$planId/assignments/$tentB/members",
                HttpMethod.POST,
                entityWithUser(AddAssignmentMemberRequest(userId = memberId), assignmentOwnerId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        }

        @Test
        fun `POST returns 409 when at capacity`() {
            val assignmentId = fixture.createAssignment(planId = planId, name = "Tiny Tent", type = "tent", maxOccupancy = 1, ownerId = assignmentOwnerId)
            fixture.addMember(assignmentId = assignmentId, userId = assignmentOwnerId, planId = planId, type = "tent")

            val response = restTemplate.exchange(
                "/api/plans/$planId/assignments/$assignmentId/members",
                HttpMethod.POST,
                entityWithUser(AddAssignmentMemberRequest(userId = memberId), assignmentOwnerId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        }
    }

    @Nested
    inner class RemoveMember {

        @Test
        fun `DELETE returns 204 for self-remove`() {
            val assignmentId = fixture.createAssignment(planId = planId, name = "Tent A", type = "tent", ownerId = assignmentOwnerId)
            fixture.addMember(assignmentId = assignmentId, userId = memberId, planId = planId, type = "tent")

            val response = restTemplate.exchange(
                "/api/plans/$planId/assignments/$assignmentId/members/$memberId",
                HttpMethod.DELETE,
                entityWithUser(null, memberId),
                Void::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        }

        @Test
        fun `DELETE returns 204 when assignment owner removes other`() {
            val assignmentId = fixture.createAssignment(planId = planId, name = "Tent A", type = "tent", ownerId = assignmentOwnerId)
            fixture.addMember(assignmentId = assignmentId, userId = memberId, planId = planId, type = "tent")

            val response = restTemplate.exchange(
                "/api/plans/$planId/assignments/$assignmentId/members/$memberId",
                HttpMethod.DELETE,
                entityWithUser(null, assignmentOwnerId),
                Void::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        }

        @Test
        fun `DELETE returns 204 when plan owner removes other`() {
            val assignmentId = fixture.createAssignment(planId = planId, name = "Tent A", type = "tent", ownerId = assignmentOwnerId)
            fixture.addMember(assignmentId = assignmentId, userId = memberId, planId = planId, type = "tent")

            val response = restTemplate.exchange(
                "/api/plans/$planId/assignments/$assignmentId/members/$memberId",
                HttpMethod.DELETE,
                entityWithUser(null, planOwnerId),
                Void::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        }

        @Test
        fun `DELETE returns 403 when non-owner tries to remove someone else`() {
            val assignmentId = fixture.createAssignment(planId = planId, name = "Tent A", type = "tent", ownerId = assignmentOwnerId)
            fixture.addMember(assignmentId = assignmentId, userId = memberId, planId = planId, type = "tent")

            val response = restTemplate.exchange(
                "/api/plans/$planId/assignments/$assignmentId/members/$memberId",
                HttpMethod.DELETE,
                entityWithUser(null, otherUserId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        }
    }

    @Nested
    inner class TransferOwnership {

        @Test
        fun `PUT returns 200 when assignment owner transfers`() {
            val assignmentId = fixture.createAssignment(planId = planId, name = "Tent A", type = "tent", ownerId = assignmentOwnerId)

            val response = restTemplate.exchange(
                "/api/plans/$planId/assignments/$assignmentId/owner",
                HttpMethod.PUT,
                entityWithUser(TransferOwnershipRequest(newOwnerId = memberId), assignmentOwnerId),
                AssignmentResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.ownerId).isEqualTo(memberId)
        }

        @Test
        fun `PUT returns 200 when plan owner transfers`() {
            val assignmentId = fixture.createAssignment(planId = planId, name = "Tent A", type = "tent", ownerId = assignmentOwnerId)

            val response = restTemplate.exchange(
                "/api/plans/$planId/assignments/$assignmentId/owner",
                HttpMethod.PUT,
                entityWithUser(TransferOwnershipRequest(newOwnerId = memberId), planOwnerId),
                AssignmentResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.ownerId).isEqualTo(memberId)
        }

        @Test
        fun `PUT returns 403 when non-owner transfers`() {
            val assignmentId = fixture.createAssignment(planId = planId, name = "Tent A", type = "tent", ownerId = assignmentOwnerId)

            val response = restTemplate.exchange(
                "/api/plans/$planId/assignments/$assignmentId/owner",
                HttpMethod.PUT,
                entityWithUser(TransferOwnershipRequest(newOwnerId = memberId), otherUserId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        }
    }

    private fun entityWithUser(body: Any?, userId: UUID): HttpEntity<Any?> {
        val headers = HttpHeaders()
        headers.set("X-User-Id", userId.toString())
        return HttpEntity(body, headers)
    }
}
