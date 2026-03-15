package com.acme.services.camperservice.features.gearpack.params

import java.util.UUID

data class ListGearPacksParam(val requestingUserId: UUID)

data class GetGearPackParam(val id: UUID, val requestingUserId: UUID)

data class ApplyGearPackParam(
    val gearPackId: UUID,
    val planId: UUID,
    val groupSize: Int,
    val requestingUserId: UUID,
)
