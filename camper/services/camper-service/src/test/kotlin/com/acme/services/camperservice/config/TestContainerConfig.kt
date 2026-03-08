package com.acme.services.camperservice.config

import com.acme.clients.assignmentclient.api.AssignmentClient
import com.acme.clients.assignmentclient.createAssignmentClient
import com.acme.clients.itemclient.api.ItemClient
import com.acme.clients.itemclient.createItemClient
import com.acme.clients.planclient.api.PlanClient
import com.acme.clients.planclient.createPlanClient
import com.acme.clients.userclient.api.UserClient
import com.acme.clients.userclient.createUserClient
import com.acme.clients.worldclient.api.WorldClient
import com.acme.clients.worldclient.createWorldClient
import com.acme.clients.worldclient.test.WorldTestDb
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.testcontainers.containers.PostgreSQLContainer

@TestConfiguration
class TestContainerConfig {

    companion object {
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("camper_db")
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

    @Bean
    @Primary
    fun userClient(): UserClient = createUserClient()

    @Bean
    @Primary
    fun planClient(): PlanClient = createPlanClient()

    @Bean
    @Primary
    fun itemClient(): ItemClient = createItemClient()

    @Bean
    @Primary
    fun assignmentClient(): AssignmentClient = createAssignmentClient()
}
