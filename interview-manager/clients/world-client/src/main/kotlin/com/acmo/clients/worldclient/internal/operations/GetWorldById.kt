package com.acmo.clients.worldclient.internal.operations

import com.acmo.clients.common.Result
import com.acmo.clients.common.error.AppError
import com.acmo.clients.common.error.NotFoundError
import com.acmo.clients.common.failure
import com.acmo.clients.common.success
import com.acmo.clients.worldclient.api.GetByIdParam
import com.acmo.clients.worldclient.internal.adapters.WorldRowAdapter
import com.acmo.clients.worldclient.internal.validations.ValidateGetWorldById
import com.acmo.clients.worldclient.model.World
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetWorldById(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetWorldById::class.java)
    private val validate = ValidateGetWorldById()

    fun execute(param: GetByIdParam): Result<World, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

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
