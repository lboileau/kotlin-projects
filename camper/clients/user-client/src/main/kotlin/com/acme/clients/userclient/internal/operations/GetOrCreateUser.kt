package com.acme.clients.userclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.userclient.api.CreateUserParam
import com.acme.clients.userclient.api.GetByEmailParam
import com.acme.clients.userclient.api.GetOrCreateUserParam
import com.acme.clients.userclient.model.User
import org.slf4j.LoggerFactory

internal class GetOrCreateUser(
    private val getUserByEmail: GetUserByEmail,
    private val createUser: CreateUser
) {
    private val logger = LoggerFactory.getLogger(GetOrCreateUser::class.java)

    fun execute(param: GetOrCreateUserParam): Result<User, AppError> {
        logger.debug("Get or create user email={}", param.email)
        val existing = getUserByEmail.execute(GetByEmailParam(param.email))
        if (existing is Result.Success) return existing

        return createUser.execute(CreateUserParam(email = param.email, username = param.username))
    }
}
