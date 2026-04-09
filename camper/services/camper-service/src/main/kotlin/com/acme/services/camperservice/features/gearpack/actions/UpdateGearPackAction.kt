package com.acme.services.camperservice.features.gearpack.actions

import com.acme.clients.common.Result
import com.acme.clients.gearpackclient.api.GearPackClient
import com.acme.clients.gearpackclient.api.GetGearPackByIdParam
import com.acme.clients.gearpackclient.api.UpdateGearPackParam as ClientUpdateGearPackParam
import com.acme.services.camperservice.features.gearpack.error.GearPackError
import com.acme.services.camperservice.features.gearpack.mapper.GearPackMapper
import com.acme.services.camperservice.features.gearpack.model.GearPack
import com.acme.services.camperservice.features.gearpack.params.UpdateGearPackParam
import com.acme.services.camperservice.features.gearpack.validations.ValidateUpdateGearPack
import org.slf4j.LoggerFactory

internal class UpdateGearPackAction(private val gearPackClient: GearPackClient) {
    private val logger = LoggerFactory.getLogger(UpdateGearPackAction::class.java)
    private val validate = ValidateUpdateGearPack()

    fun execute(param: UpdateGearPackParam): Result<GearPack, GearPackError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        val pack = when (val result = gearPackClient.getById(GetGearPackByIdParam(id = param.id))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(GearPackError.fromClientError(result.error))
        }

        if (pack.createdBy == null) return Result.Failure(GearPackError.SystemPack(pack.id))
        if (pack.createdBy != param.requestingUserId) return Result.Failure(GearPackError.NotCreator(pack.id, param.requestingUserId))

        logger.debug("Updating gear pack id={}", param.id)
        return when (val result = gearPackClient.update(
            ClientUpdateGearPackParam(
                id = param.id,
                name = param.name,
                description = param.description,
            )
        )) {
            is Result.Success -> Result.Success(GearPackMapper.fromClient(result.value))
            is Result.Failure -> Result.Failure(GearPackError.fromClientError(result.error))
        }
    }
}
