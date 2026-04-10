package com.acme.clients.activityladderclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.activityladderclient.api.GetVotesForRoundParam
import com.acme.clients.activityladderclient.internal.adapters.LadderVoteRowAdapter
import com.acme.clients.activityladderclient.internal.validations.ValidateGetVotesForRound
import com.acme.clients.activityladderclient.model.LadderVote
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetVotesForRound(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetVotesForRound::class.java)
    private val validate = ValidateGetVotesForRound()

    fun execute(param: GetVotesForRoundParam): Result<List<LadderVote>, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Fetching votes for ladder id={} round={}", param.ladderId, param.roundNumber)
        val votes = jdbi.withHandle<List<LadderVote>, Exception> { handle ->
            fetchForRound(handle, param)
        }
        return success(votes)
    }

    companion object {
        fun fetchForRound(handle: Handle, param: GetVotesForRoundParam): List<LadderVote> =
            handle.createQuery(
                """
                SELECT id, ladder_id, round_number, user_id, voted_for_activity_id, created_at
                FROM ladder_votes
                WHERE ladder_id = :ladderId AND round_number = :roundNumber
                """.trimIndent()
            )
                .bind("ladderId", param.ladderId)
                .bind("roundNumber", param.roundNumber)
                .map { rs, _ -> LadderVoteRowAdapter.fromResultSet(rs) }
                .list()
    }
}
