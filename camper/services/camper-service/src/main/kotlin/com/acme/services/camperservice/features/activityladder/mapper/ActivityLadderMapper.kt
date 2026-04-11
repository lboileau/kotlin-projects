package com.acme.services.camperservice.features.activityladder.mapper

import com.acme.services.camperservice.features.activityladder.dto.CurrentRoundView
import com.acme.services.camperservice.features.activityladder.dto.LadderActivityResponse
import com.acme.services.camperservice.features.activityladder.dto.LadderDetailResponse
import com.acme.services.camperservice.features.activityladder.dto.LadderParticipantResponse
import com.acme.services.camperservice.features.activityladder.dto.LadderSummaryResponse
import com.acme.services.camperservice.features.activityladder.model.EnrichedParticipant
import com.acme.services.camperservice.features.activityladder.model.LadderActivity
import com.acme.services.camperservice.features.activityladder.model.LadderBracket
import com.acme.services.camperservice.features.activityladder.model.LadderDetail
import com.acme.services.camperservice.features.activityladder.model.LadderStatus
import com.acme.services.camperservice.features.activityladder.model.LadderSummary
import com.acme.clients.activityladderclient.model.Ladder as ClientLadder
import com.acme.clients.activityladderclient.model.LadderActivity as ClientLadderActivity
import com.acme.clients.activityladderclient.model.LadderParticipant as ClientLadderParticipant
import com.acme.clients.activityladderclient.model.LadderStatus as ClientLadderStatus
import com.acme.clients.activityladderclient.model.LadderBracket as ClientLadderBracket
import com.acme.services.camperservice.features.activityladder.model.Ladder
import com.acme.services.camperservice.features.activityladder.model.LadderParticipant
import com.acme.services.camperservice.features.activityladder.model.LadderVote
import com.acme.clients.activityladderclient.model.LadderVote as ClientLadderVote

object ActivityLadderMapper {

    fun fromClient(clientLadder: ClientLadder): Ladder = Ladder(
        id = clientLadder.id,
        creatorId = clientLadder.creatorId,
        title = clientLadder.title,
        status = fromClientStatus(clientLadder.status),
        currentRoundNumber = clientLadder.currentRoundNumber,
        currentMatchActivityAId = clientLadder.currentMatchActivityAId,
        currentMatchActivityBId = clientLadder.currentMatchActivityBId,
        isFinalRound = clientLadder.isFinalRound,
        isGrandFinalReset = clientLadder.isGrandFinalReset,
        winnerActivityId = clientLadder.winnerActivityId,
        createdAt = clientLadder.createdAt,
        updatedAt = clientLadder.updatedAt,
    )

    fun fromClientActivity(clientActivity: ClientLadderActivity): LadderActivity = LadderActivity(
        id = clientActivity.id,
        ladderId = clientActivity.ladderId,
        name = clientActivity.name,
        imageUrl = clientActivity.imageUrl,
        distanceMinutes = clientActivity.distanceMinutes,
        costPerPerson = clientActivity.costPerPerson,
        losses = clientActivity.losses,
        bracket = fromClientBracket(clientActivity.bracket),
        displayOrder = clientActivity.displayOrder,
        createdAt = clientActivity.createdAt,
    )

    fun fromClientParticipant(clientParticipant: ClientLadderParticipant): LadderParticipant = LadderParticipant(
        id = clientParticipant.id,
        ladderId = clientParticipant.ladderId,
        userId = clientParticipant.userId,
        createdAt = clientParticipant.createdAt,
    )

    fun fromClientVote(clientVote: ClientLadderVote): LadderVote = LadderVote(
        id = clientVote.id,
        ladderId = clientVote.ladderId,
        roundNumber = clientVote.roundNumber,
        userId = clientVote.userId,
        votedForActivityId = clientVote.votedForActivityId,
        createdAt = clientVote.createdAt,
    )

