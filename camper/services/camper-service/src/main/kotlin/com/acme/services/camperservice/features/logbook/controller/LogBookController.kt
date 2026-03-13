package com.acme.services.camperservice.features.logbook.controller

import com.acme.clients.common.Result
import com.acme.services.camperservice.common.error.toResponseEntity
import com.acme.services.camperservice.features.logbook.dto.AskFaqRequest
import com.acme.services.camperservice.features.logbook.dto.AnswerFaqRequest
import com.acme.services.camperservice.features.logbook.dto.CreateJournalEntryRequest
import com.acme.services.camperservice.features.logbook.dto.UpdateJournalEntryRequest
import com.acme.services.camperservice.features.logbook.mapper.LogBookMapper
import com.acme.services.camperservice.features.logbook.params.*
import com.acme.services.camperservice.features.logbook.service.LogBookService
import com.acme.services.camperservice.websocket.PlanEventPublisher
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/plans/{planId}/log-book")
class LogBookController(
    private val logBookService: LogBookService,
    private val eventPublisher: PlanEventPublisher,
) {
    private val logger = LoggerFactory.getLogger(LogBookController::class.java)

    @PostMapping("/faqs")
    fun askFaq(
        @PathVariable planId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: AskFaqRequest,
    ): ResponseEntity<Any> {
        logger.info("POST /api/plans/{}/log-book/faqs", planId)
        val param = AskFaqParam(
            planId = planId,
            question = request.question,
            requestingUserId = userId,
        )
        val result = logBookService.askFaq(param)
        if (result is Result.Success) eventPublisher.publishUpdate(planId, "log-book-faqs", "updated")
        return result.toResponseEntity(successStatus = 201) { LogBookMapper.toResponse(it) }
    }

    @PutMapping("/faqs/{faqId}/answer")
    fun answerFaq(
        @PathVariable planId: UUID,
        @PathVariable faqId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: AnswerFaqRequest,
    ): ResponseEntity<Any> {
        logger.info("PUT /api/plans/{}/log-book/faqs/{}/answer", planId, faqId)
        val param = AnswerFaqParam(
            faqId = faqId,
            planId = planId,
            answer = request.answer,
            requestingUserId = userId,
        )
        val result = logBookService.answerFaq(param)
        if (result is Result.Success) eventPublisher.publishUpdate(planId, "log-book-faqs", "updated")
        return result.toResponseEntity { LogBookMapper.toResponse(it) }
    }

    @GetMapping("/faqs")
    fun getFaqs(
        @PathVariable planId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
    ): ResponseEntity<Any> {
        logger.info("GET /api/plans/{}/log-book/faqs", planId)
        val param = GetFaqsParam(planId = planId, requestingUserId = userId)
        return logBookService.getFaqs(param).toResponseEntity { list -> list.map { LogBookMapper.toResponse(it) } }
    }

    @DeleteMapping("/faqs/{faqId}")
    fun deleteFaq(
        @PathVariable planId: UUID,
        @PathVariable faqId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
    ): ResponseEntity<Any> {
        logger.info("DELETE /api/plans/{}/log-book/faqs/{}", planId, faqId)
        val param = DeleteFaqParam(faqId = faqId, planId = planId, requestingUserId = userId)
        val result = logBookService.deleteFaq(param)
        if (result is Result.Success) eventPublisher.publishUpdate(planId, "log-book-faqs", "updated")
        return result.toResponseEntity(successStatus = 204) { }
    }

    @PostMapping("/journal")
    fun createJournalEntry(
        @PathVariable planId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: CreateJournalEntryRequest,
    ): ResponseEntity<Any> {
        logger.info("POST /api/plans/{}/log-book/journal", planId)
        val param = CreateJournalEntryParam(
            planId = planId,
            content = request.content,
            requestingUserId = userId,
        )
        val result = logBookService.createJournalEntry(param)
        if (result is Result.Success) eventPublisher.publishUpdate(planId, "log-book-journal", "updated")
        return result.toResponseEntity(successStatus = 201) { LogBookMapper.toResponse(it) }
    }

    @PutMapping("/journal/{entryId}")
    fun updateJournalEntry(
        @PathVariable planId: UUID,
        @PathVariable entryId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: UpdateJournalEntryRequest,
    ): ResponseEntity<Any> {
        logger.info("PUT /api/plans/{}/log-book/journal/{}", planId, entryId)
        val param = UpdateJournalEntryParam(
            entryId = entryId,
            planId = planId,
            content = request.content,
            requestingUserId = userId,
        )
        val result = logBookService.updateJournalEntry(param)
        if (result is Result.Success) eventPublisher.publishUpdate(planId, "log-book-journal", "updated")
        return result.toResponseEntity { LogBookMapper.toResponse(it) }
    }

    @GetMapping("/journal")
    fun getJournalEntries(
        @PathVariable planId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
    ): ResponseEntity<Any> {
        logger.info("GET /api/plans/{}/log-book/journal", planId)
        val param = GetJournalEntriesParam(planId = planId, requestingUserId = userId)
        return logBookService.getJournalEntries(param).toResponseEntity { list -> list.map { LogBookMapper.toResponse(it) } }
    }

    @DeleteMapping("/journal/{entryId}")
    fun deleteJournalEntry(
        @PathVariable planId: UUID,
        @PathVariable entryId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
    ): ResponseEntity<Any> {
        logger.info("DELETE /api/plans/{}/log-book/journal/{}", planId, entryId)
        val param = DeleteJournalEntryParam(entryId = entryId, planId = planId, requestingUserId = userId)
        val result = logBookService.deleteJournalEntry(param)
        if (result is Result.Success) eventPublisher.publishUpdate(planId, "log-book-journal", "updated")
        return result.toResponseEntity(successStatus = 204) { }
    }
}
