package com.trioForce.voyage.auth

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @field:Email @field:NotBlank val email: String,
    @field:NotBlank @field:Size(min = 6, max = 64) val password: String,
    @field:NotBlank val name: String,
    val phone: String? = null,
    val country: String? = null
)

data class LoginRequest(
    @field:Email @field:NotBlank val email: String,
    @field:NotBlank val password: String
)

data class ChangePasswordRequest(
    @field:NotBlank val oldPassword: String,
    @field:NotBlank @field:Size(min = 6, max = 64) val newPassword: String
)

data class LoginResponse(val token: String)
data class MeResponse(val id: Long, val email: String, val name: String, val role: String)
