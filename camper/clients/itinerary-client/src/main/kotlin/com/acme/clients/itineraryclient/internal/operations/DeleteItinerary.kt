package com.acme.clients.itineraryclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.itineraryclient.api.DeleteItineraryParam
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class DeleteItinerary(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(DeleteItinerary::class.java)

    fun execute(param: DeleteItineraryParam): Result<Unit, AppError> {
        logger.debug("Deleting itinerary for planId={}", param.planId)
        val rowsAffected = jdbi.withHandle<Int, Exception> { handle ->
            handle.createUpdate("DELETE FROM itineraries WHERE plan_id = :planId")
                .bind("planId", param.planId)
                .execute()
        }
        return if (rowsAffected > 0) success(Unit) else failure(NotFoundError("Itinerary", param.planId.toString()))
    }
}
