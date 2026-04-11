package com.acme.services.camperservice.features.activityladder.actions.roundresolution

import com.acme.clients.activityladderclient.fake.FakeActivityLadderClient
import com.acme.clients.activityladderclient.model.Ladder as ClientLadder
import com.acme.clients.activityladderclient.model.LadderActivity as ClientLadderActivity
import com.acme.clients.activityladderclient.model.LadderBracket as ClientLadderBracket
import com.acme.clients.activityladderclient.model.LadderStatus as ClientLadderStatus
import com.acme.clients.activityladderclient.model.LadderVote as ClientLadderVote
import com.acme.services.camperservice.features.activityladder.model.Ladder
import com.acme.services.camperservice.features.activityladder.model.LadderBracket
import com.acme.services.camperservice.features.activityladder.model.LadderStatus
import com.acme.services.camperservice.features.activityladder.model.VoteOutcome
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Unit tests for [RoundResolver].
 *
 * RoundResolver takes a [FakeActivityLadderClient] and a service-layer [Ladder] snapshot,
 * fetches votes and activities from the client, applies bracket logic, and returns a [VoteOutcome].
 *
 * Note: [RoundResolver] uses [kotlin.random.Random.Default] for pair selection, so tests
 * assert that the selected pair is drawn from the expected bracket — not the exact pair.
 */
class RoundResolverTest {

    private val fake = FakeActivityLadderClient()
    private val resolver = RoundResolver(fake)
    private val now = Instant.now()

    private val creatorId = UUID.fromString("dd000000-0001-4000-8000-000000000001")

    /**
     * Build a service-layer [Ladder] snapshot matching a seeded client ladder.
     * The [RoundResolver] extracts `id`, `currentMatchActivityAId`, `currentMatchActivityBId`,
     * `isFinalRound`, and `isGrandFinalReset` from this snapshot.
     */
    private fun serviceLadder(
        id: UUID,
        activityAId: UUID,
        activityBId: UUID,
        isFinalRound: Boolean = false,
        isGrandFinalReset: Boolean = false,
    ) = Ladder(
        id = id,
        creatorId = creatorId,
        title = "Test Ladder",
        status = LadderStatus.ACTIVE,
        currentRoundNumber = 1,
        currentMatchActivityAId = activityAId,
        currentMatchActivityBId = activityBId,
        isFinalRound = isFinalRound,
        isGrandFinalReset = isGrandFinalReset,
        winnerActivityId = null,
        createdAt = now,
        updatedAt = now,
    )

    private fun clientLadder(id: UUID) = ClientLadder(
        id = id,
        creatorId = creatorId,
        title = "Test Ladder",
        status = ClientLadderStatus.ACTIVE,
        currentRoundNumber = 1,
        currentMatchActivityAId = null,
        currentMatchActivityBId = null,
        isFinalRound = false,
        isGrandFinalReset = false,
        winnerActivityId = null,
        createdAt = now,
        updatedAt = now,
    )

