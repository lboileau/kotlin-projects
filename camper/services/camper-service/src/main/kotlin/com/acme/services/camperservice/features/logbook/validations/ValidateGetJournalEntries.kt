package com.acme.services.camperservice.features.logbook.validations

import com.acme.clients.common.Result
import com.acme.clients.common.success
import com.acme.services.camperservice.features.logbook.error.LogBookError
import com.acme.services.camperservice.features.logbook.params.GetJournalEntriesParam
import org.slf4j.LoggerFactory

internal class ValidateGetJournalEntries {
    private val logger = LoggerFactory.getLogger(ValidateGetJournalEntries::class.java)

    fun execute(param: GetJournalEntriesParam): Result<Unit, LogBookError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(@Suppress("UNUSED_PARAMETER") param: GetJournalEntriesParam): Result<Unit, LogBookError> {
        return success(Unit)
    }
}
