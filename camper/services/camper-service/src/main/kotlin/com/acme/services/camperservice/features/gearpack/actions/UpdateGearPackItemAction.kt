package com.acme.services.camperservice.features.gearpack.actions

import com.acme.clients.common.Result
import com.acme.clients.common.error.ConflictError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.gearpackclient.api.GearPackClient
import com.acme.clients.gearpackclient.api.GetGearPackByIdParam
import com.acme.clients.gearpackclient.api.UpdateGearPackItemParam as ClientUpdateGearPackItemParam
import com.acme.services.camperservice.features.gearpack.dto.GearPackItemResponse
import com.acme.services.camperservice.features.gearpack.error.GearPackError
import com.acme.services.camperservice.features.gearpack.mapper.GearPackMapper
import com.acme.services.camperservice.features.gearpack.params.UpdateGearPackItemParam
import com.acme.services.camperservice.features.gearpack.validations.ValidateUpdateGearPackItem
import org.slf4j.LoggerFactory

internal class UpdateGearPackItemAction(private val gearPackClient: GearPackClient) {
    private val logger = LoggerFactory.getLogger(UpdateGearPackItemAction::class.java)
    private val validate = ValidateUpdateGearPackItem()

    fun execute(param: UpdateGearPackItemParam): Result<GearPackItemResponse, GearPackError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        val pack = when (val result = gearPackClient.getById(GetGearPackByIdParam(id = param.gearPackId))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(GearPackError.fromClientError(result.error))
        }

        if (pack.createdBy == null) return Result.Failure(GearPackError.SystemPack(pack.id))
        if (pack.createdBy != param.requestingUserId) return Result.Failure(GearPackError.NotCreator(pack.id, param.requestingUserId))

        logger.debug("Updating gear pack item id={} in pack id={}", param.id, param.gearPackId)
        return when (val result = gearPackClient.updateItem(
            ClientUpdateGearPackItemParam(
                id = param.id,
                gearPackId = param.gearPackId,
                name = param.name,
                category = param.category,
                defaultQuantity = param.defaultQuantity,
                scalable = param.scalable,
            )
        )) {
            is Result.Success -> Result.Success(GearPackMapper.toItemResponse(GearPackMapper.fromClientItem(result.value)))
            is Result.Failure -> {
                val error = result.error
                Result.Failure(
                    when {
                        error is ConflictError -> GearPackError.DuplicateItemName(error.detail, pack.name)
                        error is NotFoundError -> GearPackError.ItemNotFound(param.id)
                        else -> GearPackError.fromClientError(error)
                    }
                )
            }
        }
    }
}
