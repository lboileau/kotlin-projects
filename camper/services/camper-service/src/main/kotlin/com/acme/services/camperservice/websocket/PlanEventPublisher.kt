package com.acme.services.camperservice.websocket

import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class PlanEventPublisher(private val messagingTemplate: SimpMessagingTemplate) {

    private val logger = LoggerFactory.getLogger(PlanEventPublisher::class.java)

    fun publishUpdate(planId: UUID, resource: String, action: String) {
        val destination = "/topic/plans/$planId"
        val message = PlanUpdateMessage(resource = resource, action = action)
        logger.debug("Publishing update to {}: resource={} action={}", destination, resource, action)
        messagingTemplate.convertAndSend(destination, message)
    }
}
