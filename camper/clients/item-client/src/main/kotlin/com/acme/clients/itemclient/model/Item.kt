package com.acme.clients.itemclient.model

import java.time.Instant
import java.util.UUID

data class Item(
    val id: UUID,
    val planId: UUID?,
    val userId: UUID?,
    val name: String,
    val category: String,
    val quantity: Int,
    val packed: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)
