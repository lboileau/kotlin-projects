package com.acme.clients.logbookclient.internal

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.logbookclient.api.*
import com.acme.clients.logbookclient.internal.operations.*
import com.acme.clients.logbookclient.model.LogBookFaq
import com.acme.clients.logbookclient.model.LogBookJournalEntry
import org.jdbi.v3.core.Jdbi

/**
 * Facade that delegates to individual operation classes.
 */
internal class JdbiLogBookClient(jdbi: Jdbi) : LogBookClient {

    private val createFaqOp = CreateFaq(jdbi)
    private val answerFaqOp = AnswerFaq(jdbi)
    private val getFaqsByPlanIdOp = GetFaqsByPlanId(jdbi)
    private val deleteFaqOp = DeleteFaq(jdbi)
    private val createJournalEntryOp = CreateJournalEntry(jdbi)
    private val updateJournalEntryOp = UpdateJournalEntry(jdbi)
    private val getJournalEntriesByPlanIdOp = GetJournalEntriesByPlanId(jdbi)
    private val deleteJournalEntryOp = DeleteJournalEntry(jdbi)

    override fun createFaq(param: CreateFaqParam): Result<LogBookFaq, AppError> = createFaqOp.execute(param)
    override fun answerFaq(param: AnswerFaqParam): Result<LogBookFaq, AppError> = answerFaqOp.execute(param)
    override fun getFaqsByPlanId(param: GetFaqsByPlanIdParam): Result<List<LogBookFaq>, AppError> = getFaqsByPlanIdOp.execute(param)
    override fun deleteFaq(param: DeleteFaqParam): Result<Unit, AppError> = deleteFaqOp.execute(param)
    override fun createJournalEntry(param: CreateJournalEntryParam): Result<LogBookJournalEntry, AppError> = createJournalEntryOp.execute(param)
    override fun updateJournalEntry(param: UpdateJournalEntryParam): Result<LogBookJournalEntry, AppError> = updateJournalEntryOp.execute(param)
    override fun getJournalEntriesByPlanId(param: GetJournalEntriesByPlanIdParam): Result<List<LogBookJournalEntry>, AppError> = getJournalEntriesByPlanIdOp.execute(param)
    override fun deleteJournalEntry(param: DeleteJournalEntryParam): Result<Unit, AppError> = deleteJournalEntryOp.execute(param)
}
