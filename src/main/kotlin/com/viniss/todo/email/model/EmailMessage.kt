package com.viniss.todo.email.model

/**
 * Represents an email message to be sent.
 *
 * This data class encapsulates all information needed to send an email,
 * following SOLID principles by providing a clear contract between
 * the email service and providers.
 */
data class EmailMessage(
    /**
     * Recipient email address.
     */
    val to: String,

    /**
     * Email subject line.
     */
    val subject: String,

    /**
     * Plain text content of the email.
     */
    val textContent: String,

    /**
     * Optional HTML content of the email.
     * If provided, email clients that support HTML will render this instead of textContent.
     */
    val htmlContent: String? = null,

    /**
     * Optional sender email address.
     * If not provided, the provider's default sender will be used.
     */
    val from: String? = null,

    /**
     * Optional reply-to email address.
     */
    val replyTo: String? = null
)
