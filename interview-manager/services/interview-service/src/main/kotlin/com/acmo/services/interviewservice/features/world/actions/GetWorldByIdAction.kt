package com.acmo.services.interviewservice.features.world.actions

import com.acmo.clients.common.Result
import com.acmo.clients.worldclient.api.GetByIdParam as ClientGetByIdParam
import com.acmo.clients.worldclient.api.WorldClient
import com.acmo.services.interviewservice.features.world.error.WorldError
import com.acmo.services.interviewservice.features.world.mapper.WorldMapper
import com.acmo.services.interviewservice.features.world.model.World
import com.acmo.services.interviewservice.features.world.params.GetWorldByIdParam
import com.acmo.services.interviewservice.features.world.validations.ValidateGetWorldById
import org.slf4j.LoggerFactory

internal class GetWorldByIdAction(private val worldClient: WorldClient) {
    private val logger = LoggerFactory.getLogger(GetWorldByIdAction::class.java)
    private val validate = ValidateGetWorldById()

    fun execute(param: GetWorldByIdParam): Result<World, WorldError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Getting world by id={}", param.id)
        return when (val result = worldClient.getById(ClientGetByIdParam(id = param.id))) {
            is Result.Success -> Result.Success(WorldMapper.fromClient(result.value))
            is Result.Failure -> Result.Failure(WorldError.fromClientError(result.error))
        }
    }
}
