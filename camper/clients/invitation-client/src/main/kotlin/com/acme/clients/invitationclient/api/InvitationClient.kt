package com.acme.clients.invitationclient.api

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.invitationclient.model.Invitation

/**
 * Client interface for Invitation entity operations.
 *
 * All operations return [Result] to represent success or typed failure
 * without throwing exceptions for expected error conditions.
 */
interface InvitationClient {
    /** Create or update an invitation (upserts on plan_id + user_id). */
    fun upsert(param: UpsertInvitationParam): Result<Invitation, AppError>

    /** Retrieve all invitations for a plan. */
    fun getByPlanId(param: GetByPlanIdParam): Result<List<Invitation>, AppError>

    /** Retrieve an invitation by plan and user. */
    fun getByPlanIdAndUserId(param: GetByPlanIdAndUserIdParam): Result<Invitation?, AppError>

    /** Retrieve an invitation by its Resend email ID. */
    fun getByResendEmailId(param: GetByResendEmailIdParam): Result<Invitation?, AppError>

    /** Update the status of an invitation. */
    fun updateStatus(param: UpdateStatusParam): Result<Invitation, AppError>
}
