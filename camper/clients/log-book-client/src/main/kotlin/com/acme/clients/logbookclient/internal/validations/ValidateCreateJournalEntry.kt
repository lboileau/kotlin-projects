package com.acme.clients.logbookclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ValidationError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.logbookclient.api.CreateJournalEntryParam
import org.slf4j.LoggerFactory

internal class ValidateCreateJournalEntry {
    private val logger = LoggerFactory.getLogger(ValidateCreateJournalEntry::class.java)

    fun execute(param: CreateJournalEntryParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: CreateJournalEntryParam): Result<Unit, AppError> {
        if (param.content.isBlank()) return failure(ValidationError("content", "must not be blank"))
        return success(Unit)
    }
}
