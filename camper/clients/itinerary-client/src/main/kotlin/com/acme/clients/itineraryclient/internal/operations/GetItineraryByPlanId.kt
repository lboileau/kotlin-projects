package com.acme.clients.itineraryclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.itineraryclient.api.GetByPlanIdParam
import com.acme.clients.itineraryclient.internal.adapters.ItineraryRowAdapter
import com.acme.clients.itineraryclient.model.Itinerary
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetItineraryByPlanId(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetItineraryByPlanId::class.java)

    fun execute(param: GetByPlanIdParam): Result<Itinerary, AppError> {
        logger.debug("Finding itinerary by planId={}", param.planId)
        val entity = jdbi.withHandle<Itinerary?, Exception> { handle ->
            handle.createQuery("SELECT id, plan_id, created_at, updated_at FROM itineraries WHERE plan_id = :planId")
                .bind("planId", param.planId)
                .map { rs, _ -> ItineraryRowAdapter.fromResultSet(rs) }
                .findOne()
                .orElse(null)
        }
        return if (entity != null) success(entity) else failure(NotFoundError("Itinerary", param.planId.toString()))
    }
}
