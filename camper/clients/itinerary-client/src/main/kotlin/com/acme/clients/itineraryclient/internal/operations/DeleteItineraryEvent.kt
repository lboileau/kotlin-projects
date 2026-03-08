package com.acme.clients.itineraryclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.itineraryclient.api.DeleteEventParam
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class DeleteItineraryEvent(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(DeleteItineraryEvent::class.java)

    fun execute(param: DeleteEventParam): Result<Unit, AppError> {
        logger.debug("Deleting itinerary event id={}", param.id)
        val rowsAffected = jdbi.withHandle<Int, Exception> { handle ->
            handle.createUpdate("DELETE FROM itinerary_events WHERE id = :id")
                .bind("id", param.id)
                .execute()
        }
        return if (rowsAffected > 0) success(Unit) else failure(NotFoundError("ItineraryEvent", param.id.toString()))
    }
}
