package com.acme.clients.userclient.fake

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ConflictError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.userclient.api.*
import com.acme.clients.common.EmailNormalizer
import com.acme.clients.userclient.internal.validations.ValidateCreateUser
import com.acme.clients.userclient.internal.validations.ValidateGetUserByEmail
import com.acme.clients.userclient.internal.validations.ValidateGetUserById
import com.acme.clients.userclient.internal.validations.ValidateGetDietaryRestrictions
import com.acme.clients.userclient.internal.validations.ValidateSetDietaryRestrictions
import com.acme.clients.userclient.internal.validations.ValidateUpdateUser
import com.acme.clients.userclient.model.User
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class FakeUserClient : UserClient {
    private val store = ConcurrentHashMap<UUID, User>()
    private val dietaryRestrictionsStore = ConcurrentHashMap<UUID, List<String>>()

    private val validateGetById = ValidateGetUserById()
    private val validateGetByEmail = ValidateGetUserByEmail()
    private val validateCreate = ValidateCreateUser()
    private val validateUpdate = ValidateUpdateUser()
    private val validateGetDietaryRestrictions = ValidateGetDietaryRestrictions()
    private val validateSetDietaryRestrictions = ValidateSetDietaryRestrictions()

    override fun getById(param: GetByIdParam): Result<User, AppError> {
        val validation = validateGetById.execute(param)
        if (validation is Result.Failure) return validation

        val entity = store[param.id]
        return if (entity != null) {
            success(entity.copy(dietaryRestrictions = dietaryRestrictionsStore[param.id] ?: emptyList()))
        } else {
            failure(NotFoundError("User", param.id.toString()))
        }
    }

    override fun getByEmail(param: GetByEmailParam): Result<User, AppError> {
        val validation = validateGetByEmail.execute(param)
        if (validation is Result.Failure) return validation

        val normalizedEmail = EmailNormalizer.normalize(param.email)
        val entity = store.values.find { EmailNormalizer.normalize(it.email) == normalizedEmail }
        return if (entity != null) {
            success(entity.copy(dietaryRestrictions = dietaryRestrictionsStore[entity.id] ?: emptyList()))
        } else {
            failure(NotFoundError("User", param.email))
        }
    }

    override fun create(param: CreateUserParam): Result<User, AppError> {
        val validation = validateCreate.execute(param)
        if (validation is Result.Failure) return validation

        val normalizedEmail = EmailNormalizer.normalize(param.email)
        if (store.values.any { EmailNormalizer.normalize(it.email) == normalizedEmail }) {
            return failure(ConflictError("User", "email '${normalizedEmail}' already exists"))
        }
        val entity = User(
            id = UUID.randomUUID(),
            email = normalizedEmail,
            username = param.username,
            experienceLevel = null,
            avatarSeed = param.avatarSeed,
            profileCompleted = false,
            dietaryRestrictions = emptyList(),
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        store[entity.id] = entity
        return success(entity)
    }

    override fun getOrCreate(param: GetOrCreateUserParam): Result<User, AppError> {
        val normalizedEmail = EmailNormalizer.normalize(param.email)
        val existing = store.values.find { EmailNormalizer.normalize(it.email) == normalizedEmail }
        if (existing != null) return success(existing)

        return create(CreateUserParam(email = param.email, username = param.username))
    }

    override fun update(param: UpdateUserParam): Result<User, AppError> {
        val validation = validateUpdate.execute(param)
        if (validation is Result.Failure) return validation

        val existing = store[param.id] ?: return failure(NotFoundError("User", param.id.toString()))
        val updated = existing.copy(
            username = param.username,
            experienceLevel = param.experienceLevel ?: existing.experienceLevel,
            avatarSeed = param.avatarSeed ?: existing.avatarSeed,
            profileCompleted = if (param.profileCompleted == true) true else existing.profileCompleted,
            updatedAt = Instant.now()
        )
        store[param.id] = updated
        return success(updated.copy(dietaryRestrictions = dietaryRestrictionsStore[param.id] ?: emptyList()))
    }

    override fun getDietaryRestrictions(param: GetDietaryRestrictionsParam): Result<List<String>, AppError> {
        val validation = validateGetDietaryRestrictions.execute(param)
        if (validation is Result.Failure) return validation

        if (!store.containsKey(param.userId)) return failure(NotFoundError("User", param.userId.toString()))
        return success(dietaryRestrictionsStore[param.userId] ?: emptyList())
    }

    override fun setDietaryRestrictions(param: SetDietaryRestrictionsParam): Result<List<String>, AppError> {
        val validation = validateSetDietaryRestrictions.execute(param)
        if (validation is Result.Failure) return validation

        if (!store.containsKey(param.userId)) return failure(NotFoundError("User", param.userId.toString()))
        dietaryRestrictionsStore[param.userId] = param.restrictions
        return success(param.restrictions)
    }

    fun reset() {
        store.clear()
        dietaryRestrictionsStore.clear()
    }

    fun seed(vararg entities: User) {
        entities.forEach {
            store[it.id] = it
            if (it.dietaryRestrictions.isNotEmpty()) {
                dietaryRestrictionsStore[it.id] = it.dietaryRestrictions
            }
        }
    }
}
