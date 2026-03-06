package com.example.services.hello.features.world.actions

import com.example.clients.common.Result
import com.example.clients.worldclient.api.WorldClient
import com.example.clients.worldclient.api.GetListParam as ClientGetListParam
import com.example.services.hello.features.world.error.WorldError
import com.example.services.hello.features.world.mapper.WorldMapper
import com.example.services.hello.features.world.model.World
import com.example.services.hello.features.world.params.GetAllWorldsParam
import com.example.services.hello.features.world.validations.ValidateGetAllWorlds
import org.slf4j.LoggerFactory

internal class GetAllWorldsAction(private val worldClient: WorldClient) {
    private val logger = LoggerFactory.getLogger(GetAllWorldsAction::class.java)
    private val validate = ValidateGetAllWorlds()

    fun execute(param: GetAllWorldsParam): Result<List<World>, WorldError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Getting all worlds")
        return when (val result = worldClient.getList(ClientGetListParam(limit = param.limit, offset = param.offset))) {
            is Result.Success -> Result.Success(result.value.map { WorldMapper.fromClient(it) })
            is Result.Failure -> Result.Failure(WorldError.fromClientError(result.error))
        }
    }
}
