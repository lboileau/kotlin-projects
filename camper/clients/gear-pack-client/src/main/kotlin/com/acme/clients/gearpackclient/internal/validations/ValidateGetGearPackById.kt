package com.acme.clients.gearpackclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.gearpackclient.api.GetGearPackByIdParam
import org.slf4j.LoggerFactory

internal class ValidateGetGearPackById {
    private val logger = LoggerFactory.getLogger(ValidateGetGearPackById::class.java)

    fun execute(param: GetGearPackByIdParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
