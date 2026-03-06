package com.example.services.hello.features.world.params

import java.util.UUID

data class GetWorldByIdParam(val id: UUID)
data class GetAllWorldsParam(val limit: Int? = null, val offset: Int? = null)
data class CreateWorldParam(val name: String, val greeting: String)
data class UpdateWorldParam(val id: UUID, val name: String? = null, val greeting: String? = null)
data class DeleteWorldParam(val id: UUID)
