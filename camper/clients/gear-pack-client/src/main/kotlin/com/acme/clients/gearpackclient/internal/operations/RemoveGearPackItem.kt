package com.acme.clients.gearpackclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.gearpackclient.api.RemoveGearPackItemParam
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class RemoveGearPackItem(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(RemoveGearPackItem::class.java)

    fun execute(param: RemoveGearPackItemParam): Result<Unit, AppError> {
        logger.debug("Removing gear pack item id={} from gear pack id={}", param.id, param.gearPackId)
        val rowsAffected = jdbi.withHandle<Int, Exception> { handle ->
            handle.createUpdate("DELETE FROM gear_pack_items WHERE id = :id AND gear_pack_id = :gearPackId")
                .bind("id", param.id)
                .bind("gearPackId", param.gearPackId)
                .execute()
        }
        return if (rowsAffected > 0) success(Unit) else failure(NotFoundError("GearPackItem", param.id.toString()))
    }
}
