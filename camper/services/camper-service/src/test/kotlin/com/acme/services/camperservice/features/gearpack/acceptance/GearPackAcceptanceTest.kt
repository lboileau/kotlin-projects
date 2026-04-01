package com.acme.services.camperservice.features.gearpack.acceptance

import com.acme.services.camperservice.config.TestContainerConfig
import com.acme.services.camperservice.features.gearpack.acceptance.fixture.GearPackFixture
import com.acme.services.camperservice.features.gearpack.acceptance.fixture.GearPackFixture.Companion.COOKING_EQUIPMENT_PACK_ID
import com.acme.services.camperservice.features.gearpack.dto.ApplyGearPackRequest
import com.acme.services.camperservice.features.gearpack.dto.ApplyGearPackResponse
import com.acme.services.camperservice.features.gearpack.dto.GearPackDetailResponse
import com.acme.services.camperservice.features.gearpack.dto.GearPackSummaryResponse
import com.acme.services.camperservice.features.item.dto.ItemResponse
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
class GearPackAcceptanceTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    private lateinit var fixture: GearPackFixture
    private lateinit var ownerId: UUID
    private lateinit var planId: UUID

    @BeforeEach
    fun setUp() {
        fixture = GearPackFixture(jdbcTemplate)
        fixture.truncateAll()
        ownerId = fixture.insertUser(email = "owner@example.com")
        planId = fixture.insertPlan(name = "Camping Trip", ownerId = ownerId)
        fixture.insertPlanMember(planId = planId, userId = ownerId, role = "member")
    }

    @Nested
    inner class ListGearPacks {

        @Test
        fun `GET returns 200 with list of gear packs`() {
            val response = restTemplate.exchange(
                "/api/gear-packs",
                HttpMethod.GET,
                entityWithUser(null, ownerId),
                Array<GearPackSummaryResponse>::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val packs = response.body!!
            assertThat(packs).isNotEmpty

            val cookingPack = packs.find { it.id == COOKING_EQUIPMENT_PACK_ID }
            assertThat(cookingPack).isNotNull
            assertThat(cookingPack!!.name).isEqualTo("Cooking Equipment")
            assertThat(cookingPack.description).isEqualTo("Essential cooking gear for campfire meals. Includes pots, pans, utensils, and tableware.")
            assertThat(cookingPack.itemCount).isEqualTo(12)
            assertThat(cookingPack.createdAt).isNotNull()
            assertThat(cookingPack.updatedAt).isNotNull()
        }
    }

    @Nested
    inner class GetGearPackById {

        @Test
        fun `GET returns 200 with pack details including items`() {
            val response = restTemplate.exchange(
                "/api/gear-packs/$COOKING_EQUIPMENT_PACK_ID",
                HttpMethod.GET,
                entityWithUser(null, ownerId),
                GearPackDetailResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val pack = response.body!!
            assertThat(pack.id).isEqualTo(COOKING_EQUIPMENT_PACK_ID)
            assertThat(pack.name).isEqualTo("Cooking Equipment")
            assertThat(pack.items).hasSize(12)
            assertThat(pack.createdAt).isNotNull()
            assertThat(pack.updatedAt).isNotNull()
        }

        @Test
        fun `GET returns items ordered by sortOrder`() {
            val response = restTemplate.exchange(
                "/api/gear-packs/$COOKING_EQUIPMENT_PACK_ID",
                HttpMethod.GET,
                entityWithUser(null, ownerId),
                GearPackDetailResponse::class.java
            )

            val items = response.body!!.items
            assertThat(items.map { it.sortOrder }).isSorted
            assertThat(items.first().name).isEqualTo("Cast Iron Pan")
            assertThat(items.last().name).isEqualTo("Cutlery Set")
        }

        @Test
        fun `GET returns items with correct scalable flags`() {
            val response = restTemplate.exchange(
                "/api/gear-packs/$COOKING_EQUIPMENT_PACK_ID",
                HttpMethod.GET,
                entityWithUser(null, ownerId),
                GearPackDetailResponse::class.java
            )

            val items = response.body!!.items
            val nonScalable = items.filter { !it.scalable }
            val scalable = items.filter { it.scalable }

            assertThat(nonScalable).hasSize(8)
            assertThat(scalable).hasSize(4)
            assertThat(scalable.map { it.name }).containsExactlyInAnyOrder("Plates", "Cups", "Bowls", "Cutlery Set")
        }

        @Test
        fun `GET returns 404 for nonexistent pack ID`() {
            val response = restTemplate.exchange(
                "/api/gear-packs/${UUID.randomUUID()}",
                HttpMethod.GET,
                entityWithUser(null, ownerId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
            assertThat(response.body!!["code"]).isEqualTo("NOT_FOUND")
        }
    }

    @Nested
    inner class ApplyGearPack {

        @Test
        fun `POST returns 201 with created items when applied by plan owner`() {
            val request = ApplyGearPackRequest(planId = planId, groupSize = 1)

            val response = restTemplate.exchange(
                "/api/gear-packs/$COOKING_EQUIPMENT_PACK_ID/apply",
                HttpMethod.POST,
                entityWithUser(request, ownerId),
                ApplyGearPackResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            val body = response.body!!
            assertThat(body.appliedCount).isEqualTo(12)
            assertThat(body.items).hasSize(12)
            assertThat(body.items.all { it.planId == planId }).isTrue()
            assertThat(body.items.all { it.userId == null }).isTrue()
            assertThat(body.items.all { !it.packed }).isTrue()
        }

        @Test
        fun `POST correctly scales scalable items by groupSize`() {
            val groupSize = 4
            val request = ApplyGearPackRequest(planId = planId, groupSize = groupSize)

            val response = restTemplate.exchange(
                "/api/gear-packs/$COOKING_EQUIPMENT_PACK_ID/apply",
                HttpMethod.POST,
                entityWithUser(request, ownerId),
                ApplyGearPackResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            val items = response.body!!.items

            // Scalable items should be multiplied by groupSize
            val plates = items.find { it.name == "Plates" }
            assertThat(plates!!.quantity).isEqualTo(4)

            val cups = items.find { it.name == "Cups" }
            assertThat(cups!!.quantity).isEqualTo(4)

            val bowls = items.find { it.name == "Bowls" }
            assertThat(bowls!!.quantity).isEqualTo(4)

            val cutlery = items.find { it.name == "Cutlery Set" }
            assertThat(cutlery!!.quantity).isEqualTo(4)
        }

        @Test
        fun `POST does not scale non-scalable items`() {
            val request = ApplyGearPackRequest(planId = planId, groupSize = 4)

            val response = restTemplate.exchange(
                "/api/gear-packs/$COOKING_EQUIPMENT_PACK_ID/apply",
                HttpMethod.POST,
                entityWithUser(request, ownerId),
                ApplyGearPackResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            val items = response.body!!.items

            val castIronPan = items.find { it.name == "Cast Iron Pan" }
            assertThat(castIronPan!!.quantity).isEqualTo(1)

            val grillGrate = items.find { it.name == "Grill Grate" }
            assertThat(grillGrate!!.quantity).isEqualTo(1)

            val largePot = items.find { it.name == "Large Pot" }
            assertThat(largePot!!.quantity).isEqualTo(1)
        }

        @Test
        fun `POST returns 201 for manager role`() {
            val managerId = fixture.insertUser(email = "manager@example.com")
            fixture.insertPlanMember(planId = planId, userId = managerId, role = "manager")

            val request = ApplyGearPackRequest(planId = planId, groupSize = 1)

            val response = restTemplate.exchange(
                "/api/gear-packs/$COOKING_EQUIPMENT_PACK_ID/apply",
                HttpMethod.POST,
                entityWithUser(request, managerId),
                ApplyGearPackResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            val body = response.body!!
            assertThat(body.appliedCount).isEqualTo(12)
            assertThat(body.items).hasSize(12)
            assertThat(body.items.all { it.planId == planId }).isTrue()
        }

        @Test
        fun `POST returns 403 for regular member`() {
            val memberId = fixture.insertUser(email = "member@example.com")
            fixture.insertPlanMember(planId = planId, userId = memberId, role = "member")

            val request = ApplyGearPackRequest(planId = planId, groupSize = 1)

            val response = restTemplate.exchange(
                "/api/gear-packs/$COOKING_EQUIPMENT_PACK_ID/apply",
                HttpMethod.POST,
                entityWithUser(request, memberId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            assertThat(response.body!!["code"]).isEqualTo("FORBIDDEN")
        }

        @Test
        fun `POST returns 403 for non-member user`() {
            val nonMemberId = fixture.insertUser(email = "stranger@example.com")

            val request = ApplyGearPackRequest(planId = planId, groupSize = 1)

            val response = restTemplate.exchange(
                "/api/gear-packs/$COOKING_EQUIPMENT_PACK_ID/apply",
                HttpMethod.POST,
                entityWithUser(request, nonMemberId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            assertThat(response.body!!["code"]).isEqualTo("FORBIDDEN")
        }

        @Test
        fun `POST returns 403 for nonexistent planId`() {
            val request = ApplyGearPackRequest(planId = UUID.randomUUID(), groupSize = 1)

            val response = restTemplate.exchange(
                "/api/gear-packs/$COOKING_EQUIPMENT_PACK_ID/apply",
                HttpMethod.POST,
                entityWithUser(request, ownerId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            assertThat(response.body!!["code"]).isEqualTo("FORBIDDEN")
        }

        @Test
        fun `POST returns 404 for nonexistent pack ID`() {
            val request = ApplyGearPackRequest(planId = planId, groupSize = 1)

            val response = restTemplate.exchange(
                "/api/gear-packs/${UUID.randomUUID()}/apply",
                HttpMethod.POST,
                entityWithUser(request, ownerId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
            assertThat(response.body!!["code"]).isEqualTo("NOT_FOUND")
        }

        @Test
        fun `POST returns 400 for groupSize of zero`() {
            val request = ApplyGearPackRequest(planId = planId, groupSize = 0)

            val response = restTemplate.exchange(
                "/api/gear-packs/$COOKING_EQUIPMENT_PACK_ID/apply",
                HttpMethod.POST,
                entityWithUser(request, ownerId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(response.body!!["code"]).isEqualTo("BAD_REQUEST")
            assertThat(response.body!!["message"] as String).contains("groupSize")
        }

        @Test
        fun `POST returns 400 for negative groupSize`() {
            val request = ApplyGearPackRequest(planId = planId, groupSize = -1)

            val response = restTemplate.exchange(
                "/api/gear-packs/$COOKING_EQUIPMENT_PACK_ID/apply",
                HttpMethod.POST,
                entityWithUser(request, ownerId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(response.body!!["code"]).isEqualTo("BAD_REQUEST")
            assertThat(response.body!!["message"] as String).contains("groupSize")
        }
    }

    @Nested
    inner class ApplyGearPackSetsGearPackId {

        @Test
        fun `POST apply sets gearPackId on all created items`() {
            val request = ApplyGearPackRequest(planId = planId, groupSize = 1)

            val response = restTemplate.exchange(
                "/api/gear-packs/$COOKING_EQUIPMENT_PACK_ID/apply",
                HttpMethod.POST,
                entityWithUser(request, ownerId),
                ApplyGearPackResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            val items = response.body!!.items
            assertThat(items).hasSize(12)
            assertThat(items.all { it.gearPackId == COOKING_EQUIPMENT_PACK_ID }).isTrue()
        }

        @Test
        fun `POST apply then GET items list shows gearPackId and gearPackName`() {
            val request = ApplyGearPackRequest(planId = planId, groupSize = 1)

            val applyResponse = restTemplate.exchange(
                "/api/gear-packs/$COOKING_EQUIPMENT_PACK_ID/apply",
                HttpMethod.POST,
                entityWithUser(request, ownerId),
                ApplyGearPackResponse::class.java
            )
            assertThat(applyResponse.statusCode).isEqualTo(HttpStatus.CREATED)

            val itemsResponse = restTemplate.exchange(
                "/api/items?ownerType=plan&ownerId=$planId",
                HttpMethod.GET,
                entityWithUser(null, ownerId),
                Array<ItemResponse>::class.java
            )

            assertThat(itemsResponse.statusCode).isEqualTo(HttpStatus.OK)
            val items = itemsResponse.body!!
            assertThat(items).hasSize(12)
            assertThat(items.all { it.gearPackId == COOKING_EQUIPMENT_PACK_ID }).isTrue()
            assertThat(items.all { it.gearPackName == "Cooking Equipment" }).isTrue()
        }
    }

    @Nested
    inner class ReadYourOwnWrites {

        @Test
        fun `apply pack then GET items shows the created items`() {
            // Apply the cooking equipment pack
            val applyRequest = ApplyGearPackRequest(planId = planId, groupSize = 3)
            val applyResponse = restTemplate.exchange(
                "/api/gear-packs/$COOKING_EQUIPMENT_PACK_ID/apply",
                HttpMethod.POST,
                entityWithUser(applyRequest, ownerId),
                ApplyGearPackResponse::class.java
            )
            assertThat(applyResponse.statusCode).isEqualTo(HttpStatus.CREATED)

            // Verify items are visible via the items API
            val itemsResponse = restTemplate.exchange(
                "/api/items?ownerType=plan&ownerId=$planId",
                HttpMethod.GET,
                entityWithUser(null, ownerId),
                Array<ItemResponse>::class.java
            )

            assertThat(itemsResponse.statusCode).isEqualTo(HttpStatus.OK)
            val items = itemsResponse.body!!
            assertThat(items).hasSize(12)

            // Verify scaling: scalable items should be 3x, non-scalable stay at 1
            val plates = items.find { it.name == "Plates" }
            assertThat(plates!!.quantity).isEqualTo(3)

            val castIronPan = items.find { it.name == "Cast Iron Pan" }
            assertThat(castIronPan!!.quantity).isEqualTo(1)

            // All items should be shared gear (no userId) under the plan
            assertThat(items.all { it.planId == planId }).isTrue()
            assertThat(items.all { it.userId == null }).isTrue()
        }

        @Test
        fun `apply pack then verify items in database`() {
            val applyRequest = ApplyGearPackRequest(planId = planId, groupSize = 2)
            val applyResponse = restTemplate.exchange(
                "/api/gear-packs/$COOKING_EQUIPMENT_PACK_ID/apply",
                HttpMethod.POST,
                entityWithUser(applyRequest, ownerId),
                ApplyGearPackResponse::class.java
            )
            assertThat(applyResponse.statusCode).isEqualTo(HttpStatus.CREATED)

            // Verify directly in database
            val dbItems = fixture.getItemsByPlanId(planId)
            assertThat(dbItems).hasSize(12)

            // All items are kitchen category
            assertThat(dbItems.all { it["category"] == "kitchen" }).isTrue()

            // Verify a scalable item's quantity
            val bowls = dbItems.find { it["name"] == "Bowls" }
            assertThat(bowls!!["quantity"]).isEqualTo(2)

            // Verify a non-scalable item's quantity
            val spatula = dbItems.find { it["name"] == "Spatula" }
            assertThat(spatula!!["quantity"]).isEqualTo(1)

            // Verify gear_pack_id is stored on all items in the database
            assertThat(dbItems.all { it["gear_pack_id"] == COOKING_EQUIPMENT_PACK_ID }).isTrue()
        }
    }

    private fun entityWithUser(body: Any?, userId: UUID): HttpEntity<Any?> {
        val headers = HttpHeaders()
        headers.set("X-User-Id", userId.toString())
        if (body != null) {
            headers.set("Content-Type", "application/json")
        }
        return HttpEntity(body, headers)
    }
}
