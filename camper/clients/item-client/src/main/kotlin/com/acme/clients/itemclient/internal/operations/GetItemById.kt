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
            handle.createQuery("SELECT id, plan_id, user_id, name, category, quantity, packed, created_at, updated_at FROM items WHERE id = :id")
                .bind("id", param.id)
                .map { rs, _ -> ItemRowAdapter.fromResultSet(rs) }
                .findOne()
                .orElse(null)
        }
        return if (entity != null) success(entity) else failure(NotFoundError("Item", param.id.toString()))
    }
}
