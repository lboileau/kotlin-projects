package com.acme.clients.assignmentclient.internal.validations

import com.acme.clients.assignmentclient.api.TransferOwnershipParam
import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import org.slf4j.LoggerFactory

internal class ValidateTransferOwnership {
    private val logger = LoggerFactory.getLogger(ValidateTransferOwnership::class.java)

    fun execute(param: TransferOwnershipParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
