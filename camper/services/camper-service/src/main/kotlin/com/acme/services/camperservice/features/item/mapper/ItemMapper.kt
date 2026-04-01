package com.acme.services.camperservice.features.item.mapper

import com.acme.clients.itemclient.model.Item as ClientItem
import com.acme.services.camperservice.features.item.dto.ItemResponse
import com.acme.services.camperservice.features.item.model.Item

object ItemMapper {

    fun fromClient(clientItem: ClientItem): Item = Item(
        id = clientItem.id,
        planId = clientItem.planId,
        userId = clientItem.userId,
        name = clientItem.name,
        category = clientItem.category,
        quantity = clientItem.quantity,
        packed = clientItem.packed,
        gearPackId = clientItem.gearPackId,
        gearPackName = clientItem.gearPackName,
        createdAt = clientItem.createdAt,
        updatedAt = clientItem.updatedAt,
    )

    fun toResponse(item: Item): ItemResponse = ItemResponse(
        id = item.id,
        planId = item.planId,
        userId = item.userId,
        name = item.name,
        category = item.category,
        quantity = item.quantity,
        packed = item.packed,
        gearPackId = item.gearPackId,
        gearPackName = item.gearPackName,
        createdAt = item.createdAt,
        updatedAt = item.updatedAt,
    )
}
