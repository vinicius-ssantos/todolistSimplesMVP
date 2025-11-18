package com.viniss.todo.auth

data class AuthResponse(val token: String)

data class AuthResponseWithRefresh(
    val accessToken: String,
    val refreshToken: String
)