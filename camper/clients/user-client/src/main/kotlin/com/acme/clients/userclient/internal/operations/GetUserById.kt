package com.acme.clients.userclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.userclient.api.GetByIdParam
import com.acme.clients.userclient.internal.adapters.UserRowAdapter
import com.acme.clients.userclient.internal.validations.ValidateGetUserById
import com.acme.clients.userclient.model.User
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetUserById(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetUserById::class.java)
    private val validate = ValidateGetUserById()

    fun execute(param: GetByIdParam): Result<User, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Finding user by id={}", param.id)
        val entity = jdbi.withHandle<User?, Exception> { handle ->
            val user = handle.createQuery("SELECT id, email, username, experience_level, avatar_seed, profile_completed, created_at, updated_at FROM users WHERE id = :id")
                .bind("id", param.id)
                .map { rs, _ -> UserRowAdapter.fromResultSet(rs) }
                .findOne()
                .orElse(null)

            if (user != null) {
                val restrictions = handle.createQuery("SELECT restriction FROM user_dietary_restrictions WHERE user_id = :userId ORDER BY restriction")
                    .bind("userId", user.id)
                    .mapTo(String::class.java)
                    .list()
                user.copy(dietaryRestrictions = restrictions)
            } else {
                null
            }
        }
        return if (entity != null) success(entity) else failure(NotFoundError("User", param.id.toString()))
    }
}
