package com.acme.services.camperservice.websocket

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.SimpMessagingTemplate
import java.util.UUID

class LadderPresenceTrackerTest {

    private val noOpChannel = object : MessageChannel {
        override fun send(message: Message<*>): Boolean = true
        override fun send(message: Message<*>, timeout: Long): Boolean = true
    }
    private val eventPublisher = LadderEventPublisher(SimpMessagingTemplate(noOpChannel))
    private val tracker = LadderPresenceTracker(eventPublisher)

    private val ladderId = UUID.fromString("aa000000-0001-4000-8000-000000000001")
    private val userId1 = UUID.fromString("bb000000-0001-4000-8000-000000000001")
    private val userId2 = UUID.fromString("bb000000-0001-4000-8000-000000000002")
    private val session1 = "sess-1"
    private val session2 = "sess-2"
    private val session3 = "sess-3"

    @BeforeEach
    fun setUp() {
        // tracker is stateless across tests — nothing to reset since each test creates a new tracker
    }

    @Nested
    inner class Snapshot {

        @Test
        fun `returns empty set when no subscriptions exist`() {
            assertThat(tracker.snapshot(ladderId)).isEmpty()
        }

        @Test
        fun `returns empty set after all sessions unsubscribe`() {
            tracker.subscribed(session1, ladderId, userId1)
            tracker.unsubscribed(session1)

            assertThat(tracker.snapshot(ladderId)).isEmpty()
        }
    }

    @Nested
    inner class Subscribe {

        @Test
        fun `adds user to presence after subscribe`() {
            tracker.subscribed(session1, ladderId, userId1)

            assertThat(tracker.snapshot(ladderId)).containsExactly(userId1)
        }

        @Test
        fun `adds multiple distinct users`() {
            tracker.subscribed(session1, ladderId, userId1)
            tracker.subscribed(session2, ladderId, userId2)

            assertThat(tracker.snapshot(ladderId)).containsExactlyInAnyOrder(userId1, userId2)
        }

        @Test
        fun `same user with two sessions counted once in snapshot`() {
            tracker.subscribed(session1, ladderId, userId1)
            tracker.subscribed(session2, ladderId, userId1)

            assertThat(tracker.snapshot(ladderId)).containsExactly(userId1)
        }

        @Test
        fun `subscriptions for different ladders are isolated`() {
            val otherLadderId = UUID.fromString("aa000000-0002-4000-8000-000000000001")
            tracker.subscribed(session1, ladderId, userId1)
            tracker.subscribed(session2, otherLadderId, userId2)

            assertThat(tracker.snapshot(ladderId)).containsExactly(userId1)
            assertThat(tracker.snapshot(otherLadderId)).containsExactly(userId2)
        }
    }

    @Nested
    inner class Unsubscribe {

        @Test
        fun `removes user from presence when last session disconnects`() {
            tracker.subscribed(session1, ladderId, userId1)
            tracker.unsubscribed(session1)

            assertThat(tracker.snapshot(ladderId)).isEmpty()
        }

        @Test
        fun `user stays present when one of two sessions unsubscribes`() {
            tracker.subscribed(session1, ladderId, userId1)
            tracker.subscribed(session2, ladderId, userId1)

            tracker.unsubscribed(session1)

            assertThat(tracker.snapshot(ladderId)).containsExactly(userId1)
        }

        @Test
        fun `user removed only after both sessions disconnect`() {
            tracker.subscribed(session1, ladderId, userId1)
            tracker.subscribed(session2, ladderId, userId1)

            tracker.unsubscribed(session1)
            tracker.unsubscribed(session2)

            assertThat(tracker.snapshot(ladderId)).isEmpty()
        }

        @Test
        fun `unsubscribing unknown session is a no-op`() {
            tracker.subscribed(session1, ladderId, userId1)
            tracker.unsubscribed("unknown-session-id")

            assertThat(tracker.snapshot(ladderId)).containsExactly(userId1)
        }

        @Test
        fun `removing one user leaves other users in snapshot`() {
            tracker.subscribed(session1, ladderId, userId1)
            tracker.subscribed(session2, ladderId, userId2)

            tracker.unsubscribed(session1)

            assertThat(tracker.snapshot(ladderId)).containsExactly(userId2)
        }

        @Test
        fun `unsubscribing all users removes ladder entry entirely`() {
            tracker.subscribed(session1, ladderId, userId1)
            tracker.subscribed(session2, ladderId, userId2)
            tracker.unsubscribed(session1)
            tracker.unsubscribed(session2)

            // snapshot returns empty — no stale ladder entry
            assertThat(tracker.snapshot(ladderId)).isEmpty()
        }
    }

    @Nested
    inner class GetPresent {

        @Test
        fun `getPresent returns same result as snapshot`() {
            tracker.subscribed(session1, ladderId, userId1)
            tracker.subscribed(session2, ladderId, userId2)

            assertThat(tracker.getPresent(ladderId))
                .isEqualTo(tracker.snapshot(ladderId))
                .containsExactlyInAnyOrder(userId1, userId2)
        }
    }

    @Nested
    inner class MultiTabScenarios {

        @Test
        fun `three tabs — user remains present until last tab closes`() {
            // Three browser tabs for userId1
            tracker.subscribed(session1, ladderId, userId1)
            tracker.subscribed(session2, ladderId, userId1)
            tracker.subscribed(session3, ladderId, userId1)

            tracker.unsubscribed(session1)
            assertThat(tracker.snapshot(ladderId)).containsExactly(userId1)

            tracker.unsubscribed(session2)
            assertThat(tracker.snapshot(ladderId)).containsExactly(userId1)

            tracker.unsubscribed(session3)
            assertThat(tracker.snapshot(ladderId)).isEmpty()
        }

        @Test
        fun `mixed users with multiple sessions — independent counts`() {
            // userId1 has 2 tabs, userId2 has 1 tab
            tracker.subscribed(session1, ladderId, userId1)
            tracker.subscribed(session2, ladderId, userId1)
            tracker.subscribed(session3, ladderId, userId2)

            tracker.unsubscribed(session1)
            // userId1 still present (1 session left), userId2 still present
            assertThat(tracker.snapshot(ladderId)).containsExactlyInAnyOrder(userId1, userId2)

            tracker.unsubscribed(session3)
            // userId2 gone, userId1 still has 1 session
            assertThat(tracker.snapshot(ladderId)).containsExactly(userId1)

            tracker.unsubscribed(session2)
            // All gone
            assertThat(tracker.snapshot(ladderId)).isEmpty()
        }
    }
}
