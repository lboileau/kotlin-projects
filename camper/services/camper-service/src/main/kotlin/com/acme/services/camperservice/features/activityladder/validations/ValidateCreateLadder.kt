package com.acme.services.camperservice.features.activityladder.validations

import com.acme.clients.common.Result
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.services.camperservice.features.activityladder.error.LadderError
import com.acme.services.camperservice.features.activityladder.params.CreateLadderParam
import org.slf4j.LoggerFactory

internal class ValidateCreateLadder {
    private val logger = LoggerFactory.getLogger(ValidateCreateLadder::class.java)

    fun execute(param: CreateLadderParam): Result<Unit, LadderError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: CreateLadderParam): Result<Unit, LadderError> {
        if (param.title.isBlank()) {
            return failure(LadderError.Invalid("title", "must not be blank"))
        }
        if (param.title.length > 200) {
            return failure(LadderError.Invalid("title", "must be 200 characters or fewer"))
        }
        if (param.activities.size < 2) {
            return failure(LadderError.NotEnoughActivities(param.activities.size))
        }
        for ((index, activity) in param.activities.withIndex()) {
            if (activity.name.isBlank()) {
                return failure(LadderError.Invalid("activities[$index].name", "must not be blank"))
            }
            if (activity.name.length > 200) {
                return failure(LadderError.Invalid("activities[$index].name", "must be 200 characters or fewer"))
            }
            if (!activity.imageUrl.startsWith("http://") && !activity.imageUrl.startsWith("https://")) {
                return failure(LadderError.Invalid("activities[$index].imageUrl", "must start with http:// or https://"))
            }
            if (activity.imageUrl.length > 2000) {
                return failure(LadderError.Invalid("activities[$index].imageUrl", "must be 2000 characters or fewer"))
            }
            if (activity.distanceMinutes < 0) {
                return failure(LadderError.Invalid("activities[$index].distanceMinutes", "must be >= 0"))
            }
            if (activity.costPerPerson < java.math.BigDecimal.ZERO) {
                return failure(LadderError.Invalid("activities[$index].costPerPerson", "must be >= 0"))
            }
        }
        return success(Unit)
    }
}
