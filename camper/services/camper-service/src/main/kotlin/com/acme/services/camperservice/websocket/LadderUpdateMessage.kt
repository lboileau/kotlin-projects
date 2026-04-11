package com.acme.services.camperservice.websocket

data class LadderUpdateMessage(
    val resource: String = "ladder",
    val action: String,
    val payload: Map<String, Any?>? = null,
)
