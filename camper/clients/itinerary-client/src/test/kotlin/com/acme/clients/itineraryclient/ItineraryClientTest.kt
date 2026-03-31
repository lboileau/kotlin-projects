package com.acme.clients.itineraryclient

import com.acme.clients.common.Result
import com.acme.clients.common.error.ConflictError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.error.ValidationError
import com.acme.clients.itineraryclient.api.AddEventParam
import com.acme.clients.itineraryclient.api.CreateItineraryParam
import com.acme.clients.itineraryclient.api.DeleteEventParam
import com.acme.clients.itineraryclient.api.DeleteItineraryParam
import com.acme.clients.itineraryclient.api.GetByPlanIdParam
import com.acme.clients.itineraryclient.api.GetEventsParam
import com.acme.clients.itineraryclient.api.GetLinksByEventIdsParam
import com.acme.clients.itineraryclient.api.ItineraryClient
import com.acme.clients.itineraryclient.api.LinkInput
import com.acme.clients.itineraryclient.api.ReplaceEventLinksParam
import com.acme.clients.itineraryclient.api.UpdateEventParam
import com.acme.clients.itineraryclient.test.ItineraryTestDb
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Testcontainers
class ItineraryClientTest {

    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("camper_db")
            .withUsername("postgres")
            .withPassword("postgres")

        private lateinit var client: ItineraryClient
        private lateinit var jdbi: org.jdbi.v3.core.Jdbi

