package com.acme.services.camperservice.features.activityladder.acceptance

import com.acme.services.camperservice.config.TestContainerConfig
import com.acme.services.camperservice.features.activityladder.acceptance.fixture.ActivityLadderFixture
import com.acme.services.camperservice.websocket.LadderPresenceTracker
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
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestContainerConfig::class)
class ActivityLadderAcceptanceTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    lateinit var presenceTracker: LadderPresenceTracker

    private lateinit var fixture: ActivityLadderFixture
    private lateinit var creatorId: UUID
    private lateinit var otherUserId: UUID

    @BeforeEach
    fun setUp() {
        fixture = ActivityLadderFixture(jdbcTemplate)
        fixture.truncateAll()
        creatorId = fixture.insertUser(email = "creator@example.com", username = "creator")
        otherUserId = fixture.insertUser(email = "other@example.com", username = "other")
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private fun entityWithUser(body: Any?, userId: UUID): HttpEntity<Any?> {
        val headers = HttpHeaders()
        headers.set("X-User-Id", userId.toString())
        headers.set("Content-Type", "application/json")
        return HttpEntity(body, headers)
    }

    private fun defaultActivities() = listOf(
        mapOf("name" to "Hiking", "imageUrl" to "https://example.com/hiking.jpg", "distanceMinutes" to 60, "costPerPerson" to 10),
        mapOf("name" to "Kayaking", "imageUrl" to "https://example.com/kayaking.jpg", "distanceMinutes" to 90, "costPerPerson" to 25),
    )

    private fun createLadder(title: String = "Weekend Activities", userId: UUID = creatorId): UUID {
        val body = mapOf("title" to title, "activities" to defaultActivities())
        val response = restTemplate.exchange(
            "/api/ladders",
            HttpMethod.POST,
            entityWithUser(body, userId),
            Map::class.java,
        )
        assertThat(response.statusCode.value()).isEqualTo(201)
        @Suppress("UNCHECKED_CAST")
        return UUID.fromString((response.body as Map<String, Any?>)["id"] as String)
    }

    /** Simulates presence for userId and starts the ladder. Returns the ladderId. */
    private fun startLadder(ladderId: UUID, userId: UUID = creatorId): UUID {
        val sessionId = UUID.randomUUID().toString()
        presenceTracker.subscribed(sessionId, ladderId, userId)
        val response = restTemplate.exchange(
            "/api/ladders/$ladderId/start",
            HttpMethod.POST,
            entityWithUser(null, userId),
            Map::class.java,
        )
        assertThat(response.statusCode.value()).isEqualTo(200)
        return ladderId
    }

    @Suppress("UNCHECKED_CAST")
    private fun getDetailMap(ladderId: UUID, userId: UUID = creatorId): Map<String, Any?> {
        val response = restTemplate.exchange(
            "/api/ladders/$ladderId",
            HttpMethod.GET,
            entityWithUser(null, userId),
            Map::class.java,
        )
        return response.body as Map<String, Any?>
    }

    @Suppress("UNCHECKED_CAST")
    private fun getActivityIds(ladderId: UUID, userId: UUID = creatorId): List<UUID> {
        val detail = getDetailMap(ladderId, userId)
        val activities = detail["activities"] as List<Map<String, Any?>>
        return activities.map { UUID.fromString(it["id"] as String) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun getCurrentMatchAId(ladderId: UUID, userId: UUID = creatorId): UUID {
        val detail = getDetailMap(ladderId, userId)
        val currentRound = detail["currentRound"] as Map<String, Any?>
        return UUID.fromString(currentRound["activityAId"] as String)
    }

    @Suppress("UNCHECKED_CAST")
    private fun getCurrentMatchBId(ladderId: UUID, userId: UUID = creatorId): UUID {
        val detail = getDetailMap(ladderId, userId)
        val currentRound = detail["currentRound"] as Map<String, Any?>
        return UUID.fromString(currentRound["activityBId"] as String)
    }

    /**
     * Drives the ladder to completion by always voting for slot A.
     * Returns the final status string.
     */
    @Suppress("UNCHECKED_CAST")
    private fun driveToCompletion(ladderId: UUID, voterId: UUID): String {
        repeat(20) {
            val detail = getDetailMap(ladderId, voterId)
            val status = detail["status"] as String
            if (status != "ACTIVE") return status

            val currentRound = detail["currentRound"] as? Map<String, Any?> ?: return status
            val activityAId = currentRound["activityAId"] as String

            restTemplate.exchange(
                "/api/ladders/$ladderId/vote",
                HttpMethod.POST,
                entityWithUser(mapOf("votedForActivityId" to activityAId), voterId),
                Map::class.java,
            )
        }
        return getDetailMap(ladderId, voterId)["status"] as String
    }

    // ── Test groups ─────────────────────────────────────────────────────────────

    @Nested
    inner class CreateLadder {

        @Test
        fun `POST creates ladder and returns 201 with detail`() {
            val body = mapOf("title" to "Summer Trip", "activities" to defaultActivities())
            val response = restTemplate.exchange(
                "/api/ladders",
                HttpMethod.POST,
                entityWithUser(body, creatorId),
                Map::class.java,
            )

            assertThat(response.statusCode.value()).isEqualTo(201)
            @Suppress("UNCHECKED_CAST")
            val detail = response.body as Map<String, Any?>
            assertThat(detail["title"]).isEqualTo("Summer Trip")
            assertThat(detail["status"]).isEqualTo("DRAFT")
            assertThat(detail["creatorId"]).isEqualTo(creatorId.toString())
            @Suppress("UNCHECKED_CAST")
            assertThat(detail["activities"] as List<*>).hasSize(2)
            assertThat(detail["id"]).isNotNull()
        }

        @Test
        fun `POST returns 400 when title is blank`() {
            val body = mapOf("title" to "", "activities" to defaultActivities())
            val response = restTemplate.exchange(
                "/api/ladders",
                HttpMethod.POST,
                entityWithUser(body, creatorId),
                Map::class.java,
            )

            assertThat(response.statusCode.value()).isEqualTo(400)
        }

        @Test
        fun `POST returns 400 when fewer than 2 activities provided`() {
            val singleActivity = listOf(
                mapOf("name" to "Hiking", "imageUrl" to "https://example.com/hiking.jpg", "distanceMinutes" to 60, "costPerPerson" to 10),
            )
            val body = mapOf("title" to "Only One Activity", "activities" to singleActivity)
            val response = restTemplate.exchange(
                "/api/ladders",
                HttpMethod.POST,
                entityWithUser(body, creatorId),
                Map::class.java,
            )

            assertThat(response.statusCode.value()).isEqualTo(400)
        }

        @Test
        fun `POST returns 400 when activity name is blank`() {
            val badActivities = listOf(
                mapOf("name" to "", "imageUrl" to "https://example.com/hiking.jpg", "distanceMinutes" to 60, "costPerPerson" to 10),
                mapOf("name" to "Kayaking", "imageUrl" to "https://example.com/kayaking.jpg", "distanceMinutes" to 90, "costPerPerson" to 25),
            )
            val body = mapOf("title" to "Test", "activities" to badActivities)
            val response = restTemplate.exchange(
                "/api/ladders",
                HttpMethod.POST,
                entityWithUser(body, creatorId),
                Map::class.java,
            )

            assertThat(response.statusCode.value()).isEqualTo(400)
        }
    }

    @Nested
    inner class ListLadders {

        @Test
        fun `GET returns 200 with list of ladders created by user`() {
            createLadder("Trip Alpha")
            createLadder("Trip Beta")

            val response = restTemplate.exchange(
                "/api/ladders",
                HttpMethod.GET,
                entityWithUser(null, creatorId),
                List::class.java,
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            @Suppress("UNCHECKED_CAST")
            val summaries = response.body as List<Map<String, Any?>>
            assertThat(summaries).hasSize(2)
            val titles = summaries.map { it["title"] as String }
            assertThat(titles).containsExactlyInAnyOrder("Trip Alpha", "Trip Beta")
        }

        @Test
        fun `GET returns 200 with empty list when user has no ladders`() {
            val response = restTemplate.exchange(
                "/api/ladders",
                HttpMethod.GET,
                entityWithUser(null, otherUserId),
                List::class.java,
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body as List<*>).isEmpty()
        }
    }

    @Nested
    inner class GetLadderDetail {

        @Test
        fun `GET returns 200 with ladder detail`() {
            val ladderId = createLadder("Alpine Challenge")

            val response = restTemplate.exchange(
                "/api/ladders/$ladderId",
                HttpMethod.GET,
                entityWithUser(null, creatorId),
                Map::class.java,
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            @Suppress("UNCHECKED_CAST")
            val detail = response.body as Map<String, Any?>
            assertThat(detail["id"]).isEqualTo(ladderId.toString())
            assertThat(detail["title"]).isEqualTo("Alpine Challenge")
            assertThat(detail["status"]).isEqualTo("DRAFT")
            assertThat(detail["currentRound"]).isNull()
        }

        @Test
        fun `GET returns 404 for unknown ladder`() {
            val response = restTemplate.exchange(
                "/api/ladders/${UUID.randomUUID()}",
                HttpMethod.GET,
                entityWithUser(null, creatorId),
                Map::class.java,
            )

            assertThat(response.statusCode.value()).isEqualTo(404)
        }
    }

    @Nested
    inner class AddActivity {

        @Test
        fun `POST adds activity and returns 201`() {
            val ladderId = createLadder()
            val body = mapOf(
                "name" to "Rock Climbing",
                "imageUrl" to "https://example.com/climbing.jpg",
                "distanceMinutes" to 45,
                "costPerPerson" to 30,
            )

            val response = restTemplate.exchange(
                "/api/ladders/$ladderId/activities",
                HttpMethod.POST,
                entityWithUser(body, creatorId),
                Map::class.java,
            )

            assertThat(response.statusCode.value()).isEqualTo(201)
            @Suppress("UNCHECKED_CAST")
            val activity = response.body as Map<String, Any?>
            assertThat(activity["name"]).isEqualTo("Rock Climbing")
            assertThat(activity["bracket"]).isEqualTo("WINNERS")

            // Read-your-own-writes: detail now shows 3 activities
            val activityIds = getActivityIds(ladderId)
            assertThat(activityIds).hasSize(3)
        }

        @Test
        fun `POST returns 403 when called by non-creator`() {
            val ladderId = createLadder()
            val body = mapOf(
                "name" to "Swimming",
                "imageUrl" to "https://example.com/swim.jpg",
                "distanceMinutes" to 30,
                "costPerPerson" to 5,
            )

            val response = restTemplate.exchange(
                "/api/ladders/$ladderId/activities",
                HttpMethod.POST,
                entityWithUser(body, otherUserId),
                Map::class.java,
            )

            assertThat(response.statusCode.value()).isEqualTo(403)
        }

        @Test
        fun `POST returns 409 when ladder is ACTIVE`() {
            val ladderId = createLadder()
            startLadder(ladderId)

            val body = mapOf(
                "name" to "Swimming",
                "imageUrl" to "https://example.com/swim.jpg",
                "distanceMinutes" to 30,
                "costPerPerson" to 5,
            )

            val response = restTemplate.exchange(
                "/api/ladders/$ladderId/activities",
                HttpMethod.POST,
                entityWithUser(body, creatorId),
                Map::class.java,
            )

            assertThat(response.statusCode.value()).isEqualTo(409)
        }
    }

    @Nested
    inner class RemoveActivity {

        @Test
        fun `DELETE removes activity and returns 204`() {
            val ladderId = createLadder()
            val activityIds = getActivityIds(ladderId)
            val activityToRemove = activityIds.first()

            val response = restTemplate.exchange(
                "/api/ladders/$ladderId/activities/$activityToRemove",
                HttpMethod.DELETE,
                entityWithUser(null, creatorId),
                Void::class.java,
            )

            assertThat(response.statusCode.value()).isEqualTo(204)

            // Read-your-own-writes: detail now shows 1 activity
            val remainingIds = getActivityIds(ladderId)
            assertThat(remainingIds).hasSize(1)
            assertThat(remainingIds).doesNotContain(activityToRemove)
        }

        @Test
        fun `DELETE returns 403 when called by non-creator`() {
            val ladderId = createLadder()
            val activityId = getActivityIds(ladderId).first()

            val response = restTemplate.exchange(
                "/api/ladders/$ladderId/activities/$activityId",
                HttpMethod.DELETE,
                entityWithUser(null, otherUserId),
                Void::class.java,
            )

            assertThat(response.statusCode.value()).isEqualTo(403)
        }
    }

    @Nested
    inner class StartLadder {

        @Test
        fun `POST start returns 200 with ACTIVE ladder`() {
            val ladderId = createLadder()
            val sessionId = UUID.randomUUID().toString()
            presenceTracker.subscribed(sessionId, ladderId, creatorId)

            val response = restTemplate.exchange(
                "/api/ladders/$ladderId/start",
                HttpMethod.POST,
                entityWithUser(null, creatorId),
                Map::class.java,
            )

            assertThat(response.statusCode.value()).isEqualTo(200)
            @Suppress("UNCHECKED_CAST")
            val detail = response.body as Map<String, Any?>
            assertThat(detail["status"]).isEqualTo("ACTIVE")

            // Read-your-own-writes: GET detail shows active round
            val fetchedDetail = getDetailMap(ladderId)
            assertThat(fetchedDetail["status"]).isEqualTo("ACTIVE")
            assertThat(fetchedDetail["currentRound"]).isNotNull()
        }

        @Test
        fun `POST start returns 403 when called by non-creator`() {
            val ladderId = createLadder()
            val sessionId = UUID.randomUUID().toString()
            presenceTracker.subscribed(sessionId, ladderId, otherUserId)

            val response = restTemplate.exchange(
                "/api/ladders/$ladderId/start",
                HttpMethod.POST,
                entityWithUser(null, otherUserId),
                Map::class.java,
            )

            assertThat(response.statusCode.value()).isEqualTo(403)
        }

        @Test
        fun `POST start returns 409 when no users are present`() {
            val ladderId = createLadder()
            // No presence tracked — no presenceTracker.subscribed call

            val response = restTemplate.exchange(
                "/api/ladders/$ladderId/start",
                HttpMethod.POST,
                entityWithUser(null, creatorId),
                Map::class.java,
            )

            assertThat(response.statusCode.value()).isEqualTo(409)
        }

        @Test
        fun `POST start returns 409 when ladder is already ACTIVE`() {
            val ladderId = createLadder()
            startLadder(ladderId)

            // Try to start again — no new presence registered (same session would also work)
            val sessionId2 = UUID.randomUUID().toString()
            presenceTracker.subscribed(sessionId2, ladderId, creatorId)

            val response = restTemplate.exchange(
                "/api/ladders/$ladderId/start",
                HttpMethod.POST,
                entityWithUser(null, creatorId),
                Map::class.java,
            )

            assertThat(response.statusCode.value()).isEqualTo(409)
        }
    }

    @Nested
    inner class CastVote {

        @Test
        fun `POST vote returns 200 with vote recorded when not last voter`() {
            val ladderId = createLadder()
            // Two participants in presence so a single vote won't resolve the round
            val session1 = UUID.randomUUID().toString()
            val session2 = UUID.randomUUID().toString()
            presenceTracker.subscribed(session1, ladderId, creatorId)
            presenceTracker.subscribed(session2, ladderId, otherUserId)

            val startResponse = restTemplate.exchange(
                "/api/ladders/$ladderId/start",
                HttpMethod.POST,
                entityWithUser(null, creatorId),
                Map::class.java,
            )
            assertThat(startResponse.statusCode.value()).isEqualTo(200)

            val activityAId = getCurrentMatchAId(ladderId)

            val voteResponse = restTemplate.exchange(
                "/api/ladders/$ladderId/vote",
                HttpMethod.POST,
                entityWithUser(mapOf("votedForActivityId" to activityAId.toString()), creatorId),
                Map::class.java,
            )

            assertThat(voteResponse.statusCode.value()).isEqualTo(200)
            @Suppress("UNCHECKED_CAST")
            val body = voteResponse.body as Map<String, Any?>
            assertThat(body["voteCount"]).isEqualTo(1)
            assertThat(body["votersRemaining"]).isEqualTo(1)
        }

        @Test
        fun `POST vote returns 403 when user is not a participant`() {
            val ladderId = createLadder()
            startLadder(ladderId)

            val activityAId = getCurrentMatchAId(ladderId)

            // otherUserId was NOT present at start — not a participant
            val response = restTemplate.exchange(
                "/api/ladders/$ladderId/vote",
                HttpMethod.POST,
                entityWithUser(mapOf("votedForActivityId" to activityAId.toString()), otherUserId),
                Map::class.java,
            )

            assertThat(response.statusCode.value()).isEqualTo(403)
        }

        @Test
        fun `POST vote returns 409 when user has already voted in this round`() {
            val ladderId = createLadder()
            // Three participants so voting twice doesn't resolve the round
            val thirdUserId = fixture.insertUser(email = "third@example.com")
            val session1 = UUID.randomUUID().toString()
            val session2 = UUID.randomUUID().toString()
            val session3 = UUID.randomUUID().toString()
            presenceTracker.subscribed(session1, ladderId, creatorId)
            presenceTracker.subscribed(session2, ladderId, otherUserId)
            presenceTracker.subscribed(session3, ladderId, thirdUserId)

            restTemplate.exchange(
                "/api/ladders/$ladderId/start",
                HttpMethod.POST,
                entityWithUser(null, creatorId),
                Map::class.java,
            )

            val activityAId = getCurrentMatchAId(ladderId)
            // First vote — succeeds
            restTemplate.exchange(
                "/api/ladders/$ladderId/vote",
                HttpMethod.POST,
                entityWithUser(mapOf("votedForActivityId" to activityAId.toString()), creatorId),
                Map::class.java,
            )

            // Duplicate vote — should fail
            val response = restTemplate.exchange(
                "/api/ladders/$ladderId/vote",
                HttpMethod.POST,
                entityWithUser(mapOf("votedForActivityId" to activityAId.toString()), creatorId),
                Map::class.java,
            )

            assertThat(response.statusCode.value()).isEqualTo(409)
        }

        @Test
        fun `round does not advance when not all present voters have voted`() {
            val ladderId = createLadder()
            val thirdUserId = fixture.insertUser(email = "third@example.com")
            val session1 = UUID.randomUUID().toString()
            val session2 = UUID.randomUUID().toString()
            val session3 = UUID.randomUUID().toString()
            presenceTracker.subscribed(session1, ladderId, creatorId)
            presenceTracker.subscribed(session2, ladderId, otherUserId)
            presenceTracker.subscribed(session3, ladderId, thirdUserId)

            restTemplate.exchange(
                "/api/ladders/$ladderId/start",
                HttpMethod.POST,
                entityWithUser(null, creatorId),
                Map::class.java,
            )

            val activityAId = getCurrentMatchAId(ladderId).toString()
            val activityBId = getCurrentMatchBId(ladderId).toString()

            // vote 1 of 3 — round must not advance
            restTemplate.exchange(
                "/api/ladders/$ladderId/vote",
                HttpMethod.POST,
                entityWithUser(mapOf("votedForActivityId" to activityAId), creatorId),
                Map::class.java,
            )

            @Suppress("UNCHECKED_CAST")
            val afterVote1 = getDetailMap(ladderId)
            val round1 = afterVote1["currentRound"] as Map<String, Any?>
            assertThat(afterVote1["status"]).isEqualTo("ACTIVE")
            assertThat(round1["roundNumber"]).isEqualTo(1)
            assertThat(round1["currentMatchActivityAId"] ?: round1["activityAId"]).isEqualTo(activityAId)
            assertThat(round1["currentMatchActivityBId"] ?: round1["activityBId"]).isEqualTo(activityBId)
            @Suppress("UNCHECKED_CAST")
            assertThat(round1["votedUserIds"] as List<*>).containsExactly(creatorId.toString())

            // vote 2 of 3 — round must still not advance
            restTemplate.exchange(
                "/api/ladders/$ladderId/vote",
                HttpMethod.POST,
                entityWithUser(mapOf("votedForActivityId" to activityAId), otherUserId),
                Map::class.java,
            )

            @Suppress("UNCHECKED_CAST")
            val afterVote2 = getDetailMap(ladderId)
            val round2 = afterVote2["currentRound"] as Map<String, Any?>
            assertThat(afterVote2["status"]).isEqualTo("ACTIVE")
            assertThat(round2["roundNumber"]).isEqualTo(1)
            @Suppress("UNCHECKED_CAST")
            assertThat(round2["votedUserIds"] as List<*>).containsExactlyInAnyOrder(
                creatorId.toString(), otherUserId.toString()
            )

            // vote 3 of 3 (thirdUser decides 2-1 for A) — round must now advance
            restTemplate.exchange(
                "/api/ladders/$ladderId/vote",
                HttpMethod.POST,
                entityWithUser(mapOf("votedForActivityId" to activityBId), thirdUserId),
                Map::class.java,
            )

            val afterVote3 = getDetailMap(ladderId)
            val finalStatus = afterVote3["status"] as String
            val finalRoundNumber = (afterVote3["currentRound"] as? Map<*, *>)?.get("roundNumber") as? Int
            // Either the ladder completed (COMPLETED) or moved to round 2+
            assertThat(finalStatus == "COMPLETED" || (finalRoundNumber != null && finalRoundNumber > 1)).isTrue()
        }
    }

    @Nested
    inner class RestartLadder {

        @Test
        fun `POST restart from ACTIVE returns 200 and resets to DRAFT`() {
            val ladderId = createLadder()
            startLadder(ladderId)

            val response = restTemplate.exchange(
                "/api/ladders/$ladderId/restart",
                HttpMethod.POST,
                entityWithUser(null, creatorId),
                Map::class.java,
            )

            assertThat(response.statusCode.value()).isEqualTo(200)
            @Suppress("UNCHECKED_CAST")
            val detail = response.body as Map<String, Any?>
            assertThat(detail["status"]).isEqualTo("DRAFT")

            // Read-your-own-writes: ladder back to DRAFT, no current round
            val fetchedDetail = getDetailMap(ladderId)
            assertThat(fetchedDetail["status"]).isEqualTo("DRAFT")
        }

        @Test
        fun `POST restart from COMPLETED returns 200 and resets to DRAFT`() {
            val ladderId = createLadder()
            startLadder(ladderId)

            val finalStatus = driveToCompletion(ladderId, creatorId)
            assertThat(finalStatus).isEqualTo("COMPLETED")

            val response = restTemplate.exchange(
                "/api/ladders/$ladderId/restart",
                HttpMethod.POST,
                entityWithUser(null, creatorId),
                Map::class.java,
            )

            assertThat(response.statusCode.value()).isEqualTo(200)
            @Suppress("UNCHECKED_CAST")
            val detail = response.body as Map<String, Any?>
            assertThat(detail["status"]).isEqualTo("DRAFT")
        }

        @Test
        fun `POST restart returns 403 when called by non-creator`() {
            val ladderId = createLadder()
            startLadder(ladderId)

            val response = restTemplate.exchange(
                "/api/ladders/$ladderId/restart",
                HttpMethod.POST,
                entityWithUser(null, otherUserId),
                Map::class.java,
            )

            assertThat(response.statusCode.value()).isEqualTo(403)
        }

        @Test
        fun `POST restart returns 409 when ladder is in DRAFT`() {
            val ladderId = createLadder()
            // No start — ladder stays in DRAFT

            val response = restTemplate.exchange(
                "/api/ladders/$ladderId/restart",
                HttpMethod.POST,
                entityWithUser(null, creatorId),
                Map::class.java,
            )

            assertThat(response.statusCode.value()).isEqualTo(409)
        }
    }

    @Nested
    inner class EndToEnd {

        @Test
        fun `full ladder workflow from creation through voting to completion`() {
            // Create with 2 activities
            val ladderId = createLadder("Weekend Showdown")
            val activitiesBeforeStart = getActivityIds(ladderId)
            assertThat(activitiesBeforeStart).hasSize(2)

            // Start with creator in presence
            val sessionId = UUID.randomUUID().toString()
            presenceTracker.subscribed(sessionId, ladderId, creatorId)
            val startResponse = restTemplate.exchange(
                "/api/ladders/$ladderId/start",
                HttpMethod.POST,
                entityWithUser(null, creatorId),
                Map::class.java,
            )
            assertThat(startResponse.statusCode.value()).isEqualTo(200)

            // GET detail shows ACTIVE with a current round
            val activeDetail = getDetailMap(ladderId)
            assertThat(activeDetail["status"]).isEqualTo("ACTIVE")
            @Suppress("UNCHECKED_CAST")
            val currentRound = activeDetail["currentRound"] as Map<String, Any?>
            assertThat(currentRound["roundNumber"]).isEqualTo(1)
            assertThat(currentRound["totalVoters"]).isEqualTo(1)
            assertThat(currentRound["votesCast"]).isEqualTo(0)

            // Drive to completion by always voting for slot A
            val finalStatus = driveToCompletion(ladderId, creatorId)
            assertThat(finalStatus).isEqualTo("COMPLETED")

            // GET detail shows COMPLETED with a winner
            val completedDetail = getDetailMap(ladderId)
            assertThat(completedDetail["status"]).isEqualTo("COMPLETED")
            assertThat(completedDetail["winnerActivityId"]).isNotNull()
            assertThat(completedDetail["currentRound"]).isNull()
        }

        @Test
        fun `late-joiner blocked from voting after start`() {
            // Start ladder with only creator in presence
            val ladderId = createLadder()
            startLadder(ladderId)

            // otherUserId was NOT present at start — not a participant, cannot vote
            val activityAId = getCurrentMatchAId(ladderId)
            val response = restTemplate.exchange(
                "/api/ladders/$ladderId/vote",
                HttpMethod.POST,
                entityWithUser(mapOf("votedForActivityId" to activityAId.toString()), otherUserId),
                Map::class.java,
            )

            assertThat(response.statusCode.value()).isEqualTo(403)
        }

        @Test
        fun `creator can restart completed ladder and run another round`() {
            // Complete the ladder
            val ladderId = createLadder()
            startLadder(ladderId)
            driveToCompletion(ladderId, creatorId)
            assertThat(getDetailMap(ladderId)["status"]).isEqualTo("COMPLETED")

            // Restart back to DRAFT
            val restartResponse = restTemplate.exchange(
                "/api/ladders/$ladderId/restart",
                HttpMethod.POST,
                entityWithUser(null, creatorId),
                Map::class.java,
            )
            assertThat(restartResponse.statusCode.value()).isEqualTo(200)

            // Start again — fresh session required
            val sessionId2 = UUID.randomUUID().toString()
            presenceTracker.subscribed(sessionId2, ladderId, creatorId)
            val startResponse2 = restTemplate.exchange(
                "/api/ladders/$ladderId/start",
                HttpMethod.POST,
                entityWithUser(null, creatorId),
                Map::class.java,
            )
            assertThat(startResponse2.statusCode.value()).isEqualTo(200)

            @Suppress("UNCHECKED_CAST")
            val detail2 = startResponse2.body as Map<String, Any?>
            assertThat(detail2["status"]).isEqualTo("ACTIVE")
        }
    }
}
