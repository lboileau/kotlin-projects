package com.acme.services.camperservice.features.user.actions

import com.acme.clients.common.Result
import com.acme.clients.userclient.api.GetByIdParam
import com.acme.clients.userclient.api.UserClient
import com.acme.libs.avatargenerator.AvatarGenerator
import com.acme.services.camperservice.features.user.dto.AvatarResponse
import com.acme.services.camperservice.features.user.error.UserError
import com.acme.services.camperservice.features.user.mapper.AvatarMapper
import com.acme.services.camperservice.features.user.params.GetAvatarParam
import com.acme.services.camperservice.features.user.validations.ValidateGetAvatar
import org.slf4j.LoggerFactory

internal class GetAvatarAction(private val userClient: UserClient) {
    private val logger = LoggerFactory.getLogger(GetAvatarAction::class.java)
    private val validate = ValidateGetAvatar()

    fun execute(param: GetAvatarParam): Result<AvatarResponse, UserError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Getting avatar for user id={}", param.userId)
        val user = when (val result = userClient.getById(GetByIdParam(param.userId))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(UserError.fromClientError(result.error))
        }

        val avatarSeed = user.avatarSeed
            ?: return Result.Failure(UserError.Invalid("avatarSeed", "user has no avatar seed"))

        val avatar = AvatarGenerator.generate(avatarSeed)
        return Result.Success(AvatarMapper.toResponse(avatar))
    }
}
