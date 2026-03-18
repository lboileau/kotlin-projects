package com.acmo.services.interviewservice.features.world.actions

import com.acmo.clients.common.Result
import com.acmo.clients.worldclient.api.UpdateWorldParam as ClientUpdateWorldParam
import com.acmo.clients.worldclient.api.WorldClient
import com.acmo.services.interviewservice.features.world.error.WorldError
import com.acmo.services.interviewservice.features.world.mapper.WorldMapper
import com.acmo.services.interviewservice.features.world.model.World
import com.acmo.services.interviewservice.features.world.params.UpdateWorldParam
import com.acmo.services.interviewservice.features.world.validations.ValidateUpdateWorld
import org.slf4j.LoggerFactory

internal class UpdateWorldAction(private val worldClient: WorldClient) {
    private val logger = LoggerFactory.getLogger(UpdateWorldAction::class.java)
    private val validate = ValidateUpdateWorld()

    fun execute(param: UpdateWorldParam): Result<World, WorldError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Updating world id={}", param.id)
        return when (val result = worldClient.update(ClientUpdateWorldParam(id = param.id, name = param.name, greeting = param.greeting))) {
            is Result.Success -> Result.Success(WorldMapper.fromClient(result.value))
            is Result.Failure -> Result.Failure(WorldError.fromClientError(result.error))
        }
    }
}
