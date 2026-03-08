package com.acme.services.camperservice.features.item.actions

import com.acme.clients.common.Result
import com.acme.clients.itemclient.api.ItemClient
import com.acme.clients.itemclient.api.DeleteItemParam as ClientDeleteItemParam
import com.acme.services.camperservice.features.item.error.ItemError
import com.acme.services.camperservice.features.item.params.DeleteItemParam
import com.acme.services.camperservice.features.item.validations.ValidateDeleteItem
import org.slf4j.LoggerFactory

internal class DeleteItemAction(private val itemClient: ItemClient) {
    private val logger = LoggerFactory.getLogger(DeleteItemAction::class.java)
    private val validate = ValidateDeleteItem()

    fun execute(param: DeleteItemParam): Result<Unit, ItemError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Deleting item id={}", param.id)
        return when (val result = itemClient.delete(ClientDeleteItemParam(id = param.id))) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> Result.Failure(ItemError.fromClientError(result.error))
        }
    }
}
