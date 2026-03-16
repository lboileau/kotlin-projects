package com.acme.services.camperservice.features.gearpack.dto

import java.time.Instant
import java.util.UUID

data class GearPackSummaryResponse(
    val id: UUID,
    val name: String,
    val description: String,
    val itemCount: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class GearPackDetailResponse(
    val id: UUID,
    val name: String,
    val description: String,
    val items: List<GearPackItemResponse>,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class GearPackItemResponse(
    val id: UUID,
    val name: String,
    val category: String,
    val defaultQuantity: Int,
    val scalable: Boolean,
    val sortOrder: Int,
)

data class ApplyGearPackResponse(
    val appliedCount: Int,
    val items: List<AppliedItemResponse>,
)

data class AppliedItemResponse(
    val id: UUID,
    val planId: UUID,
    val userId: UUID?,
    val name: String,
    val category: String,
    val quantity: Int,
    val packed: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)
