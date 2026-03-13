package com.acme.services.camperservice.features.logbook.mapper

import com.acme.clients.logbookclient.model.LogBookFaq as ClientLogBookFaq
import com.acme.clients.logbookclient.model.LogBookJournalEntry as ClientLogBookJournalEntry
import com.acme.services.camperservice.features.logbook.dto.LogBookFaqResponse
import com.acme.services.camperservice.features.logbook.dto.LogBookJournalEntryResponse
import com.acme.services.camperservice.features.logbook.model.LogBookFaq
import com.acme.services.camperservice.features.logbook.model.LogBookJournalEntry

object LogBookMapper {

    fun fromClient(faq: ClientLogBookFaq): LogBookFaq = LogBookFaq(
        id = faq.id,
        planId = faq.planId,
        question = faq.question,
        askedById = faq.askedById,
        answer = faq.answer,
        answeredById = faq.answeredById,
        createdAt = faq.createdAt,
        updatedAt = faq.updatedAt,
    )

    fun fromClient(entry: ClientLogBookJournalEntry): LogBookJournalEntry = LogBookJournalEntry(
        id = entry.id,
        planId = entry.planId,
        userId = entry.userId,
        pageNumber = entry.pageNumber,
        content = entry.content,
        createdAt = entry.createdAt,
        updatedAt = entry.updatedAt,
    )

    fun toResponse(faq: LogBookFaq): LogBookFaqResponse = LogBookFaqResponse(
        id = faq.id,
        planId = faq.planId,
        question = faq.question,
        askedById = faq.askedById,
        answer = faq.answer,
        answeredById = faq.answeredById,
        createdAt = faq.createdAt,
        updatedAt = faq.updatedAt,
    )

    fun toResponse(entry: LogBookJournalEntry): LogBookJournalEntryResponse = LogBookJournalEntryResponse(
        id = entry.id,
        planId = entry.planId,
        userId = entry.userId,
        pageNumber = entry.pageNumber,
        content = entry.content,
        createdAt = entry.createdAt,
        updatedAt = entry.updatedAt,
    )
}
