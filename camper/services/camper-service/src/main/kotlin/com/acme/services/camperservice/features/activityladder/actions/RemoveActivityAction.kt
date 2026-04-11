package com.acme.services.camperservice.features.activityladder.actions

import com.acme.clients.activityladderclient.api.ActivityLadderClient
import com.acme.clients.activityladderclient.api.GetLadderByIdParam
import com.acme.clients.activityladderclient.api.RemoveLadderActivityParam as ClientRemoveActivityParam
import com.acme.clients.common.Result
import com.acme.services.camperservice.features.activityladder.error.LadderError
import com.acme.services.camperservice.features.activityladder.mapper.ActivityLadderMapper
import com.acme.services.camperservice.features.activityladder.model.LadderStatus
import com.acme.services.camperservice.features.activityladder.params.RemoveActivityParam
import com.acme.services.camperservice.features.activityladder.validations.ValidateRemoveActivity
import org.slf4j.LoggerFactory

internal class RemoveActivityAction(
    private val ladderClient: ActivityLadderClient,
) {
    private val logger = LoggerFactory.getLogger(RemoveActivityAction::class.java)
    private val validate = ValidateRemoveActivity()

    fun execute(param: RemoveActivityParam): Result<Unit, LadderError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Removing activity from ladder ladderId={} activityId={} userId={}", param.ladderId, param.activityId, param.requestingUserId)

        val ladderResult = ladderClient.getLadderById(GetLadderByIdParam(param.ladderId))
        val ladder = when (ladderResult) {
            is Result.Success -> ActivityLadderMapper.fromClient(ladderResult.value)
            is Result.Failure -> return Result.Failure(LadderError.fromClientError(ladderResult.error))
        }

        if (ladder.creatorId != param.requestingUserId) {
            return Result.Failure(LadderError.NotCreator(param.ladderId, param.requestingUserId))
        }
        if (ladder.status != LadderStatus.DRAFT) {
            return Result.Failure(LadderError.IllegalState("DRAFT", ladder.status.name))
        }

        return when (val result = ladderClient.removeActivity(
            ClientRemoveActivityParam(ladderId = param.ladderId, activityId = param.activityId)
        )) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> Result.Failure(LadderError.fromClientError(result.error))
        }
    }
}
