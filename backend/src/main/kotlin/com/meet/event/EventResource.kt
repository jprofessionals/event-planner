package com.meet.event

import com.meet.auth.JwtService
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
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.SecurityContext
import org.eclipse.microprofile.jwt.JsonWebToken
import java.util.UUID

@Path("/api/events")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class EventResource(
    private val eventService: EventService,
    private val sseService: SseService,
    private val jwtService: JwtService,
) {
    @POST
    fun createEvent(
        @Context securityContext: SecurityContext,
        @Valid request: CreateEventRequest,
    ): Response {
        val ownerId = extractUserId(securityContext)
        val event = eventService.createEvent(request, ownerId)
        return Response.status(Response.Status.CREATED).entity(event).build()
    }

    @GET
    @Path("/{eventId}")
    fun getEvent(
        @PathParam("eventId") eventId: UUID,
        @HeaderParam("X-Admin-Token") adminToken: UUID?,
    ): Response {
        val event = eventService.getEventWithAdminCheck(eventId, adminToken)
        return Response.ok(event).build()
    }

    @PATCH
    @Path("/{eventId}")
    fun updateEvent(
        @PathParam("eventId") eventId: UUID,
        @HeaderParam("X-Admin-Token") adminToken: UUID?,
        @Valid request: UpdateEventRequest,
    ): Response {
        if (adminToken == null) {
            return Response.status(Response.Status.FORBIDDEN).build()
        }
        val event = eventService.updateEvent(eventId, adminToken, request)
        sseService.broadcast(eventId, "event-updated", event)
        return Response.ok(event).build()
    }

    @POST
    @Path("/{eventId}/join")
    fun joinEvent(
        @PathParam("eventId") eventId: UUID,
        @Context securityContext: SecurityContext,
        @Valid request: JoinEventRequest,
    ): Response {
        val userId = extractUserId(securityContext)
        val participant = eventService.joinEvent(eventId, request, userId)
        sseService.broadcast(eventId, "participant-joined", participant)
        return Response.ok(participant).build()
    }

    @POST
    @Path("/{eventId}/decide")
    fun decideTime(
        @PathParam("eventId") eventId: UUID,
        @HeaderParam("X-Admin-Token") adminToken: UUID?,
        @Valid request: DecideTimeRequest,
    ): Response {
        if (adminToken == null) {
            return Response.status(Response.Status.FORBIDDEN).build()
        }
        val event = eventService.decideTime(eventId, adminToken, request)
        sseService.broadcast(eventId, "time-decided", event)
        return Response.ok(event).build()
    }

    private fun extractUserId(securityContext: SecurityContext): UUID? {
        val principal = securityContext.userPrincipal ?: return null
        return if (principal is JsonWebToken) {
            jwtService.getUserId(principal)
        } else {
            null
        }
    }
}
