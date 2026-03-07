package com.acme.clients.planclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.planclient.api.RemoveMemberParam
import org.slf4j.LoggerFactory

internal class ValidateRemoveMember {
    private val logger = LoggerFactory.getLogger(ValidateRemoveMember::class.java)

    fun execute(param: RemoveMemberParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
