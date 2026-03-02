package com.meet.checklist

import com.meet.event.EventService
import com.meet.sse.SseService
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.PATCH
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.util.UUID

@Path("/api/events/{eventId}/checklist")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class ChecklistResource(
    private val checklistService: ChecklistService,
    private val eventService: EventService,
    private val sseService: SseService,
) {
    @POST
    fun addItem(
        @PathParam("eventId") eventId: UUID,
        @HeaderParam("X-Admin-Token") adminToken: UUID?,
        @Valid request: AddChecklistItemRequest,
    ): Response {
        val event = eventService.getEvent(eventId)
        if (!event.participantsCanChecklist) {
            if (adminToken == null) {
                return Response.status(Response.Status.FORBIDDEN).build()
            }
            eventService.verifyAdmin(eventId, adminToken)
        }
        val item = checklistService.addItem(eventId, request)
        sseService.broadcast(eventId, "checklist-item-added", item)
        return Response.status(Response.Status.CREATED).entity(item).build()
    }

    @GET
    fun getItems(
        @PathParam("eventId") eventId: UUID,
    ): Response {
        val items = checklistService.getItems(eventId)
        return Response.ok(items).build()
    }

    @PATCH
    @Path("/{itemId}")
    fun updateItem(
        @PathParam("eventId") eventId: UUID,
        @PathParam("itemId") itemId: UUID,
        @Valid request: UpdateChecklistItemRequest,
    ): Response {
        val item = checklistService.updateItem(eventId, itemId, request)
        sseService.broadcast(eventId, "checklist-item-updated", item)
        return Response.ok(item).build()
    }
}
