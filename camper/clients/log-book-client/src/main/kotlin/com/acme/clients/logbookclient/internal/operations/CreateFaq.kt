package com.acme.clients.logbookclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.logbookclient.api.CreateFaqParam
import com.acme.clients.logbookclient.internal.validations.ValidateCreateFaq
import com.acme.clients.logbookclient.model.LogBookFaq
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

internal class CreateFaq(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(CreateFaq::class.java)
    private val validate = ValidateCreateFaq()

    fun execute(param: CreateFaqParam): Result<LogBookFaq, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Creating FAQ for plan id={}", param.planId)
        val entity = jdbi.withHandle<LogBookFaq, Exception> { handle ->
            val id = UUID.randomUUID()
            val now = Instant.now()
            handle.createUpdate(
                """
                INSERT INTO log_book_faqs (id, plan_id, question, asked_by_id, created_at, updated_at)
                VALUES (:id, :planId, :question, :askedById, :createdAt, :updatedAt)
                """.trimIndent()
            )
                .bind("id", id)
                .bind("planId", param.planId)
                .bind("question", param.question)
                .bind("askedById", param.askedById)
                .bind("createdAt", now)
                .bind("updatedAt", now)
                .execute()
            LogBookFaq(
                id = id,
                planId = param.planId,
                question = param.question,
                askedById = param.askedById,
                answer = null,
                answeredById = null,
                createdAt = now,
                updatedAt = now,
            )
        }
        return success(entity)
    }
}
