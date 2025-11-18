package com.viniss.todo.config


import com.viniss.todo.auth.JwtAuthFilter
import com.viniss.todo.auth.JwtProps
import com.viniss.todo.auth.JsonAuthEntryPoint
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter


@EnableConfigurationProperties(JwtProps::class)
@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthFilter: JwtAuthFilter,
    private val authenticationEntryPoint: JsonAuthEntryPoint
) {
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()


    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain = http
        .cors { }
        .csrf { it.disable() }
        .headers { headers ->
            headers
                // HSTS: Force HTTPS for 1 year, include subdomains
                .httpStrictTransportSecurity { hsts ->
                    hsts.includeSubDomains(true)
                        .maxAgeInSeconds(31536000)
                }
                // Prevent MIME-sniffing attacks
                .contentTypeOptions { }
                // Prevent clickjacking attacks
                .frameOptions { frame ->
                    frame.deny()
                }
                // XSS Protection (legacy browsers)
                .xssProtection { xss ->
                    xss.headerValue("1; mode=block")
                }
                // Referrer Policy
                .referrerPolicy { referrer ->
                    referrer.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                }
                // Permissions Policy (formerly Feature Policy)
                .permissionsPolicy { permissions ->
                    permissions.policy("geolocation=(), microphone=(), camera=()")
                }
        }
        .exceptionHandling { it.authenticationEntryPoint(authenticationEntryPoint) }
        .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        .authorizeHttpRequests {
            it.requestMatchers("/api/auth/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                .anyRequest().authenticated()
        }
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
        .build()


    @Bean
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager =
        config.authenticationManager
}
