package com.acme.clients.activityladderclient

import com.acme.clients.activityladderclient.api.AddLadderActivityParam
import com.acme.clients.activityladderclient.api.BulkInsertParticipantsParam
import com.acme.clients.activityladderclient.api.CastVoteParam
import com.acme.clients.activityladderclient.api.CountVotesForRoundParam
import com.acme.clients.activityladderclient.api.CreateLadderParam
import com.acme.clients.activityladderclient.api.GetLadderActivitiesParam
import com.acme.clients.activityladderclient.api.GetLadderByIdParam
import com.acme.clients.activityladderclient.api.GetLadderListParam
import com.acme.clients.activityladderclient.api.GetParticipantsParam
import com.acme.clients.activityladderclient.api.GetVotesForRoundParam
import com.acme.clients.activityladderclient.api.RemoveLadderActivityParam
import com.acme.clients.activityladderclient.api.RestartLadderParam
import com.acme.clients.activityladderclient.api.SetLadderWinnerParam
import com.acme.clients.activityladderclient.api.UpdateActivityLossAndBracketParam
import com.acme.clients.activityladderclient.api.UpdateLadderStateParam
import com.acme.clients.activityladderclient.model.LadderBracket
import com.acme.clients.activityladderclient.model.LadderStatus
import com.acme.clients.activityladderclient.test.ActivityLadderTestDb
import com.acme.clients.common.Result
import com.acme.clients.common.error.ConflictError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.error.ValidationError
import com.acme.clients.common.success
import org.assertj.core.api.Assertions.assertThat
import org.jdbi.v3.core.Jdbi
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
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CyclicBarrier

@Testcontainers
class JdbiActivityLadderClientTest {

    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("camper_db")
            .withUsername("postgres")
            .withPassword("postgres")

        private lateinit var client: com.acme.clients.activityladderclient.api.ActivityLadderClient
        private lateinit var jdbi: Jdbi

