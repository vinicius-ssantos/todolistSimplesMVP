package com.viniss.todo.auth

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets

/**
 * Produz respostas 401 em JSON coerente para consumidores HTTP.
 */
@Component
class JsonAuthEntryPoint(
    private val mapper: ObjectMapper
) : AuthenticationEntryPoint {

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        if (response.isCommitted) return

        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = StandardCharsets.UTF_8.name()
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate")
        response.setHeader(HttpHeaders.PRAGMA, "no-cache")

        val payload = AuthErrorResponse(
            error = "invalid_token",
            message = authException.message,
            path = request.requestURI ?: ""
        )

        mapper.writeValue(response.writer, payload)
        response.writer.flush()
    }
}