        @BeforeAll
        @JvmStatic
        fun setup() {
            ItineraryTestDb.cleanAndMigrate(postgres.jdbcUrl, postgres.username, postgres.password)

            System.setProperty("DB_URL", postgres.jdbcUrl)
            System.setProperty("DB_USER", postgres.username)
            System.setProperty("DB_PASSWORD", postgres.password)
            client = createItineraryClient()
            jdbi = org.jdbi.v3.core.Jdbi.create(postgres.jdbcUrl, postgres.username, postgres.password)
        }
    }

    private lateinit var userId: UUID
    private lateinit var planId: UUID

    @BeforeEach
    fun truncateAndSeed() {
        jdbi.withHandle<Unit, Exception> { handle ->
            handle.createUpdate("TRUNCATE TABLE itinerary_event_links, itinerary_events, itineraries, plan_members, plans, users CASCADE").execute()
        }
        userId = UUID.randomUUID()
        planId = UUID.randomUUID()
        jdbi.withHandle<Unit, Exception> { handle ->
            handle.createUpdate("INSERT INTO users (id, email, username) VALUES (:id, :email, :username)")
                .bind("id", userId).bind("email", "test@example.com").bind("username", "tester").execute()
            handle.createUpdate("INSERT INTO plans (id, name, visibility, owner_id) VALUES (:id, :name, :visibility, :ownerId)")
                .bind("id", planId).bind("name", "Test Plan").bind("visibility", "private").bind("ownerId", userId).execute()
        }
    }

    @Nested
    inner class GetByPlanId {
        @Test
        fun `getByPlanId returns NotFoundError when no itinerary exists`() {
            val result = client.getByPlanId(GetByPlanIdParam(planId))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(NotFoundError::class.java)
        }

        @Test
        fun `getByPlanId returns itinerary after creation`() {
            val created = (client.create(CreateItineraryParam(planId)) as Result.Success).value

            val result = client.getByPlanId(GetByPlanIdParam(planId))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val found = (result as Result.Success).value
            assertThat(found.id).isEqualTo(created.id)
            assertThat(found.planId).isEqualTo(planId)
            assertThat(found.createdAt).isNotNull()
            assertThat(found.updatedAt).isNotNull()
        }
    }

    @Nested
    inner class Create {
        @Test
        fun `create returns created itinerary`() {
            val result = client.create(CreateItineraryParam(planId))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val itinerary = (result as Result.Success).value
            assertThat(itinerary.planId).isEqualTo(planId)
            assertThat(itinerary.id).isNotNull()
            assertThat(itinerary.createdAt).isNotNull()
            assertThat(itinerary.updatedAt).isNotNull()
        }

        @Test
        fun `create returns ConflictError when itinerary already exists for plan`() {
            client.create(CreateItineraryParam(planId))

            val result = client.create(CreateItineraryParam(planId))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ConflictError::class.java)
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `delete deletes itinerary`() {
            client.create(CreateItineraryParam(planId))

            val result = client.delete(DeleteItineraryParam(planId))
            assertThat(result).isInstanceOf(Result.Success::class.java)

            val getResult = client.getByPlanId(GetByPlanIdParam(planId))
            assertThat(getResult).isInstanceOf(Result.Failure::class.java)
        }

        @Test
        fun `delete returns NotFoundError when itinerary does not exist`() {
            val result = client.delete(DeleteItineraryParam(planId))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(NotFoundError::class.java)
        }
    }

    @Nested
    inner class AddEvent {
        @Test
        fun `addEvent adds event with all metadata fields`() {
            val itinerary = (client.create(CreateItineraryParam(planId)) as Result.Success).value
            val eventAt = Instant.parse("2026-07-15T10:00:00Z")
            val eventEndAt = Instant.parse("2026-07-15T14:00:00Z")

            val result = client.addEvent(
                AddEventParam(
                    itineraryId = itinerary.id,
                    title = "Morning Hike",
                    description = "Trail to the summit",
                    details = "Bring water and sunscreen",
                    eventAt = eventAt,
                    category = "activity",
                    estimatedCost = BigDecimal("25.50"),
                    location = "Summit Trail",
                    eventEndAt = eventEndAt
                )
            )
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val event = (result as Result.Success).value
            assertThat(event.itineraryId).isEqualTo(itinerary.id)
            assertThat(event.title).isEqualTo("Morning Hike")
            assertThat(event.description).isEqualTo("Trail to the summit")
            assertThat(event.details).isEqualTo("Bring water and sunscreen")
            assertThat(event.eventAt).isEqualTo(eventAt)
            assertThat(event.category).isEqualTo("activity")
            assertThat(event.estimatedCost).isEqualByComparingTo(BigDecimal("25.50"))
            assertThat(event.location).isEqualTo("Summit Trail")
            assertThat(event.eventEndAt).isEqualTo(eventEndAt)
            assertThat(event.id).isNotNull()
            assertThat(event.createdAt).isNotNull()
        }

        @Test
        fun `addEvent with category only and other metadata fields null`() {
            val itinerary = (client.create(CreateItineraryParam(planId)) as Result.Success).value

            val result = client.addEvent(
                AddEventParam(
                    itineraryId = itinerary.id,
                    title = "Simple Event",
                    description = null,
                    details = null,
                    eventAt = Instant.parse("2026-07-15T10:00:00Z"),
                    category = "travel",
                    estimatedCost = null,
                    location = null,
                    eventEndAt = null
                )
            )
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val event = (result as Result.Success).value
            assertThat(event.category).isEqualTo("travel")
            assertThat(event.estimatedCost).isNull()
            assertThat(event.location).isNull()
            assertThat(event.eventEndAt).isNull()
        }

        @Test
        fun `addEvent with zero estimated cost`() {
            val itinerary = (client.create(CreateItineraryParam(planId)) as Result.Success).value

            val result = client.addEvent(
                AddEventParam(
                    itineraryId = itinerary.id,
                    title = "Free Activity",
                    description = null,
                    details = null,
                    eventAt = Instant.parse("2026-07-15T10:00:00Z"),
                    category = "activity",
                    estimatedCost = BigDecimal.ZERO,
                    location = null,
                    eventEndAt = null
                )
            )
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val event = (result as Result.Success).value
            assertThat(event.estimatedCost).isEqualByComparingTo(BigDecimal.ZERO)
        }

        @Test
        fun `addEvent returns ValidationError when title is blank`() {
            val itinerary = (client.create(CreateItineraryParam(planId)) as Result.Success).value

            val result = client.addEvent(
                AddEventParam(
                    itineraryId = itinerary.id,
                    title = "",
                    description = null,
                    details = null,
                    eventAt = Instant.now(),
                    category = "other",
                    estimatedCost = null,
                    location = null,
                    eventEndAt = null
                )
            )
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
            assertThat((error as ValidationError).field).isEqualTo("title")
        }

        @Test
        fun `addEvent returns ValidationError for invalid category`() {
            val itinerary = (client.create(CreateItineraryParam(planId)) as Result.Success).value

            val result = client.addEvent(
                AddEventParam(
                    itineraryId = itinerary.id,
                    title = "Event",
                    description = null,
                    details = null,
                    eventAt = Instant.now(),
                    category = "invalid_category",
                    estimatedCost = null,
                    location = null,
                    eventEndAt = null
                )
            )
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
            assertThat((error as ValidationError).field).isEqualTo("category")
        }

        @Test
        fun `addEvent returns ValidationError for negative estimated cost`() {
            val itinerary = (client.create(CreateItineraryParam(planId)) as Result.Success).value

            val result = client.addEvent(
                AddEventParam(
                    itineraryId = itinerary.id,
                    title = "Event",
                    description = null,
                    details = null,
                    eventAt = Instant.now(),
                    category = "other",
                    estimatedCost = BigDecimal("-1.00"),
                    location = null,
                    eventEndAt = null
                )
            )
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
            assertThat((error as ValidationError).field).isEqualTo("estimatedCost")
        }

        @Test
        fun `addEvent returns ValidationError when eventEndAt is before eventAt`() {
            val itinerary = (client.create(CreateItineraryParam(planId)) as Result.Success).value
            val eventAt = Instant.parse("2026-07-15T14:00:00Z")
            val eventEndAt = Instant.parse("2026-07-15T10:00:00Z")

            val result = client.addEvent(
                AddEventParam(
                    itineraryId = itinerary.id,
                    title = "Event",
                    description = null,
                    details = null,
                    eventAt = eventAt,
                    category = "other",
                    estimatedCost = null,
                    location = null,
                    eventEndAt = eventEndAt
                )
            )
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
            assertThat((error as ValidationError).field).isEqualTo("eventEndAt")
        }

        @Test
        fun `addEvent returns ValidationError when eventEndAt equals eventAt`() {
            val itinerary = (client.create(CreateItineraryParam(planId)) as Result.Success).value
            val eventAt = Instant.parse("2026-07-15T14:00:00Z")

            val result = client.addEvent(
                AddEventParam(
                    itineraryId = itinerary.id,
                    title = "Event",
                    description = null,
                    details = null,
                    eventAt = eventAt,
                    category = "other",
                    estimatedCost = null,
                    location = null,
                    eventEndAt = eventAt
                )
            )
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
            assertThat((error as ValidationError).field).isEqualTo("eventEndAt")
        }

        @Test
        fun `addEvent with each valid category`() {
            val itinerary = (client.create(CreateItineraryParam(planId)) as Result.Success).value
            val validCategories = listOf("travel", "accommodation", "activity", "meal", "other")

            for (category in validCategories) {
                val result = client.addEvent(
                    AddEventParam(
                        itineraryId = itinerary.id,
                        title = "Event $category",
                        description = null,
                        details = null,
                        eventAt = Instant.now(),
                        category = category,
                        estimatedCost = null,
                        location = null,
                        eventEndAt = null
                    )
                )
                assertThat(result).isInstanceOf(Result.Success::class.java)
                val event = (result as Result.Success).value
                assertThat(event.category).isEqualTo(category)
            }
        }
    }

    @Nested
    inner class GetEvents {
        @Test
        fun `getEvents returns events ordered by event_at`() {
            val itinerary = (client.create(CreateItineraryParam(planId)) as Result.Success).value
            val laterTime = Instant.parse("2026-07-15T14:00:00Z")
            val earlierTime = Instant.parse("2026-07-15T08:00:00Z")

            val swimEndTime = Instant.parse("2026-07-15T16:00:00Z")
            client.addEvent(
                AddEventParam(
                    itineraryId = itinerary.id,
                    title = "Afternoon Swim",
                    description = null,
                    details = null,
                    eventAt = laterTime,
                    category = "activity",
                    estimatedCost = BigDecimal("10.00"),
                    location = "Lake",
                    eventEndAt = swimEndTime
                )
            )
            client.addEvent(
                AddEventParam(
                    itineraryId = itinerary.id,
                    title = "Morning Coffee",
                    description = null,
                    details = null,
                    eventAt = earlierTime,
                    category = "meal",
                    estimatedCost = BigDecimal("5.00"),
                    location = "Campsite",
                    eventEndAt = null
                )
            )

            val result = client.getEvents(GetEventsParam(itinerary.id))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val events = (result as Result.Success).value
            assertThat(events).hasSize(2)
            assertThat(events[0].title).isEqualTo("Morning Coffee")
            assertThat(events[0].category).isEqualTo("meal")
            assertThat(events[0].estimatedCost).isEqualByComparingTo(BigDecimal("5.00"))
            assertThat(events[0].location).isEqualTo("Campsite")
            assertThat(events[0].eventEndAt).isNull()
            assertThat(events[1].title).isEqualTo("Afternoon Swim")
            assertThat(events[1].category).isEqualTo("activity")
            assertThat(events[1].estimatedCost).isEqualByComparingTo(BigDecimal("10.00"))
            assertThat(events[1].location).isEqualTo("Lake")
            assertThat(events[1].eventEndAt).isEqualTo(swimEndTime)
        }

        @Test
        fun `getEvents returns empty list when no events exist`() {
            val itinerary = (client.create(CreateItineraryParam(planId)) as Result.Success).value

            val result = client.getEvents(GetEventsParam(itinerary.id))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            assertThat((result as Result.Success).value).isEmpty()
        }
    }

    @Nested
    inner class UpdateEvent {
        @Test
        fun `updateEvent updates all fields including metadata`() {
            val itinerary = (client.create(CreateItineraryParam(planId)) as Result.Success).value
            val event = (client.addEvent(
                AddEventParam(
                    itineraryId = itinerary.id,
                    title = "Old Title",
                    description = "Old desc",
                    details = "Old details",
                    eventAt = Instant.parse("2026-07-15T10:00:00Z"),
                    category = "other",
                    estimatedCost = null,
                    location = null,
                    eventEndAt = null
                )
            ) as Result.Success).value

            val newEventAt = Instant.parse("2026-07-16T12:00:00Z")
            val newEventEndAt = Instant.parse("2026-07-16T16:00:00Z")
            val result = client.updateEvent(
                UpdateEventParam(
                    id = event.id,
                    title = "New Title",
                    description = "New desc",
                    details = "New details",
                    eventAt = newEventAt,
                    category = "activity",
                    estimatedCost = BigDecimal("42.00"),
                    location = "New Location",
                    eventEndAt = newEventEndAt
                )
            )
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val updated = (result as Result.Success).value
            assertThat(updated.id).isEqualTo(event.id)
            assertThat(updated.title).isEqualTo("New Title")
            assertThat(updated.description).isEqualTo("New desc")
            assertThat(updated.details).isEqualTo("New details")
            assertThat(updated.eventAt).isEqualTo(newEventAt)
            assertThat(updated.category).isEqualTo("activity")
            assertThat(updated.estimatedCost).isEqualByComparingTo(BigDecimal("42.00"))
            assertThat(updated.location).isEqualTo("New Location")
            assertThat(updated.eventEndAt).isEqualTo(newEventEndAt)
            assertThat(updated.updatedAt).isAfterOrEqualTo(event.updatedAt)

            // Read-your-own-writes: verify via getEvents
            val fetchedEvents = (client.getEvents(GetEventsParam(itinerary.id)) as Result.Success).value
            assertThat(fetchedEvents).hasSize(1)
            val fetched = fetchedEvents[0]
            assertThat(fetched.id).isEqualTo(event.id)
            assertThat(fetched.title).isEqualTo("New Title")
            assertThat(fetched.description).isEqualTo("New desc")
            assertThat(fetched.details).isEqualTo("New details")
            assertThat(fetched.eventAt).isEqualTo(newEventAt)
            assertThat(fetched.category).isEqualTo("activity")
            assertThat(fetched.estimatedCost).isEqualByComparingTo(BigDecimal("42.00"))
            assertThat(fetched.location).isEqualTo("New Location")
            assertThat(fetched.eventEndAt).isEqualTo(newEventEndAt)
        }

        @Test
        fun `updateEvent clears nullable metadata fields to null`() {
            val itinerary = (client.create(CreateItineraryParam(planId)) as Result.Success).value
            val event = (client.addEvent(
                AddEventParam(
                    itineraryId = itinerary.id,
                    title = "Event",
                    description = "Has desc",
                    details = "Has details",
                    eventAt = Instant.parse("2026-07-15T10:00:00Z"),
                    category = "activity",
                    estimatedCost = BigDecimal("100.00"),
                    location = "Some Place",
                    eventEndAt = Instant.parse("2026-07-15T14:00:00Z")
                )
            ) as Result.Success).value

            val result = client.updateEvent(
                UpdateEventParam(
                    id = event.id,
                    title = "Event",
                    description = null,
                    details = null,
                    eventAt = Instant.parse("2026-07-15T10:00:00Z"),
                    category = "other",
                    estimatedCost = null,
                    location = null,
                    eventEndAt = null
                )
            )
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val updated = (result as Result.Success).value
            assertThat(updated.estimatedCost).isNull()
            assertThat(updated.location).isNull()
            assertThat(updated.eventEndAt).isNull()
            assertThat(updated.description).isNull()
            assertThat(updated.details).isNull()
        }

        @Test
        fun `updateEvent returns ValidationError when title is blank`() {
            val itinerary = (client.create(CreateItineraryParam(planId)) as Result.Success).value
            val event = (client.addEvent(
                AddEventParam(
                    itineraryId = itinerary.id,
                    title = "Valid Title",
                    description = null,
                    details = null,
                    eventAt = Instant.now(),
                    category = "other",
                    estimatedCost = null,
                    location = null,
                    eventEndAt = null
                )
            ) as Result.Success).value

            val result = client.updateEvent(
                UpdateEventParam(
                    id = event.id,
                    title = "",
                    description = null,
                    details = null,
                    eventAt = Instant.now(),
                    category = "other",
                    estimatedCost = null,
                    location = null,
                    eventEndAt = null
                )
            )
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
            assertThat((error as ValidationError).field).isEqualTo("title")
        }

        @Test
        fun `updateEvent returns NotFoundError for non-existent event`() {
            val result = client.updateEvent(
                UpdateEventParam(
                    id = UUID.randomUUID(),
                    title = "Nope",
                    description = null,
                    details = null,
                    eventAt = Instant.now(),
                    category = "other",
                    estimatedCost = null,
                    location = null,
                    eventEndAt = null
                )
            )
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(NotFoundError::class.java)
        }

        @Test
        fun `updateEvent returns ValidationError for invalid category`() {
            val itinerary = (client.create(CreateItineraryParam(planId)) as Result.Success).value
            val event = (client.addEvent(
                AddEventParam(
                    itineraryId = itinerary.id,
                    title = "Event",
                    description = null,
                    details = null,
                    eventAt = Instant.now(),
                    category = "other",
                    estimatedCost = null,
                    location = null,
                    eventEndAt = null
                )
            ) as Result.Success).value

            val result = client.updateEvent(
                UpdateEventParam(
                    id = event.id,
                    title = "Event",
                    description = null,
                    details = null,
                    eventAt = Instant.now(),
                    category = "bogus",
                    estimatedCost = null,
                    location = null,
                    eventEndAt = null
                )
            )
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
            assertThat((error as ValidationError).field).isEqualTo("category")
        }

        @Test
        fun `updateEvent returns ValidationError for negative estimated cost`() {
            val itinerary = (client.create(CreateItineraryParam(planId)) as Result.Success).value
            val event = (client.addEvent(
                AddEventParam(
                    itineraryId = itinerary.id,
                    title = "Event",
                    description = null,
                    details = null,
                    eventAt = Instant.now(),
                    category = "other",
                    estimatedCost = null,
                    location = null,
                    eventEndAt = null
                )
            ) as Result.Success).value

            val result = client.updateEvent(
                UpdateEventParam(
                    id = event.id,
                    title = "Event",
                    description = null,
                    details = null,
                    eventAt = Instant.now(),
                    category = "other",
                    estimatedCost = BigDecimal("-5.00"),
                    location = null,
                    eventEndAt = null
                )
            )
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
            assertThat((error as ValidationError).field).isEqualTo("estimatedCost")
        }

        @Test
        fun `updateEvent returns ValidationError when eventEndAt is before eventAt`() {
            val itinerary = (client.create(CreateItineraryParam(planId)) as Result.Success).value
            val event = (client.addEvent(
                AddEventParam(
                    itineraryId = itinerary.id,
                    title = "Event",
                    description = null,
                    details = null,
                    eventAt = Instant.now(),
                    category = "other",
                    estimatedCost = null,
                    location = null,
                    eventEndAt = null
                )
            ) as Result.Success).value

            val eventAt = Instant.parse("2026-07-15T14:00:00Z")
            val eventEndAt = Instant.parse("2026-07-15T10:00:00Z")

            val result = client.updateEvent(
                UpdateEventParam(
                    id = event.id,
                    title = "Event",
                    description = null,
                    details = null,
                    eventAt = eventAt,
                    category = "other",
                    estimatedCost = null,
                    location = null,
                    eventEndAt = eventEndAt
                )
            )
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
            assertThat((error as ValidationError).field).isEqualTo("eventEndAt")
        }
    }

    @Nested
    inner class DeleteEvent {
        @Test
        fun `deleteEvent deletes event`() {
            val itinerary = (client.create(CreateItineraryParam(planId)) as Result.Success).value
            val event = (client.addEvent(
                AddEventParam(
                    itineraryId = itinerary.id,
                    title = "Doomed Event",
                    description = null,
                    details = null,
                    eventAt = Instant.now(),
                    category = "other",
                    estimatedCost = null,
                    location = null,
                    eventEndAt = null
                )
            ) as Result.Success).value

            val result = client.deleteEvent(DeleteEventParam(event.id))
            assertThat(result).isInstanceOf(Result.Success::class.java)

            val events = client.getEvents(GetEventsParam(itinerary.id))
            assertThat((events as Result.Success).value).isEmpty()
        }

        @Test
        fun `deleteEvent returns NotFoundError for non-existent event`() {
            val result = client.deleteEvent(DeleteEventParam(UUID.randomUUID()))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(NotFoundError::class.java)
        }
    }

    @Nested
    inner class GetLinksByEventIds {
        @Test
        fun `getLinksByEventIds returns links for given event IDs ordered by created_at`() {
            val itinerary = (client.create(CreateItineraryParam(planId)) as Result.Success).value
            val event = (client.addEvent(
                AddEventParam(
                    itineraryId = itinerary.id,
                    title = "Hike",
                    description = null,
                    details = null,
                    eventAt = Instant.now(),
                    category = "activity",
                    estimatedCost = null,
                    location = null,
                    eventEndAt = null
                )
            ) as Result.Success).value

            client.replaceEventLinks(
                ReplaceEventLinksParam(
                    eventId = event.id,
                    links = listOf(
                        LinkInput(url = "https://alltrails.com/hike", label = "AllTrails"),
                        LinkInput(url = "https://maps.google.com", label = "Google Maps")
                    )
                )
            )

            val result = client.getLinksByEventIds(GetLinksByEventIdsParam(listOf(event.id)))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val links = (result as Result.Success).value
            assertThat(links).hasSize(2)
            assertThat(links[0].url).isEqualTo("https://alltrails.com/hike")
            assertThat(links[0].label).isEqualTo("AllTrails")
            assertThat(links[0].eventId).isEqualTo(event.id)
            assertThat(links[0].id).isNotNull()
            assertThat(links[0].createdAt).isNotNull()
            assertThat(links[1].url).isEqualTo("https://maps.google.com")
            assertThat(links[1].label).isEqualTo("Google Maps")
        }

        @Test
        fun `getLinksByEventIds returns links across multiple events`() {
            val itinerary = (client.create(CreateItineraryParam(planId)) as Result.Success).value
            val event1 = (client.addEvent(
                AddEventParam(
                    itineraryId = itinerary.id,
                    title = "Event 1",
                    description = null,
                    details = null,
                    eventAt = Instant.now(),
                    category = "activity",
                    estimatedCost = null,
                    location = null,
                    eventEndAt = null
                )
            ) as Result.Success).value
            val event2 = (client.addEvent(
                AddEventParam(
                    itineraryId = itinerary.id,
                    title = "Event 2",
                    description = null,
                    details = null,
                    eventAt = Instant.now(),
                    category = "meal",
                    estimatedCost = null,
                    location = null,
                    eventEndAt = null
                )
            ) as Result.Success).value

            client.replaceEventLinks(
                ReplaceEventLinksParam(event1.id, listOf(LinkInput("https://example.com/1", "Link 1")))
            )
            client.replaceEventLinks(
                ReplaceEventLinksParam(event2.id, listOf(LinkInput("https://example.com/2", "Link 2")))
            )

            val result = client.getLinksByEventIds(GetLinksByEventIdsParam(listOf(event1.id, event2.id)))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val links = (result as Result.Success).value
            assertThat(links).hasSize(2)
            val eventIds = links.map { it.eventId }.toSet()
            assertThat(eventIds).containsExactlyInAnyOrder(event1.id, event2.id)
        }

        @Test
        fun `getLinksByEventIds returns empty list for empty input`() {
            val result = client.getLinksByEventIds(GetLinksByEventIdsParam(emptyList()))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            assertThat((result as Result.Success).value).isEmpty()
        }

        @Test
        fun `getLinksByEventIds returns empty list when no links exist`() {
            val itinerary = (client.create(CreateItineraryParam(planId)) as Result.Success).value
            val event = (client.addEvent(
                AddEventParam(
                    itineraryId = itinerary.id,
                    title = "No Links Event",
                    description = null,
                    details = null,
                    eventAt = Instant.now(),
                    category = "other",
                    estimatedCost = null,
                    location = null,
                    eventEndAt = null
                )
            ) as Result.Success).value

            val result = client.getLinksByEventIds(GetLinksByEventIdsParam(listOf(event.id)))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            assertThat((result as Result.Success).value).isEmpty()
        }
    }

    @Nested
    inner class ReplaceEventLinks {
        @Test
        fun `replaceEventLinks creates links for an event`() {
            val itinerary = (client.create(CreateItineraryParam(planId)) as Result.Success).value
            val event = (client.addEvent(
                AddEventParam(
                    itineraryId = itinerary.id,
                    title = "Event",
                    description = null,
                    details = null,
                    eventAt = Instant.now(),
                    category = "other",
                    estimatedCost = null,
                    location = null,
                    eventEndAt = null
                )
            ) as Result.Success).value

            val result = client.replaceEventLinks(
                ReplaceEventLinksParam(
                    eventId = event.id,
                    links = listOf(
                        LinkInput(url = "https://example.com", label = "Example"),
                        LinkInput(url = "https://other.com", label = null)
                    )
                )
            )
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val links = (result as Result.Success).value
            assertThat(links).hasSize(2)
            assertThat(links[0].url).isEqualTo("https://example.com")
            assertThat(links[0].label).isEqualTo("Example")
            assertThat(links[0].eventId).isEqualTo(event.id)
            assertThat(links[0].id).isNotNull()
            assertThat(links[0].createdAt).isNotNull()
            assertThat(links[1].url).isEqualTo("https://other.com")
            assertThat(links[1].label).isNull()
        }

        @Test
        fun `replaceEventLinks replaces existing links`() {
            val itinerary = (client.create(CreateItineraryParam(planId)) as Result.Success).value
            val event = (client.addEvent(
                AddEventParam(
                    itineraryId = itinerary.id,
                    title = "Event",
                    description = null,
                    details = null,
                    eventAt = Instant.now(),
                    category = "other",
                    estimatedCost = null,
                    location = null,
                    eventEndAt = null
                )
            ) as Result.Success).value

            // Create initial links
            val initial = (client.replaceEventLinks(
                ReplaceEventLinksParam(
                    eventId = event.id,
                    links = listOf(LinkInput("https://old.com", "Old"))
                )
            ) as Result.Success).value
            val oldLinkId = initial[0].id

            // Replace with new links
            val result = client.replaceEventLinks(
                ReplaceEventLinksParam(
                    eventId = event.id,
                    links = listOf(
                        LinkInput("https://new1.com", "New 1"),
                        LinkInput("https://new2.com", "New 2")
                    )
                )
            )
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val links = (result as Result.Success).value
            assertThat(links).hasSize(2)
            assertThat(links.map { it.url }).containsExactly("https://new1.com", "https://new2.com")
            // Old link should be gone
            assertThat(links.map { it.id }).doesNotContain(oldLinkId)

            // Verify via getLinksByEventIds
            val fetched = (client.getLinksByEventIds(GetLinksByEventIdsParam(listOf(event.id))) as Result.Success).value
            assertThat(fetched).hasSize(2)
            assertThat(fetched.map { it.url }).containsExactly("https://new1.com", "https://new2.com")
        }

        @Test
        fun `replaceEventLinks with empty list deletes all links`() {
            val itinerary = (client.create(CreateItineraryParam(planId)) as Result.Success).value
            val event = (client.addEvent(
                AddEventParam(
                    itineraryId = itinerary.id,
                    title = "Event",
                    description = null,
                    details = null,
                    eventAt = Instant.now(),
                    category = "other",
                    estimatedCost = null,
                    location = null,
                    eventEndAt = null
                )
            ) as Result.Success).value

            // Create links first
            client.replaceEventLinks(
                ReplaceEventLinksParam(
                    eventId = event.id,
                    links = listOf(LinkInput("https://example.com", "Example"))
                )
            )

            // Replace with empty list
            val result = client.replaceEventLinks(
                ReplaceEventLinksParam(eventId = event.id, links = emptyList())
            )
            assertThat(result).isInstanceOf(Result.Success::class.java)
            assertThat((result as Result.Success).value).isEmpty()

            // Verify links are gone
            val fetched = (client.getLinksByEventIds(GetLinksByEventIdsParam(listOf(event.id))) as Result.Success).value
            assertThat(fetched).isEmpty()
        }

        @Test
        fun `replaceEventLinks returns ValidationError for more than 10 links`() {
            val itinerary = (client.create(CreateItineraryParam(planId)) as Result.Success).value
            val event = (client.addEvent(
                AddEventParam(
                    itineraryId = itinerary.id,
                    title = "Event",
                    description = null,
                    details = null,
                    eventAt = Instant.now(),
                    category = "other",
                    estimatedCost = null,
                    location = null,
                    eventEndAt = null
                )
            ) as Result.Success).value

            val tooManyLinks = (1..11).map { LinkInput("https://example.com/$it", "Link $it") }

            val result = client.replaceEventLinks(
                ReplaceEventLinksParam(eventId = event.id, links = tooManyLinks)
            )
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
            assertThat((error as ValidationError).field).isEqualTo("links")
        }

        @Test
        fun `replaceEventLinks returns ValidationError for blank URL`() {
            val itinerary = (client.create(CreateItineraryParam(planId)) as Result.Success).value
            val event = (client.addEvent(
                AddEventParam(
                    itineraryId = itinerary.id,
                    title = "Event",
                    description = null,
                    details = null,
                    eventAt = Instant.now(),
                    category = "other",
                    estimatedCost = null,
                    location = null,
                    eventEndAt = null
                )
            ) as Result.Success).value

            val result = client.replaceEventLinks(
                ReplaceEventLinksParam(
                    eventId = event.id,
                    links = listOf(LinkInput("", "Empty URL"))
                )
            )
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
            assertThat((error as ValidationError).field).isEqualTo("url")
        }

        @Test
        fun `replaceEventLinks with exactly 10 links succeeds`() {
            val itinerary = (client.create(CreateItineraryParam(planId)) as Result.Success).value
            val event = (client.addEvent(
                AddEventParam(
                    itineraryId = itinerary.id,
                    title = "Event",
                    description = null,
                    details = null,
                    eventAt = Instant.now(),
                    category = "other",
                    estimatedCost = null,
                    location = null,
                    eventEndAt = null
                )
            ) as Result.Success).value

            val tenLinks = (1..10).map { LinkInput("https://example.com/$it", "Link $it") }

            val result = client.replaceEventLinks(
                ReplaceEventLinksParam(eventId = event.id, links = tenLinks)
            )
            assertThat(result).isInstanceOf(Result.Success::class.java)
            assertThat((result as Result.Success).value).hasSize(10)
        }
    }

    @Nested
    inner class CascadeDelete {
        @Test
        fun `deleting itinerary deletes all its events`() {
            val itinerary = (client.create(CreateItineraryParam(planId)) as Result.Success).value
            client.addEvent(
                AddEventParam(
                    itineraryId = itinerary.id,
                    title = "Event 1",
                    description = null,
                    details = null,
                    eventAt = Instant.now(),
                    category = "other",
                    estimatedCost = null,
                    location = null,
                    eventEndAt = null
                )
            )
            client.addEvent(
                AddEventParam(
                    itineraryId = itinerary.id,
                    title = "Event 2",
                    description = null,
                    details = null,
                    eventAt = Instant.now(),
                    category = "other",
                    estimatedCost = null,
                    location = null,
                    eventEndAt = null
                )
            )

            client.delete(DeleteItineraryParam(planId))

            // Verify events are gone by querying directly
            val count = jdbi.withHandle<Int, Exception> { handle ->
                handle.createQuery("SELECT COUNT(*) FROM itinerary_events WHERE itinerary_id = :id")
                    .bind("id", itinerary.id)
                    .mapTo(Int::class.java)
                    .one()
            }
            assertThat(count).isZero()
        }

        @Test
        fun `deleting event deletes its links`() {
            val itinerary = (client.create(CreateItineraryParam(planId)) as Result.Success).value
            val event = (client.addEvent(
                AddEventParam(
                    itineraryId = itinerary.id,
                    title = "Event with links",
                    description = null,
                    details = null,
                    eventAt = Instant.now(),
                    category = "other",
                    estimatedCost = null,
                    location = null,
                    eventEndAt = null
                )
            ) as Result.Success).value

            client.replaceEventLinks(
                ReplaceEventLinksParam(
                    eventId = event.id,
                    links = listOf(
                        LinkInput("https://example.com", "Example"),
                        LinkInput("https://other.com", "Other")
                    )
                )
            )

            client.deleteEvent(DeleteEventParam(event.id))

            // Verify links are gone by querying directly
            val count = jdbi.withHandle<Int, Exception> { handle ->
                handle.createQuery("SELECT COUNT(*) FROM itinerary_event_links WHERE event_id = :id")
                    .bind("id", event.id)
                    .mapTo(Int::class.java)
                    .one()
            }
            assertThat(count).isZero()
        }

        @Test
        fun `deleting itinerary deletes event links`() {
            val itinerary = (client.create(CreateItineraryParam(planId)) as Result.Success).value
            val event = (client.addEvent(
                AddEventParam(
                    itineraryId = itinerary.id,
                    title = "Event with links",
                    description = null,
                    details = null,
                    eventAt = Instant.now(),
                    category = "other",
                    estimatedCost = null,
                    location = null,
                    eventEndAt = null
                )
            ) as Result.Success).value

            client.replaceEventLinks(
                ReplaceEventLinksParam(
                    eventId = event.id,
                    links = listOf(LinkInput("https://example.com", "Example"))
                )
            )

            client.delete(DeleteItineraryParam(planId))

            // Verify links are gone by querying directly
            val count = jdbi.withHandle<Int, Exception> { handle ->
                handle.createQuery("SELECT COUNT(*) FROM itinerary_event_links WHERE event_id = :id")
                    .bind("id", event.id)
                    .mapTo(Int::class.java)
                    .one()
            }
            assertThat(count).isZero()
        }
    }
}
