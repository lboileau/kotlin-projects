package com.acme.clients.itemclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.itemclient.api.DeleteItemParam
import com.acme.clients.itemclient.internal.validations.ValidateDeleteItem
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class DeleteItem(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(DeleteItem::class.java)
    private val validate = ValidateDeleteItem()

    fun execute(param: DeleteItemParam): Result<Unit, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Deleting item id={}", param.id)
        val rowsAffected = jdbi.withHandle<Int, Exception> { handle ->
            handle.createUpdate("DELETE FROM items WHERE id = :id")
                .bind("id", param.id)
                .execute()
        }
        return if (rowsAffected > 0) success(Unit) else failure(NotFoundError("Item", param.id.toString()))
    }
}
