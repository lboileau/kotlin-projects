package com.acme.clients.activityladderclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.activityladderclient.api.GetLadderActivitiesParam
import com.acme.clients.activityladderclient.internal.adapters.LadderActivityRowAdapter
import com.acme.clients.activityladderclient.internal.validations.ValidateGetLadderActivities
import com.acme.clients.activityladderclient.model.LadderActivity
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetLadderActivities(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetLadderActivities::class.java)
    private val validate = ValidateGetLadderActivities()

    fun execute(param: GetLadderActivitiesParam): Result<List<LadderActivity>, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Fetching activities for ladder id={}", param.ladderId)
        val activities = jdbi.withHandle<List<LadderActivity>, Exception> { handle ->
            fetchByLadderId(handle, param)
        }
        return success(activities)
    }

    companion object {
        fun fetchByLadderId(handle: Handle, param: GetLadderActivitiesParam): List<LadderActivity> =
            handle.createQuery(
                """
                SELECT id, ladder_id, name, image_url, distance_minutes, cost_per_person,
                    losses, bracket, display_order, created_at
                FROM ladder_activities
                WHERE ladder_id = :ladderId
                ORDER BY display_order
                """.trimIndent()
            )
                .bind("ladderId", param.ladderId)
                .map { rs, _ -> LadderActivityRowAdapter.fromResultSet(rs) }
                .list()
    }
}
