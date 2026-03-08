package com.acme.services.camperservice.websocket

data class PlanUpdateMessage(
    val resource: String,
    val action: String,
)
