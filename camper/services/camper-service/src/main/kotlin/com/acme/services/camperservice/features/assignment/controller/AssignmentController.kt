package com.acme.services.camperservice.features.assignment.controller

import com.acme.clients.common.Result
import com.acme.services.camperservice.common.error.toResponseEntity
import com.acme.services.camperservice.features.assignment.dto.AddAssignmentMemberRequest
import com.acme.services.camperservice.features.assignment.dto.CreateAssignmentRequest
import com.acme.services.camperservice.features.assignment.dto.TransferOwnershipRequest
import com.acme.services.camperservice.features.assignment.dto.UpdateAssignmentRequest
import com.acme.services.camperservice.features.assignment.params.*
import com.acme.services.camperservice.features.assignment.service.AssignmentService
import com.acme.services.camperservice.websocket.PlanEventPublisher
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/plans/{planId}/assignments")
class AssignmentController(
    private val assignmentService: AssignmentService,
    private val eventPublisher: PlanEventPublisher,
) {
    private val logger = LoggerFactory.getLogger(AssignmentController::class.java)

    @PostMapping
    fun create(
        @PathVariable planId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: CreateAssignmentRequest
    ): ResponseEntity<Any> {
        logger.info("POST /api/plans/{}/assignments", planId)
        val param = CreateAssignmentParam(
            planId = planId,
            name = request.name,
            type = request.type,
            maxOccupancy = request.maxOccupancy,
            userId = userId
        )
        val result = assignmentService.create(param)
        if (result is Result.Success) eventPublisher.publishUpdate(planId, "assignments", "updated")
        return result.toResponseEntity(successStatus = 201) { it }
    }

    @GetMapping
    fun getAssignments(
        @PathVariable planId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestParam(required = false) type: String?
    ): ResponseEntity<Any> {
        logger.info("GET /api/plans/{}/assignments", planId)
        val param = GetAssignmentsParam(planId = planId, type = type)
        return assignmentService.getAssignments(param).toResponseEntity { it }
    }

    @GetMapping("/{assignmentId}")
    fun getAssignment(
        @PathVariable planId: UUID,
        @PathVariable assignmentId: UUID,
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<Any> {
        logger.info("GET /api/plans/{}/assignments/{}", planId, assignmentId)
        val param = GetAssignmentParam(assignmentId = assignmentId)
        return assignmentService.getAssignment(param).toResponseEntity { it }
    }

    @PutMapping("/{assignmentId}")
    fun update(
        @PathVariable planId: UUID,
        @PathVariable assignmentId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: UpdateAssignmentRequest
    ): ResponseEntity<Any> {
        logger.info("PUT /api/plans/{}/assignments/{}", planId, assignmentId)
        val param = UpdateAssignmentParam(
            assignmentId = assignmentId,
            name = request.name,
            maxOccupancy = request.maxOccupancy,
            userId = userId
        )
        val result = assignmentService.update(param)
        if (result is Result.Success) eventPublisher.publishUpdate(planId, "assignments", "updated")
        return result.toResponseEntity { it }
    }

    @DeleteMapping("/{assignmentId}")
    fun delete(
        @PathVariable planId: UUID,
        @PathVariable assignmentId: UUID,
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<Any> {
        logger.info("DELETE /api/plans/{}/assignments/{}", planId, assignmentId)
        val param = DeleteAssignmentParam(assignmentId = assignmentId, userId = userId)
        val result = assignmentService.delete(param)
        if (result is Result.Success) eventPublisher.publishUpdate(planId, "assignments", "updated")
        return result.toResponseEntity(successStatus = 204) { }
    }

    @PostMapping("/{assignmentId}/members")
    fun addMember(
        @PathVariable planId: UUID,
        @PathVariable assignmentId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: AddAssignmentMemberRequest
    ): ResponseEntity<Any> {
        logger.info("POST /api/plans/{}/assignments/{}/members", planId, assignmentId)
        val param = AddAssignmentMemberParam(
            assignmentId = assignmentId,
            memberUserId = request.userId,
            userId = userId
        )
        val result = assignmentService.addMember(param)
        if (result is Result.Success) eventPublisher.publishUpdate(planId, "assignments", "updated")
        return result.toResponseEntity(successStatus = 201) { it }
    }

    @DeleteMapping("/{assignmentId}/members/{memberUserId}")
    fun removeMember(
        @PathVariable planId: UUID,
        @PathVariable assignmentId: UUID,
        @PathVariable memberUserId: UUID,
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<Any> {
        logger.info("DELETE /api/plans/{}/assignments/{}/members/{}", planId, assignmentId, memberUserId)
        val param = RemoveAssignmentMemberParam(
            assignmentId = assignmentId,
            memberUserId = memberUserId,
            userId = userId
        )
        val result = assignmentService.removeMember(param)
        if (result is Result.Success) eventPublisher.publishUpdate(planId, "assignments", "updated")
        return result.toResponseEntity(successStatus = 204) { }
    }

    @PutMapping("/{assignmentId}/owner")
    fun transferOwnership(
        @PathVariable planId: UUID,
        @PathVariable assignmentId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: TransferOwnershipRequest
    ): ResponseEntity<Any> {
        logger.info("PUT /api/plans/{}/assignments/{}/owner", planId, assignmentId)
        val param = TransferOwnershipParam(
            assignmentId = assignmentId,
            newOwnerId = request.newOwnerId,
            userId = userId
        )
        val result = assignmentService.transferOwnership(param)
        if (result is Result.Success) eventPublisher.publishUpdate(planId, "assignments", "updated")
        return result.toResponseEntity { it }
    }
}
