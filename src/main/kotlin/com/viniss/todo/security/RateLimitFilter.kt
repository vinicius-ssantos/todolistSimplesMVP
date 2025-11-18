package com.viniss.todo.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class RateLimitFilter(
    private val rateLimitService: RateLimitService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val path = request.requestURI

        // Apply rate limiting only to authentication endpoints
        if (path.startsWith("/api/auth/")) {
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

    private fun getClientIP(request: HttpServletRequest): String {
        val xfHeader = request.getHeader("X-Forwarded-For")
        return if (xfHeader.isNullOrEmpty()) {
            request.remoteAddr
        } else {
            xfHeader.split(",").firstOrNull()?.trim() ?: request.remoteAddr
        }
    }
}
