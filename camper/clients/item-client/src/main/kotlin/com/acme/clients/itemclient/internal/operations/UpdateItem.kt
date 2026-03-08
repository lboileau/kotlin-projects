package com.acme.clients.itemclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.itemclient.api.GetByIdParam
import com.acme.clients.itemclient.api.UpdateItemParam
import com.acme.clients.itemclient.internal.validations.ValidateUpdateItem
import com.acme.clients.itemclient.model.Item
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant

internal class UpdateItem(
    private val jdbi: Jdbi,
    private val getItemById: GetItemById
) {
    private val logger = LoggerFactory.getLogger(UpdateItem::class.java)
    private val validate = ValidateUpdateItem()

    fun execute(param: UpdateItemParam): Result<Item, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Updating item id={}", param.id)
        return when (val existing = getItemById.execute(GetByIdParam(param.id))) {
            is Result.Failure -> existing
            is Result.Success -> {
                val now = Instant.now()
                jdbi.withHandle<Unit, Exception> { handle ->
                    handle.createUpdate(
                        """
                        UPDATE items SET name = :name, category = :category, quantity = :quantity, packed = :packed, updated_at = :updatedAt
                        WHERE id = :id
                        """.trimIndent()
                    )
                        .bind("id", param.id)
                        .bind("name", param.name)
                        .bind("category", param.category)
                        .bind("quantity", param.quantity)
                        .bind("packed", param.packed)
                        .bind("updatedAt", now)
                        .execute()
                }
                success(
                    existing.value.copy(
                        name = param.name,
                        category = param.category,
                        quantity = param.quantity,
                        packed = param.packed,
                        updatedAt = now
                    )
                )
            }
        }
    }
}
