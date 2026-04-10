package com.acme.services.camperservice.features.gearpack.actions

import com.acme.clients.common.Result
import com.acme.clients.gearpackclient.api.GearPackClient
import com.acme.clients.gearpackclient.api.SearchGearPackItemsParam as ClientSearchGearPackItemsParam
import com.acme.services.camperservice.features.gearpack.dto.GearPackItemSearchResultResponse
import com.acme.services.camperservice.features.gearpack.error.GearPackError
import com.acme.services.camperservice.features.gearpack.mapper.GearPackMapper
import com.acme.services.camperservice.features.gearpack.params.SearchGearPackItemsParam
import com.acme.services.camperservice.features.gearpack.validations.ValidateSearchGearPackItems
import org.slf4j.LoggerFactory

internal class SearchGearPackItemsAction(private val gearPackClient: GearPackClient) {
    private val logger = LoggerFactory.getLogger(SearchGearPackItemsAction::class.java)
    private val validate = ValidateSearchGearPackItems()

    fun execute(param: SearchGearPackItemsParam): Result<List<GearPackItemSearchResultResponse>, GearPackError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Searching gear pack items query='{}'", param.query)
        return when (val result = gearPackClient.searchItems(ClientSearchGearPackItemsParam(query = param.query))) {
            is Result.Success -> Result.Success(result.value.map { GearPackMapper.toSearchResultResponse(it) })
            is Result.Failure -> Result.Failure(GearPackError.fromClientError(result.error))
        }
    }
}
