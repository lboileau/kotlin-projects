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

data class CreateGearPackParam(
    val name: String,
    val description: String,
    val requestingUserId: UUID,
)

data class UpdateGearPackParam(
    val id: UUID,
    val name: String?,
    val description: String?,
    val requestingUserId: UUID,
)

data class DeleteGearPackParam(
    val id: UUID,
    val requestingUserId: UUID,
)

data class AddGearPackItemParam(
    val gearPackId: UUID,
    val name: String,
    val category: String,
    val defaultQuantity: Int,
    val scalable: Boolean,
    val requestingUserId: UUID,
)

data class UpdateGearPackItemParam(
    val id: UUID,
    val gearPackId: UUID,
    val name: String?,
    val category: String?,
    val defaultQuantity: Int?,
    val scalable: Boolean?,
    val requestingUserId: UUID,
)

data class RemoveGearPackItemParam(
    val id: UUID,
    val gearPackId: UUID,
    val requestingUserId: UUID,
)

data class SearchGearPackItemsParam(
    val query: String,
    val requestingUserId: UUID,
)
