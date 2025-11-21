package com.viniss.todo.security

import com.viniss.todo.config.RateLimitConfig
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Filter that applies rate limiting to configured endpoints.
 *
 * Refactored to follow SOLID principles:
 * - Open/Closed: Path patterns are externalized via RateLimitConfig
 * - Single Responsibility: Only handles HTTP filtering and delegates rate limiting logic
 */
@Component
class RateLimitFilter(
    private val rateLimitService: RateLimitService,
    private val config: RateLimitConfig
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (!config.enabled) {
            filterChain.doFilter(request, response)
            return
        }

        val path = request.requestURI

        // Skip rate limiting for login and register endpoints
        // - Login has its own rate limiting via LoginAttemptService (email-based)
        // - Register has other protections (email uniqueness, password validation)
        if (path == "/api/auth/login" || path == "/api/auth/register") {
            filterChain.doFilter(request, response)
            return
        }

        // Apply rate limiting to configured path patterns
        if (shouldApplyRateLimit(path)) {
            val clientIp = getClientIP(request)
            val key = "$clientIp:${path}"

            if (!rateLimitService.isAllowed(key)) {
                response.status = HttpStatus.TOO_MANY_REQUESTS.value()
                response.contentType = "application/json"
                response.writer.write("""{"error":"Too many requests. Please try again later."}""")
                return
            }
        }

        filterChain.doFilter(request, response)
    }

    /**
     * Checks if the given path matches any configured rate limit patterns.
     * Supports prefix matching.
     */
    private fun shouldApplyRateLimit(path: String): Boolean {
        return config.paths.any { pattern -> path.startsWith(pattern) }
    }

    private fun getClientIP(request: HttpServletRequest): String {
        val xfHeader = request.getHeader("X-Forwarded-For")
        return if (xfHeader.isNullOrEmpty()) {
            request.remoteAddr
        } else {
            xfHeader.split(",").firstOrNull()?.trim() ?: request.remoteAddr
        }
    }
}
