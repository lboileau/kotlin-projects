package com.acme.services.camperservice.features.plan.acceptance

import com.acme.services.camperservice.config.TestContainerConfig
import com.acme.services.camperservice.features.plan.acceptance.fixture.PlanFixture
import com.acme.services.camperservice.features.plan.dto.AddMemberRequest
import com.acme.services.camperservice.features.plan.dto.CreatePlanRequest
import com.acme.services.camperservice.features.plan.dto.PlanMemberResponse
import com.acme.services.camperservice.features.plan.dto.PlanResponse
import com.acme.services.camperservice.features.plan.dto.UpdateMemberRoleRequest
import com.acme.services.camperservice.features.plan.dto.UpdatePlanRequest
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
class PlanAcceptanceTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    private lateinit var fixture: PlanFixture
    private lateinit var ownerId: UUID
    private lateinit var otherUserId: UUID

    @BeforeEach
    fun setUp() {
        fixture = PlanFixture(jdbcTemplate)
        fixture.truncateAll()
        ownerId = fixture.insertUser(email = "owner@example.com", username = "owner")
        otherUserId = fixture.insertUser(email = "other@example.com", username = "other")
    }

    @Nested
    inner class CreatePlan {

        @Test
        fun `POST returns 201 with created plan`() {
            val response = restTemplate.exchange(
                "/api/plans",
                HttpMethod.POST,
                entityWithUser(CreatePlanRequest(name = "Beach Trip"), ownerId),
                PlanResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            assertThat(response.body!!.name).isEqualTo("Beach Trip")
            assertThat(response.body!!.visibility).isEqualTo("private")
            assertThat(response.body!!.ownerId).isEqualTo(ownerId)
        }

        @Test
        fun `POST returns 400 when name is blank`() {
            val response = restTemplate.exchange(
                "/api/plans",
                HttpMethod.POST,
                entityWithUser(CreatePlanRequest(name = ""), ownerId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @Nested
    inner class GetPlans {

        @Test
        fun `GET returns 200 with empty list when no plans`() {
            val response = restTemplate.exchange(
                "/api/plans",
                HttpMethod.GET,
                entityWithUser(null, ownerId),
                Array<PlanResponse>::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body).isEmpty()
        }

        @Test
        fun `GET returns user plans and public plans`() {
            // Private plan owned by owner (with membership)
            val privatePlanId = fixture.insertPlan(name = "Private Trip", visibility = "private", ownerId = ownerId)
            fixture.insertPlanMember(planId = privatePlanId, userId = ownerId)

            // Public plan owned by other user
            fixture.insertPlan(name = "Public Trip", visibility = "public", ownerId = otherUserId)

            val response = restTemplate.exchange(
                "/api/plans",
                HttpMethod.GET,
                entityWithUser(null, ownerId),
                Array<PlanResponse>::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body).hasSize(2)
            assertThat(response.body!!.map { it.name }).containsExactlyInAnyOrder("Private Trip", "Public Trip")
        }

        @Test
        fun `GET deduplicates public plans user is member of`() {
            val publicPlanId = fixture.insertPlan(name = "Public Trip", visibility = "public", ownerId = otherUserId)
            fixture.insertPlanMember(planId = publicPlanId, userId = ownerId)

            val response = restTemplate.exchange(
                "/api/plans",
                HttpMethod.GET,
                entityWithUser(null, ownerId),
                Array<PlanResponse>::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body).hasSize(1)
        }
    }

    @Nested
    inner class UpdatePlan {

        @Test
        fun `PUT returns 200 with updated plan when owner`() {
            val planId = fixture.insertPlan(name = "Old Name", ownerId = ownerId)

            val response = restTemplate.exchange(
                "/api/plans/$planId",
                HttpMethod.PUT,
                entityWithUser(UpdatePlanRequest(name = "New Name"), ownerId),
                PlanResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.name).isEqualTo("New Name")
        }

        @Test
        fun `PUT returns 403 when non-owner`() {
            val planId = fixture.insertPlan(name = "Trip", ownerId = ownerId)

            val response = restTemplate.exchange(
                "/api/plans/$planId",
                HttpMethod.PUT,
                entityWithUser(UpdatePlanRequest(name = "Renamed"), otherUserId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        }

        @Test
        fun `PUT returns 404 when plan does not exist`() {
            val response = restTemplate.exchange(
                "/api/plans/${UUID.randomUUID()}",
                HttpMethod.PUT,
                entityWithUser(UpdatePlanRequest(name = "Nope"), ownerId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @Test
        fun `PUT returns 400 when name is blank`() {
            val planId = fixture.insertPlan(name = "Trip", ownerId = ownerId)

            val response = restTemplate.exchange(
                "/api/plans/$planId",
                HttpMethod.PUT,
                entityWithUser(UpdatePlanRequest(name = ""), ownerId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @Nested
    inner class DeletePlan {

        @Test
        fun `DELETE returns 204 when owner`() {
            val planId = fixture.insertPlan(name = "Doomed", ownerId = ownerId)

            val response = restTemplate.exchange(
                "/api/plans/$planId",
                HttpMethod.DELETE,
                entityWithUser(null, ownerId),
                Void::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        }

        @Test
        fun `DELETE returns 403 when non-owner`() {
            val planId = fixture.insertPlan(name = "Protected", ownerId = ownerId)

            val response = restTemplate.exchange(
                "/api/plans/$planId",
                HttpMethod.DELETE,
                entityWithUser(null, otherUserId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        }

        @Test
        fun `DELETE returns 404 when plan does not exist`() {
            val response = restTemplate.exchange(
                "/api/plans/${UUID.randomUUID()}",
                HttpMethod.DELETE,
                entityWithUser(null, ownerId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @Nested
    inner class GetMembers {

        @Test
        fun `GET returns 200 with members`() {
            val planId = fixture.insertPlan(name = "Trip", ownerId = ownerId)
            fixture.insertPlanMember(planId = planId, userId = ownerId)
            fixture.insertPlanMember(planId = planId, userId = otherUserId)

            val response = restTemplate.exchange(
                "/api/plans/$planId/members",
                HttpMethod.GET,
                entityWithUser(null, ownerId),
                Array<PlanMemberResponse>::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body).hasSize(2)
            val usernames = response.body!!.map { it.username }
            assertThat(usernames).containsExactlyInAnyOrder("owner", "other")
        }

        @Test
        fun `GET returns 200 with empty list when no members`() {
            val planId = fixture.insertPlan(name = "Empty", ownerId = ownerId)

            val response = restTemplate.exchange(
                "/api/plans/$planId/members",
                HttpMethod.GET,
                entityWithUser(null, ownerId),
                Array<PlanMemberResponse>::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body).isEmpty()
        }
    }

    @Nested
    inner class AddMember {

        @Test
        fun `POST returns 201 with new member`() {
            val planId = fixture.insertPlan(name = "Trip", ownerId = ownerId)

            val response = restTemplate.exchange(
                "/api/plans/$planId/members",
                HttpMethod.POST,
                entityWithUser(AddMemberRequest(email = "other@example.com"), ownerId),
                PlanMemberResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            assertThat(response.body!!.planId).isEqualTo(planId)
            assertThat(response.body!!.userId).isEqualTo(otherUserId)
            assertThat(response.body!!.username).isEqualTo("other")
        }

        @Test
        fun `POST creates user and adds member when email does not exist`() {
            val planId = fixture.insertPlan(name = "Trip", ownerId = ownerId)

            val response = restTemplate.exchange(
                "/api/plans/$planId/members",
                HttpMethod.POST,
                entityWithUser(AddMemberRequest(email = "newuser@example.com"), ownerId),
                PlanMemberResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            assertThat(response.body!!.planId).isEqualTo(planId)
        }

        @Test
        fun `POST returns 409 when user is already a member`() {
            val planId = fixture.insertPlan(name = "Trip", ownerId = ownerId)
            fixture.insertPlanMember(planId = planId, userId = otherUserId)

            val response = restTemplate.exchange(
                "/api/plans/$planId/members",
                HttpMethod.POST,
                entityWithUser(AddMemberRequest(email = "other@example.com"), ownerId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        }

        @Test
        fun `POST returns 400 when email is blank`() {
            val planId = fixture.insertPlan(name = "Trip", ownerId = ownerId)

            val response = restTemplate.exchange(
                "/api/plans/$planId/members",
                HttpMethod.POST,
                entityWithUser(AddMemberRequest(email = ""), ownerId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @Nested
    inner class RemoveMember {

        @Test
        fun `DELETE returns 204 when self-removing`() {
            val planId = fixture.insertPlan(name = "Trip", ownerId = ownerId)
            fixture.insertPlanMember(planId = planId, userId = otherUserId)

            val response = restTemplate.exchange(
                "/api/plans/$planId/members/$otherUserId",
                HttpMethod.DELETE,
                entityWithUser(null, otherUserId),
                Void::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        }

        @Test
        fun `DELETE returns 204 when owner removes member`() {
            val planId = fixture.insertPlan(name = "Trip", ownerId = ownerId)
            fixture.insertPlanMember(planId = planId, userId = otherUserId)

            val response = restTemplate.exchange(
                "/api/plans/$planId/members/$otherUserId",
                HttpMethod.DELETE,
                entityWithUser(null, ownerId),
                Void::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        }

        @Test
        fun `DELETE returns 403 when non-owner tries to remove another user`() {
            val thirdUserId = fixture.insertUser(email = "third@example.com", username = "third")
            val planId = fixture.insertPlan(name = "Trip", ownerId = ownerId)
            fixture.insertPlanMember(planId = planId, userId = otherUserId)
            fixture.insertPlanMember(planId = planId, userId = thirdUserId)

            val response = restTemplate.exchange(
                "/api/plans/$planId/members/$thirdUserId",
                HttpMethod.DELETE,
                entityWithUser(null, otherUserId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        }

        @Test
        fun `DELETE returns 404 when membership does not exist`() {
            val planId = fixture.insertPlan(name = "Trip", ownerId = ownerId)

            val response = restTemplate.exchange(
                "/api/plans/$planId/members/$otherUserId",
                HttpMethod.DELETE,
                entityWithUser(null, otherUserId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @Nested
    inner class ReadYourOwnWrites {

        @Test
        fun `POST plan then GET plans returns the created plan`() {
            val createResponse = restTemplate.exchange(
                "/api/plans",
                HttpMethod.POST,
                entityWithUser(CreatePlanRequest(name = "My Trip"), ownerId),
                PlanResponse::class.java
            )
            assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
            val planId = createResponse.body!!.id

            val getResponse = restTemplate.exchange(
                "/api/plans",
                HttpMethod.GET,
                entityWithUser(null, ownerId),
                Array<PlanResponse>::class.java
            )
            assertThat(getResponse.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(getResponse.body!!.map { it.id }).contains(planId)
        }

        @Test
        fun `POST plan then PUT update then GET plans returns updated name`() {
            val createResponse = restTemplate.exchange(
                "/api/plans",
                HttpMethod.POST,
                entityWithUser(CreatePlanRequest(name = "Old"), ownerId),
                PlanResponse::class.java
            )
            assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
            val planId = createResponse.body!!.id

            val updateResponse = restTemplate.exchange(
                "/api/plans/$planId",
                HttpMethod.PUT,
                entityWithUser(UpdatePlanRequest(name = "New"), ownerId),
                PlanResponse::class.java
            )
            assertThat(updateResponse.statusCode).isEqualTo(HttpStatus.OK)

            val getResponse = restTemplate.exchange(
                "/api/plans",
                HttpMethod.GET,
                entityWithUser(null, ownerId),
                Array<PlanResponse>::class.java
            )
            assertThat(getResponse.statusCode).isEqualTo(HttpStatus.OK)
            val plan = getResponse.body!!.find { it.id == planId }
            assertThat(plan!!.name).isEqualTo("New")
        }

        @Test
        fun `POST plan then DELETE then GET plans does not include it`() {
            val createResponse = restTemplate.exchange(
                "/api/plans",
                HttpMethod.POST,
                entityWithUser(CreatePlanRequest(name = "Doomed"), ownerId),
                PlanResponse::class.java
            )
            assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
            val planId = createResponse.body!!.id

            val deleteResponse = restTemplate.exchange(
                "/api/plans/$planId",
                HttpMethod.DELETE,
                entityWithUser(null, ownerId),
                Void::class.java
            )
            assertThat(deleteResponse.statusCode).isEqualTo(HttpStatus.NO_CONTENT)

            val getResponse = restTemplate.exchange(
                "/api/plans",
                HttpMethod.GET,
                entityWithUser(null, ownerId),
                Array<PlanResponse>::class.java
            )
            assertThat(getResponse.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(getResponse.body!!.map { it.id }).doesNotContain(planId)
        }

        @Test
        fun `POST plan then add member then GET members includes the member`() {
            val createResponse = restTemplate.exchange(
                "/api/plans",
                HttpMethod.POST,
                entityWithUser(CreatePlanRequest(name = "Group Trip"), ownerId),
                PlanResponse::class.java
            )
            assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
            val planId = createResponse.body!!.id

            val addResponse = restTemplate.exchange(
                "/api/plans/$planId/members",
                HttpMethod.POST,
                entityWithUser(AddMemberRequest(email = "other@example.com"), ownerId),
                PlanMemberResponse::class.java
            )
            assertThat(addResponse.statusCode).isEqualTo(HttpStatus.CREATED)

            val membersResponse = restTemplate.exchange(
                "/api/plans/$planId/members",
                HttpMethod.GET,
                entityWithUser(null, ownerId),
                Array<PlanMemberResponse>::class.java
            )
            assertThat(membersResponse.statusCode).isEqualTo(HttpStatus.OK)
            val memberUserIds = membersResponse.body!!.map { it.userId }
            assertThat(memberUserIds).contains(ownerId)
            assertThat(memberUserIds).contains(otherUserId)
        }

        @Test
        fun `PATCH role then GET members reflects the updated role`() {
            val planId = fixture.insertPlan(name = "Trip", ownerId = ownerId)
            fixture.insertPlanMember(planId = planId, userId = ownerId)
            fixture.insertPlanMember(planId = planId, userId = otherUserId)

            val patchResponse = restTemplate.exchange(
                "/api/plans/$planId/members/$otherUserId/role",
                HttpMethod.PATCH,
                entityWithUser(UpdateMemberRoleRequest(role = "manager"), ownerId),
                PlanMemberResponse::class.java
            )
            assertThat(patchResponse.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(patchResponse.body!!.role).isEqualTo("manager")

            val membersResponse = restTemplate.exchange(
                "/api/plans/$planId/members",
                HttpMethod.GET,
                entityWithUser(null, ownerId),
                Array<PlanMemberResponse>::class.java
            )
            assertThat(membersResponse.statusCode).isEqualTo(HttpStatus.OK)
            val promoted = membersResponse.body!!.find { it.userId == otherUserId }
            assertThat(promoted!!.role).isEqualTo("manager")
        }
    }

    @Nested
    inner class UpdateMemberRole {

        @Test
        fun `PATCH returns 200 with promoted member`() {
            val planId = fixture.insertPlan(name = "Trip", ownerId = ownerId)
            fixture.insertPlanMember(planId = planId, userId = ownerId)
            fixture.insertPlanMember(planId = planId, userId = otherUserId)

            val response = restTemplate.exchange(
                "/api/plans/$planId/members/$otherUserId/role",
                HttpMethod.PATCH,
                entityWithUser(UpdateMemberRoleRequest(role = "manager"), ownerId),
                PlanMemberResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.role).isEqualTo("manager")
            assertThat(response.body!!.userId).isEqualTo(otherUserId)
            assertThat(response.body!!.planId).isEqualTo(planId)
            assertThat(response.body!!.username).isEqualTo("other")
        }

        @Test
        fun `PATCH returns 200 when demoting manager to member`() {
            val planId = fixture.insertPlan(name = "Trip", ownerId = ownerId)
            fixture.insertPlanMember(planId = planId, userId = ownerId)
            fixture.insertPlanMember(planId = planId, userId = otherUserId, role = "manager")

            val response = restTemplate.exchange(
                "/api/plans/$planId/members/$otherUserId/role",
                HttpMethod.PATCH,
                entityWithUser(UpdateMemberRoleRequest(role = "member"), ownerId),
                PlanMemberResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.role).isEqualTo("member")
        }

        @Test
        fun `PATCH returns 403 when non-owner requests`() {
            val planId = fixture.insertPlan(name = "Trip", ownerId = ownerId)
            fixture.insertPlanMember(planId = planId, userId = ownerId)
            fixture.insertPlanMember(planId = planId, userId = otherUserId)

            val response = restTemplate.exchange(
                "/api/plans/$planId/members/$ownerId/role",
                HttpMethod.PATCH,
                entityWithUser(UpdateMemberRoleRequest(role = "manager"), otherUserId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        }

        @Test
        fun `PATCH returns 400 when target is the owner`() {
            val planId = fixture.insertPlan(name = "Trip", ownerId = ownerId)
            fixture.insertPlanMember(planId = planId, userId = ownerId)

            val response = restTemplate.exchange(
                "/api/plans/$planId/members/$ownerId/role",
                HttpMethod.PATCH,
                entityWithUser(UpdateMemberRoleRequest(role = "manager"), ownerId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        fun `PATCH returns 404 when target is not a member`() {
            val planId = fixture.insertPlan(name = "Trip", ownerId = ownerId)
            fixture.insertPlanMember(planId = planId, userId = ownerId)
            val nonMemberId = fixture.insertUser(email = "nonmember@example.com", username = "nonmember")

            val response = restTemplate.exchange(
                "/api/plans/$planId/members/$nonMemberId/role",
                HttpMethod.PATCH,
                entityWithUser(UpdateMemberRoleRequest(role = "manager"), ownerId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @Test
        fun `PATCH returns 400 for invalid role value`() {
            val planId = fixture.insertPlan(name = "Trip", ownerId = ownerId)
            fixture.insertPlanMember(planId = planId, userId = ownerId)
            fixture.insertPlanMember(planId = planId, userId = otherUserId)

            val response = restTemplate.exchange(
                "/api/plans/$planId/members/$otherUserId/role",
                HttpMethod.PATCH,
                entityWithUser(UpdateMemberRoleRequest(role = "admin"), ownerId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        fun `PATCH returns 404 when plan does not exist`() {
            val response = restTemplate.exchange(
                "/api/plans/${UUID.randomUUID()}/members/$otherUserId/role",
                HttpMethod.PATCH,
                entityWithUser(UpdateMemberRoleRequest(role = "manager"), ownerId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @Test
        fun `GET members includes role field with default member value`() {
            val planId = fixture.insertPlan(name = "Trip", ownerId = ownerId)
            fixture.insertPlanMember(planId = planId, userId = ownerId)
            fixture.insertPlanMember(planId = planId, userId = otherUserId)

            val response = restTemplate.exchange(
                "/api/plans/$planId/members",
                HttpMethod.GET,
                entityWithUser(null, ownerId),
                Array<PlanMemberResponse>::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body).hasSize(2)
            response.body!!.forEach { member ->
                assertThat(member.role).isEqualTo("member")
            }
        }

        @Test
        fun `GET members shows mixed roles after promotion`() {
            val planId = fixture.insertPlan(name = "Trip", ownerId = ownerId)
            fixture.insertPlanMember(planId = planId, userId = ownerId)
            fixture.insertPlanMember(planId = planId, userId = otherUserId, role = "manager")

            val response = restTemplate.exchange(
                "/api/plans/$planId/members",
                HttpMethod.GET,
                entityWithUser(null, ownerId),
                Array<PlanMemberResponse>::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val memberByUserId = response.body!!.associateBy { it.userId }
            assertThat(memberByUserId[ownerId]!!.role).isEqualTo("member")
            assertThat(memberByUserId[otherUserId]!!.role).isEqualTo("manager")
        }
    }

    private fun entityWithUser(body: Any?, userId: UUID): HttpEntity<Any?> {
        val headers = HttpHeaders()
        headers.set("X-User-Id", userId.toString())
        return HttpEntity(body, headers)
    }
}
