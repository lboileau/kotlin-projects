package com.acme.services.camperservice.features.activityladder.service

import com.acme.clients.activityladderclient.fake.FakeActivityLadderClient
import com.acme.clients.activityladderclient.model.Ladder as ClientLadder
import com.acme.clients.activityladderclient.model.LadderActivity as ClientLadderActivity
import com.acme.clients.activityladderclient.model.LadderBracket as ClientLadderBracket
import com.acme.clients.activityladderclient.model.LadderParticipant as ClientLadderParticipant
import com.acme.clients.activityladderclient.model.LadderStatus as ClientLadderStatus
import com.acme.clients.activityladderclient.model.LadderVote as ClientLadderVote
import com.acme.clients.common.Result
import com.acme.clients.userclient.fake.FakeUserClient
import com.acme.clients.userclient.model.User
import com.acme.services.camperservice.features.activityladder.error.LadderError
import com.acme.services.camperservice.features.activityladder.model.LadderStatus
import com.acme.services.camperservice.features.activityladder.model.VoteOutcome
import com.acme.services.camperservice.features.activityladder.params.AddActivityParam
import com.acme.services.camperservice.features.activityladder.params.CastVoteParam
import com.acme.services.camperservice.features.activityladder.params.CreateLadderParam
import com.acme.services.camperservice.features.activityladder.params.GetLadderDetailParam
import com.acme.services.camperservice.features.activityladder.params.ListLaddersParam
import com.acme.services.camperservice.features.activityladder.params.NewActivityInput
import com.acme.services.camperservice.features.activityladder.params.RemoveActivityParam
import com.acme.services.camperservice.features.activityladder.params.RestartLadderParam
import com.acme.services.camperservice.features.activityladder.params.StartLadderParam
import com.acme.services.camperservice.websocket.LadderEventPublisher
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.SimpMessagingTemplate
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class ActivityLadderServiceTest {

    private val fakeLadderClient = FakeActivityLadderClient()
    private val fakeUserClient = FakeUserClient()
    private val noOpChannel = object : MessageChannel {
        override fun send(message: Message<*>): Boolean = true
        override fun send(message: Message<*>, timeout: Long): Boolean = true
    }
    private val eventPublisher = LadderEventPublisher(SimpMessagingTemplate(noOpChannel))
    private val service = ActivityLadderService(fakeLadderClient, fakeUserClient, eventPublisher)

    private val creatorId = UUID.fromString("cc000000-0001-4000-8000-000000000001")
    private val otherUserId = UUID.fromString("cc000000-0001-4000-8000-000000000002")
    private val userId1 = UUID.fromString("cc000000-0001-4000-8000-000000000003")
    private val userId2 = UUID.fromString("cc000000-0001-4000-8000-000000000004")
    private val now = Instant.now()

    private fun clientLadder(
        id: UUID = UUID.randomUUID(),
        creatorId: UUID = this.creatorId,
        status: ClientLadderStatus = ClientLadderStatus.DRAFT,
        currentRoundNumber: Int? = null,
        currentMatchActivityAId: UUID? = null,
        currentMatchActivityBId: UUID? = null,
        isFinalRound: Boolean = false,
        isGrandFinalReset: Boolean = false,
        winnerActivityId: UUID? = null,
    ) = ClientLadder(
        id = id,
        creatorId = creatorId,
        title = "Summer Activities",
        status = status,
        currentRoundNumber = currentRoundNumber,
        currentMatchActivityAId = currentMatchActivityAId,
        currentMatchActivityBId = currentMatchActivityBId,
        isFinalRound = isFinalRound,
        isGrandFinalReset = isGrandFinalReset,
        winnerActivityId = winnerActivityId,
        createdAt = now,
        updatedAt = now,
    )

    private fun clientActivity(
        id: UUID = UUID.randomUUID(),
        ladderId: UUID,
        name: String = "Activity",
        losses: Int = 0,
        bracket: ClientLadderBracket = ClientLadderBracket.WINNERS,
        displayOrder: Int = 1,
    ) = ClientLadderActivity(
        id = id,
        ladderId = ladderId,
        name = name,
        imageUrl = "http://example.com/img.jpg",
        distanceMinutes = 15,
        costPerPerson = BigDecimal("25.00"),
        losses = losses,
        bracket = bracket,
        displayOrder = displayOrder,
        createdAt = now,
    )

    private fun clientParticipant(ladderId: UUID, userId: UUID) = ClientLadderParticipant(
        id = UUID.randomUUID(),
        ladderId = ladderId,
        userId = userId,
        createdAt = now,
    )

    private fun clientVote(
        ladderId: UUID,
        roundNumber: Int,
        userId: UUID,
        votedForActivityId: UUID,
    ) = ClientLadderVote(
        id = UUID.randomUUID(),
        ladderId = ladderId,
        roundNumber = roundNumber,
        userId = userId,
        votedForActivityId = votedForActivityId,
        createdAt = now,
    )

    private fun clientUser(id: UUID, username: String = "user_$id") = User(
        id = id,
        email = "$username@example.com",
        username = username,
        createdAt = now,
        updatedAt = now,
    )

    @BeforeEach
    fun setUp() {
        fakeLadderClient.reset()
        fakeUserClient.reset()
    }

    @Nested
    inner class CreateLadder {

        @Test
        fun `creates ladder with activities and returns detail`() {
            val result = service.create(
                CreateLadderParam(
                    requestingUserId = creatorId,
                    title = "Summer Activities",
                    activities = listOf(
                        NewActivityInput("Hiking", "http://img.com/hike.jpg", 60, BigDecimal("10.00")),
                        NewActivityInput("Swimming", "http://img.com/swim.jpg", 30, BigDecimal("5.00")),
                    ),
                )
            )

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val detail = (result as Result.Success).value
            assertThat(detail.ladder.title).isEqualTo("Summer Activities")
            assertThat(detail.ladder.creatorId).isEqualTo(creatorId)
            assertThat(detail.ladder.status).isEqualTo(LadderStatus.DRAFT)
            assertThat(detail.activities).hasSize(2)
            assertThat(detail.currentRound).isNull()
        }

        @Test
        fun `returns NotEnoughActivities when fewer than 2 activities provided at creation`() {
            val result = service.create(
                CreateLadderParam(
                    requestingUserId = creatorId,
                    title = "Solo Activity",
                    activities = listOf(
                        NewActivityInput("Only One", "http://img.com/one.jpg", 30, BigDecimal("10.00")),
                    ),
                )
            )

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).error).isInstanceOf(LadderError.NotEnoughActivities::class.java)
        }

        @Test
        fun `returns Invalid error for blank title`() {
            val result = service.create(
                CreateLadderParam(
                    requestingUserId = creatorId,
                    title = "   ",
                    activities = emptyList(),
                )
            )

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(LadderError.Invalid::class.java)
        }
    }

    @Nested
    inner class ListLadders {

        @Test
        fun `returns empty list when no ladders exist`() {
            val result = service.list(ListLaddersParam(requestingUserId = creatorId))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            assertThat((result as Result.Success).value).isEmpty()
        }

        @Test
        fun `returns summary with activity and participant counts`() {
            val ladderId = UUID.randomUUID()
            val ladder = clientLadder(id = ladderId)
            fakeLadderClient.seed(ladder)
            fakeLadderClient.seedActivities(
                clientActivity(ladderId = ladderId, displayOrder = 1),
                clientActivity(ladderId = ladderId, displayOrder = 2),
                clientActivity(ladderId = ladderId, displayOrder = 3),
            )
            fakeLadderClient.seedParticipants(
                clientParticipant(ladderId, userId1),
                clientParticipant(ladderId, userId2),
            )

            val result = service.list(ListLaddersParam(requestingUserId = creatorId))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val summaries = (result as Result.Success).value
            assertThat(summaries).hasSize(1)
            assertThat(summaries[0].activityCount).isEqualTo(3)
            assertThat(summaries[0].participantCount).isEqualTo(2)
        }

        @Test
        fun `returns summaries for multiple ladders`() {
            fakeLadderClient.seed(clientLadder(id = UUID.randomUUID()))
            fakeLadderClient.seed(clientLadder(id = UUID.randomUUID()))

            val result = service.list(ListLaddersParam(requestingUserId = creatorId))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            assertThat((result as Result.Success).value).hasSize(2)
        }
    }

    @Nested
    inner class GetLadderDetail {

        @Test
        fun `returns DRAFT ladder with no current round`() {
            val ladderId = UUID.randomUUID()
            fakeLadderClient.seed(clientLadder(id = ladderId, status = ClientLadderStatus.DRAFT))
            fakeLadderClient.seedActivities(
                clientActivity(ladderId = ladderId, name = "Hiking"),
            )

            val result = service.getDetail(GetLadderDetailParam(ladderId = ladderId, requestingUserId = creatorId))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val detail = (result as Result.Success).value
            assertThat(detail.ladder.status).isEqualTo(LadderStatus.DRAFT)
            assertThat(detail.currentRound).isNull()
            assertThat(detail.activities).hasSize(1)
            assertThat(detail.activities[0].name).isEqualTo("Hiking")
        }

        @Test
        fun `returns ACTIVE ladder with currentRound populated`() {
            val ladderId = UUID.randomUUID()
            val activityAId = UUID.randomUUID()
            val activityBId = UUID.randomUUID()
            fakeLadderClient.seed(
                clientLadder(
                    id = ladderId,
                    status = ClientLadderStatus.ACTIVE,
                    currentRoundNumber = 1,
                    currentMatchActivityAId = activityAId,
                    currentMatchActivityBId = activityBId,
                )
            )
            fakeLadderClient.seedActivities(
                clientActivity(id = activityAId, ladderId = ladderId, name = "Surfing"),
                clientActivity(id = activityBId, ladderId = ladderId, name = "Kayaking"),
            )
            fakeLadderClient.seedParticipants(clientParticipant(ladderId, userId1))
            fakeLadderClient.seedVotes(clientVote(ladderId, 1, userId1, activityAId))

            val result = service.getDetail(GetLadderDetailParam(ladderId = ladderId, requestingUserId = creatorId))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val detail = (result as Result.Success).value
            assertThat(detail.ladder.status).isEqualTo(LadderStatus.ACTIVE)
            assertThat(detail.currentRound).isNotNull
            assertThat(detail.currentRound!!.roundNumber).isEqualTo(1)
            assertThat(detail.currentRound.activityAId).isEqualTo(activityAId)
            assertThat(detail.currentRound.activityBId).isEqualTo(activityBId)
            assertThat(detail.currentRound.votesCast).isEqualTo(1)
            assertThat(detail.currentRound.totalVoters).isEqualTo(1)
            assertThat(detail.currentRound.votedUserIds).containsExactly(userId1)
        }

        @Test
        fun `enriches participants with user profile data`() {
            val ladderId = UUID.randomUUID()
            fakeLadderClient.seed(clientLadder(id = ladderId))
            fakeLadderClient.seedParticipants(clientParticipant(ladderId, userId1))
            fakeUserClient.seed(clientUser(userId1, "alice"))

            val result = service.getDetail(GetLadderDetailParam(ladderId = ladderId, requestingUserId = creatorId))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val detail = (result as Result.Success).value
            assertThat(detail.participants).hasSize(1)
            assertThat(detail.participants[0].userId).isEqualTo(userId1)
            assertThat(detail.participants[0].username).isEqualTo("alice")
        }

        @Test
        fun `participant without user record still appears with null username`() {
            val ladderId = UUID.randomUUID()
            fakeLadderClient.seed(clientLadder(id = ladderId))
            fakeLadderClient.seedParticipants(clientParticipant(ladderId, userId1))
            // userId1 not seeded in fakeUserClient

            val result = service.getDetail(GetLadderDetailParam(ladderId = ladderId, requestingUserId = creatorId))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val detail = (result as Result.Success).value
            assertThat(detail.participants).hasSize(1)
            assertThat(detail.participants[0].username).isNull()
        }

        @Test
        fun `returns NotFound for non-existent ladder`() {
            val result = service.getDetail(
                GetLadderDetailParam(ladderId = UUID.randomUUID(), requestingUserId = creatorId)
            )

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(LadderError.NotFound::class.java)
        }
    }

    @Nested
    inner class AddActivity {

        @Test
        fun `adds activity to DRAFT ladder`() {
            val ladderId = UUID.randomUUID()
            fakeLadderClient.seed(clientLadder(id = ladderId, creatorId = creatorId))

            val result = service.addActivity(
                AddActivityParam(
                    ladderId = ladderId,
                    requestingUserId = creatorId,
                    name = "Rock Climbing",
                    imageUrl = "http://img.com/climb.jpg",
                    distanceMinutes = 60,
                    costPerPerson = BigDecimal("80.00"),
                )
            )

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val activity = (result as Result.Success).value
            assertThat(activity.name).isEqualTo("Rock Climbing")
            assertThat(activity.ladderId).isEqualTo(ladderId)
        }

        @Test
        fun `returns NotCreator when requester is not the creator`() {
            val ladderId = UUID.randomUUID()
            fakeLadderClient.seed(clientLadder(id = ladderId, creatorId = creatorId))

            val result = service.addActivity(
                AddActivityParam(
                    ladderId = ladderId,
                    requestingUserId = otherUserId,
                    name = "Activity",
                    imageUrl = "http://img.com/img.jpg",
                    distanceMinutes = 30,
                    costPerPerson = BigDecimal("20.00"),
                )
            )

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).error).isInstanceOf(LadderError.NotCreator::class.java)
        }

        @Test
        fun `returns IllegalState when ladder is ACTIVE`() {
            val ladderId = UUID.randomUUID()
            val activityAId = UUID.randomUUID()
            val activityBId = UUID.randomUUID()
            fakeLadderClient.seed(
                clientLadder(
                    id = ladderId,
                    creatorId = creatorId,
                    status = ClientLadderStatus.ACTIVE,
                    currentRoundNumber = 1,
                    currentMatchActivityAId = activityAId,
                    currentMatchActivityBId = activityBId,
                )
            )

            val result = service.addActivity(
                AddActivityParam(
                    ladderId = ladderId,
                    requestingUserId = creatorId,
                    name = "Activity",
                    imageUrl = "http://img.com/img.jpg",
                    distanceMinutes = 30,
                    costPerPerson = BigDecimal("20.00"),
                )
            )

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(LadderError.IllegalState::class.java)
            assertThat((error as LadderError.IllegalState).expected).isEqualTo("DRAFT")
        }

        @Test
        fun `returns NotFound for non-existent ladder`() {
            val result = service.addActivity(
                AddActivityParam(
                    ladderId = UUID.randomUUID(),
                    requestingUserId = creatorId,
                    name = "Activity",
                    imageUrl = "http://img.com/img.jpg",
                    distanceMinutes = 30,
                    costPerPerson = BigDecimal("20.00"),
                )
            )

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).error).isInstanceOf(LadderError.NotFound::class.java)
        }
    }

    @Nested
    inner class RemoveActivity {

        @Test
        fun `removes activity from DRAFT ladder`() {
            val ladderId = UUID.randomUUID()
            val activityId = UUID.randomUUID()
            fakeLadderClient.seed(clientLadder(id = ladderId, creatorId = creatorId))
            fakeLadderClient.seedActivities(clientActivity(id = activityId, ladderId = ladderId))

            val result = service.removeActivity(
                RemoveActivityParam(ladderId = ladderId, activityId = activityId, requestingUserId = creatorId)
            )

            assertThat(result).isInstanceOf(Result.Success::class.java)
        }

        @Test
        fun `returns NotCreator when requester is not the creator`() {
            val ladderId = UUID.randomUUID()
            val activityId = UUID.randomUUID()
            fakeLadderClient.seed(clientLadder(id = ladderId, creatorId = creatorId))
            fakeLadderClient.seedActivities(clientActivity(id = activityId, ladderId = ladderId))

            val result = service.removeActivity(
                RemoveActivityParam(ladderId = ladderId, activityId = activityId, requestingUserId = otherUserId)
            )

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).error).isInstanceOf(LadderError.NotCreator::class.java)
        }

        @Test
        fun `returns IllegalState when ladder is ACTIVE`() {
            val ladderId = UUID.randomUUID()
            val activityAId = UUID.randomUUID()
            val activityBId = UUID.randomUUID()
            fakeLadderClient.seed(
                clientLadder(
                    id = ladderId,
                    creatorId = creatorId,
                    status = ClientLadderStatus.ACTIVE,
                    currentRoundNumber = 1,
                    currentMatchActivityAId = activityAId,
                    currentMatchActivityBId = activityBId,
                )
            )
            fakeLadderClient.seedActivities(clientActivity(id = activityAId, ladderId = ladderId))

            val result = service.removeActivity(
                RemoveActivityParam(ladderId = ladderId, activityId = activityAId, requestingUserId = creatorId)
            )

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).error).isInstanceOf(LadderError.IllegalState::class.java)
        }

        @Test
        fun `returns NotFound when activity does not exist`() {
            val ladderId = UUID.randomUUID()
            fakeLadderClient.seed(clientLadder(id = ladderId, creatorId = creatorId))

            val result = service.removeActivity(
                RemoveActivityParam(ladderId = ladderId, activityId = UUID.randomUUID(), requestingUserId = creatorId)
            )

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).error).isInstanceOf(LadderError.NotFound::class.java)
        }
    }

    @Nested
    inner class StartLadder {

        @Test
        fun `starts DRAFT ladder and transitions to ACTIVE with selected pair`() {
            val ladderId = UUID.randomUUID()
            fakeLadderClient.seed(clientLadder(id = ladderId, creatorId = creatorId))
            fakeLadderClient.seedActivities(
                clientActivity(id = UUID.randomUUID(), ladderId = ladderId, name = "Surfing", displayOrder = 1),
                clientActivity(id = UUID.randomUUID(), ladderId = ladderId, name = "Kayaking", displayOrder = 2),
            )

            val result = service.start(
                StartLadderParam(
                    ladderId = ladderId,
                    requestingUserId = creatorId,
                    presentUserIds = setOf(userId1, userId2),
                )
            )

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val detail = (result as Result.Success).value
            assertThat(detail.ladder.status).isEqualTo(LadderStatus.ACTIVE)
            assertThat(detail.participants).hasSize(2)
            assertThat(detail.participants.map { it.userId }).containsExactlyInAnyOrder(userId1, userId2)
        }

        @Test
        fun `returns NotCreator when requester is not the creator`() {
            val ladderId = UUID.randomUUID()
            fakeLadderClient.seed(clientLadder(id = ladderId, creatorId = creatorId))
            fakeLadderClient.seedActivities(
                clientActivity(ladderId = ladderId, displayOrder = 1),
                clientActivity(ladderId = ladderId, displayOrder = 2),
            )

            val result = service.start(
                StartLadderParam(ladderId = ladderId, requestingUserId = otherUserId, presentUserIds = setOf(userId1))
            )

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).error).isInstanceOf(LadderError.NotCreator::class.java)
        }

        @Test
        fun `returns IllegalState when ladder is already ACTIVE`() {
            val ladderId = UUID.randomUUID()
            val activityAId = UUID.randomUUID()
            val activityBId = UUID.randomUUID()
            fakeLadderClient.seed(
                clientLadder(
                    id = ladderId,
                    creatorId = creatorId,
                    status = ClientLadderStatus.ACTIVE,
                    currentRoundNumber = 1,
                    currentMatchActivityAId = activityAId,
                    currentMatchActivityBId = activityBId,
                )
            )

            val result = service.start(
                StartLadderParam(ladderId = ladderId, requestingUserId = creatorId, presentUserIds = setOf(userId1))
            )

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(LadderError.IllegalState::class.java)
            assertThat((error as LadderError.IllegalState).expected).isEqualTo("DRAFT")
        }

        @Test
        fun `returns NoPresentUsers when presentUserIds is empty`() {
            val ladderId = UUID.randomUUID()
            fakeLadderClient.seed(clientLadder(id = ladderId, creatorId = creatorId))

            val result = service.start(
                StartLadderParam(ladderId = ladderId, requestingUserId = creatorId, presentUserIds = emptySet())
            )

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).error).isInstanceOf(LadderError.NoPresentUsers::class.java)
        }

        @Test
        fun `returns NotEnoughActivities when fewer than 2 activities exist`() {
            val ladderId = UUID.randomUUID()
            fakeLadderClient.seed(clientLadder(id = ladderId, creatorId = creatorId))
            fakeLadderClient.seedActivities(clientActivity(ladderId = ladderId))

            val result = service.start(
                StartLadderParam(ladderId = ladderId, requestingUserId = creatorId, presentUserIds = setOf(userId1))
            )

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).error).isInstanceOf(LadderError.NotEnoughActivities::class.java)
        }

        @Test
        fun `returns NotFound for non-existent ladder`() {
            val result = service.start(
                StartLadderParam(ladderId = UUID.randomUUID(), requestingUserId = creatorId, presentUserIds = setOf(userId1))
            )

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).error).isInstanceOf(LadderError.NotFound::class.java)
        }
    }

    @Nested
    inner class CastVote {

        private fun activeLadder(id: UUID, activityAId: UUID, activityBId: UUID) = clientLadder(
            id = id,
            creatorId = creatorId,
            status = ClientLadderStatus.ACTIVE,
            currentRoundNumber = 1,
            currentMatchActivityAId = activityAId,
            currentMatchActivityBId = activityBId,
        )

        @Test
        fun `returns VoteRecorded when more voters remain`() {
            val ladderId = UUID.randomUUID()
            val activityAId = UUID.randomUUID()
            val activityBId = UUID.randomUUID()
            fakeLadderClient.seed(activeLadder(ladderId, activityAId, activityBId))
            fakeLadderClient.seedActivities(
                clientActivity(id = activityAId, ladderId = ladderId, name = "A", displayOrder = 1),
                clientActivity(id = activityBId, ladderId = ladderId, name = "B", displayOrder = 2),
            )
            // 2 participants: userId1 votes first, userId2 still pending
            fakeLadderClient.seedParticipants(
                clientParticipant(ladderId, userId1),
                clientParticipant(ladderId, userId2),
            )

            val result = service.castVote(
                CastVoteParam(ladderId = ladderId, requestingUserId = userId1, votedForActivityId = activityAId)
            )

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val outcome = (result as Result.Success).value
            assertThat(outcome).isInstanceOf(VoteOutcome.VoteRecorded::class.java)
            val recorded = outcome as VoteOutcome.VoteRecorded
            assertThat(recorded.voteCount).isEqualTo(1)
            assertThat(recorded.votersRemaining).isEqualTo(1)
            assertThat(recorded.voterId).isEqualTo(userId1)
        }

        @Test
        fun `resolves round when last voter casts final vote — returns RoundDecided`() {
            val ladderId = UUID.randomUUID()
            val activityAId = UUID.randomUUID()
            val activityBId = UUID.randomUUID()
            val activityCId = UUID.randomUUID()
            fakeLadderClient.seed(activeLadder(ladderId, activityAId, activityBId))
            fakeLadderClient.seedActivities(
                clientActivity(id = activityAId, ladderId = ladderId, name = "A", displayOrder = 1),
                clientActivity(id = activityBId, ladderId = ladderId, name = "B", displayOrder = 2),
                clientActivity(id = activityCId, ladderId = ladderId, name = "C", displayOrder = 3),
            )
            // 2 participants: userId1 already voted for A, userId2 is the last voter
            fakeLadderClient.seedParticipants(
                clientParticipant(ladderId, userId1),
                clientParticipant(ladderId, userId2),
            )
            fakeLadderClient.seedVotes(clientVote(ladderId, 1, userId1, activityAId))

            val result = service.castVote(
                CastVoteParam(ladderId = ladderId, requestingUserId = userId2, votedForActivityId = activityAId)
            )

            // A wins (2 vs 0), B gets 1st loss → LOSERS; round advances
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val outcome = (result as Result.Success).value
            assertThat(outcome).isInstanceOf(VoteOutcome.RoundDecided::class.java)
            val decided = outcome as VoteOutcome.RoundDecided
            assertThat(decided.winnerActivityId).isEqualTo(activityAId)
            assertThat(decided.newRoundNumber).isEqualTo(2)
            assertThat(decided.isFinalRound).isFalse()
            // Next pair selected from WINNERS bracket: A and C
            assertThat(setOf(decided.nextMatchAId, decided.nextMatchBId))
                .isSubsetOf(setOf(activityAId, activityCId))
        }

        @Test
        fun `completes ladder when last activity is eliminated`() {
            val ladderId = UUID.randomUUID()
            val activityAId = UUID.randomUUID()
            val activityBId = UUID.randomUUID()
            // B has 1 loss (LOSERS), so a 2nd loss → ELIMINATED → LadderCompleted
            fakeLadderClient.seed(
                clientLadder(
                    id = ladderId,
                    creatorId = creatorId,
                    status = ClientLadderStatus.ACTIVE,
                    currentRoundNumber = 2,
                    currentMatchActivityAId = activityAId,
                    currentMatchActivityBId = activityBId,
                    isFinalRound = true,
                )
            )
            fakeLadderClient.seedActivities(
                clientActivity(id = activityAId, ladderId = ladderId, name = "A", losses = 0,
                    bracket = ClientLadderBracket.WINNERS, displayOrder = 1),
                clientActivity(id = activityBId, ladderId = ladderId, name = "B", losses = 1,
                    bracket = ClientLadderBracket.LOSERS, displayOrder = 2),
            )
            fakeLadderClient.seedParticipants(clientParticipant(ladderId, userId1))

            val result = service.castVote(
                CastVoteParam(ladderId = ladderId, requestingUserId = userId1, votedForActivityId = activityAId)
            )

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val outcome = (result as Result.Success).value
            assertThat(outcome).isInstanceOf(VoteOutcome.LadderCompleted::class.java)
            assertThat((outcome as VoteOutcome.LadderCompleted).winnerActivityId).isEqualTo(activityAId)
        }

        @Test
        fun `returns IllegalState when ladder is not ACTIVE`() {
            val ladderId = UUID.randomUUID()
            fakeLadderClient.seed(clientLadder(id = ladderId, creatorId = creatorId, status = ClientLadderStatus.DRAFT))

            val result = service.castVote(
                CastVoteParam(ladderId = ladderId, requestingUserId = userId1, votedForActivityId = UUID.randomUUID())
            )

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(LadderError.IllegalState::class.java)
            assertThat((error as LadderError.IllegalState).expected).isEqualTo("ACTIVE")
        }

        @Test
        fun `returns NotEligibleVoter when user is not a participant`() {
            val ladderId = UUID.randomUUID()
            val activityAId = UUID.randomUUID()
            val activityBId = UUID.randomUUID()
            fakeLadderClient.seed(activeLadder(ladderId, activityAId, activityBId))
            fakeLadderClient.seedParticipants(clientParticipant(ladderId, userId1))

            val result = service.castVote(
                CastVoteParam(ladderId = ladderId, requestingUserId = otherUserId, votedForActivityId = activityAId)
            )

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).error).isInstanceOf(LadderError.NotEligibleVoter::class.java)
        }

        @Test
        fun `returns InvalidVoteTarget when voting for activity not in current match`() {
            val ladderId = UUID.randomUUID()
            val activityAId = UUID.randomUUID()
            val activityBId = UUID.randomUUID()
            val unrelatedActivityId = UUID.randomUUID()
            fakeLadderClient.seed(activeLadder(ladderId, activityAId, activityBId))
            fakeLadderClient.seedParticipants(clientParticipant(ladderId, userId1))

            val result = service.castVote(
                CastVoteParam(ladderId = ladderId, requestingUserId = userId1, votedForActivityId = unrelatedActivityId)
            )

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).error).isInstanceOf(LadderError.InvalidVoteTarget::class.java)
        }

        @Test
        fun `resolves round as tied when last vote produces equal counts — returns RoundTied`() {
            val ladderId = UUID.randomUUID()
            val activityAId = UUID.randomUUID()
            val activityBId = UUID.randomUUID()
            // Only 2 activities — both WINNERS, so after tie the re-selected pair is still A and B
            fakeLadderClient.seed(activeLadder(ladderId, activityAId, activityBId))
            fakeLadderClient.seedActivities(
                clientActivity(id = activityAId, ladderId = ladderId, name = "A", displayOrder = 1),
                clientActivity(id = activityBId, ladderId = ladderId, name = "B", displayOrder = 2),
            )
            // 2 participants: userId1 already voted for A; userId2 votes for B → 1 each → tie
            fakeLadderClient.seedParticipants(
                clientParticipant(ladderId, userId1),
                clientParticipant(ladderId, userId2),
            )
            fakeLadderClient.seedVotes(clientVote(ladderId, 1, userId1, activityAId))

            val result = service.castVote(
                CastVoteParam(ladderId = ladderId, requestingUserId = userId2, votedForActivityId = activityBId)
            )

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val outcome = (result as Result.Success).value
            assertThat(outcome).isInstanceOf(VoteOutcome.RoundTied::class.java)
            val tied = outcome as VoteOutcome.RoundTied
            assertThat(tied.voteCount).isEqualTo(2)
            assertThat(tied.newRoundNumber).isEqualTo(2)
            // Neither activity should have taken a loss
            assertThat(tied.isFinalRound).isFalse()
            // New pair drawn from the two non-eliminated activities
            assertThat(setOf(tied.nextMatchAId, tied.nextMatchBId))
                .containsExactlyInAnyOrder(activityAId, activityBId)
        }

        @Test
        fun `returns AlreadyVoted when user already voted this round`() {
            val ladderId = UUID.randomUUID()
            val activityAId = UUID.randomUUID()
            val activityBId = UUID.randomUUID()
            fakeLadderClient.seed(activeLadder(ladderId, activityAId, activityBId))
            fakeLadderClient.seedParticipants(
                clientParticipant(ladderId, userId1),
                clientParticipant(ladderId, userId2),
            )
            fakeLadderClient.seedVotes(clientVote(ladderId, 1, userId1, activityAId))

            val result = service.castVote(
                CastVoteParam(ladderId = ladderId, requestingUserId = userId1, votedForActivityId = activityBId)
            )

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).error).isInstanceOf(LadderError.AlreadyVoted::class.java)
        }

        @Test
        fun `returns NotFound for non-existent ladder`() {
            val result = service.castVote(
                CastVoteParam(ladderId = UUID.randomUUID(), requestingUserId = userId1, votedForActivityId = UUID.randomUUID())
            )

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).error).isInstanceOf(LadderError.NotFound::class.java)
        }
    }

    @Nested
    inner class RestartLadder {

        @Test
        fun `restarts ACTIVE ladder back to DRAFT`() {
            val ladderId = UUID.randomUUID()
            val activityAId = UUID.randomUUID()
            val activityBId = UUID.randomUUID()
            fakeLadderClient.seed(
                clientLadder(
                    id = ladderId,
                    creatorId = creatorId,
                    status = ClientLadderStatus.ACTIVE,
                    currentRoundNumber = 3,
                    currentMatchActivityAId = activityAId,
                    currentMatchActivityBId = activityBId,
                )
            )
            fakeLadderClient.seedActivities(
                clientActivity(id = activityAId, ladderId = ladderId, losses = 0, bracket = ClientLadderBracket.WINNERS),
                clientActivity(id = activityBId, ladderId = ladderId, losses = 1, bracket = ClientLadderBracket.LOSERS),
            )
            fakeLadderClient.seedParticipants(clientParticipant(ladderId, userId1))
            fakeLadderClient.seedVotes(clientVote(ladderId, 1, userId1, activityAId))

            val result = service.restart(RestartLadderParam(ladderId = ladderId, requestingUserId = creatorId))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val ladder = (result as Result.Success).value
            assertThat(ladder.status).isEqualTo(LadderStatus.DRAFT)
            assertThat(ladder.currentRoundNumber).isNull()
            assertThat(ladder.winnerActivityId).isNull()
        }

        @Test
        fun `restarts COMPLETED ladder back to DRAFT and clears winner`() {
            val ladderId = UUID.randomUUID()
            val winnerActivityId = UUID.randomUUID()
            fakeLadderClient.seed(
                clientLadder(
                    id = ladderId,
                    creatorId = creatorId,
                    status = ClientLadderStatus.COMPLETED,
                    winnerActivityId = winnerActivityId,
                )
            )

            val result = service.restart(RestartLadderParam(ladderId = ladderId, requestingUserId = creatorId))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val ladder = (result as Result.Success).value
            assertThat(ladder.status).isEqualTo(LadderStatus.DRAFT)
            assertThat(ladder.winnerActivityId).isNull()
        }

        @Test
        fun `returns IllegalState when ladder is already DRAFT`() {
            val ladderId = UUID.randomUUID()
            fakeLadderClient.seed(clientLadder(id = ladderId, creatorId = creatorId, status = ClientLadderStatus.DRAFT))

            val result = service.restart(RestartLadderParam(ladderId = ladderId, requestingUserId = creatorId))

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(LadderError.IllegalState::class.java)
            assertThat((error as LadderError.IllegalState).expected).isEqualTo("ACTIVE or COMPLETED")
        }

        @Test
        fun `returns NotCreator when requester is not the creator`() {
            val ladderId = UUID.randomUUID()
            fakeLadderClient.seed(
                clientLadder(id = ladderId, creatorId = creatorId, status = ClientLadderStatus.ACTIVE,
                    currentRoundNumber = 1, currentMatchActivityAId = UUID.randomUUID(), currentMatchActivityBId = UUID.randomUUID())
            )

            val result = service.restart(RestartLadderParam(ladderId = ladderId, requestingUserId = otherUserId))

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).error).isInstanceOf(LadderError.NotCreator::class.java)
        }

        @Test
        fun `returns NotFound for non-existent ladder`() {
            val result = service.restart(RestartLadderParam(ladderId = UUID.randomUUID(), requestingUserId = creatorId))

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).error).isInstanceOf(LadderError.NotFound::class.java)
        }
    }
}
