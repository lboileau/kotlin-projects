package com.acme.clients.logbookclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.logbookclient.api.GetFaqsByPlanIdParam
import com.acme.clients.logbookclient.internal.adapters.LogBookFaqRowAdapter
import com.acme.clients.logbookclient.internal.validations.ValidateGetFaqsByPlanId
import com.acme.clients.logbookclient.model.LogBookFaq
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetFaqsByPlanId(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetFaqsByPlanId::class.java)
    private val validate = ValidateGetFaqsByPlanId()

    fun execute(param: GetFaqsByPlanIdParam): Result<List<LogBookFaq>, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Finding FAQs for plan id={}", param.planId)
        val entities = jdbi.withHandle<List<LogBookFaq>, Exception> { handle ->
            handle.createQuery(
                """
                SELECT id, plan_id, question, asked_by_id, answer, answered_by_id, created_at, updated_at
                FROM log_book_faqs
                WHERE plan_id = :planId
                ORDER BY created_at DESC
                """.trimIndent()
            )
                .bind("planId", param.planId)
                .map { rs, _ -> LogBookFaqRowAdapter.fromResultSet(rs) }
                .list()
        }
        return success(entities)
    }
}
