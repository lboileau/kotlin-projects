package com.acme.services.camperservice.features.item.actions

import com.acme.clients.common.Result
import com.acme.clients.itemclient.api.ItemClient
import com.acme.services.camperservice.features.item.error.ItemError
import com.acme.services.camperservice.features.item.params.DeleteItemParam
import org.slf4j.LoggerFactory

internal class DeleteItemAction(private val itemClient: ItemClient) {
    private val logger = LoggerFactory.getLogger(DeleteItemAction::class.java)

    fun execute(param: DeleteItemParam): Result<Unit, ItemError> {
        TODO()
    }
}
