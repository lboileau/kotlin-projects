package com.acme.services.camperservice.features.gearsync.dto

data class GearSyncItemResponse(
    val name: String,
    val category: String,
    val quantity: Int,
)

data class GearSyncResponse(
    val items: List<GearSyncItemResponse>,
)
