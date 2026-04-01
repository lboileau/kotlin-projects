package com.acme.services.camperservice.features.itinerary.acceptance

import com.acme.services.camperservice.config.TestContainerConfig
import com.acme.services.camperservice.features.itinerary.acceptance.fixture.ItineraryFixture
import com.acme.services.camperservice.features.itinerary.dto.AddEventRequest
import com.acme.services.camperservice.features.itinerary.dto.ItineraryEventResponse
import com.acme.services.camperservice.features.itinerary.dto.ItineraryResponse
import com.acme.services.camperservice.features.itinerary.dto.LinkInput
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
import java.math.BigDecimal
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
        fun `GET returns totalEstimatedCost as sum of event costs`() {
            val itineraryId = fixture.insertItinerary(planId = planId)
            fixture.insertEvent(
                itineraryId = itineraryId, title = "Hike",
                eventAt = Instant.now().plus(1, ChronoUnit.DAYS),
                category = "activity", estimatedCost = BigDecimal("25.00")
            )
            fixture.insertEvent(
                itineraryId = itineraryId, title = "Dinner",
                eventAt = Instant.now().plus(1, ChronoUnit.DAYS).plus(6, ChronoUnit.HOURS),
                category = "meal", estimatedCost = BigDecimal("35.50")
            )

            val response = restTemplate.getForEntity(
                "/api/plans/$planId/itinerary",
                ItineraryResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.totalEstimatedCost).isEqualByComparingTo(BigDecimal("60.50"))
        }

        @Test
        fun `GET returns null totalEstimatedCost when no events have costs`() {
            val itineraryId = fixture.insertItinerary(planId = planId)
            fixture.insertEvent(
                itineraryId = itineraryId, title = "Free Event",
                eventAt = Instant.now().plus(1, ChronoUnit.DAYS)
            )

            val response = restTemplate.getForEntity(
                "/api/plans/$planId/itinerary",
                ItineraryResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.totalEstimatedCost).isNull()
        }

        @Test
        fun `GET returns null totalEstimatedCost when no events exist`() {
            fixture.insertItinerary(planId = planId)

            val response = restTemplate.getForEntity(
                "/api/plans/$planId/itinerary",
                ItineraryResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.events).isEmpty()
            assertThat(response.body!!.totalEstimatedCost).isNull()
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
        fun `POST returns 201 with all metadata fields`() {
            fixture.insertItinerary(planId = planId)
            val eventAt = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS)
            val eventEndAt = Instant.now().plus(1, ChronoUnit.DAYS).plus(4, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MILLIS)

            val request = AddEventRequest(
                title = "Morning Hike",
                description = "Trail through the forest",
                details = "Bring water and snacks",
                eventAt = eventAt,
                category = "activity",
                estimatedCost = BigDecimal("25.50"),
                location = "Eagle Peak Trailhead",
                eventEndAt = eventEndAt,
                links = null
            )

            val response = restTemplate.exchange(
                "/api/plans/$planId/itinerary/events",
                HttpMethod.POST,
                jsonEntity(request),
                ItineraryEventResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            val body = response.body!!
            assertThat(body.title).isEqualTo("Morning Hike")
            assertThat(body.description).isEqualTo("Trail through the forest")
            assertThat(body.details).isEqualTo("Bring water and snacks")
            assertThat(body.eventAt).isEqualTo(eventAt)
            assertThat(body.category).isEqualTo("activity")
            assertThat(body.estimatedCost).isEqualByComparingTo(BigDecimal("25.50"))
            assertThat(body.location).isEqualTo("Eagle Peak Trailhead")
            assertThat(body.eventEndAt).isEqualTo(eventEndAt)
        }

        @Test
        fun `POST with minimal fields returns default category and nulls`() {
            val eventAt = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS)

            val request = AddEventRequest(
                title = "Simple Event",
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
            val body = response.body!!
            assertThat(body.category).isEqualTo("other")
            assertThat(body.estimatedCost).isNull()
            assertThat(body.location).isNull()
            assertThat(body.eventEndAt).isNull()
            assertThat(body.links).isEmpty()
        }

        @Test
        fun `POST with links returns links in response`() {
            val eventAt = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS)

            val request = AddEventRequest(
                title = "Hike",
                description = null,
                details = null,
                eventAt = eventAt,
                category = "activity",
                estimatedCost = null,
                location = null,
                eventEndAt = null,
                links = listOf(
                    LinkInput(url = "https://alltrails.com/hike", label = "AllTrails"),
                    LinkInput(url = "https://maps.google.com", label = null)
                )
            )

            val response = restTemplate.exchange(
                "/api/plans/$planId/itinerary/events",
                HttpMethod.POST,
                jsonEntity(request),
                ItineraryEventResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            val links = response.body!!.links
            assertThat(links).hasSize(2)
            assertThat(links[0].url).isEqualTo("https://alltrails.com/hike")
            assertThat(links[0].label).isEqualTo("AllTrails")
            assertThat(links[0].id).isNotNull()
            assertThat(links[0].createdAt).isNotNull()
            assertThat(links[1].url).isEqualTo("https://maps.google.com")
            assertThat(links[1].label).isNull()
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

            val getResponse = restTemplate.getForEntity(
                "/api/plans/$planId/itinerary",
                ItineraryResponse::class.java
            )
            assertThat(getResponse.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(getResponse.body!!.events).hasSize(1)
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
        fun `POST returns 400 for invalid category`() {
            val request = AddEventRequest(
                title = "Event",
                description = null,
                details = null,
                eventAt = Instant.now().plus(1, ChronoUnit.DAYS),
                category = "invalid",
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
        fun `POST returns 400 for negative estimatedCost`() {
            val request = AddEventRequest(
                title = "Event",
                description = null,
                details = null,
                eventAt = Instant.now().plus(1, ChronoUnit.DAYS),
                category = "other",
                estimatedCost = BigDecimal("-1.00"),
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
        fun `POST returns 400 when eventEndAt is before eventAt`() {
            val eventAt = Instant.now().plus(2, ChronoUnit.DAYS)
            val eventEndAt = Instant.now().plus(1, ChronoUnit.DAYS)

            val request = AddEventRequest(
                title = "Event",
                description = null,
                details = null,
                eventAt = eventAt,
                category = "other",
                estimatedCost = null,
                location = null,
                eventEndAt = eventEndAt,
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
        fun `POST returns 400 for more than 10 links`() {
            val tooManyLinks = (1..11).map { LinkInput(url = "https://example.com/$it", label = "Link $it") }

            val request = AddEventRequest(
                title = "Event",
                description = null,
                details = null,
                eventAt = Instant.now().plus(1, ChronoUnit.DAYS),
                category = "other",
                estimatedCost = null,
                location = null,
                eventEndAt = null,
                links = tooManyLinks
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
        fun `POST returns 400 for blank link URL`() {
            val request = AddEventRequest(
                title = "Event",
                description = null,
                details = null,
                eventAt = Instant.now().plus(1, ChronoUnit.DAYS),
                category = "other",
                estimatedCost = null,
                location = null,
                eventEndAt = null,
                links = listOf(LinkInput(url = "", label = "Empty"))
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
        fun `PUT returns 200 with updated metadata fields`() {
            val itineraryId = fixture.insertItinerary(planId = planId)
            val eventAt = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS)
            val eventId = fixture.insertEvent(
                itineraryId = itineraryId,
                title = "Old Title",
                eventAt = eventAt,
                category = "other"
            )
            val newEventAt = Instant.now().plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS)
            val newEventEndAt = Instant.now().plus(3, ChronoUnit.DAYS).plus(4, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MILLIS)

            val request = UpdateEventRequest(
                title = "New Title",
                description = "Updated description",
                details = "Updated details",
                eventAt = newEventAt,
                category = "activity",
                estimatedCost = BigDecimal("42.00"),
                location = "New Location",
                eventEndAt = newEventEndAt,
                links = null
            )

            val response = restTemplate.exchange(
                "/api/plans/$planId/itinerary/events/$eventId",
                HttpMethod.PUT,
                jsonEntity(request),
                ItineraryEventResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val body = response.body!!
            assertThat(body.title).isEqualTo("New Title")
            assertThat(body.description).isEqualTo("Updated description")
            assertThat(body.details).isEqualTo("Updated details")
            assertThat(body.eventAt).isEqualTo(newEventAt)
            assertThat(body.category).isEqualTo("activity")
            assertThat(body.estimatedCost).isEqualByComparingTo(BigDecimal("42.00"))
            assertThat(body.location).isEqualTo("New Location")
            assertThat(body.eventEndAt).isEqualTo(newEventEndAt)
        }

        @Test
        fun `PUT with links replaces existing links`() {
            val eventAt = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS)

            // Create event with initial links
            val createRequest = AddEventRequest(
                title = "Event",
                description = null,
                details = null,
                eventAt = eventAt,
                category = "other",
                estimatedCost = null,
                location = null,
                eventEndAt = null,
                links = listOf(LinkInput(url = "https://old.com", label = "Old"))
            )
            val createResponse = restTemplate.exchange(
                "/api/plans/$planId/itinerary/events",
                HttpMethod.POST,
                jsonEntity(createRequest),
                ItineraryEventResponse::class.java
            )
            assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
            val eventId = createResponse.body!!.id

            // Update with new links
            val updateRequest = UpdateEventRequest(
                title = "Event",
                description = null,
                details = null,
                eventAt = eventAt,
                category = "other",
                estimatedCost = null,
                location = null,
                eventEndAt = null,
                links = listOf(
                    LinkInput(url = "https://new1.com", label = "New 1"),
                    LinkInput(url = "https://new2.com", label = "New 2")
                )
            )

            val response = restTemplate.exchange(
                "/api/plans/$planId/itinerary/events/$eventId",
                HttpMethod.PUT,
                jsonEntity(updateRequest),
                ItineraryEventResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val links = response.body!!.links
            assertThat(links).hasSize(2)
            assertThat(links.map { it.url }).containsExactly("https://new1.com", "https://new2.com")
        }

        @Test
        fun `PUT with null links preserves existing links`() {
            val eventAt = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS)

            // Create event with links
            val createRequest = AddEventRequest(
                title = "Event",
                description = null,
                details = null,
                eventAt = eventAt,
                category = "other",
                estimatedCost = null,
                location = null,
                eventEndAt = null,
                links = listOf(LinkInput(url = "https://keep.com", label = "Keep"))
            )
            val createResponse = restTemplate.exchange(
                "/api/plans/$planId/itinerary/events",
                HttpMethod.POST,
                jsonEntity(createRequest),
                ItineraryEventResponse::class.java
            )
            assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
            val eventId = createResponse.body!!.id

            // Update with null links — should preserve
            val updateRequest = UpdateEventRequest(
                title = "Updated Title",
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
                "/api/plans/$planId/itinerary/events/$eventId",
                HttpMethod.PUT,
                jsonEntity(updateRequest),
                ItineraryEventResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.title).isEqualTo("Updated Title")
            val links = response.body!!.links
            assertThat(links).hasSize(1)
            assertThat(links[0].url).isEqualTo("https://keep.com")
        }

        @Test
        fun `PUT with empty links clears all links`() {
            val eventAt = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS)

            // Create event with links
            val createRequest = AddEventRequest(
                title = "Event",
                description = null,
                details = null,
                eventAt = eventAt,
                category = "other",
                estimatedCost = null,
                location = null,
                eventEndAt = null,
                links = listOf(LinkInput(url = "https://remove.com", label = "Remove"))
            )
            val createResponse = restTemplate.exchange(
                "/api/plans/$planId/itinerary/events",
                HttpMethod.POST,
                jsonEntity(createRequest),
                ItineraryEventResponse::class.java
            )
            assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
            val eventId = createResponse.body!!.id

            // Update with empty links — should clear
            val updateRequest = UpdateEventRequest(
                title = "Event",
                description = null,
                details = null,
                eventAt = eventAt,
                category = "other",
                estimatedCost = null,
                location = null,
                eventEndAt = null,
                links = emptyList()
            )

            val response = restTemplate.exchange(
                "/api/plans/$planId/itinerary/events/$eventId",
                HttpMethod.PUT,
                jsonEntity(updateRequest),
                ItineraryEventResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.links).isEmpty()
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

        @Test
        fun `PUT returns 400 for invalid category`() {
            val itineraryId = fixture.insertItinerary(planId = planId)
            val eventId = fixture.insertEvent(
                itineraryId = itineraryId,
                title = "Event",
                eventAt = Instant.now().plus(1, ChronoUnit.DAYS)
            )

            val request = UpdateEventRequest(
                title = "Event",
                description = null,
                details = null,
                eventAt = Instant.now().plus(1, ChronoUnit.DAYS),
                category = "bogus",
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

        @Test
        fun `PUT returns 400 for negative estimatedCost`() {
            val itineraryId = fixture.insertItinerary(planId = planId)
            val eventId = fixture.insertEvent(
                itineraryId = itineraryId,
                title = "Event",
                eventAt = Instant.now().plus(1, ChronoUnit.DAYS)
            )

            val request = UpdateEventRequest(
                title = "Event",
                description = null,
                details = null,
                eventAt = Instant.now().plus(1, ChronoUnit.DAYS),
                category = "other",
                estimatedCost = BigDecimal("-5.00"),
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

        @Test
        fun `PUT returns 400 when eventEndAt is before eventAt`() {
            val itineraryId = fixture.insertItinerary(planId = planId)
            val eventId = fixture.insertEvent(
                itineraryId = itineraryId,
                title = "Event",
                eventAt = Instant.now().plus(1, ChronoUnit.DAYS)
            )

            val eventAt = Instant.now().plus(3, ChronoUnit.DAYS)
            val eventEndAt = Instant.now().plus(1, ChronoUnit.DAYS)

            val request = UpdateEventRequest(
                title = "Event",
                description = null,
                details = null,
                eventAt = eventAt,
                category = "other",
                estimatedCost = null,
                location = null,
                eventEndAt = eventEndAt,
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

        @Test
        fun `PUT returns 400 for more than 10 links`() {
            val itineraryId = fixture.insertItinerary(planId = planId)
            val eventId = fixture.insertEvent(
                itineraryId = itineraryId,
                title = "Event",
                eventAt = Instant.now().plus(1, ChronoUnit.DAYS)
            )
            val tooManyLinks = (1..11).map { LinkInput(url = "https://example.com/$it", label = "Link $it") }

            val request = UpdateEventRequest(
                title = "Event",
                description = null,
                details = null,
                eventAt = Instant.now().plus(1, ChronoUnit.DAYS),
                category = "other",
                estimatedCost = null,
                location = null,
                eventEndAt = null,
                links = tooManyLinks
            )

            val response = restTemplate.exchange(
                "/api/plans/$planId/itinerary/events/$eventId",
                HttpMethod.PUT,
                jsonEntity(request),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        fun `PUT returns 400 for blank link URL`() {
            val itineraryId = fixture.insertItinerary(planId = planId)
            val eventId = fixture.insertEvent(
                itineraryId = itineraryId,
                title = "Event",
                eventAt = Instant.now().plus(1, ChronoUnit.DAYS)
            )

            val request = UpdateEventRequest(
                title = "Event",
                description = null,
                details = null,
                eventAt = Instant.now().plus(1, ChronoUnit.DAYS),
                category = "other",
                estimatedCost = null,
                location = null,
                eventEndAt = null,
                links = listOf(LinkInput(url = "", label = "Empty"))
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

            restTemplate.exchange("/api/plans/$planId/itinerary/events", HttpMethod.POST, jsonEntity(request1), ItineraryEventResponse::class.java)
            restTemplate.exchange("/api/plans/$planId/itinerary/events", HttpMethod.POST, jsonEntity(request2), ItineraryEventResponse::class.java)
            restTemplate.exchange("/api/plans/$planId/itinerary/events", HttpMethod.POST, jsonEntity(request3), ItineraryEventResponse::class.java)

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
        fun `POST event with metadata and links then GET returns them`() {
            val eventAt = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS)
            val eventEndAt = Instant.now().plus(1, ChronoUnit.DAYS).plus(4, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MILLIS)

            val request = AddEventRequest(
                title = "Mountain Hike",
                description = "Trail to Eagle Peak",
                details = "Bring water and sunscreen",
                eventAt = eventAt,
                category = "activity",
                estimatedCost = BigDecimal("25.50"),
                location = "Eagle Peak Trailhead",
                eventEndAt = eventEndAt,
                links = listOf(
                    LinkInput(url = "https://alltrails.com/hike", label = "AllTrails"),
                    LinkInput(url = "https://maps.google.com", label = null)
                )
            )

            val postResponse = restTemplate.exchange(
                "/api/plans/$planId/itinerary/events",
                HttpMethod.POST,
                jsonEntity(request),
                ItineraryEventResponse::class.java
            )
            assertThat(postResponse.statusCode).isEqualTo(HttpStatus.CREATED)

            val getResponse = restTemplate.getForEntity(
                "/api/plans/$planId/itinerary",
                ItineraryResponse::class.java
            )
            assertThat(getResponse.statusCode).isEqualTo(HttpStatus.OK)

            val event = getResponse.body!!.events[0]
            assertThat(event.title).isEqualTo("Mountain Hike")
            assertThat(event.category).isEqualTo("activity")
            assertThat(event.estimatedCost).isEqualByComparingTo(BigDecimal("25.50"))
            assertThat(event.location).isEqualTo("Eagle Peak Trailhead")
            assertThat(event.eventEndAt).isEqualTo(eventEndAt)
            assertThat(event.links).hasSize(2)
            assertThat(event.links[0].url).isEqualTo("https://alltrails.com/hike")
            assertThat(event.links[0].label).isEqualTo("AllTrails")
            assertThat(event.links[1].url).isEqualTo("https://maps.google.com")

            assertThat(getResponse.body!!.totalEstimatedCost).isEqualByComparingTo(BigDecimal("25.50"))
        }

        @Test
        fun `POST then PUT with metadata and links then GET returns updated values`() {
            val eventAt = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS)

            // Create event with initial state
            val createRequest = AddEventRequest(
                title = "Original",
                description = null,
                details = null,
                eventAt = eventAt,
                category = "other",
                estimatedCost = null,
                location = null,
                eventEndAt = null,
                links = listOf(LinkInput(url = "https://old.com", label = "Old"))
            )

            val postResponse = restTemplate.exchange(
                "/api/plans/$planId/itinerary/events",
                HttpMethod.POST,
                jsonEntity(createRequest),
                ItineraryEventResponse::class.java
            )
            assertThat(postResponse.statusCode).isEqualTo(HttpStatus.CREATED)
            val eventId = postResponse.body!!.id

            // Update with new metadata and links
            val updatedEventAt = Instant.now().plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS)
            val updatedEventEndAt = Instant.now().plus(3, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MILLIS)

            val updateRequest = UpdateEventRequest(
                title = "Updated Hike",
                description = "New description",
                details = "New details",
                eventAt = updatedEventAt,
                category = "activity",
                estimatedCost = BigDecimal("50.00"),
                location = "West Trail",
                eventEndAt = updatedEventEndAt,
                links = listOf(LinkInput(url = "https://new.com", label = "New Link"))
            )

            val putResponse = restTemplate.exchange(
                "/api/plans/$planId/itinerary/events/$eventId",
                HttpMethod.PUT,
                jsonEntity(updateRequest),
                ItineraryEventResponse::class.java
            )
            assertThat(putResponse.statusCode).isEqualTo(HttpStatus.OK)

            // Verify via GET
            val getResponse = restTemplate.getForEntity(
                "/api/plans/$planId/itinerary",
                ItineraryResponse::class.java
            )
            assertThat(getResponse.statusCode).isEqualTo(HttpStatus.OK)

            val event = getResponse.body!!.events[0]
            assertThat(event.id).isEqualTo(eventId)
            assertThat(event.title).isEqualTo("Updated Hike")
            assertThat(event.description).isEqualTo("New description")
            assertThat(event.details).isEqualTo("New details")
            assertThat(event.eventAt).isEqualTo(updatedEventAt)
            assertThat(event.category).isEqualTo("activity")
            assertThat(event.estimatedCost).isEqualByComparingTo(BigDecimal("50.00"))
            assertThat(event.location).isEqualTo("West Trail")
            assertThat(event.eventEndAt).isEqualTo(updatedEventEndAt)
            assertThat(event.links).hasSize(1)
            assertThat(event.links[0].url).isEqualTo("https://new.com")

            assertThat(getResponse.body!!.totalEstimatedCost).isEqualByComparingTo(BigDecimal("50.00"))
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
    }

    private fun jsonEntity(body: Any): HttpEntity<Any> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        return HttpEntity(body, headers)
    }
}
