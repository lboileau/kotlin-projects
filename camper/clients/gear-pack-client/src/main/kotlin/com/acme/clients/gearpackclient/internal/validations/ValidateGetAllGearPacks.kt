package com.acme.clients.gearpackclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.gearpackclient.api.GetAllGearPacksParam
import org.slf4j.LoggerFactory

internal class ValidateGetAllGearPacks {
    private val logger = LoggerFactory.getLogger(ValidateGetAllGearPacks::class.java)

    fun execute(param: GetAllGearPacksParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
