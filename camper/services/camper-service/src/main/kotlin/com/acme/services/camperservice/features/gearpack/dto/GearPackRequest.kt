package com.acme.services.camperservice.features.gearpack.dto

import java.util.UUID

data class ApplyGearPackRequest(
    val planId: UUID,
    val groupSize: Int,
)
