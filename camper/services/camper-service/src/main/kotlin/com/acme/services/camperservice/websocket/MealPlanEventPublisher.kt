package com.acme.services.camperservice.websocket

import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class MealPlanEventPublisher(private val messagingTemplate: SimpMessagingTemplate) {

    private val logger = LoggerFactory.getLogger(MealPlanEventPublisher::class.java)

    fun publishUpdate(mealPlanId: UUID, resource: String, action: String) {
        val destination = "/topic/meal-plans/$mealPlanId"
        val message = PlanUpdateMessage(resource = resource, action = action)
        logger.debug("Publishing update to {}: resource={} action={}", destination, resource, action)
        messagingTemplate.convertAndSend(destination, message)
    }
}
