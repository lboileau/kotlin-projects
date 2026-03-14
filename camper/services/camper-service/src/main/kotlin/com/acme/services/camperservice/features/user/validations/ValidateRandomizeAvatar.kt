package com.acme.services.camperservice.features.user.validations

import com.acme.clients.common.Result
import com.acme.clients.common.success
import com.acme.services.camperservice.features.user.error.UserError
import com.acme.services.camperservice.features.user.params.RandomizeAvatarParam

internal class ValidateRandomizeAvatar {
    fun execute(param: RandomizeAvatarParam): Result<Unit, UserError> = success(Unit)
}
