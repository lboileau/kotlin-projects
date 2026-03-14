package com.acme.clients.userclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.userclient.api.SetDietaryRestrictionsParam
import org.jdbi.v3.core.Jdbi

internal class SetDietaryRestrictions(private val jdbi: Jdbi) {

    fun execute(param: SetDietaryRestrictionsParam): Result<List<String>, AppError> {
        throw NotImplementedError("SetDietaryRestrictions not yet implemented")
    }
}
