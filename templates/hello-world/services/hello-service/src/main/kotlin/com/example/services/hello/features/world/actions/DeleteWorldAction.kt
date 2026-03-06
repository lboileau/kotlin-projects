package com.example.services.hello.features.world.actions

import com.example.clients.common.Result
import com.example.clients.worldclient.api.WorldClient
import com.example.clients.worldclient.api.DeleteWorldParam as ClientDeleteWorldParam
import com.example.services.hello.features.world.error.WorldError
import com.example.services.hello.features.world.params.DeleteWorldParam
import com.example.services.hello.features.world.validations.ValidateDeleteWorld
import org.slf4j.LoggerFactory

internal class DeleteWorldAction(private val worldClient: WorldClient) {
    private val logger = LoggerFactory.getLogger(DeleteWorldAction::class.java)
    private val validate = ValidateDeleteWorld()

    fun execute(param: DeleteWorldParam): Result<Unit, WorldError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Deleting world id={}", param.id)
        return when (val result = worldClient.delete(ClientDeleteWorldParam(id = param.id))) {
            is Result.Success -> Result.Success(result.value)
            is Result.Failure -> Result.Failure(WorldError.fromClientError(result.error))
        }
    }
}
