package com.viniss.todo.email

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Email service for sending verification and notification emails.
 * In production, replace with actual email provider (SendGrid, AWS SES, etc.)
 */
@Service
class EmailService {

    private val logger = LoggerFactory.getLogger(EmailService::class.java)

    /**
     * Send email verification message.
     * For development/testing, this logs the verification link.
     * In production, integrate with actual email provider.
     */
    fun sendVerificationEmail(email: String, verificationToken: String, baseUrl: String = "http://localhost:8082") {
        val verificationLink = "$baseUrl/api/auth/verify-email?token=$verificationToken"

        // Log for development (replace with actual email sending in production)
        logger.info("=".repeat(80))
        logger.info("Email Verification for: $email")
        logger.info("Verification Link: $verificationLink")
        logger.info("=".repeat(80))

        // TODO: In production, replace with actual email sending:
        // - SendGrid: sendgridClient.send(...)
        // - AWS SES: sesClient.sendEmail(...)
        // - SMTP: javaMailSender.send(...)
    }

    /**
     * Send password reset email.
     */
    fun sendPasswordResetEmail(email: String, resetToken: String, baseUrl: String = "http://localhost:8082") {
        val resetLink = "$baseUrl/api/auth/reset-password?token=$resetToken"

        logger.info("=".repeat(80))
        logger.info("Password Reset for: $email")
        logger.info("Reset Link: $resetLink")
        logger.info("=".repeat(80))

        // TODO: Implement actual email sending
    }
}
