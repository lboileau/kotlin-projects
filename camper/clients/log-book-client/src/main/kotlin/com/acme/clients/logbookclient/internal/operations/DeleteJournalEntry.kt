package com.acme.clients.logbookclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.logbookclient.api.DeleteJournalEntryParam
import com.acme.clients.logbookclient.internal.validations.ValidateDeleteJournalEntry
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class DeleteJournalEntry(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(DeleteJournalEntry::class.java)
    private val validate = ValidateDeleteJournalEntry()

    fun execute(param: DeleteJournalEntryParam): Result<Unit, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Deleting journal entry id={}", param.id)
        val rowsAffected = jdbi.withHandle<Int, Exception> { handle ->
            handle.createUpdate("DELETE FROM log_book_journal_entries WHERE id = :id")
                .bind("id", param.id)
                .execute()
        }
        return if (rowsAffected > 0) success(Unit) else failure(NotFoundError("LogBookJournalEntry", param.id.toString()))
    }
}
