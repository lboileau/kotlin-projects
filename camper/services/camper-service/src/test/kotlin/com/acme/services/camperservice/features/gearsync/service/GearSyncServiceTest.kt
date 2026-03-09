package com.acme.services.camperservice.features.gearsync.service

import com.acme.clients.assignmentclient.fake.FakeAssignmentClient
import com.acme.clients.assignmentclient.model.Assignment
import com.acme.clients.common.Result
import com.acme.clients.gearsyncclient.createGearSyncClient
import com.acme.clients.itemclient.fake.FakeItemClient
import com.acme.clients.itemclient.model.Item
import com.acme.clients.planclient.fake.FakePlanClient
import com.acme.clients.planclient.model.Plan
import com.acme.services.camperservice.features.gearsync.error.GearSyncError
import com.acme.services.camperservice.features.gearsync.params.SyncGearParam
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class GearSyncServiceTest {

    private val fakeAssignmentClient = FakeAssignmentClient()
    private val fakeItemClient = FakeItemClient()
    private val fakePlanClient = FakePlanClient()
    private val gearSyncClient = createGearSyncClient(fakeAssignmentClient, fakeItemClient, fakePlanClient)

    private val service = GearSyncService(gearSyncClient)

    private val planId = UUID.randomUUID()
    private val ownerId = UUID.randomUUID()
    private val now = Instant.now()

    @BeforeEach
    fun setUp() {
        fakeAssignmentClient.reset()
        fakeItemClient.reset()
        fakePlanClient.reset()

        fakePlanClient.seedPlan(
            Plan(id = planId, name = "Camping Trip", visibility = "private", ownerId = ownerId, createdAt = now, updatedAt = now)
        )
    }

    @Nested
    inner class Sync {

        @Test
        fun `returns error when plan not found`() {
            val result = service.sync(SyncGearParam(planId = UUID.randomUUID()))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(GearSyncError.PlanNotFound::class.java)
        }

        @Test
        fun `returns empty items when no assignments exist`() {
            val result = service.sync(SyncGearParam(planId = planId))

            assertThat(result.isSuccess).isTrue()
            val response = (result as Result.Success).value
            assertThat(response.items).isEmpty()
        }

        @Test
        fun `creates tent gear for tent assignments`() {
            val tentId = UUID.randomUUID()
            fakeAssignmentClient.seedAssignment(
                Assignment(id = tentId, planId = planId, name = "Big Agnes", type = "tent", maxOccupancy = 4, ownerId = ownerId, createdAt = now, updatedAt = now)
            )

            val result = service.sync(SyncGearParam(planId = planId))

            assertThat(result.isSuccess).isTrue()
            val response = (result as Result.Success).value
            val tents = response.items.find { it.name == "Tents" }
            assertThat(tents).isNotNull
            assertThat(tents!!.quantity).isEqualTo(1)
            assertThat(tents.category).isEqualTo("camp")
        }

        @Test
        fun `creates canoe gear with paddles and life jackets based on maxOccupancy`() {
            fakeAssignmentClient.seedAssignment(
                Assignment(id = UUID.randomUUID(), planId = planId, name = "Old Town", type = "canoe", maxOccupancy = 3, ownerId = ownerId, createdAt = now, updatedAt = now)
            )

            val result = service.sync(SyncGearParam(planId = planId))

            assertThat(result.isSuccess).isTrue()
            val response = (result as Result.Success).value
            val canoes = response.items.find { it.name == "Canoes" }
            val paddles = response.items.find { it.name == "Paddles" }
            val lifeJackets = response.items.find { it.name == "Life Jackets" }

            assertThat(canoes).isNotNull
            assertThat(canoes!!.quantity).isEqualTo(1)
            assertThat(canoes.category).isEqualTo("canoe")

            assertThat(paddles).isNotNull
            assertThat(paddles!!.quantity).isEqualTo(3)

            assertThat(lifeJackets).isNotNull
            assertThat(lifeJackets!!.quantity).isEqualTo(3)
        }

        @Test
        fun `counts multiple tent assignments`() {
            fakeAssignmentClient.seedAssignment(
                Assignment(id = UUID.randomUUID(), planId = planId, name = "Tent 1", type = "tent", maxOccupancy = 2, ownerId = ownerId, createdAt = now, updatedAt = now),
                Assignment(id = UUID.randomUUID(), planId = planId, name = "Tent 2", type = "tent", maxOccupancy = 4, ownerId = ownerId, createdAt = now, updatedAt = now),
                Assignment(id = UUID.randomUUID(), planId = planId, name = "Tent 3", type = "tent", maxOccupancy = 3, ownerId = ownerId, createdAt = now, updatedAt = now),
            )

            val result = service.sync(SyncGearParam(planId = planId))

            assertThat(result.isSuccess).isTrue()
            val tents = (result as Result.Success).value.items.find { it.name == "Tents" }
            assertThat(tents!!.quantity).isEqualTo(3)
        }

        @Test
        fun `updates existing gear items instead of creating duplicates`() {
            // Seed existing gear items from a previous sync
            fakeItemClient.seedItem(
                Item(id = UUID.randomUUID(), planId = planId, userId = null, name = "Tents", category = "camp", quantity = 1, packed = false, createdAt = now, updatedAt = now)
            )

            // Now there are 2 tents
            fakeAssignmentClient.seedAssignment(
                Assignment(id = UUID.randomUUID(), planId = planId, name = "Tent A", type = "tent", maxOccupancy = 2, ownerId = ownerId, createdAt = now, updatedAt = now),
                Assignment(id = UUID.randomUUID(), planId = planId, name = "Tent B", type = "tent", maxOccupancy = 4, ownerId = ownerId, createdAt = now, updatedAt = now),
            )

            val result = service.sync(SyncGearParam(planId = planId))

            assertThat(result.isSuccess).isTrue()
            val tents = (result as Result.Success).value.items.find { it.name == "Tents" }
            assertThat(tents!!.quantity).isEqualTo(2)

            // Verify only 1 "Tents" item exists (updated, not duplicated)
            val allItems = (fakeItemClient.getByPlanId(com.acme.clients.itemclient.api.GetByPlanIdParam(planId)) as Result.Success).value
            assertThat(allItems.filter { it.name == "Tents" }).hasSize(1)
        }

        @Test
        fun `deletes gear items when assignments are removed`() {
            // Seed existing gear from previous sync (1 tent existed)
            fakeItemClient.seedItem(
                Item(id = UUID.randomUUID(), planId = planId, userId = null, name = "Tents", category = "camp", quantity = 1, packed = false, createdAt = now, updatedAt = now)
            )

            // No assignments exist now
            val result = service.sync(SyncGearParam(planId = planId))

            assertThat(result.isSuccess).isTrue()
            val response = (result as Result.Success).value
            assertThat(response.items).isEmpty()

            // Verify item was deleted
            val allItems = (fakeItemClient.getByPlanId(com.acme.clients.itemclient.api.GetByPlanIdParam(planId)) as Result.Success).value
            assertThat(allItems.filter { it.name == "Tents" }).isEmpty()
        }

        @Test
        fun `aggregates maxOccupancy across multiple canoes for paddles and life jackets`() {
            fakeAssignmentClient.seedAssignment(
                Assignment(id = UUID.randomUUID(), planId = planId, name = "Canoe 1", type = "canoe", maxOccupancy = 2, ownerId = ownerId, createdAt = now, updatedAt = now),
                Assignment(id = UUID.randomUUID(), planId = planId, name = "Canoe 2", type = "canoe", maxOccupancy = 3, ownerId = ownerId, createdAt = now, updatedAt = now),
            )

            val result = service.sync(SyncGearParam(planId = planId))

            assertThat(result.isSuccess).isTrue()
            val response = (result as Result.Success).value
            assertThat(response.items.find { it.name == "Canoes" }!!.quantity).isEqualTo(2)
            assertThat(response.items.find { it.name == "Paddles" }!!.quantity).isEqualTo(5)
            assertThat(response.items.find { it.name == "Life Jackets" }!!.quantity).isEqualTo(5)
        }

        @Test
        fun `is idempotent — multiple syncs produce same result`() {
            val tentId = UUID.randomUUID()
            fakeAssignmentClient.seedAssignment(
                Assignment(id = tentId, planId = planId, name = "Tent A", type = "tent", maxOccupancy = 4, ownerId = ownerId, createdAt = now, updatedAt = now)
            )

            val result1 = service.sync(SyncGearParam(planId = planId))
            val result2 = service.sync(SyncGearParam(planId = planId))

            assertThat(result1.isSuccess).isTrue()
            assertThat(result2.isSuccess).isTrue()
            assertThat((result1 as Result.Success).value.items).isEqualTo((result2 as Result.Success).value.items)

            // Only 1 "Tents" item exists
            val allItems = (fakeItemClient.getByPlanId(com.acme.clients.itemclient.api.GetByPlanIdParam(planId)) as Result.Success).value
            assertThat(allItems.filter { it.name == "Tents" }).hasSize(1)
        }
    }
}
