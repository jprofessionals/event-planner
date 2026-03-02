package com.meet.checklist

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime
import java.util.UUID

data class AddChecklistItemRequest(
    @field:NotBlank @field:Size(max = 500)
    val text: String,
    val assignedTo: String? = null,
)

data class UpdateChecklistItemRequest(
    val completed: Boolean? = null,
    val text: String? = null,
    val assignedTo: String? = null,
)

data class ChecklistItemResponse(
    val id: UUID,
    val text: String,
    val completed: Boolean,
    val assignedTo: String?,
    val createdAt: OffsetDateTime,
)
