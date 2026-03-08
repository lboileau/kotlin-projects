package com.acme.services.camperservice.features.assignment.dto

import java.util.UUID

data class CreateAssignmentRequest(val name: String, val type: String, val maxOccupancy: Int?)

data class UpdateAssignmentRequest(val name: String?, val maxOccupancy: Int?)

data class AddAssignmentMemberRequest(val userId: UUID)

data class TransferOwnershipRequest(val newOwnerId: UUID)
