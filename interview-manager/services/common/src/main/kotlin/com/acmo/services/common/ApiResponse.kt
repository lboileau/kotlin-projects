package com.acmo.services.common

data class ApiResponse<T>(
    val status: Int,
    val body: T? = null,
    val error: ErrorBody? = null
) {
    data class ErrorBody(
        val code: String,
        val message: String,
        val details: Map<String, Any>? = null
    )

    companion object {
        fun <T> ok(body: T) = ApiResponse(status = 200, body = body)
        fun <T> created(body: T) = ApiResponse(status = 201, body = body)
        fun noContent() = ApiResponse<Unit>(status = 204)
        fun notFound(message: String) = ApiResponse<Nothing>(status = 404, error = ErrorBody("NOT_FOUND", message))
        fun conflict(message: String) = ApiResponse<Nothing>(status = 409, error = ErrorBody("CONFLICT", message))
        fun badRequest(message: String) = ApiResponse<Nothing>(status = 400, error = ErrorBody("BAD_REQUEST", message))
        fun validationError(message: String, details: Map<String, Any>) =
            ApiResponse<Nothing>(status = 422, error = ErrorBody("VALIDATION_ERROR", message, details))
        fun internalError(message: String) = ApiResponse<Nothing>(status = 500, error = ErrorBody("INTERNAL_ERROR", message))
    }
}
