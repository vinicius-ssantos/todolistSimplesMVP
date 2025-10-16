package com.viniss.todo.testsupport

import org.slf4j.LoggerFactory
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.sql.DriverManager
import java.util.concurrent.atomic.AtomicBoolean

object PostgresTestContainer {

    private val log = LoggerFactory.getLogger(PostgresTestContainer::class.java)
    private val delegate: DbDelegate by lazy { selectDelegate() }

    fun register(registry: DynamicPropertyRegistry) {
        delegate.ensureStarted()
        delegate.register(registry)
    }

    fun jdbcUrl(): String {
        delegate.ensureStarted()
        return delegate.jdbcUrl()
    }

    fun username(): String {
        delegate.ensureStarted()
        return delegate.username()
    }

    fun password(): String {
        delegate.ensureStarted()
        return delegate.password()
    }

    private fun selectDelegate(): DbDelegate =
        try {
            ContainerDelegate().also { it.ensureStarted() }
        } catch (ex: Throwable) {
            if (shouldFallback(ex)) {
                log.warn("Docker indisponível — usando H2 em modo PostgreSQL para os testes.", ex)
                H2FallbackDelegate().also { it.ensureStarted() }
            } else {
                throw ex
            }
        }

    private fun shouldFallback(error: Throwable?): Boolean {
        if (error == null) return false
        val message = error.message.orEmpty().lowercase()
        if ("docker" in message && "environment" in message) return true
        return shouldFallback(error.cause)
    }

    private interface DbDelegate {
        fun ensureStarted()
        fun register(registry: DynamicPropertyRegistry)
        fun jdbcUrl(): String
        fun username(): String
        fun password(): String
    }

    private class ContainerDelegate : DbDelegate {
        private val container: PostgreSQLContainer<*> = PostgreSQLContainer(
            DockerImageName.parse("postgres:16.4-alpine")
        ).apply {
            withDatabaseName("todo_it")
            withUsername("todo")
            withPassword("todo")
            withReuse(true)
        }

        override fun ensureStarted() {
            if (!container.isRunning) {
                container.start()
            }
        }

        override fun register(registry: DynamicPropertyRegistry) {
            registry.add("DB_URL") { container.jdbcUrl }
            registry.add("DB_USER") { container.username }
            registry.add("DB_PASSWORD") { container.password }
        }

        override fun jdbcUrl(): String = container.jdbcUrl
        override fun username(): String = container.username
        override fun password(): String = container.password
    }

    private class H2FallbackDelegate : DbDelegate {
        private val started = AtomicBoolean(false)
        private val url =
            "jdbc:h2:mem:todo_it_pg_fallback;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;DEFAULT_NULL_ORDERING=HIGH"
        private val username = "sa"
        private val password = ""

        override fun ensureStarted() {
            if (started.compareAndSet(false, true)) {
                DriverManager.getConnection(url, username, password).use { _ -> }
            }
        }

        override fun register(registry: DynamicPropertyRegistry) {
            registry.add("DB_URL") { url }
            registry.add("DB_USER") { username }
            registry.add("DB_PASSWORD") { password }
        }

        override fun jdbcUrl(): String = url
        override fun username(): String = username
        override fun password(): String = password
    }
}
