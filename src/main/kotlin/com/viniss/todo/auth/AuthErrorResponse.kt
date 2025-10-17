package com.viniss.todo.auth

import java.time.Instant

/**
 * Payload padrão para respostas de autenticação 401 em JSON.
 */
data class AuthErrorResponse(
    val error: String,
    val message: String?,
    val path: String,
    val timestamp: Instant = Instant.now()
)
