package com.acme.services.camperservice.features.assignment.controller

import com.acme.services.camperservice.features.assignment.dto.AddAssignmentMemberRequest
import com.acme.services.camperservice.features.assignment.dto.CreateAssignmentRequest
import com.acme.services.camperservice.features.assignment.dto.TransferOwnershipRequest
import com.acme.services.camperservice.features.assignment.dto.UpdateAssignmentRequest
import com.acme.services.camperservice.features.assignment.service.AssignmentService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/plans/{planId}/assignments")
class AssignmentController(private val assignmentService: AssignmentService) {
    private val logger = LoggerFactory.getLogger(AssignmentController::class.java)

    @PostMapping
    fun create(
        @PathVariable planId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: CreateAssignmentRequest
    ): ResponseEntity<Any> {
        logger.info("POST /api/plans/{}/assignments", planId)
        return ResponseEntity.status(501).body(mapOf("error" to "Not implemented"))
    }

    @GetMapping
    fun getAssignments(
        @PathVariable planId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestParam(required = false) type: String?
    ): ResponseEntity<Any> {
        logger.info("GET /api/plans/{}/assignments", planId)
        return ResponseEntity.status(501).body(mapOf("error" to "Not implemented"))
    }

    @GetMapping("/{assignmentId}")
    fun getAssignment(
        @PathVariable planId: UUID,
        @PathVariable assignmentId: UUID,
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<Any> {
        logger.info("GET /api/plans/{}/assignments/{}", planId, assignmentId)
        return ResponseEntity.status(501).body(mapOf("error" to "Not implemented"))
    }

    @PutMapping("/{assignmentId}")
    fun update(
        @PathVariable planId: UUID,
        @PathVariable assignmentId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: UpdateAssignmentRequest
    ): ResponseEntity<Any> {
        logger.info("PUT /api/plans/{}/assignments/{}", planId, assignmentId)
        return ResponseEntity.status(501).body(mapOf("error" to "Not implemented"))
    }

    @DeleteMapping("/{assignmentId}")
    fun delete(
        @PathVariable planId: UUID,
        @PathVariable assignmentId: UUID,
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<Any> {
        logger.info("DELETE /api/plans/{}/assignments/{}", planId, assignmentId)
        return ResponseEntity.status(501).body(mapOf("error" to "Not implemented"))
    }

    @PostMapping("/{assignmentId}/members")
    fun addMember(
        @PathVariable planId: UUID,
        @PathVariable assignmentId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: AddAssignmentMemberRequest
    ): ResponseEntity<Any> {
        logger.info("POST /api/plans/{}/assignments/{}/members", planId, assignmentId)
        return ResponseEntity.status(501).body(mapOf("error" to "Not implemented"))
    }

    @DeleteMapping("/{assignmentId}/members/{memberUserId}")
    fun removeMember(
        @PathVariable planId: UUID,
        @PathVariable assignmentId: UUID,
        @PathVariable memberUserId: UUID,
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<Any> {
        logger.info("DELETE /api/plans/{}/assignments/{}/members/{}", planId, assignmentId, memberUserId)
        return ResponseEntity.status(501).body(mapOf("error" to "Not implemented"))
    }

    @PutMapping("/{assignmentId}/owner")
    fun transferOwnership(
        @PathVariable planId: UUID,
        @PathVariable assignmentId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: TransferOwnershipRequest
    ): ResponseEntity<Any> {
        logger.info("PUT /api/plans/{}/assignments/{}/owner", planId, assignmentId)
        return ResponseEntity.status(501).body(mapOf("error" to "Not implemented"))
    }
}
