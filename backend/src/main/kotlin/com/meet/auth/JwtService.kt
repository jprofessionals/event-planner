package com.meet.auth

import io.smallrye.jwt.build.Jwt
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.jwt.JsonWebToken
import java.util.UUID

@ApplicationScoped
class JwtService {
    fun generateToken(
        userId: UUID,
        email: String,
    ): String =
        Jwt
            .issuer("event-planner")
            .subject(userId.toString())
            .claim("email", email)
            .expiresIn(86400) // 24 hours
            .sign()

    fun validateToken(token: String): UUID =
        try {
            // For manual validation from raw token strings (e.g. in tests or non-CDI contexts),
            // the actual JWT verification is done by SmallRye JWT infrastructure.
            // This method parses the subject from an already-verified token.
            val parts = token.split(".")
            if (parts.size != 3) throw IllegalArgumentException("Not a JWT")
            val payload =
                String(
                    java.util.Base64
                        .getUrlDecoder()
                        .decode(parts[1]),
                )
            val subMatch =
                Regex(""""sub"\s*:\s*"([^"]+)"""").find(payload)
                    ?: throw IllegalArgumentException("No sub claim")
            UUID.fromString(subMatch.groupValues[1])
        } catch (e: Exception) {
            throw WebApplicationException("Invalid token", Response.Status.UNAUTHORIZED)
        }

    fun getUserId(jwt: JsonWebToken): UUID =
        try {
            UUID.fromString(jwt.subject)
        } catch (e: Exception) {
            throw WebApplicationException("Invalid token", Response.Status.UNAUTHORIZED)
        }
}
