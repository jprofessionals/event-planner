package com.meet.event

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime
import java.util.UUID

enum class EventStage { SCHEDULING, PLANNING }

data class CreateEventRequest(
    @field:NotBlank @field:Size(max = 255)
    val title: String,
    val description: String? = null,
    val secretVotes: Boolean = false,
    val participantsCanPoll: Boolean = true,
    val participantsCanChecklist: Boolean = true,
    val participantsCanShoppingList: Boolean = false,
    @field:NotBlank
    val passphrase: String,
)

data class UpdateEventRequest(
    @field:Size(max = 255)
    val title: String? = null,
    val description: String? = null,
    val passphrase: String? = null,
    val participantsCanPoll: Boolean? = null,
    val participantsCanChecklist: Boolean? = null,
    val participantsCanShoppingList: Boolean? = null,
)

data class EventResponse(
    val id: UUID,
    val title: String,
    val description: String?,
    val secretVotes: Boolean,
    val participantsCanPoll: Boolean,
    val participantsCanChecklist: Boolean,
    val participantsCanShoppingList: Boolean,
    val stage: EventStage,
    val adminToken: UUID? = null,
    val passphrase: String? = null,
    val decidedTimeStart: OffsetDateTime? = null,
    val decidedTimeEnd: OffsetDateTime? = null,
    val createdAt: OffsetDateTime,
)

data class JoinEventRequest(
    @field:NotBlank
    val passphrase: String,
    @field:NotBlank
    val displayName: String,
)

data class JoinEventResponse(
    val participantId: UUID,
    val displayName: String,
    val eventId: UUID,
)

data class DecideTimeRequest(
    val startTime: OffsetDateTime,
    val endTime: OffsetDateTime,
)
