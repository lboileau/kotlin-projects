package com.acme.services.camperservice.features.item.params

import java.util.UUID

data class CreateItemParam(
    val name: String,
    val category: String,
    val quantity: Int,
    val packed: Boolean,
    val ownerType: String,
    val ownerId: UUID,
    val planId: UUID? = null,
    val gearPackId: UUID? = null,
    val requestingUserId: UUID,
)

data class GetItemParam(val id: UUID, val requestingUserId: UUID)

data class GetItemsByOwnerParam(val ownerType: String, val ownerId: UUID, val planId: UUID? = null, val requestingUserId: UUID)

data class UpdateItemParam(
    val id: UUID,
    val name: String,
    val category: String,
    val quantity: Int,
    val packed: Boolean,
    val gearPackId: UUID? = null,
    val requestingUserId: UUID,
)

data class DeleteItemParam(val id: UUID, val requestingUserId: UUID)
