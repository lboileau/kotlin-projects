package com.acme.services.camperservice.features.assignment.dto

import java.time.Instant
import java.util.UUID

data class AssignmentResponse(
    val id: UUID,
    val planId: UUID,
    val name: String,
    val type: String,
    val maxOccupancy: Int,
    val ownerId: UUID,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class AssignmentDetailResponse(
    val id: UUID,
    val planId: UUID,
    val name: String,
    val type: String,
    val maxOccupancy: Int,
    val ownerId: UUID,
    val members: List<AssignmentMemberResponse>,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class AssignmentMemberResponse(
    val assignmentId: UUID,
    val userId: UUID,
    val username: String?,
    val createdAt: Instant
)
