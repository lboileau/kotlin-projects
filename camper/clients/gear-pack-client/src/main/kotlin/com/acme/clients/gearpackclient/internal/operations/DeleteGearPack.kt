package com.acme.clients.gearpackclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.gearpackclient.api.DeleteGearPackParam
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class DeleteGearPack(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(DeleteGearPack::class.java)

    fun execute(param: DeleteGearPackParam): Result<Unit, AppError> {
        logger.debug("Deleting gear pack id={}", param.id)
        val rowsAffected = jdbi.withHandle<Int, Exception> { handle ->
            handle.createUpdate("DELETE FROM gear_packs WHERE id = :id")
                .bind("id", param.id)
                .execute()
        }
        return if (rowsAffected > 0) success(Unit) else failure(NotFoundError("GearPack", param.id.toString()))
    }
}
