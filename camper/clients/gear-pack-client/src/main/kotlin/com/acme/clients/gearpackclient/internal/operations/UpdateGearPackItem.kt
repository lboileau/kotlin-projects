package com.acme.clients.gearpackclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ConflictError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.gearpackclient.api.UpdateGearPackItemParam
import com.acme.clients.gearpackclient.internal.adapters.GearPackItemRowAdapter
import com.acme.clients.gearpackclient.internal.validations.ValidateUpdateGearPackItem
import com.acme.clients.gearpackclient.model.GearPackItem
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant

internal class UpdateGearPackItem(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(UpdateGearPackItem::class.java)
    private val validate = ValidateUpdateGearPackItem()

    fun execute(param: UpdateGearPackItemParam): Result<GearPackItem, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        val existing = jdbi.withHandle<GearPackItem?, Exception> { handle ->
            handle.createQuery(
                """
                SELECT id, gear_pack_id, name, category, default_quantity, scalable, sort_order, created_at, updated_at
                FROM gear_pack_items
                WHERE id = :id AND gear_pack_id = :gearPackId
                """.trimIndent()
            )
                .bind("id", param.id)
                .bind("gearPackId", param.gearPackId)
                .map { rs, _ -> GearPackItemRowAdapter.fromResultSet(rs) }
                .findOne()
                .orElse(null)
        }
        if (existing == null) return failure(NotFoundError("GearPackItem", param.id.toString()))

        logger.debug("Updating gear pack item id={}", param.id)
        return try {
            val sets = mutableListOf<String>()
            if (param.name != null) sets.add("name = :name")
            if (param.category != null) sets.add("category = :category")
            if (param.defaultQuantity != null) sets.add("default_quantity = :defaultQuantity")
            if (param.scalable != null) sets.add("scalable = :scalable")
            sets.add("updated_at = :updatedAt")

            val entity = jdbi.withHandle<GearPackItem, Exception> { handle ->
                val now = Instant.now()
                handle.createUpdate("UPDATE gear_pack_items SET ${sets.joinToString(", ")} WHERE id = :id AND gear_pack_id = :gearPackId")
                    .bind("id", param.id)
                    .bind("gearPackId", param.gearPackId)
                    .also { q -> if (param.name != null) q.bind("name", param.name) }
                    .also { q -> if (param.category != null) q.bind("category", param.category) }
                    .also { q -> if (param.defaultQuantity != null) q.bind("defaultQuantity", param.defaultQuantity) }
                    .also { q -> if (param.scalable != null) q.bind("scalable", param.scalable) }
                    .bind("updatedAt", now)
                    .execute()

                handle.createQuery(
                    """
                    SELECT id, gear_pack_id, name, category, default_quantity, scalable, sort_order, created_at, updated_at
                    FROM gear_pack_items
                    WHERE id = :id
                    """.trimIndent()
                )
                    .bind("id", param.id)
                    .map { rs, _ -> GearPackItemRowAdapter.fromResultSet(rs) }
                    .one()
            }
            Result.Success(entity)
        } catch (e: Exception) {
            if (e.message?.contains("uq_gear_pack_items_pack_name") == true || e.message?.contains("duplicate key") == true) {
                failure(ConflictError("GearPackItem", "name '${param.name}' already exists in this gear pack"))
            } else {
                throw e
            }
        }
    }
}
