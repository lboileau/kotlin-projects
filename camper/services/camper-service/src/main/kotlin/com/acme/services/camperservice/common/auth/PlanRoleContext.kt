package com.acme.services.camperservice.common.auth

import java.util.UUID

/** Context returned on successful authorization — actions can inspect the resolved role if needed. */
data class PlanRoleContext(
    val planId: UUID,
    val userId: UUID,
    val role: PlanRole
)
