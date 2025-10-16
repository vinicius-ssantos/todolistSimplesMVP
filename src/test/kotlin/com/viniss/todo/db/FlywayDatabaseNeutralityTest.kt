package com.viniss.todo.db

import com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable
import com.viniss.todo.testsupport.PostgresTestContainer
import org.assertj.core.api.Assertions.assertThatCode
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test

class FlywayDatabaseNeutralityTest {

    @Test
    fun `flyway info and migrate succeed on H2 using env`() {
        runWithEnvironment(
            url = "jdbc:h2:mem:flyway-neutrality;DB_CLOSE_DELAY=-1",
            user = "sa",
            password = ""
        )
    }

    @Test
    fun `flyway info and migrate succeed on Postgres using env`() {
        runWithEnvironment(
            url = PostgresTestContainer.jdbcUrl(),
            user = PostgresTestContainer.username(),
            password = PostgresTestContainer.password()
        )
    }

    private fun runWithEnvironment(url: String, user: String, password: String) {
        assertThatCode {
            withEnvironmentVariable("DB_URL", url)
                .and("DB_USER", user)
                .and("DB_PASSWORD", password)
                .and("SPRING_FLYWAY_LOCATIONS", "classpath:dbmigration")
                .execute {
                    val flyway = configureFlywayFromEnv()
                    flyway.clean()
                    flyway.info()
                    flyway.migrate()
                    flyway.info()
                    flyway.clean()
                }
        }.doesNotThrowAnyException()
    }

    private fun configureFlywayFromEnv(): Flyway {
        val url = requireNotNull(System.getenv("DB_URL")) { "DB_URL not set" }
        val user = System.getenv("DB_USER")
        val password = System.getenv("DB_PASSWORD")
        val locations = System.getenv("SPRING_FLYWAY_LOCATIONS") ?: "classpath:dbmigration"

        return Flyway.configure()
            .dataSource(url, user, password)
            .locations(locations)
            .cleanDisabled(false)
            .load()
    }
}
