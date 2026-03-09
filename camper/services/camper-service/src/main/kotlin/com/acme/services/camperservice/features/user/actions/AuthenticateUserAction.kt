package com.acme.services.camperservice.features.user.actions

import com.acme.clients.common.Result
import com.acme.clients.userclient.api.GetByEmailParam
import com.acme.clients.userclient.api.UserClient
import com.acme.services.camperservice.features.user.error.UserError
import com.acme.services.camperservice.features.user.mapper.UserMapper
import com.acme.services.camperservice.features.user.model.User
import com.acme.services.camperservice.features.user.params.AuthenticateUserParam
import com.acme.services.camperservice.features.user.validations.ValidateAuthenticateUser
import org.slf4j.LoggerFactory

internal class AuthenticateUserAction(private val userClient: UserClient) {
    private val logger = LoggerFactory.getLogger(AuthenticateUserAction::class.java)
    private val validate = ValidateAuthenticateUser()

    fun execute(param: AuthenticateUserParam): Result<User, UserError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Authenticating user email={}", param.email)
        return when (val result = userClient.getByEmail(GetByEmailParam(param.email))) {
            is Result.Success -> {
                val user = UserMapper.fromClient(result.value)
                if (user.username == null) {
                    Result.Failure(UserError.RegistrationRequired(param.email))
                } else {
                    Result.Success(user)
                }
            }
            is Result.Failure -> Result.Failure(UserError.fromClientError(result.error))
        }
    }
}
