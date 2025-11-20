package com.viniss.todo.security

import com.viniss.todo.config.RateLimitConfig
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Refill
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Service responsible for rate limiting using the Token Bucket algorithm.
 *
 * Refactored to follow SOLID principles:
 * - Single Responsibility: Only handles rate limiting logic
 * - Open/Closed: Configuration is externalized via RateLimitConfig
 * - Dependency Inversion: Depends on abstraction (RateLimitConfig)
 */
@Service
class RateLimitService(
    private val config: RateLimitConfig
) {

    private val cache = ConcurrentHashMap<String, Bucket>()

    /**
     * Resolves or creates a rate limit bucket for the given key.
     * Rate limit is configured via application properties.
     */
    fun resolveBucket(key: String): Bucket {
        return cache.computeIfAbsent(key) { createNewBucket() }
    }

    private fun createNewBucket(): Bucket {
        val limit = Bandwidth.classic(
            config.maxRequests,
            Refill.intervally(config.maxRequests, Duration.ofMinutes(config.windowMinutes))
        )
        return Bucket.builder()
            .addLimit(limit)
            .build()
    }

    fun isAllowed(key: String): Boolean {
        if (!config.enabled) {
            return true
        }
        val bucket = resolveBucket(key)
        return bucket.tryConsume(1)
    }

    /**
     * Clear all rate limit buckets. Useful for testing.
     */
    fun reset() {
        cache.clear()
    }
}
