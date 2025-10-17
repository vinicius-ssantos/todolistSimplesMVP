package com.viniss.todo.auth

import java.time.Instant
import java.util.UUID

/**
 * Lightweight representation kept for backwards compatibility.
 * Effective principal at runtime is provided by AuthUser.
 */
data class AuthPrincipal(
    val userId: UUID,
    val issuedAt: Instant? = null,
    val expiresAt: Instant? = null
)
