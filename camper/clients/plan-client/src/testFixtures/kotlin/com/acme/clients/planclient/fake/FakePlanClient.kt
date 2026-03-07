package com.acme.clients.planclient.fake

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ConflictError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.planclient.api.*
import com.acme.clients.planclient.internal.validations.ValidateCreatePlan
import com.acme.clients.planclient.internal.validations.ValidateDeletePlan
import com.acme.clients.planclient.internal.validations.ValidateGetPlanById
import com.acme.clients.planclient.internal.validations.ValidateUpdatePlan
import com.acme.clients.planclient.model.Plan
import com.acme.clients.planclient.model.PlanMember
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class FakePlanClient : PlanClient {
    private val planStore = ConcurrentHashMap<UUID, Plan>()
    private val memberStore = mutableListOf<PlanMember>()

    private val validateGetById = ValidateGetPlanById()
    private val validateCreate = ValidateCreatePlan()
    private val validateUpdate = ValidateUpdatePlan()
    private val validateDelete = ValidateDeletePlan()

    override fun getById(param: GetByIdParam): Result<Plan, AppError> {
        val validation = validateGetById.execute(param)
        if (validation is Result.Failure) return validation

        val entity = planStore[param.id]
        return if (entity != null) success(entity) else failure(NotFoundError("Plan", param.id.toString()))
    }

    override fun getByUserId(param: GetByUserIdParam): Result<List<Plan>, AppError> {
        val planIds = memberStore.filter { it.userId == param.userId }.map { it.planId }.toSet()
        val plans = planStore.values.filter { it.id in planIds }.sortedByDescending { it.createdAt }
        return success(plans)
    }

    override fun getPublicPlans(param: GetPublicPlansParam): Result<List<Plan>, AppError> {
        val plans = planStore.values.filter { it.visibility == "public" }.sortedByDescending { it.createdAt }
        return success(plans)
    }

    override fun create(param: CreatePlanParam): Result<Plan, AppError> {
        val validation = validateCreate.execute(param)
        if (validation is Result.Failure) return validation

        val entity = Plan(
            id = UUID.randomUUID(),
            name = param.name,
            visibility = param.visibility,
            ownerId = param.ownerId,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        planStore[entity.id] = entity
        return success(entity)
    }

    override fun update(param: UpdatePlanParam): Result<Plan, AppError> {
        val validation = validateUpdate.execute(param)
        if (validation is Result.Failure) return validation

        val existing = planStore[param.id] ?: return failure(NotFoundError("Plan", param.id.toString()))
        val updated = existing.copy(name = param.name, updatedAt = Instant.now())
        planStore[param.id] = updated
        return success(updated)
    }

    override fun delete(param: DeletePlanParam): Result<Unit, AppError> {
        val validation = validateDelete.execute(param)
        if (validation is Result.Failure) return validation

        val removed = planStore.remove(param.id)
        if (removed != null) {
            memberStore.removeAll { it.planId == param.id }
            return success(Unit)
        }
        return failure(NotFoundError("Plan", param.id.toString()))
    }

    override fun getMembers(param: GetMembersParam): Result<List<PlanMember>, AppError> {
        val members = memberStore.filter { it.planId == param.planId }.sortedBy { it.createdAt }
        return success(members)
    }

    override fun addMember(param: AddMemberParam): Result<PlanMember, AppError> {
        if (memberStore.any { it.planId == param.planId && it.userId == param.userId }) {
            return failure(ConflictError("PlanMember", "user is already a member of this plan"))
        }
        val member = PlanMember(planId = param.planId, userId = param.userId, createdAt = Instant.now())
        memberStore.add(member)
        return success(member)
    }

    override fun removeMember(param: RemoveMemberParam): Result<Unit, AppError> {
        val removed = memberStore.removeAll { it.planId == param.planId && it.userId == param.userId }
        return if (removed) success(Unit) else failure(NotFoundError("PlanMember", "${param.planId}/${param.userId}"))
    }

    fun reset() {
        planStore.clear()
        memberStore.clear()
    }

    fun seedPlan(vararg entities: Plan) {
        entities.forEach { planStore[it.id] = it }
    }

    fun seedMember(vararg entities: PlanMember) {
        entities.forEach { memberStore.add(it) }
    }
}
