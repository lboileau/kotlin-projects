package com.example.clients.common

/**
 * Context information passed to client instances.
 * Carries cross-cutting concerns like correlation IDs and caller metadata.
 */
data class ClientContext(
    val correlationId: String? = null,
    val callerName: String? = null
)
