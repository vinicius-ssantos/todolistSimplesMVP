package com.viniss.todo.email.provider

import com.viniss.todo.email.model.EmailMessage
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * Email provider that logs emails instead of sending them.
 *
 * This implementation is useful for:
 * - Development environments (avoid sending real emails)
 * - Testing environments (verify email content without external dependencies)
 * - Demo environments (show email functionality without email infrastructure)
 *
 * Activated when: email.provider=logging (or by default if no provider is specified)
 */
@Component
@ConditionalOnProperty(
    prefix = "email",
    name = ["provider"],
    havingValue = "logging",
    matchIfMissing = true
)
class LoggingEmailProvider : EmailProvider {

    private val logger = LoggerFactory.getLogger(LoggingEmailProvider::class.java)

    override fun sendEmail(message: EmailMessage) {
        logger.info("=".repeat(80))
        logger.info("📧 SIMULATED EMAIL (LoggingEmailProvider)")
        logger.info("From: ${message.from ?: "default-sender@todolist.com"}")
        logger.info("To: ${message.to}")
        logger.info("Subject: ${message.subject}")
        logger.info("-".repeat(80))
        logger.info("Text Content:")
        logger.info(message.textContent)

        if (message.htmlContent != null) {
            logger.info("-".repeat(80))
            logger.info("HTML Content:")
            logger.info(message.htmlContent)
        }

        if (message.replyTo != null) {
            logger.info("-".repeat(80))
            logger.info("Reply-To: ${message.replyTo}")
        }

        logger.info("=".repeat(80))
    }

    override fun getProviderName(): String = "LoggingEmailProvider"
}
