package com.acme.services.camperservice.features.activityladder.dto

import java.util.UUID

data class LadderParticipantResponse(
    val userId: UUID,
    val username: String?,
    val avatarSeed: String?,
)
