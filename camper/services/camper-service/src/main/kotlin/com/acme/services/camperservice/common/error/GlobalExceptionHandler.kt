package com.acme.services.camperservice.common.error

import com.acme.services.common.ApiResponse
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(RuntimeException::class)
    fun handleRuntimeException(ex: RuntimeException): ResponseEntity<ApiResponse.ErrorBody> {
        logger.error("Unexpected error", ex)
        return ResponseEntity.status(500)
            .body(ApiResponse.ErrorBody("INTERNAL_ERROR", "An unexpected error occurred"))
    }
}
