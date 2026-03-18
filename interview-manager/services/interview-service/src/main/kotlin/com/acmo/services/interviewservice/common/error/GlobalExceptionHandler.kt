package com.acmo.services.interviewservice.common.error

import com.acmo.services.common.ApiResponse
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException): ResponseEntity<ApiResponse.ErrorBody> {
        logger.warn("Type mismatch: {}", ex.message)
        return ResponseEntity.status(400)
            .body(ApiResponse.ErrorBody("BAD_REQUEST", "Invalid value for parameter '${ex.name}': ${ex.value}"))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleMessageNotReadable(ex: HttpMessageNotReadableException): ResponseEntity<ApiResponse.ErrorBody> {
        logger.warn("Malformed request body: {}", ex.message)
        return ResponseEntity.status(400)
            .body(ApiResponse.ErrorBody("BAD_REQUEST", "Malformed or missing required fields in request body"))
    }

    @ExceptionHandler(RuntimeException::class)
    fun handleRuntimeException(ex: RuntimeException): ResponseEntity<ApiResponse.ErrorBody> {
        logger.error("Unexpected error", ex)
        return ResponseEntity.status(500)
            .body(ApiResponse.ErrorBody("INTERNAL_ERROR", "An unexpected error occurred"))
    }
}
