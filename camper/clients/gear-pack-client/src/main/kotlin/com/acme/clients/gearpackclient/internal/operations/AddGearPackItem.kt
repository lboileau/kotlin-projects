package com.acme.clients.gearpackclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ConflictError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.gearpackclient.api.AddGearPackItemParam
import com.acme.clients.gearpackclient.internal.validations.ValidateAddGearPackItem
import com.acme.clients.gearpackclient.model.GearPackItem
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

internal class AddGearPackItem(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(AddGearPackItem::class.java)
    private val validate = ValidateAddGearPackItem()

    fun execute(param: AddGearPackItemParam): Result<GearPackItem, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Adding item name={} to gear pack id={}", param.name, param.gearPackId)
        return try {
            val entity = jdbi.withHandle<GearPackItem, Exception> { handle ->
                val id = UUID.randomUUID()
                val now = Instant.now()

                val sortOrder = handle.createQuery(
                    """
                    INSERT INTO gear_pack_items (id, gear_pack_id, name, category, default_quantity, scalable, sort_order, created_at, updated_at)
                    SELECT :id, :gearPackId, :name, :category, :defaultQuantity, :scalable,
                           COALESCE((SELECT MAX(sort_order) FROM gear_pack_items WHERE gear_pack_id = :gearPackId), 0) + 1,
                           :createdAt, :updatedAt
                    RETURNING sort_order
                    """.trimIndent()
                )
                    .bind("id", id)
                    .bind("gearPackId", param.gearPackId)
                    .bind("name", param.name)
                    .bind("category", param.category)
                    .bind("defaultQuantity", param.defaultQuantity)
                    .bind("scalable", param.scalable)
                    .bind("createdAt", now)
                    .bind("updatedAt", now)
                    .mapTo(Int::class.java)
                    .one()

                GearPackItem(
                    id = id,
                    gearPackId = param.gearPackId,
                    name = param.name,
                    category = param.category,
                    defaultQuantity = param.defaultQuantity,
                    scalable = param.scalable,
                    sortOrder = sortOrder,
                    createdAt = now,
                    updatedAt = now,
                )
            }
            success(entity)
        } catch (e: Exception) {
            if (e.message?.contains("uq_gear_pack_items_pack_name") == true || e.message?.contains("duplicate key") == true) {
                failure(ConflictError("GearPackItem", "name '${param.name}' already exists in this gear pack"))
            } else {
                throw e
            }
        }
    }
}
