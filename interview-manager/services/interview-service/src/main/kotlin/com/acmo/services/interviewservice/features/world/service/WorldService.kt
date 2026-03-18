package com.acmo.services.interviewservice.features.world.service

import com.acmo.clients.worldclient.api.WorldClient
import com.acmo.services.interviewservice.features.world.actions.*
import com.acmo.services.interviewservice.features.world.params.*

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
