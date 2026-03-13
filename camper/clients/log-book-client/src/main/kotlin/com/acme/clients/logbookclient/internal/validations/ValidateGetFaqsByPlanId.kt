package com.acme.clients.logbookclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.logbookclient.api.GetFaqsByPlanIdParam

internal class ValidateGetFaqsByPlanId {
    fun execute(param: GetFaqsByPlanIdParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
