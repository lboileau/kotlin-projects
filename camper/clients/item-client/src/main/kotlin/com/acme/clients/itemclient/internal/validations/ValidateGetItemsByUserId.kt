package com.acme.clients.itemclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.itemclient.api.GetByUserIdParam
import org.slf4j.LoggerFactory

internal class ValidateGetItemsByUserId {
    private val logger = LoggerFactory.getLogger(ValidateGetItemsByUserId::class.java)

    fun execute(param: GetByUserIdParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
