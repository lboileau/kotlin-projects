package com.acme.services.camperservice.features.gearsync.acceptance

import com.acme.services.camperservice.config.TestContainerConfig
import com.acme.services.camperservice.features.gearsync.acceptance.fixture.GearSyncFixture
import com.acme.services.camperservice.features.gearsync.dto.GearSyncResponse
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
class GearSyncAcceptanceTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    private lateinit var fixture: GearSyncFixture
    private lateinit var ownerId: UUID
    private lateinit var memberId: UUID
    private lateinit var planId: UUID

    @BeforeEach
    fun setUp() {
        fixture = GearSyncFixture(jdbcTemplate)
        fixture.truncateAll()
        ownerId = fixture.insertUser(email = "owner@example.com", username = "owner")
        memberId = fixture.insertUser(email = "member@example.com", username = "member")
        planId = fixture.insertPlan(name = "Camping Trip", ownerId = ownerId)
        fixture.insertPlanMember(planId = planId, userId = ownerId)
        fixture.insertPlanMember(planId = planId, userId = memberId)
    }

    @Nested
    inner class SyncGear {

        @Test
        fun `POST returns 200 with empty items when no assignments`() {
            val response = restTemplate.exchange(
                "/api/plans/$planId/gear-sync",
                HttpMethod.POST,
                entityWithUser(ownerId),
                GearSyncResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.items).isEmpty()
        }

        @Test
        fun `POST returns 404 for non-existent plan`() {
            val response = restTemplate.exchange(
                "/api/plans/${UUID.randomUUID()}/gear-sync",
                HttpMethod.POST,
                entityWithUser(ownerId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @Test
        fun `POST creates tent gear for tent assignments`() {
            fixture.insertAssignment(planId = planId, name = "Big Agnes", type = "tent", ownerId = ownerId)

            val response = restTemplate.exchange(
                "/api/plans/$planId/gear-sync",
                HttpMethod.POST,
                entityWithUser(ownerId),
                GearSyncResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val items = response.body!!.items
            val tents = items.find { it.name == "Tents" }
            assertThat(tents).isNotNull
            assertThat(tents!!.quantity).isEqualTo(1)
            assertThat(tents.category).isEqualTo("camp")
        }

        @Test
        fun `POST creates canoe gear with paddles and life jackets based on maxOccupancy`() {
            fixture.insertAssignment(planId = planId, name = "Old Town", type = "canoe", maxOccupancy = 3, ownerId = ownerId)

            val response = restTemplate.exchange(
                "/api/plans/$planId/gear-sync",
                HttpMethod.POST,
                entityWithUser(ownerId),
                GearSyncResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val items = response.body!!.items
            assertThat(items.find { it.name == "Canoes" }!!.quantity).isEqualTo(1)
            assertThat(items.find { it.name == "Paddles" }!!.quantity).isEqualTo(3)
            assertThat(items.find { it.name == "Life Jackets" }!!.quantity).isEqualTo(3)
        }

        @Test
        fun `POST updates existing gear items on re-sync`() {
            // First sync with 1 tent
            fixture.insertAssignment(planId = planId, name = "Tent A", type = "tent", ownerId = ownerId)

            restTemplate.exchange(
                "/api/plans/$planId/gear-sync",
                HttpMethod.POST,
                entityWithUser(ownerId),
                GearSyncResponse::class.java
            )

            // Add a second tent and re-sync
            fixture.insertAssignment(planId = planId, name = "Tent B", type = "tent", ownerId = ownerId)

            val response = restTemplate.exchange(
                "/api/plans/$planId/gear-sync",
                HttpMethod.POST,
                entityWithUser(ownerId),
                GearSyncResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.items.find { it.name == "Tents" }!!.quantity).isEqualTo(2)

            // Verify only 1 "Tents" item in DB (updated, not duplicated)
            val dbItems = fixture.getItemsByPlanId(planId)
            assertThat(dbItems.filter { it["name"] == "Tents" }).hasSize(1)
        }

        @Test
        fun `POST is idempotent`() {
            fixture.insertAssignment(planId = planId, name = "Tent A", type = "tent", ownerId = ownerId)

            val response1 = restTemplate.exchange(
                "/api/plans/$planId/gear-sync",
                HttpMethod.POST,
                entityWithUser(ownerId),
                GearSyncResponse::class.java
            )

            val response2 = restTemplate.exchange(
                "/api/plans/$planId/gear-sync",
                HttpMethod.POST,
                entityWithUser(ownerId),
                GearSyncResponse::class.java
            )

            assertThat(response1.body!!.items).isEqualTo(response2.body!!.items)
        }

        @Test
        fun `POST deletes gear when assignments are removed`() {
            // Create a tent and sync
            val tentId = fixture.insertAssignment(planId = planId, name = "Tent A", type = "tent", ownerId = ownerId)

            restTemplate.exchange(
                "/api/plans/$planId/gear-sync",
                HttpMethod.POST,
                entityWithUser(ownerId),
                GearSyncResponse::class.java
            )

            // Delete the tent from DB and re-sync
            jdbcTemplate.update("DELETE FROM assignments WHERE id = ?", tentId)

            val response = restTemplate.exchange(
                "/api/plans/$planId/gear-sync",
                HttpMethod.POST,
                entityWithUser(ownerId),
                GearSyncResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.items).isEmpty()

            // Verify item was deleted from DB
            val dbItems = fixture.getItemsByPlanId(planId)
            assertThat(dbItems.filter { it["name"] == "Tents" }).isEmpty()
        }
    }

    private fun entityWithUser(userId: UUID): HttpEntity<Any?> {
        val headers = HttpHeaders()
        headers.set("X-User-Id", userId.toString())
        return HttpEntity(null, headers)
    }
}
