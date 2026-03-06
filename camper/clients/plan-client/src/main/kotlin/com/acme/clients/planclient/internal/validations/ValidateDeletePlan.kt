package com.acme.clients.planclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.planclient.api.DeletePlanParam
import org.slf4j.LoggerFactory

internal class ValidateDeletePlan {
    private val logger = LoggerFactory.getLogger(ValidateDeletePlan::class.java)

    fun execute(param: DeletePlanParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
