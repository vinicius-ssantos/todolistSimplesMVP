package com.viniss.todo.config

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order

/**
 * Configuration for enforcing HTTPS in production environments.
 *
 * Enable this by setting app.security.require-https=true in application.yml
 * or via environment variable APP_SECURITY_REQUIRE_HTTPS=true
 */
@Configuration
class HttpsEnforcerConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ConditionalOnProperty(
        prefix = "app.security",
        name = ["require-https"],
        havingValue = "true",
        matchIfMissing = false
    )
    fun httpsEnforcerFilter(): Filter {
        return Filter { request, response, chain ->
            val httpRequest = request as HttpServletRequest
            val httpResponse = response as HttpServletResponse

            // Check if request is not secure and not from localhost
            val isLocalhost = httpRequest.serverName.contains("localhost") ||
                             httpRequest.serverName == "127.0.0.1"

            if (!httpRequest.isSecure && !isLocalhost) {
                // Redirect HTTP to HTTPS
                val httpsUrl = buildHttpsUrl(httpRequest)
                httpResponse.setHeader("Location", httpsUrl)
                httpResponse.status = HttpServletResponse.SC_MOVED_PERMANENTLY
            } else {
                chain.doFilter(request, response)
            }
        }
    }

    private fun buildHttpsUrl(request: HttpServletRequest): String {
        val serverName = request.serverName
        val requestUri = request.requestURI
        val queryString = request.queryString

        return buildString {
            append("https://")
            append(serverName)

            // Include port if non-standard HTTPS port
            if (request.serverPort != 80 && request.serverPort != 443) {
                append(":").append(request.serverPort)
            }

            append(requestUri)

            if (queryString != null) {
                append("?").append(queryString)
            }
        }
    }
}
