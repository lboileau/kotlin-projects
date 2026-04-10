package com.acme.services.camperservice.websocket

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks which users are currently subscribed to each ladder's STOMP topic.
 *
 * Uses a session count per user (rather than a set of session IDs) so that a user
 * with multiple browser tabs open remains "present" as long as any tab stays connected.
 *
 * Thread-safe: all state is held in [ConcurrentHashMap]s and updated atomically.
 */
@Component
class LadderPresenceTracker(private val eventPublisher: LadderEventPublisher) {
    private val logger = LoggerFactory.getLogger(LadderPresenceTracker::class.java)

    // ladderId -> (userId -> sessionCount)
    private val presence = ConcurrentHashMap<UUID, ConcurrentHashMap<UUID, Int>>()

    // sessionId -> (ladderId, userId)
    private val sessions = ConcurrentHashMap<String, Pair<UUID, UUID>>()

    /** Returns the set of userIds currently subscribed to the given ladder. */
    fun snapshot(ladderId: UUID): Set<UUID> =
        presence[ladderId]?.keys?.toSet() ?: emptySet()

    /** Alias for [snapshot] — returns the set of userIds currently present on a ladder. */
    fun getPresent(ladderId: UUID): Set<UUID> = snapshot(ladderId)

    /**
     * Record that a STOMP session has subscribed to a ladder topic.
     * Increments the session count for the user on that ladder and publishes a presence-changed event.
     */
    fun subscribed(sessionId: String, ladderId: UUID, userId: UUID) {
        val ladderMap = presence.computeIfAbsent(ladderId) { ConcurrentHashMap() }
        ladderMap.merge(userId, 1) { old, _ -> old + 1 }
        sessions[sessionId] = ladderId to userId
        logger.debug("Session {} subscribed: ladderId={} userId={}", sessionId, ladderId, userId)
        eventPublisher.presenceChanged(ladderId, ladderMap.keys.toSet())
    }

    /**
     * Record that a STOMP session has unsubscribed or disconnected.
     * Decrements the session count; removes the user from presence when count reaches zero,
     * and publishes a presence-changed event.
     */
    fun unsubscribed(sessionId: String) {
        val (ladderId, userId) = sessions.remove(sessionId) ?: run {
            logger.debug("Session {} not tracked — ignoring unsubscribe", sessionId)
            return
        }
        val ladderMap = presence[ladderId] ?: return
        ladderMap.compute(userId) { _, count ->
            val next = (count ?: 1) - 1
            if (next <= 0) null else next
        }
        if (ladderMap.isEmpty()) presence.remove(ladderId)
        logger.debug("Session {} unsubscribed: ladderId={} userId={}", sessionId, ladderId, userId)
        eventPublisher.presenceChanged(ladderId, ladderMap.keys.toSet())
    }
}
