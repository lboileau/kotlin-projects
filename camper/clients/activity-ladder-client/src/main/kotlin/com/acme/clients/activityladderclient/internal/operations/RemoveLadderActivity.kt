package com.acme.clients.activityladderclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.activityladderclient.api.RemoveLadderActivityParam
import com.acme.clients.activityladderclient.internal.validations.ValidateRemoveLadderActivity
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class RemoveLadderActivity(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(RemoveLadderActivity::class.java)
    private val validate = ValidateRemoveLadderActivity()

    fun execute(param: RemoveLadderActivityParam): Result<Unit, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Removing activity id={} from ladder id={}", param.activityId, param.ladderId)
        val rowsAffected = jdbi.withHandle<Int, Exception> { handle ->
            delete(handle, param)
        }
        return if (rowsAffected > 0) success(Unit) else failure(NotFoundError("LadderActivity", param.activityId.toString()))
    }

    companion object {
        fun delete(handle: Handle, param: RemoveLadderActivityParam): Int =
            handle.createUpdate(
                "DELETE FROM ladder_activities WHERE id = :id AND ladder_id = :ladderId"
            )
                .bind("id", param.activityId)
                .bind("ladderId", param.ladderId)
                .execute()
    }
}
