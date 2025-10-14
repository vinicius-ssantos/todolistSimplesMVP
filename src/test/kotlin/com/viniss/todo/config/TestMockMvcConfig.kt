package com.viniss.todo.config

import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpMethod
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf

@TestConfiguration
class TestMockMvcConfig {

    @Bean
    fun defaultCsrfCustomizer(): MockMvcBuilderCustomizer =
        MockMvcBuilderCustomizer { builder ->
            builder.defaultRequest(
                MockMvcRequestBuilders.request(HttpMethod.GET, "/").with(csrf())
            )
        }
}
