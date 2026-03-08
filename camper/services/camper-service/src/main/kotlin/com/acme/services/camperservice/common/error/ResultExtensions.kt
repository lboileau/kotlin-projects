package com.acme.services.camperservice.common.error

import com.acme.clients.common.Result
import com.acme.services.common.ApiResponse
import com.acme.services.camperservice.features.item.error.ItemError
import com.acme.services.camperservice.features.plan.error.PlanError
import com.acme.services.camperservice.features.user.error.UserError
import com.acme.services.camperservice.features.world.error.WorldError
import org.springframework.http.ResponseEntity

fun WorldError.toResponseEntity(): ResponseEntity<Any> = when (this) {
    is WorldError.NotFound -> ResponseEntity.status(404)
        .body(ApiResponse.ErrorBody("NOT_FOUND", message))
    is WorldError.AlreadyExists -> ResponseEntity.status(409)
        .body(ApiResponse.ErrorBody("CONFLICT", message))
    is WorldError.Invalid -> ResponseEntity.status(400)
        .body(ApiResponse.ErrorBody("BAD_REQUEST", message))
}

@JvmName("worldResultToResponseEntity")
fun <T> Result<T, WorldError>.toResponseEntity(
    successStatus: Int = 200,
    transform: (T) -> Any = { it as Any }
): ResponseEntity<Any> = when (this) {
    is Result.Success -> ResponseEntity.status(successStatus).body(transform(value))
    is Result.Failure -> error.toResponseEntity()
}

fun UserError.toResponseEntity(): ResponseEntity<Any> = when (this) {
    is UserError.NotFound -> ResponseEntity.status(404)
        .body(ApiResponse.ErrorBody("NOT_FOUND", message))
    is UserError.Invalid -> ResponseEntity.status(400)
        .body(ApiResponse.ErrorBody("BAD_REQUEST", message))
    is UserError.Forbidden -> ResponseEntity.status(403)
        .body(ApiResponse.ErrorBody("FORBIDDEN", message))
}

@JvmName("userResultToResponseEntity")
fun <T> Result<T, UserError>.toResponseEntity(
    successStatus: Int = 200,
    transform: (T) -> Any = { it as Any }
): ResponseEntity<Any> = when (this) {
    is Result.Success -> ResponseEntity.status(successStatus).body(transform(value))
    is Result.Failure -> error.toResponseEntity()
}

fun PlanError.toResponseEntity(): ResponseEntity<Any> = when (this) {
    is PlanError.NotFound -> ResponseEntity.status(404)
        .body(ApiResponse.ErrorBody("NOT_FOUND", message))
    is PlanError.NotOwner -> ResponseEntity.status(403)
        .body(ApiResponse.ErrorBody("FORBIDDEN", message))
    is PlanError.AlreadyMember -> ResponseEntity.status(409)
        .body(ApiResponse.ErrorBody("CONFLICT", message))
    is PlanError.NotMember -> ResponseEntity.status(404)
        .body(ApiResponse.ErrorBody("NOT_FOUND", message))
    is PlanError.Invalid -> ResponseEntity.status(400)
        .body(ApiResponse.ErrorBody("BAD_REQUEST", message))
}

@JvmName("planResultToResponseEntity")
fun <T> Result<T, PlanError>.toResponseEntity(
    successStatus: Int = 200,
    transform: (T) -> Any = { it as Any }
): ResponseEntity<Any> = when (this) {
    is Result.Success -> ResponseEntity.status(successStatus).body(transform(value))
    is Result.Failure -> error.toResponseEntity()
}

fun ItemError.toResponseEntity(): ResponseEntity<Any> = when (this) {
    is ItemError.NotFound -> ResponseEntity.status(404)
        .body(ApiResponse.ErrorBody("NOT_FOUND", message))
    is ItemError.Invalid -> ResponseEntity.status(400)
        .body(ApiResponse.ErrorBody("BAD_REQUEST", message))
}

@JvmName("itemResultToResponseEntity")
fun <T> Result<T, ItemError>.toResponseEntity(
    successStatus: Int = 200,
    transform: (T) -> Any = { it as Any }
): ResponseEntity<Any> = when (this) {
    is Result.Success -> ResponseEntity.status(successStatus).body(transform(value))
    is Result.Failure -> error.toResponseEntity()
}
