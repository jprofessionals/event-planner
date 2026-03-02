package com.meet.comment

import com.meet.sse.SseService
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.util.UUID

@Path("/api/events/{eventId}/comments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class CommentResource(
    private val commentService: CommentService,
    private val sseService: SseService,
) {
    @POST
    fun addComment(
        @PathParam("eventId") eventId: UUID,
        @Valid request: AddCommentRequest,
    ): Response {
        val comment = commentService.addComment(eventId, request)
        sseService.broadcast(eventId, "comment-added", comment)
        return Response.status(Response.Status.CREATED).entity(comment).build()
    }

    @GET
    fun getComments(
        @PathParam("eventId") eventId: UUID,
    ): Response {
        val comments = commentService.getComments(eventId)
        return Response.ok(comments).build()
    }
}
