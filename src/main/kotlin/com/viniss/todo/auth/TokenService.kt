package com.viniss.todo.auth

import java.util.UUID

/**
 * Stable contract for JWT creation and validation.
 * Allow swapping signing strategies without breaking consumers.
 */
interface TokenService {
    fun generateToken(userId: UUID, email: String): String
    fun isValid(token: String): Boolean
    fun extractUserId(token: String): UUID
    fun extractEmail(token: String): String
}
