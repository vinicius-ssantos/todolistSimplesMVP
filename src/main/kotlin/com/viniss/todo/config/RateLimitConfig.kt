package com.viniss.todo.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.validation.annotation.Validated
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty

/**
 * Configuration properties for Rate Limiting.
 *
 * Follows the Open/Closed Principle (OCP) by allowing rate limit
 * configuration to be extended through external configuration files
 * without modifying the source code.
 */
@Configuration
@ConfigurationProperties(prefix = "rate-limit")
@Validated
data class RateLimitConfig(
    /**
     * Maximum number of requests allowed within the time window.
     * Default: 5 requests
     */
    @field:Min(1, message = "Max requests must be at least 1")
    var maxRequests: Long = 5,

    /**
     * Time window duration in minutes for rate limiting.
     * Default: 1 minute
     */
    @field:Min(1, message = "Window duration must be at least 1 minute")
    var windowMinutes: Long = 1,

    /**
     * List of path patterns to apply rate limiting.
     * Supports prefix matching (e.g., "/api/auth/" will match all paths starting with it).
     * Default: ["/api/auth/"]
     */
    @field:NotEmpty(message = "At least one path pattern must be configured")
    var paths: List<String> = listOf("/api/auth/"),

    /**
     * Enable or disable rate limiting globally.
     * Useful for testing or temporary disabling without removing configuration.
     * Default: true
     */
    var enabled: Boolean = true
)
