package com.acme.clients.itemclient.api

import java.util.UUID

/** Parameter for creating a new item. */
data class CreateItemParam(
    val planId: UUID?,
    val userId: UUID?,
    val name: String,
    val category: String,
    val quantity: Int,
    val packed: Boolean,
    val gearPackId: UUID? = null,
)

/** Parameter for retrieving an item by ID. */
data class GetByIdParam(val id: UUID)

/** Parameter for retrieving items by plan ID. */
data class GetByPlanIdParam(val planId: UUID)

/** Parameter for retrieving items by user ID. */
data class GetByUserIdParam(val userId: UUID)

/** Parameter for retrieving personal items by plan ID and user ID. */
data class GetByPlanIdAndUserIdParam(val planId: UUID, val userId: UUID)

/** Parameter for updating an existing item. */
data class UpdateItemParam(
    val id: UUID,
    val name: String,
    val category: String,
    val quantity: Int,
    val packed: Boolean,
    val gearPackId: UUID? = null,
)

/** Parameter for deleting an item by ID. */
data class DeleteItemParam(val id: UUID)
