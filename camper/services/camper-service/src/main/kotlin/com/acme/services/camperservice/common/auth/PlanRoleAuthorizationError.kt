package com.acme.services.camperservice.common.auth

import java.util.UUID

/** Authorization failed. */
data class PlanRoleAuthorizationError(
    val planId: UUID,
    val userId: UUID,
    val requiredRoles: Set<PlanRole>,
    val actualRole: PlanRole?
)
