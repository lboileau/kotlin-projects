package com.acme.services.camperservice.features.plan.params

import java.util.UUID

data class CreatePlanParam(val name: String, val userId: UUID)

data class GetPlansParam(val userId: UUID)

data class UpdatePlanParam(val planId: UUID, val name: String, val userId: UUID)

data class DeletePlanParam(val planId: UUID, val userId: UUID)

data class GetPlanMembersParam(val planId: UUID)

data class AddPlanMemberParam(val planId: UUID, val email: String)

data class RemovePlanMemberParam(val planId: UUID, val userId: UUID, val requestingUserId: UUID)
