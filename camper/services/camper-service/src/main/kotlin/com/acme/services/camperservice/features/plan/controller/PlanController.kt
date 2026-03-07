package com.acme.services.camperservice.features.plan.controller

import com.acme.services.camperservice.features.plan.dto.AddMemberRequest
import com.acme.services.camperservice.features.plan.dto.CreatePlanRequest
import com.acme.services.camperservice.features.plan.dto.UpdatePlanRequest
import com.acme.services.camperservice.features.plan.service.PlanService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/plans")
class PlanController(private val planService: PlanService) {
    private val logger = LoggerFactory.getLogger(PlanController::class.java)

    @PostMapping
    fun create(
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: CreatePlanRequest
    ): ResponseEntity<Any> {
        logger.info("POST /api/plans")
        return ResponseEntity.status(501).body("Not implemented")
    }

    @GetMapping
    fun getPlans(@RequestHeader("X-User-Id") userId: UUID): ResponseEntity<Any> {
        logger.info("GET /api/plans")
        return ResponseEntity.status(501).body("Not implemented")
    }

    @PutMapping("/{planId}")
    fun update(
        @PathVariable planId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: UpdatePlanRequest
    ): ResponseEntity<Any> {
        logger.info("PUT /api/plans/{}", planId)
        return ResponseEntity.status(501).body("Not implemented")
    }

    @DeleteMapping("/{planId}")
    fun delete(
        @PathVariable planId: UUID,
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<Any> {
        logger.info("DELETE /api/plans/{}", planId)
        return ResponseEntity.status(501).body("Not implemented")
    }

    @GetMapping("/{planId}/members")
    fun getMembers(
        @PathVariable planId: UUID,
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<Any> {
        logger.info("GET /api/plans/{}/members", planId)
        return ResponseEntity.status(501).body("Not implemented")
    }

    @PostMapping("/{planId}/members")
    fun addMember(
        @PathVariable planId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: AddMemberRequest
    ): ResponseEntity<Any> {
        logger.info("POST /api/plans/{}/members", planId)
        return ResponseEntity.status(501).body("Not implemented")
    }

    @DeleteMapping("/{planId}/members/{memberId}")
    fun removeMember(
        @PathVariable planId: UUID,
        @PathVariable memberId: UUID,
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<Any> {
        logger.info("DELETE /api/plans/{}/members/{}", planId, memberId)
        return ResponseEntity.status(501).body("Not implemented")
    }
}
