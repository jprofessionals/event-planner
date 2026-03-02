package com.meet.auth

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

data class RegisterRequest(
    @field:NotBlank @field:Email
    val email: String,
    @field:Size(min = 8, max = 100)
    val password: String,
    @field:NotBlank
    val displayName: String,
)

data class LoginRequest(
    @field:NotBlank
    val email: String,
    @field:NotBlank
    val password: String,
)

data class AuthResponse(
    val token: String,
    val user: UserResponse,
)

data class UserResponse(
    val id: UUID,
    val email: String,
    val displayName: String,
)
