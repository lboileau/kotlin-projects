package com.acme.clients.itemclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.itemclient.api.GetByPlanIdParam
import org.slf4j.LoggerFactory

internal class ValidateGetItemsByPlanId {
    private val logger = LoggerFactory.getLogger(ValidateGetItemsByPlanId::class.java)

    fun execute(param: GetByPlanIdParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
