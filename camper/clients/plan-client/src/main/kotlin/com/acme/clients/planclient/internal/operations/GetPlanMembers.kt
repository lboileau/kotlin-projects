package com.acme.clients.planclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.planclient.api.GetMembersParam
import com.acme.clients.planclient.internal.adapters.PlanMemberRowAdapter
import com.acme.clients.planclient.model.PlanMember
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetPlanMembers(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetPlanMembers::class.java)

    fun execute(param: GetMembersParam): Result<List<PlanMember>, AppError> {
        logger.debug("Finding members for plan id={}", param.planId)
        val entities = jdbi.withHandle<List<PlanMember>, Exception> { handle ->
            handle.createQuery(
                """
                SELECT plan_id, user_id, created_at
                FROM plan_members
                WHERE plan_id = :planId
                ORDER BY created_at
                """.trimIndent()
            )
                .bind("planId", param.planId)
                .map { rs, _ -> PlanMemberRowAdapter.fromResultSet(rs) }
                .list()
        }
        return success(entities)
    }
}
