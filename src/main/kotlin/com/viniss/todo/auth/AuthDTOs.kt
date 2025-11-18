package com.viniss.todo.auth

data class AuthResponseWithRefresh(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long = 900 // 15 minutes in seconds
)

data class RefreshTokenRequest(
    val refreshToken: String
)

data class VerifyEmailRequest(
    val token: String
)

data class VerifyEmailResponse(
    val success: Boolean,
    val message: String
)
