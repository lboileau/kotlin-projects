package com.acme.services.camperservice.features.webhook.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class ResendWebhookEvent(
    val type: String,
    @JsonProperty("created_at") val createdAt: String,
    val data: ResendWebhookData
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ResendWebhookData(
    @JsonProperty("email_id") val emailId: String,
    val from: String?,
    val to: List<String>?,
    val subject: String?
)
