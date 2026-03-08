package com.acme.clients.assignmentclient.api

import java.util.UUID

/** Parameter for creating a new assignment. */
data class CreateAssignmentParam(
    val planId: UUID,
    val name: String,
    val type: String,
    val maxOccupancy: Int,
    val ownerId: UUID
)

/** Parameter for retrieving an assignment by ID. */
data class GetByIdParam(val id: UUID)

/** Parameter for retrieving assignments by plan ID, with optional type filter. */
data class GetByPlanIdParam(val planId: UUID, val type: String? = null)

/** Parameter for updating an existing assignment. Null fields are left unchanged. */
data class UpdateAssignmentParam(val id: UUID, val name: String?, val maxOccupancy: Int?)

/** Parameter for deleting an assignment by ID. */
data class DeleteAssignmentParam(val id: UUID)

/** Parameter for adding a member to an assignment. */
data class AddAssignmentMemberParam(
    val assignmentId: UUID,
    val userId: UUID,
    val planId: UUID,
    val assignmentType: String
)

/** Parameter for removing a member from an assignment. */
data class RemoveAssignmentMemberParam(val assignmentId: UUID, val userId: UUID)

/** Parameter for retrieving members of an assignment. */
data class GetAssignmentMembersParam(val assignmentId: UUID)

/** Parameter for transferring ownership of an assignment. */
data class TransferOwnershipParam(val id: UUID, val newOwnerId: UUID)
