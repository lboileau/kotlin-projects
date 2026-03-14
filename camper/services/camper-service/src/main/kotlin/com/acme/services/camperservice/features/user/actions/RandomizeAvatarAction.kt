package com.acme.services.camperservice.features.user.actions

import com.acme.clients.common.Result
import com.acme.clients.userclient.api.UserClient
import com.acme.services.camperservice.features.user.error.UserError
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

        logger.debug("Randomizing avatar for user id={}", param.userId)
        TODO("RandomizeAvatarAction not yet implemented")
    }
}
