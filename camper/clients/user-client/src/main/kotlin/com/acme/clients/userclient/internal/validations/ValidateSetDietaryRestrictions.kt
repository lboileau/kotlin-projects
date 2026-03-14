package com.acme.clients.userclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ValidationError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.userclient.api.SetDietaryRestrictionsParam
import org.slf4j.LoggerFactory

internal class ValidateSetDietaryRestrictions {
    private val logger = LoggerFactory.getLogger(ValidateSetDietaryRestrictions::class.java)

    companion object {
        val ALLOWED_RESTRICTIONS = setOf(
            "gluten_free", "nut_allergy", "vegetarian", "vegan",
            "lactose_intolerant", "shellfish_allergy", "halal", "kosher"
        )
    }

    fun execute(param: SetDietaryRestrictionsParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: SetDietaryRestrictionsParam): Result<Unit, AppError> {
        val invalid = param.restrictions.filter { it !in ALLOWED_RESTRICTIONS }
        if (invalid.isNotEmpty()) {
            return failure(ValidationError("restrictions", "invalid values: ${invalid.joinToString(", ")}. Allowed: ${ALLOWED_RESTRICTIONS.joinToString(", ")}"))
        }
        return success(Unit)
    }
}
