package com.acme.clients.common.error

data class ValidationError(
    val field: String,
    val reason: String,
    override val message: String = "Validation failed for $field: $reason"
) : AppError
