package com.example.services.hello.config

import com.example.clients.worldclient.api.WorldClient
import com.example.clients.worldclient.createWorldClient
import com.example.clients.worldclient.test.WorldTestDb
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.testcontainers.containers.PostgreSQLContainer

@TestConfiguration
class TestContainerConfig {

    companion object {
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("hello_world_db")
            .withUsername("postgres")
            .withPassword("postgres")
            .also { it.start() }

        init {
            WorldTestDb.cleanAndMigrate(postgres.jdbcUrl, postgres.username, postgres.password)

            System.setProperty("DB_URL", postgres.jdbcUrl)
            System.setProperty("DB_USER", postgres.username)
            System.setProperty("DB_PASSWORD", postgres.password)
        }
    }

    @Bean
    @Primary
    fun worldClient(): WorldClient = createWorldClient()
}
