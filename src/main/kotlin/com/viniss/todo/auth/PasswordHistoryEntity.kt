package com.viniss.todo.auth

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

/**
 * Entity for tracking password history to prevent password reuse.
 */
@Entity
@Table(
    name = "password_history",
    indexes = [
        Index(name = "idx_password_history_user_id", columnList = "user_id"),
        Index(name = "idx_password_history_created_at", columnList = "created_at")
    ]
)
data class PasswordHistoryEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "password_hash", nullable = false, length = 60)
    val passwordHash: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
