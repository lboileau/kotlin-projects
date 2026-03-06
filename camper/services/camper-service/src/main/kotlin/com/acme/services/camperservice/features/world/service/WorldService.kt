package com.acme.services.camperservice.features.world.service

import com.acme.clients.worldclient.api.WorldClient
import com.acme.services.camperservice.features.world.actions.*
import com.acme.services.camperservice.features.world.params.*

class WorldService(worldClient: WorldClient) {
    private val getWorldById = GetWorldByIdAction(worldClient)
    private val getAllWorlds = GetAllWorldsAction(worldClient)
    private val createWorld = CreateWorldAction(worldClient)
    private val updateWorld = UpdateWorldAction(worldClient)
    private val deleteWorld = DeleteWorldAction(worldClient)

    fun getById(param: GetWorldByIdParam) = getWorldById.execute(param)
    fun getAll(param: GetAllWorldsParam) = getAllWorlds.execute(param)
    fun create(param: CreateWorldParam) = createWorld.execute(param)
    fun update(param: UpdateWorldParam) = updateWorld.execute(param)
    fun delete(param: DeleteWorldParam) = deleteWorld.execute(param)
}
