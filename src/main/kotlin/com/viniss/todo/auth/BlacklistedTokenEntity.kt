package com.viniss.todo.auth

import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "blacklisted_token")
data class BlacklistedTokenEntity(
    @Id
    @Column(columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "token_jti", nullable = false, unique = true)
    val tokenJti: String,

    @Column(name = "user_id", nullable = false, columnDefinition = "UUID")
    val userId: UUID,

    @Column(name = "blacklisted_at", nullable = false)
    val blacklistedAt: Instant = Instant.now(),

    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,

    @Column(length = 100)
    val reason: String? = null
)
