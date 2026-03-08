package com.acme.clients.gearsyncclient.internal

import com.acme.clients.assignmentclient.api.AssignmentClient
import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.gearsyncclient.api.GearSyncClient
import com.acme.clients.gearsyncclient.api.SyncGearParam
import com.acme.clients.gearsyncclient.model.GearSyncItem
import com.acme.clients.itemclient.api.CreateItemParam
import com.acme.clients.itemclient.api.DeleteItemParam
import com.acme.clients.itemclient.api.ItemClient
import com.acme.clients.itemclient.api.UpdateItemParam
import com.acme.clients.itemclient.model.Item
import com.acme.clients.planclient.api.PlanClient
import java.util.UUID
import com.acme.clients.assignmentclient.api.GetByPlanIdParam as AssignmentGetByPlanIdParam
import com.acme.clients.itemclient.api.GetByPlanIdParam as ItemGetByPlanIdParam
import com.acme.clients.planclient.api.GetByIdParam as PlanGetByIdParam

internal class DefaultGearSyncClient(
    private val assignmentClient: AssignmentClient,
    private val itemClient: ItemClient,
    private val planClient: PlanClient,
) : GearSyncClient {

    companion object {
        private const val TENTS = "Tents"
        private const val CANOES = "Canoes"
        private const val PADDLES = "Paddles"
        private const val LIFE_JACKETS = "Life Jackets"
        private const val CATEGORY_CAMP = "camp"
        private const val CATEGORY_CANOE = "canoe"

        private val MANAGED_ITEMS = setOf(TENTS, CANOES, PADDLES, LIFE_JACKETS)
    }

    override fun sync(param: SyncGearParam): Result<List<GearSyncItem>, AppError> {
        // 1. Verify plan exists
        when (planClient.getById(PlanGetByIdParam(param.planId))) {
            is Result.Failure -> return Result.Failure(NotFoundError("Plan", param.planId.toString()))
            is Result.Success -> {}
        }

        // 2. Read all assignments for the plan
        val assignments = when (val result = assignmentClient.getByPlanId(AssignmentGetByPlanIdParam(param.planId))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(NotFoundError("Plan", param.planId.toString()))
        }

        // 3. Count tents and canoes
        val tentCount = assignments.count { it.type == "tent" }
        val canoeCount = assignments.count { it.type == "canoe" }

        // 4. Sum max occupancy across all canoes for paddles/life jackets
        val totalCanoeCapacity = assignments.filter { it.type == "canoe" }.sumOf { it.maxOccupancy }

        // 5. Build target quantities
        val targets = mapOf(
            TENTS to GearTarget(TENTS, CATEGORY_CAMP, tentCount),
            CANOES to GearTarget(CANOES, CATEGORY_CANOE, canoeCount),
            PADDLES to GearTarget(PADDLES, CATEGORY_CANOE, totalCanoeCapacity),
            LIFE_JACKETS to GearTarget(LIFE_JACKETS, CATEGORY_CANOE, totalCanoeCapacity),
        )

        // 6. Read current gear items for the plan
        val currentItems = when (val result = itemClient.getByPlanId(ItemGetByPlanIdParam(param.planId))) {
            is Result.Success -> result.value
            is Result.Failure -> emptyList()
        }

        // 7. Find existing managed items by name
        val existingByName = currentItems.filter { it.name in MANAGED_ITEMS }.associateBy { it.name }

        // 8. Sync each managed item
        val syncedItems = mutableListOf<GearSyncItem>()
        for ((name, target) in targets) {
            val existing = existingByName[name]
            val synced = syncItem(param.planId, existing, target)
            if (synced != null) {
                syncedItems.add(synced)
            }
        }

        return Result.Success(syncedItems)
    }

    private fun syncItem(planId: UUID, existing: Item?, target: GearTarget): GearSyncItem? {
        if (target.quantity == 0 && existing != null) {
            itemClient.delete(DeleteItemParam(existing.id))
            return null
        }

        if (target.quantity == 0) {
            return null
        }

        if (existing == null) {
            return when (val result = itemClient.create(
                CreateItemParam(
                    planId = planId,
                    userId = null,
                    name = target.name,
                    category = target.category,
                    quantity = target.quantity,
                    packed = false,
                )
            )) {
                is Result.Success -> GearSyncItem(
                    name = result.value.name,
                    category = result.value.category,
                    quantity = result.value.quantity,
                )
                is Result.Failure -> null
            }
        }

        if (existing.quantity != target.quantity) {
            return when (val result = itemClient.update(
                UpdateItemParam(
                    id = existing.id,
                    name = existing.name,
                    category = existing.category,
                    quantity = target.quantity,
                    packed = existing.packed,
                )
            )) {
                is Result.Success -> GearSyncItem(
                    name = result.value.name,
                    category = result.value.category,
                    quantity = result.value.quantity,
                )
                is Result.Failure -> null
            }
        }

        // No change needed
        return GearSyncItem(
            name = existing.name,
            category = existing.category,
            quantity = existing.quantity,
        )
    }

    private data class GearTarget(val name: String, val category: String, val quantity: Int)
}
