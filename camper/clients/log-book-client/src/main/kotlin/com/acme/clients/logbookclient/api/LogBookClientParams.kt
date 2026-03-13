package com.acme.clients.logbookclient.api

import java.util.UUID

/** Parameter for creating a new FAQ question. */
data class CreateFaqParam(
    val planId: UUID,
    val question: String,
    val askedById: UUID,
)

/** Parameter for answering an existing FAQ. */
data class AnswerFaqParam(
    val id: UUID,
    val answer: String,
    val answeredById: UUID,
)

/** Parameter for listing FAQs by plan ID. */
data class GetFaqsByPlanIdParam(val planId: UUID)

/** Parameter for deleting a FAQ. */
data class DeleteFaqParam(val id: UUID)

/** Parameter for creating a new journal entry. */
data class CreateJournalEntryParam(
    val planId: UUID,
    val userId: UUID,
    val content: String,
)

/** Parameter for updating a journal entry's content. */
data class UpdateJournalEntryParam(
    val id: UUID,
    val content: String,
)

/** Parameter for listing journal entries by plan ID. */
data class GetJournalEntriesByPlanIdParam(val planId: UUID)

/** Parameter for deleting a journal entry. */
data class DeleteJournalEntryParam(val id: UUID)
