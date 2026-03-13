package com.acme.services.camperservice.features.logbook.service

import com.acme.clients.common.Result
import com.acme.clients.logbookclient.api.LogBookClient
import com.acme.services.camperservice.common.auth.PlanRoleAuthorizer
import com.acme.services.camperservice.features.logbook.actions.*
import com.acme.services.camperservice.features.logbook.error.LogBookError
import com.acme.services.camperservice.features.logbook.model.LogBookFaq
import com.acme.services.camperservice.features.logbook.model.LogBookJournalEntry
import com.acme.services.camperservice.features.logbook.params.*

class LogBookService(
    logBookClient: LogBookClient,
    planRoleAuthorizer: PlanRoleAuthorizer,
) {
    private val askFaq = AskFaqAction(logBookClient, planRoleAuthorizer)
    private val answerFaq = AnswerFaqAction(logBookClient, planRoleAuthorizer)
    private val getFaqs = GetFaqsAction(logBookClient, planRoleAuthorizer)
    private val deleteFaq = DeleteFaqAction(logBookClient, planRoleAuthorizer)
    private val createJournalEntry = CreateJournalEntryAction(logBookClient, planRoleAuthorizer)
    private val updateJournalEntry = UpdateJournalEntryAction(logBookClient, planRoleAuthorizer)
    private val getJournalEntries = GetJournalEntriesAction(logBookClient, planRoleAuthorizer)
    private val deleteJournalEntry = DeleteJournalEntryAction(logBookClient, planRoleAuthorizer)

    fun askFaq(param: AskFaqParam): Result<LogBookFaq, LogBookError> = askFaq.execute(param)
    fun answerFaq(param: AnswerFaqParam): Result<LogBookFaq, LogBookError> = answerFaq.execute(param)
    fun getFaqs(param: GetFaqsParam): Result<List<LogBookFaq>, LogBookError> = getFaqs.execute(param)
    fun deleteFaq(param: DeleteFaqParam): Result<Unit, LogBookError> = deleteFaq.execute(param)
    fun createJournalEntry(param: CreateJournalEntryParam): Result<LogBookJournalEntry, LogBookError> = createJournalEntry.execute(param)
    fun updateJournalEntry(param: UpdateJournalEntryParam): Result<LogBookJournalEntry, LogBookError> = updateJournalEntry.execute(param)
    fun getJournalEntries(param: GetJournalEntriesParam): Result<List<LogBookJournalEntry>, LogBookError> = getJournalEntries.execute(param)
    fun deleteJournalEntry(param: DeleteJournalEntryParam): Result<Unit, LogBookError> = deleteJournalEntry.execute(param)
}
