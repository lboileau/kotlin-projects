package com.acme.clients.planclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.planclient.api.AddMemberParam
import org.slf4j.LoggerFactory

internal class ValidateAddMember {
    private val logger = LoggerFactory.getLogger(ValidateAddMember::class.java)

    fun execute(param: AddMemberParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