        @BeforeAll
        @JvmStatic
        fun setup() {
            ActivityLadderTestDb.cleanAndMigrate(postgres.jdbcUrl, postgres.username, postgres.password)

            System.setProperty("DB_URL", postgres.jdbcUrl)
            System.setProperty("DB_USER", postgres.username)
            System.setProperty("DB_PASSWORD", postgres.password)
            client = createActivityLadderClient()
            jdbi = Jdbi.create(postgres.jdbcUrl, postgres.username, postgres.password)
        }
    }

    @BeforeEach
    fun truncate() {
        jdbi.withHandle<Unit, Exception> { handle ->
            handle.createUpdate(
                "TRUNCATE TABLE ladder_votes, ladder_participants, ladder_activities, activity_ladders, users CASCADE"
            ).execute()
        }
    }

    // ─── Seed helpers ────────────────────────────────────────────────────────

    private fun insertUser(id: UUID, email: String = "user@example.com") {
        jdbi.withHandle<Unit, Exception> { handle ->
            handle.createUpdate("INSERT INTO users (id, email) VALUES (:id, :email)")
                .bind("id", id)
                .bind("email", email)
                .execute()
        }
    }

    private fun insertLadder(
        id: UUID,
        creatorId: UUID,
        title: String = "Test Ladder",
        status: String = "DRAFT",
        currentRoundNumber: Int? = null,
        currentMatchActivityAId: UUID? = null,
        currentMatchActivityBId: UUID? = null,
        isFinalRound: Boolean = false,
        isGrandFinalReset: Boolean = false,
        winnerActivityId: UUID? = null,
    ) {
        val now = Instant.now()
        jdbi.withHandle<Unit, Exception> { handle ->
            handle.createUpdate(
                """
                INSERT INTO activity_ladders (
                    id, creator_id, title, status, current_round_number,
                    current_match_activity_a_id, current_match_activity_b_id,
                    is_final_round, is_grand_final_reset, winner_activity_id,
                    created_at, updated_at
                ) VALUES (
                    :id, :creatorId, :title, :status, :currentRoundNumber,
                    CAST(:currentMatchActivityAId AS uuid), CAST(:currentMatchActivityBId AS uuid),
                    :isFinalRound, :isGrandFinalReset, CAST(:winnerActivityId AS uuid),
                    :createdAt, :updatedAt
                )
                """.trimIndent()
            )
                .bind("id", id)
                .bind("creatorId", creatorId)
                .bind("title", title)
                .bind("status", status)
                .bind("currentRoundNumber", currentRoundNumber)
                .bind("currentMatchActivityAId", currentMatchActivityAId?.toString())
                .bind("currentMatchActivityBId", currentMatchActivityBId?.toString())
                .bind("isFinalRound", isFinalRound)
                .bind("isGrandFinalReset", isGrandFinalReset)
                .bind("winnerActivityId", winnerActivityId?.toString())
                .bind("createdAt", now)
                .bind("updatedAt", now)
                .execute()
        }
    }

    private fun insertActivity(
        id: UUID,
        ladderId: UUID,
        name: String = "Test Activity",
        imageUrl: String = "https://example.com/img.jpg",
        distanceMinutes: Int = 30,
        costPerPerson: BigDecimal = BigDecimal("10.00"),
        losses: Int = 0,
        bracket: String = "WINNERS",
        displayOrder: Int = 1,
    ) {
        val now = Instant.now()
        jdbi.withHandle<Unit, Exception> { handle ->
            handle.createUpdate(
                """
                INSERT INTO ladder_activities (
                    id, ladder_id, name, image_url, distance_minutes, cost_per_person,
                    losses, bracket, display_order, created_at
                ) VALUES (
                    :id, :ladderId, :name, :imageUrl, :distanceMinutes, :costPerPerson,
                    :losses, :bracket, :displayOrder, :createdAt
                )
                """.trimIndent()
            )
                .bind("id", id)
                .bind("ladderId", ladderId)
                .bind("name", name)
                .bind("imageUrl", imageUrl)
                .bind("distanceMinutes", distanceMinutes)
                .bind("costPerPerson", costPerPerson)
                .bind("losses", losses)
                .bind("bracket", bracket)
                .bind("displayOrder", displayOrder)
                .bind("createdAt", now)
                .execute()
        }
    }

    private fun insertParticipant(id: UUID, ladderId: UUID, userId: UUID) {
        val now = Instant.now()
        jdbi.withHandle<Unit, Exception> { handle ->
            handle.createUpdate(
                "INSERT INTO ladder_participants (id, ladder_id, user_id, created_at) VALUES (:id, :ladderId, :userId, :createdAt)"
            )
                .bind("id", id)
                .bind("ladderId", ladderId)
                .bind("userId", userId)
                .bind("createdAt", now)
                .execute()
        }
    }

    private fun insertVote(
        id: UUID,
        ladderId: UUID,
        roundNumber: Int,
        userId: UUID,
        votedForActivityId: UUID,
    ) {
        val now = Instant.now()
        jdbi.withHandle<Unit, Exception> { handle ->
            handle.createUpdate(
                """
                INSERT INTO ladder_votes (id, ladder_id, round_number, user_id, voted_for_activity_id, created_at)
                VALUES (:id, :ladderId, :roundNumber, :userId, :votedForActivityId, :createdAt)
                """.trimIndent()
            )
                .bind("id", id)
                .bind("ladderId", ladderId)
                .bind("roundNumber", roundNumber)
                .bind("userId", userId)
                .bind("votedForActivityId", votedForActivityId)
                .bind("createdAt", now)
                .execute()
        }
    }

    private fun countRowsWhere(table: String, column: String, id: UUID): Long =
        jdbi.withHandle<Long, Exception> { handle ->
            handle.createQuery("SELECT COUNT(*) FROM $table WHERE $column = :id")
                .bind("id", id)
                .mapTo(Long::class.java)
                .one()
        }

    // ─── createLadder ────────────────────────────────────────────────────────

    @Nested
    inner class CreateLadder {

        @Test
        fun `createLadder returns a DRAFT ladder with correct fields`() {
            val creatorId = UUID.randomUUID()
            insertUser(creatorId)

            val result = client.createLadder(CreateLadderParam(creatorId = creatorId, title = "Summer Activities"))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val ladder = (result as Result.Success).value
            assertThat(ladder.id).isNotNull()
            assertThat(ladder.creatorId).isEqualTo(creatorId)
            assertThat(ladder.title).isEqualTo("Summer Activities")
            assertThat(ladder.status).isEqualTo(LadderStatus.DRAFT)
            assertThat(ladder.currentRoundNumber).isNull()
            assertThat(ladder.currentMatchActivityAId).isNull()
            assertThat(ladder.currentMatchActivityBId).isNull()
            assertThat(ladder.isFinalRound).isFalse()
            assertThat(ladder.isGrandFinalReset).isFalse()
            assertThat(ladder.winnerActivityId).isNull()
            assertThat(ladder.createdAt).isInstanceOf(Instant::class.java)
            assertThat(ladder.updatedAt).isInstanceOf(Instant::class.java)
        }

        @Test
        fun `createLadder with 3 activities verifies row counts`() {
            val creatorId = UUID.randomUUID()
            insertUser(creatorId)

            val ladder = (client.createLadder(CreateLadderParam(creatorId = creatorId, title = "Activity Ladder")) as Result.Success).value

            client.addActivity(AddLadderActivityParam(ladder.id, "Hiking", "https://img.com/1.jpg", 60, BigDecimal("10.00")))
            client.addActivity(AddLadderActivityParam(ladder.id, "Kayaking", "https://img.com/2.jpg", 120, BigDecimal("25.00")))
            client.addActivity(AddLadderActivityParam(ladder.id, "Rock Climbing", "https://img.com/3.jpg", 180, BigDecimal("42.50")))

            assertThat(countRowsWhere("activity_ladders", "id", ladder.id)).isEqualTo(1L)
            assertThat(countRowsWhere("ladder_activities", "ladder_id", ladder.id)).isEqualTo(3L)
        }
    }

    // ─── getLadderById ───────────────────────────────────────────────────────

    @Nested
    inner class GetLadderById {

        @Test
        fun `getLadderById returns the created ladder`() {
            val creatorId = UUID.randomUUID()
            insertUser(creatorId)
            val created = (client.createLadder(CreateLadderParam(creatorId = creatorId, title = "Lookup Ladder")) as Result.Success).value

            val result = client.getLadderById(GetLadderByIdParam(id = created.id))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val ladder = (result as Result.Success).value
            assertThat(ladder.id).isEqualTo(created.id)
            assertThat(ladder.creatorId).isEqualTo(creatorId)
            assertThat(ladder.title).isEqualTo("Lookup Ladder")
            assertThat(ladder.status).isEqualTo(LadderStatus.DRAFT)
            assertThat(ladder.createdAt).isInstanceOf(Instant::class.java)
        }

        @Test
        fun `getLadderById returns NotFoundError for missing UUID`() {
            val result = client.getLadderById(GetLadderByIdParam(id = UUID.randomUUID()))

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).error).isInstanceOf(NotFoundError::class.java)
        }
    }

    // ─── getLadderList ───────────────────────────────────────────────────────

    @Nested
    inner class GetLadderList {

        @Test
        fun `getLadderList returns all ladders newest first`() {
            val creatorId = UUID.randomUUID()
            insertUser(creatorId)
            client.createLadder(CreateLadderParam(creatorId, "Ladder Alpha"))
            Thread.sleep(10)
            client.createLadder(CreateLadderParam(creatorId, "Ladder Beta"))
            Thread.sleep(10)
            client.createLadder(CreateLadderParam(creatorId, "Ladder Gamma"))

            val result = client.getLadderList(GetLadderListParam())

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val ladders = (result as Result.Success).value
            assertThat(ladders).hasSize(3)
            assertThat(ladders[0].title).isEqualTo("Ladder Gamma")
            assertThat(ladders[1].title).isEqualTo("Ladder Beta")
            assertThat(ladders[2].title).isEqualTo("Ladder Alpha")
        }

        @Test
        fun `getLadderList respects limit`() {
            val creatorId = UUID.randomUUID()
            insertUser(creatorId)
            repeat(5) { i ->
                insertLadder(UUID.randomUUID(), creatorId, title = "Ladder $i")
            }

            val result = client.getLadderList(GetLadderListParam(limit = 2))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            assertThat((result as Result.Success).value).hasSize(2)
        }

        @Test
        fun `getLadderList respects offset for pagination`() {
            val creatorId = UUID.randomUUID()
            insertUser(creatorId)
            repeat(4) { i ->
                insertLadder(UUID.randomUUID(), creatorId, title = "Ladder $i")
                Thread.sleep(5)
            }

            val page1 = (client.getLadderList(GetLadderListParam(limit = 2, offset = 0)) as Result.Success).value
            val page2 = (client.getLadderList(GetLadderListParam(limit = 2, offset = 2)) as Result.Success).value

            assertThat(page1).hasSize(2)
            assertThat(page2).hasSize(2)
            val page1Ids = page1.map { it.id }.toSet()
            val page2Ids = page2.map { it.id }.toSet()
            assertThat(page1Ids.intersect(page2Ids)).isEmpty()
        }

        @Test
        fun `getLadderList returns empty list when no ladders exist`() {
            val result = client.getLadderList(GetLadderListParam())

            assertThat(result).isInstanceOf(Result.Success::class.java)
            assertThat((result as Result.Success).value).isEmpty()
        }
    }

    // ─── addActivity ─────────────────────────────────────────────────────────

    @Nested
    inner class AddActivity {

        @Test
        fun `addActivity assigns displayOrder starting at 1 for empty ladder`() {
            val creatorId = UUID.randomUUID()
            insertUser(creatorId)
            val ladder = (client.createLadder(CreateLadderParam(creatorId, "Empty Ladder")) as Result.Success).value

            val result = client.addActivity(
                AddLadderActivityParam(ladder.id, "Canoeing", "https://img.com/canoe.jpg", 90, BigDecimal("15.00"))
            )

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val activity = (result as Result.Success).value
            assertThat(activity.id).isNotNull()
            assertThat(activity.ladderId).isEqualTo(ladder.id)
            assertThat(activity.name).isEqualTo("Canoeing")
            assertThat(activity.imageUrl).isEqualTo("https://img.com/canoe.jpg")
            assertThat(activity.distanceMinutes).isEqualTo(90)
            assertThat(activity.costPerPerson).isEqualByComparingTo(BigDecimal("15.00"))
            assertThat(activity.losses).isEqualTo(0)
            assertThat(activity.bracket).isEqualTo(LadderBracket.WINNERS)
            assertThat(activity.displayOrder).isEqualTo(1)
            assertThat(activity.createdAt).isInstanceOf(Instant::class.java)
        }

        @Test
        fun `addActivity assigns displayOrder as MAX + 1 for each subsequent activity`() {
            val creatorId = UUID.randomUUID()
            insertUser(creatorId)
            val ladder = (client.createLadder(CreateLadderParam(creatorId, "Multi Activity Ladder")) as Result.Success).value

            val act1 = (client.addActivity(AddLadderActivityParam(ladder.id, "Hiking", "https://img.com/hike.jpg", 60, BigDecimal("10.00"))) as Result.Success).value
            val act2 = (client.addActivity(AddLadderActivityParam(ladder.id, "Kayaking", "https://img.com/kayak.jpg", 120, BigDecimal("25.00"))) as Result.Success).value
            val act3 = (client.addActivity(AddLadderActivityParam(ladder.id, "Rock Climbing", "https://img.com/rock.jpg", 180, BigDecimal("42.50"))) as Result.Success).value

            assertThat(act1.displayOrder).isEqualTo(1)
            assertThat(act2.displayOrder).isEqualTo(2)
            assertThat(act3.displayOrder).isEqualTo(3)
        }

        @Test
        fun `addActivity round-trips BigDecimal costPerPerson at 42_50`() {
            val creatorId = UUID.randomUUID()
            insertUser(creatorId)
            val ladder = (client.createLadder(CreateLadderParam(creatorId, "Cost Ladder")) as Result.Success).value

            val result = client.addActivity(
                AddLadderActivityParam(ladder.id, "Skydiving", "https://img.com/sky.jpg", 30, BigDecimal("42.50"))
            )

            assertThat((result as Result.Success).value.costPerPerson).isEqualByComparingTo(BigDecimal("42.50"))
        }

        @Test
        fun `addActivity round-trips BigDecimal costPerPerson at 0_01`() {
            val creatorId = UUID.randomUUID()
            insertUser(creatorId)
            val ladder = (client.createLadder(CreateLadderParam(creatorId, "Cheap Ladder")) as Result.Success).value

            val result = client.addActivity(
                AddLadderActivityParam(ladder.id, "Walking", "https://img.com/walk.jpg", 15, BigDecimal("0.01"))
            )

            assertThat((result as Result.Success).value.costPerPerson).isEqualByComparingTo(BigDecimal("0.01"))
        }

        @Test
        fun `addActivity with negative distanceMinutes returns ValidationError without touching DB`() {
            val creatorId = UUID.randomUUID()
            insertUser(creatorId)
            val ladder = (client.createLadder(CreateLadderParam(creatorId, "Invalid Activity Ladder")) as Result.Success).value

            val result = client.addActivity(
                AddLadderActivityParam(ladder.id, "Bad Activity", "https://img.com/bad.jpg", -1, BigDecimal("10.00"))
            )

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).error).isInstanceOf(ValidationError::class.java)
            assertThat(countRowsWhere("ladder_activities", "ladder_id", ladder.id)).isEqualTo(0L)
        }

        @Test
        fun `addActivity with negative costPerPerson returns ValidationError`() {
            val creatorId = UUID.randomUUID()
            insertUser(creatorId)
            val ladder = (client.createLadder(CreateLadderParam(creatorId, "Invalid Cost Ladder")) as Result.Success).value

            val result = client.addActivity(
                AddLadderActivityParam(ladder.id, "Bad Cost Activity", "https://img.com/bad2.jpg", 30, BigDecimal("-1.00"))
            )

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).error).isInstanceOf(ValidationError::class.java)
        }
    }

    // ─── removeActivity ──────────────────────────────────────────────────────

    @Nested
    inner class RemoveActivity {

        @Test
        fun `removeActivity deletes the activity`() {
            val creatorId = UUID.randomUUID()
            insertUser(creatorId)
            val ladderId = UUID.randomUUID()
            insertLadder(ladderId, creatorId)
            val actId = UUID.randomUUID()
            insertActivity(actId, ladderId, displayOrder = 1)

            val result = client.removeActivity(RemoveLadderActivityParam(ladderId = ladderId, activityId = actId))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            assertThat(countRowsWhere("ladder_activities", "id", actId)).isEqualTo(0L)
        }

        @Test
        fun `removeActivity leaves other activities with their original displayOrder`() {
            val creatorId = UUID.randomUUID()
            insertUser(creatorId)
            val ladderId = UUID.randomUUID()
            insertLadder(ladderId, creatorId)
            val actId1 = UUID.randomUUID()
            val actId2 = UUID.randomUUID()
            val actId3 = UUID.randomUUID()
            insertActivity(actId1, ladderId, name = "Act 1", displayOrder = 1)
            insertActivity(actId2, ladderId, name = "Act 2", displayOrder = 2)
            insertActivity(actId3, ladderId, name = "Act 3", displayOrder = 3)

            client.removeActivity(RemoveLadderActivityParam(ladderId = ladderId, activityId = actId2))

            val remaining = (client.getActivities(GetLadderActivitiesParam(ladderId)) as Result.Success).value
            assertThat(remaining).hasSize(2)
            assertThat(remaining.map { it.displayOrder }).containsExactly(1, 3)
            assertThat(remaining.map { it.name }).containsExactly("Act 1", "Act 3")
        }

        @Test
        fun `removeActivity returns NotFoundError for missing activity`() {
            val creatorId = UUID.randomUUID()
            insertUser(creatorId)
            val ladderId = UUID.randomUUID()
            insertLadder(ladderId, creatorId)

            val result = client.removeActivity(RemoveLadderActivityParam(ladderId = ladderId, activityId = UUID.randomUUID()))

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).error).isInstanceOf(NotFoundError::class.java)
        }
    }

    // ─── getActivities ───────────────────────────────────────────────────────

    @Nested
    inner class GetActivities {

        @Test
        fun `getActivities returns activities ordered by displayOrder`() {
            val creatorId = UUID.randomUUID()
            insertUser(creatorId)
            val ladderId = UUID.randomUUID()
            insertLadder(ladderId, creatorId)
            // Insert out of order to verify sort
            insertActivity(UUID.randomUUID(), ladderId, name = "Third", displayOrder = 3)
            insertActivity(UUID.randomUUID(), ladderId, name = "First", displayOrder = 1)
            insertActivity(UUID.randomUUID(), ladderId, name = "Second", displayOrder = 2)

            val result = client.getActivities(GetLadderActivitiesParam(ladderId = ladderId))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val activities = (result as Result.Success).value
            assertThat(activities).hasSize(3)
            assertThat(activities.map { it.name }).containsExactly("First", "Second", "Third")
            assertThat(activities.map { it.displayOrder }).containsExactly(1, 2, 3)
        }

        @Test
        fun `getActivities returns empty list for ladder with no activities`() {
            val creatorId = UUID.randomUUID()
            insertUser(creatorId)
            val ladderId = UUID.randomUUID()
            insertLadder(ladderId, creatorId)

            val result = client.getActivities(GetLadderActivitiesParam(ladderId = ladderId))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            assertThat((result as Result.Success).value).isEmpty()
        }
    }

    // ─── updateLadderState ───────────────────────────────────────────────────

    @Nested
    inner class UpdateLadderState {

        @Test
        fun `updateLadderState updates status and round and bumps updatedAt`() {
            val creatorId = UUID.randomUUID()
            insertUser(creatorId)
            val ladderId = UUID.randomUUID()
            insertLadder(ladderId, creatorId)
            val before = (client.getLadderById(GetLadderByIdParam(ladderId)) as Result.Success).value
            Thread.sleep(10) // ensure updatedAt is strictly after createdAt

            val result = client.updateLadderState(
                UpdateLadderStateParam(
                    ladderId = ladderId,
                    status = LadderStatus.ACTIVE,
                    currentRoundNumber = 1,
                    currentMatchActivityAId = null,
                    currentMatchActivityBId = null,
                    isFinalRound = false,
                    isGrandFinalReset = false,
                )
            )

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val updated = (result as Result.Success).value
            assertThat(updated.status).isEqualTo(LadderStatus.ACTIVE)
            assertThat(updated.currentRoundNumber).isEqualTo(1)
            assertThat(updated.isFinalRound).isFalse()
            assertThat(updated.isGrandFinalReset).isFalse()
            assertThat(updated.updatedAt).isAfter(before.updatedAt)
        }

        @Test
        fun `updateLadderState cycles through all LadderStatus values`() {
            val creatorId = UUID.randomUUID()
            insertUser(creatorId)
            val ladderId = UUID.randomUUID()
            insertLadder(ladderId, creatorId, status = "DRAFT")

            val draftLadder = (client.getLadderById(GetLadderByIdParam(ladderId)) as Result.Success).value
            assertThat(draftLadder.status).isEqualTo(LadderStatus.DRAFT)

            val activeLadder = (client.updateLadderState(
                UpdateLadderStateParam(ladderId, LadderStatus.ACTIVE, 1, null, null, isFinalRound = false, isGrandFinalReset = false)
            ) as Result.Success).value
            assertThat(activeLadder.status).isEqualTo(LadderStatus.ACTIVE)

            // COMPLETED reached via setLadderWinner
            val actId = UUID.randomUUID()
            insertActivity(actId, ladderId)
            val completedLadder = (client.setLadderWinner(SetLadderWinnerParam(ladderId, actId)) as Result.Success).value
            assertThat(completedLadder.status).isEqualTo(LadderStatus.COMPLETED)
        }

        @Test
        fun `updateLadderState returns NotFoundError for non-existent ladder`() {
            val result = client.updateLadderState(
                UpdateLadderStateParam(
                    ladderId = UUID.randomUUID(),
                    status = LadderStatus.ACTIVE,
                    currentRoundNumber = 1,
                    currentMatchActivityAId = null,
                    currentMatchActivityBId = null,
                    isFinalRound = false,
                    isGrandFinalReset = false,
                )
            )

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).error).isInstanceOf(NotFoundError::class.java)
        }

        @Test
        fun `updateLadderState round-trips nullable UUID match fields — set non-null then null`() {
            val creatorId = UUID.randomUUID()
            insertUser(creatorId)
            val ladderId = UUID.randomUUID()
            insertLadder(ladderId, creatorId)
            val actAId = UUID.randomUUID()
            val actBId = UUID.randomUUID()
            insertActivity(actAId, ladderId, displayOrder = 1)
            insertActivity(actBId, ladderId, displayOrder = 2)

            // Set non-null match IDs and boolean flags
            val withMatches = (client.updateLadderState(
                UpdateLadderStateParam(
                    ladderId = ladderId,
                    status = LadderStatus.ACTIVE,
                    currentRoundNumber = 1,
                    currentMatchActivityAId = actAId,
                    currentMatchActivityBId = actBId,
                    isFinalRound = true,
                    isGrandFinalReset = true,
                )
            ) as Result.Success).value

            assertThat(withMatches.currentMatchActivityAId).isEqualTo(actAId)
            assertThat(withMatches.currentMatchActivityBId).isEqualTo(actBId)
            assertThat(withMatches.isFinalRound).isTrue()
            assertThat(withMatches.isGrandFinalReset).isTrue()

            // Clear the nullable UUIDs back to null
            val withNulls = (client.updateLadderState(
                UpdateLadderStateParam(
                    ladderId = ladderId,
                    status = LadderStatus.ACTIVE,
                    currentRoundNumber = 2,
                    currentMatchActivityAId = null,
                    currentMatchActivityBId = null,
                    isFinalRound = false,
                    isGrandFinalReset = false,
                )
            ) as Result.Success).value

            assertThat(withNulls.currentMatchActivityAId).isNull()
            assertThat(withNulls.currentMatchActivityBId).isNull()
            assertThat(withNulls.isFinalRound).isFalse()
            assertThat(withNulls.isGrandFinalReset).isFalse()
        }
    }

    // ─── setLadderWinner ─────────────────────────────────────────────────────

    @Nested
    inner class SetLadderWinner {

        @Test
        fun `setLadderWinner sets winner activity and marks ladder COMPLETED`() {
            val creatorId = UUID.randomUUID()
            insertUser(creatorId)
            val ladderId = UUID.randomUUID()
            insertLadder(ladderId, creatorId, status = "ACTIVE", currentRoundNumber = 3)
            val winnerId = UUID.randomUUID()
            insertActivity(winnerId, ladderId)

            val result = client.setLadderWinner(SetLadderWinnerParam(ladderId = ladderId, winnerActivityId = winnerId))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val ladder = (result as Result.Success).value
            assertThat(ladder.status).isEqualTo(LadderStatus.COMPLETED)
            assertThat(ladder.winnerActivityId).isEqualTo(winnerId)
        }

        @Test
        fun `setLadderWinner returns NotFoundError for non-existent ladder`() {
            val result = client.setLadderWinner(
                SetLadderWinnerParam(ladderId = UUID.randomUUID(), winnerActivityId = UUID.randomUUID())
            )

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).error).isInstanceOf(NotFoundError::class.java)
        }
    }

    // ─── restartLadder ───────────────────────────────────────────────────────

    @Nested
    inner class RestartLadder {

        @Test
        fun `restartLadder atomically deletes votes and participants, resets all activities, returns DRAFT ladder`() {
            val creatorId = UUID.randomUUID()
            val user1 = UUID.randomUUID()
            val user2 = UUID.randomUUID()
            insertUser(creatorId)
            insertUser(user1, "user1@example.com")
            insertUser(user2, "user2@example.com")

            val ladderId = UUID.randomUUID()
            insertLadder(
                ladderId, creatorId,
                status = "ACTIVE",
                currentRoundNumber = 3,
                isFinalRound = true,
                isGrandFinalReset = true,
            )

            val actId1 = UUID.randomUUID()
            val actId2 = UUID.randomUUID()
            val actId3 = UUID.randomUUID()
            insertActivity(actId1, ladderId, name = "Hiking", losses = 1, bracket = "LOSERS", displayOrder = 1)
            insertActivity(actId2, ladderId, name = "Kayaking", losses = 2, bracket = "ELIMINATED", displayOrder = 2)
            insertActivity(actId3, ladderId, name = "Cycling", losses = 0, bracket = "WINNERS", displayOrder = 3)

            insertParticipant(UUID.randomUUID(), ladderId, user1)
            insertParticipant(UUID.randomUUID(), ladderId, user2)

            insertVote(UUID.randomUUID(), ladderId, 1, user1, actId1)
            insertVote(UUID.randomUUID(), ladderId, 1, user2, actId2)
            insertVote(UUID.randomUUID(), ladderId, 2, user1, actId3)

            // Pre-conditions: votes, participants, activities with non-zero losses
            assertThat(countRowsWhere("ladder_votes", "ladder_id", ladderId)).isEqualTo(3L)
            assertThat(countRowsWhere("ladder_participants", "ladder_id", ladderId)).isEqualTo(2L)

            val result = client.restartLadder(RestartLadderParam(ladderId = ladderId))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val ladder = (result as Result.Success).value
            assertThat(ladder.status).isEqualTo(LadderStatus.DRAFT)
            assertThat(ladder.currentRoundNumber).isNull()
            assertThat(ladder.currentMatchActivityAId).isNull()
            assertThat(ladder.currentMatchActivityBId).isNull()
            assertThat(ladder.isFinalRound).isFalse()
            assertThat(ladder.isGrandFinalReset).isFalse()
            assertThat(ladder.winnerActivityId).isNull()

            // ALL votes gone
            assertThat(countRowsWhere("ladder_votes", "ladder_id", ladderId)).isEqualTo(0L)
            // ALL participants gone
            assertThat(countRowsWhere("ladder_participants", "ladder_id", ladderId)).isEqualTo(0L)
            // ALL activities reset to losses=0 / bracket=WINNERS
            val activities = (client.getActivities(GetLadderActivitiesParam(ladderId)) as Result.Success).value
            assertThat(activities).hasSize(3)
            assertThat(activities).allSatisfy { activity ->
                assertThat(activity.losses).isEqualTo(0)
                assertThat(activity.bracket).isEqualTo(LadderBracket.WINNERS)
            }
        }

        @Test
        fun `restartLadder from COMPLETED status clears winnerActivityId and all state`() {
            val creatorId = UUID.randomUUID()
            val user1 = UUID.randomUUID()
            insertUser(creatorId)
            insertUser(user1, "user1@example.com")

            val ladderId = UUID.randomUUID()
            val actId1 = UUID.randomUUID()
            val actId2 = UUID.randomUUID()
            // Seed acts first (needed for the FK on winner_activity_id)
            // We insert the ladder last so winnerActivityId FK resolves — use two-step approach:
            // insert ladder without winner, then use a direct UPDATE to set winner
            insertLadder(ladderId, creatorId, status = "DRAFT")
            insertActivity(actId1, ladderId, name = "Swimming", losses = 1, bracket = "LOSERS", displayOrder = 1)
            insertActivity(actId2, ladderId, name = "Running", losses = 0, bracket = "WINNERS", displayOrder = 2)

            // Advance ladder to COMPLETED with a winner set
            client.updateLadderState(
                UpdateLadderStateParam(ladderId, LadderStatus.ACTIVE, 2, actId1, actId2, isFinalRound = true, isGrandFinalReset = false)
            )
            val completedLadder = (client.setLadderWinner(SetLadderWinnerParam(ladderId, actId2)) as Result.Success).value
            assertThat(completedLadder.status).isEqualTo(LadderStatus.COMPLETED)
            assertThat(completedLadder.winnerActivityId).isEqualTo(actId2)

            // Seed participant + vote to prove they get wiped
            insertParticipant(UUID.randomUUID(), ladderId, user1)
            insertVote(UUID.randomUUID(), ladderId, 2, user1, actId2)

            val result = client.restartLadder(RestartLadderParam(ladderId = ladderId))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val restarted = (result as Result.Success).value
            assertThat(restarted.status).isEqualTo(LadderStatus.DRAFT)
            assertThat(restarted.winnerActivityId).isNull()
            assertThat(restarted.currentRoundNumber).isNull()
            assertThat(restarted.currentMatchActivityAId).isNull()
            assertThat(restarted.currentMatchActivityBId).isNull()
            assertThat(restarted.isFinalRound).isFalse()
            assertThat(restarted.isGrandFinalReset).isFalse()

            // votes and participants gone
            assertThat(countRowsWhere("ladder_votes", "ladder_id", ladderId)).isEqualTo(0L)
            assertThat(countRowsWhere("ladder_participants", "ladder_id", ladderId)).isEqualTo(0L)

            // activities reset
            val activities = (client.getActivities(GetLadderActivitiesParam(ladderId)) as Result.Success).value
            assertThat(activities).hasSize(2)
            assertThat(activities).allSatisfy { activity ->
                assertThat(activity.losses).isEqualTo(0)
                assertThat(activity.bracket).isEqualTo(LadderBracket.WINNERS)
            }
        }
    }

    // ─── updateActivityLossAndBracket ─────────────────────────────────────────

    @Nested
    inner class UpdateActivityLossAndBracket {

        @Test
        fun `updateActivityLossAndBracket updates losses and bracket`() {
            val creatorId = UUID.randomUUID()
            insertUser(creatorId)
            val ladderId = UUID.randomUUID()
            insertLadder(ladderId, creatorId)
            val actId = UUID.randomUUID()
            insertActivity(actId, ladderId, losses = 0, bracket = "WINNERS")

            val result = client.updateActivityLossAndBracket(
                UpdateActivityLossAndBracketParam(activityId = actId, losses = 1, bracket = LadderBracket.LOSERS)
            )

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val activity = (result as Result.Success).value
            assertThat(activity.id).isEqualTo(actId)
            assertThat(activity.losses).isEqualTo(1)
            assertThat(activity.bracket).isEqualTo(LadderBracket.LOSERS)
        }

        @Test
        fun `updateActivityLossAndBracket round-trips all LadderBracket enum values`() {
            val creatorId = UUID.randomUUID()
            insertUser(creatorId)
            val ladderId = UUID.randomUUID()
            insertLadder(ladderId, creatorId)
            val actId = UUID.randomUUID()
            insertActivity(actId, ladderId, losses = 0, bracket = "WINNERS")

            val losersResult = (client.updateActivityLossAndBracket(
                UpdateActivityLossAndBracketParam(actId, 1, LadderBracket.LOSERS)
            ) as Result.Success).value
            assertThat(losersResult.bracket).isEqualTo(LadderBracket.LOSERS)

            val eliminatedResult = (client.updateActivityLossAndBracket(
                UpdateActivityLossAndBracketParam(actId, 2, LadderBracket.ELIMINATED)
            ) as Result.Success).value
            assertThat(eliminatedResult.bracket).isEqualTo(LadderBracket.ELIMINATED)

            val winnersResult = (client.updateActivityLossAndBracket(
                UpdateActivityLossAndBracketParam(actId, 0, LadderBracket.WINNERS)
            ) as Result.Success).value
            assertThat(winnersResult.bracket).isEqualTo(LadderBracket.WINNERS)
        }

        @Test
        fun `updateActivityLossAndBracket returns NotFoundError for missing activity`() {
            val result = client.updateActivityLossAndBracket(
                UpdateActivityLossAndBracketParam(UUID.randomUUID(), 1, LadderBracket.LOSERS)
            )

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).error).isInstanceOf(NotFoundError::class.java)
        }
    }

    // ─── bulkInsertParticipants ───────────────────────────────────────────────

    @Nested
    inner class BulkInsertParticipants {

        @Test
        fun `bulkInsertParticipants inserts N participants atomically and returns them all`() {
            val creatorId = UUID.randomUUID()
            val user1 = UUID.randomUUID()
            val user2 = UUID.randomUUID()
            val user3 = UUID.randomUUID()
            insertUser(creatorId)
            insertUser(user1, "u1@example.com")
            insertUser(user2, "u2@example.com")
            insertUser(user3, "u3@example.com")
            val ladderId = UUID.randomUUID()
            insertLadder(ladderId, creatorId)

            val result = client.bulkInsertParticipants(
                BulkInsertParticipantsParam(ladderId = ladderId, userIds = setOf(user1, user2, user3))
            )

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val participants = (result as Result.Success).value
            assertThat(participants).hasSize(3)
            assertThat(participants.map { it.userId }).containsExactlyInAnyOrder(user1, user2, user3)
            assertThat(participants).allSatisfy { p ->
                assertThat(p.ladderId).isEqualTo(ladderId)
                assertThat(p.createdAt).isInstanceOf(Instant::class.java)
            }
        }

        @Test
        fun `bulkInsertParticipants is idempotent — overlapping userId does not raise and does not duplicate`() {
            val creatorId = UUID.randomUUID()
            val user1 = UUID.randomUUID()
            val user2 = UUID.randomUUID()
            insertUser(creatorId)
            insertUser(user1, "u1@example.com")
            insertUser(user2, "u2@example.com")
            val ladderId = UUID.randomUUID()
            insertLadder(ladderId, creatorId)

            val first = client.bulkInsertParticipants(BulkInsertParticipantsParam(ladderId, setOf(user1, user2)))
            assertThat(first).isInstanceOf(Result.Success::class.java)

            // Calling again with overlapping user1 must NOT raise ConflictError
            val second = client.bulkInsertParticipants(BulkInsertParticipantsParam(ladderId, setOf(user1)))
            assertThat(second).isInstanceOf(Result.Success::class.java)

            // Still only 2 distinct participants in the DB
            assertThat(countRowsWhere("ladder_participants", "ladder_id", ladderId)).isEqualTo(2L)
        }

        @Test
        fun `bulkInsertParticipants with empty set returns empty list`() {
            val creatorId = UUID.randomUUID()
            insertUser(creatorId)
            val ladderId = UUID.randomUUID()
            insertLadder(ladderId, creatorId)

            val result = client.bulkInsertParticipants(BulkInsertParticipantsParam(ladderId, emptySet()))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            assertThat((result as Result.Success).value).isEmpty()
        }
    }

    // ─── getParticipants ─────────────────────────────────────────────────────

    @Nested
    inner class GetParticipants {

        @Test
        fun `getParticipants returns participants for the ladder`() {
            val creatorId = UUID.randomUUID()
            val user1 = UUID.randomUUID()
            val user2 = UUID.randomUUID()
            insertUser(creatorId)
            insertUser(user1, "u1@example.com")
            insertUser(user2, "u2@example.com")
            val ladderId = UUID.randomUUID()
            insertLadder(ladderId, creatorId)
            insertParticipant(UUID.randomUUID(), ladderId, user1)
            insertParticipant(UUID.randomUUID(), ladderId, user2)

            val result = client.getParticipants(GetParticipantsParam(ladderId = ladderId))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val participants = (result as Result.Success).value
            assertThat(participants).hasSize(2)
            assertThat(participants.map { it.userId }).containsExactlyInAnyOrder(user1, user2)
            assertThat(participants).allSatisfy { p ->
                assertThat(p.ladderId).isEqualTo(ladderId)
                assertThat(p.id).isNotNull()
                assertThat(p.createdAt).isInstanceOf(Instant::class.java)
            }
        }

        @Test
        fun `getParticipants returns empty list when no participants`() {
            val creatorId = UUID.randomUUID()
            insertUser(creatorId)
            val ladderId = UUID.randomUUID()
            insertLadder(ladderId, creatorId)

            val result = client.getParticipants(GetParticipantsParam(ladderId = ladderId))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            assertThat((result as Result.Success).value).isEmpty()
        }
    }

    // ─── castVote ────────────────────────────────────────────────────────────

    @Nested
    inner class CastVote {

        @Test
        fun `castVote inserts a vote and returns it with all fields`() {
            val creatorId = UUID.randomUUID()
            val userId = UUID.randomUUID()
            insertUser(creatorId)
            insertUser(userId, "voter@example.com")
            val ladderId = UUID.randomUUID()
            insertLadder(ladderId, creatorId, status = "ACTIVE")
            val actId = UUID.randomUUID()
            insertActivity(actId, ladderId)

            val result = client.withLadderLocked(ladderId) { scopedClient ->
                scopedClient.castVote(
                    CastVoteParam(ladderId = ladderId, roundNumber = 1, userId = userId, votedForActivityId = actId)
                )
            }

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val vote = (result as Result.Success).value
            assertThat(vote.id).isNotNull()
            assertThat(vote.ladderId).isEqualTo(ladderId)
            assertThat(vote.roundNumber).isEqualTo(1)
            assertThat(vote.userId).isEqualTo(userId)
            assertThat(vote.votedForActivityId).isEqualTo(actId)
            assertThat(vote.createdAt).isInstanceOf(Instant::class.java)
        }

        @Test
        fun `castVote duplicate on same (ladderId, roundNumber, userId) returns ConflictError`() {
            val creatorId = UUID.randomUUID()
            val userId = UUID.randomUUID()
            insertUser(creatorId)
            insertUser(userId, "voter2@example.com")
            val ladderId = UUID.randomUUID()
            insertLadder(ladderId, creatorId, status = "ACTIVE")
            val actId = UUID.randomUUID()
            insertActivity(actId, ladderId)

            val firstVote = client.withLadderLocked(ladderId) { scopedClient ->
                scopedClient.castVote(CastVoteParam(ladderId, 1, userId, actId))
            }
            assertThat(firstVote).isInstanceOf(Result.Success::class.java)

            val duplicateVote = client.withLadderLocked(ladderId) { scopedClient ->
                scopedClient.castVote(CastVoteParam(ladderId, 1, userId, actId))
            }
            assertThat(duplicateVote).isInstanceOf(Result.Failure::class.java)
            assertThat((duplicateVote as Result.Failure).error).isInstanceOf(ConflictError::class.java)
        }
    }

    // ─── getVotesForRound ────────────────────────────────────────────────────

    @Nested
    inner class GetVotesForRound {

        @Test
        fun `getVotesForRound returns votes only for the specified round`() {
            val creatorId = UUID.randomUUID()
            val user1 = UUID.randomUUID()
            val user2 = UUID.randomUUID()
            insertUser(creatorId)
            insertUser(user1, "u1@example.com")
            insertUser(user2, "u2@example.com")
            val ladderId = UUID.randomUUID()
            insertLadder(ladderId, creatorId)
            val actId = UUID.randomUUID()
            insertActivity(actId, ladderId)

            insertVote(UUID.randomUUID(), ladderId, 1, user1, actId)
            insertVote(UUID.randomUUID(), ladderId, 1, user2, actId)
            insertVote(UUID.randomUUID(), ladderId, 2, user1, actId) // different round

            val result = client.getVotesForRound(GetVotesForRoundParam(ladderId = ladderId, roundNumber = 1))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val votes = (result as Result.Success).value
            assertThat(votes).hasSize(2)
            assertThat(votes).allSatisfy { v ->
                assertThat(v.roundNumber).isEqualTo(1)
                assertThat(v.ladderId).isEqualTo(ladderId)
                assertThat(v.votedForActivityId).isEqualTo(actId)
                assertThat(v.createdAt).isInstanceOf(Instant::class.java)
            }
        }

        @Test
        fun `getVotesForRound returns empty list for round with no votes`() {
            val creatorId = UUID.randomUUID()
            insertUser(creatorId)
            val ladderId = UUID.randomUUID()
            insertLadder(ladderId, creatorId)

            val result = client.getVotesForRound(GetVotesForRoundParam(ladderId = ladderId, roundNumber = 99))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            assertThat((result as Result.Success).value).isEmpty()
        }
    }

    // ─── countVotesForRound ──────────────────────────────────────────────────

    @Nested
    inner class CountVotesForRound {

        @Test
        fun `countVotesForRound returns correct count for the round`() {
            val creatorId = UUID.randomUUID()
            val user1 = UUID.randomUUID()
            val user2 = UUID.randomUUID()
            val user3 = UUID.randomUUID()
            insertUser(creatorId)
            insertUser(user1, "u1@example.com")
            insertUser(user2, "u2@example.com")
            insertUser(user3, "u3@example.com")
            val ladderId = UUID.randomUUID()
            insertLadder(ladderId, creatorId)
            val actId = UUID.randomUUID()
            insertActivity(actId, ladderId)

            insertVote(UUID.randomUUID(), ladderId, 1, user1, actId)
            insertVote(UUID.randomUUID(), ladderId, 1, user2, actId)
            insertVote(UUID.randomUUID(), ladderId, 1, user3, actId)
            insertVote(UUID.randomUUID(), ladderId, 2, user1, actId) // round 2 — excluded

            val result = client.countVotesForRound(CountVotesForRoundParam(ladderId = ladderId, roundNumber = 1))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            assertThat((result as Result.Success).value).isEqualTo(3)
        }

        @Test
        fun `countVotesForRound returns 0 for round with no votes`() {
            val creatorId = UUID.randomUUID()
            insertUser(creatorId)
            val ladderId = UUID.randomUUID()
            insertLadder(ladderId, creatorId)

            val result = client.countVotesForRound(CountVotesForRoundParam(ladderId = ladderId, roundNumber = 99))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            assertThat((result as Result.Success).value).isEqualTo(0)
        }
    }

    // ─── withLadderLocked ────────────────────────────────────────────────────

    @Nested
    inner class WithLadderLocked {

        @Test
        fun `withLadderLocked returns NotFoundError for non-existent ladder and does not invoke block`() {
            var blockWasCalled = false

            val result = client.withLadderLocked(UUID.randomUUID()) { _ ->
                blockWasCalled = true
                success(Unit)
            }

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).error).isInstanceOf(NotFoundError::class.java)
            assertThat(blockWasCalled).isFalse()
        }

        /**
         * Concurrency test: two threads race to call `withLadderLocked` on the same ladder and
         * both increment `currentRoundNumber` by 1.
         *
         * Mechanism:
         * 1. Both threads arrive at a [CyclicBarrier] so they start as close to simultaneously
         *    as possible, maximising lock contention.
         * 2. Inside the locked block each thread reads the current `currentRoundNumber`,
         *    records it in [readValuesInsideLock], then writes `currentRoundNumber + 1`.
         * 3. Because `withLadderLocked` issues `SELECT ... FOR UPDATE`, only one thread can
         *    hold the lock at a time. The second thread blocks until the first transaction commits,
         *    then re-fetches the row under its own lock — seeing the first thread's committed value.
         * 4. After both threads join:
         *    - [readValuesInsideLock] must contain {0, 1} — proving one thread saw the other's
         *      committed write (no stale read).
         *    - The final `currentRoundNumber` in the DB must be 2 — proving no update was lost.
         */
        @Test
        fun `withLadderLocked serializes concurrent increments — no update is lost`() {
            val creatorId = UUID.randomUUID()
            insertUser(creatorId)
            val ladderId = UUID.randomUUID()
            insertLadder(ladderId, creatorId, status = "ACTIVE", currentRoundNumber = 0)

            val barrier = CyclicBarrier(2)
            val errors = CopyOnWriteArrayList<Throwable>()
            val readValuesInsideLock = CopyOnWriteArrayList<Int>()

            fun makeThread() = Thread {
                try {
                    barrier.await() // sync both threads to maximise simultaneous lock contention
                    client.withLadderLocked(ladderId) { scopedClient ->
                        val ladder = (scopedClient.getLadderById(GetLadderByIdParam(ladderId)) as Result.Success).value
                        val current = ladder.currentRoundNumber!!
                        readValuesInsideLock.add(current)
                        scopedClient.updateLadderState(
                            UpdateLadderStateParam(
                                ladderId = ladderId,
                                status = LadderStatus.ACTIVE,
                                currentRoundNumber = current + 1,
                                currentMatchActivityAId = null,
                                currentMatchActivityBId = null,
                                isFinalRound = false,
                                isGrandFinalReset = false,
                            )
                        )
                    }
                } catch (e: Throwable) {
                    errors.add(e)
                }
            }

            val t1 = makeThread()
            val t2 = makeThread()
            t1.start()
            t2.start()
            t1.join(10_000)
            t2.join(10_000)

            assertThat(t1.isAlive).isFalse()
            assertThat(t2.isAlive).isFalse()
            assertThat(errors).isEmpty()

            // No update was lost — final currentRoundNumber must be 2
            val finalLadder = (client.getLadderById(GetLadderByIdParam(ladderId)) as Result.Success).value
            assertThat(finalLadder.currentRoundNumber).isEqualTo(2)

            // Lock serialized the transactions: one thread read 0, the other read 1
            assertThat(readValuesInsideLock).containsExactlyInAnyOrder(0, 1)
        }
    }
}
