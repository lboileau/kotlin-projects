package com.acme.services.camperservice.features.plan.controller

import com.acme.clients.common.Result
import com.acme.services.camperservice.common.error.toResponseEntity
import com.acme.services.camperservice.features.plan.dto.AddMemberRequest
import com.acme.services.camperservice.features.plan.dto.CreatePlanRequest
import com.acme.services.camperservice.features.plan.dto.UpdateMemberRoleRequest
import com.acme.services.camperservice.features.plan.dto.UpdatePlanRequest
import com.acme.services.camperservice.features.plan.mapper.PlanMapper
import com.acme.services.camperservice.features.plan.params.*
import com.acme.services.camperservice.features.plan.service.PlanService
import com.acme.services.camperservice.websocket.PlanEventPublisher
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/plans")
class PlanController(
    private val planService: PlanService,
    private val eventPublisher: PlanEventPublisher,
) {
    private val logger = LoggerFactory.getLogger(PlanController::class.java)

    @PostMapping
    fun create(
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: CreatePlanRequest
    ): ResponseEntity<Any> {
        logger.info("POST /api/plans")
        val param = CreatePlanParam(name = request.name, userId = userId)
        return planService.create(param).toResponseEntity(successStatus = 201) { PlanMapper.toResponse(it) }
    }

    @GetMapping
    fun getPlans(@RequestHeader("X-User-Id") userId: UUID): ResponseEntity<Any> {
        logger.info("GET /api/plans")
        val param = GetPlansParam(userId = userId)
        return planService.getPlans(param).toResponseEntity { list -> list.map { PlanMapper.toResponse(it) } }
    }

    @PutMapping("/{planId}")
    fun update(
        @PathVariable planId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: UpdatePlanRequest
    ): ResponseEntity<Any> {
        logger.info("PUT /api/plans/{}", planId)
        val param = UpdatePlanParam(planId = planId, name = request.name, visibility = request.visibility, userId = userId)
        val result = planService.update(param)
        if (result is Result.Success) eventPublisher.publishUpdate(planId, "plan", "updated")
        return result.toResponseEntity { PlanMapper.toResponse(it) }
    }

    @DeleteMapping("/{planId}")
    fun delete(
        @PathVariable planId: UUID,
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<Any> {
        logger.info("DELETE /api/plans/{}", planId)
        val param = DeletePlanParam(planId = planId, userId = userId)
        val result = planService.delete(param)
        if (result is Result.Success) eventPublisher.publishUpdate(planId, "plan", "deleted")
        return result.toResponseEntity(successStatus = 204) { }
    }

    @GetMapping("/{planId}/members")
    fun getMembers(
        @PathVariable planId: UUID,
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<Any> {
        logger.info("GET /api/plans/{}/members", planId)
        val param = GetPlanMembersParam(planId = planId)
        return planService.getMembers(param).toResponseEntity { list -> list.map { PlanMapper.toResponse(it) } }
    }

    @PostMapping("/{planId}/members")
    fun addMember(
        @PathVariable planId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: AddMemberRequest
    ): ResponseEntity<Any> {
        logger.info("POST /api/plans/{}/members", planId)
        val param = AddPlanMemberParam(planId = planId, email = request.email, requestingUserId = userId)
        val result = planService.addMember(param)
        if (result is Result.Success) eventPublisher.publishUpdate(planId, "members", "updated")
        return result.toResponseEntity(successStatus = 201) { PlanMapper.toResponse(it) }
    }

    @PatchMapping("/{planId}/members/{userId}/role")
    fun updateMemberRole(
        @PathVariable planId: UUID,
        @PathVariable userId: UUID,
        @RequestHeader("X-User-Id") requestingUserId: UUID,
        @RequestBody request: UpdateMemberRoleRequest
    ): ResponseEntity<Any> {
        logger.info("PATCH /api/plans/{}/members/{}/role", planId, userId)
        val param = UpdateMemberRoleParam(planId = planId, userId = userId, role = request.role, requestingUserId = requestingUserId)
        val result = planService.updateMemberRole(param)
        if (result is Result.Success) eventPublisher.publishUpdate(planId, "members", "updated")
        return result.toResponseEntity { PlanMapper.toResponse(it) }
    }

    @DeleteMapping("/{planId}/members/{memberId}")
    fun removeMember(
        @PathVariable planId: UUID,
        @PathVariable memberId: UUID,
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<Any> {
        logger.info("DELETE /api/plans/{}/members/{}", planId, memberId)
        val param = RemovePlanMemberParam(planId = planId, userId = memberId, requestingUserId = userId)
        val result = planService.removeMember(param)
        if (result is Result.Success) eventPublisher.publishUpdate(planId, "members", "updated")
        return result.toResponseEntity(successStatus = 204) { }
    }
}
