package com.acme.services.camperservice.features.plan.mapper

import com.acme.clients.planclient.model.Plan as ClientPlan
import com.acme.clients.planclient.model.PlanMember as ClientPlanMember
import com.acme.libs.avatargenerator.AvatarGenerator
import com.acme.services.camperservice.features.plan.dto.PlanMemberResponse
import com.acme.services.camperservice.features.plan.dto.PlanResponse
import com.acme.services.camperservice.features.plan.model.Plan
import com.acme.services.camperservice.features.plan.model.PlanMember
import com.acme.services.camperservice.features.user.mapper.AvatarMapper

object PlanMapper {

    fun fromClient(clientPlan: ClientPlan): Plan = Plan(
        id = clientPlan.id,
        name = clientPlan.name,
        visibility = clientPlan.visibility,
        ownerId = clientPlan.ownerId,
        createdAt = clientPlan.createdAt,
        updatedAt = clientPlan.updatedAt
    )

    fun fromClient(
        clientMember: ClientPlanMember,
        username: String? = null,
        email: String? = null,
        invitationStatus: String? = null,
        avatarSeed: String? = null
    ): PlanMember = PlanMember(
        planId = clientMember.planId,
        userId = clientMember.userId,
        username = username,
        email = email,
        invitationStatus = invitationStatus,
        role = clientMember.role,
        avatarSeed = avatarSeed,
        createdAt = clientMember.createdAt
    )

    fun toResponse(plan: Plan): PlanResponse = PlanResponse(
        id = plan.id,
        name = plan.name,
        visibility = plan.visibility,
        ownerId = plan.ownerId,
        createdAt = plan.createdAt,
        updatedAt = plan.updatedAt,
        isMember = plan.isMember
    )

    fun toResponse(member: PlanMember): PlanMemberResponse = PlanMemberResponse(
        planId = member.planId,
        userId = member.userId,
        username = member.username,
        email = member.email,
        invitationStatus = member.invitationStatus,
        role = member.role,
        avatarSeed = member.avatarSeed,
        avatar = member.avatarSeed?.let { AvatarMapper.toResponse(AvatarGenerator.generate(it)) },
        createdAt = member.createdAt
    )
}
