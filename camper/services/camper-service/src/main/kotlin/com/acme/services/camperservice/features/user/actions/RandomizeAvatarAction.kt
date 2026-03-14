package com.acme.services.camperservice.features.user.actions

import com.acme.clients.common.Result
import com.acme.clients.userclient.api.GetByIdParam
import com.acme.clients.userclient.api.UserClient
import com.acme.clients.userclient.api.UpdateUserParam as ClientUpdateUserParam
import com.acme.libs.avatargenerator.AvatarGenerator
import com.acme.services.camperservice.features.user.error.UserError
import com.acme.services.camperservice.features.user.mapper.UserMapper
import com.acme.services.camperservice.features.user.model.User
import com.acme.services.camperservice.features.user.params.RandomizeAvatarParam
import com.acme.services.camperservice.features.user.validations.ValidateRandomizeAvatar
import org.slf4j.LoggerFactory

internal class RandomizeAvatarAction(private val userClient: UserClient) {
    private val logger = LoggerFactory.getLogger(RandomizeAvatarAction::class.java)
    private val validate = ValidateRandomizeAvatar()

    fun execute(param: RandomizeAvatarParam): Result<User, UserError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        if (param.userId != param.requestingUserId) {
            return Result.Failure(UserError.Forbidden(param.requestingUserId.toString()))
        }

        logger.debug("Randomizing avatar for user id={}", param.userId)

        // Fetch current user to get username for the update call
        val currentUser = when (val result = userClient.getById(GetByIdParam(param.userId))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(UserError.fromClientError(result.error))
        }

        val newSeed = AvatarGenerator.randomSeed()
        return when (val result = userClient.update(ClientUpdateUserParam(
            id = param.userId,
            username = currentUser.username ?: "",
            avatarSeed = newSeed
        ))) {
            is Result.Success -> Result.Success(UserMapper.fromClient(result.value))
            is Result.Failure -> Result.Failure(UserError.fromClientError(result.error))
        }
    }
}
