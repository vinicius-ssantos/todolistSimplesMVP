package com.viniss.todo.email

import com.viniss.todo.email.model.EmailMessage
import com.viniss.todo.email.provider.EmailProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Email service for sending verification and notification emails.
 *
 * Refactored to follow SOLID principles:
 * - Single Responsibility: Only handles business logic for email composition
 * - Dependency Inversion: Depends on EmailProvider abstraction, not concrete implementations
 * - Open/Closed: New email providers can be added without modifying this class
 *
 * To switch email providers, configure in application.yml:
 *   email.provider: logging  # for development/testing
 *   email.provider: sendgrid # for production with SendGrid
 *   email.provider: aws-ses  # for production with AWS SES
 */
@Service
class EmailService(
    private val emailProvider: EmailProvider
) {

    private val logger = LoggerFactory.getLogger(EmailService::class.java)

    init {
        logger.info("EmailService initialized with provider: ${emailProvider.getProviderName()}")
    }

    /**
     * Send email verification message.
     * Composes the email content and delegates sending to the configured email provider.
     */
    fun sendVerificationEmail(email: String, verificationToken: String, baseUrl: String = "http://localhost:8082") {
        val verificationLink = "$baseUrl/api/auth/verify-email?token=$verificationToken"

        val message = EmailMessage(
            to = email,
            subject = "Verify Your Email Address",
            textContent = buildVerificationEmailText(verificationLink),
            htmlContent = buildVerificationEmailHtml(verificationLink)
        )

        try {
            emailProvider.sendEmail(message)
            logger.info("Verification email queued for: $email")
        } catch (e: Exception) {
            logger.error("Failed to send verification email to: $email", e)
            throw e
        }
    }

    /**
     * Send password reset email.
     * Composes the email content and delegates sending to the configured email provider.
     */
    fun sendPasswordResetEmail(email: String, resetToken: String, baseUrl: String = "http://localhost:8082") {
        val resetLink = "$baseUrl/api/auth/reset-password?token=$resetToken"

        val message = EmailMessage(
            to = email,
            subject = "Reset Your Password",
            textContent = buildPasswordResetEmailText(resetLink),
            htmlContent = buildPasswordResetEmailHtml(resetLink)
        )

        try {
            emailProvider.sendEmail(message)
            logger.info("Password reset email queued for: $email")
        } catch (e: Exception) {
            logger.error("Failed to send password reset email to: $email", e)
            throw e
        }
    }

    // Private helper methods for email content composition

    private fun buildVerificationEmailText(verificationLink: String): String {
        return """
            Welcome to TodoList!

            Please verify your email address by clicking the link below:

            $verificationLink

            This link will expire in 24 hours.

            If you didn't create an account, please ignore this email.

            Best regards,
            TodoList Team
        """.trimIndent()
    }

    private fun buildVerificationEmailHtml(verificationLink: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Verify Your Email</title>
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #4CAF50;">Welcome to TodoList!</h2>
                    <p>Please verify your email address by clicking the button below:</p>
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="$verificationLink"
                           style="background-color: #4CAF50; color: white; padding: 12px 30px;
                                  text-decoration: none; border-radius: 5px; display: inline-block;">
                            Verify Email Address
                        </a>
                    </div>
                    <p style="color: #666; font-size: 14px;">
                        Or copy and paste this link into your browser:<br>
                        <a href="$verificationLink">$verificationLink</a>
                    </p>
                    <p style="color: #666; font-size: 14px;">
                        This link will expire in 24 hours.
                    </p>
                    <p style="color: #666; font-size: 14px;">
                        If you didn't create an account, please ignore this email.
                    </p>
                    <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
                    <p style="color: #999; font-size: 12px;">
                        Best regards,<br>
                        TodoList Team
                    </p>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun buildPasswordResetEmailText(resetLink: String): String {
        return """
            Password Reset Request

            We received a request to reset your password. Click the link below to reset it:

            $resetLink

            This link will expire in 1 hour.

            If you didn't request a password reset, please ignore this email.

            Best regards,
            TodoList Team
        """.trimIndent()
    }

    private fun buildPasswordResetEmailHtml(resetLink: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Reset Your Password</title>
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #FF9800;">Password Reset Request</h2>
                    <p>We received a request to reset your password. Click the button below to proceed:</p>
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="$resetLink"
                           style="background-color: #FF9800; color: white; padding: 12px 30px;
                                  text-decoration: none; border-radius: 5px; display: inline-block;">
                            Reset Password
                        </a>
                    </div>
                    <p style="color: #666; font-size: 14px;">
                        Or copy and paste this link into your browser:<br>
                        <a href="$resetLink">$resetLink</a>
                    </p>
                    <p style="color: #666; font-size: 14px;">
                        This link will expire in 1 hour.
                    </p>
                    <p style="color: #d32f2f; font-size: 14px;">
                        <strong>Important:</strong> If you didn't request a password reset,
                        please ignore this email and consider changing your password as a precaution.
                    </p>
                    <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
                    <p style="color: #999; font-size: 12px;">
                        Best regards,<br>
                        TodoList Team
                    </p>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
}
