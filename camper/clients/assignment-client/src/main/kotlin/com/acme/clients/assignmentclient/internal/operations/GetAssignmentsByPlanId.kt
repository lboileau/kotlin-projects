package com.acme.clients.assignmentclient.internal.operations

import com.acme.clients.assignmentclient.api.GetByPlanIdParam
import com.acme.clients.assignmentclient.internal.adapters.AssignmentRowAdapter
import com.acme.clients.assignmentclient.model.Assignment
import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetAssignmentsByPlanId(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetAssignmentsByPlanId::class.java)

    fun execute(param: GetByPlanIdParam): Result<List<Assignment>, AppError> {
        logger.debug("Finding assignments for plan id={} type={}", param.planId, param.type)
        val entities = jdbi.withHandle<List<Assignment>, Exception> { handle ->
            val sql = buildString {
                append("SELECT id, plan_id, name, type, max_occupancy, owner_id, created_at, updated_at FROM assignments WHERE plan_id = :planId")
                if (param.type != null) append(" AND type = :type")
                append(" ORDER BY name")
            }
            val query = handle.createQuery(sql).bind("planId", param.planId)
            if (param.type != null) query.bind("type", param.type)
            query.map { rs, _ -> AssignmentRowAdapter.fromResultSet(rs) }.list()
        }
        return success(entities)
    }
}
