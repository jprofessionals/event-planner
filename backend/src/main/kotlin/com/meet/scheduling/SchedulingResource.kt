package com.meet.scheduling

import com.meet.sse.SseService
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

@Path("/api/events/{eventId}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class SchedulingResource(
    private val schedulingService: SchedulingService,
    private val sseService: SseService,
) {
    @POST
    @Path("/time-options")
    fun addTimeOptions(
        @PathParam("eventId") eventId: UUID,
        @HeaderParam("X-Admin-Token") adminToken: UUID?,
        @Valid request: AddTimeOptionsRequest,
    ): Response {
        if (adminToken == null) {
            return Response.status(Response.Status.FORBIDDEN).build()
        }
        val options = schedulingService.addTimeOptions(eventId, adminToken, request)
        sseService.broadcast(eventId, "time-options-added", options)
        return Response.status(Response.Status.CREATED).entity(options).build()
    }

    @DELETE
    @Path("/time-options/{optionId}")
    fun deleteTimeOption(
        @PathParam("eventId") eventId: UUID,
        @PathParam("optionId") optionId: UUID,
        @HeaderParam("X-Admin-Token") adminToken: UUID?,
    ): Response {
        if (adminToken == null) {
            return Response.status(Response.Status.FORBIDDEN).build()
        }
        schedulingService.deleteTimeOption(eventId, adminToken, optionId)
        sseService.broadcast(eventId, "time-option-deleted", mapOf("optionId" to optionId))
        return Response.noContent().build()
    }

    @GET
    @Path("/time-options")
    fun getTimeOptions(
        @PathParam("eventId") eventId: UUID,
        @HeaderParam("X-Admin-Token") adminToken: UUID?,
        @HeaderParam("X-Participant-Name") participantName: String?,
    ): Response {
        val isAdmin = adminToken != null
        val options = schedulingService.getTimeOptions(eventId, participantName, isAdmin)
        return Response.ok(options).build()
    }

    @POST
    @Path("/votes")
    fun castVotes(
        @PathParam("eventId") eventId: UUID,
        @Valid request: CastVotesRequest,
    ): Response {
        val votes = schedulingService.castVotes(eventId, request)
        sseService.broadcast(eventId, "votes-cast", votes)
        return Response.ok(votes).build()
    }
}
