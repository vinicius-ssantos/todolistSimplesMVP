package com.viniss.todo.config


import com.viniss.todo.auth.JwtAuthFilter
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


@Configuration
@EnableWebSecurity
class SecurityConfig(private val jwtAuthFilter: JwtAuthFilter) {
@Bean fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()


@Bean
fun filterChain(http: HttpSecurity): SecurityFilterChain = http
.csrf { it.disable() }
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