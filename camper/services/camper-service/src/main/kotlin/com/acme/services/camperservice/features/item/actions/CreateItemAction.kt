package com.acme.services.camperservice.features.item.actions

import com.acme.clients.common.Result
import com.acme.clients.itemclient.api.ItemClient
import com.acme.clients.itemclient.api.CreateItemParam as ClientCreateItemParam
import com.acme.services.camperservice.features.item.error.ItemError
import com.acme.services.camperservice.features.item.mapper.ItemMapper
import com.acme.services.camperservice.features.item.model.Item
import com.acme.services.camperservice.features.item.params.CreateItemParam
import com.acme.services.camperservice.features.item.validations.ValidateCreateItem
import org.slf4j.LoggerFactory

internal class CreateItemAction(private val itemClient: ItemClient) {
    private val logger = LoggerFactory.getLogger(CreateItemAction::class.java)
    private val validate = ValidateCreateItem()

    fun execute(param: CreateItemParam): Result<Item, ItemError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Creating item name={} for ownerType={} ownerId={}", param.name, param.ownerType, param.ownerId)

        val planId = if (param.ownerType == "plan") param.ownerId else param.planId
        val userId = if (param.ownerType == "user") param.ownerId else null

        return when (val result = itemClient.create(ClientCreateItemParam(
            planId = planId,
            userId = userId,
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
