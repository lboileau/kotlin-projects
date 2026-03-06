package com.example.clients.worldclient.internal.operations

import com.example.clients.common.Result
import com.example.clients.common.error.AppError
import com.example.clients.common.error.NotFoundError
import com.example.clients.common.failure
import com.example.clients.common.success
import com.example.clients.worldclient.api.DeleteWorldParam
import com.example.clients.worldclient.internal.validations.ValidateDeleteWorld
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class DeleteWorld(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(DeleteWorld::class.java)
    private val validate = ValidateDeleteWorld()

    fun execute(param: DeleteWorldParam): Result<Unit, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Deleting world id={}", param.id)
        val rowsAffected = jdbi.withHandle<Int, Exception> { handle ->
            handle.createUpdate("DELETE FROM worlds WHERE id = :id")
                .bind("id", param.id)
                .execute()
        }
        return if (rowsAffected > 0) success(Unit) else failure(NotFoundError("World", param.id.toString()))
    }
}
