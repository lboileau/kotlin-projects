package com.acme.clients.itemclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.itemclient.api.CreateItemParam
import com.acme.clients.itemclient.internal.validations.ValidateCreateItem
import com.acme.clients.itemclient.model.Item
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

internal class CreateItem(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(CreateItem::class.java)
    private val validate = ValidateCreateItem()

    fun execute(param: CreateItemParam): Result<Item, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Creating item name={}", param.name)
        val entity = jdbi.withHandle<Item, Exception> { handle ->
            val id = UUID.randomUUID()
            val now = Instant.now()
            handle.createUpdate(
                """
                INSERT INTO items (id, plan_id, user_id, name, category, quantity, packed, created_at, updated_at)
                VALUES (:id, :planId, :userId, :name, :category, :quantity, :packed, :createdAt, :updatedAt)
                """.trimIndent()
            )
                .bind("id", id)
                .bind("planId", param.planId)
                .bind("userId", param.userId)
                .bind("name", param.name)
                .bind("category", param.category)
                .bind("quantity", param.quantity)
                .bind("packed", param.packed)
                .bind("createdAt", now)
                .bind("updatedAt", now)
                .execute()
            Item(
                id = id,
                planId = param.planId,
                userId = param.userId,
                name = param.name,
                category = param.category,
                quantity = param.quantity,
                packed = param.packed,
                createdAt = now,
                updatedAt = now
            )
        }
        return success(entity)
    }
}
