package com.meet.poll

import com.meet.event.EventService
import com.meet.sse.SseService
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.util.UUID

@Path("/api/events/{eventId}/polls")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class PollResource(
    private val pollService: PollService,
    private val eventService: EventService,
    private val sseService: SseService,
) {
    @POST
    fun createPoll(
        @PathParam("eventId") eventId: UUID,
        @HeaderParam("X-Admin-Token") adminToken: UUID?,
        @Valid request: CreatePollRequest,
    ): Response {
        val event = eventService.getEvent(eventId)
        if (!event.participantsCanPoll) {
            if (adminToken == null) {
                return Response.status(Response.Status.FORBIDDEN).build()
            }
            eventService.verifyAdmin(eventId, adminToken)
        }
        val poll = pollService.createPoll(eventId, request)
        sseService.broadcast(eventId, "poll-created", poll)
        return Response.status(Response.Status.CREATED).entity(poll).build()
    }

    @GET
    fun getPolls(
        @PathParam("eventId") eventId: UUID,
    ): Response {
        val polls = pollService.getPolls(eventId)
        return Response.ok(polls).build()
    }

    @POST
    @Path("/{pollId}/vote")
    fun votePoll(
        @PathParam("eventId") eventId: UUID,
        @PathParam("pollId") pollId: UUID,
        @Valid request: VotePollRequest,
    ): Response {
        pollService.votePoll(eventId, pollId, request)
        sseService.broadcast(eventId, "poll-vote", mapOf("pollId" to pollId, "participantName" to request.participantName))
        return Response.ok().build()
    }
}
