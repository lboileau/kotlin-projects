package com.acme.services.camperservice.features.gearsync.error

import com.acme.clients.common.error.AppError

sealed class GearSyncError(override val message: String) : AppError {
    data class PlanNotFound(val planId: String) : GearSyncError("Plan not found: $planId")
}
