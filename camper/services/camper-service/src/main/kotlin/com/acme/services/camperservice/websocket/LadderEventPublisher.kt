package com.acme.services.camperservice.websocket

import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class LadderEventPublisher(private val messagingTemplate: SimpMessagingTemplate) {
    private val logger = LoggerFactory.getLogger(LadderEventPublisher::class.java)

    private fun publish(ladderId: UUID, action: String, payload: Map<String, Any?>? = null) {
        val destination = "/topic/ladders/$ladderId"
        val message = LadderUpdateMessage(action = action, payload = payload)
        logger.debug("Publishing to {}: {}", destination, message)
        messagingTemplate.convertAndSend(destination, message)
    }

    fun presenceChanged(ladderId: UUID, presentUserIds: Set<UUID>) =
        publish(ladderId, "presence-changed", mapOf("presentUserIds" to presentUserIds.map { it.toString() }))

    fun activityAdded(ladderId: UUID) = publish(ladderId, "activity-added")

    fun activityRemoved(ladderId: UUID) = publish(ladderId, "activity-removed")

    fun started(ladderId: UUID) = publish(ladderId, "started")

    fun roundStarted(
        ladderId: UUID,
        roundNumber: Int,
        activityAId: UUID,
        activityBId: UUID,
        isFinal: Boolean,
        isReset: Boolean,
    ) = publish(
        ladderId, "round-started", mapOf(
            "roundNumber" to roundNumber,
            "activityAId" to activityAId.toString(),
            "activityBId" to activityBId.toString(),
            "isFinalRound" to isFinal,
            "isGrandFinalReset" to isReset,
        )
    )

    fun voteCast(ladderId: UUID, voterId: UUID, voteCount: Int, votersRemaining: Int) =
        publish(
            ladderId, "vote-cast", mapOf(
                "voterId" to voterId.toString(),
                "voteCount" to voteCount,
                "votersRemaining" to votersRemaining,
            )
        )

    fun roundResolved(ladderId: UUID, outcome: String, winnerActivityId: UUID?, voteTotals: Map<UUID, Int>?) =
        publish(
            ladderId, "round-resolved", mapOf(
                "outcome" to outcome,
                "winnerActivityId" to winnerActivityId?.toString(),
                "voteTotals" to voteTotals?.mapKeys { it.key.toString() },
            )
        )

    fun completed(ladderId: UUID, winnerActivityId: UUID) =
        publish(ladderId, "completed", mapOf("winnerActivityId" to winnerActivityId.toString()))

    fun restarted(ladderId: UUID) = publish(ladderId, "restarted")
}
