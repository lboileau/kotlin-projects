package com.acme.services.camperservice.features.logbook.service

import com.acme.clients.common.Result
import com.acme.clients.logbookclient.fake.FakeLogBookClient
import com.acme.clients.logbookclient.model.LogBookFaq
import com.acme.clients.logbookclient.model.LogBookJournalEntry
import com.acme.clients.planclient.fake.FakePlanClient
import com.acme.clients.planclient.model.Plan
import com.acme.clients.planclient.model.PlanMember
import com.acme.services.camperservice.common.auth.PlanRoleAuthorizer
import com.acme.services.camperservice.features.logbook.error.LogBookError
import com.acme.services.camperservice.features.logbook.params.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class LogBookServiceTest {

    private val fakeLogBookClient = FakeLogBookClient()
    private val fakePlanClient = FakePlanClient()
    private val planRoleAuthorizer = PlanRoleAuthorizer(fakePlanClient)
    private val service = LogBookService(fakeLogBookClient, planRoleAuthorizer)

    private val planId = UUID.randomUUID()
    private val ownerId = UUID.randomUUID()
    private val managerId = UUID.randomUUID()
    private val memberId = UUID.randomUUID()
    private val nonMemberId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        fakeLogBookClient.reset()
        fakePlanClient.reset()
        fakePlanClient.seedPlan(Plan(id = planId, name = "Test Plan", visibility = "private", ownerId = ownerId, createdAt = Instant.now(), updatedAt = Instant.now()))
        fakePlanClient.seedMember(PlanMember(planId = planId, userId = ownerId, role = "member", createdAt = Instant.now()))
        fakePlanClient.seedMember(PlanMember(planId = planId, userId = managerId, role = "manager", createdAt = Instant.now()))
        fakePlanClient.seedMember(PlanMember(planId = planId, userId = memberId, role = "member", createdAt = Instant.now()))
    }

    // ── FAQ Tests ──────────────────────────────────────────────────────

    @Nested
    inner class AskFaq {
        @Test
        fun `askFaq creates FAQ and returns mapped result`() {
            val result = service.askFaq(AskFaqParam(planId = planId, question = "Where do we meet?", requestingUserId = memberId))

            assertThat(result.isSuccess).isTrue()
            val faq = (result as Result.Success).value
            assertThat(faq.question).isEqualTo("Where do we meet?")
            assertThat(faq.planId).isEqualTo(planId)
            assertThat(faq.askedById).isEqualTo(memberId)
            assertThat(faq.answer).isNull()
            assertThat(faq.answeredById).isNull()
        }

        @Test
        fun `askFaq returns Invalid for blank question`() {
            val result = service.askFaq(AskFaqParam(planId = planId, question = "", requestingUserId = memberId))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(LogBookError.Invalid::class.java)
            assertThat(((result).error as LogBookError.Invalid).field).isEqualTo("question")
        }

        @Test
        fun `askFaq returns Forbidden for non-member`() {
            val result = service.askFaq(AskFaqParam(planId = planId, question = "Can I come?", requestingUserId = nonMemberId))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(LogBookError.Forbidden::class.java)
        }
    }

    @Nested
    inner class AnswerFaq {
        private lateinit var seededFaq: LogBookFaq

        @BeforeEach
        fun seedFaq() {
            seededFaq = LogBookFaq(
                id = UUID.randomUUID(),
                planId = planId,
                question = "What to bring?",
                askedById = memberId,
                answer = null,
                answeredById = null,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )
            fakeLogBookClient.seedFaq(seededFaq)
        }

        @Test
        fun `answerFaq succeeds for OWNER`() {
            val result = service.answerFaq(AnswerFaqParam(faqId = seededFaq.id, planId = planId, answer = "Warm clothes", requestingUserId = ownerId))

            assertThat(result.isSuccess).isTrue()
            val faq = (result as Result.Success).value
            assertThat(faq.answer).isEqualTo("Warm clothes")
            assertThat(faq.answeredById).isEqualTo(ownerId)
        }

        @Test
        fun `answerFaq succeeds for MANAGER`() {
            val result = service.answerFaq(AnswerFaqParam(faqId = seededFaq.id, planId = planId, answer = "Bring a tent", requestingUserId = managerId))

            assertThat(result.isSuccess).isTrue()
            val faq = (result as Result.Success).value
            assertThat(faq.answer).isEqualTo("Bring a tent")
            assertThat(faq.answeredById).isEqualTo(managerId)
        }

        @Test
        fun `answerFaq returns Forbidden for MEMBER`() {
            val result = service.answerFaq(AnswerFaqParam(faqId = seededFaq.id, planId = planId, answer = "I'll answer", requestingUserId = memberId))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(LogBookError.Forbidden::class.java)
        }

        @Test
        fun `answerFaq returns NotFound for non-existent FAQ`() {
            val result = service.answerFaq(AnswerFaqParam(faqId = UUID.randomUUID(), planId = planId, answer = "Answer", requestingUserId = ownerId))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(LogBookError.NotFound::class.java)
        }

        @Test
        fun `answerFaq returns Invalid for blank answer`() {
            val result = service.answerFaq(AnswerFaqParam(faqId = seededFaq.id, planId = planId, answer = "", requestingUserId = ownerId))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(LogBookError.Invalid::class.java)
            assertThat(((result).error as LogBookError.Invalid).field).isEqualTo("answer")
        }
    }

    @Nested
    inner class GetFaqs {
        @Test
        fun `getFaqs returns mapped FAQs with correct fields`() {
            fakeLogBookClient.seedFaq(
                LogBookFaq(id = UUID.randomUUID(), planId = planId, question = "Q1", askedById = memberId, answer = null, answeredById = null, createdAt = Instant.now(), updatedAt = Instant.now()),
                LogBookFaq(id = UUID.randomUUID(), planId = planId, question = "Q2", askedById = memberId, answer = "A2", answeredById = ownerId, createdAt = Instant.now(), updatedAt = Instant.now()),
            )

            val result = service.getFaqs(GetFaqsParam(planId = planId, requestingUserId = memberId))

            assertThat(result.isSuccess).isTrue()
            val faqs = (result as Result.Success).value
            assertThat(faqs).hasSize(2)
            val unanswered = faqs.find { it.question == "Q1" }!!
            assertThat(unanswered.planId).isEqualTo(planId)
            assertThat(unanswered.askedById).isEqualTo(memberId)
            assertThat(unanswered.answer).isNull()
            assertThat(unanswered.answeredById).isNull()
            val answered = faqs.find { it.question == "Q2" }!!
            assertThat(answered.planId).isEqualTo(planId)
            assertThat(answered.answer).isEqualTo("A2")
            assertThat(answered.answeredById).isEqualTo(ownerId)
        }

        @Test
        fun `getFaqs returns Forbidden for non-member`() {
            val result = service.getFaqs(GetFaqsParam(planId = planId, requestingUserId = nonMemberId))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(LogBookError.Forbidden::class.java)
        }
    }

    @Nested
    inner class DeleteFaq {
        private lateinit var askerFaq: LogBookFaq

        @BeforeEach
        fun seedFaq() {
            askerFaq = LogBookFaq(
                id = UUID.randomUUID(),
                planId = planId,
                question = "My question",
                askedById = memberId,
                answer = null,
                answeredById = null,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )
            fakeLogBookClient.seedFaq(askerFaq)
        }

        @Test
        fun `deleteFaq succeeds for OWNER`() {
            val result = service.deleteFaq(DeleteFaqParam(faqId = askerFaq.id, planId = planId, requestingUserId = ownerId))

            assertThat(result.isSuccess).isTrue()
        }

        @Test
        fun `deleteFaq succeeds for MANAGER`() {
            val result = service.deleteFaq(DeleteFaqParam(faqId = askerFaq.id, planId = planId, requestingUserId = managerId))

            assertThat(result.isSuccess).isTrue()
        }

        @Test
        fun `deleteFaq succeeds for original asker`() {
            val result = service.deleteFaq(DeleteFaqParam(faqId = askerFaq.id, planId = planId, requestingUserId = memberId))

            assertThat(result.isSuccess).isTrue()
        }

        @Test
        fun `deleteFaq returns Forbidden for MEMBER who is not the asker`() {
            val otherMemberId = UUID.randomUUID()
            fakePlanClient.seedMember(PlanMember(planId = planId, userId = otherMemberId, role = "member", createdAt = Instant.now()))

            val result = service.deleteFaq(DeleteFaqParam(faqId = askerFaq.id, planId = planId, requestingUserId = otherMemberId))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(LogBookError.Forbidden::class.java)
        }

        @Test
        fun `deleteFaq returns NotFound for non-existent FAQ`() {
            val result = service.deleteFaq(DeleteFaqParam(faqId = UUID.randomUUID(), planId = planId, requestingUserId = ownerId))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(LogBookError.NotFound::class.java)
        }
    }

    // ── Journal Entry Tests ────────────────────────────────────────────

    @Nested
    inner class CreateJournalEntry {
        @Test
        fun `createJournalEntry creates entry with auto page number`() {
            val result = service.createJournalEntry(CreateJournalEntryParam(planId = planId, content = "Day 1 notes", requestingUserId = memberId))

            assertThat(result.isSuccess).isTrue()
            val entry = (result as Result.Success).value
            assertThat(entry.content).isEqualTo("Day 1 notes")
            assertThat(entry.planId).isEqualTo(planId)
            assertThat(entry.userId).isEqualTo(memberId)
            assertThat(entry.pageNumber).isEqualTo(1)
        }

        @Test
        fun `createJournalEntry returns Invalid for blank content`() {
            val result = service.createJournalEntry(CreateJournalEntryParam(planId = planId, content = "", requestingUserId = memberId))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(LogBookError.Invalid::class.java)
            assertThat(((result).error as LogBookError.Invalid).field).isEqualTo("content")
        }

        @Test
        fun `createJournalEntry returns Forbidden for non-member`() {
            val result = service.createJournalEntry(CreateJournalEntryParam(planId = planId, content = "Sneaky entry", requestingUserId = nonMemberId))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(LogBookError.Forbidden::class.java)
        }
    }

    @Nested
    inner class UpdateJournalEntry {
        private lateinit var authorEntry: LogBookJournalEntry

        @BeforeEach
        fun seedEntry() {
            authorEntry = LogBookJournalEntry(
                id = UUID.randomUUID(),
                planId = planId,
                userId = memberId,
                pageNumber = 1,
                content = "Original content",
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )
            fakeLogBookClient.seedJournalEntry(authorEntry)
        }

        @Test
        fun `updateJournalEntry succeeds for original author`() {
            val result = service.updateJournalEntry(UpdateJournalEntryParam(entryId = authorEntry.id, planId = planId, content = "Updated content", requestingUserId = memberId))

            assertThat(result.isSuccess).isTrue()
            val entry = (result as Result.Success).value
            assertThat(entry.content).isEqualTo("Updated content")
            assertThat(entry.pageNumber).isEqualTo(1)
        }

        @Test
        fun `updateJournalEntry returns Forbidden for non-author`() {
            val result = service.updateJournalEntry(UpdateJournalEntryParam(entryId = authorEntry.id, planId = planId, content = "Hijack", requestingUserId = ownerId))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(LogBookError.Forbidden::class.java)
        }

        @Test
        fun `updateJournalEntry returns NotFound for non-existent entry`() {
            val result = service.updateJournalEntry(UpdateJournalEntryParam(entryId = UUID.randomUUID(), planId = planId, content = "Nope", requestingUserId = memberId))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(LogBookError.NotFound::class.java)
        }

        @Test
        fun `updateJournalEntry returns Invalid for blank content`() {
            val result = service.updateJournalEntry(UpdateJournalEntryParam(entryId = authorEntry.id, planId = planId, content = "", requestingUserId = memberId))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(LogBookError.Invalid::class.java)
            assertThat(((result).error as LogBookError.Invalid).field).isEqualTo("content")
        }
    }

    @Nested
    inner class GetJournalEntries {
        @Test
        fun `getJournalEntries returns entries ordered by page number`() {
            fakeLogBookClient.seedJournalEntry(
                LogBookJournalEntry(id = UUID.randomUUID(), planId = planId, userId = memberId, pageNumber = 2, content = "Page 2", createdAt = Instant.now(), updatedAt = Instant.now()),
                LogBookJournalEntry(id = UUID.randomUUID(), planId = planId, userId = memberId, pageNumber = 1, content = "Page 1", createdAt = Instant.now(), updatedAt = Instant.now()),
            )

            val result = service.getJournalEntries(GetJournalEntriesParam(planId = planId, requestingUserId = memberId))

            assertThat(result.isSuccess).isTrue()
            val entries = (result as Result.Success).value
            assertThat(entries).hasSize(2)
            assertThat(entries[0].pageNumber).isEqualTo(1)
            assertThat(entries[1].pageNumber).isEqualTo(2)
        }

        @Test
        fun `getJournalEntries returns Forbidden for non-member`() {
            val result = service.getJournalEntries(GetJournalEntriesParam(planId = planId, requestingUserId = nonMemberId))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(LogBookError.Forbidden::class.java)
        }
    }

    @Nested
    inner class DeleteJournalEntry {
        private lateinit var authorEntry: LogBookJournalEntry

        @BeforeEach
        fun seedEntry() {
            authorEntry = LogBookJournalEntry(
                id = UUID.randomUUID(),
                planId = planId,
                userId = memberId,
                pageNumber = 1,
                content = "My journal entry",
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )
            fakeLogBookClient.seedJournalEntry(authorEntry)
        }

        @Test
        fun `deleteJournalEntry succeeds for OWNER`() {
            val result = service.deleteJournalEntry(DeleteJournalEntryParam(entryId = authorEntry.id, planId = planId, requestingUserId = ownerId))

            assertThat(result.isSuccess).isTrue()
        }

        @Test
        fun `deleteJournalEntry succeeds for MANAGER`() {
            val result = service.deleteJournalEntry(DeleteJournalEntryParam(entryId = authorEntry.id, planId = planId, requestingUserId = managerId))

            assertThat(result.isSuccess).isTrue()
        }

        @Test
        fun `deleteJournalEntry succeeds for original author`() {
            val result = service.deleteJournalEntry(DeleteJournalEntryParam(entryId = authorEntry.id, planId = planId, requestingUserId = memberId))

            assertThat(result.isSuccess).isTrue()
        }

        @Test
        fun `deleteJournalEntry returns Forbidden for MEMBER who is not the author`() {
            val otherMemberId = UUID.randomUUID()
            fakePlanClient.seedMember(PlanMember(planId = planId, userId = otherMemberId, role = "member", createdAt = Instant.now()))

            val result = service.deleteJournalEntry(DeleteJournalEntryParam(entryId = authorEntry.id, planId = planId, requestingUserId = otherMemberId))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(LogBookError.Forbidden::class.java)
        }

        @Test
        fun `deleteJournalEntry returns NotFound for non-existent entry`() {
            val result = service.deleteJournalEntry(DeleteJournalEntryParam(entryId = UUID.randomUUID(), planId = planId, requestingUserId = ownerId))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(LogBookError.NotFound::class.java)
        }
    }
}
