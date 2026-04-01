package com.acme.clients.itemclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.itemclient.api.GetByIdParam
import com.acme.clients.itemclient.internal.adapters.ItemRowAdapter
import com.acme.clients.itemclient.internal.validations.ValidateGetItemById
import com.acme.clients.itemclient.model.Item
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetItemById(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetItemById::class.java)
    private val validate = ValidateGetItemById()

    fun execute(param: GetByIdParam): Result<Item, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Finding item by id={}", param.id)
        val entity = jdbi.withHandle<Item?, Exception> { handle ->
            handle.createQuery(
                """
                SELECT i.id, i.plan_id, i.user_id, i.name, i.category, i.quantity, i.packed, i.gear_pack_id, gp.name AS gear_pack_name, i.created_at, i.updated_at
                FROM items i
                LEFT JOIN gear_packs gp ON gp.id = i.gear_pack_id
                WHERE i.id = :id
                """.trimIndent()
            )
                .bind("id", param.id)
                .map { rs, _ -> ItemRowAdapter.fromResultSet(rs) }
                .findOne()
                .orElse(null)
        }
        return if (entity != null) success(entity) else failure(NotFoundError("Item", param.id.toString()))
    }
}
