package com.acme.clients.planclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.planclient.api.GetByIdParam
import org.slf4j.LoggerFactory

internal class ValidateGetPlanById {
    private val logger = LoggerFactory.getLogger(ValidateGetPlanById::class.java)

    fun execute(param: GetByIdParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
