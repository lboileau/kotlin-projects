package com.example.services.hello.features.world.actions

import com.example.clients.common.Result
import com.example.clients.worldclient.api.WorldClient
import com.example.clients.worldclient.api.CreateWorldParam as ClientCreateWorldParam
import com.example.services.hello.features.world.error.WorldError
import com.example.services.hello.features.world.mapper.WorldMapper
import com.example.services.hello.features.world.model.World
import com.example.services.hello.features.world.params.CreateWorldParam
import com.example.services.hello.features.world.validations.ValidateCreateWorld
import org.slf4j.LoggerFactory

internal class CreateWorldAction(private val worldClient: WorldClient) {
    private val logger = LoggerFactory.getLogger(CreateWorldAction::class.java)
    private val validate = ValidateCreateWorld()

    fun execute(param: CreateWorldParam): Result<World, WorldError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Creating world name={}", param.name)
        return when (val result = worldClient.create(ClientCreateWorldParam(name = param.name, greeting = param.greeting))) {
            is Result.Success -> Result.Success(WorldMapper.fromClient(result.value))
            is Result.Failure -> Result.Failure(WorldError.fromClientError(result.error))
        }
    }
}
