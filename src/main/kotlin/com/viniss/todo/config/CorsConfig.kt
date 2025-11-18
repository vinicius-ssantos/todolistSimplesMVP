package com.viniss.todo.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@ConfigurationProperties("app.cors")
data class CorsProps(
    val allowedOrigins: List<String> = listOf(
        "http://localhost:3000",
        "http://localhost:5173",
        "https://v0.app",
        "https://v0.dev"
    ),
    val allowedOriginPatterns: List<String> = listOf(
        "https://*.v0.app",
        "https://*.v0.dev"
    ),
    val allowedMethods: List<String> = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"),
    // Specific headers instead of wildcard "*" for better security
    val allowedHeaders: List<String> = listOf(
        "Authorization",
        "Content-Type",
        "Accept",
        "Origin",
        "X-Requested-With"
    ),
    // Expose only necessary headers to the frontend
    val exposedHeaders: List<String> = listOf(
        "Authorization",
        "Content-Type"
    ),
    val allowCredentials: Boolean = true,
    val maxAgeSeconds: Long = 3600
)

@Configuration
@EnableConfigurationProperties(CorsProps::class)
class CorsConfig(
    private val corsProps: CorsProps
) {
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        if (corsProps.allowedOrigins.isNotEmpty()) {
            configuration.allowedOrigins = corsProps.allowedOrigins
        }
        if (corsProps.allowedOriginPatterns.isNotEmpty()) {
            configuration.allowedOriginPatterns = corsProps.allowedOriginPatterns
        }
        configuration.allowedMethods = corsProps.allowedMethods
        configuration.allowedHeaders = corsProps.allowedHeaders
        configuration.exposedHeaders = corsProps.exposedHeaders
        configuration.allowCredentials = corsProps.allowCredentials
        configuration.maxAge = corsProps.maxAgeSeconds

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
