package com.acme.clients.itemclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.itemclient.api.GetByIdParam
import org.slf4j.LoggerFactory

internal class ValidateGetItemById {
    private val logger = LoggerFactory.getLogger(ValidateGetItemById::class.java)

    fun execute(param: GetByIdParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
