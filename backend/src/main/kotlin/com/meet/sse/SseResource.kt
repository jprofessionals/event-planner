package com.meet.sse

import io.smallrye.mutiny.Multi
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.jboss.resteasy.reactive.RestStreamElementType
import java.util.UUID

@Path("/api/events/{eventId}/stream")
class SseResource(
    private val sseService: SseService,
) {
    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    fun stream(
        @PathParam("eventId") eventId: UUID,
    ): Multi<SseEvent> = sseService.subscribe(eventId)
}
