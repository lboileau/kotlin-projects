package com.acme.clients.logbookclient

import com.acme.clients.common.Result
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.error.ValidationError
import com.acme.clients.logbookclient.api.*
import com.acme.clients.logbookclient.test.LogBookTestDb
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID

@Testcontainers
class LogBookClientIntegrationTest {

    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("camper_db")
            .withUsername("postgres")
            .withPassword("postgres")

        private lateinit var client: LogBookClient
        private lateinit var jdbi: org.jdbi.v3.core.Jdbi

        @BeforeAll
        @JvmStatic
        fun setup() {
            LogBookTestDb.cleanAndMigrate(postgres.jdbcUrl, postgres.username, postgres.password)

            System.setProperty("DB_URL", postgres.jdbcUrl)
            System.setProperty("DB_USER", postgres.username)
            System.setProperty("DB_PASSWORD", postgres.password)
            client = createLogBookClient()
            jdbi = org.jdbi.v3.core.Jdbi.create(postgres.jdbcUrl, postgres.username, postgres.password)
        }
    }

    private lateinit var testUserId: UUID
    private lateinit var testUserId2: UUID
    private lateinit var testPlanId: UUID

    @BeforeEach
    fun truncateAndSeed() {
        jdbi.withHandle<Unit, Exception> { handle ->
            handle.createUpdate("TRUNCATE TABLE log_book_journal_entries, log_book_faqs, plan_members, plans, users CASCADE").execute()
        }
        testUserId = UUID.randomUUID()
        testUserId2 = UUID.randomUUID()
        testPlanId = UUID.randomUUID()
        jdbi.withHandle<Unit, Exception> { handle ->
            handle.createUpdate("INSERT INTO users (id, email, username) VALUES (:id, :email, :username)")
                .bind("id", testUserId).bind("email", "test@example.com").bind("username", "testuser").execute()
            handle.createUpdate("INSERT INTO users (id, email, username) VALUES (:id, :email, :username)")
                .bind("id", testUserId2).bind("email", "test2@example.com").bind("username", "testuser2").execute()
            handle.createUpdate("INSERT INTO plans (id, name, visibility, owner_id) VALUES (:id, :name, :visibility, :ownerId)")
                .bind("id", testPlanId).bind("name", "Test Plan").bind("visibility", "private").bind("ownerId", testUserId).execute()
        }
    }

    // ── FAQ Tests ──────────────────────────────────────────────────────

    @Nested
    inner class CreateFaq {
        @Test
        fun `createFaq creates with correct fields, answer is null, answeredById is null`() {
            val result = client.createFaq(
                CreateFaqParam(planId = testPlanId, question = "Where is the campsite?", askedById = testUserId)
            )
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val faq = (result as Result.Success).value
            assertThat(faq.planId).isEqualTo(testPlanId)
            assertThat(faq.question).isEqualTo("Where is the campsite?")
            assertThat(faq.askedById).isEqualTo(testUserId)
            assertThat(faq.answer).isNull()
            assertThat(faq.answeredById).isNull()
            assertThat(faq.id).isNotNull()
            assertThat(faq.createdAt).isNotNull()
            assertThat(faq.updatedAt).isNotNull()
        }

        @Test
        fun `createFaq returns validation error when question is blank`() {
            val result = client.createFaq(
                CreateFaqParam(planId = testPlanId, question = "", askedById = testUserId)
            )
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
            assertThat((error as ValidationError).field).isEqualTo("question")
        }
    }

    @Nested
    inner class AnswerFaq {
        @Test
        fun `answerFaq updates answer and answeredById`() {
            val created = (client.createFaq(
                CreateFaqParam(planId = testPlanId, question = "What to bring?", askedById = testUserId)
            ) as Result.Success).value

            val result = client.answerFaq(
                AnswerFaqParam(id = created.id, answer = "Bring warm clothes", answeredById = testUserId2)
            )
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val answered = (result as Result.Success).value
            assertThat(answered.id).isEqualTo(created.id)
            assertThat(answered.answer).isEqualTo("Bring warm clothes")
            assertThat(answered.answeredById).isEqualTo(testUserId2)
            assertThat(answered.question).isEqualTo("What to bring?")
        }

        @Test
        fun `answerFaq returns not found for non-existent FAQ`() {
            val result = client.answerFaq(
                AnswerFaqParam(id = UUID.randomUUID(), answer = "Some answer", answeredById = testUserId)
            )
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(NotFoundError::class.java)
        }

        @Test
        fun `answerFaq re-answering updates answer and answeredById`() {
            val created = (client.createFaq(
                CreateFaqParam(planId = testPlanId, question = "What time do we leave?", askedById = testUserId)
            ) as Result.Success).value

            // First answer
            val firstAnswer = (client.answerFaq(
                AnswerFaqParam(id = created.id, answer = "8am", answeredById = testUserId)
            ) as Result.Success).value
            assertThat(firstAnswer.answer).isEqualTo("8am")
            assertThat(firstAnswer.answeredById).isEqualTo(testUserId)

            // Re-answer with different user and different answer
            val secondAnswer = (client.answerFaq(
                AnswerFaqParam(id = created.id, answer = "Actually 9am", answeredById = testUserId2)
            ) as Result.Success).value
            assertThat(secondAnswer.answer).isEqualTo("Actually 9am")
            assertThat(secondAnswer.answeredById).isEqualTo(testUserId2)
            assertThat(secondAnswer.question).isEqualTo("What time do we leave?")
        }

        @Test
        fun `answerFaq returns validation error when answer is blank`() {
            val created = (client.createFaq(
                CreateFaqParam(planId = testPlanId, question = "What to bring?", askedById = testUserId)
            ) as Result.Success).value

            val result = client.answerFaq(
                AnswerFaqParam(id = created.id, answer = "", answeredById = testUserId2)
            )
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
            assertThat((error as ValidationError).field).isEqualTo("answer")
        }
    }

    @Nested
    inner class GetFaqsByPlanId {
        @Test
        fun `getFaqsByPlanId returns FAQs ordered by created_at desc`() {
            client.createFaq(CreateFaqParam(planId = testPlanId, question = "First question", askedById = testUserId))
            client.createFaq(CreateFaqParam(planId = testPlanId, question = "Second question", askedById = testUserId))
            client.createFaq(CreateFaqParam(planId = testPlanId, question = "Third question", askedById = testUserId))

            val result = client.getFaqsByPlanId(GetFaqsByPlanIdParam(testPlanId))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val faqs = (result as Result.Success).value
            assertThat(faqs).hasSize(3)
            // created_at desc means most recent first
            assertThat(faqs[0].question).isEqualTo("Third question")
            assertThat(faqs[1].question).isEqualTo("Second question")
            assertThat(faqs[2].question).isEqualTo("First question")
        }

        @Test
        fun `getFaqsByPlanId returns empty list when none exist`() {
            val result = client.getFaqsByPlanId(GetFaqsByPlanIdParam(testPlanId))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val faqs = (result as Result.Success).value
            assertThat(faqs).isEmpty()
        }

        @Test
        fun `getFaqsByPlanId only returns FAQs for the specified plan`() {
            val otherPlanId = UUID.randomUUID()
            jdbi.withHandle<Unit, Exception> { handle ->
                handle.createUpdate("INSERT INTO plans (id, name, visibility, owner_id) VALUES (:id, :name, :visibility, :ownerId)")
                    .bind("id", otherPlanId).bind("name", "Other Plan").bind("visibility", "private").bind("ownerId", testUserId).execute()
            }

            client.createFaq(CreateFaqParam(planId = testPlanId, question = "Plan A question", askedById = testUserId))
            client.createFaq(CreateFaqParam(planId = otherPlanId, question = "Plan B question", askedById = testUserId))

            val result = client.getFaqsByPlanId(GetFaqsByPlanIdParam(testPlanId))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val faqs = (result as Result.Success).value
            assertThat(faqs).hasSize(1)
            assertThat(faqs[0].question).isEqualTo("Plan A question")
        }
    }

    @Nested
    inner class DeleteFaq {
        @Test
        fun `deleteFaq deletes existing FAQ`() {
            val created = (client.createFaq(
                CreateFaqParam(planId = testPlanId, question = "To be deleted", askedById = testUserId)
            ) as Result.Success).value

            val deleteResult = client.deleteFaq(DeleteFaqParam(created.id))
            assertThat(deleteResult).isInstanceOf(Result.Success::class.java)

            // Verify it's gone by checking the list
            val listResult = client.getFaqsByPlanId(GetFaqsByPlanIdParam(testPlanId))
            val faqs = (listResult as Result.Success).value
            assertThat(faqs).isEmpty()
        }

        @Test
        fun `deleteFaq returns not found for non-existent FAQ`() {
            val result = client.deleteFaq(DeleteFaqParam(UUID.randomUUID()))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(NotFoundError::class.java)
        }
    }

    // ── Journal Entry Tests ────────────────────────────────────────────

    @Nested
    inner class CreateJournalEntry {
        @Test
        fun `createJournalEntry creates with auto-assigned page_number starting at 1`() {
            val result = client.createJournalEntry(
                CreateJournalEntryParam(planId = testPlanId, userId = testUserId, content = "Day one at camp!")
            )
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val entry = (result as Result.Success).value
            assertThat(entry.planId).isEqualTo(testPlanId)
            assertThat(entry.userId).isEqualTo(testUserId)
            assertThat(entry.content).isEqualTo("Day one at camp!")
            assertThat(entry.pageNumber).isEqualTo(1)
            assertThat(entry.id).isNotNull()
            assertThat(entry.createdAt).isNotNull()
            assertThat(entry.updatedAt).isNotNull()
        }

        @Test
        fun `createJournalEntry assigns incrementing page numbers`() {
            val entry1 = (client.createJournalEntry(
                CreateJournalEntryParam(planId = testPlanId, userId = testUserId, content = "Page 1")
            ) as Result.Success).value
            val entry2 = (client.createJournalEntry(
                CreateJournalEntryParam(planId = testPlanId, userId = testUserId, content = "Page 2")
            ) as Result.Success).value
            val entry3 = (client.createJournalEntry(
                CreateJournalEntryParam(planId = testPlanId, userId = testUserId, content = "Page 3")
            ) as Result.Success).value

            assertThat(entry1.pageNumber).isEqualTo(1)
            assertThat(entry2.pageNumber).isEqualTo(2)
            assertThat(entry3.pageNumber).isEqualTo(3)
        }

        @Test
        fun `createJournalEntry returns validation error when content is blank`() {
            val result = client.createJournalEntry(
                CreateJournalEntryParam(planId = testPlanId, userId = testUserId, content = "")
            )
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
            assertThat((error as ValidationError).field).isEqualTo("content")
        }

        @Test
        fun `createJournalEntry page numbers are per-plan`() {
            val otherPlanId = UUID.randomUUID()
            jdbi.withHandle<Unit, Exception> { handle ->
                handle.createUpdate("INSERT INTO plans (id, name, visibility, owner_id) VALUES (:id, :name, :visibility, :ownerId)")
                    .bind("id", otherPlanId).bind("name", "Other Plan").bind("visibility", "private").bind("ownerId", testUserId).execute()
            }

            val entryA = (client.createJournalEntry(
                CreateJournalEntryParam(planId = testPlanId, userId = testUserId, content = "Plan A entry")
            ) as Result.Success).value
            val entryB = (client.createJournalEntry(
                CreateJournalEntryParam(planId = otherPlanId, userId = testUserId, content = "Plan B entry")
            ) as Result.Success).value

            assertThat(entryA.pageNumber).isEqualTo(1)
            assertThat(entryB.pageNumber).isEqualTo(1)
        }
    }

    @Nested
    inner class UpdateJournalEntry {
        @Test
        fun `updateJournalEntry updates content, preserves page number`() {
            val created = (client.createJournalEntry(
                CreateJournalEntryParam(planId = testPlanId, userId = testUserId, content = "Original content")
            ) as Result.Success).value

            val result = client.updateJournalEntry(
                UpdateJournalEntryParam(id = created.id, content = "Updated content")
            )
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val updated = (result as Result.Success).value
            assertThat(updated.id).isEqualTo(created.id)
            assertThat(updated.content).isEqualTo("Updated content")
            assertThat(updated.pageNumber).isEqualTo(created.pageNumber)
            assertThat(updated.planId).isEqualTo(testPlanId)
            assertThat(updated.userId).isEqualTo(testUserId)
        }

        @Test
        fun `updateJournalEntry returns not found for non-existent entry`() {
            val result = client.updateJournalEntry(
                UpdateJournalEntryParam(id = UUID.randomUUID(), content = "Nope")
            )
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(NotFoundError::class.java)
        }

        @Test
        fun `updateJournalEntry returns validation error when content is blank`() {
            val created = (client.createJournalEntry(
                CreateJournalEntryParam(planId = testPlanId, userId = testUserId, content = "Some content")
            ) as Result.Success).value

            val result = client.updateJournalEntry(
                UpdateJournalEntryParam(id = created.id, content = "")
            )
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
            assertThat((error as ValidationError).field).isEqualTo("content")
        }
    }

    @Nested
    inner class GetJournalEntriesByPlanId {
        @Test
        fun `getJournalEntriesByPlanId returns entries ordered by page_number asc`() {
            client.createJournalEntry(CreateJournalEntryParam(planId = testPlanId, userId = testUserId, content = "Page 1"))
            client.createJournalEntry(CreateJournalEntryParam(planId = testPlanId, userId = testUserId, content = "Page 2"))
            client.createJournalEntry(CreateJournalEntryParam(planId = testPlanId, userId = testUserId, content = "Page 3"))

            val result = client.getJournalEntriesByPlanId(GetJournalEntriesByPlanIdParam(testPlanId))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val entries = (result as Result.Success).value
            assertThat(entries).hasSize(3)
            assertThat(entries[0].pageNumber).isEqualTo(1)
            assertThat(entries[0].content).isEqualTo("Page 1")
            assertThat(entries[1].pageNumber).isEqualTo(2)
            assertThat(entries[2].pageNumber).isEqualTo(3)
        }

        @Test
        fun `getJournalEntriesByPlanId returns empty list when none exist`() {
            val result = client.getJournalEntriesByPlanId(GetJournalEntriesByPlanIdParam(testPlanId))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val entries = (result as Result.Success).value
            assertThat(entries).isEmpty()
        }

        @Test
        fun `getJournalEntriesByPlanId only returns entries for the specified plan`() {
            val otherPlanId = UUID.randomUUID()
            jdbi.withHandle<Unit, Exception> { handle ->
                handle.createUpdate("INSERT INTO plans (id, name, visibility, owner_id) VALUES (:id, :name, :visibility, :ownerId)")
                    .bind("id", otherPlanId).bind("name", "Other Plan").bind("visibility", "private").bind("ownerId", testUserId).execute()
            }

            client.createJournalEntry(CreateJournalEntryParam(planId = testPlanId, userId = testUserId, content = "Plan A journal"))
            client.createJournalEntry(CreateJournalEntryParam(planId = otherPlanId, userId = testUserId, content = "Plan B journal"))

            val result = client.getJournalEntriesByPlanId(GetJournalEntriesByPlanIdParam(testPlanId))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val entries = (result as Result.Success).value
            assertThat(entries).hasSize(1)
            assertThat(entries[0].content).isEqualTo("Plan A journal")
        }
    }

    @Nested
    inner class DeleteJournalEntry {
        @Test
        fun `deleteJournalEntry deletes existing entry`() {
            val created = (client.createJournalEntry(
                CreateJournalEntryParam(planId = testPlanId, userId = testUserId, content = "To be deleted")
            ) as Result.Success).value

            val deleteResult = client.deleteJournalEntry(DeleteJournalEntryParam(created.id))
            assertThat(deleteResult).isInstanceOf(Result.Success::class.java)

            val listResult = client.getJournalEntriesByPlanId(GetJournalEntriesByPlanIdParam(testPlanId))
            val entries = (listResult as Result.Success).value
            assertThat(entries).isEmpty()
        }

        @Test
        fun `deleteJournalEntry returns not found for non-existent entry`() {
            val result = client.deleteJournalEntry(DeleteJournalEntryParam(UUID.randomUUID()))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(NotFoundError::class.java)
        }

        @Test
        fun `deleteJournalEntry middle page numbers are not reused`() {
            val entry1 = (client.createJournalEntry(
                CreateJournalEntryParam(planId = testPlanId, userId = testUserId, content = "Page 1")
            ) as Result.Success).value
            val entry2 = (client.createJournalEntry(
                CreateJournalEntryParam(planId = testPlanId, userId = testUserId, content = "Page 2")
            ) as Result.Success).value
            val entry3 = (client.createJournalEntry(
                CreateJournalEntryParam(planId = testPlanId, userId = testUserId, content = "Page 3")
            ) as Result.Success).value

            // Delete middle page (page 2)
            client.deleteJournalEntry(DeleteJournalEntryParam(entry2.id))

            // Create a new entry — MAX is still 3, so next page = 4 (page 2 gap is preserved)
            val entry4 = (client.createJournalEntry(
                CreateJournalEntryParam(planId = testPlanId, userId = testUserId, content = "Page 4")
            ) as Result.Success).value

            assertThat(entry1.pageNumber).isEqualTo(1)
            assertThat(entry3.pageNumber).isEqualTo(3)
            assertThat(entry4.pageNumber).isEqualTo(4)

            // Verify the list shows pages 1, 3, 4 (no page 2)
            val listResult = client.getJournalEntriesByPlanId(GetJournalEntriesByPlanIdParam(testPlanId))
            val entries = (listResult as Result.Success).value
            assertThat(entries).hasSize(3)
            assertThat(entries.map { it.pageNumber }).containsExactly(1, 3, 4)
        }
    }
}
