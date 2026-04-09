package com.acme.services.camperservice.features.gearpack.dto

import java.util.UUID

data class ApplyGearPackRequest(
    val planId: UUID,
    val groupSize: Int,
)

data class CreateGearPackRequest(
    val name: String,
    val description: String = "",
)

data class UpdateGearPackRequest(
    val name: String? = null,
    val description: String? = null,
)

data class AddGearPackItemRequest(
    val name: String,
    val category: String,
    val defaultQuantity: Int = 1,
    val scalable: Boolean = false,
)

data class UpdateGearPackItemRequest(
    val name: String? = null,
    val category: String? = null,
    val defaultQuantity: Int? = null,
    val scalable: Boolean? = null,
)
