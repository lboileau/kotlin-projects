package com.acme.services.camperservice.features.user.actions

import com.acme.clients.common.Result
import com.acme.clients.userclient.api.UserClient
import com.acme.clients.userclient.api.GetByIdParam
import com.acme.services.camperservice.features.user.error.UserError
import com.acme.services.camperservice.features.user.mapper.UserMapper
import com.acme.services.camperservice.features.user.model.User
import com.acme.services.camperservice.features.user.params.GetUserByIdParam
import com.acme.services.camperservice.features.user.validations.ValidateGetUserById
import org.slf4j.LoggerFactory

internal class GetUserByIdAction(private val userClient: UserClient) {
    private val logger = LoggerFactory.getLogger(GetUserByIdAction::class.java)
    private val validate = ValidateGetUserById()

    fun execute(param: GetUserByIdParam): Result<User, UserError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Getting user by id={}", param.userId)
        return when (val result = userClient.getById(GetByIdParam(param.userId))) {
            is Result.Success -> Result.Success(UserMapper.fromClient(result.value))
            is Result.Failure -> Result.Failure(UserError.fromClientError(result.error))
        }
    }
}
