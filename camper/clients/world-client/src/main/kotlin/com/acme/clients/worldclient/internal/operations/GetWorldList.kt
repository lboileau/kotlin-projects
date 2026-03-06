package com.acme.clients.worldclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.worldclient.api.GetListParam
import com.acme.clients.worldclient.internal.adapters.WorldRowAdapter
import com.acme.clients.worldclient.internal.validations.ValidateGetWorldList
import com.acme.clients.worldclient.model.World
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetWorldList(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetWorldList::class.java)
    private val validate = ValidateGetWorldList()

    private companion object {
        const val MAX_LIMIT = 100
    }

    fun execute(param: GetListParam): Result<List<World>, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        val effectiveLimit = minOf(param.limit ?: MAX_LIMIT, MAX_LIMIT)

        logger.debug("Finding all worlds")
        val entities = jdbi.withHandle<List<World>, Exception> { handle ->
            val sql = buildString {
                append("SELECT id, name, greeting, created_at, updated_at FROM worlds ORDER BY name")
                append(" LIMIT :limit")
                if (param.offset != null) append(" OFFSET :offset")
            }
            val query = handle.createQuery(sql)
            query.bind("limit", effectiveLimit)
            if (param.offset != null) query.bind("offset", param.offset)
            query.map { rs, _ -> WorldRowAdapter.fromResultSet(rs) }.list()
        }
        return success(entities)
    }
}
