package com.acme.clients.gearpackclient.model

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
    val gearPackId: UUID,
    val name: String,
    val category: String,
    val defaultQuantity: Int,
    val scalable: Boolean,
    val sortOrder: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
)
