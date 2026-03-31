package com.acme.services.camperservice.features.item.acceptance

import com.acme.services.camperservice.config.TestContainerConfig
import com.acme.services.camperservice.features.item.acceptance.fixture.ItemFixture
import com.acme.services.camperservice.features.item.dto.CreateItemRequest
import com.acme.services.camperservice.features.item.dto.ItemResponse
import com.acme.services.camperservice.features.item.dto.UpdateItemRequest
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
class ItemAcceptanceTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    private lateinit var fixture: ItemFixture
    private lateinit var userId: UUID
    private lateinit var planId: UUID

    @BeforeEach
    fun setUp() {
        fixture = ItemFixture(jdbcTemplate)
        fixture.truncateAll()
        userId = fixture.insertUser(email = "testuser@example.com")
        planId = fixture.insertPlan(name = "Test Plan", ownerId = userId)
    }

    @Nested
    inner class CreateItem {

        @Test
        fun `POST creates item for plan and returns 201`() {
            val request = CreateItemRequest(
                name = "Tent",
                category = "shelter",
                quantity = 1,
                packed = false,
                ownerType = "plan",
                ownerId = planId,
            )

            val response = restTemplate.exchange(
                "/api/items",
                HttpMethod.POST,
                entityWithUser(request, userId),
                ItemResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            val body = response.body!!
            assertThat(body.name).isEqualTo("Tent")
            assertThat(body.category).isEqualTo("shelter")
            assertThat(body.quantity).isEqualTo(1)
            assertThat(body.packed).isFalse()
            assertThat(body.planId).isEqualTo(planId)
            assertThat(body.userId).isNull()
            assertThat(body.id).isNotNull()
        }

        @Test
        fun `POST creates personal item for user with planId and returns 201`() {
            val request = CreateItemRequest(
                name = "Headlamp",
                category = "lighting",
                quantity = 2,
                packed = true,
                ownerType = "user",
                ownerId = userId,
                planId = planId,
            )

            val response = restTemplate.exchange(
                "/api/items",
                HttpMethod.POST,
                entityWithUser(request, userId),
                ItemResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            val body = response.body!!
            assertThat(body.name).isEqualTo("Headlamp")
            assertThat(body.category).isEqualTo("lighting")
            assertThat(body.quantity).isEqualTo(2)
            assertThat(body.packed).isTrue()
            assertThat(body.userId).isEqualTo(userId)
            assertThat(body.planId).isEqualTo(planId)
        }

        @Test
        fun `POST returns 400 when user item has no planId`() {
            val request = CreateItemRequest(
                name = "Headlamp",
                category = "lighting",
                quantity = 1,
                packed = false,
                ownerType = "user",
                ownerId = userId,
            )

            val response = restTemplate.exchange(
                "/api/items",
                HttpMethod.POST,
                entityWithUser(request, userId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        fun `POST returns 400 when ownerType is invalid`() {
            val request = CreateItemRequest(
                name = "Tent",
                category = "shelter",
                quantity = 1,
                packed = false,
                ownerType = "invalid",
                ownerId = planId,
            )

            val response = restTemplate.exchange(
                "/api/items",
                HttpMethod.POST,
                entityWithUser(request, userId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        fun `POST returns 400 when name is blank`() {
            val request = CreateItemRequest(
                name = "",
                category = "shelter",
                quantity = 1,
                packed = false,
                ownerType = "plan",
                ownerId = planId,
            )

            val response = restTemplate.exchange(
                "/api/items",
                HttpMethod.POST,
                entityWithUser(request, userId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @Nested
    inner class GetItem {

        @Test
        fun `GET returns 200 with item when found`() {
            val itemId = fixture.insertItem(planId = planId, name = "Sleeping Bag", category = "sleep", quantity = 1, packed = false)

            val response = restTemplate.exchange(
                "/api/items/$itemId",
                HttpMethod.GET,
                entityWithUser(null, userId),
                ItemResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val body = response.body!!
            assertThat(body.id).isEqualTo(itemId)
            assertThat(body.name).isEqualTo("Sleeping Bag")
            assertThat(body.category).isEqualTo("sleep")
        }

        @Test
        fun `GET returns 404 when item does not exist`() {
            val response = restTemplate.exchange(
                "/api/items/${UUID.randomUUID()}",
                HttpMethod.GET,
                entityWithUser(null, userId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @Nested
    inner class ListItems {

        @Test
        fun `GET returns items by plan`() {
            fixture.insertItem(planId = planId, name = "Tent", category = "shelter")
            fixture.insertItem(planId = planId, name = "Stove", category = "cooking")

            val response = restTemplate.exchange(
                "/api/items?ownerType=plan&ownerId=$planId",
                HttpMethod.GET,
                entityWithUser(null, userId),
                Array<ItemResponse>::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body).hasSize(2)
            assertThat(response.body!!.map { it.name }).containsExactlyInAnyOrder("Tent", "Stove")
        }

        @Test
        fun `GET by plan excludes personal items`() {
            fixture.insertItem(planId = planId, name = "Shared Tent", category = "shelter")
            fixture.insertItem(planId = planId, name = "Shared Stove", category = "cooking")
            fixture.insertItem(planId = planId, userId = userId, name = "Personal Headlamp", category = "lighting")
            fixture.insertItem(planId = planId, userId = userId, name = "Personal Knife", category = "tools")

            val response = restTemplate.exchange(
                "/api/items?ownerType=plan&ownerId=$planId",
                HttpMethod.GET,
                entityWithUser(null, userId),
                Array<ItemResponse>::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body).hasSize(2)
            assertThat(response.body!!.map { it.name }).containsExactlyInAnyOrder("Shared Tent", "Shared Stove")
            assertThat(response.body!!.all { it.userId == null }).isTrue()
        }

        @Test
        fun `GET returns personal items scoped to plan`() {
            fixture.insertItem(planId = planId, userId = userId, name = "Headlamp", category = "lighting")
            fixture.insertItem(planId = planId, userId = userId, name = "Knife", category = "tools")

            val plan2Id = fixture.insertPlan(name = "Other Plan", ownerId = userId)
            fixture.insertItem(planId = plan2Id, userId = userId, name = "Other Item", category = "misc")

            val response = restTemplate.exchange(
                "/api/items?ownerType=user&ownerId=$userId&planId=$planId",
                HttpMethod.GET,
                entityWithUser(null, userId),
                Array<ItemResponse>::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body).hasSize(2)
            assertThat(response.body!!.map { it.name }).containsExactlyInAnyOrder("Headlamp", "Knife")
        }
    }

    @Nested
    inner class UpdateItem {

        @Test
        fun `PUT updates item and returns 200`() {
            val itemId = fixture.insertItem(planId = planId, name = "Old Tent", category = "shelter", quantity = 1, packed = false)

            val request = UpdateItemRequest(
                name = "New Tent",
                category = "shelter",
                quantity = 2,
                packed = true,
            )

            val response = restTemplate.exchange(
                "/api/items/$itemId",
                HttpMethod.PUT,
                entityWithUser(request, userId),
                ItemResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val body = response.body!!
            assertThat(body.name).isEqualTo("New Tent")
            assertThat(body.quantity).isEqualTo(2)
            assertThat(body.packed).isTrue()
        }

        @Test
        fun `PUT returns 404 when item does not exist`() {
            val request = UpdateItemRequest(
                name = "Ghost",
                category = "mystery",
                quantity = 1,
                packed = false,
            )

            val response = restTemplate.exchange(
                "/api/items/${UUID.randomUUID()}",
                HttpMethod.PUT,
                entityWithUser(request, userId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @Nested
    inner class DeleteItem {

        @Test
        fun `DELETE removes item and returns 204`() {
            val itemId = fixture.insertItem(planId = planId, name = "Doomed Item", category = "misc")

            val response = restTemplate.exchange(
                "/api/items/$itemId",
                HttpMethod.DELETE,
                entityWithUser(null, userId),
                Void::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)

            val getResponse = restTemplate.exchange(
                "/api/items/$itemId",
                HttpMethod.GET,
                entityWithUser(null, userId),
                Map::class.java
            )
            assertThat(getResponse.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @Test
        fun `DELETE returns 404 when item does not exist`() {
            val response = restTemplate.exchange(
                "/api/items/${UUID.randomUUID()}",
                HttpMethod.DELETE,
                entityWithUser(null, userId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @Nested
    inner class SharedGearAuthorization {

        private lateinit var managerId: UUID
        private lateinit var memberId: UUID

        @BeforeEach
        fun setUpRoles() {
            managerId = fixture.insertUser(email = "manager@example.com")
            memberId = fixture.insertUser(email = "member@example.com")
            fixture.insertPlanMember(planId = planId, userId = userId) // owner as member
            fixture.insertPlanMember(planId = planId, userId = managerId, role = "manager")
            fixture.insertPlanMember(planId = planId, userId = memberId, role = "member")
        }

        @Test
        fun `POST shared gear returns 403 for regular member`() {
            val request = CreateItemRequest(
                name = "Tent", category = "shelter", quantity = 1, packed = false,
                ownerType = "plan", ownerId = planId,
            )

            val response = restTemplate.exchange(
                "/api/items",
                HttpMethod.POST,
                entityWithUser(request, memberId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        }

        @Test
        fun `POST shared gear returns 201 for manager`() {
            val request = CreateItemRequest(
                name = "Tent", category = "shelter", quantity = 1, packed = false,
                ownerType = "plan", ownerId = planId,
            )

            val response = restTemplate.exchange(
                "/api/items",
                HttpMethod.POST,
                entityWithUser(request, managerId),
                ItemResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            assertThat(response.body!!.name).isEqualTo("Tent")
        }

        @Test
        fun `POST shared gear returns 201 for owner`() {
            val request = CreateItemRequest(
                name = "Stove", category = "cooking", quantity = 1, packed = false,
                ownerType = "plan", ownerId = planId,
            )

            val response = restTemplate.exchange(
                "/api/items",
                HttpMethod.POST,
                entityWithUser(request, userId),
                ItemResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        }

        @Test
        fun `PUT shared gear returns 403 for regular member`() {
            val itemId = fixture.insertItem(planId = planId, name = "Tent", category = "shelter")
            val request = UpdateItemRequest(name = "Big Tent", category = "shelter", quantity = 2, packed = false)

            val response = restTemplate.exchange(
                "/api/items/$itemId",
                HttpMethod.PUT,
                entityWithUser(request, memberId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        }

        @Test
        fun `PUT shared gear returns 200 for manager`() {
            val itemId = fixture.insertItem(planId = planId, name = "Tent", category = "shelter")
            val request = UpdateItemRequest(name = "Big Tent", category = "shelter", quantity = 2, packed = false)

            val response = restTemplate.exchange(
                "/api/items/$itemId",
                HttpMethod.PUT,
                entityWithUser(request, managerId),
                ItemResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.name).isEqualTo("Big Tent")
        }

        @Test
        fun `DELETE shared gear returns 403 for regular member`() {
            val itemId = fixture.insertItem(planId = planId, name = "Tent", category = "shelter")

            val response = restTemplate.exchange(
                "/api/items/$itemId",
                HttpMethod.DELETE,
                entityWithUser(null, memberId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        }

        @Test
        fun `DELETE shared gear returns 204 for manager`() {
            val itemId = fixture.insertItem(planId = planId, name = "Tent", category = "shelter")

            val response = restTemplate.exchange(
                "/api/items/$itemId",
                HttpMethod.DELETE,
                entityWithUser(null, managerId),
                Void::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        }

        @Test
        fun `POST personal gear succeeds for regular member`() {
            val request = CreateItemRequest(
                name = "Flashlight", category = "lighting", quantity = 1, packed = false,
                ownerType = "user", ownerId = memberId, planId = planId,
            )

            val response = restTemplate.exchange(
                "/api/items",
                HttpMethod.POST,
                entityWithUser(request, memberId),
                ItemResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            assertThat(response.body!!.userId).isEqualTo(memberId)
        }

        @Test
        fun `PUT personal gear succeeds for regular member`() {
            val itemId = fixture.insertItem(planId = planId, userId = memberId, name = "Flashlight", category = "lighting")
            val request = UpdateItemRequest(name = "LED Flashlight", category = "lighting", quantity = 1, packed = true)

            val response = restTemplate.exchange(
                "/api/items/$itemId",
                HttpMethod.PUT,
                entityWithUser(request, memberId),
                ItemResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.name).isEqualTo("LED Flashlight")
        }

        @Test
        fun `DELETE personal gear succeeds for regular member`() {
            val itemId = fixture.insertItem(planId = planId, userId = memberId, name = "Flashlight", category = "lighting")

            val response = restTemplate.exchange(
                "/api/items/$itemId",
                HttpMethod.DELETE,
                entityWithUser(null, memberId),
                Void::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        }
    }

    @Nested
    inner class ReadYourOwnWrites {

        @Test
        fun `POST then GET then PUT then list then DELETE full lifecycle`() {
            // 1. Create item
            val createRequest = CreateItemRequest(
                name = "Camping Chair",
                category = "furniture",
                quantity = 1,
                packed = false,
                ownerType = "plan",
                ownerId = planId,
            )
            val createResponse = restTemplate.exchange(
                "/api/items",
                HttpMethod.POST,
                entityWithUser(createRequest, userId),
                ItemResponse::class.java
            )
            assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
            val itemId = createResponse.body!!.id

            // 2. Get item by ID
            val getResponse = restTemplate.exchange(
                "/api/items/$itemId",
                HttpMethod.GET,
                entityWithUser(null, userId),
                ItemResponse::class.java
            )
            assertThat(getResponse.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(getResponse.body!!.name).isEqualTo("Camping Chair")

            // 3. Update item
            val updateRequest = UpdateItemRequest(
                name = "Folding Chair",
                category = "furniture",
                quantity = 2,
                packed = true,
            )
            val updateResponse = restTemplate.exchange(
                "/api/items/$itemId",
                HttpMethod.PUT,
                entityWithUser(updateRequest, userId),
                ItemResponse::class.java
            )
            assertThat(updateResponse.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(updateResponse.body!!.name).isEqualTo("Folding Chair")
            assertThat(updateResponse.body!!.quantity).isEqualTo(2)
            assertThat(updateResponse.body!!.packed).isTrue()

            // 4. List by owner — verify updated item appears
            val listResponse = restTemplate.exchange(
                "/api/items?ownerType=plan&ownerId=$planId",
                HttpMethod.GET,
                entityWithUser(null, userId),
                Array<ItemResponse>::class.java
            )
            assertThat(listResponse.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(listResponse.body).hasSize(1)
            assertThat(listResponse.body!![0].name).isEqualTo("Folding Chair")

            // 5. Delete item
            val deleteResponse = restTemplate.exchange(
                "/api/items/$itemId",
                HttpMethod.DELETE,
                entityWithUser(null, userId),
                Void::class.java
            )
            assertThat(deleteResponse.statusCode).isEqualTo(HttpStatus.NO_CONTENT)

            // 6. Verify gone
            val verifyResponse = restTemplate.exchange(
                "/api/items/$itemId",
                HttpMethod.GET,
                entityWithUser(null, userId),
                Map::class.java
            )
            assertThat(verifyResponse.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    private fun entityWithUser(body: Any?, userId: UUID): HttpEntity<Any?> {
        val headers = HttpHeaders()
        headers.set("X-User-Id", userId.toString())
        return HttpEntity(body, headers)
    }
}
