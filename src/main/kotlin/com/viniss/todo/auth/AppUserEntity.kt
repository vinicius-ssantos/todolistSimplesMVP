package com.viniss.todo.auth


import jakarta.persistence.*
import java.time.Instant
import java.util.*


@Entity
@Table(name = "app_user")
data class AppUserEntity(
@Id
@Column(columnDefinition = "UUID")
val id: UUID = UUID.randomUUID(),


@Column(nullable = false, unique = true)
val email: String,


@Column(name = "password_hash", nullable = false)
val passwordHash: String,


@Column(name = "created_at", nullable = false)
val createdAt: Instant = Instant.now(),


@Column(name = "email_verified", nullable = false)
val emailVerified: Boolean = false,


@Column(name = "email_verification_token")
val emailVerificationToken: String? = null,


@Column(name = "verification_token_expires_at")
val verificationTokenExpiresAt: Instant? = null
)