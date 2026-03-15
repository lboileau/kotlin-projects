package com.acme.services.camperservice.features.gearpack.service

import com.acme.clients.common.Result
import com.acme.clients.gearpackclient.fake.FakeGearPackClient
import com.acme.clients.gearpackclient.model.GearPack as ClientGearPack
import com.acme.clients.gearpackclient.model.GearPackItem as ClientGearPackItem
import com.acme.clients.itemclient.fake.FakeItemClient
import com.acme.clients.planclient.fake.FakePlanClient
import com.acme.clients.planclient.model.Plan
import com.acme.clients.planclient.model.PlanMember
import com.acme.services.camperservice.common.auth.PlanRoleAuthorizer
import com.acme.services.camperservice.features.gearpack.error.GearPackError
import com.acme.services.camperservice.features.gearpack.params.ApplyGearPackParam
import com.acme.services.camperservice.features.gearpack.params.GetGearPackParam
import com.acme.services.camperservice.features.gearpack.params.ListGearPacksParam
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class GearPackServiceTest {

    private val fakeGearPackClient = FakeGearPackClient()
    private val fakeItemClient = FakeItemClient()
    private val fakePlanClient = FakePlanClient()
    private val planRoleAuthorizer = PlanRoleAuthorizer(fakePlanClient)
    private val gearPackService = GearPackService(fakeGearPackClient, fakeItemClient, planRoleAuthorizer)

    private val planId = UUID.randomUUID()
    private val ownerId = UUID.randomUUID()
    private val managerId = UUID.randomUUID()
    private val memberId = UUID.randomUUID()
    private val now = Instant.now()

    private val packId = UUID.fromString("cc000000-0001-4000-8000-000000000001")

    private fun clientGearPackItem(
        name: String,
        category: String = "kitchen",
        defaultQuantity: Int = 1,
        scalable: Boolean = false,
        sortOrder: Int = 1,
    ) = ClientGearPackItem(
        id = UUID.randomUUID(),
        gearPackId = packId,
        name = name,
        category = category,
        defaultQuantity = defaultQuantity,
        scalable = scalable,
        sortOrder = sortOrder,
        createdAt = now,
        updatedAt = now,
    )

    private fun clientGearPack(
        id: UUID = packId,
        name: String = "Cooking Equipment",
        description: String = "Essential cooking gear",
        items: List<ClientGearPackItem> = emptyList(),
    ) = ClientGearPack(
        id = id,
        name = name,
        description = description,
        items = items,
        createdAt = now,
        updatedAt = now,
    )

    @BeforeEach
    fun setUp() {
        fakeGearPackClient.reset()
        fakeItemClient.reset()
        fakePlanClient.reset()
        fakePlanClient.seedPlan(
            Plan(id = planId, name = "Test Plan", visibility = "private", ownerId = ownerId, createdAt = now, updatedAt = now)
        )
        fakePlanClient.seedMember(PlanMember(planId = planId, userId = ownerId, role = "member", createdAt = now))
        fakePlanClient.seedMember(PlanMember(planId = planId, userId = managerId, role = "manager", createdAt = now))
        fakePlanClient.seedMember(PlanMember(planId = planId, userId = memberId, role = "member", createdAt = now))
    }

    @Nested
    inner class ListGearPacks {

        @Test
        fun `returns empty list when no packs exist`() {
            val result = gearPackService.list(ListGearPacksParam(requestingUserId = ownerId))

            assertThat(result.isSuccess).isTrue()
            val packs = (result as Result.Success).value
            assertThat(packs).isEmpty()
        }

        @Test
        fun `returns all packs mapped from client`() {
            fakeGearPackClient.seedGearPack(
                clientGearPack(name = "Cooking Equipment"),
                clientGearPack(id = UUID.randomUUID(), name = "Sleeping Gear"),
            )

            val result = gearPackService.list(ListGearPacksParam(requestingUserId = ownerId))

            assertThat(result.isSuccess).isTrue()
            val packs = (result as Result.Success).value
            assertThat(packs).hasSize(2)
            assertThat(packs.map { it.name }).containsExactly("Cooking Equipment", "Sleeping Gear")
        }

        @Test
        fun `maps client GearPack to service GearPack correctly`() {
            val pack = clientGearPack(
                name = "Cooking Equipment",
                description = "Essential cooking gear",
            )
            fakeGearPackClient.seedGearPack(pack)

            val result = gearPackService.list(ListGearPacksParam(requestingUserId = ownerId))

            assertThat(result.isSuccess).isTrue()
            val servicePack = (result as Result.Success).value.first()
            assertThat(servicePack.id).isEqualTo(pack.id)
            assertThat(servicePack.name).isEqualTo("Cooking Equipment")
            assertThat(servicePack.description).isEqualTo("Essential cooking gear")
            assertThat(servicePack.createdAt).isEqualTo(pack.createdAt)
            assertThat(servicePack.updatedAt).isEqualTo(pack.updatedAt)
            // list returns packs without items
            assertThat(servicePack.items).isEmpty()
        }
    }

    @Nested
    inner class GetGearPack {

        @Test
        fun `returns pack with items mapped from client`() {
            val items = listOf(
                clientGearPackItem(name = "Cast Iron Pan", sortOrder = 1),
                clientGearPackItem(name = "Plates", scalable = true, sortOrder = 2),
            )
            fakeGearPackClient.seedGearPack(clientGearPack(items = items))

            val result = gearPackService.getById(GetGearPackParam(id = packId, requestingUserId = ownerId))

            assertThat(result.isSuccess).isTrue()
            val pack = (result as Result.Success).value
            assertThat(pack.name).isEqualTo("Cooking Equipment")
            assertThat(pack.items).hasSize(2)
            assertThat(pack.items.map { it.name }).containsExactly("Cast Iron Pan", "Plates")
        }

        @Test
        fun `returns NotFound error when pack does not exist`() {
            val result = gearPackService.getById(GetGearPackParam(id = UUID.randomUUID(), requestingUserId = ownerId))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(GearPackError.NotFound::class.java)
        }

        @Test
        fun `maps all item fields correctly`() {
            val item = clientGearPackItem(
                name = "Plates",
                category = "kitchen",
                defaultQuantity = 2,
                scalable = true,
                sortOrder = 5,
            )
            fakeGearPackClient.seedGearPack(clientGearPack(items = listOf(item)))

            val result = gearPackService.getById(GetGearPackParam(id = packId, requestingUserId = ownerId))

            assertThat(result.isSuccess).isTrue()
            val mappedItem = (result as Result.Success).value.items.first()
            assertThat(mappedItem.id).isEqualTo(item.id)
            assertThat(mappedItem.name).isEqualTo("Plates")
            assertThat(mappedItem.category).isEqualTo("kitchen")
            assertThat(mappedItem.defaultQuantity).isEqualTo(2)
            assertThat(mappedItem.scalable).isTrue()
            assertThat(mappedItem.sortOrder).isEqualTo(5)
        }
    }

    @Nested
    inner class ApplyGearPack {

        private val nonScalableItem = clientGearPackItem(name = "Cast Iron Pan", defaultQuantity = 1, scalable = false, sortOrder = 1)
        private val scalableItem = clientGearPackItem(name = "Plates", defaultQuantity = 1, scalable = true, sortOrder = 2)

        @BeforeEach
        fun seedPack() {
            fakeGearPackClient.seedGearPack(clientGearPack(items = listOf(nonScalableItem, scalableItem)))
        }

        @Test
        fun `successfully applies a pack and creates items via ItemClient`() {
            val result = gearPackService.apply(
                ApplyGearPackParam(gearPackId = packId, planId = planId, groupSize = 4, requestingUserId = ownerId)
            )

            assertThat(result.isSuccess).isTrue()
            val applyResult = (result as Result.Success).value
            assertThat(applyResult.appliedCount).isEqualTo(2)
            assertThat(applyResult.items).hasSize(2)
        }

        @Test
        fun `scales quantities correctly for scalable items`() {
            val result = gearPackService.apply(
                ApplyGearPackParam(gearPackId = packId, planId = planId, groupSize = 4, requestingUserId = ownerId)
            )

            assertThat(result.isSuccess).isTrue()
            val items = (result as Result.Success).value.items
            val plates = items.find { it.name == "Plates" }!!
            assertThat(plates.quantity).isEqualTo(4) // 1 * 4
        }

        @Test
        fun `does not scale non-scalable items`() {
            val result = gearPackService.apply(
                ApplyGearPackParam(gearPackId = packId, planId = planId, groupSize = 4, requestingUserId = ownerId)
            )

            assertThat(result.isSuccess).isTrue()
            val items = (result as Result.Success).value.items
            val pan = items.find { it.name == "Cast Iron Pan" }!!
            assertThat(pan.quantity).isEqualTo(1) // stays at defaultQuantity
        }

        @Test
        fun `returns NotFound error when pack does not exist`() {
            val result = gearPackService.apply(
                ApplyGearPackParam(gearPackId = UUID.randomUUID(), planId = planId, groupSize = 4, requestingUserId = ownerId)
            )

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(GearPackError.NotFound::class.java)
        }

        @Test
        fun `returns Forbidden when user is regular member`() {
            val result = gearPackService.apply(
                ApplyGearPackParam(gearPackId = packId, planId = planId, groupSize = 4, requestingUserId = memberId)
            )

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(GearPackError.Forbidden::class.java)
        }

        @Test
        fun `succeeds for plan manager and applies items correctly`() {
            val result = gearPackService.apply(
                ApplyGearPackParam(gearPackId = packId, planId = planId, groupSize = 3, requestingUserId = managerId)
            )

            assertThat(result.isSuccess).isTrue()
            val applyResult = (result as Result.Success).value
            assertThat(applyResult.appliedCount).isEqualTo(2)
            assertThat(applyResult.items).hasSize(2)

            val pan = applyResult.items.find { it.name == "Cast Iron Pan" }!!
            assertThat(pan.quantity).isEqualTo(1) // non-scalable stays at default

            val plates = applyResult.items.find { it.name == "Plates" }!!
            assertThat(plates.quantity).isEqualTo(3) // scalable: 1 * 3
        }

        @Test
        fun `creates items with correct fields`() {
            val result = gearPackService.apply(
                ApplyGearPackParam(gearPackId = packId, planId = planId, groupSize = 3, requestingUserId = ownerId)
            )

            assertThat(result.isSuccess).isTrue()
            val items = (result as Result.Success).value.items

            for (item in items) {
                assertThat(item.planId).isEqualTo(planId)
                assertThat(item.packed).isFalse()
            }

            val pan = items.find { it.name == "Cast Iron Pan" }!!
            assertThat(pan.category).isEqualTo("kitchen")
            assertThat(pan.quantity).isEqualTo(1)

            val plates = items.find { it.name == "Plates" }!!
            assertThat(plates.category).isEqualTo("kitchen")
            assertThat(plates.quantity).isEqualTo(3) // 1 * 3
        }

        @Test
        fun `scales with higher default quantities`() {
            fakeGearPackClient.reset()
            val highQuantityItem = clientGearPackItem(name = "Nails", defaultQuantity = 5, scalable = true, sortOrder = 1)
            fakeGearPackClient.seedGearPack(clientGearPack(items = listOf(highQuantityItem)))

            val result = gearPackService.apply(
                ApplyGearPackParam(gearPackId = packId, planId = planId, groupSize = 3, requestingUserId = ownerId)
            )

            assertThat(result.isSuccess).isTrue()
            val nails = (result as Result.Success).value.items.first()
            assertThat(nails.quantity).isEqualTo(15) // 5 * 3
        }

        @Test
        fun `applies empty pack successfully with zero items`() {
            fakeGearPackClient.reset()
            fakeGearPackClient.seedGearPack(clientGearPack(items = emptyList()))

            val result = gearPackService.apply(
                ApplyGearPackParam(gearPackId = packId, planId = planId, groupSize = 4, requestingUserId = ownerId)
            )

            assertThat(result.isSuccess).isTrue()
            val applyResult = (result as Result.Success).value
            assertThat(applyResult.appliedCount).isEqualTo(0)
            assertThat(applyResult.items).isEmpty()
        }

        @Test
        fun `groupSize of 1 keeps scalable items at default quantity`() {
            val result = gearPackService.apply(
                ApplyGearPackParam(gearPackId = packId, planId = planId, groupSize = 1, requestingUserId = ownerId)
            )

            assertThat(result.isSuccess).isTrue()
            val items = (result as Result.Success).value.items
            val plates = items.find { it.name == "Plates" }!!
            assertThat(plates.quantity).isEqualTo(1) // 1 * 1
        }

        @Test
        fun `returns ApplyFailed when item creation fails`() {
            fakeGearPackClient.reset()
            val invalidItem = clientGearPackItem(name = "", sortOrder = 1) // blank name triggers validation failure in FakeItemClient
            fakeGearPackClient.seedGearPack(clientGearPack(name = "Bad Pack", items = listOf(invalidItem)))

            val result = gearPackService.apply(
                ApplyGearPackParam(gearPackId = packId, planId = planId, groupSize = 2, requestingUserId = ownerId)
            )

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(GearPackError.ApplyFailed::class.java)
            assertThat((error as GearPackError.ApplyFailed).packName).isEqualTo("Bad Pack")
        }

        @Test
        fun `returns Forbidden when user is not a plan member`() {
            val nonMemberId = UUID.randomUUID()

            val result = gearPackService.apply(
                ApplyGearPackParam(gearPackId = packId, planId = planId, groupSize = 4, requestingUserId = nonMemberId)
            )

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(GearPackError.Forbidden::class.java)
        }
    }

    @Nested
    inner class ValidateApplyGearPack {

        @Test
        fun `returns success when groupSize is positive`() {
            fakeGearPackClient.seedGearPack(clientGearPack(items = emptyList()))

            val result = gearPackService.apply(
                ApplyGearPackParam(gearPackId = packId, planId = planId, groupSize = 1, requestingUserId = ownerId)
            )

            assertThat(result.isSuccess).isTrue()
        }

        @Test
        fun `returns Invalid error when groupSize is zero`() {
            val result = gearPackService.apply(
                ApplyGearPackParam(gearPackId = packId, planId = planId, groupSize = 0, requestingUserId = ownerId)
            )

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(GearPackError.Invalid::class.java)
            assertThat((error as GearPackError.Invalid).field).isEqualTo("groupSize")
            assertThat(error.reason).isEqualTo("must be greater than 0")
        }

        @Test
        fun `returns Invalid error when groupSize is negative`() {
            val result = gearPackService.apply(
                ApplyGearPackParam(gearPackId = packId, planId = planId, groupSize = -5, requestingUserId = ownerId)
            )

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(GearPackError.Invalid::class.java)
            assertThat((error as GearPackError.Invalid).field).isEqualTo("groupSize")
        }
    }
}
