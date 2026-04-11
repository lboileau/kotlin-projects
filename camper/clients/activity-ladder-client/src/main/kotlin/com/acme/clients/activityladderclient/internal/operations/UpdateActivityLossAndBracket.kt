package com.acme.clients.activityladderclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.activityladderclient.api.UpdateActivityLossAndBracketParam
import com.acme.clients.activityladderclient.internal.adapters.LadderActivityRowAdapter
import com.acme.clients.activityladderclient.internal.validations.ValidateUpdateActivityLossAndBracket
import com.acme.clients.activityladderclient.model.LadderActivity
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class UpdateActivityLossAndBracket(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(UpdateActivityLossAndBracket::class.java)
    private val validate = ValidateUpdateActivityLossAndBracket()

    fun execute(param: UpdateActivityLossAndBracketParam): Result<LadderActivity, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Updating activity id={} losses={} bracket={}", param.activityId, param.losses, param.bracket)
        val activity = jdbi.withHandle<LadderActivity?, Exception> { handle ->
            update(handle, param)
        }
        return if (activity != null) success(activity) else failure(NotFoundError("LadderActivity", param.activityId.toString()))
    }

    companion object {
        fun update(handle: Handle, param: UpdateActivityLossAndBracketParam): LadderActivity? {
            val rowsAffected = handle.createUpdate(
                """
                UPDATE ladder_activities
                SET losses = :losses, bracket = :bracket
                WHERE id = :id
                """.trimIndent()
            )
                .bind("id", param.activityId)
                .bind("losses", param.losses)
                .bind("bracket", param.bracket.name)
                .execute()

            if (rowsAffected == 0) return null

            return handle.createQuery(
                """
                SELECT id, ladder_id, name, image_url, distance_minutes, cost_per_person,
                    losses, bracket, display_order, created_at
                FROM ladder_activities WHERE id = :id
                """.trimIndent()
            )
                .bind("id", param.activityId)
                .map { rs, _ -> LadderActivityRowAdapter.fromResultSet(rs) }
                .findOne()
                .orElse(null)
        }
    }
}
