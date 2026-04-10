package com.acme.clients.activityladderclient.model

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class LadderActivity(
    val id: UUID,
    val ladderId: UUID,
    val name: String,
    val imageUrl: String,
    val distanceMinutes: Int,
    val costPerPerson: BigDecimal,
    val losses: Int,
    val bracket: LadderBracket,
    val displayOrder: Int,
    val createdAt: Instant,
)
