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

    fun fromClient(client: ClientGearPack): GearPack = TODO()

    fun fromClientItem(client: ClientGearPackItem): GearPackItem = TODO()

    fun toSummaryResponse(pack: GearPack): GearPackSummaryResponse = TODO()

    fun toDetailResponse(pack: GearPack): GearPackDetailResponse = TODO()

    fun toApplyResponse(result: ApplyGearPackResult): ApplyGearPackResponse = TODO()

    fun appliedItemToResponse(item: AppliedItem): AppliedItemResponse = TODO()
}
