package com.meet.scheduling

import jakarta.validation.constraints.NotBlank
import java.time.OffsetDateTime
import java.util.UUID

enum class VoteType { YES, MAYBE, NO, NONE }

data class AddTimeOptionsRequest(
    val options: List<TimeOptionInput>,
)

data class TimeOptionInput(
    val startTime: OffsetDateTime,
    val endTime: OffsetDateTime,
)

data class TimeOptionResponse(
    val id: UUID,
    val startTime: OffsetDateTime,
    val endTime: OffsetDateTime,
    val votes: List<VoteResponse> = emptyList(),
)

data class CastVotesRequest(
    @field:NotBlank
    val participantName: String,
    val votes: List<VoteInput>,
)

data class VoteInput(
    val timeOptionId: UUID,
    val vote: VoteType,
)

data class VoteResponse(
    val id: UUID,
    val participantName: String,
    val vote: VoteType,
)
