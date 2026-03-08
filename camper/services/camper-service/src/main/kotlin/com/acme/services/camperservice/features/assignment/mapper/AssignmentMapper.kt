package com.acme.services.camperservice.features.assignment.mapper

import com.acme.clients.assignmentclient.model.Assignment as ClientAssignment
import com.acme.clients.assignmentclient.model.AssignmentMember as ClientAssignmentMember
import com.acme.services.camperservice.features.assignment.dto.AssignmentDetailResponse
import com.acme.services.camperservice.features.assignment.dto.AssignmentMemberResponse
import com.acme.services.camperservice.features.assignment.dto.AssignmentResponse
import com.acme.services.camperservice.features.assignment.model.Assignment
import com.acme.services.camperservice.features.assignment.model.AssignmentDetail
import com.acme.services.camperservice.features.assignment.model.AssignmentMember

object AssignmentMapper {

    fun fromClient(clientAssignment: ClientAssignment): Assignment = Assignment(
        id = clientAssignment.id,
        planId = clientAssignment.planId,
        name = clientAssignment.name,
        type = clientAssignment.type,
        maxOccupancy = clientAssignment.maxOccupancy,
        ownerId = clientAssignment.ownerId,
        createdAt = clientAssignment.createdAt,
        updatedAt = clientAssignment.updatedAt
    )

    fun fromClient(clientMember: ClientAssignmentMember, username: String? = null): AssignmentMember = AssignmentMember(
        assignmentId = clientMember.assignmentId,
        userId = clientMember.userId,
        username = username,
        createdAt = clientMember.createdAt
    )

    fun toResponse(assignment: Assignment): AssignmentResponse = AssignmentResponse(
        id = assignment.id,
        planId = assignment.planId,
        name = assignment.name,
        type = assignment.type,
        maxOccupancy = assignment.maxOccupancy,
        ownerId = assignment.ownerId,
        createdAt = assignment.createdAt,
        updatedAt = assignment.updatedAt
    )

    fun toDetailResponse(assignment: Assignment, members: List<AssignmentMember>): AssignmentDetailResponse = AssignmentDetailResponse(
        id = assignment.id,
        planId = assignment.planId,
        name = assignment.name,
        type = assignment.type,
        maxOccupancy = assignment.maxOccupancy,
        ownerId = assignment.ownerId,
        members = members.map { toResponse(it) },
        createdAt = assignment.createdAt,
        updatedAt = assignment.updatedAt
    )

    fun toResponse(detail: AssignmentDetail): AssignmentDetailResponse = AssignmentDetailResponse(
        id = detail.id,
        planId = detail.planId,
        name = detail.name,
        type = detail.type,
        maxOccupancy = detail.maxOccupancy,
        ownerId = detail.ownerId,
        members = detail.members.map { toResponse(it) },
        createdAt = detail.createdAt,
        updatedAt = detail.updatedAt
    )

    fun toResponse(member: AssignmentMember): AssignmentMemberResponse = AssignmentMemberResponse(
        assignmentId = member.assignmentId,
        userId = member.userId,
        username = member.username,
        createdAt = member.createdAt
    )
}
