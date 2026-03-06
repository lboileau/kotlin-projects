package com.acme.clients.common.error

data class NotFoundError(
    val entity: String,
    val id: String,
    override val message: String = "$entity not found: $id"
) : AppError
