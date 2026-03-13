package com.acme.clients.logbookclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.logbookclient.api.CreateJournalEntryParam
import com.acme.clients.logbookclient.internal.adapters.LogBookJournalEntryRowAdapter
import com.acme.clients.logbookclient.internal.validations.ValidateCreateJournalEntry
import com.acme.clients.logbookclient.model.LogBookJournalEntry
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.util.UUID

internal class CreateJournalEntry(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(CreateJournalEntry::class.java)
    private val validate = ValidateCreateJournalEntry()

    fun execute(param: CreateJournalEntryParam): Result<LogBookJournalEntry, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Creating journal entry for plan id={}", param.planId)
        val entity = jdbi.inTransaction<LogBookJournalEntry, Exception> { handle ->
            val id = UUID.randomUUID()
            handle.createUpdate(
                """
                INSERT INTO log_book_journal_entries (id, plan_id, user_id, page_number, content)
                VALUES (:id, :planId, :userId, COALESCE((SELECT MAX(page_number) FROM log_book_journal_entries WHERE plan_id = :planId), 0) + 1, :content)
                """.trimIndent()
            )
                .bind("id", id)
                .bind("planId", param.planId)
                .bind("userId", param.userId)
                .bind("content", param.content)
                .execute()

            handle.createQuery(
                """
                SELECT id, plan_id, user_id, page_number, content, created_at, updated_at
                FROM log_book_journal_entries WHERE id = :id
                """.trimIndent()
            )
                .bind("id", id)
                .map { rs, _ -> LogBookJournalEntryRowAdapter.fromResultSet(rs) }
                .one()
        }
        return success(entity)
    }
}
