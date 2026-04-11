package com.acme.clients.activityladderclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.activityladderclient.api.AddLadderActivityParam
import com.acme.clients.activityladderclient.internal.adapters.LadderActivityRowAdapter
import com.acme.clients.activityladderclient.internal.validations.ValidateAddLadderActivity
import com.acme.clients.activityladderclient.model.LadderActivity
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

internal class AddLadderActivity(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(AddLadderActivity::class.java)
    private val validate = ValidateAddLadderActivity()

    fun execute(param: AddLadderActivityParam): Result<LadderActivity, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Adding activity to ladder id={} name={}", param.ladderId, param.name)
        val activity = jdbi.withHandle<LadderActivity, Exception> { handle ->
            insert(handle, param)
        }
        return success(activity)
    }

    companion object {
        fun insert(handle: Handle, param: AddLadderActivityParam): LadderActivity {
            val id = UUID.randomUUID()
            val now = Instant.now()
            handle.createUpdate(
                """
                INSERT INTO ladder_activities (id, ladder_id, name, image_url, distance_minutes, cost_per_person,
                    losses, bracket, display_order, created_at)
                VALUES (:id, :ladderId, :name, :imageUrl, :distanceMinutes, :costPerPerson,
                    0, 'WINNERS',
                    COALESCE((SELECT MAX(display_order) FROM ladder_activities WHERE ladder_id = :ladderId), 0) + 1,
                    :createdAt)
                """.trimIndent()
            )
                .bind("id", id)
                .bind("ladderId", param.ladderId)
                .bind("name", param.name)
                .bind("imageUrl", param.imageUrl)
                .bind("distanceMinutes", param.distanceMinutes)
                .bind("costPerPerson", param.costPerPerson)
                .bind("createdAt", now)
                .execute()

            return handle.createQuery(
                """
                SELECT id, ladder_id, name, image_url, distance_minutes, cost_per_person,
                    losses, bracket, display_order, created_at
                FROM ladder_activities WHERE id = :id
                """.trimIndent()
            )
                .bind("id", id)
                .map { rs, _ -> LadderActivityRowAdapter.fromResultSet(rs) }
                .one()
        }
    }
}
