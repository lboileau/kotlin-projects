package com.example.clients.common

sealed class Result<out T, out E> {
    data class Success<T>(val value: T) : Result<T, Nothing>()
    data class Failure<E>(val error: E) : Result<Nothing, E>()

    fun <R> map(transform: (T) -> R): Result<R, E> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> Failure(error)
    }

    fun <R> flatMap(transform: (T) -> Result<R, @UnsafeVariance E>): Result<R, E> = when (this) {
        is Success -> transform(value)
        is Failure -> Failure(error)
    }

    fun getOrElse(default: () -> @UnsafeVariance T): T = when (this) {
        is Success -> value
        is Failure -> default()
    }

    fun getOrNull(): T? = when (this) {
        is Success -> value
        is Failure -> null
    }

    fun errorOrNull(): E? = when (this) {
        is Success -> null
        is Failure -> error
    }

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure
}

fun <T> success(value: T): Result<T, Nothing> = Result.Success(value)
fun <E> failure(error: E): Result<Nothing, E> = Result.Failure(error)
