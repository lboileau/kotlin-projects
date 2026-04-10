package com.acme.services.camperservice.features.activityladder.dto

import java.math.BigDecimal

data class AddActivityRequest(
    val name: String,
    val imageUrl: String,
    val distanceMinutes: Int,
    val costPerPerson: BigDecimal,
)
