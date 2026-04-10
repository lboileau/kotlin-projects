package com.acme.clients.activityladderclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.activityladderclient.api.CountVotesForRoundParam
import com.acme.clients.activityladderclient.internal.validations.ValidateCountVotesForRound
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class CountVotesForRound(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(CountVotesForRound::class.java)
    private val validate = ValidateCountVotesForRound()

    fun execute(param: CountVotesForRoundParam): Result<Int, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Counting votes for ladder id={} round={}", param.ladderId, param.roundNumber)
        val count = jdbi.withHandle<Int, Exception> { handle ->
            countForRound(handle, param)
        }
        return success(count)
    }

    companion object {
        fun countForRound(handle: Handle, param: CountVotesForRoundParam): Int =
            handle.createQuery(
                """
                SELECT COUNT(*) FROM ladder_votes
                WHERE ladder_id = :ladderId AND round_number = :roundNumber
                """.trimIndent()
            )
                .bind("ladderId", param.ladderId)
                .bind("roundNumber", param.roundNumber)
                .mapTo(Int::class.java)
                .one()
    }
}
