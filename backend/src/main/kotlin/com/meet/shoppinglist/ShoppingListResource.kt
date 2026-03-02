package com.meet.shoppinglist

import com.meet.event.EventService
import com.meet.sse.SseService
import io.smallrye.common.annotation.Blocking
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.util.UUID

@Path("/api/events/{eventId}/shopping-lists")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class ShoppingListResource(
    private val shoppingListService: ShoppingListIntegrationService,
    private val eventService: EventService,
    private val sseService: SseService,
) {
    @POST
    @Blocking
    fun createShoppingList(
        @PathParam("eventId") eventId: UUID,
        @HeaderParam("X-Admin-Token") adminToken: UUID?,
        @Valid request: CreateShoppingListRequest,
    ): Response {
        val event = eventService.getEvent(eventId)
        if (!event.participantsCanShoppingList) {
            if (adminToken == null) {
                return Response.status(Response.Status.FORBIDDEN).build()
            }
            eventService.verifyAdmin(eventId, adminToken)
        }

        val participantName = if (adminToken != null) "Admin" else "Participant"
        val list = shoppingListService.createShoppingList(eventId, request, participantName)
        sseService.broadcast(eventId, "shopping-list-added", list)
        return Response.status(Response.Status.CREATED).entity(list).build()
    }

    @GET
    fun getShoppingLists(
        @PathParam("eventId") eventId: UUID,
    ): Response {
        val lists = shoppingListService.getShoppingLists(eventId)
        return Response.ok(lists).build()
    }

    @DELETE
    @Path("/{listId}")
    fun deleteShoppingList(
        @PathParam("eventId") eventId: UUID,
        @PathParam("listId") listId: UUID,
        @HeaderParam("X-Admin-Token") adminToken: UUID?,
    ): Response {
        if (adminToken == null) {
            return Response.status(Response.Status.FORBIDDEN).build()
        }
        eventService.verifyAdmin(eventId, adminToken)
        shoppingListService.deleteShoppingList(eventId, listId)
        sseService.broadcast(eventId, "shopping-list-removed", mapOf("id" to listId))
        return Response.noContent().build()
    }
}
