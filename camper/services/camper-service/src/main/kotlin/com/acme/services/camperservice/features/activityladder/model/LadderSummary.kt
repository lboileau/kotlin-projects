package com.acme.services.camperservice.features.activityladder.model

import java.time.Instant
import java.util.UUID

data class LadderSummary(
    val ladder: Ladder,
    val activityCount: Int,
    val participantCount: Int,
)
