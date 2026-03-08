package com.acme.clients.assignmentclient.fake

import com.acme.clients.assignmentclient.api.*
import com.acme.clients.assignmentclient.internal.validations.ValidateCreateAssignment
import com.acme.clients.assignmentclient.internal.validations.ValidateDeleteAssignment
import com.acme.clients.assignmentclient.internal.validations.ValidateGetAssignmentById
import com.acme.clients.assignmentclient.internal.validations.ValidateUpdateAssignment
import com.acme.clients.assignmentclient.model.Assignment
import com.acme.clients.assignmentclient.model.AssignmentMember
import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ConflictError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class FakeAssignmentClient : AssignmentClient {
    private val assignmentStore = ConcurrentHashMap<UUID, Assignment>()
    private val memberStore = mutableListOf<AssignmentMember>()

    private val validateGetById = ValidateGetAssignmentById()
    private val validateCreate = ValidateCreateAssignment()
    private val validateUpdate = ValidateUpdateAssignment()
    private val validateDelete = ValidateDeleteAssignment()

    override fun create(param: CreateAssignmentParam): Result<Assignment, AppError> {
        val validation = validateCreate.execute(param)
        if (validation is Result.Failure) return validation

        if (assignmentStore.values.any { it.planId == param.planId && it.name == param.name && it.type == param.type }) {
            return failure(ConflictError("Assignment", "name '${param.name}' already exists for this plan and type"))
        }
        val entity = Assignment(
            id = UUID.randomUUID(),
            planId = param.planId,
            name = param.name,
            type = param.type,
            maxOccupancy = param.maxOccupancy,
            ownerId = param.ownerId,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        assignmentStore[entity.id] = entity
        return success(entity)
    }

    override fun getById(param: GetByIdParam): Result<Assignment, AppError> {
        val validation = validateGetById.execute(param)
        if (validation is Result.Failure) return validation

        val entity = assignmentStore[param.id]
        return if (entity != null) success(entity) else failure(NotFoundError("Assignment", param.id.toString()))
    }

    override fun getByPlanId(param: GetByPlanIdParam): Result<List<Assignment>, AppError> {
        var assignments = assignmentStore.values.filter { it.planId == param.planId }
        if (param.type != null) assignments = assignments.filter { it.type == param.type }
        return success(assignments.sortedBy { it.name })
    }

    override fun update(param: UpdateAssignmentParam): Result<Assignment, AppError> {
        val validation = validateUpdate.execute(param)
        if (validation is Result.Failure) return validation

        val existing = assignmentStore[param.id] ?: return failure(NotFoundError("Assignment", param.id.toString()))
        val updatedName = param.name ?: existing.name
        val updatedMaxOccupancy = param.maxOccupancy ?: existing.maxOccupancy

        if (param.name != null && assignmentStore.values.any { it.planId == existing.planId && it.name == updatedName && it.type == existing.type && it.id != param.id }) {
            return failure(ConflictError("Assignment", "name '$updatedName' already exists for this plan and type"))
        }

        val updated = existing.copy(name = updatedName, maxOccupancy = updatedMaxOccupancy, updatedAt = Instant.now())
        assignmentStore[param.id] = updated
        return success(updated)
    }

    override fun delete(param: DeleteAssignmentParam): Result<Unit, AppError> {
        val validation = validateDelete.execute(param)
        if (validation is Result.Failure) return validation

        val removed = assignmentStore.remove(param.id)
        if (removed != null) {
            memberStore.removeAll { it.assignmentId == param.id }
            return success(Unit)
        }
        return failure(NotFoundError("Assignment", param.id.toString()))
    }

    override fun addMember(param: AddAssignmentMemberParam): Result<AssignmentMember, AppError> {
        if (memberStore.any { it.assignmentId == param.assignmentId && it.userId == param.userId }) {
            return failure(ConflictError("AssignmentMember", "already_member"))
        }
        if (memberStore.any { it.planId == param.planId && it.userId == param.userId && it.assignmentType == param.assignmentType }) {
            return failure(ConflictError("AssignmentMember", "already_assigned_type"))
        }
        val member = AssignmentMember(
            assignmentId = param.assignmentId,
            userId = param.userId,
            planId = param.planId,
            assignmentType = param.assignmentType,
            createdAt = Instant.now()
        )
        memberStore.add(member)
        return success(member)
    }

    override fun removeMember(param: RemoveAssignmentMemberParam): Result<Unit, AppError> {
        val removed = memberStore.removeAll { it.assignmentId == param.assignmentId && it.userId == param.userId }
        return if (removed) success(Unit) else failure(NotFoundError("AssignmentMember", "${param.assignmentId}/${param.userId}"))
    }

    override fun getMembers(param: GetAssignmentMembersParam): Result<List<AssignmentMember>, AppError> {
        val members = memberStore.filter { it.assignmentId == param.assignmentId }.sortedBy { it.createdAt }
        return success(members)
    }

    override fun transferOwnership(param: TransferOwnershipParam): Result<Assignment, AppError> {
        val existing = assignmentStore[param.id] ?: return failure(NotFoundError("Assignment", param.id.toString()))
        val updated = existing.copy(ownerId = param.newOwnerId, updatedAt = Instant.now())
        assignmentStore[param.id] = updated
        return success(updated)
    }

    fun reset() {
        assignmentStore.clear()
        memberStore.clear()
    }

    fun seedAssignment(vararg entities: Assignment) {
        entities.forEach { assignmentStore[it.id] = it }
    }

    fun seedMember(vararg entities: AssignmentMember) {
        entities.forEach { memberStore.add(it) }
    }
}
