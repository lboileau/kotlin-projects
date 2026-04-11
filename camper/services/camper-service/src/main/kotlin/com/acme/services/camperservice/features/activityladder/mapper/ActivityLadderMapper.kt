package com.acme.services.camperservice.features.activityladder.mapper

import com.acme.services.camperservice.features.activityladder.dto.CurrentRoundView
import com.acme.services.camperservice.features.activityladder.dto.LadderActivityResponse
import com.acme.services.camperservice.features.activityladder.dto.LadderDetailResponse
import com.acme.services.camperservice.features.activityladder.dto.LadderParticipantResponse
import com.acme.services.camperservice.features.activityladder.dto.LadderSummaryResponse
import com.acme.services.camperservice.features.activityladder.model.LadderActivity
import com.acme.services.camperservice.features.activityladder.model.LadderBracket
import com.acme.services.camperservice.features.activityladder.model.LadderDetail
import com.acme.services.camperservice.features.activityladder.model.LadderStatus
import com.acme.services.camperservice.features.activityladder.model.LadderSummary
import com.acme.clients.activityladderclient.model.Ladder as ClientLadder
import com.acme.clients.activityladderclient.model.LadderActivity as ClientLadderActivity
import com.acme.clients.activityladderclient.model.LadderStatus as ClientLadderStatus
import com.acme.clients.activityladderclient.model.LadderBracket as ClientLadderBracket
import com.acme.services.camperservice.features.activityladder.model.Ladder

object ActivityLadderMapper {

    fun fromClient(clientLadder: ClientLadder): Ladder {
        TODO("Implementation in PR 5c")
    }

    fun fromClientActivity(clientActivity: ClientLadderActivity): LadderActivity {
        TODO("Implementation in PR 5c")
    }

    fun fromClientStatus(status: ClientLadderStatus): LadderStatus {
        TODO("Implementation in PR 5c")
    }

    fun fromClientBracket(bracket: ClientLadderBracket): LadderBracket {
        TODO("Implementation in PR 5c")
    }

    fun toSummaryResponse(summary: LadderSummary): LadderSummaryResponse {
        TODO("Implementation in PR 5c")
    }

    fun toDetailResponse(detail: LadderDetail): LadderDetailResponse {
        TODO("Implementation in PR 5c")
    }

    fun toActivityResponse(activity: LadderActivity): LadderActivityResponse {
        TODO("Implementation in PR 5c")
    }

    fun toParticipantResponse(userId: java.util.UUID, username: String?, avatarSeed: String?): LadderParticipantResponse {
        TODO("Implementation in PR 5c")
    }

    fun toLadderDetailResponse(ladder: Ladder): LadderDetailResponse {
        TODO("Implementation in PR 5c")
    }
}
