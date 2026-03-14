package com.acme.clients.userclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.userclient.api.GetDietaryRestrictionsParam
import org.jdbi.v3.core.Jdbi

internal class GetDietaryRestrictions(private val jdbi: Jdbi) {

    fun execute(param: GetDietaryRestrictionsParam): Result<List<String>, AppError> {
        throw NotImplementedError("GetDietaryRestrictions not yet implemented")
    }
}
