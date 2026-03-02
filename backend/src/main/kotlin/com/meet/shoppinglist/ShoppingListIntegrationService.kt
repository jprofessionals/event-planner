package com.meet.shoppinglist

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.meet.config.ShoppingListConfig
import com.meet.generated.jooq.tables.EventShoppingLists.Companion.EVENT_SHOPPING_LISTS
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import org.jooq.DSLContext
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.OffsetDateTime
import java.util.UUID

@ApplicationScoped
class ShoppingListIntegrationService(
    private val dsl: DSLContext,
    private val config: ShoppingListConfig,
) {
    private val httpClient = HttpClient.newHttpClient()
    private val mapper = jacksonObjectMapper()

    fun createShoppingList(
        eventId: UUID,
        request: CreateShoppingListRequest,
        participantName: String,
    ): ShoppingListResponse {
        val externalResponse = callExternalApi(request)

        val id = UUID.randomUUID()
        val now = OffsetDateTime.now()

        dsl
            .insertInto(EVENT_SHOPPING_LISTS)
            .set(EVENT_SHOPPING_LISTS.ID, id)
            .set(EVENT_SHOPPING_LISTS.EVENT_ID, eventId)
            .set(EVENT_SHOPPING_LISTS.TITLE, request.title)
            .set(EVENT_SHOPPING_LISTS.SHARE_TOKEN, externalResponse.shareToken)
            .set(EVENT_SHOPPING_LISTS.WIDGET_URL, externalResponse.widgetUrl)
            .set(EVENT_SHOPPING_LISTS.CREATED_BY_PARTICIPANT, participantName)
            .set(EVENT_SHOPPING_LISTS.CREATED_AT, now)
            .execute()

        return ShoppingListResponse(
            id = id,
            eventId = eventId,
            title = request.title,
            shareToken = externalResponse.shareToken,
            widgetUrl = "${config.apiUrl()}${externalResponse.widgetUrl}",
            createdByParticipant = participantName,
            createdAt = now,
        )
    }

    fun getShoppingLists(eventId: UUID): List<ShoppingListResponse> =
        dsl
            .selectFrom(EVENT_SHOPPING_LISTS)
            .where(EVENT_SHOPPING_LISTS.EVENT_ID.eq(eventId))
            .orderBy(EVENT_SHOPPING_LISTS.CREATED_AT.asc())
            .fetch()
            .map { record ->
                ShoppingListResponse(
                    id = record.id!!,
                    eventId = record.eventId,
                    title = record.title,
                    shareToken = record.shareToken,
                    widgetUrl = "${config.apiUrl()}${record.widgetUrl}",
                    createdByParticipant = record.createdByParticipant,
                    createdAt = record.createdAt!!,
                )
            }

    fun deleteShoppingList(
        eventId: UUID,
        listId: UUID,
    ) {
        val deleted =
            dsl
                .deleteFrom(EVENT_SHOPPING_LISTS)
                .where(
                    EVENT_SHOPPING_LISTS.ID
                        .eq(listId)
                        .and(EVENT_SHOPPING_LISTS.EVENT_ID.eq(eventId)),
                ).execute()

        if (deleted == 0) {
            throw WebApplicationException(Response.Status.NOT_FOUND)
        }
    }

    private fun callExternalApi(request: CreateShoppingListRequest): ExternalCreateResponse {
        val body =
            mapper.writeValueAsString(
                ExternalCreateRequest(title = request.title, email = request.email),
            )

        val httpRequest =
            HttpRequest
                .newBuilder()
                .uri(URI.create("${config.apiUrl()}/api/external/lists"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build()

        val response =
            try {
                httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())
            } catch (e: Exception) {
                throw WebApplicationException(
                    "Shopping list service unavailable",
                    Response.Status.BAD_GATEWAY,
                )
            }

        if (response.statusCode() != 201) {
            throw WebApplicationException(
                "Shopping list service error: ${response.statusCode()}",
                Response.Status.BAD_GATEWAY,
            )
        }

        return mapper.readValue(response.body())
    }
}
