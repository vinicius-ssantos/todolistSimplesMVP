package com.viniss.todo.security

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Refill
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Service
class RateLimitService {

    private val cache = ConcurrentHashMap<String, Bucket>()

    /**
     * Rate limit: 5 requests per minute per IP
     */
    fun resolveBucket(key: String): Bucket {
        return cache.computeIfAbsent(key) { createNewBucket() }
    }

    private fun createNewBucket(): Bucket {
        val limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1)))
        return Bucket.builder()
            .addLimit(limit)
            .build()
    }

    fun isAllowed(key: String): Boolean {
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
