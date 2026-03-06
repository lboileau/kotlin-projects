package com.acme.clients.worldclient

import com.acme.clients.common.Result
import com.acme.clients.common.error.ConflictError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.error.ValidationError
import com.acme.clients.worldclient.api.CreateWorldParam
import com.acme.clients.worldclient.api.DeleteWorldParam
import com.acme.clients.worldclient.api.GetByIdParam
import com.acme.clients.worldclient.api.GetListParam
import com.acme.clients.worldclient.api.UpdateWorldParam
import com.acme.clients.worldclient.api.WorldClient
import com.acme.clients.worldclient.test.WorldTestDb
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID

@Testcontainers
class JdbiWorldClientTest {

    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("camper_db")
            .withUsername("postgres")
            .withPassword("postgres")

        private lateinit var client: WorldClient

        @BeforeAll
        @JvmStatic
        fun setup() {
            WorldTestDb.cleanAndMigrate(postgres.jdbcUrl, postgres.username, postgres.password)

            System.setProperty("DB_URL", postgres.jdbcUrl)
            System.setProperty("DB_USER", postgres.username)
            System.setProperty("DB_PASSWORD", postgres.password)
            client = createWorldClient()
        }
    }

    @BeforeEach
    fun truncate() {
        val jdbi = org.jdbi.v3.core.Jdbi.create(postgres.jdbcUrl, postgres.username, postgres.password)
        jdbi.withHandle<Unit, Exception> { handle ->
            handle.createUpdate("TRUNCATE TABLE worlds CASCADE").execute()
        }
    }

    @Nested
    inner class GetById {
        @Test
        fun `getById returns world when it exists`() {
            val created = client.create(CreateWorldParam(name = "Earth", greeting = "Hello"))
            assertThat(created).isInstanceOf(Result.Success::class.java)
            val world = (created as Result.Success).value

            val result = client.getById(GetByIdParam(world.id))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val found = (result as Result.Success).value
            assertThat(found.name).isEqualTo("Earth")
            assertThat(found.greeting).isEqualTo("Hello")
        }

        @Test
        fun `getById returns NotFoundError when world does not exist`() {
            val result = client.getById(GetByIdParam(UUID.randomUUID()))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(NotFoundError::class.java)
        }
    }

    @Nested
    inner class GetList {
        @Test
        fun `getList returns all worlds ordered by name`() {
            client.create(CreateWorldParam(name = "Zebra", greeting = "Zello"))
            client.create(CreateWorldParam(name = "Alpha", greeting = "Ahoy"))

            val result = client.getList(GetListParam())
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val worlds = (result as Result.Success).value
            assertThat(worlds).hasSize(2)
            assertThat(worlds[0].name).isEqualTo("Alpha")
            assertThat(worlds[1].name).isEqualTo("Zebra")
        }

        @Test
        fun `getList respects limit and offset`() {
            client.create(CreateWorldParam(name = "A", greeting = "a"))
            client.create(CreateWorldParam(name = "B", greeting = "b"))
            client.create(CreateWorldParam(name = "C", greeting = "c"))

            val result = client.getList(GetListParam(limit = 1, offset = 1))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val worlds = (result as Result.Success).value
            assertThat(worlds).hasSize(1)
            assertThat(worlds[0].name).isEqualTo("B")
        }

        @Test
        fun `getList returns empty list when no worlds exist`() {
            val result = client.getList(GetListParam())
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val worlds = (result as Result.Success).value
            assertThat(worlds).isEmpty()
        }
    }

    @Nested
    inner class Create {
        @Test
        fun `create returns created world`() {
            val result = client.create(CreateWorldParam(name = "Mars", greeting = "Greetings"))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val world = (result as Result.Success).value
            assertThat(world.name).isEqualTo("Mars")
            assertThat(world.greeting).isEqualTo("Greetings")
            assertThat(world.id).isNotNull()
            assertThat(world.createdAt).isNotNull()
            assertThat(world.updatedAt).isNotNull()
        }

        @Test
        fun `create returns ConflictError for duplicate name`() {
            client.create(CreateWorldParam(name = "Earth", greeting = "Hello"))
            val result = client.create(CreateWorldParam(name = "Earth", greeting = "Hi"))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ConflictError::class.java)
        }

        @Test
        fun `create returns ValidationError for blank name`() {
            val result = client.create(CreateWorldParam(name = "", greeting = "Hello"))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
            assertThat((error as ValidationError).field).isEqualTo("name")
        }

        @Test
        fun `create returns ValidationError for blank greeting`() {
            val result = client.create(CreateWorldParam(name = "Venus", greeting = ""))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
            assertThat((error as ValidationError).field).isEqualTo("greeting")
        }
    }

    @Nested
    inner class Update {
        @Test
        fun `update returns updated world`() {
            val created = (client.create(CreateWorldParam(name = "Earth", greeting = "Hello")) as Result.Success).value

            val result = client.update(UpdateWorldParam(id = created.id, name = "Terra"))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val updated = (result as Result.Success).value
            assertThat(updated.name).isEqualTo("Terra")
            assertThat(updated.greeting).isEqualTo("Hello")
        }

        @Test
        fun `update returns NotFoundError when world does not exist`() {
            val result = client.update(UpdateWorldParam(id = UUID.randomUUID(), name = "Nope"))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(NotFoundError::class.java)
        }

        @Test
        fun `update returns ConflictError for duplicate name`() {
            client.create(CreateWorldParam(name = "Earth", greeting = "Hello"))
            val mars = (client.create(CreateWorldParam(name = "Mars", greeting = "Hi")) as Result.Success).value

            val result = client.update(UpdateWorldParam(id = mars.id, name = "Earth"))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ConflictError::class.java)
        }

        @Test
        fun `update returns ValidationError for blank name`() {
            val created = (client.create(CreateWorldParam(name = "Earth", greeting = "Hello")) as Result.Success).value

            val result = client.update(UpdateWorldParam(id = created.id, name = ""))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
        }

        @Test
        fun `update returns ValidationError for blank greeting`() {
            val created = (client.create(CreateWorldParam(name = "Earth", greeting = "Hello")) as Result.Success).value

            val result = client.update(UpdateWorldParam(id = created.id, greeting = ""))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `delete returns success when world exists`() {
            val created = (client.create(CreateWorldParam(name = "Earth", greeting = "Hello")) as Result.Success).value

            val result = client.delete(DeleteWorldParam(created.id))
            assertThat(result).isInstanceOf(Result.Success::class.java)

            val getResult = client.getById(GetByIdParam(created.id))
            assertThat(getResult).isInstanceOf(Result.Failure::class.java)
        }

        @Test
        fun `delete returns NotFoundError when world does not exist`() {
            val result = client.delete(DeleteWorldParam(UUID.randomUUID()))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(NotFoundError::class.java)
        }
    }
}
