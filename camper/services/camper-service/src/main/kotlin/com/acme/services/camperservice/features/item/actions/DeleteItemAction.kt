package com.acme.services.camperservice.features.item.actions

import com.acme.clients.common.Result
import com.acme.clients.itemclient.api.GetByIdParam as ClientGetByIdParam
import com.acme.clients.itemclient.api.ItemClient
import com.acme.clients.itemclient.api.DeleteItemParam as ClientDeleteItemParam
import com.acme.services.camperservice.common.auth.PlanRole
import com.acme.services.camperservice.common.auth.PlanRoleAuthorizer
import com.acme.services.camperservice.features.item.error.ItemError
import com.acme.services.camperservice.features.item.params.DeleteItemParam
import com.acme.services.camperservice.features.item.validations.ValidateDeleteItem
import org.slf4j.LoggerFactory

internal class DeleteItemAction(
    private val itemClient: ItemClient,
    private val planRoleAuthorizer: PlanRoleAuthorizer
) {
    private val logger = LoggerFactory.getLogger(DeleteItemAction::class.java)
    private val validate = ValidateDeleteItem()

    fun execute(param: DeleteItemParam): Result<Unit, ItemError> {
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

        logger.debug("Deleting item id={}", param.id)
        return when (val result = itemClient.delete(ClientDeleteItemParam(id = param.id))) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> Result.Failure(ItemError.fromClientError(result.error))
        }
    }
}
