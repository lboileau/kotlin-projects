package com.acme.clients.userclient.fake

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ConflictError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.userclient.api.*
import com.acme.clients.userclient.internal.validations.ValidateCreateUser
import com.acme.clients.userclient.internal.validations.ValidateGetUserByEmail
import com.acme.clients.userclient.internal.validations.ValidateGetUserById
import com.acme.clients.userclient.internal.validations.ValidateUpdateUser
import com.acme.clients.userclient.model.User
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class FakeUserClient : UserClient {
    private val store = ConcurrentHashMap<UUID, User>()

    private val validateGetById = ValidateGetUserById()
    private val validateGetByEmail = ValidateGetUserByEmail()
    private val validateCreate = ValidateCreateUser()
    private val validateUpdate = ValidateUpdateUser()

    override fun getById(param: GetByIdParam): Result<User, AppError> {
        val validation = validateGetById.execute(param)
        if (validation is Result.Failure) return validation

        val entity = store[param.id]
        return if (entity != null) success(entity) else failure(NotFoundError("User", param.id.toString()))
    }

    override fun getByEmail(param: GetByEmailParam): Result<User, AppError> {
        val validation = validateGetByEmail.execute(param)
        if (validation is Result.Failure) return validation

        val entity = store.values.find { it.email == param.email }
        return if (entity != null) success(entity) else failure(NotFoundError("User", param.email))
    }

    override fun create(param: CreateUserParam): Result<User, AppError> {
        val validation = validateCreate.execute(param)
        if (validation is Result.Failure) return validation

        if (store.values.any { it.email == param.email }) {
            return failure(ConflictError("User", "email '${param.email}' already exists"))
        }
        val entity = User(
            id = UUID.randomUUID(),
            email = param.email,
            username = param.username,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        store[entity.id] = entity
        return success(entity)
    }

    override fun getOrCreate(param: GetOrCreateUserParam): Result<User, AppError> {
        val existing = store.values.find { it.email == param.email }
        if (existing != null) return success(existing)

        return create(CreateUserParam(email = param.email, username = param.username))
    }

    override fun update(param: UpdateUserParam): Result<User, AppError> {
        val validation = validateUpdate.execute(param)
        if (validation is Result.Failure) return validation

        val existing = store[param.id] ?: return failure(NotFoundError("User", param.id.toString()))
        val updated = existing.copy(username = param.username, updatedAt = Instant.now())
        store[param.id] = updated
        return success(updated)
    }

    fun reset() = store.clear()

    fun seed(vararg entities: User) {
        entities.forEach { store[it.id] = it }
    }
}
