package com.acme.clients.userclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.userclient.api.GetByEmailParam
import com.acme.clients.common.EmailNormalizer
import com.acme.clients.userclient.internal.adapters.UserRowAdapter
import com.acme.clients.userclient.internal.validations.ValidateGetUserByEmail
import com.acme.clients.userclient.model.User
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetUserByEmail(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetUserByEmail::class.java)
    private val validate = ValidateGetUserByEmail()

    fun execute(param: GetByEmailParam): Result<User, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        val normalizedEmail = EmailNormalizer.normalize(param.email)
        logger.debug("Finding user by email={}", normalizedEmail)
        val entity = jdbi.withHandle<User?, Exception> { handle ->
            handle.createQuery("SELECT id, email, username, experience_level, avatar_seed, profile_completed, created_at, updated_at FROM users WHERE ${EmailNormalizer.SQL_EXPRESSION} = :email")
                .bind("email", normalizedEmail)
                .map { rs, _ -> UserRowAdapter.fromResultSet(rs) }
                .findOne()
                .orElse(null)
        }
        return if (entity != null) success(entity) else failure(NotFoundError("User", param.email))
    }
}
