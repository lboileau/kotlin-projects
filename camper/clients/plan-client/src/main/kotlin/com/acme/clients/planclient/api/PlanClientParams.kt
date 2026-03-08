package com.acme.clients.planclient.api

import java.util.UUID

/** Parameter for retrieving a plan by ID. */
data class GetByIdParam(val id: UUID)

/** Parameter for retrieving plans a user is a member of. */
data class GetByUserIdParam(val userId: UUID)

/** Parameter for retrieving all public plans. */
class GetPublicPlansParam

/** Parameter for creating a new plan. */
data class CreatePlanParam(val name: String, val visibility: String, val ownerId: UUID)

/** Parameter for updating a plan. */
data class UpdatePlanParam(val id: UUID, val name: String, val visibility: String? = null)

/** Parameter for deleting a plan. */
data class DeletePlanParam(val id: UUID)

/** Parameter for retrieving members of a plan. */
data class GetMembersParam(val planId: UUID)

/** Parameter for adding a member to a plan. */
data class AddMemberParam(val planId: UUID, val userId: UUID)

/** Parameter for removing a member from a plan. */
data class RemoveMemberParam(val planId: UUID, val userId: UUID)
