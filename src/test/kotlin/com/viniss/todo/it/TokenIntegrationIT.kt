package com.viniss.todo.it

import com.viniss.todo.auth.InvalidTokenException
import com.viniss.todo.auth.JwtProps
import com.viniss.todo.auth.TokenService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.stereotype.Component
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Instant
import java.util.*

/**
 * Integra TokenService + filtro de auth + rota protegida.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TokenIntegrationIT.TestSecurity::class, TokenIntegrationIT.TestController::class)
class TokenIntegrationIT {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var tokenService: TokenService
    @Autowired lateinit var props: JwtProps

    private val fixedNow: Instant = Instant.parse("2024-01-01T00:00:00Z")

    // --- Casos ---

    @Test
    fun `sem token - 401`() {
        mockMvc.perform(get("/__auth/ping"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `token valido - 200 e retorna userId`() {
        val uid = UUID.randomUUID()
        val jwt = tokenService.signHS384(uid, ttlSeconds = 3600)

        mockMvc.perform(get("/__auth/ping").header("Authorization", "Bearer $jwt"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(uid.toString()))
    }

    @Test
    fun `assinatura invalida - 401`() {
        val uid = UUID.randomUUID()

        // outro segredo (diferente do bean do contexto)
        val otherSecretB64 = strongSecretB64(64)
        val badProps = JwtProps(
            issuer = props.issuer,
            audience = props.audience,
            clockSkewSeconds = props.clockSkewSeconds,
            secretB64 = otherSecretB64,
            jwksUri = null,
            acceptRS256 = false
        )
        val badSigner = TokenService(badProps) { fixedNow }

        val jwt = badSigner.signHS384(uid, ttlSeconds = 3600)

        mockMvc.perform(get("/__auth/ping").header("Authorization", "Bearer $jwt"))
            .andExpect(status().isUnauthorized)
    }

    // util p/ gerar segredo forte base64-url
    private fun strongSecretB64(bytes: Int = 64): String {
        val buff = ByteArray(bytes)
        java.security.SecureRandom().nextBytes(buff)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buff)
    }

    // --- Infra de teste (Security + Controller) ---

    @Configuration
    class TestSecurity {

        private val fixedNow: Instant = Instant.parse("2024-01-01T00:00:00Z")

        @Bean
        fun jwtProperties(): JwtProps {
            // 64 bytes => ok para HS384 (>= 48)
            val secretB64 = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(ByteArray(64)) // zeros; para teste serve
            return JwtProps(
                issuer = "tickr-api",
                audience = "tickr-web",
                clockSkewSeconds = 60,
                secretB64 = secretB64,
                jwksUri = null,
                acceptRS256 = false
            )
        }

        @Bean
        fun tokenService(props: JwtProps): TokenService =
            TokenService(props) { fixedNow }

        @Bean
        fun jwtAuthFilter(tokenService: TokenService) = TestJwtAuthFilter(tokenService)

        @Bean
        fun filterChain(http: HttpSecurity, filter: TestJwtAuthFilter): SecurityFilterChain =
            http.csrf { it.disable() }
                .authorizeHttpRequests {
                    it.requestMatchers("/__auth/public").permitAll()
                    it.anyRequest().authenticated()
                }
                .addFilterBefore(filter, UsernamePasswordAuthenticationFilter::class.java)
                .build()
    }

    /**
     * Filtro mínimo p/ teste: extrai Bearer, valida com TokenService,
     * injeta Authentication com principal = userId.
     */
    @Component
    class TestJwtAuthFilter(private val tokenService: TokenService) : OncePerRequestFilter() {

        override fun doFilterInternal(
            request: HttpServletRequest,
            response: HttpServletResponse,
            chain: FilterChain
        ) {
            val authz = request.getHeader("Authorization")
            if (authz == null || !authz.startsWith("Bearer ")) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
                return
            }
            val token = authz.removePrefix("Bearer ").trim()
            try {
                val p = tokenService.parseAndValidate(token)
                val authentication: Authentication =
                    UsernamePasswordAuthenticationToken(p.userId.toString(), null, emptyList())
                SecurityContextHolder.getContext().authentication = authentication
                chain.doFilter(request, response)
            } catch (_: InvalidTokenException) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
            }
        }

        override fun shouldNotFilter(request: HttpServletRequest): Boolean {
            val path = request.requestURI
            return path == "/__auth/public" || path == "/error"
        }
    }

    @RestController
    @RequestMapping("/__auth")
    class TestController {
        @GetMapping("/ping")
        fun ping(auth: Authentication): Map<String, String> =
            mapOf("userId" to auth.name)
    }
}
