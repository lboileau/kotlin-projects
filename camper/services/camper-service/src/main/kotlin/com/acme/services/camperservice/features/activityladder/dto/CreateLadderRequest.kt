package com.acme.services.camperservice.features.activityladder.dto

import java.math.BigDecimal

data class NewActivityPayload(
    val name: String,
    val imageUrl: String,
    val distanceMinutes: Int,
    val costPerPerson: BigDecimal,
)

data class CreateLadderRequest(
    val title: String,
    val activities: List<NewActivityPayload>,
)
