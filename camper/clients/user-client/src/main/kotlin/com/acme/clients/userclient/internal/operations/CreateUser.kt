package com.acme.clients.userclient.internal.operations

import com.acme.clients.common.EmailNormalizer
import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ConflictError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.userclient.api.CreateUserParam
import com.acme.clients.userclient.internal.validations.ValidateCreateUser
import com.acme.clients.userclient.model.User
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

internal class CreateUser(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(CreateUser::class.java)
    private val validate = ValidateCreateUser()

    fun execute(param: CreateUserParam): Result<User, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        val normalizedEmail = EmailNormalizer.normalize(param.email)
        logger.debug("Creating user email={}", normalizedEmail)
        return try {
            val entity = jdbi.withHandle<User, Exception> { handle ->
                val id = UUID.randomUUID()
                val now = Instant.now()
                handle.createUpdate(
                    """
                    INSERT INTO users (id, email, username, created_at, updated_at)
                    VALUES (:id, :email, :username, :createdAt, :updatedAt)
                    """.trimIndent()
                )
                    .bind("id", id)
                    .bind("email", normalizedEmail)
                    .bind("username", param.username)
                    .bind("createdAt", now)
                    .bind("updatedAt", now)
                    .execute()
                User(id = id, email = normalizedEmail, username = param.username, createdAt = now, updatedAt = now)
            }
            success(entity)
        } catch (e: Exception) {
            if (e.message?.contains("uq_users_email") == true || e.message?.contains("duplicate key") == true) {
                failure(ConflictError("User", "email '${normalizedEmail}' already exists"))
            } else {
                throw e
            }
        }
    }
}
