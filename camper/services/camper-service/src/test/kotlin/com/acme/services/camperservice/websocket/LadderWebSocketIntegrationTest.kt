package com.acme.services.camperservice.websocket

import com.acme.services.camperservice.config.TestContainerConfig
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
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.support.AbstractSubscribableChannel
import org.springframework.messaging.support.ChannelInterceptor
import java.util.UUID
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestContainerConfig::class)
class LadderWebSocketIntegrationTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    lateinit var brokerChannel: AbstractSubscribableChannel

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var presenceTracker: LadderPresenceTracker

    private lateinit var creatorId: UUID
    private lateinit var userId2: UUID

    // Each test instance gets its own queue. Old interceptors from prior instances write to
    // their own (now-stale) queues — no cross-test message pollution.
    private val capturedMessages = ArrayBlockingQueue<CapturedMessage>(100)

    data class CapturedMessage(val destination: String, val body: LadderUpdateMessage)

    @BeforeEach
    fun setUp() {
        jdbcTemplate.execute(
            "TRUNCATE TABLE ladder_votes, ladder_participants, ladder_activities, activity_ladders, users CASCADE"
        )
        creatorId = insertUser("creator@example.com", "creator")
        userId2 = insertUser("user2@example.com", "user2")
        capturedMessages.clear()

        brokerChannel.addInterceptor(object : ChannelInterceptor {
            override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
                val destination = message.headers["simpDestination"] as? String
                if (destination?.startsWith("/topic/ladders/") == true) {
                    val payload = message.payload
                    val body = when (payload) {
                        is ByteArray -> runCatching {
                            objectMapper.readValue(payload, LadderUpdateMessage::class.java)
                        }.getOrNull()
                        is LadderUpdateMessage -> payload
                        else -> null
                    }
                    if (body != null) {
                        capturedMessages.offer(CapturedMessage(destination, body))
                    }
                }
                return message
            }
        })
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private fun insertUser(email: String, username: String): UUID {
        val id = UUID.randomUUID()
        jdbcTemplate.update(
            "INSERT INTO users (id, email, username, created_at, updated_at) VALUES (?, ?, ?, NOW(), NOW())",
            id, email, username,
        )
        return id
    }

    private fun entityWithUser(body: Any?, userId: UUID): HttpEntity<Any?> {
        val headers = HttpHeaders()
        headers.set("X-User-Id", userId.toString())
        headers.set("Content-Type", "application/json")
        return HttpEntity(body, headers)
    }

    private fun createLadder(): UUID {
        val body = mapOf(
            "title" to "WS Test Ladder",
            "activities" to listOf(
                mapOf("name" to "Hiking", "imageUrl" to "https://example.com/h.jpg", "distanceMinutes" to 60, "costPerPerson" to 10),
                mapOf("name" to "Kayaking", "imageUrl" to "https://example.com/k.jpg", "distanceMinutes" to 90, "costPerPerson" to 25),
            ),
        )
        val response = restTemplate.exchange(
            "/api/ladders", HttpMethod.POST, entityWithUser(body, creatorId), Map::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        @Suppress("UNCHECKED_CAST")
        return UUID.fromString((response.body as Map<String, Any?>)["id"] as String)
    }

    /**
     * Simulates presence for a user and starts the ladder.
     * Clears ALL ladder messages (presence-changed, started, round-started) so tests that
     * call startLadder() have a clean capturedMessages queue when they return.
     */
    private fun startLadder(ladderId: UUID, userId: UUID = creatorId) {
        val sessionId = UUID.randomUUID().toString()
        presenceTracker.subscribed(sessionId, ladderId, userId)
        // Drain presence-changed before calling start
        Thread.sleep(50)
        capturedMessages.clear()

        val response = restTemplate.exchange(
            "/api/ladders/$ladderId/start", HttpMethod.POST, entityWithUser(null, userId), Map::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        // Drain the started + round-started messages emitted by StartLadderAction
        capturedMessages.clear()
    }

    @Suppress("UNCHECKED_CAST")
    private fun getActivityIds(ladderId: UUID): List<UUID> {
        val response = restTemplate.exchange(
            "/api/ladders/$ladderId", HttpMethod.GET, entityWithUser(null, creatorId), Map::class.java
        )
        val detail = response.body as Map<String, Any?>
        val activities = detail["activities"] as List<Map<String, Any?>>
        return activities.map { UUID.fromString(it["id"] as String) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun getCurrentMatchAId(ladderId: UUID): UUID {
        val response = restTemplate.exchange(
            "/api/ladders/$ladderId", HttpMethod.GET, entityWithUser(null, creatorId), Map::class.java
        )
        val detail = response.body as Map<String, Any?>
        val currentRound = detail["currentRound"] as Map<String, Any?>
        return UUID.fromString(currentRound["activityAId"] as String)
    }

    @Suppress("UNCHECKED_CAST")
    private fun getCurrentMatchBId(ladderId: UUID): UUID {
        val response = restTemplate.exchange(
            "/api/ladders/$ladderId", HttpMethod.GET, entityWithUser(null, creatorId), Map::class.java
        )
        val detail = response.body as Map<String, Any?>
        val currentRound = detail["currentRound"] as Map<String, Any?>
        return UUID.fromString(currentRound["activityBId"] as String)
    }

    private fun pollMessage(timeoutSeconds: Long = 5): CapturedMessage? =
        capturedMessages.poll(timeoutSeconds, TimeUnit.SECONDS)

    // ── Test groups ─────────────────────────────────────────────────────────────

    @Nested
    inner class ActivityAddedEvent {

        @Test
        fun `POST adds activity publishes activity-added message`() {
            val ladderId = createLadder()
            capturedMessages.clear()

            val body = mapOf(
                "name" to "Biking",
                "imageUrl" to "https://example.com/bike.jpg",
                "distanceMinutes" to 45,
                "costPerPerson" to 15,
            )
            restTemplate.exchange(
                "/api/ladders/$ladderId/activities", HttpMethod.POST, entityWithUser(body, creatorId), Map::class.java
            )

            val captured = pollMessage()
            assertThat(captured).isNotNull
            assertThat(captured!!.destination).isEqualTo("/topic/ladders/$ladderId")
            assertThat(captured.body.resource).isEqualTo("ladder")
            assertThat(captured.body.action).isEqualTo("activity-added")
        }

        @Test
        fun `no message published when add activity fails due to permission`() {
            val ladderId = createLadder()
            capturedMessages.clear()

            val body = mapOf(
                "name" to "Biking",
                "imageUrl" to "https://example.com/bike.jpg",
                "distanceMinutes" to 45,
                "costPerPerson" to 15,
            )
            val response = restTemplate.exchange(
                "/api/ladders/$ladderId/activities", HttpMethod.POST, entityWithUser(body, userId2), Map::class.java
            )
            assertThat(response.statusCode.value()).isEqualTo(403)

            val captured = capturedMessages.poll(2, TimeUnit.SECONDS)
            assertThat(captured).isNull()
        }
    }

    @Nested
    inner class ActivityRemovedEvent {

        @Test
        fun `DELETE activity publishes activity-removed message`() {
            val ladderId = createLadder()
            val activityId = getActivityIds(ladderId).first()
            capturedMessages.clear()

            restTemplate.exchange(
                "/api/ladders/$ladderId/activities/$activityId",
                HttpMethod.DELETE,
                entityWithUser(null, creatorId),
                Void::class.java,
            )

            val captured = pollMessage()
            assertThat(captured).isNotNull
            assertThat(captured!!.destination).isEqualTo("/topic/ladders/$ladderId")
            assertThat(captured.body.action).isEqualTo("activity-removed")
        }
    }

    @Nested
    inner class StartedAndRoundStartedEvents {

        @Test
        fun `POST start publishes started then round-started messages`() {
            val ladderId = createLadder()
            val sessionId = UUID.randomUUID().toString()
            presenceTracker.subscribed(sessionId, ladderId, creatorId)
            Thread.sleep(50)
            capturedMessages.clear()

            restTemplate.exchange(
                "/api/ladders/$ladderId/start", HttpMethod.POST, entityWithUser(null, creatorId), Map::class.java
            )

            val msg1 = pollMessage()
            val msg2 = pollMessage()

            assertThat(msg1).isNotNull
            assertThat(msg1!!.destination).isEqualTo("/topic/ladders/$ladderId")
            assertThat(msg1.body.action).isEqualTo("started")

            assertThat(msg2).isNotNull
            assertThat(msg2!!.destination).isEqualTo("/topic/ladders/$ladderId")
            assertThat(msg2.body.action).isEqualTo("round-started")
            assertThat(msg2.body.payload).isNotNull()
            assertThat(msg2.body.payload!!["roundNumber"]).isEqualTo(1)
        }
    }

    @Nested
    inner class VoteCastEvent {

        @Test
        fun `POST vote publishes vote-cast with votersRemaining when not last voter`() {
            val ladderId = createLadder()
            // Two participants so a single vote won't resolve the round
            val session1 = UUID.randomUUID().toString()
            val session2 = UUID.randomUUID().toString()
            presenceTracker.subscribed(session1, ladderId, creatorId)
            presenceTracker.subscribed(session2, ladderId, userId2)
            Thread.sleep(50)
            capturedMessages.clear()

            restTemplate.exchange(
                "/api/ladders/$ladderId/start", HttpMethod.POST, entityWithUser(null, creatorId), Map::class.java
            )
            capturedMessages.clear() // discard started + round-started

            val activityAId = getCurrentMatchAId(ladderId)
            restTemplate.exchange(
                "/api/ladders/$ladderId/vote",
                HttpMethod.POST,
                entityWithUser(mapOf("votedForActivityId" to activityAId.toString()), creatorId),
                Map::class.java,
            )

            val captured = pollMessage()
            assertThat(captured).isNotNull
            assertThat(captured!!.body.action).isEqualTo("vote-cast")
            @Suppress("UNCHECKED_CAST")
            val payload = captured.body.payload as Map<String, Any?>
            assertThat(payload["voterId"]).isEqualTo(creatorId.toString())
            assertThat(payload["votersRemaining"]).isEqualTo(1)
        }
    }

    @Nested
    inner class RoundResolvedWithRoundStartedEvents {

        @Test
        fun `last vote of a decided round publishes vote-cast, round-resolved, round-started`() {
            val ladderId = createLadder()
            startLadder(ladderId) // 1 participant (creatorId), clears captured messages

            val activityAId = getCurrentMatchAId(ladderId)

            // Single voter — this vote resolves the round (RoundDecided)
            restTemplate.exchange(
                "/api/ladders/$ladderId/vote",
                HttpMethod.POST,
                entityWithUser(mapOf("votedForActivityId" to activityAId.toString()), creatorId),
                Map::class.java,
            )

            val msg1 = pollMessage()
            val msg2 = pollMessage()
            val msg3 = pollMessage()

            assertThat(msg1).isNotNull
            assertThat(msg1!!.body.action).isEqualTo("vote-cast")

            assertThat(msg2).isNotNull
            assertThat(msg2!!.body.action).isEqualTo("round-resolved")
            @Suppress("UNCHECKED_CAST")
            val resolvedPayload = msg2.body.payload as Map<String, Any?>
            assertThat(resolvedPayload["outcome"]).isEqualTo("decided")
            assertThat(resolvedPayload["winnerActivityId"]).isEqualTo(activityAId.toString())

            assertThat(msg3).isNotNull
            assertThat(msg3!!.body.action).isEqualTo("round-started")
            @Suppress("UNCHECKED_CAST")
            val roundPayload = msg3.body.payload as Map<String, Any?>
            assertThat(roundPayload["roundNumber"]).isEqualTo(2)
            assertThat(roundPayload["isFinalRound"]).isEqualTo(true)
        }

        @Test
        fun `last vote of a tied round publishes vote-cast, round-resolved with tie, round-started`() {
            val ladderId = createLadder()
            // Two participants — each votes for the opposite activity to force a tie
            val session1 = UUID.randomUUID().toString()
            val session2 = UUID.randomUUID().toString()
            presenceTracker.subscribed(session1, ladderId, creatorId)
            presenceTracker.subscribed(session2, ladderId, userId2)
            Thread.sleep(50)
            capturedMessages.clear()

            restTemplate.exchange(
                "/api/ladders/$ladderId/start", HttpMethod.POST, entityWithUser(null, creatorId), Map::class.java
            )
            capturedMessages.clear() // discard started + round-started

            val activityAId = getCurrentMatchAId(ladderId)
            val activityBId = getCurrentMatchBId(ladderId)

            // Creator votes for A
            restTemplate.exchange(
                "/api/ladders/$ladderId/vote",
                HttpMethod.POST,
                entityWithUser(mapOf("votedForActivityId" to activityAId.toString()), creatorId),
                Map::class.java,
            )
            capturedMessages.clear() // discard VoteRecorded event (not the last vote)

            // userId2 votes for B — last vote, causes tie 1-1
            restTemplate.exchange(
                "/api/ladders/$ladderId/vote",
                HttpMethod.POST,
                entityWithUser(mapOf("votedForActivityId" to activityBId.toString()), userId2),
                Map::class.java,
            )

            val msg1 = pollMessage()
            val msg2 = pollMessage()
            val msg3 = pollMessage()

            assertThat(msg1).isNotNull
            assertThat(msg1!!.body.action).isEqualTo("vote-cast")

            assertThat(msg2).isNotNull
            assertThat(msg2!!.body.action).isEqualTo("round-resolved")
            @Suppress("UNCHECKED_CAST")
            val resolvedPayload = msg2.body.payload as Map<String, Any?>
            assertThat(resolvedPayload["outcome"]).isEqualTo("tie")
            assertThat(resolvedPayload["winnerActivityId"]).isNull()

            assertThat(msg3).isNotNull
            assertThat(msg3!!.body.action).isEqualTo("round-started")
        }
    }

    @Nested
    inner class LadderCompletedEvent {

        @Test
        fun `last vote of final round publishes vote-cast, round-resolved, completed`() {
            val ladderId = createLadder()
            startLadder(ladderId) // 1 participant (creatorId), clears captured messages

            // Round 1: vote to decide (not final yet with 2 activities — next is the Grand Final)
            val round1ActivityAId = getCurrentMatchAId(ladderId)
            restTemplate.exchange(
                "/api/ladders/$ladderId/vote",
                HttpMethod.POST,
                entityWithUser(mapOf("votedForActivityId" to round1ActivityAId.toString()), creatorId),
                Map::class.java,
            )
            capturedMessages.clear() // discard vote-cast + round-resolved + round-started for round 1

            // Round 2 (Grand Final): vote to complete the ladder
            val round2ActivityAId = getCurrentMatchAId(ladderId)
            restTemplate.exchange(
                "/api/ladders/$ladderId/vote",
                HttpMethod.POST,
                entityWithUser(mapOf("votedForActivityId" to round2ActivityAId.toString()), creatorId),
                Map::class.java,
            )

            val msg1 = pollMessage()
            val msg2 = pollMessage()
            val msg3 = pollMessage()

            assertThat(msg1).isNotNull
            assertThat(msg1!!.body.action).isEqualTo("vote-cast")

            assertThat(msg2).isNotNull
            assertThat(msg2!!.body.action).isEqualTo("round-resolved")
            @Suppress("UNCHECKED_CAST")
            val resolvedPayload = msg2.body.payload as Map<String, Any?>
            assertThat(resolvedPayload["outcome"]).isEqualTo("decided")
            assertThat(resolvedPayload["winnerActivityId"]).isNotNull()

            assertThat(msg3).isNotNull
            assertThat(msg3!!.body.action).isEqualTo("completed")
            @Suppress("UNCHECKED_CAST")
            val completedPayload = msg3.body.payload as Map<String, Any?>
            assertThat(completedPayload["winnerActivityId"]).isNotNull()
        }
    }

    @Nested
    inner class RestartedEvent {

        @Test
        fun `POST restart publishes restarted message`() {
            val ladderId = createLadder()
            startLadder(ladderId) // clears captured messages
            capturedMessages.clear() // also clear started + round-started

            restTemplate.exchange(
                "/api/ladders/$ladderId/restart", HttpMethod.POST, entityWithUser(null, creatorId), Map::class.java
            )

            val captured = pollMessage()
            assertThat(captured).isNotNull
            assertThat(captured!!.destination).isEqualTo("/topic/ladders/$ladderId")
            assertThat(captured.body.action).isEqualTo("restarted")
        }

        @Test
        fun `no restarted message published when restart fails due to wrong state`() {
            val ladderId = createLadder()
            // Ladder is DRAFT — restart should fail with 409
            capturedMessages.clear()

            val response = restTemplate.exchange(
                "/api/ladders/$ladderId/restart", HttpMethod.POST, entityWithUser(null, creatorId), Map::class.java
            )
            assertThat(response.statusCode.value()).isEqualTo(409)

            val captured = capturedMessages.poll(2, TimeUnit.SECONDS)
            assertThat(captured).isNull()
        }
    }

    @Nested
    inner class PresenceChangedEvent {

        @Test
        fun `subscribed publishes presence-changed message with user in set`() {
            val ladderId = createLadder()
            capturedMessages.clear()

            val sessionId = UUID.randomUUID().toString()
            presenceTracker.subscribed(sessionId, ladderId, creatorId)

            val captured = pollMessage()
            assertThat(captured).isNotNull
            assertThat(captured!!.destination).isEqualTo("/topic/ladders/$ladderId")
            assertThat(captured.body.action).isEqualTo("presence-changed")
            @Suppress("UNCHECKED_CAST")
            val payload = captured.body.payload as Map<String, Any?>
            @Suppress("UNCHECKED_CAST")
            val presentUserIds = payload["presentUserIds"] as List<String>
            assertThat(presentUserIds).contains(creatorId.toString())
        }

        @Test
        fun `unsubscribed publishes presence-changed message with empty set`() {
            val ladderId = createLadder()
            val sessionId = UUID.randomUUID().toString()
            presenceTracker.subscribed(sessionId, ladderId, creatorId)
            capturedMessages.clear()

            presenceTracker.unsubscribed(sessionId)

            val captured = pollMessage()
            assertThat(captured).isNotNull
            assertThat(captured!!.body.action).isEqualTo("presence-changed")
            @Suppress("UNCHECKED_CAST")
            val payload = captured.body.payload as Map<String, Any?>
            @Suppress("UNCHECKED_CAST")
            val presentUserIds = payload["presentUserIds"] as List<String>
            assertThat(presentUserIds).isEmpty()
        }
    }
}
