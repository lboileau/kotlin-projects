package com.acme.services.camperservice.features.user.actions

import com.acme.clients.common.Result
import com.acme.clients.userclient.api.GetByIdParam
import com.acme.clients.userclient.api.UserClient
import com.acme.libs.avatargenerator.AvatarGenerator
import com.acme.services.camperservice.features.user.dto.AvatarPreviewResponse
import com.acme.services.camperservice.features.user.error.UserError
import com.acme.services.camperservice.features.user.mapper.AvatarMapper
import com.acme.services.camperservice.features.user.params.RandomizeAvatarParam
import com.acme.services.camperservice.features.user.validations.ValidateRandomizeAvatar
import org.slf4j.LoggerFactory

internal class RandomizeAvatarAction(private val userClient: UserClient) {
    private val logger = LoggerFactory.getLogger(RandomizeAvatarAction::class.java)
    private val validate = ValidateRandomizeAvatar()

    fun execute(param: RandomizeAvatarParam): Result<AvatarPreviewResponse, UserError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        if (param.userId != param.requestingUserId) {
            return Result.Failure(UserError.Forbidden(param.requestingUserId.toString()))
        }

        val userResult = userClient.getById(GetByIdParam(param.userId))
        if (userResult is Result.Failure) {
            return Result.Failure(UserError.NotFound(param.userId.toString()))
        }

        logger.debug("Generating avatar preview for user id={}", param.userId)

        val newSeed = AvatarGenerator.randomSeed()
        val avatar = AvatarGenerator.generate(newSeed)
        return Result.Success(AvatarPreviewResponse(
            seed = newSeed,
            avatar = AvatarMapper.toResponse(avatar)
        ))
    }
}
