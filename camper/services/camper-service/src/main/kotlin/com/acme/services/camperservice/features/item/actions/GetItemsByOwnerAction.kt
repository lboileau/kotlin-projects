package com.acme.services.camperservice.features.item.actions

import com.acme.clients.common.Result
import com.acme.clients.itemclient.api.ItemClient
import com.acme.services.camperservice.features.item.error.ItemError
import com.acme.services.camperservice.features.item.model.Item
import com.acme.services.camperservice.features.item.params.GetItemsByOwnerParam
import org.slf4j.LoggerFactory

internal class GetItemsByOwnerAction(private val itemClient: ItemClient) {
    private val logger = LoggerFactory.getLogger(GetItemsByOwnerAction::class.java)

    fun execute(param: GetItemsByOwnerParam): Result<List<Item>, ItemError> {
        TODO()
    }
}
