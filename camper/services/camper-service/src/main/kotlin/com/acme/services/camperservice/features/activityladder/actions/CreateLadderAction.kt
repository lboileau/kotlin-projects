package com.acme.services.camperservice.features.activityladder.actions

import com.acme.clients.activityladderclient.api.ActivityLadderClient
import com.acme.clients.activityladderclient.api.AddLadderActivityParam as ClientAddActivityParam
import com.acme.clients.activityladderclient.api.CreateLadderParam as ClientCreateLadderParam
import com.acme.clients.activityladderclient.api.GetLadderActivitiesParam
import com.acme.clients.common.Result
import com.acme.services.camperservice.features.activityladder.error.LadderError
import com.acme.services.camperservice.features.activityladder.mapper.ActivityLadderMapper
import com.acme.services.camperservice.features.activityladder.model.LadderDetail
import com.acme.services.camperservice.features.activityladder.params.CreateLadderParam
import com.acme.services.camperservice.features.activityladder.validations.ValidateCreateLadder
import org.slf4j.LoggerFactory

internal class CreateLadderAction(
    private val ladderClient: ActivityLadderClient,
) {
    private val logger = LoggerFactory.getLogger(CreateLadderAction::class.java)
    private val validate = ValidateCreateLadder()

    fun execute(param: CreateLadderParam): Result<LadderDetail, LadderError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Creating ladder title='{}' creatorId={}", param.title, param.requestingUserId)

        val ladderResult = ladderClient.createLadder(ClientCreateLadderParam(creatorId = param.requestingUserId, title = param.title))
        val ladder = when (ladderResult) {
            is Result.Success -> ladderResult.value
            is Result.Failure -> return Result.Failure(LadderError.fromClientError(ladderResult.error))
        }

        for (activity in param.activities) {
            val activityResult = ladderClient.addActivity(
                ClientAddActivityParam(
                    ladderId = ladder.id,
                    name = activity.name,
                    imageUrl = activity.imageUrl,
                    distanceMinutes = activity.distanceMinutes,
                    costPerPerson = activity.costPerPerson,
                )
            )
            if (activityResult is Result.Failure) {
                return Result.Failure(LadderError.fromClientError(activityResult.error))
            }
        }

        val activitiesResult = ladderClient.getActivities(GetLadderActivitiesParam(ladderId = ladder.id))
        val activities = when (activitiesResult) {
            is Result.Success -> activitiesResult.value
            is Result.Failure -> return Result.Failure(LadderError.fromClientError(activitiesResult.error))
        }

        val detail = com.acme.services.camperservice.features.activityladder.model.LadderDetail(
            ladder = ActivityLadderMapper.fromClient(ladder),
            activities = activities.map { ActivityLadderMapper.fromClientActivity(it) },
            participants = emptyList(),
            currentRound = null,
        )
        return Result.Success(detail)
    }
}
