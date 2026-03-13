package com.acme.clients.logbookclient.fake

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.logbookclient.api.*
import com.acme.clients.logbookclient.internal.validations.*
import com.acme.clients.logbookclient.model.LogBookFaq
import com.acme.clients.logbookclient.model.LogBookJournalEntry
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class FakeLogBookClient : LogBookClient {
    private val faqStore = ConcurrentHashMap<UUID, LogBookFaq>()
    private val journalStore = ConcurrentHashMap<UUID, LogBookJournalEntry>()

    private val validateCreateFaq = ValidateCreateFaq()
    private val validateAnswerFaq = ValidateAnswerFaq()
    private val validateGetFaqsByPlanId = ValidateGetFaqsByPlanId()
    private val validateDeleteFaq = ValidateDeleteFaq()
    private val validateCreateJournalEntry = ValidateCreateJournalEntry()
    private val validateUpdateJournalEntry = ValidateUpdateJournalEntry()
    private val validateGetJournalEntriesByPlanId = ValidateGetJournalEntriesByPlanId()
    private val validateDeleteJournalEntry = ValidateDeleteJournalEntry()

    override fun createFaq(param: CreateFaqParam): Result<LogBookFaq, AppError> {
        val validation = validateCreateFaq.execute(param)
        if (validation is Result.Failure) return validation

        val entity = LogBookFaq(
            id = UUID.randomUUID(),
            planId = param.planId,
            question = param.question,
            askedById = param.askedById,
            answer = null,
            answeredById = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        faqStore[entity.id] = entity
        return success(entity)
    }

    override fun answerFaq(param: AnswerFaqParam): Result<LogBookFaq, AppError> {
        val validation = validateAnswerFaq.execute(param)
        if (validation is Result.Failure) return validation

        val existing = faqStore[param.id]
            ?: return failure(NotFoundError("LogBookFaq", param.id.toString()))
        val updated = existing.copy(
            answer = param.answer,
            answeredById = param.answeredById,
            updatedAt = Instant.now(),
        )
        faqStore[param.id] = updated
        return success(updated)
    }

    override fun getFaqsByPlanId(param: GetFaqsByPlanIdParam): Result<List<LogBookFaq>, AppError> {
        val validation = validateGetFaqsByPlanId.execute(param)
        if (validation is Result.Failure) return validation

        val entities = faqStore.values
            .filter { it.planId == param.planId }
            .sortedByDescending { it.createdAt }
        return success(entities)
    }

    override fun deleteFaq(param: DeleteFaqParam): Result<Unit, AppError> {
        val validation = validateDeleteFaq.execute(param)
        if (validation is Result.Failure) return validation

        return if (faqStore.remove(param.id) != null) success(Unit)
        else failure(NotFoundError("LogBookFaq", param.id.toString()))
    }

    override fun createJournalEntry(param: CreateJournalEntryParam): Result<LogBookJournalEntry, AppError> {
        val validation = validateCreateJournalEntry.execute(param)
        if (validation is Result.Failure) return validation

        val maxPage = journalStore.values
            .filter { it.planId == param.planId }
            .maxOfOrNull { it.pageNumber } ?: 0

        val entity = LogBookJournalEntry(
            id = UUID.randomUUID(),
            planId = param.planId,
            userId = param.userId,
            pageNumber = maxPage + 1,
            content = param.content,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        journalStore[entity.id] = entity
        return success(entity)
    }

    override fun updateJournalEntry(param: UpdateJournalEntryParam): Result<LogBookJournalEntry, AppError> {
        val validation = validateUpdateJournalEntry.execute(param)
        if (validation is Result.Failure) return validation

        val existing = journalStore[param.id]
            ?: return failure(NotFoundError("LogBookJournalEntry", param.id.toString()))
        val updated = existing.copy(
            content = param.content,
            updatedAt = Instant.now(),
        )
        journalStore[param.id] = updated
        return success(updated)
    }

    override fun getJournalEntriesByPlanId(param: GetJournalEntriesByPlanIdParam): Result<List<LogBookJournalEntry>, AppError> {
        val validation = validateGetJournalEntriesByPlanId.execute(param)
        if (validation is Result.Failure) return validation

        val entities = journalStore.values
            .filter { it.planId == param.planId }
            .sortedBy { it.pageNumber }
        return success(entities)
    }

    override fun deleteJournalEntry(param: DeleteJournalEntryParam): Result<Unit, AppError> {
        val validation = validateDeleteJournalEntry.execute(param)
        if (validation is Result.Failure) return validation

        return if (journalStore.remove(param.id) != null) success(Unit)
        else failure(NotFoundError("LogBookJournalEntry", param.id.toString()))
    }

    fun reset() {
        faqStore.clear()
        journalStore.clear()
    }

    fun seedFaq(vararg entities: LogBookFaq) {
        entities.forEach { faqStore[it.id] = it }
    }

    fun seedJournalEntry(vararg entities: LogBookJournalEntry) {
        entities.forEach { journalStore[it.id] = it }
    }
}
