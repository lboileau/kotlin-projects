package com.acmo.clients.common.error

data class ConflictError(
    val entity: String,
    val detail: String,
    override val message: String = "Conflict for $entity: $detail"
) : AppError
