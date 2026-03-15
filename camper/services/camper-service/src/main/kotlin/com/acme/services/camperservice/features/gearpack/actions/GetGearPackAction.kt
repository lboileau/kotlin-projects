package com.acme.services.camperservice.features.gearpack.actions

import com.acme.clients.common.Result
import com.acme.clients.gearpackclient.api.GearPackClient
import com.acme.clients.gearpackclient.api.GetGearPackByIdParam
import com.acme.services.camperservice.features.gearpack.error.GearPackError
import com.acme.services.camperservice.features.gearpack.mapper.GearPackMapper
import com.acme.services.camperservice.features.gearpack.model.GearPack
import com.acme.services.camperservice.features.gearpack.params.GetGearPackParam
import com.acme.services.camperservice.features.gearpack.validations.ValidateGetGearPack
import org.slf4j.LoggerFactory

internal class GetGearPackAction(private val gearPackClient: GearPackClient) {
    private val logger = LoggerFactory.getLogger(GetGearPackAction::class.java)
    private val validate = ValidateGetGearPack()

    fun execute(param: GetGearPackParam): Result<GearPack, GearPackError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Getting gear pack id={}", param.id)
        return when (val result = gearPackClient.getById(GetGearPackByIdParam(id = param.id))) {
            is Result.Success -> Result.Success(GearPackMapper.fromClient(result.value))
            is Result.Failure -> Result.Failure(GearPackError.fromClientError(result.error))
        }
    }
}
