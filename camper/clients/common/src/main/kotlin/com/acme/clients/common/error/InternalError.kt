package com.acme.clients.common.error

data class InternalError(
    override val message: String
) : AppError
