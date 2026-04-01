package com.acme.clients.itineraryclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ValidationError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.itineraryclient.api.AddEventParam
import org.slf4j.LoggerFactory

internal class ValidateAddEvent {
    private val logger = LoggerFactory.getLogger(ValidateAddEvent::class.java)

    fun execute(param: AddEventParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: AddEventParam): Result<Unit, AppError> {
        if (param.title.isBlank()) return failure(ValidationError("title", "must not be blank"))
        if (param.category !in VALID_CATEGORIES) return failure(ValidationError("category", "must be one of: $VALID_CATEGORIES_STRING"))
        if (param.estimatedCost != null && param.estimatedCost < java.math.BigDecimal.ZERO) return failure(ValidationError("estimatedCost", "must be >= 0"))
        if (param.eventEndAt != null && !param.eventEndAt.isAfter(param.eventAt)) return failure(ValidationError("eventEndAt", "must be after eventAt"))
        return success(Unit)
    }

    companion object {
        private val VALID_CATEGORIES = setOf("travel", "accommodation", "activity", "meal", "other")
        private val VALID_CATEGORIES_STRING = VALID_CATEGORIES.joinToString(", ")
    }
}
