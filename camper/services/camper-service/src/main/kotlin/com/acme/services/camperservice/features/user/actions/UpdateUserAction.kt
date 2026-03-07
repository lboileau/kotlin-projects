package com.acme.services.camperservice.features.user.actions

import com.acme.clients.common.Result
import com.acme.clients.userclient.api.UserClient
import com.acme.services.camperservice.features.user.error.UserError
import com.acme.services.camperservice.features.user.model.User
import com.acme.services.camperservice.features.user.params.UpdateUserParam
import com.acme.services.camperservice.features.user.validations.ValidateUpdateUser

internal class UpdateUserAction(private val userClient: UserClient) {
    private val validate = ValidateUpdateUser()

    fun execute(param: UpdateUserParam): Result<User, UserError> {
        TODO("Implementation in service-impl PR")
    }
}
