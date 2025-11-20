package com.viniss.todo.email.provider

import com.viniss.todo.email.model.EmailMessage
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

/**
 * Configuration properties for SendGrid email provider.
 */
@Configuration
@ConfigurationProperties(prefix = "email.sendgrid")
@ConditionalOnProperty(prefix = "email", name = ["provider"], havingValue = "sendgrid")
data class SendGridConfig(
    var apiKey: String = "",
    var defaultFrom: String = "noreply@todolist.com",
    var enabled: Boolean = false
)

/**
 * Email provider implementation using SendGrid API.
 *
 * This is a STUB implementation showing how to add a new email provider.
 * To enable SendGrid:
 * 1. Add SendGrid dependency to build.gradle.kts:
 *    implementation("com.sendgrid:sendgrid-java:4.9.3")
 * 2. Configure in application.yml:
 *    email:
 *      provider: sendgrid
 *      sendgrid:
 *        api-key: ${SENDGRID_API_KEY}
 *        default-from: noreply@yourdomain.com
 *        enabled: true
 *
 * Activated when: email.provider=sendgrid
 *
 * This demonstrates Open/Closed Principle:
 * - System is OPEN for extension (add new providers)
 * - System is CLOSED for modification (no changes to existing code)
 */
@Component
@ConditionalOnProperty(prefix = "email", name = ["provider"], havingValue = "sendgrid")
class SendGridEmailProvider(
    private val config: SendGridConfig
) : EmailProvider {

    private val logger = LoggerFactory.getLogger(SendGridEmailProvider::class.java)

    init {
        if (!config.enabled || config.apiKey.isBlank()) {
            logger.warn("SendGridEmailProvider is configured but not properly initialized. Check your configuration.")
        } else {
            logger.info("SendGridEmailProvider initialized successfully")
        }
    }

    override fun sendEmail(message: EmailMessage) {
        if (!config.enabled) {
            throw EmailSendException("SendGrid provider is not enabled")
        }

        if (config.apiKey.isBlank()) {
            throw EmailSendException("SendGrid API key is not configured")
        }

        // TODO: Implement actual SendGrid integration
        // Example implementation:
        /*
        val from = Email(message.from ?: config.defaultFrom)
        val to = Email(message.to)
        val content = Content("text/plain", message.textContent)
        val mail = Mail(from, message.subject, to, content)

        if (message.htmlContent != null) {
            mail.addContent(Content("text/html", message.htmlContent))
        }

        val sg = SendGrid(config.apiKey)
        val request = Request()
        request.method = Method.POST
        request.endpoint = "mail/send"
        request.body = mail.build()

        try {
            val response = sg.api(request)
            if (response.statusCode >= 400) {
                throw EmailSendException("SendGrid returned error: ${response.statusCode} - ${response.body}")
            }
            logger.info("Email sent successfully via SendGrid to: ${message.to}")
        } catch (e: IOException) {
            throw EmailSendException("Failed to send email via SendGrid", e)
        }
        */

        // For now, just log that we would send via SendGrid
        logger.info("STUB: Would send email via SendGrid to: ${message.to}")
        logger.info("Subject: ${message.subject}")
    }

    override fun getProviderName(): String = "SendGridEmailProvider"
}
