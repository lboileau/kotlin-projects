package com.acme.services.camperservice.features.gearpack.actions

import com.acme.clients.common.Result
import com.acme.clients.gearpackclient.api.GearPackClient
import com.acme.clients.gearpackclient.api.CreateGearPackParam as ClientCreateGearPackParam
import com.acme.services.camperservice.features.gearpack.error.GearPackError
import com.acme.services.camperservice.features.gearpack.mapper.GearPackMapper
import com.acme.services.camperservice.features.gearpack.model.GearPack
import com.acme.services.camperservice.features.gearpack.params.CreateGearPackParam
import com.acme.services.camperservice.features.gearpack.validations.ValidateCreateGearPack
import org.slf4j.LoggerFactory

internal class CreateGearPackAction(private val gearPackClient: GearPackClient) {
    private val logger = LoggerFactory.getLogger(CreateGearPackAction::class.java)
    private val validate = ValidateCreateGearPack()

    fun execute(param: CreateGearPackParam): Result<GearPack, GearPackError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Creating gear pack name='{}' createdBy={}", param.name, param.requestingUserId)
        return when (val result = gearPackClient.create(
            ClientCreateGearPackParam(
                name = param.name,
                description = param.description,
                createdBy = param.requestingUserId,
            )
        )) {
            is Result.Success -> Result.Success(GearPackMapper.fromClient(result.value))
            is Result.Failure -> Result.Failure(GearPackError.fromClientError(result.error))
        }
    }
}
