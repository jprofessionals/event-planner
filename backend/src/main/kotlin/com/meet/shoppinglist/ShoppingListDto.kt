package com.meet.shoppinglist

import jakarta.validation.constraints.NotBlank
import java.time.OffsetDateTime
import java.util.UUID

data class CreateShoppingListRequest(
    @field:NotBlank
    val title: String,
    val email: String? = null,
)

data class ShoppingListResponse(
    val id: UUID,
    val eventId: UUID,
    val title: String,
    val shareToken: String,
    val widgetUrl: String,
    val createdByParticipant: String,
    val createdAt: OffsetDateTime,
)

data class ExternalCreateRequest(
    val title: String,
    val email: String? = null,
)

data class ExternalCreateResponse(
    val listId: String,
    val shareToken: String,
    val widgetUrl: String,
)
