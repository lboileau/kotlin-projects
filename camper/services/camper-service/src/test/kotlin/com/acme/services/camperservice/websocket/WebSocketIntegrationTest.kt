package com.acme.services.camperservice.websocket

import com.acme.services.camperservice.config.TestContainerConfig
import com.acme.services.camperservice.features.plan.dto.CreatePlanRequest
import com.acme.services.camperservice.features.plan.dto.PlanResponse
import com.acme.services.camperservice.features.plan.dto.UpdatePlanRequest
import com.fasterxml.jackson.databind.ObjectMapper
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
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.support.AbstractSubscribableChannel
import org.springframework.messaging.Message
import org.springframework.messaging.support.ChannelInterceptor
import java.util.UUID
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestContainerConfig::class)
class WebSocketIntegrationTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    lateinit var brokerChannel: AbstractSubscribableChannel

    @Autowired
    lateinit var objectMapper: ObjectMapper

    private lateinit var ownerId: UUID
    private val capturedMessages = ArrayBlockingQueue<CapturedMessage>(50)

    data class CapturedMessage(val destination: String, val body: PlanUpdateMessage)

    @BeforeEach
    fun setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE assignment_members, assignments, itinerary_events, itineraries, items, plan_members, plans, users CASCADE")
        ownerId = insertUser("owner@example.com", "owner")
        capturedMessages.clear()

        brokerChannel.addInterceptor(object : ChannelInterceptor {
            override fun preSend(message: Message<*>, channel: org.springframework.messaging.MessageChannel): Message<*> {
                val headers = message.headers
                val destination = headers["simpDestination"] as? String
                if (destination != null && destination.startsWith("/topic/plans/")) {
                    val payload = message.payload
                    val body = when (payload) {
                        is ByteArray -> objectMapper.readValue(payload, PlanUpdateMessage::class.java)
                        is PlanUpdateMessage -> payload
                        else -> null
                    }
                    if (body != null) {
                        capturedMessages.add(CapturedMessage(destination, body))
                    }
                }
                return message
            }
        })
    }

    @Nested
    inner class PlanUpdates {

        @Test
        fun `publishes update message when plan is updated`() {
            val planId = createPlan("Test Trip")

            restTemplate.exchange(
                "/api/plans/$planId",
                HttpMethod.PUT,
                entityWithUser(UpdatePlanRequest(name = "Updated Trip"), ownerId),
                PlanResponse::class.java
            )

            val captured = capturedMessages.poll(5, TimeUnit.SECONDS)
            assertThat(captured).isNotNull
            assertThat(captured!!.destination).isEqualTo("/topic/plans/$planId")
            assertThat(captured.body.resource).isEqualTo("plan")
            assertThat(captured.body.action).isEqualTo("updated")
        }

        @Test
        fun `publishes delete message when plan is deleted`() {
            val planId = createPlan("Doomed Trip")

            restTemplate.exchange(
                "/api/plans/$planId",
                HttpMethod.DELETE,
                entityWithUser(null, ownerId),
                Void::class.java
            )

            val captured = capturedMessages.poll(5, TimeUnit.SECONDS)
            assertThat(captured).isNotNull
            assertThat(captured!!.destination).isEqualTo("/topic/plans/$planId")
            assertThat(captured.body.resource).isEqualTo("plan")
            assertThat(captured.body.action).isEqualTo("deleted")
        }
    }

    @Nested
    inner class MemberUpdates {

        @Test
        fun `publishes members update when member is added`() {
            val planId = createPlan("Group Trip")
            insertUser("new@example.com", "newuser")

            restTemplate.exchange(
                "/api/plans/$planId/members",
                HttpMethod.POST,
                entityWithUser(mapOf("email" to "new@example.com"), ownerId),
                Map::class.java
            )

            val captured = capturedMessages.poll(5, TimeUnit.SECONDS)
            assertThat(captured).isNotNull
            assertThat(captured!!.destination).isEqualTo("/topic/plans/$planId")
            assertThat(captured.body.resource).isEqualTo("members")
            assertThat(captured.body.action).isEqualTo("updated")
        }

        @Test
        fun `publishes members update when member is removed`() {
            val planId = createPlan("Group Trip")
            val memberId = insertUser("member@example.com", "member")
            jdbcTemplate.update(
                "INSERT INTO plan_members (plan_id, user_id, created_at) VALUES (?, ?, NOW())",
                planId, memberId
            )

            restTemplate.exchange(
                "/api/plans/$planId/members/$memberId",
                HttpMethod.DELETE,
                entityWithUser(null, ownerId),
                Void::class.java
            )

            val captured = capturedMessages.poll(5, TimeUnit.SECONDS)
            assertThat(captured).isNotNull
            assertThat(captured!!.body.resource).isEqualTo("members")
        }
    }

    @Nested
    inner class AssignmentUpdates {

        @Test
        fun `publishes assignments update when assignment is created`() {
            val planId = createPlan("Trip")

            restTemplate.exchange(
                "/api/plans/$planId/assignments",
                HttpMethod.POST,
                entityWithUser(mapOf("name" to "Big Tent", "type" to "tent", "maxOccupancy" to 4), ownerId),
                Map::class.java
            )

            val captured = capturedMessages.poll(5, TimeUnit.SECONDS)
            assertThat(captured).isNotNull
            assertThat(captured!!.destination).isEqualTo("/topic/plans/$planId")
            assertThat(captured.body.resource).isEqualTo("assignments")
            assertThat(captured.body.action).isEqualTo("updated")
        }

        @Test
        fun `publishes assignments update when assignment is deleted`() {
            val planId = createPlan("Trip")
            val assignmentId = insertAssignment(planId, "Old Tent", "tent")

            restTemplate.exchange(
                "/api/plans/$planId/assignments/$assignmentId",
                HttpMethod.DELETE,
                entityWithUser(null, ownerId),
                Void::class.java
            )

            val captured = capturedMessages.poll(5, TimeUnit.SECONDS)
            assertThat(captured).isNotNull
            assertThat(captured!!.body.resource).isEqualTo("assignments")
        }
    }

    @Nested
    inner class ItineraryUpdates {

        @Test
        fun `publishes itinerary update when event is added`() {
            val planId = createPlan("Trip")

            restTemplate.exchange(
                "/api/plans/$planId/itinerary/events",
                HttpMethod.POST,
                entityWithUser(mapOf("title" to "Hike", "eventAt" to "2026-06-15T09:00:00Z", "category" to "activity", "estimatedCost" to null, "location" to null, "eventEndAt" to null, "links" to null), ownerId),
                Map::class.java
            )

            val captured = capturedMessages.poll(5, TimeUnit.SECONDS)
            assertThat(captured).isNotNull
            assertThat(captured!!.destination).isEqualTo("/topic/plans/$planId")
            assertThat(captured.body.resource).isEqualTo("itinerary")
            assertThat(captured.body.action).isEqualTo("updated")
        }
    }

    @Nested
    inner class NoMessageOnFailure {

        @Test
        fun `does not publish message when update fails`() {
            val planId = createPlan("Trip")
            val nonOwnerId = insertUser("nonowner@example.com", "nonowner")

            val response = restTemplate.exchange(
                "/api/plans/$planId",
                HttpMethod.PUT,
                entityWithUser(UpdatePlanRequest(name = "Renamed"), nonOwnerId),
                Map::class.java
            )
            assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)

            val captured = capturedMessages.poll(2, TimeUnit.SECONDS)
            assertThat(captured).isNull()
        }
    }

    private fun createPlan(name: String): UUID {
        val response = restTemplate.exchange(
            "/api/plans",
            HttpMethod.POST,
            entityWithUser(CreatePlanRequest(name = name), ownerId),
            PlanResponse::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        return response.body!!.id
    }

    private fun insertAssignment(planId: UUID, name: String, type: String): UUID {
        val id = UUID.randomUUID()
        jdbcTemplate.update(
            "INSERT INTO assignments (id, plan_id, name, type, max_occupancy, owner_id, created_at, updated_at) VALUES (?, ?, ?, ?, 4, ?, NOW(), NOW())",
            id, planId, name, type, ownerId
        )
        return id
    }

    private fun insertUser(email: String, username: String): UUID {
        val id = UUID.randomUUID()
        jdbcTemplate.update(
            "INSERT INTO users (id, email, username, created_at, updated_at) VALUES (?, ?, ?, NOW(), NOW())",
            id, email, username
        )
        return id
    }

    private fun entityWithUser(body: Any?, userId: UUID): HttpEntity<Any?> {
        val headers = HttpHeaders()
        headers.set("X-User-Id", userId.toString())
        headers.set("Content-Type", "application/json")
        return HttpEntity(body, headers)
    }
}
