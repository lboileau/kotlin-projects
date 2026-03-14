package com.acme.clients.userclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.userclient.api.GetDietaryRestrictionsParam
import com.acme.clients.userclient.internal.validations.ValidateGetDietaryRestrictions
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetDietaryRestrictions(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetDietaryRestrictions::class.java)
    private val validate = ValidateGetDietaryRestrictions()

    fun execute(param: GetDietaryRestrictionsParam): Result<List<String>, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Getting dietary restrictions for userId={}", param.userId)
        val restrictions = jdbi.withHandle<List<String>, Exception> { handle ->
            handle.createQuery("SELECT restriction FROM user_dietary_restrictions WHERE user_id = :userId ORDER BY restriction")
                .bind("userId", param.userId)
                .mapTo(String::class.java)
                .list()
        }
        return success(restrictions)
    }
}
