package com.acme.services.camperservice.features.logbook.acceptance

import com.acme.services.camperservice.config.TestContainerConfig
import com.acme.services.camperservice.features.logbook.acceptance.fixture.LogBookFixture
import com.acme.services.camperservice.features.logbook.dto.AskFaqRequest
import com.acme.services.camperservice.features.logbook.dto.AnswerFaqRequest
import com.acme.services.camperservice.features.logbook.dto.CreateJournalEntryRequest
import com.acme.services.camperservice.features.logbook.dto.LogBookFaqResponse
import com.acme.services.camperservice.features.logbook.dto.LogBookJournalEntryResponse
import com.acme.services.camperservice.features.logbook.dto.UpdateJournalEntryRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Import
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestContainerConfig::class)
class LogBookAcceptanceTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    private lateinit var fixture: LogBookFixture
    private lateinit var ownerId: UUID
    private lateinit var managerId: UUID
    private lateinit var memberId: UUID
    private lateinit var planId: UUID

    @BeforeEach
    fun setUp() {
        fixture = LogBookFixture(jdbcTemplate)
        fixture.truncateAll()
        ownerId = fixture.insertUser(email = "owner@example.com")
        managerId = fixture.insertUser(email = "manager@example.com")
        memberId = fixture.insertUser(email = "member@example.com")
        planId = fixture.insertPlan(name = "Test Plan", ownerId = ownerId)
        fixture.insertPlanMember(planId = planId, userId = ownerId, role = "member")
        fixture.insertPlanMember(planId = planId, userId = managerId, role = "manager")
        fixture.insertPlanMember(planId = planId, userId = memberId, role = "member")
    }

    // ── FAQ Endpoints ──────────────────────────────────────────────────

    @Nested
    inner class AskFaq {
        @Test
        fun `POST creates FAQ and returns 201`() {
            val response = restTemplate.exchange(
                "/api/plans/$planId/log-book/faqs",
                HttpMethod.POST,
                entityWithUser(AskFaqRequest(question = "Where do we meet?"), memberId),
                LogBookFaqResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            val body = response.body!!
            assertThat(body.question).isEqualTo("Where do we meet?")
            assertThat(body.planId).isEqualTo(planId)
            assertThat(body.askedById).isEqualTo(memberId)
            assertThat(body.answer).isNull()
            assertThat(body.answeredById).isNull()
            assertThat(body.id).isNotNull()
        }

        @Test
        fun `POST returns 400 when question is blank`() {
            val response = restTemplate.exchange(
                "/api/plans/$planId/log-book/faqs",
                HttpMethod.POST,
                entityWithUser(AskFaqRequest(question = ""), memberId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @Nested
    inner class AnswerFaq {
        @Test
        fun `PUT answers FAQ and returns 200`() {
            val faqId = fixture.insertFaq(planId = planId, question = "What to bring?", askedById = memberId)

            val response = restTemplate.exchange(
                "/api/plans/$planId/log-book/faqs/$faqId/answer",
                HttpMethod.PUT,
                entityWithUser(AnswerFaqRequest(answer = "Warm clothes"), ownerId),
                LogBookFaqResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val body = response.body!!
            assertThat(body.id).isEqualTo(faqId)
            assertThat(body.answer).isEqualTo("Warm clothes")
            assertThat(body.answeredById).isEqualTo(ownerId)
            assertThat(body.question).isEqualTo("What to bring?")
        }

        @Test
        fun `PUT returns 403 when MEMBER tries to answer`() {
            val faqId = fixture.insertFaq(planId = planId, question = "What to bring?", askedById = memberId)

            val response = restTemplate.exchange(
                "/api/plans/$planId/log-book/faqs/$faqId/answer",
                HttpMethod.PUT,
                entityWithUser(AnswerFaqRequest(answer = "My answer"), memberId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        }

        @Test
        fun `PUT returns 400 for blank answer`() {
            val faqId = fixture.insertFaq(planId = planId, question = "What to bring?", askedById = memberId)

            val response = restTemplate.exchange(
                "/api/plans/$planId/log-book/faqs/$faqId/answer",
                HttpMethod.PUT,
                entityWithUser(AnswerFaqRequest(answer = ""), ownerId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        fun `PUT returns 404 for non-existent FAQ`() {
            val response = restTemplate.exchange(
                "/api/plans/$planId/log-book/faqs/${UUID.randomUUID()}/answer",
                HttpMethod.PUT,
                entityWithUser(AnswerFaqRequest(answer = "An answer"), ownerId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @Nested
    inner class GetFaqs {
        @Test
        fun `GET returns 200 with FAQs ordered by created_at desc`() {
            val earlier = java.time.Instant.now().minusSeconds(10)
            val later = java.time.Instant.now()
            fixture.insertFaq(planId = planId, question = "Q1", askedById = memberId, createdAt = earlier)
            fixture.insertFaq(planId = planId, question = "Q2", askedById = memberId, answer = "A2", answeredById = ownerId, createdAt = later)

            val response = restTemplate.exchange(
                "/api/plans/$planId/log-book/faqs",
                HttpMethod.GET,
                entityWithUser(null, memberId),
                Array<LogBookFaqResponse>::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val faqs = response.body!!
            assertThat(faqs).hasSize(2)
            assertThat(faqs.map { it.question }).containsExactly("Q2", "Q1")
        }
    }

    @Nested
    inner class DeleteFaq {
        @Test
        fun `DELETE removes FAQ and returns 204`() {
            val faqId = fixture.insertFaq(planId = planId, question = "To be deleted", askedById = memberId)

            val response = restTemplate.exchange(
                "/api/plans/$planId/log-book/faqs/$faqId",
                HttpMethod.DELETE,
                entityWithUser(null, ownerId),
                Void::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)

            // Verify FAQ is absent from list
            val listResponse = restTemplate.exchange(
                "/api/plans/$planId/log-book/faqs",
                HttpMethod.GET,
                entityWithUser(null, ownerId),
                Array<LogBookFaqResponse>::class.java
            )
            assertThat(listResponse.body!!).isEmpty()
        }

        @Test
        fun `DELETE returns 404 when FAQ not found`() {
            val response = restTemplate.exchange(
                "/api/plans/$planId/log-book/faqs/${UUID.randomUUID()}",
                HttpMethod.DELETE,
                entityWithUser(null, ownerId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @Test
        fun `DELETE returns 403 for member who is not the asker`() {
            val faqId = fixture.insertFaq(planId = planId, question = "Member's question", askedById = ownerId)
            val otherMemberId = fixture.insertUser(email = "other@example.com")
            fixture.insertPlanMember(planId = planId, userId = otherMemberId, role = "member")

            val response = restTemplate.exchange(
                "/api/plans/$planId/log-book/faqs/$faqId",
                HttpMethod.DELETE,
                entityWithUser(null, otherMemberId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        }
    }

    // ── Journal Entry Endpoints ────────────────────────────────────────

    @Nested
    inner class CreateJournalEntry {
        @Test
        fun `POST creates journal entry and returns 201 with auto page number`() {
            val response = restTemplate.exchange(
                "/api/plans/$planId/log-book/journal",
                HttpMethod.POST,
                entityWithUser(CreateJournalEntryRequest(content = "Day one at camp!"), memberId),
                LogBookJournalEntryResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            val body = response.body!!
            assertThat(body.content).isEqualTo("Day one at camp!")
            assertThat(body.planId).isEqualTo(planId)
            assertThat(body.userId).isEqualTo(memberId)
            assertThat(body.pageNumber).isEqualTo(1)
            assertThat(body.id).isNotNull()
        }

        @Test
        fun `POST returns 400 when content is blank`() {
            val response = restTemplate.exchange(
                "/api/plans/$planId/log-book/journal",
                HttpMethod.POST,
                entityWithUser(CreateJournalEntryRequest(content = ""), memberId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @Nested
    inner class UpdateJournalEntry {
        @Test
        fun `PUT updates journal entry and returns 200`() {
            val entryId = fixture.insertJournalEntry(planId = planId, userId = memberId, pageNumber = 1, content = "Original")

            val response = restTemplate.exchange(
                "/api/plans/$planId/log-book/journal/$entryId",
                HttpMethod.PUT,
                entityWithUser(UpdateJournalEntryRequest(content = "Updated"), memberId),
                LogBookJournalEntryResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val body = response.body!!
            assertThat(body.id).isEqualTo(entryId)
            assertThat(body.content).isEqualTo("Updated")
            assertThat(body.pageNumber).isEqualTo(1)
        }

        @Test
        fun `PUT returns 403 when non-author tries to update`() {
            val entryId = fixture.insertJournalEntry(planId = planId, userId = memberId, pageNumber = 1, content = "My entry")

            val response = restTemplate.exchange(
                "/api/plans/$planId/log-book/journal/$entryId",
                HttpMethod.PUT,
                entityWithUser(UpdateJournalEntryRequest(content = "Hijack"), ownerId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        }

        @Test
        fun `PUT returns 400 for blank content`() {
            val entryId = fixture.insertJournalEntry(planId = planId, userId = memberId, pageNumber = 1, content = "Original")

            val response = restTemplate.exchange(
                "/api/plans/$planId/log-book/journal/$entryId",
                HttpMethod.PUT,
                entityWithUser(UpdateJournalEntryRequest(content = ""), memberId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        fun `PUT returns 404 for non-existent entry`() {
            val response = restTemplate.exchange(
                "/api/plans/$planId/log-book/journal/${UUID.randomUUID()}",
                HttpMethod.PUT,
                entityWithUser(UpdateJournalEntryRequest(content = "Updated"), memberId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @Nested
    inner class GetJournalEntries {
        @Test
        fun `GET returns 200 with entries ordered by page_number`() {
            fixture.insertJournalEntry(planId = planId, userId = memberId, pageNumber = 2, content = "Page 2")
            fixture.insertJournalEntry(planId = planId, userId = memberId, pageNumber = 1, content = "Page 1")
            fixture.insertJournalEntry(planId = planId, userId = memberId, pageNumber = 3, content = "Page 3")

            val response = restTemplate.exchange(
                "/api/plans/$planId/log-book/journal",
                HttpMethod.GET,
                entityWithUser(null, memberId),
                Array<LogBookJournalEntryResponse>::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val entries = response.body!!
            assertThat(entries).hasSize(3)
            assertThat(entries[0].pageNumber).isEqualTo(1)
            assertThat(entries[0].content).isEqualTo("Page 1")
            assertThat(entries[1].pageNumber).isEqualTo(2)
            assertThat(entries[2].pageNumber).isEqualTo(3)
        }
    }

    @Nested
    inner class DeleteJournalEntry {
        @Test
        fun `DELETE removes journal entry and returns 204`() {
            val entryId = fixture.insertJournalEntry(planId = planId, userId = memberId, pageNumber = 1, content = "To be deleted")

            val response = restTemplate.exchange(
                "/api/plans/$planId/log-book/journal/$entryId",
                HttpMethod.DELETE,
                entityWithUser(null, ownerId),
                Void::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)

            // Verify entry is absent from list
            val listResponse = restTemplate.exchange(
                "/api/plans/$planId/log-book/journal",
                HttpMethod.GET,
                entityWithUser(null, ownerId),
                Array<LogBookJournalEntryResponse>::class.java
            )
            assertThat(listResponse.body!!).isEmpty()
        }

        @Test
        fun `DELETE returns 404 when entry not found`() {
            val response = restTemplate.exchange(
                "/api/plans/$planId/log-book/journal/${UUID.randomUUID()}",
                HttpMethod.DELETE,
                entityWithUser(null, ownerId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @Test
        fun `DELETE returns 403 for member who is not the author`() {
            val entryId = fixture.insertJournalEntry(planId = planId, userId = ownerId, pageNumber = 1, content = "Owner's entry")
            val otherMemberId = fixture.insertUser(email = "other-journal@example.com")
            fixture.insertPlanMember(planId = planId, userId = otherMemberId, role = "member")

            val response = restTemplate.exchange(
                "/api/plans/$planId/log-book/journal/$entryId",
                HttpMethod.DELETE,
                entityWithUser(null, otherMemberId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        }
    }

    // ── Read-Your-Own-Writes Flows ─────────────────────────────────────

    @Nested
    inner class ReadYourOwnWrites {

        @Test
        fun `ask FAQ then answer it then get FAQs shows answered FAQ`() {
            // 1. Ask FAQ
            val askResponse = restTemplate.exchange(
                "/api/plans/$planId/log-book/faqs",
                HttpMethod.POST,
                entityWithUser(AskFaqRequest(question = "When do we leave?"), memberId),
                LogBookFaqResponse::class.java
            )
            assertThat(askResponse.statusCode).isEqualTo(HttpStatus.CREATED)
            val faqId = askResponse.body!!.id

            // 2. Answer it
            val answerResponse = restTemplate.exchange(
                "/api/plans/$planId/log-book/faqs/$faqId/answer",
                HttpMethod.PUT,
                entityWithUser(AnswerFaqRequest(answer = "Friday at 8am"), ownerId),
                LogBookFaqResponse::class.java
            )
            assertThat(answerResponse.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(answerResponse.body!!.answer).isEqualTo("Friday at 8am")

            // 3. Get FAQs and verify
            val listResponse = restTemplate.exchange(
                "/api/plans/$planId/log-book/faqs",
                HttpMethod.GET,
                entityWithUser(null, memberId),
                Array<LogBookFaqResponse>::class.java
            )
            assertThat(listResponse.statusCode).isEqualTo(HttpStatus.OK)
            val faqs = listResponse.body!!
            assertThat(faqs).hasSize(1)
            assertThat(faqs[0].question).isEqualTo("When do we leave?")
            assertThat(faqs[0].answer).isEqualTo("Friday at 8am")
            assertThat(faqs[0].answeredById).isEqualTo(ownerId)
        }

        @Test
        fun `create multiple journal entries then get journal verifies page ordering`() {
            // 1. Create entries
            val entry1 = restTemplate.exchange(
                "/api/plans/$planId/log-book/journal",
                HttpMethod.POST,
                entityWithUser(CreateJournalEntryRequest(content = "First entry"), memberId),
                LogBookJournalEntryResponse::class.java
            )
            assertThat(entry1.statusCode).isEqualTo(HttpStatus.CREATED)
            assertThat(entry1.body!!.pageNumber).isEqualTo(1)

            val entry2 = restTemplate.exchange(
                "/api/plans/$planId/log-book/journal",
                HttpMethod.POST,
                entityWithUser(CreateJournalEntryRequest(content = "Second entry"), memberId),
                LogBookJournalEntryResponse::class.java
            )
            assertThat(entry2.statusCode).isEqualTo(HttpStatus.CREATED)
            assertThat(entry2.body!!.pageNumber).isEqualTo(2)

            // 2. Get journal and verify ordering
            val listResponse = restTemplate.exchange(
                "/api/plans/$planId/log-book/journal",
                HttpMethod.GET,
                entityWithUser(null, memberId),
                Array<LogBookJournalEntryResponse>::class.java
            )
            assertThat(listResponse.statusCode).isEqualTo(HttpStatus.OK)
            val entries = listResponse.body!!
            assertThat(entries).hasSize(2)
            assertThat(entries[0].pageNumber).isEqualTo(1)
            assertThat(entries[0].content).isEqualTo("First entry")
            assertThat(entries[1].pageNumber).isEqualTo(2)
            assertThat(entries[1].content).isEqualTo("Second entry")
        }

        @Test
        fun `create journal entry then update it then get journal shows updated content`() {
            // 1. Create entry
            val createResponse = restTemplate.exchange(
                "/api/plans/$planId/log-book/journal",
                HttpMethod.POST,
                entityWithUser(CreateJournalEntryRequest(content = "Original content"), memberId),
                LogBookJournalEntryResponse::class.java
            )
            assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
            val entryId = createResponse.body!!.id

            // 2. Update it
            val updateResponse = restTemplate.exchange(
                "/api/plans/$planId/log-book/journal/$entryId",
                HttpMethod.PUT,
                entityWithUser(UpdateJournalEntryRequest(content = "Updated content"), memberId),
                LogBookJournalEntryResponse::class.java
            )
            assertThat(updateResponse.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(updateResponse.body!!.content).isEqualTo("Updated content")

            // 3. Get journal and verify
            val listResponse = restTemplate.exchange(
                "/api/plans/$planId/log-book/journal",
                HttpMethod.GET,
                entityWithUser(null, memberId),
                Array<LogBookJournalEntryResponse>::class.java
            )
            assertThat(listResponse.statusCode).isEqualTo(HttpStatus.OK)
            val entries = listResponse.body!!
            assertThat(entries).hasSize(1)
            assertThat(entries[0].content).isEqualTo("Updated content")
            assertThat(entries[0].pageNumber).isEqualTo(1)
        }
    }

    private fun entityWithUser(body: Any?, userId: UUID): HttpEntity<Any?> {
        val headers = HttpHeaders()
        headers.set("X-User-Id", userId.toString())
        return HttpEntity(body, headers)
    }
}
