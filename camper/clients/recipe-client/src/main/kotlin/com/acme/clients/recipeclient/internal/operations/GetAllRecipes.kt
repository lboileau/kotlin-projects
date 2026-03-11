package com.acme.clients.recipeclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.recipeclient.api.GetAllParam
import com.acme.clients.recipeclient.internal.adapters.RecipeRowAdapter
import com.acme.clients.recipeclient.model.Recipe
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetAllRecipes(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetAllRecipes::class.java)

    fun execute(param: GetAllParam): Result<List<Recipe>, AppError> {
        logger.debug("Fetching recipes status={} createdBy={}", param.status, param.createdBy)
        val entities = jdbi.withHandle<List<Recipe>, Exception> { handle ->
            val conditions = mutableListOf<String>()
            if (param.status != null) conditions.add("status = :status")
            if (param.createdBy != null) conditions.add("created_by = :createdBy")
            val where = if (conditions.isEmpty()) "" else "WHERE ${conditions.joinToString(" AND ")}"
            val query = handle.createQuery(
                "SELECT id, name, description, web_link, base_servings, status, created_by, duplicate_of_id, created_at, updated_at FROM recipes $where ORDER BY name"
            )
            if (param.status != null) query.bind("status", param.status)
            if (param.createdBy != null) query.bind("createdBy", param.createdBy)
            query.map { rs, _ -> RecipeRowAdapter.fromResultSet(rs) }.list()
        }
        return success(entities)
    }
}
