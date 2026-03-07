package com.acme.services.camperservice.features.user.actions

import com.acme.clients.common.Result
import com.acme.clients.userclient.api.UserClient
import com.acme.services.camperservice.features.user.error.UserError
import com.acme.services.camperservice.features.user.model.User
import com.acme.services.camperservice.features.user.params.GetUserByIdParam
import com.acme.services.camperservice.features.user.validations.ValidateGetUserById

internal class GetUserByIdAction(private val userClient: UserClient) {
    private val validate = ValidateGetUserById()

    fun execute(param: GetUserByIdParam): Result<User, UserError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        return Result.Failure(UserError.NotFound(param.userId.toString()))
    }
}
