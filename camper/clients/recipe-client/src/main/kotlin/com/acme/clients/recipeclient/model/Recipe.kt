package com.acme.clients.recipeclient.model

import java.time.Instant
import java.util.UUID

data class Recipe(
    val id: UUID,
    val name: String,
    val description: String?,
    val webLink: String?,
    val baseServings: Int,
    val status: String,
    val createdBy: UUID,
    val duplicateOfId: UUID?,
    val createdAt: Instant,
    val updatedAt: Instant
)
