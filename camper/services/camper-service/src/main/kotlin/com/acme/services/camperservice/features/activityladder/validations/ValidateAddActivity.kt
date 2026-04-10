package com.acme.services.camperservice.features.activityladder.validations

import com.acme.clients.common.Result
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.services.camperservice.features.activityladder.error.LadderError
import com.acme.services.camperservice.features.activityladder.params.AddActivityParam
import org.slf4j.LoggerFactory

internal class ValidateAddActivity {
    private val logger = LoggerFactory.getLogger(ValidateAddActivity::class.java)

    fun execute(param: AddActivityParam): Result<Unit, LadderError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: AddActivityParam): Result<Unit, LadderError> {
        if (param.name.isBlank()) {
            return failure(LadderError.Invalid("name", "must not be blank"))
        }
        if (param.name.length > 200) {
            return failure(LadderError.Invalid("name", "must be 200 characters or fewer"))
        }
        if (!param.imageUrl.startsWith("http://") && !param.imageUrl.startsWith("https://")) {
            return failure(LadderError.Invalid("imageUrl", "must start with http:// or https://"))
        }
        if (param.imageUrl.length > 2000) {
            return failure(LadderError.Invalid("imageUrl", "must be 2000 characters or fewer"))
        }
        if (param.distanceMinutes < 0) {
            return failure(LadderError.Invalid("distanceMinutes", "must be >= 0"))
        }
        if (param.costPerPerson < java.math.BigDecimal.ZERO) {
            return failure(LadderError.Invalid("costPerPerson", "must be >= 0"))
        }
        return success(Unit)
    }
}
