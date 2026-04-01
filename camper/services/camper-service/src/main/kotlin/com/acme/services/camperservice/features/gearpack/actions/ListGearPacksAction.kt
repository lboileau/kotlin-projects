package com.acme.services.camperservice.features.gearpack.actions

import com.acme.clients.common.Result
import com.acme.clients.gearpackclient.api.GearPackClient
import com.acme.clients.gearpackclient.api.GetAllGearPacksParam
import com.acme.services.camperservice.features.gearpack.error.GearPackError
import com.acme.services.camperservice.features.gearpack.mapper.GearPackMapper
import com.acme.services.camperservice.features.gearpack.model.GearPack
import com.acme.services.camperservice.features.gearpack.params.ListGearPacksParam
import com.acme.services.camperservice.features.gearpack.validations.ValidateListGearPacks
import org.slf4j.LoggerFactory

internal class ListGearPacksAction(private val gearPackClient: GearPackClient) {
    private val logger = LoggerFactory.getLogger(ListGearPacksAction::class.java)
    private val validate = ValidateListGearPacks()

    fun execute(param: ListGearPacksParam): Result<List<GearPack>, GearPackError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Listing all gear packs for userId={}", param.requestingUserId)
        return when (val result = gearPackClient.getAll(GetAllGearPacksParam())) {
            is Result.Success -> Result.Success(result.value.map { GearPackMapper.fromClient(it) })
            is Result.Failure -> Result.Failure(GearPackError.fromClientError(result.error))
        }
    }
}
