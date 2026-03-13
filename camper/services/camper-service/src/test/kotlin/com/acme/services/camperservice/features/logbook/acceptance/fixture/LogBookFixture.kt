package com.acme.services.camperservice.features.logbook.acceptance.fixture

import org.springframework.jdbc.core.JdbcTemplate
import java.time.Instant
import java.util.UUID

class LogBookFixture(private val jdbcTemplate: JdbcTemplate) {

    fun insertUser(
        id: UUID = UUID.randomUUID(),
        email: String = "user-${UUID.randomUUID().toString().take(8)}@example.com",
        createdAt: Instant = Instant.now(),
        updatedAt: Instant = Instant.now()
    ): UUID {
        jdbcTemplate.update(
            "INSERT INTO users (id, email, created_at, updated_at) VALUES (?, ?, ?, ?)",
            id, email, java.sql.Timestamp.from(createdAt), java.sql.Timestamp.from(updatedAt)
        )
        return id
    }

    fun insertPlan(
        id: UUID = UUID.randomUUID(),
        name: String = "Plan-${UUID.randomUUID().toString().take(8)}",
        ownerId: UUID,
        createdAt: Instant = Instant.now(),
        updatedAt: Instant = Instant.now()
    ): UUID {
        jdbcTemplate.update(
            "INSERT INTO plans (id, name, visibility, owner_id, created_at, updated_at) VALUES (?, ?, 'private', ?, ?, ?)",
            id, name, ownerId, java.sql.Timestamp.from(createdAt), java.sql.Timestamp.from(updatedAt)
        )
        return id
    }

    fun insertPlanMember(
        planId: UUID,
        userId: UUID,
        role: String = "member",
        createdAt: Instant = Instant.now()
    ) {
        jdbcTemplate.update(
            "INSERT INTO plan_members (plan_id, user_id, role, created_at) VALUES (?, ?, ?, ?)",
            planId, userId, role, java.sql.Timestamp.from(createdAt)
        )
    }

    fun insertFaq(
        id: UUID = UUID.randomUUID(),
        planId: UUID,
        question: String,
        askedById: UUID,
        answer: String? = null,
        answeredById: UUID? = null,
        createdAt: Instant = Instant.now(),
        updatedAt: Instant = Instant.now()
    ): UUID {
        jdbcTemplate.update(
            "INSERT INTO log_book_faqs (id, plan_id, question, asked_by_id, answer, answered_by_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            id, planId, question, askedById, answer, answeredById, java.sql.Timestamp.from(createdAt), java.sql.Timestamp.from(updatedAt)
        )
        return id
    }

    fun insertJournalEntry(
        id: UUID = UUID.randomUUID(),
        planId: UUID,
        userId: UUID,
        pageNumber: Int,
        content: String,
        createdAt: Instant = Instant.now(),
        updatedAt: Instant = Instant.now()
    ): UUID {
        jdbcTemplate.update(
            "INSERT INTO log_book_journal_entries (id, plan_id, user_id, page_number, content, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
            id, planId, userId, pageNumber, content, java.sql.Timestamp.from(createdAt), java.sql.Timestamp.from(updatedAt)
        )
        return id
    }

    fun truncateAll() {
        jdbcTemplate.execute("TRUNCATE TABLE log_book_journal_entries, log_book_faqs, plan_members, plans, users CASCADE")
    }
}
