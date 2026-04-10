package com.acme.services.camperservice.features.activityladder.actions

import com.acme.clients.activityladderclient.api.ActivityLadderClient
import com.acme.clients.activityladderclient.api.GetLadderByIdParam
import com.acme.clients.activityladderclient.api.RestartLadderParam as ClientRestartLadderParam
import com.acme.clients.common.Result
import com.acme.services.camperservice.features.activityladder.error.LadderError
import com.acme.services.camperservice.features.activityladder.mapper.ActivityLadderMapper
import com.acme.services.camperservice.features.activityladder.model.Ladder
import com.acme.services.camperservice.features.activityladder.model.LadderStatus
import com.acme.services.camperservice.features.activityladder.params.RestartLadderParam
import com.acme.services.camperservice.features.activityladder.validations.ValidateRestartLadder
import org.slf4j.LoggerFactory

internal class RestartLadderAction(
    private val ladderClient: ActivityLadderClient,
) {
    private val logger = LoggerFactory.getLogger(RestartLadderAction::class.java)
    private val validate = ValidateRestartLadder()

    fun execute(param: RestartLadderParam): Result<Ladder, LadderError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Restarting ladder ladderId={} userId={}", param.ladderId, param.requestingUserId)

        val ladderResult = ladderClient.getLadderById(GetLadderByIdParam(param.ladderId))
        val ladder = when (ladderResult) {
            is Result.Success -> ActivityLadderMapper.fromClient(ladderResult.value)
            is Result.Failure -> return Result.Failure(LadderError.fromClientError(ladderResult.error))
        }

        if (ladder.creatorId != param.requestingUserId) {
            return Result.Failure(LadderError.NotCreator(param.ladderId, param.requestingUserId))
        }
        if (ladder.status == LadderStatus.DRAFT) {
            return Result.Failure(LadderError.IllegalState("ACTIVE or COMPLETED", ladder.status.name))
        }

        return when (val result = ladderClient.restartLadder(ClientRestartLadderParam(ladderId = param.ladderId))) {
            is Result.Success -> Result.Success(ActivityLadderMapper.fromClient(result.value))
            is Result.Failure -> Result.Failure(LadderError.fromClientError(result.error))
        }
    }
}
