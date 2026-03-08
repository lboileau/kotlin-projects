package com.acme.services.camperservice.features.item.service

import com.acme.clients.common.Result
import com.acme.clients.itemclient.fake.FakeItemClient
import com.acme.clients.itemclient.model.Item as ClientItem
import com.acme.services.camperservice.features.item.error.ItemError
import com.acme.services.camperservice.features.item.params.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class ItemServiceTest {

    private val fakeItemClient = FakeItemClient()
    private val itemService = ItemService(fakeItemClient)

    private val planId = UUID.randomUUID()
    private val userId = UUID.randomUUID()
    private val requestingUserId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        fakeItemClient.reset()
    }

    @Nested
    inner class Create {
        @Test
        fun `create returns success for plan owner`() {
            val result = itemService.create(
                CreateItemParam(
                    name = "Tent",
                    category = "Shelter",
                    quantity = 1,
                    packed = false,
                    ownerType = "plan",
                    ownerId = planId,
                    requestingUserId = requestingUserId,
                )
            )

            assertThat(result.isSuccess).isTrue()
            val item = (result as Result.Success).value
            assertThat(item.name).isEqualTo("Tent")
            assertThat(item.category).isEqualTo("Shelter")
            assertThat(item.quantity).isEqualTo(1)
            assertThat(item.packed).isFalse()
            assertThat(item.planId).isEqualTo(planId)
            assertThat(item.userId).isNull()
        }

        @Test
        fun `create returns success for user owner`() {
            val result = itemService.create(
                CreateItemParam(
                    name = "Flashlight",
                    category = "Lighting",
                    quantity = 2,
                    packed = true,
                    ownerType = "user",
                    ownerId = userId,
                    requestingUserId = requestingUserId,
                )
            )

            assertThat(result.isSuccess).isTrue()
            val item = (result as Result.Success).value
            assertThat(item.name).isEqualTo("Flashlight")
            assertThat(item.quantity).isEqualTo(2)
            assertThat(item.packed).isTrue()
            assertThat(item.planId).isNull()
            assertThat(item.userId).isEqualTo(userId)
        }

        @Test
        fun `create returns Invalid when ownerType is invalid`() {
            val result = itemService.create(
                CreateItemParam(
                    name = "Tent",
                    category = "Shelter",
                    quantity = 1,
                    packed = false,
                    ownerType = "invalid",
                    ownerId = planId,
                    requestingUserId = requestingUserId,
                )
            )

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ItemError.Invalid::class.java)
            assertThat((error as ItemError.Invalid).field).isEqualTo("ownerType")
        }

        @Test
        fun `create returns Invalid when name is blank`() {
            val result = itemService.create(
                CreateItemParam(
                    name = "",
                    category = "Shelter",
                    quantity = 1,
                    packed = false,
                    ownerType = "plan",
                    ownerId = planId,
                    requestingUserId = requestingUserId,
                )
            )

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ItemError.Invalid::class.java)
            assertThat((error as ItemError.Invalid).field).isEqualTo("name")
        }

        @Test
        fun `create returns Invalid when quantity is zero`() {
            val result = itemService.create(
                CreateItemParam(
                    name = "Tent",
                    category = "Shelter",
                    quantity = 0,
                    packed = false,
                    ownerType = "plan",
                    ownerId = planId,
                    requestingUserId = requestingUserId,
                )
            )

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ItemError.Invalid::class.java)
            assertThat((error as ItemError.Invalid).field).isEqualTo("quantity")
        }
    }

    @Nested
    inner class GetById {
        @Test
        fun `getById returns success when item exists`() {
            val created = (itemService.create(
                CreateItemParam(
                    name = "Tent",
                    category = "Shelter",
                    quantity = 1,
                    packed = false,
                    ownerType = "plan",
                    ownerId = planId,
                    requestingUserId = requestingUserId,
                )
            ) as Result.Success).value

            val result = itemService.getById(GetItemParam(id = created.id, requestingUserId = requestingUserId))

            assertThat(result.isSuccess).isTrue()
            val item = (result as Result.Success).value
            assertThat(item.id).isEqualTo(created.id)
            assertThat(item.name).isEqualTo("Tent")
        }

        @Test
        fun `getById returns NotFound when item does not exist`() {
            val result = itemService.getById(GetItemParam(id = UUID.randomUUID(), requestingUserId = requestingUserId))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(ItemError.NotFound::class.java)
        }
    }

    @Nested
    inner class GetByOwner {
        @Test
        fun `getByOwner returns items for plan`() {
            itemService.create(
                CreateItemParam(
                    name = "Tent", category = "Shelter", quantity = 1, packed = false,
                    ownerType = "plan", ownerId = planId, requestingUserId = requestingUserId,
                )
            )
            itemService.create(
                CreateItemParam(
                    name = "Sleeping Bag", category = "Sleeping", quantity = 2, packed = false,
                    ownerType = "plan", ownerId = planId, requestingUserId = requestingUserId,
                )
            )

            val result = itemService.getByOwner(
                GetItemsByOwnerParam(ownerType = "plan", ownerId = planId, requestingUserId = requestingUserId)
            )

            assertThat(result.isSuccess).isTrue()
            val items = (result as Result.Success).value
            assertThat(items).hasSize(2)
            assertThat(items.map { it.name }).containsExactlyInAnyOrder("Tent", "Sleeping Bag")
        }

        @Test
        fun `getByOwner returns items for user`() {
            itemService.create(
                CreateItemParam(
                    name = "Flashlight", category = "Lighting", quantity = 1, packed = false,
                    ownerType = "user", ownerId = userId, requestingUserId = requestingUserId,
                )
            )

            val result = itemService.getByOwner(
                GetItemsByOwnerParam(ownerType = "user", ownerId = userId, requestingUserId = requestingUserId)
            )

            assertThat(result.isSuccess).isTrue()
            val items = (result as Result.Success).value
            assertThat(items).hasSize(1)
            assertThat(items[0].name).isEqualTo("Flashlight")
        }

        @Test
        fun `getByOwner returns Invalid when ownerType is invalid`() {
            val result = itemService.getByOwner(
                GetItemsByOwnerParam(ownerType = "invalid", ownerId = planId, requestingUserId = requestingUserId)
            )

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ItemError.Invalid::class.java)
            assertThat((error as ItemError.Invalid).field).isEqualTo("ownerType")
        }
    }

    @Nested
    inner class Update {
        @Test
        fun `update returns success when item exists`() {
            val created = (itemService.create(
                CreateItemParam(
                    name = "Tent", category = "Shelter", quantity = 1, packed = false,
                    ownerType = "plan", ownerId = planId, requestingUserId = requestingUserId,
                )
            ) as Result.Success).value

            val result = itemService.update(
                UpdateItemParam(
                    id = created.id,
                    name = "Big Tent",
                    category = "Shelter",
                    quantity = 2,
                    packed = true,
                    requestingUserId = requestingUserId,
                )
            )

            assertThat(result.isSuccess).isTrue()
            val item = (result as Result.Success).value
            assertThat(item.name).isEqualTo("Big Tent")
            assertThat(item.quantity).isEqualTo(2)
            assertThat(item.packed).isTrue()
        }

        @Test
        fun `update returns NotFound when item does not exist`() {
            val result = itemService.update(
                UpdateItemParam(
                    id = UUID.randomUUID(),
                    name = "Tent",
                    category = "Shelter",
                    quantity = 1,
                    packed = false,
                    requestingUserId = requestingUserId,
                )
            )

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(ItemError.NotFound::class.java)
        }

        @Test
        fun `update returns Invalid when name is blank`() {
            val created = (itemService.create(
                CreateItemParam(
                    name = "Tent", category = "Shelter", quantity = 1, packed = false,
                    ownerType = "plan", ownerId = planId, requestingUserId = requestingUserId,
                )
            ) as Result.Success).value

            val result = itemService.update(
                UpdateItemParam(
                    id = created.id,
                    name = "",
                    category = "Shelter",
                    quantity = 1,
                    packed = false,
                    requestingUserId = requestingUserId,
                )
            )

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ItemError.Invalid::class.java)
            assertThat((error as ItemError.Invalid).field).isEqualTo("name")
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `delete returns success when item exists`() {
            val created = (itemService.create(
                CreateItemParam(
                    name = "Tent", category = "Shelter", quantity = 1, packed = false,
                    ownerType = "plan", ownerId = planId, requestingUserId = requestingUserId,
                )
            ) as Result.Success).value

            val result = itemService.delete(DeleteItemParam(id = created.id, requestingUserId = requestingUserId))

            assertThat(result.isSuccess).isTrue()

            // Verify item is gone
            val getResult = itemService.getById(GetItemParam(id = created.id, requestingUserId = requestingUserId))
            assertThat(getResult.isFailure).isTrue()
            assertThat((getResult as Result.Failure).error).isInstanceOf(ItemError.NotFound::class.java)
        }

        @Test
        fun `delete returns NotFound when item does not exist`() {
            val result = itemService.delete(DeleteItemParam(id = UUID.randomUUID(), requestingUserId = requestingUserId))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(ItemError.NotFound::class.java)
        }
    }
}
