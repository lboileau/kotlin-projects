package com.acme.services.camperservice.features.activityladder.actions

import com.acme.clients.activityladderclient.api.ActivityLadderClient
import com.acme.clients.activityladderclient.api.AddLadderActivityParam as ClientAddActivityParam
import com.acme.clients.activityladderclient.api.GetLadderByIdParam
import com.acme.clients.common.Result
import com.acme.services.camperservice.features.activityladder.error.LadderError
import com.acme.services.camperservice.features.activityladder.mapper.ActivityLadderMapper
import com.acme.services.camperservice.features.activityladder.model.LadderActivity
import com.acme.services.camperservice.features.activityladder.model.LadderStatus
import com.acme.services.camperservice.features.activityladder.params.AddActivityParam
import com.acme.services.camperservice.features.activityladder.validations.ValidateAddActivity
import org.slf4j.LoggerFactory

internal class AddActivityAction(
    private val ladderClient: ActivityLadderClient,
) {
    private val logger = LoggerFactory.getLogger(AddActivityAction::class.java)
    private val validate = ValidateAddActivity()

    fun execute(param: AddActivityParam): Result<LadderActivity, LadderError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Adding activity to ladder ladderId={} userId={}", param.ladderId, param.requestingUserId)

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

        return when (val result = ladderClient.addActivity(
            ClientAddActivityParam(
                ladderId = param.ladderId,
                name = param.name,
                imageUrl = param.imageUrl,
                distanceMinutes = param.distanceMinutes,
                costPerPerson = param.costPerPerson,
            )
        )) {
            is Result.Success -> Result.Success(ActivityLadderMapper.fromClientActivity(result.value))
            is Result.Failure -> Result.Failure(LadderError.fromClientError(result.error))
        }
    }
}
