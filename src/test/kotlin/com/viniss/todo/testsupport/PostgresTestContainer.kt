package com.viniss.todo.testsupport

import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

object PostgresTestContainer {

    private val container: PostgreSQLContainer<*> = PostgreSQLContainer(
        DockerImageName.parse("postgres:16.4-alpine")
    ).apply {
        withDatabaseName("todo_it")
        withUsername("todo")
        withPassword("todo")
        withReuse(true)
    }

    private fun ensureStarted() {
        if (!container.isRunning) {
            container.start()
        }
    }

    fun register(registry: DynamicPropertyRegistry) {
        ensureStarted()
        registry.add("DB_URL") { container.jdbcUrl }
        registry.add("DB_USER") { container.username }
        registry.add("DB_PASSWORD") { container.password }
    }

    fun jdbcUrl(): String {
        ensureStarted()
        return container.jdbcUrl
    }

    fun username(): String {
        ensureStarted()
        return container.username
    }

    fun password(): String {
        ensureStarted()
        return container.password
    }
}
