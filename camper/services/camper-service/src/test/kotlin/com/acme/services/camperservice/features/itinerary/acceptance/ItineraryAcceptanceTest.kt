package com.acme.services.camperservice.features.itinerary.acceptance

import com.acme.services.camperservice.config.TestContainerConfig
import com.acme.services.camperservice.features.itinerary.acceptance.fixture.ItineraryFixture
import com.acme.services.camperservice.features.itinerary.dto.AddEventRequest
import com.acme.services.camperservice.features.itinerary.dto.ItineraryEventResponse
import com.acme.services.camperservice.features.itinerary.dto.ItineraryResponse
import com.acme.services.camperservice.features.itinerary.dto.UpdateEventRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Import
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestContainerConfig::class)
class ItineraryAcceptanceTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    private lateinit var fixture: ItineraryFixture
    private lateinit var ownerId: UUID
    private lateinit var planId: UUID

    @BeforeEach
    fun setUp() {
        fixture = ItineraryFixture(jdbcTemplate)
        fixture.truncateAll()
        ownerId = fixture.insertUser(email = "owner@example.com", username = "owner")
        planId = fixture.insertPlan(name = "Camping Trip", ownerId = ownerId)
    }

    @Nested
    inner class GetItinerary {

        @Test
        fun `GET returns 200 with itinerary and events ordered by event_at`() {
            val itineraryId = fixture.insertItinerary(planId = planId)
            val laterTime = Instant.now().plus(2, ChronoUnit.DAYS)
            val earlierTime = Instant.now().plus(1, ChronoUnit.DAYS)
            fixture.insertEvent(itineraryId = itineraryId, title = "Later Event", eventAt = laterTime)
            fixture.insertEvent(itineraryId = itineraryId, title = "Earlier Event", eventAt = earlierTime)

            val response = restTemplate.getForEntity(
                "/api/plans/$planId/itinerary",
                ItineraryResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.planId).isEqualTo(planId)
            assertThat(response.body!!.events).hasSize(2)
            assertThat(response.body!!.events[0].title).isEqualTo("Earlier Event")
            assertThat(response.body!!.events[1].title).isEqualTo("Later Event")
        }

        @Test
        fun `GET returns 404 when plan does not exist`() {
            val response = restTemplate.getForEntity(
                "/api/plans/${UUID.randomUUID()}/itinerary",
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @Test
        fun `GET returns 404 when no itinerary exists for plan`() {
            val response = restTemplate.getForEntity(
                "/api/plans/$planId/itinerary",
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @Nested
    inner class DeleteItinerary {

        @Test
        fun `DELETE returns 204 and deletes itinerary`() {
            fixture.insertItinerary(planId = planId)

            val response = restTemplate.exchange(
                "/api/plans/$planId/itinerary",
                HttpMethod.DELETE,
                null,
                Void::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)

            val getResponse = restTemplate.getForEntity(
                "/api/plans/$planId/itinerary",
                Map::class.java
            )
            assertThat(getResponse.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @Test
        fun `DELETE returns 404 when no itinerary exists`() {
            val response = restTemplate.exchange(
                "/api/plans/$planId/itinerary",
                HttpMethod.DELETE,
                null,
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @Nested
    inner class AddEvent {

        @Test
        fun `POST returns 201 with created event`() {
            fixture.insertItinerary(planId = planId)
            val eventAt = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS)

            val request = AddEventRequest(
                title = "Morning Hike",
                description = "Trail through the forest",
                details = "Bring water and snacks",
                eventAt = eventAt,
                category = "activity",
                estimatedCost = null,
                location = null,
                eventEndAt = null,
                links = null
            )

            val response = restTemplate.exchange(
                "/api/plans/$planId/itinerary/events",
                HttpMethod.POST,
                jsonEntity(request),
                ItineraryEventResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            assertThat(response.body!!.title).isEqualTo("Morning Hike")
            assertThat(response.body!!.description).isEqualTo("Trail through the forest")
            assertThat(response.body!!.details).isEqualTo("Bring water and snacks")
            assertThat(response.body!!.eventAt).isEqualTo(eventAt)
        }

        @Test
        fun `POST auto-creates itinerary on first event`() {
            val eventAt = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS)

            val request = AddEventRequest(
                title = "First Event",
                description = null,
                details = null,
                eventAt = eventAt,
                category = "other",
                estimatedCost = null,
                location = null,
                eventEndAt = null,
                links = null
            )

            val response = restTemplate.exchange(
                "/api/plans/$planId/itinerary/events",
                HttpMethod.POST,
                jsonEntity(request),
                ItineraryEventResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            assertThat(response.body!!.title).isEqualTo("First Event")

            val getResponse = restTemplate.getForEntity(
                "/api/plans/$planId/itinerary",
                ItineraryResponse::class.java
            )
            assertThat(getResponse.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(getResponse.body!!.events).hasSize(1)
            assertThat(getResponse.body!!.events[0].title).isEqualTo("First Event")
        }

        @Test
        fun `POST returns 400 when title is blank`() {
            val request = AddEventRequest(
                title = "",
                description = null,
                details = null,
                eventAt = Instant.now().plus(1, ChronoUnit.DAYS),
                category = "other",
                estimatedCost = null,
                location = null,
                eventEndAt = null,
                links = null
            )

            val response = restTemplate.exchange(
                "/api/plans/$planId/itinerary/events",
                HttpMethod.POST,
                jsonEntity(request),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        fun `POST returns 404 when plan does not exist`() {
            val request = AddEventRequest(
                title = "Event",
                description = null,
                details = null,
                eventAt = Instant.now().plus(1, ChronoUnit.DAYS),
                category = "other",
                estimatedCost = null,
                location = null,
                eventEndAt = null,
                links = null
            )

            val response = restTemplate.exchange(
                "/api/plans/${UUID.randomUUID()}/itinerary/events",
                HttpMethod.POST,
                jsonEntity(request),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @Nested
    inner class UpdateEvent {

        @Test
        fun `PUT returns 200 with updated event`() {
            val itineraryId = fixture.insertItinerary(planId = planId)
            val eventAt = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS)
            val eventId = fixture.insertEvent(
                itineraryId = itineraryId,
                title = "Old Title",
                eventAt = eventAt
            )
            val newEventAt = Instant.now().plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS)

            val request = UpdateEventRequest(
                title = "New Title",
                description = "Updated description",
                details = "Updated details",
                eventAt = newEventAt,
                category = "other",
                estimatedCost = null,
                location = null,
                eventEndAt = null,
                links = null
            )

            val response = restTemplate.exchange(
                "/api/plans/$planId/itinerary/events/$eventId",
                HttpMethod.PUT,
                jsonEntity(request),
                ItineraryEventResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.title).isEqualTo("New Title")
            assertThat(response.body!!.description).isEqualTo("Updated description")
            assertThat(response.body!!.details).isEqualTo("Updated details")
            assertThat(response.body!!.eventAt).isEqualTo(newEventAt)
        }

        @Test
        fun `PUT returns 404 when event does not exist`() {
            fixture.insertItinerary(planId = planId)

            val request = UpdateEventRequest(
                title = "Nope",
                description = null,
                details = null,
                eventAt = Instant.now().plus(1, ChronoUnit.DAYS),
                category = "other",
                estimatedCost = null,
                location = null,
                eventEndAt = null,
                links = null
            )

            val response = restTemplate.exchange(
                "/api/plans/$planId/itinerary/events/${UUID.randomUUID()}",
                HttpMethod.PUT,
                jsonEntity(request),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @Test
        fun `PUT returns 400 when title is blank`() {
            val itineraryId = fixture.insertItinerary(planId = planId)
            val eventId = fixture.insertEvent(
                itineraryId = itineraryId,
                title = "Existing",
                eventAt = Instant.now().plus(1, ChronoUnit.DAYS)
            )

            val request = UpdateEventRequest(
                title = "",
                description = null,
                details = null,
                eventAt = Instant.now().plus(1, ChronoUnit.DAYS),
                category = "other",
                estimatedCost = null,
                location = null,
                eventEndAt = null,
                links = null
            )

            val response = restTemplate.exchange(
                "/api/plans/$planId/itinerary/events/$eventId",
                HttpMethod.PUT,
                jsonEntity(request),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @Nested
    inner class DeleteEvent {

        @Test
        fun `DELETE returns 204`() {
            val itineraryId = fixture.insertItinerary(planId = planId)
            val eventId = fixture.insertEvent(
                itineraryId = itineraryId,
                title = "Doomed Event",
                eventAt = Instant.now().plus(1, ChronoUnit.DAYS)
            )

            val response = restTemplate.exchange(
                "/api/plans/$planId/itinerary/events/$eventId",
                HttpMethod.DELETE,
                null,
                Void::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        }

        @Test
        fun `DELETE returns 404 when event does not exist`() {
            fixture.insertItinerary(planId = planId)

            val response = restTemplate.exchange(
                "/api/plans/$planId/itinerary/events/${UUID.randomUUID()}",
                HttpMethod.DELETE,
                null,
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @Nested
    inner class ReadYourOwnWrites {

        @Test
        fun `POST multiple events then GET returns them ordered by event_at`() {
            val event1At = Instant.now().plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS)
            val event2At = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS)
            val event3At = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS)

            val request1 = AddEventRequest(title = "Third Day", description = null, details = null, eventAt = event1At, category = "other", estimatedCost = null, location = null, eventEndAt = null, links = null)
            val request2 = AddEventRequest(title = "First Day", description = null, details = null, eventAt = event2At, category = "other", estimatedCost = null, location = null, eventEndAt = null, links = null)
            val request3 = AddEventRequest(title = "Second Day", description = null, details = null, eventAt = event3At, category = "other", estimatedCost = null, location = null, eventEndAt = null, links = null)

            val post1 = restTemplate.exchange(
                "/api/plans/$planId/itinerary/events",
                HttpMethod.POST, jsonEntity(request1), ItineraryEventResponse::class.java
            )
            assertThat(post1.statusCode).isEqualTo(HttpStatus.CREATED)

            val post2 = restTemplate.exchange(
                "/api/plans/$planId/itinerary/events",
                HttpMethod.POST, jsonEntity(request2), ItineraryEventResponse::class.java
            )
            assertThat(post2.statusCode).isEqualTo(HttpStatus.CREATED)

            val post3 = restTemplate.exchange(
                "/api/plans/$planId/itinerary/events",
                HttpMethod.POST, jsonEntity(request3), ItineraryEventResponse::class.java
            )
            assertThat(post3.statusCode).isEqualTo(HttpStatus.CREATED)

            val getResponse = restTemplate.getForEntity(
                "/api/plans/$planId/itinerary",
                ItineraryResponse::class.java
            )

            assertThat(getResponse.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(getResponse.body!!.events).hasSize(3)
            assertThat(getResponse.body!!.events.map { it.title })
                .containsExactly("First Day", "Second Day", "Third Day")
        }

        @Test
        fun `POST event then GET itinerary returns the event`() {
            val eventAt = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS)
            val request = AddEventRequest(
                title = "Campfire",
                description = "Evening campfire",
                details = "Bring marshmallows",
                eventAt = eventAt,
                category = "meal",
                estimatedCost = null,
                location = null,
                eventEndAt = null,
                links = null
            )

            val postResponse = restTemplate.exchange(
                "/api/plans/$planId/itinerary/events",
                HttpMethod.POST,
                jsonEntity(request),
                ItineraryEventResponse::class.java
            )
            assertThat(postResponse.statusCode).isEqualTo(HttpStatus.CREATED)
            val eventId = postResponse.body!!.id

            val getResponse = restTemplate.getForEntity(
                "/api/plans/$planId/itinerary",
                ItineraryResponse::class.java
            )
            assertThat(getResponse.statusCode).isEqualTo(HttpStatus.OK)
            val events = getResponse.body!!.events
            assertThat(events).hasSize(1)
            assertThat(events[0].id).isEqualTo(eventId)
            assertThat(events[0].title).isEqualTo("Campfire")
            assertThat(events[0].description).isEqualTo("Evening campfire")
            assertThat(events[0].details).isEqualTo("Bring marshmallows")
            assertThat(events[0].eventAt).isEqualTo(eventAt)
        }

        @Test
        fun `POST event then DELETE event then GET itinerary returns empty events`() {
            val request = AddEventRequest(
                title = "Doomed Event",
                description = null,
                details = null,
                eventAt = Instant.now().plus(1, ChronoUnit.DAYS),
                category = "other",
                estimatedCost = null,
                location = null,
                eventEndAt = null,
                links = null
            )

            val postResponse = restTemplate.exchange(
                "/api/plans/$planId/itinerary/events",
                HttpMethod.POST,
                jsonEntity(request),
                ItineraryEventResponse::class.java
            )
            assertThat(postResponse.statusCode).isEqualTo(HttpStatus.CREATED)
            val eventId = postResponse.body!!.id

            val deleteResponse = restTemplate.exchange(
                "/api/plans/$planId/itinerary/events/$eventId",
                HttpMethod.DELETE,
                null,
                Void::class.java
            )
            assertThat(deleteResponse.statusCode).isEqualTo(HttpStatus.NO_CONTENT)

            val getResponse = restTemplate.getForEntity(
                "/api/plans/$planId/itinerary",
                ItineraryResponse::class.java
            )
            assertThat(getResponse.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(getResponse.body!!.events).isEmpty()
        }

        @Test
        fun `POST event then PUT update then GET itinerary returns updated event`() {
            val originalEventAt = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS)
            val createRequest = AddEventRequest(
                title = "Original",
                description = "Original desc",
                details = null,
                eventAt = originalEventAt,
                category = "other",
                estimatedCost = null,
                location = null,
                eventEndAt = null,
                links = null
            )

            val postResponse = restTemplate.exchange(
                "/api/plans/$planId/itinerary/events",
                HttpMethod.POST,
                jsonEntity(createRequest),
                ItineraryEventResponse::class.java
            )
            assertThat(postResponse.statusCode).isEqualTo(HttpStatus.CREATED)
            val eventId = postResponse.body!!.id

            val updatedEventAt = Instant.now().plus(5, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS)
            val updateRequest = UpdateEventRequest(
                title = "Updated",
                description = "Updated desc",
                details = "New details",
                eventAt = updatedEventAt,
                category = "other",
                estimatedCost = null,
                location = null,
                eventEndAt = null,
                links = null
            )

            val putResponse = restTemplate.exchange(
                "/api/plans/$planId/itinerary/events/$eventId",
                HttpMethod.PUT,
                jsonEntity(updateRequest),
                ItineraryEventResponse::class.java
            )
            assertThat(putResponse.statusCode).isEqualTo(HttpStatus.OK)

            val getResponse = restTemplate.getForEntity(
                "/api/plans/$planId/itinerary",
                ItineraryResponse::class.java
            )
            assertThat(getResponse.statusCode).isEqualTo(HttpStatus.OK)
            val event = getResponse.body!!.events[0]
            assertThat(event.id).isEqualTo(eventId)
            assertThat(event.title).isEqualTo("Updated")
            assertThat(event.description).isEqualTo("Updated desc")
            assertThat(event.details).isEqualTo("New details")
            assertThat(event.eventAt).isEqualTo(updatedEventAt)
        }
    }

    private fun jsonEntity(body: Any): HttpEntity<Any> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        return HttpEntity(body, headers)
    }
}
