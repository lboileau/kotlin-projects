package com.acme.services.camperservice.features.gearpack.acceptance

import com.acme.services.camperservice.config.TestContainerConfig
import com.acme.services.camperservice.features.gearpack.acceptance.fixture.GearPackFixture
import com.acme.services.camperservice.features.gearpack.acceptance.fixture.GearPackFixture.Companion.COOKING_EQUIPMENT_ITEM_ID
import com.acme.services.camperservice.features.gearpack.acceptance.fixture.GearPackFixture.Companion.COOKING_EQUIPMENT_PACK_ID
import com.acme.services.camperservice.features.gearpack.dto.AddGearPackItemRequest
import com.acme.services.camperservice.features.gearpack.dto.ApplyGearPackRequest
import com.acme.services.camperservice.features.gearpack.dto.ApplyGearPackResponse
import com.acme.services.camperservice.features.gearpack.dto.CreateGearPackRequest
import com.acme.services.camperservice.features.gearpack.dto.GearPackDetailResponse
import com.acme.services.camperservice.features.gearpack.dto.GearPackItemResponse
import com.acme.services.camperservice.features.gearpack.dto.GearPackItemSearchResultResponse
import com.acme.services.camperservice.features.gearpack.dto.GearPackSummaryResponse
import com.acme.services.camperservice.features.gearpack.dto.UpdateGearPackItemRequest
import com.acme.services.camperservice.features.gearpack.dto.UpdateGearPackRequest
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
            assertThat(cookingPack.createdBy).isNull()
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
            assertThat(pack.createdBy).isNull()
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

    @Nested
    inner class CreateGearPack {

        @Test
        fun `POST creates gear pack and returns 201 with correct shape`() {
            val request = CreateGearPackRequest(name = "My Hiking Pack", description = "For day hikes")

            val response = restTemplate.exchange(
                "/api/gear-packs",
                HttpMethod.POST,
                entityWithUser(request, ownerId),
                GearPackDetailResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            val body = response.body!!
            assertThat(body.name).isEqualTo("My Hiking Pack")
            assertThat(body.description).isEqualTo("For day hikes")
            assertThat(body.createdBy).isEqualTo(ownerId)
            assertThat(body.items).isEmpty()
            assertThat(body.id).isNotNull()
            assertThat(body.createdAt).isNotNull()
            assertThat(body.updatedAt).isNotNull()
        }

        @Test
        fun `POST returns 409 on duplicate pack name`() {
            val request = CreateGearPackRequest(name = "Unique Pack Name")
            restTemplate.exchange(
                "/api/gear-packs", HttpMethod.POST,
                entityWithUser(request, ownerId), GearPackDetailResponse::class.java
            )

            val response = restTemplate.exchange(
                "/api/gear-packs", HttpMethod.POST,
                entityWithUser(request, ownerId), Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
            assertThat(response.body!!["code"]).isEqualTo("CONFLICT")
        }

        @Test
        fun `POST returns 400 when name is blank`() {
            val request = CreateGearPackRequest(name = "   ")

            val response = restTemplate.exchange(
                "/api/gear-packs", HttpMethod.POST,
                entityWithUser(request, ownerId), Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(response.body!!["code"]).isEqualTo("BAD_REQUEST")
        }
    }

    @Nested
    inner class UpdateGearPack {

        @Test
        fun `PUT updates gear pack name and returns 200`() {
            val packId = fixture.insertGearPack(name = "Original Name", createdBy = ownerId)
            val request = UpdateGearPackRequest(name = "Updated Name", description = "New desc")

            val response = restTemplate.exchange(
                "/api/gear-packs/$packId",
                HttpMethod.PUT,
                entityWithUser(request, ownerId),
                GearPackDetailResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val body = response.body!!
            assertThat(body.name).isEqualTo("Updated Name")
            assertThat(body.description).isEqualTo("New desc")
            assertThat(body.createdBy).isEqualTo(ownerId)
        }

        @Test
        fun `PUT returns 403 when non-creator tries to update`() {
            val packId = fixture.insertGearPack(name = "Creator's Pack", createdBy = ownerId)
            val otherId = fixture.insertUser(email = "other@example.com")
            val request = UpdateGearPackRequest(name = "Hijacked Name")

            val response = restTemplate.exchange(
                "/api/gear-packs/$packId", HttpMethod.PUT,
                entityWithUser(request, otherId), Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            assertThat(response.body!!["code"]).isEqualTo("FORBIDDEN")
        }

        @Test
        fun `PUT returns 403 when updating system pack`() {
            val request = UpdateGearPackRequest(name = "Hacked System Pack")

            val response = restTemplate.exchange(
                "/api/gear-packs/$COOKING_EQUIPMENT_PACK_ID", HttpMethod.PUT,
                entityWithUser(request, ownerId), Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            assertThat(response.body!!["code"]).isEqualTo("FORBIDDEN")
        }

        @Test
        fun `PUT returns 409 when renaming to an existing pack name`() {
            fixture.insertGearPack(name = "Taken Name", createdBy = ownerId)
            val pack2Id = fixture.insertGearPack(name = "Different Name", createdBy = ownerId)
            val request = UpdateGearPackRequest(name = "Taken Name")

            val response = restTemplate.exchange(
                "/api/gear-packs/$pack2Id", HttpMethod.PUT,
                entityWithUser(request, ownerId), Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
            assertThat(response.body!!["code"]).isEqualTo("CONFLICT")
        }

        @Test
        fun `PUT returns 404 when pack does not exist`() {
            val request = UpdateGearPackRequest(name = "Ghost Pack")

            val response = restTemplate.exchange(
                "/api/gear-packs/${UUID.randomUUID()}", HttpMethod.PUT,
                entityWithUser(request, ownerId), Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
            assertThat(response.body!!["code"]).isEqualTo("NOT_FOUND")
        }
    }

    @Nested
    inner class DeleteGearPack {

        @Test
        fun `DELETE removes gear pack and returns 204`() {
            val packId = fixture.insertGearPack(name = "Pack To Delete", createdBy = ownerId)

            val response = restTemplate.exchange(
                "/api/gear-packs/$packId", HttpMethod.DELETE,
                entityWithUser(null, ownerId), Void::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        }

        @Test
        fun `DELETE returns 403 when non-creator tries to delete`() {
            val packId = fixture.insertGearPack(name = "Protected Pack", createdBy = ownerId)
            val otherId = fixture.insertUser(email = "other@example.com")

            val response = restTemplate.exchange(
                "/api/gear-packs/$packId", HttpMethod.DELETE,
                entityWithUser(null, otherId), Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            assertThat(response.body!!["code"]).isEqualTo("FORBIDDEN")
        }

        @Test
        fun `DELETE returns 403 when deleting system pack`() {
            val response = restTemplate.exchange(
                "/api/gear-packs/$COOKING_EQUIPMENT_PACK_ID", HttpMethod.DELETE,
                entityWithUser(null, ownerId), Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            assertThat(response.body!!["code"]).isEqualTo("FORBIDDEN")
        }

        @Test
        fun `DELETE returns 404 when pack does not exist`() {
            val response = restTemplate.exchange(
                "/api/gear-packs/${UUID.randomUUID()}", HttpMethod.DELETE,
                entityWithUser(null, ownerId), Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
            assertThat(response.body!!["code"]).isEqualTo("NOT_FOUND")
        }

        @Test
        fun `DELETE sets gear_pack_id to null on items referencing the pack`() {
            val packId = fixture.insertGearPack(name = "Pack With Items", createdBy = ownerId)
            val itemId = fixture.insertItemWithGearPackId(planId = planId, gearPackId = packId)

            val deleteResponse = restTemplate.exchange(
                "/api/gear-packs/$packId", HttpMethod.DELETE,
                entityWithUser(null, ownerId), Void::class.java
            )
            assertThat(deleteResponse.statusCode).isEqualTo(HttpStatus.NO_CONTENT)

            // The item should still exist but gear_pack_id should be null
            val items = fixture.getItemsByPlanId(planId)
            val item = items.find { it["id"].toString() == itemId.toString() }
            assertThat(item).isNotNull()
            assertThat(item!!["gear_pack_id"]).isNull()
        }
    }

    @Nested
    inner class AddGearPackItem {

        @Test
        fun `POST adds item to gear pack and returns 201 with correct shape`() {
            val packId = fixture.insertGearPack(name = "My Camp Pack", createdBy = ownerId)
            val request = AddGearPackItemRequest(name = "Sleeping Bag", category = "camp", defaultQuantity = 1, scalable = true)

            val response = restTemplate.exchange(
                "/api/gear-packs/$packId/items",
                HttpMethod.POST,
                entityWithUser(request, ownerId),
                GearPackItemResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            val body = response.body!!
            assertThat(body.name).isEqualTo("Sleeping Bag")
            assertThat(body.category).isEqualTo("camp")
            assertThat(body.defaultQuantity).isEqualTo(1)
            assertThat(body.scalable).isTrue()
            assertThat(body.id).isNotNull()
            assertThat(body.sortOrder).isEqualTo(1)
        }

        @Test
        fun `POST returns 403 when non-creator tries to add item`() {
            val packId = fixture.insertGearPack(name = "Someone Else's Pack", createdBy = ownerId)
            val otherId = fixture.insertUser(email = "other@example.com")
            val request = AddGearPackItemRequest(name = "Tent", category = "camp")

            val response = restTemplate.exchange(
                "/api/gear-packs/$packId/items", HttpMethod.POST,
                entityWithUser(request, otherId), Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            assertThat(response.body!!["code"]).isEqualTo("FORBIDDEN")
        }

        @Test
        fun `POST returns 403 when adding item to system pack`() {
            val request = AddGearPackItemRequest(name = "New Item", category = "kitchen")

            val response = restTemplate.exchange(
                "/api/gear-packs/$COOKING_EQUIPMENT_PACK_ID/items", HttpMethod.POST,
                entityWithUser(request, ownerId), Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            assertThat(response.body!!["code"]).isEqualTo("FORBIDDEN")
        }

        @Test
        fun `POST returns 400 when name is blank`() {
            val packId = fixture.insertGearPack(name = "Validation Pack", createdBy = ownerId)
            val request = AddGearPackItemRequest(name = "   ", category = "camp")

            val response = restTemplate.exchange(
                "/api/gear-packs/$packId/items", HttpMethod.POST,
                entityWithUser(request, ownerId), Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(response.body!!["code"]).isEqualTo("BAD_REQUEST")
        }

        @Test
        fun `POST returns 400 when category is blank`() {
            val packId = fixture.insertGearPack(name = "Validation Pack 2", createdBy = ownerId)
            val request = AddGearPackItemRequest(name = "Valid Name", category = "   ")

            val response = restTemplate.exchange(
                "/api/gear-packs/$packId/items", HttpMethod.POST,
                entityWithUser(request, ownerId), Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(response.body!!["code"]).isEqualTo("BAD_REQUEST")
        }

        @Test
        fun `POST returns 400 when defaultQuantity is zero`() {
            val packId = fixture.insertGearPack(name = "Validation Pack 3", createdBy = ownerId)
            val request = AddGearPackItemRequest(name = "Zero Qty Item", category = "camp", defaultQuantity = 0)

            val response = restTemplate.exchange(
                "/api/gear-packs/$packId/items", HttpMethod.POST,
                entityWithUser(request, ownerId), Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(response.body!!["code"]).isEqualTo("BAD_REQUEST")
        }

        @Test
        fun `POST returns 409 on duplicate item name within pack`() {
            val packId = fixture.insertGearPack(name = "Pack With Dupe", createdBy = ownerId)
            val request = AddGearPackItemRequest(name = "Water Bottle", category = "camp")
            restTemplate.exchange(
                "/api/gear-packs/$packId/items", HttpMethod.POST,
                entityWithUser(request, ownerId), GearPackItemResponse::class.java
            )

            val dupeRequest = AddGearPackItemRequest(name = "water bottle", category = "kitchen") // case-insensitive
            val response = restTemplate.exchange(
                "/api/gear-packs/$packId/items", HttpMethod.POST,
                entityWithUser(dupeRequest, ownerId), Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
            assertThat(response.body!!["code"]).isEqualTo("CONFLICT")
        }
    }

    @Nested
    inner class UpdateGearPackItem {

        @Test
        fun `PUT updates gear pack item and returns 200`() {
            val packId = fixture.insertGearPack(name = "Update Test Pack", createdBy = ownerId)
            val itemId = fixture.insertGearPackItem(
                gearPackId = packId, name = "Old Name", category = "camp", scalable = false
            )
            val request = UpdateGearPackItemRequest(name = "New Name", category = "kitchen", scalable = true)

            val response = restTemplate.exchange(
                "/api/gear-packs/$packId/items/$itemId",
                HttpMethod.PUT,
                entityWithUser(request, ownerId),
                GearPackItemResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val body = response.body!!
            assertThat(body.name).isEqualTo("New Name")
            assertThat(body.category).isEqualTo("kitchen")
            assertThat(body.scalable).isTrue()
        }

        @Test
        fun `PUT returns 403 when updating item in system pack`() {
            val request = UpdateGearPackItemRequest(name = "Hacked Item")

            val response = restTemplate.exchange(
                "/api/gear-packs/$COOKING_EQUIPMENT_PACK_ID/items/$COOKING_EQUIPMENT_ITEM_ID", HttpMethod.PUT,
                entityWithUser(request, ownerId), Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            assertThat(response.body!!["code"]).isEqualTo("FORBIDDEN")
        }

        @Test
        fun `PUT returns 403 when non-creator tries to update item`() {
            val packId = fixture.insertGearPack(name = "Locked Pack", createdBy = ownerId)
            val itemId = fixture.insertGearPackItem(gearPackId = packId, name = "Item")
            val otherId = fixture.insertUser(email = "other@example.com")
            val request = UpdateGearPackItemRequest(name = "Hijacked Item")

            val response = restTemplate.exchange(
                "/api/gear-packs/$packId/items/$itemId", HttpMethod.PUT,
                entityWithUser(request, otherId), Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            assertThat(response.body!!["code"]).isEqualTo("FORBIDDEN")
        }

        @Test
        fun `PUT returns 404 when item does not exist`() {
            val packId = fixture.insertGearPack(name = "Pack For Item 404", createdBy = ownerId)
            val request = UpdateGearPackItemRequest(name = "Ghost Item")

            val response = restTemplate.exchange(
                "/api/gear-packs/$packId/items/${UUID.randomUUID()}", HttpMethod.PUT,
                entityWithUser(request, ownerId), Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
            assertThat(response.body!!["code"]).isEqualTo("NOT_FOUND")
        }
    }

    @Nested
    inner class RemoveGearPackItem {

        @Test
        fun `DELETE removes gear pack item and returns 204`() {
            val packId = fixture.insertGearPack(name = "Pack To Remove From", createdBy = ownerId)
            val itemId = fixture.insertGearPackItem(gearPackId = packId, name = "Removable Item")

            val response = restTemplate.exchange(
                "/api/gear-packs/$packId/items/$itemId", HttpMethod.DELETE,
                entityWithUser(null, ownerId), Void::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)

            val remainingItems = fixture.getGearPackItemsByPackId(packId)
            assertThat(remainingItems).isEmpty()
        }

        @Test
        fun `DELETE returns 403 when removing item from system pack`() {
            val response = restTemplate.exchange(
                "/api/gear-packs/$COOKING_EQUIPMENT_PACK_ID/items/$COOKING_EQUIPMENT_ITEM_ID", HttpMethod.DELETE,
                entityWithUser(null, ownerId), Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            assertThat(response.body!!["code"]).isEqualTo("FORBIDDEN")
        }

        @Test
        fun `DELETE returns 403 when non-creator tries to remove item`() {
            val packId = fixture.insertGearPack(name = "Pack For Remove Forbidden", createdBy = ownerId)
            val itemId = fixture.insertGearPackItem(gearPackId = packId, name = "Protected Item")
            val otherId = fixture.insertUser(email = "other@example.com")

            val response = restTemplate.exchange(
                "/api/gear-packs/$packId/items/$itemId", HttpMethod.DELETE,
                entityWithUser(null, otherId), Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            assertThat(response.body!!["code"]).isEqualTo("FORBIDDEN")
        }

        @Test
        fun `DELETE returns 404 when item does not exist`() {
            val packId = fixture.insertGearPack(name = "Pack For Item Remove 404", createdBy = ownerId)

            val response = restTemplate.exchange(
                "/api/gear-packs/$packId/items/${UUID.randomUUID()}", HttpMethod.DELETE,
                entityWithUser(null, ownerId), Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
            assertThat(response.body!!["code"]).isEqualTo("NOT_FOUND")
        }
    }

    @Nested
    inner class SearchGearPackItems {

        @Test
        fun `GET returns 200 with matching items for query`() {
            val response = restTemplate.exchange(
                "/api/gear-pack-items/search?q=cast",
                HttpMethod.GET,
                entityWithUser(null, ownerId),
                Array<GearPackItemSearchResultResponse>::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val results = response.body!!
            assertThat(results).isNotEmpty()
            assertThat(results.any { it.name == "Cast Iron Pan" }).isTrue()
            assertThat(results.all { it.gearPackName == "Cooking Equipment" }).isTrue()
        }

        @Test
        fun `GET returns 200 with empty list when no matches found`() {
            val response = restTemplate.exchange(
                "/api/gear-pack-items/search?q=xyz123notfounditem",
                HttpMethod.GET,
                entityWithUser(null, ownerId),
                Array<GearPackItemSearchResultResponse>::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!).isEmpty()
        }

        @Test
        fun `GET returns 400 for blank query`() {
            val response = restTemplate.exchange(
                "/api/gear-pack-items/search?q=   ",
                HttpMethod.GET,
                entityWithUser(null, ownerId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(response.body!!["code"]).isEqualTo("BAD_REQUEST")
        }
    }

    @Nested
    inner class ReadYourOwnWritesCrud {

        @Test
        fun `POST pack then add items then GET returns pack with items`() {
            // Create pack
            val createResponse = restTemplate.exchange(
                "/api/gear-packs", HttpMethod.POST,
                entityWithUser(CreateGearPackRequest(name = "My Trail Pack"), ownerId),
                GearPackDetailResponse::class.java
            )
            assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
            val packId = createResponse.body!!.id

            // Add two items
            val item1Response = restTemplate.exchange(
                "/api/gear-packs/$packId/items", HttpMethod.POST,
                entityWithUser(AddGearPackItemRequest(name = "Tent", category = "camp"), ownerId),
                GearPackItemResponse::class.java
            )
            assertThat(item1Response.statusCode).isEqualTo(HttpStatus.CREATED)

            val item2Response = restTemplate.exchange(
                "/api/gear-packs/$packId/items", HttpMethod.POST,
                entityWithUser(AddGearPackItemRequest(name = "Headlamp", category = "camp"), ownerId),
                GearPackItemResponse::class.java
            )
            assertThat(item2Response.statusCode).isEqualTo(HttpStatus.CREATED)

            // GET pack — verify both items are present
            val getResponse = restTemplate.exchange(
                "/api/gear-packs/$packId", HttpMethod.GET,
                entityWithUser(null, ownerId),
                GearPackDetailResponse::class.java
            )
            assertThat(getResponse.statusCode).isEqualTo(HttpStatus.OK)
            val pack = getResponse.body!!
            assertThat(pack.items).hasSize(2)
            assertThat(pack.items.map { it.name }).containsExactlyInAnyOrder("Tent", "Headlamp")
        }

        @Test
        fun `POST pack then DELETE pack then GET returns 404`() {
            // Create pack
            val createResponse = restTemplate.exchange(
                "/api/gear-packs", HttpMethod.POST,
                entityWithUser(CreateGearPackRequest(name = "Ephemeral Pack"), ownerId),
                GearPackDetailResponse::class.java
            )
            assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
            val packId = createResponse.body!!.id

            // Delete pack
            val deleteResponse = restTemplate.exchange(
                "/api/gear-packs/$packId", HttpMethod.DELETE,
                entityWithUser(null, ownerId), Void::class.java
            )
            assertThat(deleteResponse.statusCode).isEqualTo(HttpStatus.NO_CONTENT)

            // GET should return 404
            val getResponse = restTemplate.exchange(
                "/api/gear-packs/$packId", HttpMethod.GET,
                entityWithUser(null, ownerId), Map::class.java
            )
            assertThat(getResponse.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @Test
        fun `POST pack then add item then remove item then GET returns pack without item`() {
            // Create pack
            val createResponse = restTemplate.exchange(
                "/api/gear-packs", HttpMethod.POST,
                entityWithUser(CreateGearPackRequest(name = "Pack For Item Lifecycle"), ownerId),
                GearPackDetailResponse::class.java
            )
            val packId = createResponse.body!!.id

            // Add item
            val addResponse = restTemplate.exchange(
                "/api/gear-packs/$packId/items", HttpMethod.POST,
                entityWithUser(AddGearPackItemRequest(name = "Bear Canister", category = "camp"), ownerId),
                GearPackItemResponse::class.java
            )
            assertThat(addResponse.statusCode).isEqualTo(HttpStatus.CREATED)
            val itemId = addResponse.body!!.id

            // Remove item
            val removeResponse = restTemplate.exchange(
                "/api/gear-packs/$packId/items/$itemId", HttpMethod.DELETE,
                entityWithUser(null, ownerId), Void::class.java
            )
            assertThat(removeResponse.statusCode).isEqualTo(HttpStatus.NO_CONTENT)

            // GET pack — verify item is gone
            val getResponse = restTemplate.exchange(
                "/api/gear-packs/$packId", HttpMethod.GET,
                entityWithUser(null, ownerId), GearPackDetailResponse::class.java
            )
            assertThat(getResponse.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(getResponse.body!!.items).isEmpty()
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
