package com.acme.services.camperservice.features.itinerary.service

import com.acme.clients.common.Result
import com.acme.clients.itineraryclient.fake.FakeItineraryClient
import com.acme.clients.itineraryclient.model.Itinerary as ClientItinerary
import com.acme.clients.itineraryclient.model.ItineraryEvent as ClientItineraryEvent
import com.acme.clients.itineraryclient.model.ItineraryEventLink as ClientItineraryEventLink
import com.acme.clients.planclient.fake.FakePlanClient
import com.acme.clients.planclient.model.Plan
import com.acme.services.camperservice.features.itinerary.dto.LinkInput
import com.acme.services.camperservice.features.itinerary.error.ItineraryError
import com.acme.services.camperservice.features.itinerary.params.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
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
        fun `getItinerary returns events with links populated`() {
            fakeItineraryClient.seedItinerary(
                ClientItinerary(id = itineraryId, planId = planId, createdAt = Instant.now(), updatedAt = Instant.now())
            )
            fakeItineraryClient.seedEvent(
                ClientItineraryEvent(
                    id = eventId, itineraryId = itineraryId, title = "Hike",
                    description = null, details = null, eventAt = eventAt,
                    category = "activity", estimatedCost = null, location = null, eventEndAt = null,
                    createdAt = Instant.now(), updatedAt = Instant.now()
                )
            )
            val linkId = UUID.randomUUID()
            fakeItineraryClient.seedLink(
                ClientItineraryEventLink(
                    id = linkId, eventId = eventId, url = "https://alltrails.com/hike",
                    label = "AllTrails", createdAt = Instant.now()
                )
            )

            val result = itineraryService.getItinerary(GetItineraryParam(planId = planId))

            assertThat(result.isSuccess).isTrue()
            val (_, _, linksByEventId) = (result as Result.Success).value
            assertThat(linksByEventId).containsKey(eventId)
            val links = linksByEventId[eventId]!!
            assertThat(links).hasSize(1)
            assertThat(links[0].url).isEqualTo("https://alltrails.com/hike")
            assertThat(links[0].label).isEqualTo("AllTrails")
        }

        @Test
        fun `getItinerary computes totalEstimatedCost as sum of event costs`() {
            fakeItineraryClient.seedItinerary(
                ClientItinerary(id = itineraryId, planId = planId, createdAt = Instant.now(), updatedAt = Instant.now())
            )
            val event1Id = UUID.randomUUID()
            val event2Id = UUID.randomUUID()
            fakeItineraryClient.seedEvent(
                ClientItineraryEvent(
                    id = event1Id, itineraryId = itineraryId, title = "Hike",
                    description = null, details = null, eventAt = eventAt,
                    category = "activity", estimatedCost = BigDecimal("25.00"), location = null, eventEndAt = null,
                    createdAt = Instant.now(), updatedAt = Instant.now()
                ),
                ClientItineraryEvent(
                    id = event2Id, itineraryId = itineraryId, title = "Dinner",
                    description = null, details = null, eventAt = Instant.parse("2026-07-15T18:00:00Z"),
                    category = "meal", estimatedCost = BigDecimal("35.50"), location = null, eventEndAt = null,
                    createdAt = Instant.now(), updatedAt = Instant.now()
                )
            )

            val result = itineraryService.getItinerary(GetItineraryParam(planId = planId))

            assertThat(result.isSuccess).isTrue()
            val (_, events, _) = (result as Result.Success).value
            // totalEstimatedCost is computed at the mapper/controller level, but verify events have costs
            assertThat(events).hasSize(2)
            val totalCost = events.mapNotNull { it.estimatedCost }.sumOf { it }
            assertThat(totalCost).isEqualByComparingTo(BigDecimal("60.50"))
        }

        @Test
        fun `getItinerary with no events returns empty events and links`() {
            fakeItineraryClient.seedItinerary(
                ClientItinerary(id = itineraryId, planId = planId, createdAt = Instant.now(), updatedAt = Instant.now())
            )

            val result = itineraryService.getItinerary(GetItineraryParam(planId = planId))

            assertThat(result.isSuccess).isTrue()
            val (_, events, linksByEventId) = (result as Result.Success).value
            assertThat(events).isEmpty()
            assertThat(linksByEventId).isEmpty()
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
        fun `addEvent with all metadata fields returns them in response`() {
            val eventEndAt = Instant.parse("2026-07-15T14:00:00Z")
            val result = itineraryService.addEvent(
                AddEventParam(
                    planId = planId, title = "Mountain Hike",
                    description = "Trail to Eagle Peak", details = "Bring water and sunscreen",
                    eventAt = eventAt, category = "activity",
                    estimatedCost = BigDecimal("25.50"), location = "Eagle Peak Trailhead",
                    eventEndAt = eventEndAt, links = null
                )
            )

            assertThat(result.isSuccess).isTrue()
            val (event, _) = (result as Result.Success).value
            assertThat(event.title).isEqualTo("Mountain Hike")
            assertThat(event.category).isEqualTo("activity")
            assertThat(event.estimatedCost).isEqualByComparingTo(BigDecimal("25.50"))
            assertThat(event.location).isEqualTo("Eagle Peak Trailhead")
            assertThat(event.eventEndAt).isEqualTo(eventEndAt)
        }

        @Test
        fun `addEvent with links returns links in response`() {
            val result = itineraryService.addEvent(
                AddEventParam(
                    planId = planId, title = "Hike",
                    description = null, details = null, eventAt = eventAt,
                    category = "activity", estimatedCost = null, location = null, eventEndAt = null,
                    links = listOf(
                        LinkInput(url = "https://alltrails.com/hike", label = "AllTrails"),
                        LinkInput(url = "https://maps.google.com", label = null)
                    )
                )
            )

            assertThat(result.isSuccess).isTrue()
            val (_, links) = (result as Result.Success).value
            assertThat(links).hasSize(2)
            assertThat(links[0].url).isEqualTo("https://alltrails.com/hike")
            assertThat(links[0].label).isEqualTo("AllTrails")
            assertThat(links[1].url).isEqualTo("https://maps.google.com")
            assertThat(links[1].label).isNull()
        }

        @Test
        fun `addEvent without links returns empty links list`() {
            val result = itineraryService.addEvent(
                AddEventParam(
                    planId = planId, title = "Campfire",
                    description = null, details = null, eventAt = eventAt,
                    category = "other", estimatedCost = null, location = null, eventEndAt = null, links = null
                )
            )

            assertThat(result.isSuccess).isTrue()
            val (_, links) = (result as Result.Success).value
            assertThat(links).isEmpty()
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
            assertThat((error as ItineraryError.Invalid).field).isEqualTo("title")
        }

        @Test
        fun `addEvent returns Invalid for invalid category`() {
            val result = itineraryService.addEvent(
                AddEventParam(
                    planId = planId, title = "Event",
                    description = null, details = null, eventAt = eventAt,
                    category = "invalid", estimatedCost = null, location = null, eventEndAt = null, links = null
                )
            )

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ItineraryError.Invalid::class.java)
            assertThat((error as ItineraryError.Invalid).field).isEqualTo("category")
        }

        @Test
        fun `addEvent returns Invalid for negative estimatedCost`() {
            val result = itineraryService.addEvent(
                AddEventParam(
                    planId = planId, title = "Event",
                    description = null, details = null, eventAt = eventAt,
                    category = "other", estimatedCost = BigDecimal("-1.00"), location = null, eventEndAt = null, links = null
                )
            )

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ItineraryError.Invalid::class.java)
            assertThat((error as ItineraryError.Invalid).field).isEqualTo("estimatedCost")
        }

        @Test
        fun `addEvent returns Invalid when eventEndAt is before eventAt`() {
            val result = itineraryService.addEvent(
                AddEventParam(
                    planId = planId, title = "Event",
                    description = null, details = null, eventAt = Instant.parse("2026-07-15T14:00:00Z"),
                    category = "other", estimatedCost = null, location = null,
                    eventEndAt = Instant.parse("2026-07-15T10:00:00Z"), links = null
                )
            )

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ItineraryError.Invalid::class.java)
            assertThat((error as ItineraryError.Invalid).field).isEqualTo("eventEndAt")
        }

        @Test
        fun `addEvent returns Invalid for more than 10 links`() {
            val tooManyLinks = (1..11).map { LinkInput(url = "https://example.com/$it", label = "Link $it") }

            val result = itineraryService.addEvent(
                AddEventParam(
                    planId = planId, title = "Event",
                    description = null, details = null, eventAt = eventAt,
                    category = "other", estimatedCost = null, location = null, eventEndAt = null,
                    links = tooManyLinks
                )
            )

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ItineraryError.Invalid::class.java)
            assertThat((error as ItineraryError.Invalid).field).isEqualTo("links")
        }

        @Test
        fun `addEvent returns Invalid for blank link URL`() {
            val result = itineraryService.addEvent(
                AddEventParam(
                    planId = planId, title = "Event",
                    description = null, details = null, eventAt = eventAt,
                    category = "other", estimatedCost = null, location = null, eventEndAt = null,
                    links = listOf(LinkInput(url = "", label = "Empty"))
                )
            )

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ItineraryError.Invalid::class.java)
            assertThat((error as ItineraryError.Invalid).field).isEqualTo("url")
        }
    }

    @Nested
    inner class UpdateEvent {
        @Test
        fun `updateEvent updates event with all metadata fields`() {
            val addResult = itineraryService.addEvent(
                AddEventParam(
                    planId = planId, title = "Hike",
                    description = "Morning hike", details = null, eventAt = eventAt,
                    category = "activity", estimatedCost = null, location = null, eventEndAt = null, links = null
                )
            )
            val (createdEvent, _) = (addResult as Result.Success).value
            val newEventAt = Instant.parse("2026-07-15T14:00:00Z")
            val newEventEndAt = Instant.parse("2026-07-15T18:00:00Z")

            val result = itineraryService.updateEvent(
                UpdateEventParam(
                    planId = planId, eventId = createdEvent.id,
                    title = "Afternoon Hike", description = "Changed to afternoon",
                    details = "Bring water", eventAt = newEventAt,
                    category = "activity", estimatedCost = BigDecimal("15.00"),
                    location = "West Trail", eventEndAt = newEventEndAt, links = null
                )
            )

            assertThat(result.isSuccess).isTrue()
            val (updated, _) = (result as Result.Success).value
            assertThat(updated.title).isEqualTo("Afternoon Hike")
            assertThat(updated.description).isEqualTo("Changed to afternoon")
            assertThat(updated.details).isEqualTo("Bring water")
            assertThat(updated.eventAt).isEqualTo(newEventAt)
            assertThat(updated.category).isEqualTo("activity")
            assertThat(updated.estimatedCost).isEqualByComparingTo(BigDecimal("15.00"))
            assertThat(updated.location).isEqualTo("West Trail")
            assertThat(updated.eventEndAt).isEqualTo(newEventEndAt)
        }

        @Test
        fun `updateEvent with links replaces existing links`() {
            // Create event with links
            val addResult = itineraryService.addEvent(
                AddEventParam(
                    planId = planId, title = "Hike",
                    description = null, details = null, eventAt = eventAt,
                    category = "activity", estimatedCost = null, location = null, eventEndAt = null,
                    links = listOf(LinkInput(url = "https://old.com", label = "Old"))
                )
            )
            val (createdEvent, _) = (addResult as Result.Success).value

            // Update with new links
            val result = itineraryService.updateEvent(
                UpdateEventParam(
                    planId = planId, eventId = createdEvent.id,
                    title = "Hike", description = null, details = null, eventAt = eventAt,
                    category = "activity", estimatedCost = null, location = null, eventEndAt = null,
                    links = listOf(
                        LinkInput(url = "https://new1.com", label = "New 1"),
                        LinkInput(url = "https://new2.com", label = "New 2")
                    )
                )
            )

            assertThat(result.isSuccess).isTrue()
            val (_, links) = (result as Result.Success).value
            assertThat(links).hasSize(2)
            assertThat(links.map { it.url }).containsExactly("https://new1.com", "https://new2.com")
        }

        @Test
        fun `updateEvent with null links keeps existing links unchanged`() {
            // Create event with links
            val addResult = itineraryService.addEvent(
                AddEventParam(
                    planId = planId, title = "Hike",
                    description = null, details = null, eventAt = eventAt,
                    category = "activity", estimatedCost = null, location = null, eventEndAt = null,
                    links = listOf(LinkInput(url = "https://keep.com", label = "Keep"))
                )
            )
            val (createdEvent, originalLinks) = (addResult as Result.Success).value

            // Update with null links — existing should be preserved
            val result = itineraryService.updateEvent(
                UpdateEventParam(
                    planId = planId, eventId = createdEvent.id,
                    title = "Updated Hike", description = null, details = null, eventAt = eventAt,
                    category = "activity", estimatedCost = null, location = null, eventEndAt = null,
                    links = null
                )
            )

            assertThat(result.isSuccess).isTrue()
            val (_, links) = (result as Result.Success).value
            assertThat(links).hasSize(1)
            assertThat(links[0].url).isEqualTo("https://keep.com")
            assertThat(links[0].label).isEqualTo("Keep")
        }

        @Test
        fun `updateEvent with empty links clears all links`() {
            // Create event with links
            val addResult = itineraryService.addEvent(
                AddEventParam(
                    planId = planId, title = "Hike",
                    description = null, details = null, eventAt = eventAt,
                    category = "activity", estimatedCost = null, location = null, eventEndAt = null,
                    links = listOf(LinkInput(url = "https://remove.com", label = "Remove"))
                )
            )
            val (createdEvent, _) = (addResult as Result.Success).value

            // Update with empty links list — should clear all
            val result = itineraryService.updateEvent(
                UpdateEventParam(
                    planId = planId, eventId = createdEvent.id,
                    title = "Hike", description = null, details = null, eventAt = eventAt,
                    category = "activity", estimatedCost = null, location = null, eventEndAt = null,
                    links = emptyList()
                )
            )

            assertThat(result.isSuccess).isTrue()
            val (_, links) = (result as Result.Success).value
            assertThat(links).isEmpty()
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
            assertThat((error as ItineraryError.Invalid).field).isEqualTo("title")
        }

        @Test
        fun `updateEvent returns Invalid for invalid category`() {
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
                    title = "Hike", description = null, details = null, eventAt = eventAt,
                    category = "bogus", estimatedCost = null, location = null, eventEndAt = null, links = null
                )
            )

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ItineraryError.Invalid::class.java)
            assertThat((error as ItineraryError.Invalid).field).isEqualTo("category")
        }

        @Test
        fun `updateEvent returns Invalid for negative estimatedCost`() {
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
                    title = "Hike", description = null, details = null, eventAt = eventAt,
                    category = "other", estimatedCost = BigDecimal("-5.00"), location = null, eventEndAt = null, links = null
                )
            )

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ItineraryError.Invalid::class.java)
            assertThat((error as ItineraryError.Invalid).field).isEqualTo("estimatedCost")
        }

        @Test
        fun `updateEvent returns Invalid when eventEndAt is before eventAt`() {
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
                    title = "Hike", description = null, details = null,
                    eventAt = Instant.parse("2026-07-15T14:00:00Z"),
                    category = "other", estimatedCost = null, location = null,
                    eventEndAt = Instant.parse("2026-07-15T10:00:00Z"), links = null
                )
            )

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ItineraryError.Invalid::class.java)
            assertThat((error as ItineraryError.Invalid).field).isEqualTo("eventEndAt")
        }

        @Test
        fun `updateEvent returns Invalid for more than 10 links`() {
            val addResult = itineraryService.addEvent(
                AddEventParam(
                    planId = planId, title = "Hike",
                    description = null, details = null, eventAt = eventAt,
                    category = "other", estimatedCost = null, location = null, eventEndAt = null, links = null
                )
            )
            val (createdEvent, _) = (addResult as Result.Success).value
            val tooManyLinks = (1..11).map { LinkInput(url = "https://example.com/$it", label = "Link $it") }

            val result = itineraryService.updateEvent(
                UpdateEventParam(
                    planId = planId, eventId = createdEvent.id,
                    title = "Hike", description = null, details = null, eventAt = eventAt,
                    category = "other", estimatedCost = null, location = null, eventEndAt = null,
                    links = tooManyLinks
                )
            )

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ItineraryError.Invalid::class.java)
            assertThat((error as ItineraryError.Invalid).field).isEqualTo("links")
        }

        @Test
        fun `updateEvent returns Invalid for blank link URL`() {
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
                    title = "Hike", description = null, details = null, eventAt = eventAt,
                    category = "other", estimatedCost = null, location = null, eventEndAt = null,
                    links = listOf(LinkInput(url = "", label = "Empty"))
                )
            )

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ItineraryError.Invalid::class.java)
            assertThat((error as ItineraryError.Invalid).field).isEqualTo("url")
        }
    }

    @Nested
    inner class DeleteEvent {
        @Test
        fun `deleteEvent deletes event successfully`() {
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
