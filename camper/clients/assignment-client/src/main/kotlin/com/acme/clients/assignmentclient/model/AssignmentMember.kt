package com.acme.clients.assignmentclient.model

import java.time.Instant
import java.util.UUID

data class AssignmentMember(
    val assignmentId: UUID,
    val userId: UUID,
    val planId: UUID,
    val assignmentType: String,
    val createdAt: Instant
)
