package com.viniss.todo.auth

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(
    private val jwt: TokenService,
    private val entryPoint: JsonAuthEntryPoint,
    private val tokenBlacklistService: TokenBlacklistService
) : OncePerRequestFilter() {

    override fun doFilterInternal(req: HttpServletRequest, res: HttpServletResponse, chain: FilterChain) {
        val auth = req.getHeader("Authorization")?.trim()

        if (!auth.isNullOrBlank() && auth.startsWith("Bearer ", ignoreCase = true)) {
            val token = auth.substring(7).trim()
            try {
                // Check if token is blacklisted first
                if (tokenBlacklistService.isBlacklisted(token)) {
                    entryPoint.commence(req, res, BadCredentialsException("token_revoked"))
                    return
                }

                if (jwt.isValid(token)) {
                    val userId = jwt.extractUserId(token)      // sub -> UUID
                    val email = jwt.extractEmail(token)        // claim "email"
                    val principal = AuthUser(userId, email)
                    val authToken = UsernamePasswordAuthenticationToken(
                        principal, null, emptyList()
                    ).apply {
                        details = WebAuthenticationDetailsSource().buildDetails(req)
                    }
                    SecurityContextHolder.getContext().authentication = authToken
                }
            } catch (_: InvalidTokenException) {
                entryPoint.commence(req, res, BadCredentialsException("invalid_token"))
                return
            } catch (_: IllegalArgumentException) {
                entryPoint.commence(req, res, BadCredentialsException("invalid_token"))
                return
            }
        }
        chain.doFilter(req, res)
    }
}
