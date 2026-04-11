package com.acme.services.camperservice.websocket

import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.messaging.Message
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.stereotype.Component
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import org.springframework.web.socket.messaging.SessionSubscribeEvent
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent
import java.util.UUID

/**
 * Spring event listener that hooks into STOMP session lifecycle events to maintain
 * ladder presence state via [LadderPresenceTracker].
 *
 * Listens for [SessionSubscribeEvent], [SessionUnsubscribeEvent], [SessionDisconnectEvent].
 * Only reacts to destinations matching /topic/ladders/{uuid}.
 *
 * The X-User-Id header must be sent by the frontend in the STOMP CONNECT frame's
 * connectHeaders. The backend reads it via StompHeaderAccessor.getNativeHeader.
 * For disconnect events, the userId is looked up from the session registry in
 * LadderPresenceTracker (keyed by sessionId) -- no headers needed at disconnect time.
 */
@Component
class LadderStompSessionListener(private val presenceTracker: LadderPresenceTracker) {
    private val logger = LoggerFactory.getLogger(LadderStompSessionListener::class.java)
    private val LADDER_TOPIC_REGEX = Regex("""^/topic/ladders/([0-9a-fA-F-]{36})$""")

    @EventListener
    fun onSubscribe(event: SessionSubscribeEvent) {
        val accessor = StompHeaderAccessor.wrap(event.message)
        val destination = accessor.destination ?: return
        val match = LADDER_TOPIC_REGEX.matchEntire(destination) ?: return
        val ladderId = runCatching { UUID.fromString(match.groupValues[1]) }.getOrNull() ?: return
        val sessionId = accessor.sessionId ?: return
        val userId = extractUserId(accessor) ?: run {
            logger.warn("Subscribe to {} with no X-User-Id; ignoring presence tracking for session {}", destination, sessionId)
            return
        }
        presenceTracker.subscribed(sessionId, ladderId, userId)
    }

    @EventListener
    fun onUnsubscribe(event: SessionUnsubscribeEvent) {
        handleLeave(event.message)
    }

    @EventListener
    fun onDisconnect(event: SessionDisconnectEvent) {
        handleLeave(event.message)
    }

    private fun handleLeave(message: Message<*>) {
        val sessionId = StompHeaderAccessor.wrap(message).sessionId ?: return
        presenceTracker.unsubscribed(sessionId)
    }

    private fun extractUserId(accessor: StompHeaderAccessor): UUID? {
        val headers = accessor.getNativeHeader("X-User-Id") ?: return null
        return headers.firstOrNull()?.let { runCatching { UUID.fromString(it) }.getOrNull() }
    }
}
