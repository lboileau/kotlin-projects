package com.acme.clients.planclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.planclient.api.GetPublicPlansParam
import com.acme.clients.planclient.internal.adapters.PlanRowAdapter
import com.acme.clients.planclient.model.Plan
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetPublicPlans(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetPublicPlans::class.java)

    fun execute(param: GetPublicPlansParam): Result<List<Plan>, AppError> {
        logger.debug("Finding all public plans")
        val entities = jdbi.withHandle<List<Plan>, Exception> { handle ->
            handle.createQuery(
                """
                SELECT id, name, visibility, owner_id, created_at, updated_at
                FROM plans
                WHERE visibility = 'public'
                ORDER BY created_at DESC
                """.trimIndent()
            )
                .map { rs, _ -> PlanRowAdapter.fromResultSet(rs) }
                .list()
        }
        return success(entities)
    }
}
