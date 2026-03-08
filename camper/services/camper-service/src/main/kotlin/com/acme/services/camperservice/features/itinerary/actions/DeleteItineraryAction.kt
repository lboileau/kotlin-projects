package com.acme.services.camperservice.features.itinerary.actions

import com.acme.clients.common.Result
import com.acme.clients.itineraryclient.api.ItineraryClient
import com.acme.clients.itineraryclient.api.DeleteItineraryParam as ClientDeleteItineraryParam
import com.acme.clients.planclient.api.PlanClient
import com.acme.clients.planclient.api.GetByIdParam as PlanGetByIdParam
import com.acme.services.camperservice.features.itinerary.error.ItineraryError
import com.acme.services.camperservice.features.itinerary.params.DeleteItineraryParam
import com.acme.services.camperservice.features.itinerary.validations.ValidateDeleteItinerary
import org.slf4j.LoggerFactory

internal class DeleteItineraryAction(
    private val itineraryClient: ItineraryClient,
    private val planClient: PlanClient
) {
    private val logger = LoggerFactory.getLogger(DeleteItineraryAction::class.java)
    private val validate = ValidateDeleteItinerary()

    fun execute(param: DeleteItineraryParam): Result<Unit, ItineraryError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Deleting itinerary for plan={}", param.planId)

        // Verify plan exists
        when (planClient.getById(PlanGetByIdParam(id = param.planId))) {
            is Result.Success -> { /* plan exists */ }
            is Result.Failure -> return Result.Failure(ItineraryError.PlanNotFound(param.planId.toString()))
        }

        // Delete itinerary by planId (cascade deletes events)
        return when (itineraryClient.delete(ClientDeleteItineraryParam(planId = param.planId))) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> Result.Failure(ItineraryError.NotFound(param.planId.toString()))
        }
    }
}
