package com.acme.services.camperservice.features.activityladder.controller

import com.acme.clients.common.Result
import com.acme.services.camperservice.common.error.toResponseEntity
import com.acme.services.camperservice.features.activityladder.dto.AddActivityRequest
import com.acme.services.camperservice.features.activityladder.dto.CastVoteRequest
import com.acme.services.camperservice.features.activityladder.dto.CastVoteResponse
import com.acme.services.camperservice.features.activityladder.dto.CreateLadderRequest
import com.acme.services.camperservice.features.activityladder.mapper.ActivityLadderMapper
import com.acme.services.camperservice.features.activityladder.model.VoteOutcome
import com.acme.services.camperservice.features.activityladder.params.AddActivityParam
import com.acme.services.camperservice.features.activityladder.params.CastVoteParam
import com.acme.services.camperservice.features.activityladder.params.CreateLadderParam
import com.acme.services.camperservice.features.activityladder.params.GetLadderDetailParam
import com.acme.services.camperservice.features.activityladder.params.ListLaddersParam
import com.acme.services.camperservice.features.activityladder.params.NewActivityInput
import com.acme.services.camperservice.features.activityladder.params.RemoveActivityParam
import com.acme.services.camperservice.features.activityladder.params.RestartLadderParam
import com.acme.services.camperservice.features.activityladder.params.StartLadderParam
import com.acme.services.camperservice.features.activityladder.service.ActivityLadderService
import com.acme.services.camperservice.websocket.LadderEventPublisher
import com.acme.services.camperservice.websocket.LadderPresenceTracker
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/ladders")
class ActivityLadderController(
    private val ladderService: ActivityLadderService,
    private val eventPublisher: LadderEventPublisher,
    private val presenceTracker: LadderPresenceTracker,
) {
    private val logger = LoggerFactory.getLogger(ActivityLadderController::class.java)

    @PostMapping
    fun create(
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody req: CreateLadderRequest,
    ): ResponseEntity<Any> {
        logger.info("POST /api/ladders userId={}", userId)
        val param = CreateLadderParam(
            requestingUserId = userId,
            title = req.title,
            activities = req.activities.map { NewActivityInput(it.name, it.imageUrl, it.distanceMinutes, it.costPerPerson) },
        )
        return ladderService.create(param).toResponseEntity(successStatus = 201) {
            ActivityLadderMapper.toDetailResponse(it)
        }
    }

    @GetMapping
    fun list(
        @RequestHeader("X-User-Id") userId: UUID,
    ): ResponseEntity<Any> {
        logger.info("GET /api/ladders userId={}", userId)
        val param = ListLaddersParam(requestingUserId = userId)
        return ladderService.list(param).toResponseEntity { summaries ->
            summaries.map { ActivityLadderMapper.toSummaryResponse(it) }
        }
    }

    @GetMapping("/{id}")
    fun getDetail(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
    ): ResponseEntity<Any> {
        logger.info("GET /api/ladders/{} userId={}", id, userId)
        val param = GetLadderDetailParam(ladderId = id, requestingUserId = userId)
        return ladderService.getDetail(param).toResponseEntity {
            ActivityLadderMapper.toDetailResponse(it)
        }
    }

    @PostMapping("/{id}/activities")
    fun addActivity(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody req: AddActivityRequest,
    ): ResponseEntity<Any> {
        logger.info("POST /api/ladders/{}/activities userId={}", id, userId)
        val param = AddActivityParam(
            ladderId = id,
            requestingUserId = userId,
            name = req.name,
            imageUrl = req.imageUrl,
            distanceMinutes = req.distanceMinutes,
            costPerPerson = req.costPerPerson,
        )
        val result = ladderService.addActivity(param)
        if (result is Result.Success) {
            eventPublisher.activityAdded(id)
        }
        return result.toResponseEntity(successStatus = 201) {
            ActivityLadderMapper.toActivityResponse(it)
        }
    }

    @DeleteMapping("/{id}/activities/{activityId}")
    fun removeActivity(
        @PathVariable id: UUID,
        @PathVariable activityId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
    ): ResponseEntity<Any> {
        logger.info("DELETE /api/ladders/{}/activities/{} userId={}", id, activityId, userId)
        val param = RemoveActivityParam(ladderId = id, activityId = activityId, requestingUserId = userId)
        val result = ladderService.removeActivity(param)
        if (result is Result.Success) {
            eventPublisher.activityRemoved(id)
        }
        return result.toResponseEntity(successStatus = 204) { }
    }

    @PostMapping("/{id}/start")
    fun start(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
    ): ResponseEntity<Any> {
        logger.info("POST /api/ladders/{}/start userId={}", id, userId)
        val presentUserIds = presenceTracker.snapshot(id)
        val param = StartLadderParam(ladderId = id, requestingUserId = userId, presentUserIds = presentUserIds)
        return ladderService.start(param).toResponseEntity {
            ActivityLadderMapper.toDetailResponse(it)
        }
    }

    @PostMapping("/{id}/vote")
    fun vote(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody req: CastVoteRequest,
    ): ResponseEntity<Any> {
        logger.info("POST /api/ladders/{}/vote userId={}", id, userId)
        val param = CastVoteParam(ladderId = id, requestingUserId = userId, votedForActivityId = req.votedForActivityId)
        val result = ladderService.castVote(param)
        if (result is Result.Success) {
            // TODO(PR 5c): Publish the appropriate sequence of events based on VoteOutcome variant
            // (vote-cast, round-resolved, round-started, completed)
        }
        return result.toResponseEntity { outcome ->
            when (outcome) {
                is VoteOutcome.VoteRecorded -> CastVoteResponse(outcome.voteCount, outcome.votersRemaining)
                is VoteOutcome.RoundTied -> CastVoteResponse(outcome.voteCount, 0)
                is VoteOutcome.RoundDecided -> CastVoteResponse(0, 0)
                is VoteOutcome.LadderCompleted -> CastVoteResponse(0, 0)
            }
        }
    }

    @PostMapping("/{id}/restart")
    fun restart(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
    ): ResponseEntity<Any> {
        logger.info("POST /api/ladders/{}/restart userId={}", id, userId)
        val param = RestartLadderParam(ladderId = id, requestingUserId = userId)
        val result = ladderService.restart(param)
        if (result is Result.Success) {
            eventPublisher.restarted(id)
        }
        return result.toResponseEntity { ladder ->
            ActivityLadderMapper.toLadderDetailResponse(ladder)
        }
    }
}
