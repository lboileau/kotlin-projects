package com.acme.clients.userclient.api

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.userclient.model.User

/**
 * Client interface for User entity operations.
 *
 * All operations return [Result] to represent success or typed failure
 * without throwing exceptions for expected error conditions.
 */
interface UserClient {
    /** Retrieve a user by their unique identifier. */
    fun getById(param: GetByIdParam): Result<User, AppError>

    /** Retrieve a user by their email address. */
    fun getByEmail(param: GetByEmailParam): Result<User, AppError>

    /** Create a new user with the given email and username. */
    fun create(param: CreateUserParam): Result<User, AppError>

    /** Get an existing user by email, or create one if not found. */
    fun getOrCreate(param: GetOrCreateUserParam): Result<User, AppError>

    /** Update a user's username. */
    fun update(param: UpdateUserParam): Result<User, AppError>
}
