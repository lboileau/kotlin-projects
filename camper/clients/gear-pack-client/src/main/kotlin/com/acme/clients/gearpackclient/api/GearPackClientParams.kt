package com.acme.clients.gearpackclient.api

import java.util.UUID

/** Parameter for listing all gear packs. */
class GetAllGearPacksParam

/** Parameter for retrieving a gear pack by ID. */
data class GetGearPackByIdParam(val id: UUID)

/** Parameter for creating a new gear pack. */
data class CreateGearPackParam(
    val name: String,
    val description: String = "",
    val createdBy: UUID,
)

/** Parameter for updating an existing gear pack. Null fields are left unchanged. */
data class UpdateGearPackParam(
    val id: UUID,
    val name: String? = null,
    val description: String? = null,
)

/** Parameter for deleting a gear pack by ID. */
data class DeleteGearPackParam(val id: UUID)

/** Parameter for adding an item to a gear pack. */
data class AddGearPackItemParam(
    val gearPackId: UUID,
    val name: String,
    val category: String,
    val defaultQuantity: Int = 1,
    val scalable: Boolean = false,
)

/** Parameter for updating an item within a gear pack. Null fields are left unchanged. */
data class UpdateGearPackItemParam(
    val id: UUID,
    val gearPackId: UUID,
    val name: String? = null,
    val category: String? = null,
    val defaultQuantity: Int? = null,
    val scalable: Boolean? = null,
)

/** Parameter for removing an item from a gear pack. */
data class RemoveGearPackItemParam(
    val id: UUID,
    val gearPackId: UUID,
)

/** Parameter for searching gear pack items by name. */
data class SearchGearPackItemsParam(val query: String)
