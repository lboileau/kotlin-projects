package com.acme.services.camperservice.features.item.service

import com.acme.clients.itemclient.api.ItemClient
import com.acme.services.camperservice.features.item.actions.*
import com.acme.services.camperservice.features.item.params.*

class ItemService(itemClient: ItemClient) {
    private val createItem = CreateItemAction(itemClient)
    private val getItem = GetItemAction(itemClient)
    private val getItemsByOwner = GetItemsByOwnerAction(itemClient)
    private val updateItem = UpdateItemAction(itemClient)
    private val deleteItem = DeleteItemAction(itemClient)

    fun create(param: CreateItemParam) = createItem.execute(param)
    fun getById(param: GetItemParam) = getItem.execute(param)
    fun getByOwner(param: GetItemsByOwnerParam) = getItemsByOwner.execute(param)
    fun update(param: UpdateItemParam) = updateItem.execute(param)
    fun delete(param: DeleteItemParam) = deleteItem.execute(param)
}
