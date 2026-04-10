package com.acme.services.camperservice.features.activityladder.service

import com.acme.clients.activityladderclient.api.ActivityLadderClient
import com.acme.clients.userclient.api.UserClient
import com.acme.services.camperservice.features.activityladder.actions.AddActivityAction
import com.acme.services.camperservice.features.activityladder.actions.CastVoteAction
import com.acme.services.camperservice.features.activityladder.actions.CreateLadderAction
import com.acme.services.camperservice.features.activityladder.actions.GetLadderDetailAction
import com.acme.services.camperservice.features.activityladder.actions.ListLaddersAction
import com.acme.services.camperservice.features.activityladder.actions.RemoveActivityAction
import com.acme.services.camperservice.features.activityladder.actions.RestartLadderAction
import com.acme.services.camperservice.features.activityladder.actions.StartLadderAction
import com.acme.services.camperservice.features.activityladder.params.AddActivityParam
import com.acme.services.camperservice.features.activityladder.params.CastVoteParam
import com.acme.services.camperservice.features.activityladder.params.CreateLadderParam
import com.acme.services.camperservice.features.activityladder.params.GetLadderDetailParam
import com.acme.services.camperservice.features.activityladder.params.ListLaddersParam
import com.acme.services.camperservice.features.activityladder.params.RemoveActivityParam
import com.acme.services.camperservice.features.activityladder.params.RestartLadderParam
import com.acme.services.camperservice.features.activityladder.params.StartLadderParam
import com.acme.services.camperservice.websocket.LadderEventPublisher

class ActivityLadderService(
    ladderClient: ActivityLadderClient,
    userClient: UserClient,
    eventPublisher: LadderEventPublisher,
) {
    private val createLadder = CreateLadderAction(ladderClient)
    private val listLadders = ListLaddersAction(ladderClient)
    private val getLadderDetail = GetLadderDetailAction(ladderClient, userClient)
    private val addActivity = AddActivityAction(ladderClient)
    private val removeActivity = RemoveActivityAction(ladderClient)
    private val startLadder = StartLadderAction(ladderClient, eventPublisher)
    private val castVote = CastVoteAction(ladderClient)
    private val restartLadder = RestartLadderAction(ladderClient)

    fun create(param: CreateLadderParam) = createLadder.execute(param)
    fun list(param: ListLaddersParam) = listLadders.execute(param)
    fun getDetail(param: GetLadderDetailParam) = getLadderDetail.execute(param)
    fun addActivity(param: AddActivityParam) = addActivity.execute(param)
    fun removeActivity(param: RemoveActivityParam) = removeActivity.execute(param)
    fun start(param: StartLadderParam) = startLadder.execute(param)
    fun castVote(param: CastVoteParam) = castVote.execute(param)
    fun restart(param: RestartLadderParam) = restartLadder.execute(param)
}
