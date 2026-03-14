package com.acme.clients.userclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.userclient.api.GetByIdParam
import com.acme.clients.userclient.api.UpdateUserParam
import com.acme.clients.userclient.internal.validations.ValidateUpdateUser
import com.acme.clients.userclient.model.User
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant

internal class UpdateUser(
    private val jdbi: Jdbi,
    private val getUserById: GetUserById
) {
    private val logger = LoggerFactory.getLogger(UpdateUser::class.java)
    private val validate = ValidateUpdateUser()

    fun execute(param: UpdateUserParam): Result<User, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Updating user id={}", param.id)
        return when (val existing = getUserById.execute(GetByIdParam(param.id))) {
            is Result.Failure -> existing
            is Result.Success -> {
                val now = Instant.now()
                jdbi.withHandle<Unit, Exception> { handle ->
                    handle.createUpdate(
                        """
                        UPDATE users SET username = :username, updated_at = :updatedAt
                        WHERE id = :id
                        """.trimIndent()
                    )
                        .bind("id", param.id)
                        .bind("username", param.username)
                        .bind("updatedAt", now)
                        .execute()
                }
                success(
                    existing.value.copy(
                        username = param.username,
                        experienceLevel = param.experienceLevel ?: existing.value.experienceLevel,
                        avatarSeed = param.avatarSeed ?: existing.value.avatarSeed,
                        profileCompleted = if (param.profileCompleted == true) true else existing.value.profileCompleted,
                        updatedAt = now
                    )
                )
            }
        }
    }
}
