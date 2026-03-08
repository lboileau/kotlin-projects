package com.acme.services.camperservice.features.item.actions

import com.acme.clients.common.Result
import com.acme.clients.itemclient.api.ItemClient
import com.acme.services.camperservice.features.item.error.ItemError
import com.acme.services.camperservice.features.item.model.Item
import com.acme.services.camperservice.features.item.params.UpdateItemParam
import org.slf4j.LoggerFactory

internal class UpdateItemAction(private val itemClient: ItemClient) {
    private val logger = LoggerFactory.getLogger(UpdateItemAction::class.java)

    fun execute(param: UpdateItemParam): Result<Item, ItemError> {
        TODO()
    }
}
