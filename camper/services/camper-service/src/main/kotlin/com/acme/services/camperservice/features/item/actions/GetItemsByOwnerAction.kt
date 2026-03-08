package com.acme.services.camperservice.features.item.actions

import com.acme.clients.common.Result
import com.acme.clients.itemclient.api.GetByPlanIdParam
import com.acme.clients.itemclient.api.GetByUserIdParam
import com.acme.clients.itemclient.api.ItemClient
import com.acme.services.camperservice.features.item.error.ItemError
import com.acme.services.camperservice.features.item.mapper.ItemMapper
import com.acme.services.camperservice.features.item.model.Item
import com.acme.services.camperservice.features.item.params.GetItemsByOwnerParam
import com.acme.services.camperservice.features.item.validations.ValidateGetItemsByOwner
import org.slf4j.LoggerFactory

internal class GetItemsByOwnerAction(private val itemClient: ItemClient) {
    private val logger = LoggerFactory.getLogger(GetItemsByOwnerAction::class.java)
    private val validate = ValidateGetItemsByOwner()

    fun execute(param: GetItemsByOwnerParam): Result<List<Item>, ItemError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Getting items for ownerType={} ownerId={}", param.ownerType, param.ownerId)

        val result = when (param.ownerType) {
            "plan" -> itemClient.getByPlanId(GetByPlanIdParam(planId = param.ownerId))
            "user" -> itemClient.getByUserId(GetByUserIdParam(userId = param.ownerId))
            else -> return Result.Failure(ItemError.Invalid("ownerType", "must be 'plan' or 'user'"))
        }

        return when (result) {
            is Result.Success -> Result.Success(result.value.map { ItemMapper.fromClient(it) })
            is Result.Failure -> Result.Failure(ItemError.fromClientError(result.error))
        }
    }
}
