package com.acme.clients.logbookclient.api

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.logbookclient.model.LogBookFaq
import com.acme.clients.logbookclient.model.LogBookJournalEntry

/**
 * Client interface for log book operations.
 *
 * Provides FAQ and journal entry management for camp plans.
 * All operations return [Result] to represent success or typed failure
 * without throwing exceptions for expected error conditions.
 */
interface LogBookClient {

    /** Create a new FAQ question (unanswered). */
    fun createFaq(param: CreateFaqParam): Result<LogBookFaq, AppError>

    /** Answer or update the answer to an existing FAQ. */
    fun answerFaq(param: AnswerFaqParam): Result<LogBookFaq, AppError>

    /** List all FAQs for a plan, ordered by created_at desc. */
    fun getFaqsByPlanId(param: GetFaqsByPlanIdParam): Result<List<LogBookFaq>, AppError>

    /** Delete a FAQ. */
    fun deleteFaq(param: DeleteFaqParam): Result<Unit, AppError>

    /** Create a journal entry. Page number is auto-assigned. */
    fun createJournalEntry(param: CreateJournalEntryParam): Result<LogBookJournalEntry, AppError>

    /** Update a journal entry's content. */
    fun updateJournalEntry(param: UpdateJournalEntryParam): Result<LogBookJournalEntry, AppError>

    /** List all journal entries for a plan, ordered by page_number asc. */
    fun getJournalEntriesByPlanId(param: GetJournalEntriesByPlanIdParam): Result<List<LogBookJournalEntry>, AppError>

    /** Delete a journal entry. */
    fun deleteJournalEntry(param: DeleteJournalEntryParam): Result<Unit, AppError>
}
