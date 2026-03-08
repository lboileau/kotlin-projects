package com.acme.services.camperservice.features.assignment.params

import java.util.UUID

data class CreateAssignmentParam(val planId: UUID, val name: String, val type: String, val maxOccupancy: Int?, val userId: UUID)

data class GetAssignmentsParam(val planId: UUID, val type: String? = null)

data class GetAssignmentParam(val assignmentId: UUID)

data class UpdateAssignmentParam(val assignmentId: UUID, val name: String?, val maxOccupancy: Int?, val userId: UUID)

data class DeleteAssignmentParam(val assignmentId: UUID, val userId: UUID)

data class AddAssignmentMemberParam(val assignmentId: UUID, val memberUserId: UUID, val userId: UUID)

data class RemoveAssignmentMemberParam(val assignmentId: UUID, val memberUserId: UUID, val userId: UUID)

data class TransferOwnershipParam(val assignmentId: UUID, val newOwnerId: UUID, val userId: UUID)
