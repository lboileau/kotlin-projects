package com.acmo.clients.worldclient.internal.operations

import com.acmo.clients.common.Result
import com.acmo.clients.common.error.AppError
import com.acmo.clients.common.error.NotFoundError
import com.acmo.clients.common.failure
import com.acmo.clients.common.success
import com.acmo.clients.worldclient.api.DeleteWorldParam
import com.acmo.clients.worldclient.internal.validations.ValidateDeleteWorld
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
