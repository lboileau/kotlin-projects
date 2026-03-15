package com.acme.clients.gearpackclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.gearpackclient.api.GetAllGearPacksParam
import com.acme.clients.gearpackclient.internal.adapters.GearPackItemRowAdapter
import com.acme.clients.gearpackclient.internal.adapters.GearPackRowAdapter
import com.acme.clients.gearpackclient.internal.validations.ValidateGetAllGearPacks
import com.acme.clients.gearpackclient.model.GearPack
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetAllGearPacks(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetAllGearPacks::class.java)
    private val validate = ValidateGetAllGearPacks()

    fun execute(param: GetAllGearPacksParam): Result<List<GearPack>, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Finding all gear packs")
        val entities = jdbi.withHandle<List<GearPack>, Exception> { handle ->
            val packs = handle.createQuery(
                """
                SELECT gp.id, gp.name, gp.description, gp.created_at, gp.updated_at
                FROM gear_packs gp
                ORDER BY gp.name
                """.trimIndent()
            )
                .map { rs, _ -> GearPackRowAdapter.fromResultSet(rs) }
                .list()

            if (packs.isEmpty()) return@withHandle packs

            val items = handle.createQuery(
                """
                SELECT id, gear_pack_id, name, category, default_quantity, scalable, sort_order, created_at, updated_at
                FROM gear_pack_items
                ORDER BY gear_pack_id, sort_order
                """.trimIndent()
            )
                .map { rs, _ -> GearPackItemRowAdapter.fromResultSet(rs) }
                .list()

            val itemsByPackId = items.groupBy { it.gearPackId }
            packs.map { pack -> pack.copy(items = itemsByPackId[pack.id] ?: emptyList()) }
        }
        return success(entities)
    }
}
