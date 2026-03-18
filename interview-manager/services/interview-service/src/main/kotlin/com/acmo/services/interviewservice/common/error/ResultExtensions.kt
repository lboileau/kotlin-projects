package com.acmo.services.interviewservice.common.error

import com.acmo.clients.common.Result
import com.acmo.services.common.ApiResponse
import com.acmo.services.interviewservice.features.world.error.WorldError
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
