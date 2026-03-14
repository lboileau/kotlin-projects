package com.acme.clients.logbookclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.logbookclient.api.AnswerFaqParam
import com.acme.clients.logbookclient.internal.adapters.LogBookFaqRowAdapter
import com.acme.clients.logbookclient.internal.validations.ValidateAnswerFaq
import com.acme.clients.logbookclient.model.LogBookFaq
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant

internal class AnswerFaq(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(AnswerFaq::class.java)
    private val validate = ValidateAnswerFaq()

    fun execute(param: AnswerFaqParam): Result<LogBookFaq, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Answering FAQ id={}", param.id)
        val now = Instant.now()
        val rowsAffected = jdbi.withHandle<Int, Exception> { handle ->
            handle.createUpdate(
                """
                UPDATE log_book_faqs
                SET answer = :answer, answered_by_id = :answeredById, updated_at = :updatedAt
                WHERE id = :id
                """.trimIndent()
            )
                .bind("id", param.id)
                .bind("answer", param.answer)
                .bind("answeredById", param.answeredById)
                .bind("updatedAt", now)
                .execute()
        }
        if (rowsAffected == 0) return failure(NotFoundError("LogBookFaq", param.id.toString()))

        val entity = jdbi.withHandle<LogBookFaq?, Exception> { handle ->
            handle.createQuery(
                """
                SELECT id, plan_id, question, asked_by_id, answer, answered_by_id, created_at, updated_at
                FROM log_book_faqs WHERE id = :id
                """.trimIndent()
            )
                .bind("id", param.id)
                .map { rs, _ -> LogBookFaqRowAdapter.fromResultSet(rs) }
                .findOne()
                .orElse(null)
        }
        return if (entity != null) success(entity) else failure(NotFoundError("LogBookFaq", param.id.toString()))
    }
}
