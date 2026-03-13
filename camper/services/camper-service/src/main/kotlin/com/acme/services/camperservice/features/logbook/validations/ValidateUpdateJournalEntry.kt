package com.acme.services.camperservice.features.logbook.validations

import com.acme.clients.common.Result
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.services.camperservice.features.logbook.error.LogBookError
import com.acme.services.camperservice.features.logbook.params.UpdateJournalEntryParam
import org.slf4j.LoggerFactory

internal class ValidateUpdateJournalEntry {
    private val logger = LoggerFactory.getLogger(ValidateUpdateJournalEntry::class.java)

    fun execute(param: UpdateJournalEntryParam): Result<Unit, LogBookError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: UpdateJournalEntryParam): Result<Unit, LogBookError> {
        if (param.content.isBlank()) return failure(LogBookError.Invalid("content", "must not be blank"))
        return success(Unit)
    }
}
