package com.acme.services.camperservice.features.activityladder.dto

import java.math.BigDecimal
import java.util.UUID

data class LadderActivityResponse(
    val id: UUID,
    val name: String,
    val imageUrl: String,
    val distanceMinutes: Int,
    val costPerPerson: BigDecimal,
    val losses: Int,
    val bracket: String,
    val displayOrder: Int,
)
