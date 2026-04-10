package com.acme.services.camperservice.features.gearpack.actions

import com.acme.clients.common.Result
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.gearpackclient.api.GearPackClient
import com.acme.clients.gearpackclient.api.GetGearPackByIdParam
import com.acme.clients.gearpackclient.api.RemoveGearPackItemParam as ClientRemoveGearPackItemParam
import com.acme.services.camperservice.features.gearpack.error.GearPackError
import com.acme.services.camperservice.features.gearpack.params.RemoveGearPackItemParam
import com.acme.services.camperservice.features.gearpack.validations.ValidateRemoveGearPackItem
import org.slf4j.LoggerFactory

internal class RemoveGearPackItemAction(private val gearPackClient: GearPackClient) {
    private val logger = LoggerFactory.getLogger(RemoveGearPackItemAction::class.java)
    private val validate = ValidateRemoveGearPackItem()

    fun execute(param: RemoveGearPackItemParam): Result<Unit, GearPackError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        val pack = when (val result = gearPackClient.getById(GetGearPackByIdParam(id = param.gearPackId))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(GearPackError.fromClientError(result.error))
        }

        if (pack.createdBy == null) return Result.Failure(GearPackError.SystemPack(pack.id))
        if (pack.createdBy != param.requestingUserId) return Result.Failure(GearPackError.NotCreator(pack.id, param.requestingUserId))

        logger.debug("Removing gear pack item id={} from pack id={}", param.id, param.gearPackId)
        return when (val result = gearPackClient.removeItem(
            ClientRemoveGearPackItemParam(id = param.id, gearPackId = param.gearPackId)
        )) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> {
                val error = result.error
                Result.Failure(
                    if (error is NotFoundError) GearPackError.ItemNotFound(param.id)
                    else GearPackError.fromClientError(error)
                )
            }
        }
    }
}
