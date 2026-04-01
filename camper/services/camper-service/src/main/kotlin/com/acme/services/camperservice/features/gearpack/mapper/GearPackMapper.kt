package com.acme.services.camperservice.features.gearpack.mapper

import com.acme.clients.gearpackclient.model.GearPack as ClientGearPack
import com.acme.clients.gearpackclient.model.GearPackItem as ClientGearPackItem
import com.acme.services.camperservice.features.gearpack.dto.AppliedItemResponse
import com.acme.services.camperservice.features.gearpack.dto.ApplyGearPackResponse
import com.acme.services.camperservice.features.gearpack.dto.GearPackDetailResponse
import com.acme.services.camperservice.features.gearpack.dto.GearPackItemResponse
import com.acme.services.camperservice.features.gearpack.dto.GearPackSummaryResponse
import com.acme.services.camperservice.features.gearpack.model.AppliedItem
import com.acme.services.camperservice.features.gearpack.model.ApplyGearPackResult
import com.acme.services.camperservice.features.gearpack.model.GearPack
import com.acme.services.camperservice.features.gearpack.model.GearPackItem

object GearPackMapper {

    fun fromClient(client: ClientGearPack): GearPack = GearPack(
        id = client.id,
        name = client.name,
        description = client.description,
        items = client.items.map { fromClientItem(it) },
        createdAt = client.createdAt,
        updatedAt = client.updatedAt,
    )

    fun fromClientItem(client: ClientGearPackItem): GearPackItem = GearPackItem(
        id = client.id,
        name = client.name,
        category = client.category,
        defaultQuantity = client.defaultQuantity,
        scalable = client.scalable,
        sortOrder = client.sortOrder,
    )

    fun toSummaryResponse(pack: GearPack): GearPackSummaryResponse = GearPackSummaryResponse(
        id = pack.id,
        name = pack.name,
        description = pack.description,
        itemCount = pack.items.size,
        createdAt = pack.createdAt,
        updatedAt = pack.updatedAt,
    )

    fun toDetailResponse(pack: GearPack): GearPackDetailResponse = GearPackDetailResponse(
        id = pack.id,
        name = pack.name,
        description = pack.description,
        items = pack.items.map { item ->
            GearPackItemResponse(
                id = item.id,
                name = item.name,
                category = item.category,
                defaultQuantity = item.defaultQuantity,
                scalable = item.scalable,
                sortOrder = item.sortOrder,
            )
        },
        createdAt = pack.createdAt,
        updatedAt = pack.updatedAt,
    )

    fun toApplyResponse(result: ApplyGearPackResult): ApplyGearPackResponse = ApplyGearPackResponse(
        appliedCount = result.appliedCount,
        items = result.items.map { appliedItemToResponse(it) },
    )

    fun appliedItemToResponse(item: AppliedItem): AppliedItemResponse = AppliedItemResponse(
        id = item.id,
        planId = item.planId,
        userId = null,
        name = item.name,
        category = item.category,
        quantity = item.quantity,
        packed = item.packed,
        gearPackId = item.gearPackId,
        createdAt = item.createdAt,
        updatedAt = item.updatedAt,
    )
}
