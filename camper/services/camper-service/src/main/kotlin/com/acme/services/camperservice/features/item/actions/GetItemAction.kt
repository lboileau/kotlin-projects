package com.acme.services.camperservice.features.item.actions

import com.acme.clients.common.Result
import com.acme.clients.itemclient.api.GetByIdParam as ClientGetByIdParam
import com.acme.clients.itemclient.api.ItemClient
import com.acme.services.camperservice.features.item.error.ItemError
import com.acme.services.camperservice.features.item.mapper.ItemMapper
import com.acme.services.camperservice.features.item.model.Item
import com.acme.services.camperservice.features.item.params.GetItemParam
import com.acme.services.camperservice.features.item.validations.ValidateGetItem
import org.slf4j.LoggerFactory

internal class GetItemAction(private val itemClient: ItemClient) {
    private val logger = LoggerFactory.getLogger(GetItemAction::class.java)
    private val validate = ValidateGetItem()

    fun execute(param: GetItemParam): Result<Item, ItemError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Getting item id={}", param.id)
        return when (val result = itemClient.getById(ClientGetByIdParam(id = param.id))) {
            is Result.Success -> Result.Success(ItemMapper.fromClient(result.value))
            is Result.Failure -> Result.Failure(ItemError.fromClientError(result.error))
        }
    }
}
