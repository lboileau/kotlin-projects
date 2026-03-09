package com.acme.services.camperservice.features.item.dto

import java.util.UUID

data class CreateItemRequest(
    val name: String,
    val category: String,
    val quantity: Int,
    val packed: Boolean,
    val ownerType: String,
    val ownerId: UUID,
    val planId: UUID? = null,
)

data class UpdateItemRequest(
    val name: String,
    val category: String,
    val quantity: Int,
    val packed: Boolean,
)
