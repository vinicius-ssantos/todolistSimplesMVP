package com.viniss.todo.auth

import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "refresh_token")
data class RefreshTokenEntity(
    @Id
    @Column(columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false, columnDefinition = "UUID")
    val userId: UUID,

    @Column(nullable = false, unique = true, length = 500)
    val token: String,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
