package com.viniss.todo.auth

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
@EnableConfigurationProperties(JwtProps::class)
class JwtConfig {
    @Bean
    fun tokenService(props: JwtProps): TokenService {
        val nowProvider = { Clock.systemUTC().instant() }
        return if (props.acceptRS256) {
            NimbusRsaTokenService(props, nowProvider)
        } else {
            JjwtHmacTokenService(props, nowProvider)
        }
    }
}
