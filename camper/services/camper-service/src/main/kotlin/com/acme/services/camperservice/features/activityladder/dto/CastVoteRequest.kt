package com.acme.services.camperservice.features.activityladder.dto

import java.util.UUID

data class CastVoteRequest(
    val votedForActivityId: UUID,
)
