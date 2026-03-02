package com.meet.auth

import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class AuthResource(
    private val authService: AuthService,
) {
    @POST
    @Path("/register")
    fun register(
        @Valid request: RegisterRequest,
    ): Response {
        val auth = authService.register(request)
        return Response.status(Response.Status.CREATED).entity(auth).build()
    }

    @POST
    @Path("/login")
    fun login(
        @Valid request: LoginRequest,
    ): Response {
        val auth = authService.login(request)
        return Response.ok(auth).build()
    }
}
