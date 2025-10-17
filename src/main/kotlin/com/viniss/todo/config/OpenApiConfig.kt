package com.viniss.todo.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    companion object {
        private const val SECURITY_SCHEME_NAME = "bearer-jwt"
    }

    @Bean
    fun apiSpec(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("Tickr Todo API")
                .version("1.0.0")
                .description(
                    """
                    REST API for Tickr, a multi-tenant Todo application.
                    Endpoints require Bearer JWT tokens issued by /api/auth/register or /api/auth/login.
                    """.trimIndent()
                )
                .contact(
                    Contact()
                        .name("Tickr Team")
                        .email("support@tickr.local")
                )
        )
        .components(
            Components().addSecuritySchemes(
                SECURITY_SCHEME_NAME,
                SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Provide the JWT issued by the auth endpoints.")
            )
        )
        .addSecurityItem(
            SecurityRequirement().addList(SECURITY_SCHEME_NAME)
        )
}