    fun fromClientStatus(status: ClientLadderStatus): LadderStatus = when (status) {
        ClientLadderStatus.DRAFT -> LadderStatus.DRAFT
        ClientLadderStatus.ACTIVE -> LadderStatus.ACTIVE
        ClientLadderStatus.COMPLETED -> LadderStatus.COMPLETED
    }

    fun fromClientBracket(bracket: ClientLadderBracket): LadderBracket = when (bracket) {
        ClientLadderBracket.WINNERS -> LadderBracket.WINNERS
        ClientLadderBracket.LOSERS -> LadderBracket.LOSERS
        ClientLadderBracket.ELIMINATED -> LadderBracket.ELIMINATED
    }

    fun toSummaryResponse(summary: LadderSummary): LadderSummaryResponse = LadderSummaryResponse(
        id = summary.ladder.id,
        title = summary.ladder.title,
        status = summary.ladder.status.name,
        creatorId = summary.ladder.creatorId,
        activityCount = summary.activityCount,
        participantCount = summary.participantCount,
        winnerActivityId = summary.ladder.winnerActivityId,
        createdAt = summary.ladder.createdAt,
        updatedAt = summary.ladder.updatedAt,
    )

    fun toDetailResponse(detail: LadderDetail): LadderDetailResponse = LadderDetailResponse(
        id = detail.ladder.id,
        title = detail.ladder.title,
        status = detail.ladder.status.name,
        creatorId = detail.ladder.creatorId,
        activities = detail.activities.map { toActivityResponse(it) },
        participants = detail.participants.map { toParticipantResponse(it) },
        currentRound = detail.currentRound?.let { cr ->
            CurrentRoundView(
                roundNumber = cr.roundNumber,
                activityAId = cr.activityAId,
                activityBId = cr.activityBId,
                votesCast = cr.votesCast,
                totalVoters = cr.totalVoters,
                votedUserIds = cr.votedUserIds,
            )
        },
        winnerActivityId = detail.ladder.winnerActivityId,
        isFinalRound = detail.ladder.isFinalRound,
        isGrandFinalReset = detail.ladder.isGrandFinalReset,
        createdAt = detail.ladder.createdAt,
        updatedAt = detail.ladder.updatedAt,
    )

    fun toActivityResponse(activity: LadderActivity): LadderActivityResponse = LadderActivityResponse(
        id = activity.id,
        name = activity.name,
        imageUrl = activity.imageUrl,
        distanceMinutes = activity.distanceMinutes,
        costPerPerson = activity.costPerPerson,
        losses = activity.losses,
        bracket = activity.bracket.name,
        displayOrder = activity.displayOrder,
    )

    fun toParticipantResponse(participant: EnrichedParticipant): LadderParticipantResponse = LadderParticipantResponse(
        userId = participant.userId,
        username = participant.username,
        avatarSeed = participant.avatarSeed,
    )

    fun toParticipantResponse(userId: java.util.UUID, username: String?, avatarSeed: String?): LadderParticipantResponse =
        LadderParticipantResponse(userId = userId, username = username, avatarSeed = avatarSeed)

    /**
     * Maps a bare [Ladder] (without enriched data) to a [LadderDetailResponse].
     * Used after restart where no activities/participants/currentRound data is available inline.
     * Caller must have fetched the full detail separately, or this path returns an empty shell.
     * NOTE: prefer [toDetailResponse] with a full [LadderDetail] wherever possible.
     */
    fun toLadderDetailResponse(ladder: Ladder): LadderDetailResponse = LadderDetailResponse(
        id = ladder.id,
        title = ladder.title,
        status = ladder.status.name,
        creatorId = ladder.creatorId,
        activities = emptyList(),
        participants = emptyList(),
        currentRound = null,
        winnerActivityId = ladder.winnerActivityId,
        isFinalRound = ladder.isFinalRound,
        isGrandFinalReset = ladder.isGrandFinalReset,
        createdAt = ladder.createdAt,
        updatedAt = ladder.updatedAt,
    )
}
