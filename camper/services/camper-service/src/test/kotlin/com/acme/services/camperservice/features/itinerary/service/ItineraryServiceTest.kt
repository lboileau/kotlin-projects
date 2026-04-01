package com.acme.services.camperservice.features.itinerary.service

import com.acme.clients.common.Result
import com.acme.clients.itineraryclient.fake.FakeItineraryClient
import com.acme.clients.itineraryclient.model.Itinerary as ClientItinerary
import com.acme.clients.itineraryclient.model.ItineraryEvent as ClientItineraryEvent
import com.acme.clients.planclient.fake.FakePlanClient
import com.acme.clients.planclient.model.Plan
import com.acme.services.camperservice.features.itinerary.error.ItineraryError
import com.acme.services.camperservice.features.itinerary.params.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class ItineraryServiceTest {

    private val fakeItineraryClient = FakeItineraryClient()
    private val fakePlanClient = FakePlanClient()
    private val itineraryService = ItineraryService(fakeItineraryClient, fakePlanClient)

    private val planId = UUID.randomUUID()
    private val ownerId = UUID.randomUUID()
    private val itineraryId = UUID.randomUUID()
    private val eventId = UUID.randomUUID()
    private val eventAt = Instant.parse("2026-07-15T10:00:00Z")

    @BeforeEach
    fun setUp() {
        fakeItineraryClient.reset()
        fakePlanClient.reset()
        fakePlanClient.seedPlan(
            Plan(
                id = planId, name = "Camping Trip", visibility = "private",
                ownerId = ownerId, createdAt = Instant.now(), updatedAt = Instant.now()
            )
        )
    }

    @Nested
    inner class GetItinerary {
        @Test
        fun `getItinerary returns itinerary with events when exists`() {
            fakeItineraryClient.seedItinerary(
                ClientItinerary(id = itineraryId, planId = planId, createdAt = Instant.now(), updatedAt = Instant.now())
            )
            fakeItineraryClient.seedEvent(
                ClientItineraryEvent(
                    id = eventId, itineraryId = itineraryId, title = "Hike",
                    description = "Morning hike", details = null, eventAt = eventAt,
                    category = "activity", estimatedCost = null, location = null, eventEndAt = null,
                    createdAt = Instant.now(), updatedAt = Instant.now()
                )
            )

            val result = itineraryService.getItinerary(GetItineraryParam(planId = planId))

            assertThat(result.isSuccess).isTrue()
            val (itinerary, events, _) = (result as Result.Success).value
            assertThat(itinerary.planId).isEqualTo(planId)
            assertThat(events).hasSize(1)
            assertThat(events[0].title).isEqualTo("Hike")
        }

        @Test
        fun `getItinerary returns PlanNotFound when plan does not exist`() {
            val result = itineraryService.getItinerary(GetItineraryParam(planId = UUID.randomUUID()))

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ItineraryError.PlanNotFound::class.java)
        }

        @Test
        fun `getItinerary returns NotFound when no itinerary for plan`() {
            val result = itineraryService.getItinerary(GetItineraryParam(planId = planId))

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ItineraryError.NotFound::class.java)
        }
    }

    @Nested
    inner class DeleteItinerary {
        @Test
        fun `deleteItinerary deletes itinerary successfully`() {
            fakeItineraryClient.seedItinerary(
                ClientItinerary(id = itineraryId, planId = planId, createdAt = Instant.now(), updatedAt = Instant.now())
            )

            val result = itineraryService.deleteItinerary(DeleteItineraryParam(planId = planId))

            assertThat(result.isSuccess).isTrue()

            // Verify itinerary is gone
            val getResult = itineraryService.getItinerary(GetItineraryParam(planId = planId))
            assertThat(getResult.isFailure).isTrue()
            assertThat((getResult as Result.Failure).error).isInstanceOf(ItineraryError.NotFound::class.java)
        }

        @Test
        fun `deleteItinerary returns PlanNotFound when plan does not exist`() {
            val result = itineraryService.deleteItinerary(DeleteItineraryParam(planId = UUID.randomUUID()))

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ItineraryError.PlanNotFound::class.java)
        }

        @Test
        fun `deleteItinerary returns NotFound when no itinerary`() {
            val result = itineraryService.deleteItinerary(DeleteItineraryParam(planId = planId))

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ItineraryError.NotFound::class.java)
        }
    }

    @Nested
    inner class AddEvent {
        @Test
        fun `addEvent adds event and auto-creates itinerary if needed`() {
            val result = itineraryService.addEvent(
                AddEventParam(
                    planId = planId, title = "Campfire",
                    description = "Evening campfire", details = null, eventAt = eventAt,
                    category = "other", estimatedCost = null, location = null, eventEndAt = null, links = null
                )
            )

            assertThat(result.isSuccess).isTrue()
            val (event, _) = (result as Result.Success).value
            assertThat(event.title).isEqualTo("Campfire")
            assertThat(event.description).isEqualTo("Evening campfire")
            assertThat(event.eventAt).isEqualTo(eventAt)

            // Verify itinerary was auto-created
            val getResult = itineraryService.getItinerary(GetItineraryParam(planId = planId))
            assertThat(getResult.isSuccess).isTrue()
        }

        @Test
        fun `addEvent returns PlanNotFound when plan does not exist`() {
            val result = itineraryService.addEvent(
                AddEventParam(
                    planId = UUID.randomUUID(), title = "Campfire",
                    description = null, details = null, eventAt = eventAt,
                    category = "other", estimatedCost = null, location = null, eventEndAt = null, links = null
                )
            )

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ItineraryError.PlanNotFound::class.java)
        }

        @Test
        fun `addEvent returns Invalid when title is blank`() {
            val result = itineraryService.addEvent(
                AddEventParam(
                    planId = planId, title = "",
                    description = null, details = null, eventAt = eventAt,
                    category = "other", estimatedCost = null, location = null, eventEndAt = null, links = null
                )
            )

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ItineraryError.Invalid::class.java)
        }
    }

    @Nested
    inner class UpdateEvent {
        @Test
        fun `updateEvent updates event successfully`() {
            // Add an event first
            val addResult = itineraryService.addEvent(
                AddEventParam(
                    planId = planId, title = "Hike",
                    description = "Morning hike", details = null, eventAt = eventAt,
                    category = "activity", estimatedCost = null, location = null, eventEndAt = null, links = null
                )
            )
            val (createdEvent, _) = (addResult as Result.Success).value
            val newEventAt = Instant.parse("2026-07-15T14:00:00Z")

            val result = itineraryService.updateEvent(
                UpdateEventParam(
                    planId = planId, eventId = createdEvent.id,
                    title = "Afternoon Hike", description = "Changed to afternoon",
                    details = "Bring water", eventAt = newEventAt,
                    category = "activity", estimatedCost = null, location = null, eventEndAt = null, links = null
                )
            )

            assertThat(result.isSuccess).isTrue()
            val (updated, _) = (result as Result.Success).value
            assertThat(updated.title).isEqualTo("Afternoon Hike")
            assertThat(updated.description).isEqualTo("Changed to afternoon")
            assertThat(updated.details).isEqualTo("Bring water")
            assertThat(updated.eventAt).isEqualTo(newEventAt)
        }

        @Test
        fun `updateEvent returns EventNotFound when event does not exist`() {
            val result = itineraryService.updateEvent(
                UpdateEventParam(
                    planId = planId, eventId = UUID.randomUUID(),
                    title = "Nope", description = null, details = null, eventAt = eventAt,
                    category = "other", estimatedCost = null, location = null, eventEndAt = null, links = null
                )
            )

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ItineraryError.EventNotFound::class.java)
        }

        @Test
        fun `updateEvent returns Invalid when title is blank`() {
            // Add an event first
            val addResult = itineraryService.addEvent(
                AddEventParam(
                    planId = planId, title = "Hike",
                    description = null, details = null, eventAt = eventAt,
                    category = "other", estimatedCost = null, location = null, eventEndAt = null, links = null
                )
            )
            val (createdEvent, _) = (addResult as Result.Success).value

            val result = itineraryService.updateEvent(
                UpdateEventParam(
                    planId = planId, eventId = createdEvent.id,
                    title = "", description = null, details = null, eventAt = eventAt,
                    category = "other", estimatedCost = null, location = null, eventEndAt = null, links = null
                )
            )

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ItineraryError.Invalid::class.java)
        }
    }

    @Nested
    inner class DeleteEvent {
        @Test
        fun `deleteEvent deletes event successfully`() {
            // Add an event first
            val addResult = itineraryService.addEvent(
                AddEventParam(
                    planId = planId, title = "Hike",
                    description = null, details = null, eventAt = eventAt,
                    category = "other", estimatedCost = null, location = null, eventEndAt = null, links = null
                )
            )
            val (createdEvent, _) = (addResult as Result.Success).value

            val result = itineraryService.deleteEvent(DeleteEventParam(planId = planId, eventId = createdEvent.id))

            assertThat(result.isSuccess).isTrue()
        }

        @Test
        fun `deleteEvent returns EventNotFound when event does not exist`() {
            val result = itineraryService.deleteEvent(DeleteEventParam(planId = planId, eventId = UUID.randomUUID()))

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ItineraryError.EventNotFound::class.java)
        }
    }
}
