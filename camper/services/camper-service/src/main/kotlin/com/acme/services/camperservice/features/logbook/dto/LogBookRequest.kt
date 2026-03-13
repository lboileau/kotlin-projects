package com.acme.services.camperservice.features.logbook.dto

data class AskFaqRequest(val question: String)

data class AnswerFaqRequest(val answer: String)

data class CreateJournalEntryRequest(val content: String)

data class UpdateJournalEntryRequest(val content: String)
