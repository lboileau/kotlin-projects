package com.acme.services.camperservice.features.assignment.model

import java.time.Instant
import java.util.UUID

data class Assignment(
    val id: UUID,
    val planId: UUID,
    val name: String,
    val type: String,
    val maxOccupancy: Int,
    val ownerId: UUID,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class AssignmentMember(
    val assignmentId: UUID,
    val userId: UUID,
    val username: String?,
    val createdAt: Instant
)

data class AssignmentDetail(
    val id: UUID,
    val planId: UUID,
    val name: String,
    val type: String,
    val maxOccupancy: Int,
    val ownerId: UUID,
    val members: List<AssignmentMember>,
    val createdAt: Instant,
    val updatedAt: Instant
)
