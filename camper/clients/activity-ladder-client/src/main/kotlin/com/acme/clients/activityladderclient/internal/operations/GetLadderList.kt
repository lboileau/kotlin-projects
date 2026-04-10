package com.acme.clients.activityladderclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.activityladderclient.api.GetLadderListParam
import com.acme.clients.activityladderclient.internal.adapters.LadderRowAdapter
import com.acme.clients.activityladderclient.internal.validations.ValidateGetLadderList
import com.acme.clients.activityladderclient.model.Ladder
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetLadderList(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetLadderList::class.java)
    private val validate = ValidateGetLadderList()

    fun execute(param: GetLadderListParam): Result<List<Ladder>, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Listing ladders limit={} offset={}", param.limit, param.offset)
        val ladders = jdbi.withHandle<List<Ladder>, Exception> { handle ->
            fetchList(handle, param)
        }
        return success(ladders)
    }

    companion object {
        private const val MAX_LIMIT = 200

        fun fetchList(handle: Handle, param: GetLadderListParam): List<Ladder> {
            val effectiveLimit = minOf(param.limit ?: MAX_LIMIT, MAX_LIMIT)
            val sql = buildString {
                append(
                    """
                    SELECT id, creator_id, title, status, current_round_number,
                        current_match_activity_a_id, current_match_activity_b_id, is_final_round,
                        is_grand_final_reset, winner_activity_id, created_at, updated_at
                    FROM activity_ladders
                    ORDER BY created_at DESC
                    LIMIT :limit
                    """.trimIndent()
                )
                if (param.offset != null) append("\nOFFSET :offset")
            }
            val query = handle.createQuery(sql)
            query.bind("limit", effectiveLimit)
            if (param.offset != null) query.bind("offset", param.offset)
            return query.map { rs, _ -> LadderRowAdapter.fromResultSet(rs) }.list()
        }
    }
}
