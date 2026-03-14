package com.acme.services.camperservice.features.user.actions

import com.acme.clients.common.Result
import com.acme.clients.userclient.api.GetByEmailParam
import com.acme.clients.userclient.api.UserClient
import com.acme.clients.userclient.api.UpdateUserParam as ClientUpdateUserParam
import com.acme.clients.userclient.api.CreateUserParam as ClientCreateUserParam
import com.acme.libs.avatargenerator.AvatarGenerator
import com.acme.services.camperservice.features.user.error.UserError
import com.acme.services.camperservice.features.user.mapper.UserMapper
import com.acme.services.camperservice.features.user.model.User
import com.acme.services.camperservice.features.user.params.CreateUserParam
import com.acme.services.camperservice.features.user.validations.ValidateCreateUser
import org.slf4j.LoggerFactory

internal class CreateUserAction(private val userClient: UserClient) {
    private val logger = LoggerFactory.getLogger(CreateUserAction::class.java)
    private val validate = ValidateCreateUser()

    fun execute(param: CreateUserParam): Result<User, UserError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        // Idempotent: if email exists, return existing user (updating username if missing)
        val existing = userClient.getByEmail(GetByEmailParam(param.email))
        if (existing is Result.Success) {
            val existingUser = existing.value
            if (existingUser.username == null && param.username != null) {
                logger.debug("Updating username for existing user email={}", param.email)
                val avatarSeed = if (existingUser.avatarSeed == null) AvatarGenerator.seedFromName(param.username) else null
                val updated = userClient.update(ClientUpdateUserParam(id = existingUser.id, username = param.username, avatarSeed = avatarSeed))
                if (updated is Result.Success) return Result.Success(UserMapper.fromClient(updated.value))
            }
            logger.debug("User already exists for email={}, returning existing", param.email)
            return Result.Success(UserMapper.fromClient(existingUser))
        }

        logger.debug("Creating user email={}", param.email)
        val avatarSeed = AvatarGenerator.seedFromName(param.username ?: param.email)
        return when (val result = userClient.create(ClientCreateUserParam(email = param.email, username = param.username, avatarSeed = avatarSeed))) {
            is Result.Success -> Result.Success(UserMapper.fromClient(result.value))
            is Result.Failure -> Result.Failure(UserError.fromClientError(result.error))
        }
    }
}
