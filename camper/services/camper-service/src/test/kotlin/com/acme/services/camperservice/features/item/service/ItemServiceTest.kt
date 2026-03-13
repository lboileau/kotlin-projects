package com.acme.services.camperservice.features.item.service

import com.acme.clients.common.Result
import com.acme.clients.itemclient.fake.FakeItemClient
import com.acme.clients.itemclient.model.Item as ClientItem
import com.acme.clients.planclient.fake.FakePlanClient
import com.acme.clients.planclient.model.Plan
import com.acme.clients.planclient.model.PlanMember
import com.acme.services.camperservice.common.auth.PlanRoleAuthorizer
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
    private val fakePlanClient = FakePlanClient()
    private val planRoleAuthorizer = PlanRoleAuthorizer(fakePlanClient)
    private val itemService = ItemService(fakeItemClient, planRoleAuthorizer)

    private val planId = UUID.randomUUID()
    private val userId = UUID.randomUUID()
    private val requestingUserId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        fakeItemClient.reset()
        fakePlanClient.reset()
        // Seed a plan where requestingUserId is the owner, so shared gear auth passes
        fakePlanClient.seedPlan(Plan(id = planId, name = "Test Plan", visibility = "private", ownerId = requestingUserId, createdAt = Instant.now(), updatedAt = Instant.now()))
        fakePlanClient.seedMember(PlanMember(planId = planId, userId = requestingUserId, role = "member", createdAt = Instant.now()))
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
        fun `create returns success for user owner with planId`() {
            val result = itemService.create(
                CreateItemParam(
                    name = "Flashlight",
                    category = "Lighting",
                    quantity = 2,
                    packed = true,
                    ownerType = "user",
                    ownerId = userId,
                    planId = planId,
                    requestingUserId = requestingUserId,
                )
            )

            assertThat(result.isSuccess).isTrue()
            val item = (result as Result.Success).value
            assertThat(item.name).isEqualTo("Flashlight")
            assertThat(item.quantity).isEqualTo(2)
            assertThat(item.packed).isTrue()
            assertThat(item.planId).isEqualTo(planId)
            assertThat(item.userId).isEqualTo(userId)
        }

        @Test
        fun `create returns Invalid when user owner has no planId`() {
            val result = itemService.create(
                CreateItemParam(
                    name = "Flashlight",
                    category = "Lighting",
                    quantity = 1,
                    packed = false,
                    ownerType = "user",
                    ownerId = userId,
                    requestingUserId = requestingUserId,
                )
            )

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ItemError.Invalid::class.java)
            assertThat((error as ItemError.Invalid).field).isEqualTo("planId")
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
        fun `getByOwner returns user items scoped to plan`() {
            val plan2Id = UUID.randomUUID()

            itemService.create(
                CreateItemParam(
                    name = "Flashlight", category = "Lighting", quantity = 1, packed = false,
                    ownerType = "user", ownerId = userId, planId = planId, requestingUserId = requestingUserId,
                )
            )
            itemService.create(
                CreateItemParam(
                    name = "Knife", category = "Tools", quantity = 1, packed = false,
                    ownerType = "user", ownerId = userId, planId = plan2Id, requestingUserId = requestingUserId,
                )
            )

            val result = itemService.getByOwner(
                GetItemsByOwnerParam(ownerType = "user", ownerId = userId, planId = planId, requestingUserId = requestingUserId)
            )

            assertThat(result.isSuccess).isTrue()
            val items = (result as Result.Success).value
            assertThat(items).hasSize(1)
            assertThat(items[0].name).isEqualTo("Flashlight")
        }

        @Test
        fun `getByOwner returns Invalid for user without planId`() {
            val result = itemService.getByOwner(
                GetItemsByOwnerParam(ownerType = "user", ownerId = userId, requestingUserId = requestingUserId)
            )

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ItemError.Invalid::class.java)
            assertThat((error as ItemError.Invalid).field).isEqualTo("planId")
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

    @Nested
    inner class SharedGearAuthorization {

        private val managerId = UUID.randomUUID()
        private val memberId = UUID.randomUUID()

        @BeforeEach
        fun seedRoles() {
            fakePlanClient.seedMember(PlanMember(planId = planId, userId = managerId, role = "manager", createdAt = Instant.now()))
            fakePlanClient.seedMember(PlanMember(planId = planId, userId = memberId, role = "member", createdAt = Instant.now()))
        }

        @Test
        fun `create shared gear returns Forbidden for regular member`() {
            val result = itemService.create(
                CreateItemParam(
                    name = "Tent", category = "Shelter", quantity = 1, packed = false,
                    ownerType = "plan", ownerId = planId, requestingUserId = memberId,
                )
            )

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(ItemError.Forbidden::class.java)
        }

        @Test
        fun `create shared gear succeeds for manager`() {
            val result = itemService.create(
                CreateItemParam(
                    name = "Tent", category = "Shelter", quantity = 1, packed = false,
                    ownerType = "plan", ownerId = planId, requestingUserId = managerId,
                )
            )

            assertThat(result.isSuccess).isTrue()
            assertThat((result as Result.Success).value.name).isEqualTo("Tent")
        }

        @Test
        fun `create shared gear succeeds for owner`() {
            val result = itemService.create(
                CreateItemParam(
                    name = "Tent", category = "Shelter", quantity = 1, packed = false,
                    ownerType = "plan", ownerId = planId, requestingUserId = requestingUserId,
                )
            )

            assertThat(result.isSuccess).isTrue()
        }

        @Test
        fun `create personal gear is not affected by role authorization`() {
            val result = itemService.create(
                CreateItemParam(
                    name = "Flashlight", category = "Lighting", quantity = 1, packed = false,
                    ownerType = "user", ownerId = memberId, planId = planId, requestingUserId = memberId,
                )
            )

            assertThat(result.isSuccess).isTrue()
            assertThat((result as Result.Success).value.userId).isEqualTo(memberId)
        }

        @Test
        fun `update shared gear returns Forbidden for regular member`() {
            val created = (itemService.create(
                CreateItemParam(
                    name = "Tent", category = "Shelter", quantity = 1, packed = false,
                    ownerType = "plan", ownerId = planId, requestingUserId = requestingUserId,
                )
            ) as Result.Success).value

            val result = itemService.update(
                UpdateItemParam(
                    id = created.id, name = "Big Tent", category = "Shelter",
                    quantity = 2, packed = false, requestingUserId = memberId,
                )
            )

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(ItemError.Forbidden::class.java)
        }

        @Test
        fun `update shared gear succeeds for manager`() {
            val created = (itemService.create(
                CreateItemParam(
                    name = "Tent", category = "Shelter", quantity = 1, packed = false,
                    ownerType = "plan", ownerId = planId, requestingUserId = requestingUserId,
                )
            ) as Result.Success).value

            val result = itemService.update(
                UpdateItemParam(
                    id = created.id, name = "Big Tent", category = "Shelter",
                    quantity = 2, packed = false, requestingUserId = managerId,
                )
            )

            assertThat(result.isSuccess).isTrue()
            assertThat((result as Result.Success).value.name).isEqualTo("Big Tent")
        }

        @Test
        fun `delete shared gear returns Forbidden for regular member`() {
            val created = (itemService.create(
                CreateItemParam(
                    name = "Tent", category = "Shelter", quantity = 1, packed = false,
                    ownerType = "plan", ownerId = planId, requestingUserId = requestingUserId,
                )
            ) as Result.Success).value

            val result = itemService.delete(DeleteItemParam(id = created.id, requestingUserId = memberId))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(ItemError.Forbidden::class.java)
        }

        @Test
        fun `delete shared gear succeeds for manager`() {
            val created = (itemService.create(
                CreateItemParam(
                    name = "Tent", category = "Shelter", quantity = 1, packed = false,
                    ownerType = "plan", ownerId = planId, requestingUserId = requestingUserId,
                )
            ) as Result.Success).value

            val result = itemService.delete(DeleteItemParam(id = created.id, requestingUserId = managerId))

            assertThat(result.isSuccess).isTrue()
        }

        @Test
        fun `update personal gear is not affected by role authorization`() {
            val personalItem = (itemService.create(
                CreateItemParam(
                    name = "Flashlight", category = "Lighting", quantity = 1, packed = false,
                    ownerType = "user", ownerId = memberId, planId = planId, requestingUserId = memberId,
                )
            ) as Result.Success).value

            val result = itemService.update(
                UpdateItemParam(
                    id = personalItem.id, name = "LED Flashlight", category = "Lighting",
                    quantity = 1, packed = true, requestingUserId = memberId,
                )
            )

            assertThat(result.isSuccess).isTrue()
            assertThat((result as Result.Success).value.name).isEqualTo("LED Flashlight")
        }

        @Test
        fun `delete personal gear is not affected by role authorization`() {
            val personalItem = (itemService.create(
                CreateItemParam(
                    name = "Flashlight", category = "Lighting", quantity = 1, packed = false,
                    ownerType = "user", ownerId = memberId, planId = planId, requestingUserId = memberId,
                )
            ) as Result.Success).value

            val result = itemService.delete(DeleteItemParam(id = personalItem.id, requestingUserId = memberId))

            assertThat(result.isSuccess).isTrue()
        }
    }
}
