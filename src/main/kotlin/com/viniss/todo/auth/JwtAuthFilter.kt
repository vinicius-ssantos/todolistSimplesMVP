package com.viniss.todo.auth


import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter


@Component
class JwtAuthFilter(
    private val jwt: JwtService,
    private val users: AppUserRepository
) : OncePerRequestFilter() {
    override fun doFilterInternal(req: HttpServletRequest, res: HttpServletResponse, chain: FilterChain) {
        val bearer = req.getHeader("Authorization")
        val token = bearer?.takeIf { it.startsWith("Bearer ") }?.removePrefix("Bearer ")


        if (!token.isNullOrBlank() && SecurityContextHolder.getContext().authentication == null) {
            if (jwt.isValid(token)) {

                val userId = jwt.extractUserId(token)      // sub -> UUID
                val email  = jwt.extractEmail(token)       // claim "email"
                val principal = AuthUser(userId, email)
                val auth = UsernamePasswordAuthenticationToken(principal, null, emptyList())
                auth.details = WebAuthenticationDetailsSource().buildDetails(req)
                SecurityContextHolder.getContext().authentication = auth

            }

        }
        chain.doFilter(req, res)
    }
}