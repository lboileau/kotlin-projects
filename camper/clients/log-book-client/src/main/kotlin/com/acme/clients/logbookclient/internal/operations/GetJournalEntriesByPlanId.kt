package com.acme.clients.logbookclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.logbookclient.api.GetJournalEntriesByPlanIdParam
import com.acme.clients.logbookclient.internal.adapters.LogBookJournalEntryRowAdapter
import com.acme.clients.logbookclient.internal.validations.ValidateGetJournalEntriesByPlanId
import com.acme.clients.logbookclient.model.LogBookJournalEntry
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetJournalEntriesByPlanId(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetJournalEntriesByPlanId::class.java)
    private val validate = ValidateGetJournalEntriesByPlanId()

    fun execute(param: GetJournalEntriesByPlanIdParam): Result<List<LogBookJournalEntry>, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Finding journal entries for plan id={}", param.planId)
        val entities = jdbi.withHandle<List<LogBookJournalEntry>, Exception> { handle ->
            handle.createQuery(
                """
                SELECT id, plan_id, user_id, page_number, content, created_at, updated_at
                FROM log_book_journal_entries
                WHERE plan_id = :planId
                ORDER BY page_number ASC
                """.trimIndent()
            )
                .bind("planId", param.planId)
                .map { rs, _ -> LogBookJournalEntryRowAdapter.fromResultSet(rs) }
                .list()
        }
        return success(entities)
    }
}
