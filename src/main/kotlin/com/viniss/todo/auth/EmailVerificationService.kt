package com.viniss.todo.auth

import com.viniss.todo.email.EmailService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Service
class EmailVerificationService(
    private val userRepository: AppUserRepository,
    private val emailService: EmailService
) {
    private val logger = LoggerFactory.getLogger(EmailVerificationService::class.java)

    companion object {
        private const val TOKEN_VALIDITY_HOURS = 24L
    }

    @Transactional
    fun generateAndSendVerificationToken(userId: UUID) {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }

        if (user.emailVerified) {
            logger.info("Email already verified for user: ${user.email}")
            return
        }

        val verificationToken = UUID.randomUUID().toString()
        val expiresAt = Instant.now().plus(TOKEN_VALIDITY_HOURS, ChronoUnit.HOURS)

        val updatedUser = user.copy(
            emailVerificationToken = verificationToken,
            verificationTokenExpiresAt = expiresAt
        )

        userRepository.save(updatedUser)
        emailService.sendVerificationEmail(user.email, verificationToken)

        logger.info("Verification email sent to: ${user.email}")
    }

    @Transactional
    fun verifyEmail(token: String): Boolean {
        val user = userRepository.findByEmailVerificationToken(token)
            ?: run {
                logger.warn("Invalid verification token: $token")
                return false
            }

        if (user.emailVerified) {
            logger.info("Email already verified: ${user.email}")
            return true
        }

        val expiresAt = user.verificationTokenExpiresAt
        if (expiresAt == null || Instant.now().isAfter(expiresAt)) {
            logger.warn("Expired verification token for: ${user.email}")
            return false
        }

        val verifiedUser = user.copy(
            emailVerified = true,
            emailVerificationToken = null,
            verificationTokenExpiresAt = null
        )

        userRepository.save(verifiedUser)
        logger.info("Email verified successfully: ${user.email}")

        return true
    }

    fun isEmailVerified(userId: UUID): Boolean {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }
        return user.emailVerified
    }
}
