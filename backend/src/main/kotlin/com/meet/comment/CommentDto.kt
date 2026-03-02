package com.meet.comment

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime
import java.util.UUID

data class AddCommentRequest(
    @field:NotBlank
    val authorName: String,
    @field:NotBlank @field:Size(max = 5000)
    val content: String,
)

data class CommentResponse(
    val id: UUID,
    val authorName: String,
    val content: String,
    val createdAt: OffsetDateTime,
)
