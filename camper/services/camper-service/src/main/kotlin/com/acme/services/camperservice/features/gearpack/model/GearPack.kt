package com.acme.services.camperservice.features.gearpack.model

import java.time.Instant
import java.util.UUID

data class GearPack(
    val id: UUID,
    val name: String,
    val description: String,
    val items: List<GearPackItem>,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class GearPackItem(
    val id: UUID,
    val name: String,
    val category: String,
    val defaultQuantity: Int,
    val scalable: Boolean,
    val sortOrder: Int,
)

data class ApplyGearPackResult(
    val appliedCount: Int,
    val items: List<AppliedItem>,
)

data class AppliedItem(
    val id: UUID,
    val planId: UUID,
    val name: String,
    val category: String,
    val quantity: Int,
    val packed: Boolean,
    val gearPackId: UUID? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)
