package com.acme.services.camperservice.features.user.validations

import com.acme.clients.common.Result
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.services.camperservice.features.user.error.UserError
import com.acme.services.camperservice.features.user.params.UpdateUserParam
import org.slf4j.LoggerFactory

internal class ValidateUpdateUser {
    private val logger = LoggerFactory.getLogger(ValidateUpdateUser::class.java)

    companion object {
        private val VALID_EXPERIENCE_LEVELS = setOf("beginner", "intermediate", "advanced", "expert")
        private val VALID_DIETARY_RESTRICTIONS = setOf(
            "gluten_free", "nut_allergy", "vegetarian", "vegan",
            "lactose_intolerant", "shellfish_allergy", "halal", "kosher"
        )
    }

    fun execute(param: UpdateUserParam): Result<Unit, UserError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: UpdateUserParam): Result<Unit, UserError> {
        if (param.username.isBlank()) return failure(UserError.Invalid("username", "must not be blank"))

        param.experienceLevel?.let { level ->
            if (level !in VALID_EXPERIENCE_LEVELS) {
                return failure(UserError.Invalid("experienceLevel", "must be one of: ${VALID_EXPERIENCE_LEVELS.joinToString()}"))
            }
        }

        param.dietaryRestrictions?.forEach { restriction ->
            if (restriction !in VALID_DIETARY_RESTRICTIONS) {
                return failure(UserError.Invalid("dietaryRestrictions", "invalid restriction: $restriction"))
            }
        }

        return success(Unit)
    }
}
