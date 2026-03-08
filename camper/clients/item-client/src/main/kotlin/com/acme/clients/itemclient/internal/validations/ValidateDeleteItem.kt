package com.acme.clients.itemclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.itemclient.api.DeleteItemParam
import org.slf4j.LoggerFactory

internal class ValidateDeleteItem {
    private val logger = LoggerFactory.getLogger(ValidateDeleteItem::class.java)

    fun execute(param: DeleteItemParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
