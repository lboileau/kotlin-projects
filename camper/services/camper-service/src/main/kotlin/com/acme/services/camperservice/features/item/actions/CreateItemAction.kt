package com.acme.services.camperservice.features.item.actions

import com.acme.clients.common.Result
import com.acme.clients.itemclient.api.ItemClient
import com.acme.services.camperservice.features.item.error.ItemError
import com.acme.services.camperservice.features.item.model.Item
import com.acme.services.camperservice.features.item.params.CreateItemParam
import org.slf4j.LoggerFactory

internal class CreateItemAction(private val itemClient: ItemClient) {
    private val logger = LoggerFactory.getLogger(CreateItemAction::class.java)

    fun execute(param: CreateItemParam): Result<Item, ItemError> {
        TODO()
    }
}
