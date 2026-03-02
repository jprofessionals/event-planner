package com.meet.auth

import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.SecurityContext
import org.eclipse.microprofile.jwt.JsonWebToken

@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class UserResource(
    private val authService: AuthService,
    private val jwtService: JwtService,
) {
    @GET
    @Path("/me")
    fun getCurrentUser(
        @Context securityContext: SecurityContext,
    ): Response {
        val userId = extractUserId(securityContext) ?: return Response.status(Response.Status.UNAUTHORIZED).build()
        val user = authService.getCurrentUser(userId)
        return Response.ok(user).build()
    }

    @GET
    @Path("/me/events")
    fun getUserEvents(
        @Context securityContext: SecurityContext,
    ): Response {
        val userId = extractUserId(securityContext) ?: return Response.status(Response.Status.UNAUTHORIZED).build()
        val events = authService.getUserEvents(userId)
        return Response.ok(events).build()
    }

    private fun extractUserId(securityContext: SecurityContext): java.util.UUID? {
        val principal = securityContext.userPrincipal ?: return null
        return if (principal is JsonWebToken) {
            jwtService.getUserId(principal)
        } else {
            null
        }
    }
}
