package com.acme.services.camperservice.features.user.actions

import com.acme.clients.common.Result
import com.acme.clients.userclient.api.UserClient
import com.acme.services.camperservice.features.user.error.UserError
import com.acme.services.camperservice.features.user.model.User
import com.acme.services.camperservice.features.user.params.AuthenticateUserParam
import com.acme.services.camperservice.features.user.validations.ValidateAuthenticateUser

internal class AuthenticateUserAction(private val userClient: UserClient) {
    private val validate = ValidateAuthenticateUser()

    fun execute(param: AuthenticateUserParam): Result<User, UserError> {
        TODO("Implementation in service-impl PR")
    }
}
