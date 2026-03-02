package com.meet.poll

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime
import java.util.UUID

data class CreatePollRequest(
    @field:NotBlank
    val question: String,
    @field:Size(min = 2)
    val options: List<String>,
    val allowMultiple: Boolean = false,
)

data class PollResponse(
    val id: UUID,
    val question: String,
    val allowMultiple: Boolean,
    val options: List<PollOptionResponse>,
    val createdAt: OffsetDateTime,
)

data class PollOptionResponse(
    val id: UUID,
    val text: String,
    val voteCount: Int,
)

data class VotePollRequest(
    @field:NotBlank
    val participantName: String,
    val optionIds: List<UUID>,
)
