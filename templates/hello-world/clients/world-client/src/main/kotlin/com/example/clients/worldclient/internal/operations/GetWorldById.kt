package com.example.clients.worldclient.internal.operations

import com.example.clients.common.Result
import com.example.clients.common.error.AppError
import com.example.clients.common.error.NotFoundError
import com.example.clients.common.failure
import com.example.clients.common.success
import com.example.clients.worldclient.api.GetByIdParam
import com.example.clients.worldclient.internal.adapters.WorldRowAdapter
import com.example.clients.worldclient.internal.validations.ValidateGetWorldById
import com.example.clients.worldclient.model.World
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetWorldById(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetWorldById::class.java)
    private val validate = ValidateGetWorldById()

    fun execute(param: GetByIdParam): Result<World, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation as Result<World, AppError>

        logger.debug("Finding world by id={}", param.id)
        val entity = jdbi.withHandle<World?, Exception> { handle ->
            handle.createQuery("SELECT id, name, greeting, created_at, updated_at FROM worlds WHERE id = :id")
                .bind("id", param.id)
                .map { rs, _ -> WorldRowAdapter.fromResultSet(rs) }
                .findOne()
                .orElse(null)
        }
        return if (entity != null) success(entity) else failure(NotFoundError("World", param.id.toString()))
    }
}
