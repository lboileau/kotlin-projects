package com.acme.clients.logbookclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.logbookclient.api.UpdateJournalEntryParam
import com.acme.clients.logbookclient.internal.adapters.LogBookJournalEntryRowAdapter
import com.acme.clients.logbookclient.internal.validations.ValidateUpdateJournalEntry
import com.acme.clients.logbookclient.model.LogBookJournalEntry
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant

internal class UpdateJournalEntry(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(UpdateJournalEntry::class.java)
    private val validate = ValidateUpdateJournalEntry()

    fun execute(param: UpdateJournalEntryParam): Result<LogBookJournalEntry, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Updating journal entry id={}", param.id)
        val now = Instant.now()
        val rowsAffected = jdbi.withHandle<Int, Exception> { handle ->
            handle.createUpdate(
                """
                UPDATE log_book_journal_entries
                SET content = :content, updated_at = :updatedAt
                WHERE id = :id
                """.trimIndent()
            )
                .bind("id", param.id)
                .bind("content", param.content)
                .bind("updatedAt", now)
                .execute()
        }
        if (rowsAffected == 0) return failure(NotFoundError("LogBookJournalEntry", param.id.toString()))

        val entity = jdbi.withHandle<LogBookJournalEntry?, Exception> { handle ->
            handle.createQuery(
                """
                SELECT id, plan_id, user_id, page_number, content, created_at, updated_at
                FROM log_book_journal_entries WHERE id = :id
                """.trimIndent()
            )
                .bind("id", param.id)
                .map { rs, _ -> LogBookJournalEntryRowAdapter.fromResultSet(rs) }
                .findOne()
                .orElse(null)
        }
        return if (entity != null) success(entity) else failure(NotFoundError("LogBookJournalEntry", param.id.toString()))
    }
}
