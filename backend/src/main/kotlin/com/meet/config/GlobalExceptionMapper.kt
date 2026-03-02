package com.meet.config

import jakarta.validation.ConstraintViolationException
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider

@Provider
class WebApplicationExceptionMapper : ExceptionMapper<WebApplicationException> {
    override fun toResponse(exception: WebApplicationException): Response =
        Response
            .status(exception.response.status)
            .entity(mapOf("error" to (exception.message ?: "Unknown error")))
            .type("application/json")
            .build()
}

@Provider
class ConstraintViolationExceptionMapper : ExceptionMapper<ConstraintViolationException> {
    override fun toResponse(exception: ConstraintViolationException): Response {
        val violations =
            exception.constraintViolations.map { violation ->
                val field = violation.propertyPath.toString().substringAfterLast('.')
                mapOf("field" to field, "message" to violation.message)
            }
        return Response
            .status(Response.Status.BAD_REQUEST)
            .entity(mapOf("error" to "Validation failed", "violations" to violations))
            .type("application/json")
            .build()
    }
}
