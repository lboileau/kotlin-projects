package com.acme.clients.gearpackclient.api

import java.util.UUID

/** Parameter for listing all gear packs. */
class GetAllGearPacksParam

/** Parameter for retrieving a gear pack by ID. */
data class GetGearPackByIdParam(val id: UUID)
