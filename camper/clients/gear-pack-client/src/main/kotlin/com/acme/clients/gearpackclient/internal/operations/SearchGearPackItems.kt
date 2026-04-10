package com.acme.clients.gearpackclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.gearpackclient.api.SearchGearPackItemsParam
import com.acme.clients.gearpackclient.internal.adapters.GearPackItemSearchResultAdapter
import com.acme.clients.gearpackclient.internal.validations.ValidateSearchGearPackItems
import com.acme.clients.gearpackclient.model.GearPackItemSearchResult
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class SearchGearPackItems(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(SearchGearPackItems::class.java)
    private val validate = ValidateSearchGearPackItems()

    fun execute(param: SearchGearPackItemsParam): Result<List<GearPackItemSearchResult>, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Searching gear pack items query={}", param.query)
        val entities = jdbi.withHandle<List<GearPackItemSearchResult>, Exception> { handle ->
            handle.createQuery(
                """
                SELECT gpi.id, gpi.gear_pack_id, gp.name AS gear_pack_name, gpi.name, gpi.category, gpi.default_quantity, gpi.scalable
                FROM gear_pack_items gpi
                JOIN gear_packs gp ON gp.id = gpi.gear_pack_id
                WHERE LOWER(gpi.name) LIKE LOWER('%' || :query || '%')
                ORDER BY gpi.name
                """.trimIndent()
            )
                .bind("query", param.query)
                .map { rs, _ -> GearPackItemSearchResultAdapter.fromResultSet(rs) }
                .list()
        }
        return success(entities)
    }
}
