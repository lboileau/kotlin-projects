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
import com.acme.clients.itineraryclient.api.ItineraryClient
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
            handle.createUpdate("TRUNCATE TABLE itinerary_events, itineraries, plan_members, plans, users CASCADE").execute()
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
        fun `addEvent adds event to itinerary`() {
            val itinerary = (client.create(CreateItineraryParam(planId)) as Result.Success).value
            val eventAt = Instant.parse("2026-07-15T10:00:00Z")

            val result = client.addEvent(
                AddEventParam(
                    itineraryId = itinerary.id,
                    title = "Morning Hike",
                    description = "Trail to the summit",
                    details = "Bring water and sunscreen",
                    eventAt = eventAt,
                    category = "activity",
                    estimatedCost = null,
                    location = "Summit Trail",
                    eventEndAt = null
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
            assertThat(event.location).isEqualTo("Summit Trail")
            assertThat(event.id).isNotNull()
            assertThat(event.createdAt).isNotNull()
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
    }

    @Nested
    inner class GetEvents {
        @Test
        fun `getEvents returns events ordered by event_at`() {
            val itinerary = (client.create(CreateItineraryParam(planId)) as Result.Success).value
            val laterTime = Instant.parse("2026-07-15T14:00:00Z")
            val earlierTime = Instant.parse("2026-07-15T08:00:00Z")

            client.addEvent(
                AddEventParam(
                    itineraryId = itinerary.id,
                    title = "Afternoon Swim",
                    description = null,
                    details = null,
                    eventAt = laterTime,
                    category = "activity",
                    estimatedCost = null,
                    location = null,
                    eventEndAt = null
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
                    estimatedCost = null,
                    location = null,
                    eventEndAt = null
                )
            )

            val result = client.getEvents(GetEventsParam(itinerary.id))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val events = (result as Result.Success).value
            assertThat(events).hasSize(2)
            assertThat(events[0].title).isEqualTo("Morning Coffee")
            assertThat(events[1].title).isEqualTo("Afternoon Swim")
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
        fun `updateEvent updates event fields`() {
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
            val result = client.updateEvent(
                UpdateEventParam(
                    id = event.id,
                    title = "New Title",
                    description = "New desc",
                    details = "New details",
                    eventAt = newEventAt,
                    category = "activity",
                    estimatedCost = null,
                    location = "New Location",
                    eventEndAt = null
                )
            )
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val updated = (result as Result.Success).value
            assertThat(updated.id).isEqualTo(event.id)
            assertThat(updated.title).isEqualTo("New Title")
            assertThat(updated.description).isEqualTo("New desc")
            assertThat(updated.details).isEqualTo("New details")
            assertThat(updated.eventAt).isEqualTo(newEventAt)
            assertThat(updated.updatedAt).isAfterOrEqualTo(event.updatedAt)
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
    }
}
