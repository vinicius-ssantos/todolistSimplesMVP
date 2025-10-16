package com.viniss.todo.it

import com.viniss.todo.testsupport.PostgresTestContainer
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

class P0AuthorizationPostgresIT : P0AuthorizationIT() {

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun postgresProperties(registry: DynamicPropertyRegistry) {
            PostgresTestContainer.register(registry)
        }
    }
}
