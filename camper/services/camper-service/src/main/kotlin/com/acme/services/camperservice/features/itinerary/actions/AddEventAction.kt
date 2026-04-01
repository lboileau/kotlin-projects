package com.acme.services.camperservice.features.itinerary.actions

import com.acme.clients.common.Result
import com.acme.clients.itineraryclient.api.CreateItineraryParam as ClientCreateItineraryParam
import com.acme.clients.itineraryclient.api.GetByPlanIdParam as ClientGetByPlanIdParam
import com.acme.clients.itineraryclient.api.ItineraryClient
import com.acme.clients.itineraryclient.api.AddEventParam as ClientAddEventParam
import com.acme.clients.itineraryclient.api.LinkInput as ClientLinkInput
import com.acme.clients.itineraryclient.api.ReplaceEventLinksParam as ClientReplaceEventLinksParam
import com.acme.clients.planclient.api.PlanClient
import com.acme.clients.planclient.api.GetByIdParam as PlanGetByIdParam
import com.acme.services.camperservice.features.itinerary.error.ItineraryError
import com.acme.services.camperservice.features.itinerary.mapper.ItineraryMapper
import com.acme.services.camperservice.features.itinerary.model.ItineraryEvent
import com.acme.services.camperservice.features.itinerary.model.ItineraryEventLink
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

    fun execute(param: AddEventParam): Result<Pair<ItineraryEvent, List<ItineraryEventLink>>, ItineraryError> {
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
        val event = when (val result = itineraryClient.addEvent(
            ClientAddEventParam(
                itineraryId = itineraryId,
                title = param.title,
                description = param.description,
                details = param.details,
                eventAt = param.eventAt,
                category = param.category,
                estimatedCost = param.estimatedCost,
                location = param.location,
                eventEndAt = param.eventEndAt
            )
        )) {
            is Result.Success -> ItineraryMapper.fromClient(result.value)
            is Result.Failure -> return Result.Failure(ItineraryError.fromClientError(result.error))
        }

        // Replace links if provided
        val links = if (!param.links.isNullOrEmpty()) {
            when (val linkResult = itineraryClient.replaceEventLinks(
                ClientReplaceEventLinksParam(
                    eventId = event.id,
                    links = param.links.map { ClientLinkInput(url = it.url, label = it.label) }
                )
            )) {
                is Result.Success -> linkResult.value.map { ItineraryMapper.fromClient(it) }
                is Result.Failure -> return Result.Failure(ItineraryError.fromClientError(linkResult.error))
            }
        } else emptyList()

        return Result.Success(Pair(event, links))
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
