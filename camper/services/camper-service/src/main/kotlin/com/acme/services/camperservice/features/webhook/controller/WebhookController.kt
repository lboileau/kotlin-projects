package com.acme.services.camperservice.features.webhook.controller

import com.acme.services.camperservice.features.webhook.actions.HandleResendWebhookAction
import com.acme.services.camperservice.features.webhook.dto.ResendWebhookEvent
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/webhooks")
class WebhookController(private val handleResendWebhook: HandleResendWebhookAction) {
    private val logger = LoggerFactory.getLogger(WebhookController::class.java)

    @PostMapping("/resend")
    fun resendWebhook(@RequestBody event: ResendWebhookEvent): ResponseEntity<Unit> {
        logger.info("POST /api/webhooks/resend type={}", event.type)
        handleResendWebhook.execute(event)
        return ResponseEntity.ok().build()
    }
}
