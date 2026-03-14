package com.acme.services.camperservice.features.plan.model

import java.time.Instant
import java.util.UUID

data class Plan(
    val id: UUID,
    val name: String,
    val visibility: String,
    val ownerId: UUID,
    val createdAt: Instant,
    val updatedAt: Instant,
    val isMember: Boolean = true
)

data class PlanMember(
    val planId: UUID,
    val userId: UUID,
    val username: String?,
    val email: String?,
    val invitationStatus: String?,
    val role: String,
    val avatarSeed: String?,
    val createdAt: Instant
)
