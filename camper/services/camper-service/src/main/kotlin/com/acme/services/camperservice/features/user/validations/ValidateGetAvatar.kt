package com.acme.services.camperservice.features.user.validations

import com.acme.clients.common.Result
import com.acme.clients.common.success
import com.acme.services.camperservice.features.user.error.UserError
import com.acme.services.camperservice.features.user.params.GetAvatarParam

internal class ValidateGetAvatar {
    fun execute(param: GetAvatarParam): Result<Unit, UserError> = success(Unit)
}
