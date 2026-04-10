package com.acme.clients.activityladderclient.model

import java.time.Instant
import java.util.UUID

data class LadderParticipant(
    val id: UUID,
    val ladderId: UUID,
    val userId: UUID,
    val createdAt: Instant,
)
