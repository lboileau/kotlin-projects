package com.acme.services.camperservice.features.item.actions

import com.acme.clients.common.Result
import com.acme.clients.itemclient.api.GetByIdParam as ClientGetByIdParam
import com.acme.clients.itemclient.api.ItemClient
import com.acme.clients.itemclient.api.UpdateItemParam as ClientUpdateItemParam
import com.acme.services.camperservice.common.auth.PlanRole
import com.acme.services.camperservice.common.auth.PlanRoleAuthorizer
import com.acme.services.camperservice.features.item.error.ItemError
import com.acme.services.camperservice.features.item.mapper.ItemMapper
import com.acme.services.camperservice.features.item.model.Item
import com.acme.services.camperservice.features.item.params.UpdateItemParam
import com.acme.services.camperservice.features.item.validations.ValidateUpdateItem
import org.slf4j.LoggerFactory

internal class UpdateItemAction(
    private val itemClient: ItemClient,
    private val planRoleAuthorizer: PlanRoleAuthorizer
) {
    private val logger = LoggerFactory.getLogger(UpdateItemAction::class.java)
    private val validate = ValidateUpdateItem()

    fun execute(param: UpdateItemParam): Result<Item, ItemError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        // Fetch the item to check if it's shared gear
        val existingItem = when (val getResult = itemClient.getById(ClientGetByIdParam(id = param.id))) {
            is Result.Success -> getResult.value
            is Result.Failure -> return Result.Failure(ItemError.fromClientError(getResult.error))
        }

        // Authorize shared gear mutations (userId == null means shared gear)
        val itemPlanId = existingItem.planId
        if (existingItem.userId == null && itemPlanId != null) {
            val authResult = planRoleAuthorizer.authorize(
                itemPlanId, param.requestingUserId, setOf(PlanRole.OWNER, PlanRole.MANAGER)
            )
            if (authResult is Result.Failure) {
                return Result.Failure(ItemError.Forbidden(itemPlanId.toString(), param.requestingUserId.toString()))
            }
        }

        logger.debug("Updating item id={}", param.id)
        return when (val result = itemClient.update(ClientUpdateItemParam(
            id = param.id,
            name = param.name,
            category = param.category,
            quantity = param.quantity,
            packed = param.packed,
        ))) {
            is Result.Success -> Result.Success(ItemMapper.fromClient(result.value))
            is Result.Failure -> Result.Failure(ItemError.fromClientError(result.error))
        }
    }
}
