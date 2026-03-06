package com.example.clients.worldclient

import com.example.clients.common.Result
import com.example.clients.common.error.ConflictError
import com.example.clients.common.error.NotFoundError
import com.example.clients.common.error.ValidationError
import com.example.clients.worldclient.api.CreateWorldParam
import com.example.clients.worldclient.api.DeleteWorldParam
import com.example.clients.worldclient.api.GetByIdParam
import com.example.clients.worldclient.api.GetListParam
import com.example.clients.worldclient.api.UpdateWorldParam
import com.example.clients.worldclient.api.WorldClient
import com.example.clients.worldclient.test.WorldTestDb
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
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("hello_world_db")
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
    fun cleanTable() {
        org.jdbi.v3.core.Jdbi.create(postgres.jdbcUrl, postgres.username, postgres.password)
            .withHandle<Unit, Exception> { handle ->
                handle.execute("TRUNCATE TABLE worlds CASCADE")
            }
    }

    @Nested
    inner class GetById {
        @Test
        fun `getById returns world when it exists`() {
            val created = client.create(CreateWorldParam("Test World", "Hello!"))
            assertThat(created).isInstanceOf(Result.Success::class.java)
            val world = (created as Result.Success).value

            val result = client.getById(GetByIdParam(world.id))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val found = (result as Result.Success).value
            assertThat(found.name).isEqualTo("Test World")
            assertThat(found.greeting).isEqualTo("Hello!")
        }

        @Test
        fun `getById returns NotFoundError when world does not exist`() {
            val result = client.getById(GetByIdParam(UUID.randomUUID()))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).error).isInstanceOf(NotFoundError::class.java)
        }
    }

    @Nested
    inner class GetList {
        @Test
        fun `getList returns all worlds ordered by name`() {
            client.create(CreateWorldParam("Bravo", "Hi Bravo"))
            client.create(CreateWorldParam("Alpha", "Hi Alpha"))

            val result = client.getList(GetListParam())
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val worlds = (result as Result.Success).value
            assertThat(worlds).hasSize(2)
            assertThat(worlds[0].name).isEqualTo("Alpha")
            assertThat(worlds[1].name).isEqualTo("Bravo")
        }

        @Test
        fun `getList returns empty list when no worlds exist`() {
            val result = client.getList(GetListParam())
            assertThat(result).isInstanceOf(Result.Success::class.java)
            assertThat((result as Result.Success).value).isEmpty()
        }

        @Test
        fun `getList respects limit and offset`() {
            client.create(CreateWorldParam("Alpha", "Hi"))
            client.create(CreateWorldParam("Bravo", "Hi"))
            client.create(CreateWorldParam("Charlie", "Hi"))

            val result = client.getList(GetListParam(limit = 1, offset = 1))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val worlds = (result as Result.Success).value
            assertThat(worlds).hasSize(1)
            assertThat(worlds[0].name).isEqualTo("Bravo")
        }
    }

    @Nested
    inner class Create {
        @Test
        fun `create returns new world`() {
            val result = client.create(CreateWorldParam("New World", "Welcome!"))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val world = (result as Result.Success).value
            assertThat(world.name).isEqualTo("New World")
            assertThat(world.greeting).isEqualTo("Welcome!")
            assertThat(world.id).isNotNull()
            assertThat(world.createdAt).isNotNull()
            assertThat(world.updatedAt).isNotNull()
        }

        @Test
        fun `create returns ConflictError for duplicate name`() {
            client.create(CreateWorldParam("Duplicate", "Hi"))
            val result = client.create(CreateWorldParam("Duplicate", "Hello"))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).error).isInstanceOf(ConflictError::class.java)
        }

        @Test
        fun `create returns ValidationError for blank name`() {
            val result = client.create(CreateWorldParam("", "Hello"))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).error).isInstanceOf(ValidationError::class.java)
        }

        @Test
        fun `create returns ValidationError for blank greeting`() {
            val result = client.create(CreateWorldParam("Valid", ""))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).error).isInstanceOf(ValidationError::class.java)
        }
    }

    @Nested
    inner class Update {
        @Test
        fun `update returns updated world`() {
            val created = (client.create(CreateWorldParam("Original", "Hi")) as Result.Success).value
            val result = client.update(UpdateWorldParam(created.id, name = "Updated"))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val updated = (result as Result.Success).value
            assertThat(updated.name).isEqualTo("Updated")
            assertThat(updated.greeting).isEqualTo("Hi")
        }

        @Test
        fun `update returns NotFoundError for missing world`() {
            val result = client.update(UpdateWorldParam(UUID.randomUUID(), name = "Updated"))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).error).isInstanceOf(NotFoundError::class.java)
        }

        @Test
        fun `update returns ConflictError for duplicate name`() {
            client.create(CreateWorldParam("Existing", "Hi"))
            val created = (client.create(CreateWorldParam("ToUpdate", "Hi")) as Result.Success).value
            val result = client.update(UpdateWorldParam(created.id, name = "Existing"))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).error).isInstanceOf(ConflictError::class.java)
        }

        @Test
        fun `update returns ValidationError for blank name`() {
            val created = (client.create(CreateWorldParam("Valid", "Hi")) as Result.Success).value
            val result = client.update(UpdateWorldParam(created.id, name = "  "))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).error).isInstanceOf(ValidationError::class.java)
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `delete returns success when world exists`() {
            val created = (client.create(CreateWorldParam("ToDelete", "Bye")) as Result.Success).value
            val result = client.delete(DeleteWorldParam(created.id))
            assertThat(result).isInstanceOf(Result.Success::class.java)

            val getResult = client.getById(GetByIdParam(created.id))
            assertThat(getResult).isInstanceOf(Result.Failure::class.java)
        }

        @Test
        fun `delete returns NotFoundError for missing world`() {
            val result = client.delete(DeleteWorldParam(UUID.randomUUID()))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).error).isInstanceOf(NotFoundError::class.java)
        }
    }
}
