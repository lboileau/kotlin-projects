package com.acme.services.camperservice.features.itinerary.actions

import com.acme.clients.common.Result
import com.acme.clients.itineraryclient.api.CreateItineraryParam as ClientCreateItineraryParam
import com.acme.clients.itineraryclient.api.GetByPlanIdParam as ClientGetByPlanIdParam
import com.acme.clients.itineraryclient.api.ItineraryClient
import com.acme.clients.itineraryclient.api.AddEventParam as ClientAddEventParam
import com.acme.clients.planclient.api.PlanClient
import com.acme.clients.planclient.api.GetByIdParam as PlanGetByIdParam
import com.acme.services.camperservice.features.itinerary.error.ItineraryError
import com.acme.services.camperservice.features.itinerary.mapper.ItineraryMapper
import com.acme.services.camperservice.features.itinerary.model.ItineraryEvent
import com.acme.services.camperservice.features.itinerary.params.AddEventParam
import com.acme.services.camperservice.features.itinerary.validations.ValidateAddEvent
import org.slf4j.LoggerFactory
import java.util.UUID

internal class AddEventAction(
    private val itineraryClient: ItineraryClient,
    private val planClient: PlanClient
) {
    private val logger = LoggerFactory.getLogger(AddEventAction::class.java)
    private val validate = ValidateAddEvent()

    fun execute(param: AddEventParam): Result<ItineraryEvent, ItineraryError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Adding event to itinerary for plan={}", param.planId)

        // Verify plan exists
        when (planClient.getById(PlanGetByIdParam(id = param.planId))) {
            is Result.Success -> { /* plan exists */ }
            is Result.Failure -> return Result.Failure(ItineraryError.PlanNotFound(param.planId.toString()))
        }

        // Get or create itinerary for plan
        val itineraryId = getOrCreateItineraryId(param.planId)
            ?: return Result.Failure(ItineraryError.Invalid("itinerary", "failed to get or create itinerary"))

        // Add event to itinerary
        return when (val result = itineraryClient.addEvent(
            ClientAddEventParam(
                itineraryId = itineraryId,
                title = param.title,
                description = param.description,
                details = param.details,
                eventAt = param.eventAt
            )
        )) {
            is Result.Success -> Result.Success(ItineraryMapper.fromClient(result.value))
            is Result.Failure -> Result.Failure(ItineraryError.fromClientError(result.error))
        }
    }

    private fun getOrCreateItineraryId(planId: UUID): UUID? {
        // Try to get existing itinerary
        when (val result = itineraryClient.getByPlanId(ClientGetByPlanIdParam(planId = planId))) {
            is Result.Success -> return result.value.id
            is Result.Failure -> { /* not found, create one */ }
        }

        // Create new itinerary
        return when (val result = itineraryClient.create(ClientCreateItineraryParam(planId = planId))) {
            is Result.Success -> result.value.id
            is Result.Failure -> null
        }
    }
}
