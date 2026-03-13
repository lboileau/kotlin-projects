package com.acme.clients.planclient.model

import java.time.Instant
import java.util.UUID

data class PlanMember(
    val planId: UUID,
    val userId: UUID,
    val role: String,
    val createdAt: Instant
)
