package com.acme.clients.userclient.fake

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.userclient.api.*
import com.acme.clients.userclient.model.User

class FakeUserClient : UserClient {
    override fun getById(param: GetByIdParam): Result<User, AppError> =
        throw NotImplementedError("FakeUserClient.getById")

    override fun getByEmail(param: GetByEmailParam): Result<User, AppError> =
        throw NotImplementedError("FakeUserClient.getByEmail")

    override fun create(param: CreateUserParam): Result<User, AppError> =
        throw NotImplementedError("FakeUserClient.create")

    override fun getOrCreate(param: GetOrCreateUserParam): Result<User, AppError> =
        throw NotImplementedError("FakeUserClient.getOrCreate")

    override fun update(param: UpdateUserParam): Result<User, AppError> =
        throw NotImplementedError("FakeUserClient.update")
}
