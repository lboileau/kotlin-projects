package com.acme.clients.userclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.userclient.api.SetDietaryRestrictionsParam
import com.acme.clients.userclient.internal.validations.ValidateSetDietaryRestrictions
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class SetDietaryRestrictions(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(SetDietaryRestrictions::class.java)
    private val validate = ValidateSetDietaryRestrictions()

    fun execute(param: SetDietaryRestrictionsParam): Result<List<String>, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Setting dietary restrictions for userId={}, count={}", param.userId, param.restrictions.size)
        jdbi.useTransaction<Exception> { handle ->
            handle.createUpdate("DELETE FROM user_dietary_restrictions WHERE user_id = :userId")
                .bind("userId", param.userId)
                .execute()

            param.restrictions.forEach { restriction ->
                handle.createUpdate(
                    """
                    INSERT INTO user_dietary_restrictions (user_id, restriction)
                    VALUES (:userId, :restriction)
                    """.trimIndent()
                )
                    .bind("userId", param.userId)
                    .bind("restriction", restriction)
                    .execute()
            }
        }
        return success(param.restrictions)
    }
}
