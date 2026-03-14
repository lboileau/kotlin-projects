package com.acme.services.camperservice.features.plan.dto

import com.acme.services.camperservice.features.user.dto.AvatarResponse
import java.time.Instant
import java.util.UUID

data class PlanResponse(
    val id: UUID,
    val name: String,
    val visibility: String,
    val ownerId: UUID,
    val createdAt: Instant,
    val updatedAt: Instant,
    val isMember: Boolean
)

data class PlanMemberResponse(
    val planId: UUID,
    val userId: UUID,
    val username: String?,
    val email: String?,
    val invitationStatus: String?,
    val invitedBy: UUID?,
    val role: String,
    val avatarSeed: String?,
    val avatar: AvatarResponse?,
    val createdAt: Instant
)
