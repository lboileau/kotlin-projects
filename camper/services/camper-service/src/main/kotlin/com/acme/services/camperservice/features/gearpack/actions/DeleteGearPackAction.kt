package com.acme.services.camperservice.features.gearpack.actions

import com.acme.clients.common.Result
import com.acme.clients.gearpackclient.api.DeleteGearPackParam as ClientDeleteGearPackParam
import com.acme.clients.gearpackclient.api.GearPackClient
import com.acme.clients.gearpackclient.api.GetGearPackByIdParam
import com.acme.services.camperservice.features.gearpack.error.GearPackError
import com.acme.services.camperservice.features.gearpack.params.DeleteGearPackParam
import com.acme.services.camperservice.features.gearpack.validations.ValidateDeleteGearPack
import org.slf4j.LoggerFactory

internal class DeleteGearPackAction(private val gearPackClient: GearPackClient) {
    private val logger = LoggerFactory.getLogger(DeleteGearPackAction::class.java)
    private val validate = ValidateDeleteGearPack()

    fun execute(param: DeleteGearPackParam): Result<Unit, GearPackError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        val pack = when (val result = gearPackClient.getById(GetGearPackByIdParam(id = param.id))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(GearPackError.fromClientError(result.error))
        }

        if (pack.createdBy == null) return Result.Failure(GearPackError.SystemPack(pack.id))
        if (pack.createdBy != param.requestingUserId) return Result.Failure(GearPackError.NotCreator(pack.id, param.requestingUserId))

        logger.debug("Deleting gear pack id={}", param.id)
        return when (val result = gearPackClient.delete(ClientDeleteGearPackParam(id = param.id))) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> Result.Failure(GearPackError.fromClientError(result.error))
        }
    }
}
