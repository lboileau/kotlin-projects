package com.acme.services.camperservice.features.user.actions

import com.acme.clients.common.Result
import com.acme.clients.userclient.api.UserClient
import com.acme.services.camperservice.features.user.error.UserError
import com.acme.services.camperservice.features.user.model.User
import com.acme.services.camperservice.features.user.params.CreateUserParam
import com.acme.services.camperservice.features.user.validations.ValidateCreateUser

internal class CreateUserAction(private val userClient: UserClient) {
    private val validate = ValidateCreateUser()

    fun execute(param: CreateUserParam): Result<User, UserError> {
        TODO("Implementation in service-impl PR")
    }
}
