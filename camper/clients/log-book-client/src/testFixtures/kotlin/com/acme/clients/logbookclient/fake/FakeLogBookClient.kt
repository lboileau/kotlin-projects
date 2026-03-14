package com.acme.clients.logbookclient.fake

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.logbookclient.api.*
import com.acme.clients.logbookclient.model.LogBookFaq
import com.acme.clients.logbookclient.model.LogBookJournalEntry

class FakeLogBookClient : LogBookClient {

    override fun createFaq(param: CreateFaqParam): Result<LogBookFaq, AppError> {
        throw NotImplementedError()
    }

    override fun answerFaq(param: AnswerFaqParam): Result<LogBookFaq, AppError> {
        throw NotImplementedError()
    }

    override fun getFaqsByPlanId(param: GetFaqsByPlanIdParam): Result<List<LogBookFaq>, AppError> {
        throw NotImplementedError()
    }

    override fun deleteFaq(param: DeleteFaqParam): Result<Unit, AppError> {
        throw NotImplementedError()
    }

    override fun createJournalEntry(param: CreateJournalEntryParam): Result<LogBookJournalEntry, AppError> {
        throw NotImplementedError()
    }

    override fun updateJournalEntry(param: UpdateJournalEntryParam): Result<LogBookJournalEntry, AppError> {
        throw NotImplementedError()
    }

    override fun getJournalEntriesByPlanId(param: GetJournalEntriesByPlanIdParam): Result<List<LogBookJournalEntry>, AppError> {
        throw NotImplementedError()
    }

    override fun deleteJournalEntry(param: DeleteJournalEntryParam): Result<Unit, AppError> {
        throw NotImplementedError()
    }

    fun reset() {}

    fun seedFaq(vararg entities: LogBookFaq) {
        throw NotImplementedError()
    }

    fun seedJournalEntry(vararg entities: LogBookJournalEntry) {
        throw NotImplementedError()
    }
}
