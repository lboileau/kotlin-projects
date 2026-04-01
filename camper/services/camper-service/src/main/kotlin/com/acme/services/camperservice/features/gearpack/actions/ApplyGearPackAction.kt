package com.acme.services.camperservice.features.gearpack.actions

import com.acme.clients.common.Result
import com.acme.clients.gearpackclient.api.GearPackClient
import com.acme.clients.gearpackclient.api.GetGearPackByIdParam
import com.acme.clients.itemclient.api.CreateItemParam as ClientCreateItemParam
import com.acme.clients.itemclient.api.ItemClient
import com.acme.services.camperservice.common.auth.PlanRole
import com.acme.services.camperservice.common.auth.PlanRoleAuthorizer
import com.acme.services.camperservice.features.gearpack.error.GearPackError
import com.acme.services.camperservice.features.gearpack.model.AppliedItem
import com.acme.services.camperservice.features.gearpack.model.ApplyGearPackResult
import com.acme.services.camperservice.features.gearpack.params.ApplyGearPackParam
import com.acme.services.camperservice.features.gearpack.validations.ValidateApplyGearPack
import org.slf4j.LoggerFactory

internal class ApplyGearPackAction(
    private val gearPackClient: GearPackClient,
    private val itemClient: ItemClient,
    private val planRoleAuthorizer: PlanRoleAuthorizer,
) {
    private val logger = LoggerFactory.getLogger(ApplyGearPackAction::class.java)
    private val validate = ValidateApplyGearPack()

    fun execute(param: ApplyGearPackParam): Result<ApplyGearPackResult, GearPackError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        // Authorize
        val authResult = planRoleAuthorizer.authorize(
            param.planId, param.requestingUserId, setOf(PlanRole.OWNER, PlanRole.MANAGER)
        )
        if (authResult is Result.Failure) {
            return Result.Failure(
                GearPackError.Forbidden(param.planId.toString(), param.requestingUserId.toString())
            )
        }

        // Fetch gear pack
        val pack = when (val packResult = gearPackClient.getById(GetGearPackByIdParam(id = param.gearPackId))) {
            is Result.Success -> packResult.value
            is Result.Failure -> return Result.Failure(GearPackError.fromClientError(packResult.error))
        }

        logger.debug("Applying gear pack '{}' ({} items) to plan={} with groupSize={}",
            pack.name, pack.items.size, param.planId, param.groupSize)

        // Create items
        val appliedItems = mutableListOf<AppliedItem>()
        for (packItem in pack.items) {
            val finalQuantity = if (packItem.scalable) {
                packItem.defaultQuantity * param.groupSize
            } else {
                packItem.defaultQuantity
            }

            val createResult = itemClient.create(
                ClientCreateItemParam(
                    planId = param.planId,
                    userId = null,
                    name = packItem.name,
                    category = packItem.category,
                    quantity = finalQuantity,
                    packed = false,
                    gearPackId = param.gearPackId,
                )
            )

            when (createResult) {
                is Result.Success -> {
                    val item = createResult.value
                    appliedItems.add(
                        AppliedItem(
                            id = item.id,
                            planId = item.planId!!,
                            name = item.name,
                            category = item.category,
                            quantity = item.quantity,
                            packed = item.packed,
                            gearPackId = item.gearPackId,
                            createdAt = item.createdAt,
                            updatedAt = item.updatedAt,
                        )
                    )
                }
                is Result.Failure -> {
                    return Result.Failure(
                        GearPackError.ApplyFailed(pack.name, "Failed to create item '${packItem.name}': ${createResult.error.message}")
                    )
                }
            }
        }

        return Result.Success(
            ApplyGearPackResult(
                appliedCount = appliedItems.size,
                items = appliedItems,
            )
        )
    }
}
