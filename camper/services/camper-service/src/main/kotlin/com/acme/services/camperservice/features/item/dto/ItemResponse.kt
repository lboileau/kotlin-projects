package com.acme.services.camperservice.features.item.dto

import java.time.Instant
import java.util.UUID

data class ItemResponse(
    val id: UUID,
    val planId: UUID?,
    val userId: UUID?,
    val name: String,
    val category: String,
    val quantity: Int,
    val packed: Boolean,
    val gearPackId: UUID? = null,
    val gearPackName: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)
