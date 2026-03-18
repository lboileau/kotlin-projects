package com.acmo.services.interviewservice.features.world.actions

import com.acmo.clients.common.Result
import com.acmo.clients.worldclient.api.DeleteWorldParam as ClientDeleteWorldParam
import com.acmo.clients.worldclient.api.WorldClient
import com.acmo.services.interviewservice.features.world.error.WorldError
import com.acmo.services.interviewservice.features.world.params.DeleteWorldParam
import com.acmo.services.interviewservice.features.world.validations.ValidateDeleteWorld
import org.slf4j.LoggerFactory

internal class DeleteWorldAction(private val worldClient: WorldClient) {
    private val logger = LoggerFactory.getLogger(DeleteWorldAction::class.java)
    private val validate = ValidateDeleteWorld()

    fun execute(param: DeleteWorldParam): Result<Unit, WorldError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Deleting world id={}", param.id)
        return when (val result = worldClient.delete(ClientDeleteWorldParam(id = param.id))) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> Result.Failure(WorldError.fromClientError(result.error))
        }
    }
}
