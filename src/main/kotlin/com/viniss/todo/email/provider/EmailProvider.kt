package com.viniss.todo.email.provider

import com.viniss.todo.email.model.EmailMessage

/**
 * Interface for email service providers.
 *
 * Follows SOLID principles:
 * - Dependency Inversion Principle (DIP): High-level EmailService depends on this abstraction
 * - Interface Segregation Principle (ISP): Minimal interface with only essential operations
 * - Open/Closed Principle (OCP): New providers can be added without modifying existing code
 *
 * Implementations can include:
 * - LoggingEmailProvider (for development/testing)
 * - SendGridEmailProvider (for production with SendGrid)
 * - AwsSesEmailProvider (for production with AWS SES)
 * - SmtpEmailProvider (for production with SMTP)
 */
interface EmailProvider {

    /**
     * Sends an email message using the provider's implementation.
     *
     * @param message The email message to send
     * @throws EmailSendException if the email cannot be sent
     */
    fun sendEmail(message: EmailMessage)

    /**
     * Returns the name of this email provider.
     * Useful for logging and debugging.
     */
    fun getProviderName(): String
}

/**
 * Exception thrown when an email cannot be sent.
 */
class EmailSendException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
