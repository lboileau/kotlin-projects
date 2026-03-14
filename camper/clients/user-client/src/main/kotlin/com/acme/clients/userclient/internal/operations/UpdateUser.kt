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
                val setClauses = mutableListOf("username = :username", "updated_at = :updatedAt")
                if (param.experienceLevel != null) setClauses.add("experience_level = :experienceLevel")
                if (param.avatarSeed != null) setClauses.add("avatar_seed = :avatarSeed")
                if (param.profileCompleted == true) setClauses.add("profile_completed = true")

                val sql = "UPDATE users SET ${setClauses.joinToString(", ")} WHERE id = :id"

                jdbi.withHandle<Unit, Exception> { handle ->
                    val update = handle.createUpdate(sql)
                        .bind("id", param.id)
                        .bind("username", param.username)
                        .bind("updatedAt", now)

                    if (param.experienceLevel != null) update.bind("experienceLevel", param.experienceLevel)
                    if (param.avatarSeed != null) update.bind("avatarSeed", param.avatarSeed)

                    update.execute()
                }

                // Re-fetch to get enriched user with dietary restrictions
                when (val refreshed = getUserById.execute(GetByIdParam(param.id))) {
                    is Result.Success -> success(refreshed.value.copy(updatedAt = now))
                    is Result.Failure -> refreshed
                }
            }
        }
    }
}
