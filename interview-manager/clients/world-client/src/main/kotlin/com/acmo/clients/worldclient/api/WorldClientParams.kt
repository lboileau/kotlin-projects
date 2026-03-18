package com.acmo.clients.worldclient.api

import java.util.UUID

/** Parameter for retrieving a world by its unique identifier. */
data class GetByIdParam(val id: UUID)

/** Parameter for listing worlds. Extensible for future filtering/pagination. */
data class GetListParam(
    val limit: Int? = null,
    val offset: Int? = null
)

/** Parameter for creating a new world. */
data class CreateWorldParam(val name: String, val greeting: String)

/** Parameter for updating an existing world. Null fields are left unchanged. */
data class UpdateWorldParam(
    val id: UUID,
    val name: String? = null,
    val greeting: String? = null
)

/** Parameter for deleting a world by its unique identifier. */
data class DeleteWorldParam(val id: UUID)
