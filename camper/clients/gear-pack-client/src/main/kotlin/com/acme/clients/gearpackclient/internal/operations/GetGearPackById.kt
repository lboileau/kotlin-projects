package com.acme.clients.gearpackclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.gearpackclient.api.GetGearPackByIdParam
import com.acme.clients.gearpackclient.internal.adapters.GearPackItemRowAdapter
import com.acme.clients.gearpackclient.internal.adapters.GearPackRowAdapter
import com.acme.clients.gearpackclient.internal.validations.ValidateGetGearPackById
import com.acme.clients.gearpackclient.model.GearPack
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetGearPackById(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetGearPackById::class.java)
    private val validate = ValidateGetGearPackById()

    fun execute(param: GetGearPackByIdParam): Result<GearPack, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Finding gear pack by id={}", param.id)
        val pack = jdbi.withHandle<GearPack?, Exception> { handle ->
            val gearPack = handle.createQuery(
                "SELECT id, name, description, created_at, updated_at FROM gear_packs WHERE id = :id"
            )
                .bind("id", param.id)
                .map { rs, _ -> GearPackRowAdapter.fromResultSet(rs) }
                .findOne()
                .orElse(null)
                ?: return@withHandle null

            val items = handle.createQuery(
                """
                SELECT id, gear_pack_id, name, category, default_quantity, scalable, sort_order, created_at, updated_at
                FROM gear_pack_items
                WHERE gear_pack_id = :gearPackId
                ORDER BY sort_order
                """.trimIndent()
            )
                .bind("gearPackId", param.id)
                .map { rs, _ -> GearPackItemRowAdapter.fromResultSet(rs) }
                .list()

            gearPack.copy(items = items)
        }
        return if (pack != null) success(pack) else failure(NotFoundError("GearPack", param.id.toString()))
    }
}
