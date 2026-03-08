package com.acme.services.camperservice.features.gearsync.actions

import com.acme.clients.assignmentclient.api.AssignmentClient
import com.acme.clients.assignmentclient.api.GetAssignmentMembersParam
import com.acme.clients.common.Result
import com.acme.clients.itemclient.api.CreateItemParam
import com.acme.clients.itemclient.api.DeleteItemParam
import com.acme.clients.itemclient.api.ItemClient
import com.acme.clients.itemclient.api.UpdateItemParam
import com.acme.clients.itemclient.model.Item
import com.acme.clients.planclient.api.PlanClient
import com.acme.services.camperservice.features.gearsync.dto.GearSyncItemResponse
import com.acme.services.camperservice.features.gearsync.dto.GearSyncResponse
import com.acme.services.camperservice.features.gearsync.error.GearSyncError
import com.acme.services.camperservice.features.gearsync.params.SyncGearParam
import java.util.UUID
import com.acme.clients.assignmentclient.api.GetByPlanIdParam as AssignmentGetByPlanIdParam
import com.acme.clients.itemclient.api.GetByPlanIdParam as ItemGetByPlanIdParam
import com.acme.clients.planclient.api.GetByIdParam as PlanGetByIdParam

internal class SyncGearAction(
    private val assignmentClient: AssignmentClient,
    private val itemClient: ItemClient,
    private val planClient: PlanClient,
) {
    companion object {
        private const val TENTS = "Tents"
        private const val CANOES = "Canoes"
        private const val PADDLES = "Paddles"
        private const val LIFE_JACKETS = "Life Jackets"
        private const val CATEGORY_CAMP = "camp"
        private const val CATEGORY_CANOE = "canoe"

        private val MANAGED_ITEMS = setOf(TENTS, CANOES, PADDLES, LIFE_JACKETS)
    }

    fun execute(param: SyncGearParam): Result<GearSyncResponse, GearSyncError> {
        // 1. Verify plan exists
        when (planClient.getById(PlanGetByIdParam(param.planId))) {
            is Result.Failure -> return Result.Failure(GearSyncError.PlanNotFound(param.planId.toString()))
            is Result.Success -> {}
        }

        // 2. Read all assignments for the plan
        val assignments = when (val result = assignmentClient.getByPlanId(AssignmentGetByPlanIdParam(param.planId))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(GearSyncError.PlanNotFound(param.planId.toString()))
        }

        // 3. Count tents and canoes
        val tentCount = assignments.count { it.type == "tent" }
        val canoeCount = assignments.count { it.type == "canoe" }

        // 4. Count total canoe members
        val canoeAssignments = assignments.filter { it.type == "canoe" }
        var totalCanoeMembers = 0
        for (canoe in canoeAssignments) {
            when (val result = assignmentClient.getMembers(GetAssignmentMembersParam(canoe.id))) {
                is Result.Success -> totalCanoeMembers += result.value.size
                is Result.Failure -> {}
            }
        }

        // 5. Build target quantities
        val targets = mapOf(
            TENTS to GearTarget(TENTS, CATEGORY_CAMP, tentCount),
            CANOES to GearTarget(CANOES, CATEGORY_CANOE, canoeCount),
            PADDLES to GearTarget(PADDLES, CATEGORY_CANOE, totalCanoeMembers),
            LIFE_JACKETS to GearTarget(LIFE_JACKETS, CATEGORY_CANOE, totalCanoeMembers),
        )

        // 6. Read current gear items for the plan
        val currentItems = when (val result = itemClient.getByPlanId(ItemGetByPlanIdParam(param.planId))) {
            is Result.Success -> result.value
            is Result.Failure -> emptyList()
        }

        // 7. Find existing managed items by name
        val existingByName = currentItems.filter { it.name in MANAGED_ITEMS }.associateBy { it.name }

        // 8. Sync each managed item
        val syncedItems = mutableListOf<GearSyncItemResponse>()
        for ((name, target) in targets) {
            val existing = existingByName[name]
            val synced = syncItem(param.planId, existing, target)
            if (synced != null) {
                syncedItems.add(synced)
            }
        }

        return Result.Success(GearSyncResponse(items = syncedItems))
    }

    private fun syncItem(planId: UUID, existing: Item?, target: GearTarget): GearSyncItemResponse? {
        if (target.quantity == 0 && existing != null) {
            // Delete item
            itemClient.delete(DeleteItemParam(existing.id))
            return null
        }

        if (target.quantity == 0) {
            // Nothing to do
            return null
        }

        if (existing == null) {
            // Create item
            when (val result = itemClient.create(
                CreateItemParam(
                    planId = planId,
                    userId = null,
                    name = target.name,
                    category = target.category,
                    quantity = target.quantity,
                    packed = false,
                )
            )) {
                is Result.Success -> return GearSyncItemResponse(
                    name = result.value.name,
                    category = result.value.category,
                    quantity = result.value.quantity,
                )
                is Result.Failure -> return null
            }
        }

        if (existing.quantity != target.quantity) {
            // Update quantity
            when (val result = itemClient.update(
                UpdateItemParam(
                    id = existing.id,
                    name = existing.name,
                    category = existing.category,
                    quantity = target.quantity,
                    packed = existing.packed,
                )
            )) {
                is Result.Success -> return GearSyncItemResponse(
                    name = result.value.name,
                    category = result.value.category,
                    quantity = result.value.quantity,
                )
                is Result.Failure -> return null
            }
        }

        // No change needed
        return GearSyncItemResponse(
            name = existing.name,
            category = existing.category,
            quantity = existing.quantity,
        )
    }

    private data class GearTarget(val name: String, val category: String, val quantity: Int)
}
