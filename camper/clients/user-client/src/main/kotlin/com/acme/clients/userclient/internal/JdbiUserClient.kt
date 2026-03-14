package com.acme.clients.userclient.internal

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.userclient.api.*
import com.acme.clients.userclient.internal.operations.*
import com.acme.clients.userclient.model.User
import org.jdbi.v3.core.Jdbi

internal class JdbiUserClient(jdbi: Jdbi) : UserClient {

    private val getUserById = GetUserById(jdbi)
    private val getUserByEmail = GetUserByEmail(jdbi)
    private val createUser = CreateUser(jdbi)
    private val getOrCreateUser = GetOrCreateUser(getUserByEmail, createUser)
    private val updateUser = UpdateUser(jdbi, getUserById)
    private val getDietaryRestrictions = GetDietaryRestrictions(jdbi)
    private val setDietaryRestrictions = SetDietaryRestrictions(jdbi)

    override fun getById(param: GetByIdParam): Result<User, AppError> = getUserById.execute(param)
    override fun getByEmail(param: GetByEmailParam): Result<User, AppError> = getUserByEmail.execute(param)
    override fun create(param: CreateUserParam): Result<User, AppError> = createUser.execute(param)
    override fun getOrCreate(param: GetOrCreateUserParam): Result<User, AppError> = getOrCreateUser.execute(param)
    override fun update(param: UpdateUserParam): Result<User, AppError> = updateUser.execute(param)
    override fun getDietaryRestrictions(param: GetDietaryRestrictionsParam): Result<List<String>, AppError> = getDietaryRestrictions.execute(param)
    override fun setDietaryRestrictions(param: SetDietaryRestrictionsParam): Result<List<String>, AppError> = setDietaryRestrictions.execute(param)
}
