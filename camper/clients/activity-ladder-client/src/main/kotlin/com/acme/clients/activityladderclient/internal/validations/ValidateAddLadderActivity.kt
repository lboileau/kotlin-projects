package com.acme.clients.activityladderclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ValidationError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.activityladderclient.api.AddLadderActivityParam
import org.slf4j.LoggerFactory

internal class ValidateAddLadderActivity {
    private val logger = LoggerFactory.getLogger(ValidateAddLadderActivity::class.java)

    fun execute(param: AddLadderActivityParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: AddLadderActivityParam): Result<Unit, AppError> {
        if (param.distanceMinutes < 0) return failure(ValidationError("distanceMinutes", "must be >= 0"))
        if (param.costPerPerson < java.math.BigDecimal.ZERO) return failure(ValidationError("costPerPerson", "must be >= 0"))
        return success(Unit)
    }
}
