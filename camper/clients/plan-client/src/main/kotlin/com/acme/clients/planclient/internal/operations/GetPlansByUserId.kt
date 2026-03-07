package com.acme.clients.planclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.planclient.api.GetByUserIdParam
import com.acme.clients.planclient.internal.adapters.PlanRowAdapter
import com.acme.clients.planclient.model.Plan
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetPlansByUserId(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetPlansByUserId::class.java)

    fun execute(param: GetByUserIdParam): Result<List<Plan>, AppError> {
        logger.debug("Finding plans for user id={}", param.userId)
        val entities = jdbi.withHandle<List<Plan>, Exception> { handle ->
            handle.createQuery(
                """
                SELECT p.id, p.name, p.visibility, p.owner_id, p.created_at, p.updated_at
                FROM plans p
                JOIN plan_members pm ON pm.plan_id = p.id
                WHERE pm.user_id = :userId
                ORDER BY p.created_at DESC
                """.trimIndent()
            )
                .bind("userId", param.userId)
                .map { rs, _ -> PlanRowAdapter.fromResultSet(rs) }
                .list()
        }
        return success(entities)
    }
}
