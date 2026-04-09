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
import com.acme.services.camperservice.features.gearpack.dto.GearPackItemResponse
import com.acme.services.camperservice.features.gearpack.dto.GearPackItemSearchResultResponse
import com.acme.services.camperservice.features.gearpack.model.GearPack
import com.acme.services.camperservice.features.gearpack.params.AddGearPackItemParam
import com.acme.services.camperservice.features.gearpack.params.ApplyGearPackParam
import com.acme.services.camperservice.features.gearpack.params.CreateGearPackParam
import com.acme.services.camperservice.features.gearpack.params.DeleteGearPackParam
import com.acme.services.camperservice.features.gearpack.params.GetGearPackParam
import com.acme.services.camperservice.features.gearpack.params.ListGearPacksParam
import com.acme.services.camperservice.features.gearpack.params.RemoveGearPackItemParam
import com.acme.services.camperservice.features.gearpack.params.SearchGearPackItemsParam
import com.acme.services.camperservice.features.gearpack.params.UpdateGearPackItemParam
import com.acme.services.camperservice.features.gearpack.params.UpdateGearPackParam
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
        createdBy: UUID? = null,
    ) = ClientGearPack(
        id = id,
        name = name,
        description = description,
        items = items,
        createdBy = createdBy,
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
        fun `apply sets gearPackId on every created item`() {
            val result = gearPackService.apply(
                ApplyGearPackParam(gearPackId = packId, planId = planId, groupSize = 2, requestingUserId = ownerId)
            )

            assertThat(result.isSuccess).isTrue()
            val items = (result as Result.Success).value.items
            assertThat(items).hasSize(2)
            for (item in items) {
                assertThat(item.gearPackId).isEqualTo(packId)
            }
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

    @Nested
    inner class CreateGearPack {

        @Test
        fun `successfully creates a gear pack and returns it`() {
            val result = gearPackService.create(
                CreateGearPackParam(name = "My Pack", description = "A custom pack", requestingUserId = ownerId)
            )

            assertThat(result.isSuccess).isTrue()
            val pack = (result as Result.Success).value
            assertThat(pack.name).isEqualTo("My Pack")
            assertThat(pack.description).isEqualTo("A custom pack")
            assertThat(pack.items).isEmpty()
        }

        @Test
        fun `returns Invalid error when name is blank`() {
            val result = gearPackService.create(
                CreateGearPackParam(name = "   ", description = "desc", requestingUserId = ownerId)
            )

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(GearPackError.Invalid::class.java)
            assertThat((error as GearPackError.Invalid).field).isEqualTo("name")
        }

        @Test
        fun `returns DuplicateName error when name already exists`() {
            fakeGearPackClient.seedGearPack(clientGearPack(name = "Existing Pack", createdBy = ownerId))

            val result = gearPackService.create(
                CreateGearPackParam(name = "Existing Pack", description = "desc", requestingUserId = ownerId)
            )

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(GearPackError.DuplicateName::class.java)
        }
    }

    @Nested
    inner class UpdateGearPack {

        @Test
        fun `successfully updates a gear pack when caller is the creator`() {
            fakeGearPackClient.seedGearPack(clientGearPack(createdBy = ownerId))

            val result = gearPackService.update(
                UpdateGearPackParam(id = packId, name = "Updated Name", description = null, requestingUserId = ownerId)
            )

            assertThat(result.isSuccess).isTrue()
            val pack = (result as Result.Success).value
            assertThat(pack.name).isEqualTo("Updated Name")
        }

        @Test
        fun `returns NotCreator error when caller is not the creator`() {
            fakeGearPackClient.seedGearPack(clientGearPack(createdBy = ownerId))

            val result = gearPackService.update(
                UpdateGearPackParam(id = packId, name = "New Name", description = null, requestingUserId = memberId)
            )

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(GearPackError.NotCreator::class.java)
            assertThat((error as GearPackError.NotCreator).userId).isEqualTo(memberId)
        }

        @Test
        fun `returns SystemPack error when updating a pack with null createdBy`() {
            fakeGearPackClient.seedGearPack(clientGearPack(createdBy = null))

            val result = gearPackService.update(
                UpdateGearPackParam(id = packId, name = "New Name", description = null, requestingUserId = ownerId)
            )

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(GearPackError.SystemPack::class.java)
        }

        @Test
        fun `returns NotFound error when pack does not exist`() {
            val result = gearPackService.update(
                UpdateGearPackParam(id = UUID.randomUUID(), name = "New Name", description = null, requestingUserId = ownerId)
            )

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(GearPackError.NotFound::class.java)
        }
    }

    @Nested
    inner class DeleteGearPack {

        @Test
        fun `successfully deletes a gear pack when caller is the creator`() {
            fakeGearPackClient.seedGearPack(clientGearPack(createdBy = ownerId))

            val result = gearPackService.delete(
                DeleteGearPackParam(id = packId, requestingUserId = ownerId)
            )

            assertThat(result.isSuccess).isTrue()
        }

        @Test
        fun `returns NotCreator error when caller is not the creator`() {
            fakeGearPackClient.seedGearPack(clientGearPack(createdBy = ownerId))

            val result = gearPackService.delete(
                DeleteGearPackParam(id = packId, requestingUserId = memberId)
            )

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(GearPackError.NotCreator::class.java)
            assertThat((error as GearPackError.NotCreator).userId).isEqualTo(memberId)
        }

        @Test
        fun `returns SystemPack error when deleting a system pack`() {
            fakeGearPackClient.seedGearPack(clientGearPack(createdBy = null))

            val result = gearPackService.delete(
                DeleteGearPackParam(id = packId, requestingUserId = ownerId)
            )

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(GearPackError.SystemPack::class.java)
        }

        @Test
        fun `returns NotFound error when pack does not exist`() {
            val result = gearPackService.delete(
                DeleteGearPackParam(id = UUID.randomUUID(), requestingUserId = ownerId)
            )

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(GearPackError.NotFound::class.java)
        }
    }

    @Nested
    inner class AddGearPackItem {

        @Test
        fun `successfully adds an item to a pack when caller is the creator`() {
            fakeGearPackClient.seedGearPack(clientGearPack(createdBy = ownerId))

            val result = gearPackService.addItem(
                AddGearPackItemParam(
                    gearPackId = packId,
                    name = "Tent",
                    category = "camp",
                    defaultQuantity = 1,
                    scalable = false,
                    requestingUserId = ownerId,
                )
            )

            assertThat(result.isSuccess).isTrue()
            val item = (result as Result.Success).value
            assertThat(item.name).isEqualTo("Tent")
            assertThat(item.category).isEqualTo("camp")
            assertThat(item.defaultQuantity).isEqualTo(1)
            assertThat(item.scalable).isFalse()
        }

        @Test
        fun `returns NotCreator error when caller is not the creator`() {
            fakeGearPackClient.seedGearPack(clientGearPack(createdBy = ownerId))

            val result = gearPackService.addItem(
                AddGearPackItemParam(
                    gearPackId = packId,
                    name = "Tent",
                    category = "camp",
                    defaultQuantity = 1,
                    scalable = false,
                    requestingUserId = memberId,
                )
            )

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(GearPackError.NotCreator::class.java)
        }

        @Test
        fun `returns SystemPack error when adding to a system pack`() {
            fakeGearPackClient.seedGearPack(clientGearPack(createdBy = null))

            val result = gearPackService.addItem(
                AddGearPackItemParam(
                    gearPackId = packId,
                    name = "Tent",
                    category = "camp",
                    defaultQuantity = 1,
                    scalable = false,
                    requestingUserId = ownerId,
                )
            )

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(GearPackError.SystemPack::class.java)
        }

        @Test
        fun `returns DuplicateItemName error when item name already exists in pack`() {
            val existingItem = clientGearPackItem(name = "Tent")
            fakeGearPackClient.seedGearPack(clientGearPack(items = listOf(existingItem), createdBy = ownerId))

            val result = gearPackService.addItem(
                AddGearPackItemParam(
                    gearPackId = packId,
                    name = "Tent",
                    category = "camp",
                    defaultQuantity = 1,
                    scalable = false,
                    requestingUserId = ownerId,
                )
            )

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(GearPackError.DuplicateItemName::class.java)
        }

        @Test
        fun `returns NotFound when pack does not exist`() {
            val result = gearPackService.addItem(
                AddGearPackItemParam(
                    gearPackId = UUID.randomUUID(),
                    name = "Tent",
                    category = "camp",
                    defaultQuantity = 1,
                    scalable = false,
                    requestingUserId = ownerId,
                )
            )

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(GearPackError.NotFound::class.java)
        }
    }

    @Nested
    inner class UpdateGearPackItem {

        private val itemId = UUID.fromString("dd000000-0001-4000-8000-000000000001")

        private fun seededItemInCreatorPack(): ClientGearPackItem {
            val item = clientGearPackItem(name = "Sleeping Bag").copy(id = itemId)
            fakeGearPackClient.seedGearPack(clientGearPack(items = listOf(item), createdBy = ownerId))
            return item
        }

        @Test
        fun `successfully updates an item when caller is the creator`() {
            seededItemInCreatorPack()

            val result = gearPackService.updateItem(
                UpdateGearPackItemParam(
                    id = itemId,
                    gearPackId = packId,
                    name = "Sleeping Bag Pro",
                    category = null,
                    defaultQuantity = null,
                    scalable = null,
                    requestingUserId = ownerId,
                )
            )

            assertThat(result.isSuccess).isTrue()
            val item = (result as Result.Success).value
            assertThat(item.name).isEqualTo("Sleeping Bag Pro")
        }

        @Test
        fun `returns NotCreator error when caller is not the creator`() {
            seededItemInCreatorPack()

            val result = gearPackService.updateItem(
                UpdateGearPackItemParam(
                    id = itemId,
                    gearPackId = packId,
                    name = "New Name",
                    category = null,
                    defaultQuantity = null,
                    scalable = null,
                    requestingUserId = memberId,
                )
            )

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(GearPackError.NotCreator::class.java)
        }

        @Test
        fun `returns ItemNotFound when item does not exist in the pack`() {
            fakeGearPackClient.seedGearPack(clientGearPack(items = emptyList(), createdBy = ownerId))

            val result = gearPackService.updateItem(
                UpdateGearPackItemParam(
                    id = UUID.randomUUID(),
                    gearPackId = packId,
                    name = "New Name",
                    category = null,
                    defaultQuantity = null,
                    scalable = null,
                    requestingUserId = ownerId,
                )
            )

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(GearPackError.ItemNotFound::class.java)
        }

        @Test
        fun `returns SystemPack when pack is system-created`() {
            val item = clientGearPackItem(name = "Sleeping Bag").copy(id = itemId)
            fakeGearPackClient.seedGearPack(clientGearPack(items = listOf(item), createdBy = null))

            val result = gearPackService.updateItem(
                UpdateGearPackItemParam(
                    id = itemId,
                    gearPackId = packId,
                    name = "New Name",
                    category = null,
                    defaultQuantity = null,
                    scalable = null,
                    requestingUserId = ownerId,
                )
            )

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(GearPackError.SystemPack::class.java)
        }

        @Test
        fun `returns NotFound when pack does not exist`() {
            val result = gearPackService.updateItem(
                UpdateGearPackItemParam(
                    id = itemId,
                    gearPackId = UUID.randomUUID(),
                    name = "New Name",
                    category = null,
                    defaultQuantity = null,
                    scalable = null,
                    requestingUserId = ownerId,
                )
            )

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(GearPackError.NotFound::class.java)
        }
    }

    @Nested
    inner class RemoveGearPackItem {

        private val itemId = UUID.fromString("ee000000-0001-4000-8000-000000000001")

        private fun seededItemInCreatorPack(): ClientGearPackItem {
            val item = clientGearPackItem(name = "Trekking Poles").copy(id = itemId)
            fakeGearPackClient.seedGearPack(clientGearPack(items = listOf(item), createdBy = ownerId))
            return item
        }

        @Test
        fun `successfully removes an item when caller is the creator`() {
            seededItemInCreatorPack()

            val result = gearPackService.removeItem(
                RemoveGearPackItemParam(id = itemId, gearPackId = packId, requestingUserId = ownerId)
            )

            assertThat(result.isSuccess).isTrue()
        }

        @Test
        fun `returns NotCreator error when caller is not the creator`() {
            seededItemInCreatorPack()

            val result = gearPackService.removeItem(
                RemoveGearPackItemParam(id = itemId, gearPackId = packId, requestingUserId = memberId)
            )

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(GearPackError.NotCreator::class.java)
        }

        @Test
        fun `returns ItemNotFound when item does not exist in the pack`() {
            fakeGearPackClient.seedGearPack(clientGearPack(items = emptyList(), createdBy = ownerId))

            val result = gearPackService.removeItem(
                RemoveGearPackItemParam(id = UUID.randomUUID(), gearPackId = packId, requestingUserId = ownerId)
            )

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(GearPackError.ItemNotFound::class.java)
        }

        @Test
        fun `returns SystemPack when pack is system-created`() {
            val item = clientGearPackItem(name = "Trekking Poles").copy(id = itemId)
            fakeGearPackClient.seedGearPack(clientGearPack(items = listOf(item), createdBy = null))

            val result = gearPackService.removeItem(
                RemoveGearPackItemParam(id = itemId, gearPackId = packId, requestingUserId = ownerId)
            )

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(GearPackError.SystemPack::class.java)
        }

        @Test
        fun `returns NotFound when pack does not exist`() {
            val result = gearPackService.removeItem(
                RemoveGearPackItemParam(id = itemId, gearPackId = UUID.randomUUID(), requestingUserId = ownerId)
            )

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(GearPackError.NotFound::class.java)
        }
    }

    @Nested
    inner class SearchGearPackItems {

        @BeforeEach
        fun seedPacks() {
            fakeGearPackClient.seedGearPack(
                clientGearPack(
                    id = packId,
                    name = "Cooking Equipment",
                    items = listOf(
                        clientGearPackItem(name = "Cast Iron Pan"),
                        clientGearPackItem(name = "Portable Stove"),
                    ),
                ),
                clientGearPack(
                    id = UUID.randomUUID(),
                    name = "Sleeping Gear",
                    items = listOf(
                        clientGearPackItem(name = "Sleeping Bag"),
                    ),
                ),
            )
        }

        @Test
        fun `returns matching items across all packs`() {
            val result = gearPackService.searchItems(
                SearchGearPackItemsParam(query = "pan", requestingUserId = ownerId)
            )

            assertThat(result.isSuccess).isTrue()
            val items = (result as Result.Success).value
            assertThat(items).hasSize(1)
            assertThat(items.first().name).isEqualTo("Cast Iron Pan")
            assertThat(items.first().gearPackName).isEqualTo("Cooking Equipment")
        }

        @Test
        fun `returns empty list when no items match the query`() {
            val result = gearPackService.searchItems(
                SearchGearPackItemsParam(query = "hammock", requestingUserId = ownerId)
            )

            assertThat(result.isSuccess).isTrue()
            assertThat((result as Result.Success).value).isEmpty()
        }

        @Test
        fun `returns all matching items when query matches multiple packs`() {
            val result = gearPackService.searchItems(
                SearchGearPackItemsParam(query = "s", requestingUserId = ownerId)
            )

            assertThat(result.isSuccess).isTrue()
            val items = (result as Result.Success).value
            assertThat(items.map { it.name }).containsExactlyInAnyOrder("Cast Iron Pan", "Portable Stove", "Sleeping Bag")
        }

        @Test
        fun `returns Invalid error when query is blank`() {
            val result = gearPackService.searchItems(
                SearchGearPackItemsParam(query = "   ", requestingUserId = ownerId)
            )

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(GearPackError.Invalid::class.java)
            assertThat((error as GearPackError.Invalid).field).isEqualTo("q")
        }
    }
}
