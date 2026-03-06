package com.acme.services.camperservice.features.world.mapper

import com.acme.clients.worldclient.model.World as ClientWorld
import com.acme.services.camperservice.features.world.dto.WorldResponse
import com.acme.services.camperservice.features.world.model.World

object WorldMapper {

    fun fromClient(clientWorld: ClientWorld): World = World(
        id = clientWorld.id,
        name = clientWorld.name,
        greeting = clientWorld.greeting,
        createdAt = clientWorld.createdAt,
        updatedAt = clientWorld.updatedAt
    )

    fun toResponse(world: World): WorldResponse = WorldResponse(
        id = world.id,
        name = world.name,
        greeting = world.greeting,
        createdAt = world.createdAt,
        updatedAt = world.updatedAt
    )
}
