package com.acme.clients.itemclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.itemclient.api.GetByPlanIdAndUserIdParam
import org.slf4j.LoggerFactory

internal class ValidateGetItemsByPlanIdAndUserId {
    private val logger = LoggerFactory.getLogger(ValidateGetItemsByPlanIdAndUserId::class.java)

    fun execute(param: GetByPlanIdAndUserIdParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
