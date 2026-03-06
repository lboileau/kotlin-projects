package com.acme.services.camperservice.features.user.actions

import com.acme.clients.common.Result
import com.acme.clients.userclient.api.UserClient
import com.acme.clients.userclient.api.UpdateUserParam as ClientUpdateUserParam
import com.acme.services.camperservice.features.user.error.UserError
import com.acme.services.camperservice.features.user.mapper.UserMapper
import com.acme.services.camperservice.features.user.model.User
import com.acme.services.camperservice.features.user.params.UpdateUserParam
import com.acme.services.camperservice.features.user.validations.ValidateUpdateUser
import org.slf4j.LoggerFactory

internal class UpdateUserAction(private val userClient: UserClient) {
    private val logger = LoggerFactory.getLogger(UpdateUserAction::class.java)
    private val validate = ValidateUpdateUser()

    fun execute(param: UpdateUserParam): Result<User, UserError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        if (param.userId != param.requestingUserId) {
            return Result.Failure(UserError.Forbidden(param.requestingUserId.toString()))
        }

        logger.debug("Updating user id={}", param.userId)
        return when (val result = userClient.update(ClientUpdateUserParam(id = param.userId, username = param.username))) {
            is Result.Success -> Result.Success(UserMapper.fromClient(result.value))
            is Result.Failure -> Result.Failure(UserError.fromClientError(result.error))
        }
    }
}
