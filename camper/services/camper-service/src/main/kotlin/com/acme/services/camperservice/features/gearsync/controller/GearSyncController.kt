package com.acme.services.camperservice.features.gearsync.controller

import com.acme.services.camperservice.common.error.toResponseEntity
import com.acme.services.camperservice.features.gearsync.params.SyncGearParam
import com.acme.services.camperservice.features.gearsync.service.GearSyncService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/plans/{planId}/gear-sync")
class GearSyncController(private val gearSyncService: GearSyncService) {

    @PostMapping
    fun sync(
        @PathVariable planId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
    ): ResponseEntity<Any> {
        val param = SyncGearParam(planId = planId)
        return gearSyncService.sync(param).toResponseEntity { it }
    }
}