    private fun clientActivity(
        id: UUID,
        ladderId: UUID,
        name: String,
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

    private fun vote(ladderId: UUID, roundNumber: Int, userId: UUID, activityId: UUID) = ClientLadderVote(
        id = UUID.randomUUID(),
        ladderId = ladderId,
        roundNumber = roundNumber,
        userId = userId,
        votedForActivityId = activityId,
        createdAt = now,
    )

    @BeforeEach
    fun setUp() {
        fake.reset()
    }

    @Nested
    inner class StandardRoundDecided {

        @Test
        fun `loser gets first loss and moves to LOSERS bracket, winner proceeds`() {
            val ladderId = UUID.randomUUID()
            val activityAId = UUID.randomUUID()
            val activityBId = UUID.randomUUID()
            val activityCId = UUID.randomUUID()

            fake.seed(clientLadder(ladderId))
            fake.seedActivities(
                clientActivity(activityAId, ladderId, "A", losses = 0, bracket = ClientLadderBracket.WINNERS, displayOrder = 1),
                clientActivity(activityBId, ladderId, "B", losses = 0, bracket = ClientLadderBracket.WINNERS, displayOrder = 2),
                clientActivity(activityCId, ladderId, "C", losses = 0, bracket = ClientLadderBracket.WINNERS, displayOrder = 3),
            )
            // A wins 2 to 1
            fake.seedVotes(
                vote(ladderId, 1, UUID.randomUUID(), activityAId),
                vote(ladderId, 1, UUID.randomUUID(), activityAId),
                vote(ladderId, 1, UUID.randomUUID(), activityBId),
            )

            val ladder = serviceLadder(ladderId, activityAId, activityBId)
            val outcome = resolver.resolve(ladder, 1)

            assertThat(outcome).isInstanceOf(VoteOutcome.RoundDecided::class.java)
            val decided = outcome as VoteOutcome.RoundDecided
            assertThat(decided.winnerActivityId).isEqualTo(activityAId)
            assertThat(decided.newRoundNumber).isEqualTo(2)
            assertThat(decided.isFinalRound).isFalse()
            assertThat(decided.isGrandFinalReset).isFalse()
            // Next pair must come from WINNERS bracket (A and C)
            assertThat(setOf(decided.nextMatchAId, decided.nextMatchBId))
                .isSubsetOf(setOf(activityAId, activityCId))
                .hasSize(2)
            // Vote totals include both activities
            assertThat(decided.voteTotals[activityAId]).isEqualTo(2)
            assertThat(decided.voteTotals[activityBId]).isEqualTo(1)
        }

        @Test
        fun `loser with 1 loss voted out again gets ELIMINATED`() {
            // Activity B already has 1 loss (in LOSERS), loses again → ELIMINATED
            // Activity A has no losses (WINNERS)
            // After: only A remains → LadderCompleted
            val ladderId = UUID.randomUUID()
            val activityAId = UUID.randomUUID()
            val activityBId = UUID.randomUUID()

            fake.seed(clientLadder(ladderId))
            fake.seedActivities(
                clientActivity(activityAId, ladderId, "A", losses = 0, bracket = ClientLadderBracket.WINNERS, displayOrder = 1),
                clientActivity(activityBId, ladderId, "B", losses = 1, bracket = ClientLadderBracket.LOSERS, displayOrder = 2),
            )
            fake.seedVotes(vote(ladderId, 2, UUID.randomUUID(), activityAId))

            val ladder = serviceLadder(ladderId, activityAId, activityBId, isFinalRound = true)
            val outcome = resolver.resolve(ladder, 2)

            assertThat(outcome).isInstanceOf(VoteOutcome.LadderCompleted::class.java)
            val completed = outcome as VoteOutcome.LadderCompleted
            assertThat(completed.winnerActivityId).isEqualTo(activityAId)
            assertThat(completed.voteTotals[activityAId]).isEqualTo(1)
        }

        @Test
        fun `picks from LOSERS bracket when WINNERS has fewer than 2`() {
            // After deciding: WINNERS=[A] (size=1), LOSERS=[B, C] (size=2)
            // Standard selection: pick from LOSERS (size≥2)
            val ladderId = UUID.randomUUID()
            val activityAId = UUID.randomUUID()
            val activityBId = UUID.randomUUID()
            val activityCId = UUID.randomUUID()

            fake.seed(clientLadder(ladderId))
            fake.seedActivities(
                clientActivity(activityAId, ladderId, "A", losses = 0, bracket = ClientLadderBracket.WINNERS, displayOrder = 1),
                clientActivity(activityBId, ladderId, "B", losses = 0, bracket = ClientLadderBracket.WINNERS, displayOrder = 2),
                clientActivity(activityCId, ladderId, "C", losses = 1, bracket = ClientLadderBracket.LOSERS, displayOrder = 3),
            )
            // A wins, B → LOSERS (1st loss). After: WINNERS=[A], LOSERS=[B,C]
            fake.seedVotes(vote(ladderId, 1, UUID.randomUUID(), activityAId))

            val ladder = serviceLadder(ladderId, activityAId, activityBId)
            val outcome = resolver.resolve(ladder, 1)

            assertThat(outcome).isInstanceOf(VoteOutcome.RoundDecided::class.java)
            val decided = outcome as VoteOutcome.RoundDecided
            assertThat(decided.winnerActivityId).isEqualTo(activityAId)
            // Next pair from LOSERS=[B,C]
            assertThat(setOf(decided.nextMatchAId, decided.nextMatchBId))
                .isSubsetOf(setOf(activityBId, activityCId))
                .hasSize(2)
        }
    }

    @Nested
    inner class GrandFinalTrigger {

        @Test
        fun `triggers Grand Final when exactly 1 WINNERS and 1 LOSERS remain`() {
            // After B loses 1st loss: WINNERS=[A], LOSERS=[B] → Grand Final
            val ladderId = UUID.randomUUID()
            val activityAId = UUID.randomUUID()
            val activityBId = UUID.randomUUID()

            fake.seed(clientLadder(ladderId))
            fake.seedActivities(
                clientActivity(activityAId, ladderId, "A", losses = 0, bracket = ClientLadderBracket.WINNERS, displayOrder = 1),
                clientActivity(activityBId, ladderId, "B", losses = 0, bracket = ClientLadderBracket.WINNERS, displayOrder = 2),
            )
            fake.seedVotes(vote(ladderId, 1, UUID.randomUUID(), activityAId))

            val ladder = serviceLadder(ladderId, activityAId, activityBId, isFinalRound = false)
            val outcome = resolver.resolve(ladder, 1)

            assertThat(outcome).isInstanceOf(VoteOutcome.RoundDecided::class.java)
            val decided = outcome as VoteOutcome.RoundDecided
            assertThat(decided.winnerActivityId).isEqualTo(activityAId)
            assertThat(decided.isFinalRound).isTrue()
            assertThat(decided.isGrandFinalReset).isFalse()
            assertThat(decided.nextMatchAId).isEqualTo(activityAId)
            assertThat(decided.nextMatchBId).isEqualTo(activityBId)
        }

        @Test
        fun `Grand Final winner from WINNERS bracket ends tournament immediately`() {
            // A(WINNERS,0) beats B(LOSERS,1) in Grand Final → B gets 2nd loss → ELIMINATED → LadderCompleted
            val ladderId = UUID.randomUUID()
            val activityAId = UUID.randomUUID()
            val activityBId = UUID.randomUUID()

            fake.seed(clientLadder(ladderId))
            fake.seedActivities(
                clientActivity(activityAId, ladderId, "A", losses = 0, bracket = ClientLadderBracket.WINNERS, displayOrder = 1),
                clientActivity(activityBId, ladderId, "B", losses = 1, bracket = ClientLadderBracket.LOSERS, displayOrder = 2),
            )
            fake.seedVotes(vote(ladderId, 3, UUID.randomUUID(), activityAId))

            val ladder = serviceLadder(ladderId, activityAId, activityBId, isFinalRound = true)
            val outcome = resolver.resolve(ladder, 3)

            assertThat(outcome).isInstanceOf(VoteOutcome.LadderCompleted::class.java)
            assertThat((outcome as VoteOutcome.LadderCompleted).winnerActivityId).isEqualTo(activityAId)
        }

        @Test
        fun `LOSERS finalist wins Grand Final — triggers Grand Final Reset, both activities in LOSERS`() {
            // B(LOSERS, 1 loss) beats A(WINNERS, 0 losses) in the first Grand Final.
            // A gets its first loss and drops to LOSERS. Both finalists now have 1 loss each.
            // This should trigger Grand Final Reset (isGrandFinalReset=true) rather than ending the tournament.
            val ladderId = UUID.randomUUID()
            val activityAId = UUID.randomUUID()
            val activityBId = UUID.randomUUID()

            fake.seed(clientLadder(ladderId))
            fake.seedActivities(
                clientActivity(activityAId, ladderId, "A", losses = 0, bracket = ClientLadderBracket.WINNERS, displayOrder = 1),
                clientActivity(activityBId, ladderId, "B", losses = 1, bracket = ClientLadderBracket.LOSERS, displayOrder = 2),
            )
            // B wins: 2 votes for B, 1 for A
            fake.seedVotes(
                vote(ladderId, 3, UUID.randomUUID(), activityBId),
                vote(ladderId, 3, UUID.randomUUID(), activityBId),
                vote(ladderId, 3, UUID.randomUUID(), activityAId),
            )

            val ladder = serviceLadder(ladderId, activityAId, activityBId, isFinalRound = true, isGrandFinalReset = false)
            val outcome = resolver.resolve(ladder, 3)

            // NOT completed — A still has only 1 loss; Grand Final Reset must be played
            assertThat(outcome).isInstanceOf(VoteOutcome.RoundDecided::class.java)
            val decided = outcome as VoteOutcome.RoundDecided
            assertThat(decided.winnerActivityId).isEqualTo(activityBId)
            assertThat(decided.isFinalRound).isTrue()
            assertThat(decided.isGrandFinalReset).isTrue()
            // Same two activities play the reset
            assertThat(setOf(decided.nextMatchAId, decided.nextMatchBId))
                .containsExactlyInAnyOrder(activityAId, activityBId)
            assertThat(decided.newRoundNumber).isEqualTo(4)

            // A now has 1 loss and is in LOSERS bracket
            val activities = fake.getActivities(
                com.acme.clients.activityladderclient.api.GetLadderActivitiesParam(ladderId)
            )
            val aAfter = (activities as com.acme.clients.common.Result.Success).value.first { it.id == activityAId }
            assertThat(aAfter.losses).isEqualTo(1)
            assertThat(aAfter.bracket).isEqualTo(ClientLadderBracket.LOSERS)
        }

        @Test
        fun `Grand Final Reset winner completes the tournament`() {
            // Both finalists have 1 loss each (state produced by Grand Final Reset trigger).
            // Whoever wins this round gets the other eliminated → LadderCompleted.
            val ladderId = UUID.randomUUID()
            val activityAId = UUID.randomUUID()
            val activityBId = UUID.randomUUID()

            fake.seed(clientLadder(ladderId))
            fake.seedActivities(
                clientActivity(activityAId, ladderId, "A", losses = 1, bracket = ClientLadderBracket.LOSERS, displayOrder = 1),
                clientActivity(activityBId, ladderId, "B", losses = 1, bracket = ClientLadderBracket.LOSERS, displayOrder = 2),
            )
            // A wins the reset
            fake.seedVotes(
                vote(ladderId, 4, UUID.randomUUID(), activityAId),
                vote(ladderId, 4, UUID.randomUUID(), activityAId),
            )

            val ladder = serviceLadder(ladderId, activityAId, activityBId, isFinalRound = true, isGrandFinalReset = true)
            val outcome = resolver.resolve(ladder, 4)

            assertThat(outcome).isInstanceOf(VoteOutcome.LadderCompleted::class.java)
            val completed = outcome as VoteOutcome.LadderCompleted
            assertThat(completed.winnerActivityId).isEqualTo(activityAId)

            // B eliminated: 2 losses
            val activities = fake.getActivities(
                com.acme.clients.activityladderclient.api.GetLadderActivitiesParam(ladderId)
            )
            val bAfter = (activities as com.acme.clients.common.Result.Success).value.first { it.id == activityBId }
            assertThat(bAfter.losses).isEqualTo(2)
            assertThat(bAfter.bracket).isEqualTo(ClientLadderBracket.ELIMINATED)
        }
    }

    @Nested
    inner class TieHandling {

        @Test
        fun `tie increments round and re-selects pair from non-eliminated`() {
            val ladderId = UUID.randomUUID()
            val activityAId = UUID.randomUUID()
            val activityBId = UUID.randomUUID()
            val activityCId = UUID.randomUUID()

            fake.seed(clientLadder(ladderId))
            fake.seedActivities(
                clientActivity(activityAId, ladderId, "A", losses = 0, bracket = ClientLadderBracket.WINNERS, displayOrder = 1),
                clientActivity(activityBId, ladderId, "B", losses = 0, bracket = ClientLadderBracket.WINNERS, displayOrder = 2),
                clientActivity(activityCId, ladderId, "C", losses = 0, bracket = ClientLadderBracket.WINNERS, displayOrder = 3),
            )
            // Tie: 1 vote each
            fake.seedVotes(
                vote(ladderId, 1, UUID.randomUUID(), activityAId),
                vote(ladderId, 1, UUID.randomUUID(), activityBId),
            )

            val ladder = serviceLadder(ladderId, activityAId, activityBId)
            val outcome = resolver.resolve(ladder, 1)

            assertThat(outcome).isInstanceOf(VoteOutcome.RoundTied::class.java)
            val tied = outcome as VoteOutcome.RoundTied
            assertThat(tied.voteCount).isEqualTo(2)
            assertThat(tied.newRoundNumber).isEqualTo(2)
            // Next pair must be two distinct activities from the non-eliminated pool
            assertThat(tied.nextMatchAId).isNotEqualTo(tied.nextMatchBId)
            assertThat(setOf(tied.nextMatchAId, tied.nextMatchBId))
                .isSubsetOf(setOf(activityAId, activityBId, activityCId))
                .hasSize(2)
        }

        @Test
        fun `tie preserves isFinalRound and isGrandFinalReset flags`() {
            val ladderId = UUID.randomUUID()
            val activityAId = UUID.randomUUID()
            val activityBId = UUID.randomUUID()

            fake.seed(clientLadder(ladderId))
            fake.seedActivities(
                clientActivity(activityAId, ladderId, "A", losses = 0, bracket = ClientLadderBracket.WINNERS, displayOrder = 1),
                clientActivity(activityBId, ladderId, "B", losses = 1, bracket = ClientLadderBracket.LOSERS, displayOrder = 2),
            )
            // Tie in Grand Final
            fake.seedVotes(
                vote(ladderId, 3, UUID.randomUUID(), activityAId),
                vote(ladderId, 3, UUID.randomUUID(), activityBId),
            )

            val ladder = serviceLadder(ladderId, activityAId, activityBId, isFinalRound = true, isGrandFinalReset = false)
            val outcome = resolver.resolve(ladder, 3)

            assertThat(outcome).isInstanceOf(VoteOutcome.RoundTied::class.java)
            val tied = outcome as VoteOutcome.RoundTied
            assertThat(tied.isFinalRound).isTrue()
            assertThat(tied.isGrandFinalReset).isFalse()
            assertThat(tied.newRoundNumber).isEqualTo(4)
        }

        @Test
        fun `zero votes counts as a tie — re-selects pair`() {
            val ladderId = UUID.randomUUID()
            val activityAId = UUID.randomUUID()
            val activityBId = UUID.randomUUID()

            fake.seed(clientLadder(ladderId))
            fake.seedActivities(
                clientActivity(activityAId, ladderId, "A", losses = 0, bracket = ClientLadderBracket.WINNERS, displayOrder = 1),
                clientActivity(activityBId, ladderId, "B", losses = 0, bracket = ClientLadderBracket.WINNERS, displayOrder = 2),
            )
            // No votes seeded → aCount=0, bCount=0 → tie

            val ladder = serviceLadder(ladderId, activityAId, activityBId)
            val outcome = resolver.resolve(ladder, 1)

            assertThat(outcome).isInstanceOf(VoteOutcome.RoundTied::class.java)
            val tied = outcome as VoteOutcome.RoundTied
            assertThat(tied.voteCount).isEqualTo(0)
            assertThat(tied.newRoundNumber).isEqualTo(2)
            assertThat(setOf(tied.nextMatchAId, tied.nextMatchBId))
                .isSubsetOf(setOf(activityAId, activityBId))
                .hasSize(2)
        }
    }

    @Nested
    inner class LadderCompleted {

        @Test
        fun `tournament completes when only one activity remains non-eliminated`() {
            val ladderId = UUID.randomUUID()
            val activityAId = UUID.randomUUID()
            val activityBId = UUID.randomUUID()

            fake.seed(clientLadder(ladderId))
            fake.seedActivities(
                clientActivity(activityAId, ladderId, "Winner", losses = 0, bracket = ClientLadderBracket.WINNERS, displayOrder = 1),
                clientActivity(activityBId, ladderId, "About to be eliminated", losses = 1, bracket = ClientLadderBracket.LOSERS, displayOrder = 2),
            )
            // A wins → B gets 2nd loss → ELIMINATED → only A remains
            fake.seedVotes(vote(ladderId, 4, UUID.randomUUID(), activityAId))

            val ladder = serviceLadder(ladderId, activityAId, activityBId, isFinalRound = true)
            val outcome = resolver.resolve(ladder, 4)

            assertThat(outcome).isInstanceOf(VoteOutcome.LadderCompleted::class.java)
            val completed = outcome as VoteOutcome.LadderCompleted
            assertThat(completed.winnerActivityId).isEqualTo(activityAId)
            assertThat(completed.voteTotals[activityAId]).isEqualTo(1)
            assertThat(completed.voteTotals[activityBId]).isEqualTo(0)
        }
    }

    @Nested
    inner class VoteTotals {

        @Test
        fun `vote totals reflect actual vote counts for both activities`() {
            val ladderId = UUID.randomUUID()
            val activityAId = UUID.randomUUID()
            val activityBId = UUID.randomUUID()
            val activityCId = UUID.randomUUID()

            fake.seed(clientLadder(ladderId))
            fake.seedActivities(
                clientActivity(activityAId, ladderId, "A", displayOrder = 1),
                clientActivity(activityBId, ladderId, "B", displayOrder = 2),
                clientActivity(activityCId, ladderId, "C", displayOrder = 3),
            )
            // 3 for A, 2 for B
            fake.seedVotes(
                vote(ladderId, 1, UUID.randomUUID(), activityAId),
                vote(ladderId, 1, UUID.randomUUID(), activityAId),
                vote(ladderId, 1, UUID.randomUUID(), activityAId),
                vote(ladderId, 1, UUID.randomUUID(), activityBId),
                vote(ladderId, 1, UUID.randomUUID(), activityBId),
            )

            val ladder = serviceLadder(ladderId, activityAId, activityBId)
            val outcome = resolver.resolve(ladder, 1)

            assertThat(outcome).isInstanceOf(VoteOutcome.RoundDecided::class.java)
            val decided = outcome as VoteOutcome.RoundDecided
            assertThat(decided.voteTotals[activityAId]).isEqualTo(3)
            assertThat(decided.voteTotals[activityBId]).isEqualTo(2)
        }
    }
}
