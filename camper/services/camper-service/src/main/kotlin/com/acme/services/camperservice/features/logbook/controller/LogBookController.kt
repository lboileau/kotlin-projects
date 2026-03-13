package com.acme.services.camperservice.features.logbook.controller

import com.acme.services.camperservice.features.logbook.dto.AskFaqRequest
import com.acme.services.camperservice.features.logbook.dto.AnswerFaqRequest
import com.acme.services.camperservice.features.logbook.dto.CreateJournalEntryRequest
import com.acme.services.camperservice.features.logbook.dto.UpdateJournalEntryRequest
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
        return ResponseEntity.status(501).body("Not yet implemented")
    }

    @PutMapping("/faqs/{faqId}/answer")
    fun answerFaq(
        @PathVariable planId: UUID,
        @PathVariable faqId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: AnswerFaqRequest,
    ): ResponseEntity<Any> {
        logger.info("PUT /api/plans/{}/log-book/faqs/{}/answer", planId, faqId)
        return ResponseEntity.status(501).body("Not yet implemented")
    }

    @GetMapping("/faqs")
    fun getFaqs(
        @PathVariable planId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
    ): ResponseEntity<Any> {
        logger.info("GET /api/plans/{}/log-book/faqs", planId)
        return ResponseEntity.status(501).body("Not yet implemented")
    }

    @DeleteMapping("/faqs/{faqId}")
    fun deleteFaq(
        @PathVariable planId: UUID,
        @PathVariable faqId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
    ): ResponseEntity<Any> {
        logger.info("DELETE /api/plans/{}/log-book/faqs/{}", planId, faqId)
        return ResponseEntity.status(501).body("Not yet implemented")
    }

    @PostMapping("/journal")
    fun createJournalEntry(
        @PathVariable planId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: CreateJournalEntryRequest,
    ): ResponseEntity<Any> {
        logger.info("POST /api/plans/{}/log-book/journal", planId)
        return ResponseEntity.status(501).body("Not yet implemented")
    }

    @PutMapping("/journal/{entryId}")
    fun updateJournalEntry(
        @PathVariable planId: UUID,
        @PathVariable entryId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: UpdateJournalEntryRequest,
    ): ResponseEntity<Any> {
        logger.info("PUT /api/plans/{}/log-book/journal/{}", planId, entryId)
        return ResponseEntity.status(501).body("Not yet implemented")
    }

    @GetMapping("/journal")
    fun getJournalEntries(
        @PathVariable planId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
    ): ResponseEntity<Any> {
        logger.info("GET /api/plans/{}/log-book/journal", planId)
        return ResponseEntity.status(501).body("Not yet implemented")
    }

    @DeleteMapping("/journal/{entryId}")
    fun deleteJournalEntry(
        @PathVariable planId: UUID,
        @PathVariable entryId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
    ): ResponseEntity<Any> {
        logger.info("DELETE /api/plans/{}/log-book/journal/{}", planId, entryId)
        return ResponseEntity.status(501).body("Not yet implemented")
    }
}
