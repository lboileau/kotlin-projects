package com.acme.clients.gearpackclient.internal.adapters

import com.acme.clients.gearpackclient.model.GearPackItemSearchResult
import java.sql.ResultSet
import java.util.UUID

/**
 * Adapts database rows to [GearPackItemSearchResult] domain objects.
 */
object GearPackItemSearchResultAdapter {

    fun fromResultSet(rs: ResultSet): GearPackItemSearchResult = GearPackItemSearchResult(
        id = rs.getObject("id", UUID::class.java),
        gearPackId = rs.getObject("gear_pack_id", UUID::class.java),
        gearPackName = rs.getString("gear_pack_name"),
        name = rs.getString("name"),
        category = rs.getString("category"),
        defaultQuantity = rs.getInt("default_quantity"),
        scalable = rs.getBoolean("scalable"),
    )
}
