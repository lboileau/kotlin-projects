package com.acme.clients.userclient

import com.acme.clients.common.Result
import com.acme.clients.common.error.ConflictError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.error.ValidationError
import com.acme.clients.userclient.api.CreateUserParam
import com.acme.clients.userclient.api.GetByEmailParam
import com.acme.clients.userclient.api.GetByIdParam
import com.acme.clients.userclient.api.GetOrCreateUserParam
import com.acme.clients.userclient.api.UpdateUserParam
import com.acme.clients.userclient.api.UserClient
import com.acme.clients.userclient.test.UserTestDb
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
class JdbiUserClientTest {

    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("camper_db")
            .withUsername("postgres")
            .withPassword("postgres")

        private lateinit var client: UserClient

        @BeforeAll
        @JvmStatic
        fun setup() {
            UserTestDb.cleanAndMigrate(postgres.jdbcUrl, postgres.username, postgres.password)

            System.setProperty("DB_URL", postgres.jdbcUrl)
            System.setProperty("DB_USER", postgres.username)
            System.setProperty("DB_PASSWORD", postgres.password)
            client = createUserClient()
        }
    }

    @BeforeEach
    fun truncate() {
        val jdbi = org.jdbi.v3.core.Jdbi.create(postgres.jdbcUrl, postgres.username, postgres.password)
        jdbi.withHandle<Unit, Exception> { handle ->
            handle.createUpdate("TRUNCATE TABLE users CASCADE").execute()
        }
    }

    @Nested
    inner class GetById {
        @Test
        fun `getById returns user when it exists`() {
            val created = (client.create(CreateUserParam(email = "alice@example.com", username = "alice")) as Result.Success).value

            val result = client.getById(GetByIdParam(created.id))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val found = (result as Result.Success).value
            assertThat(found.email).isEqualTo("alice@example.com")
            assertThat(found.username).isEqualTo("alice")
        }

        @Test
        fun `getById returns NotFoundError when user does not exist`() {
            val result = client.getById(GetByIdParam(UUID.randomUUID()))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(NotFoundError::class.java)
        }
    }

    @Nested
    inner class GetByEmail {
        @Test
        fun `getByEmail returns user when it exists`() {
            client.create(CreateUserParam(email = "bob@example.com", username = "bob"))

            val result = client.getByEmail(GetByEmailParam("bob@example.com"))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val found = (result as Result.Success).value
            assertThat(found.email).isEqualTo("bob@example.com")
            assertThat(found.username).isEqualTo("bob")
        }

        @Test
        fun `getByEmail returns NotFoundError when user does not exist`() {
            val result = client.getByEmail(GetByEmailParam("nobody@example.com"))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(NotFoundError::class.java)
        }
    }

    @Nested
    inner class Create {
        @Test
        fun `create returns created user with username`() {
            val result = client.create(CreateUserParam(email = "carol@example.com", username = "carol"))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val user = (result as Result.Success).value
            assertThat(user.email).isEqualTo("carol@example.com")
            assertThat(user.username).isEqualTo("carol")
            assertThat(user.id).isNotNull()
            assertThat(user.createdAt).isNotNull()
            assertThat(user.updatedAt).isNotNull()
        }

        @Test
        fun `create returns created user with null username`() {
            val result = client.create(CreateUserParam(email = "dave@example.com"))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val user = (result as Result.Success).value
            assertThat(user.email).isEqualTo("dave@example.com")
            assertThat(user.username).isNull()
        }

        @Test
        fun `create returns ConflictError for duplicate email`() {
            client.create(CreateUserParam(email = "dup@example.com"))
            val result = client.create(CreateUserParam(email = "dup@example.com"))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ConflictError::class.java)
        }

        @Test
        fun `create returns ValidationError for blank email`() {
            val result = client.create(CreateUserParam(email = ""))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
            assertThat((error as ValidationError).field).isEqualTo("email")
        }
    }

    @Nested
    inner class EmailNormalization {
        @Test
        fun `create normalizes email to lowercase without dots in local part`() {
            val result = client.create(CreateUserParam(email = "A.Li.Ce@Example.COM", username = "alice"))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val user = (result as Result.Success).value
            assertThat(user.email).isEqualTo("alice@example.com")
        }

        @Test
        fun `getByEmail finds user regardless of case and dots`() {
            client.create(CreateUserParam(email = "alice@example.com", username = "alice"))

            val result = client.getByEmail(GetByEmailParam("A.Li.Ce@Example.COM"))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val user = (result as Result.Success).value
            assertThat(user.username).isEqualTo("alice")
        }

        @Test
        fun `create returns ConflictError for email that normalizes to same value`() {
            client.create(CreateUserParam(email = "alice@example.com"))
            val result = client.create(CreateUserParam(email = "A.Li.Ce@Example.COM"))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ConflictError::class.java)
        }

        @Test
        fun `getOrCreate finds existing user regardless of case and dots`() {
            val created = (client.create(CreateUserParam(email = "bob@example.com", username = "bob")) as Result.Success).value

            val result = client.getOrCreate(GetOrCreateUserParam(email = "B.O.B@Example.COM", username = "different"))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val user = (result as Result.Success).value
            assertThat(user.id).isEqualTo(created.id)
            assertThat(user.username).isEqualTo("bob")
        }
    }

    @Nested
    inner class GetOrCreate {
        @Test
        fun `getOrCreate returns existing user when email exists`() {
            val created = (client.create(CreateUserParam(email = "exists@example.com", username = "existing")) as Result.Success).value

            val result = client.getOrCreate(GetOrCreateUserParam(email = "exists@example.com", username = "different"))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val user = (result as Result.Success).value
            assertThat(user.id).isEqualTo(created.id)
            assertThat(user.username).isEqualTo("existing")
        }

        @Test
        fun `getOrCreate creates new user when email does not exist`() {
            val result = client.getOrCreate(GetOrCreateUserParam(email = "new@example.com", username = "newuser"))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val user = (result as Result.Success).value
            assertThat(user.email).isEqualTo("new@example.com")
            assertThat(user.username).isEqualTo("newuser")
        }

        @Test
        fun `getOrCreate creates user with null username`() {
            val result = client.getOrCreate(GetOrCreateUserParam(email = "noname@example.com"))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val user = (result as Result.Success).value
            assertThat(user.email).isEqualTo("noname@example.com")
            assertThat(user.username).isNull()
        }
    }

    @Nested
    inner class Update {
        @Test
        fun `update returns updated user`() {
            val created = (client.create(CreateUserParam(email = "eve@example.com", username = "eve")) as Result.Success).value

            val result = client.update(UpdateUserParam(id = created.id, username = "evelyn"))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val updated = (result as Result.Success).value
            assertThat(updated.username).isEqualTo("evelyn")
            assertThat(updated.email).isEqualTo("eve@example.com")
        }

        @Test
        fun `update returns NotFoundError when user does not exist`() {
            val result = client.update(UpdateUserParam(id = UUID.randomUUID(), username = "nope"))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(NotFoundError::class.java)
        }

        @Test
        fun `update returns ValidationError for blank username`() {
            val created = (client.create(CreateUserParam(email = "frank@example.com", username = "frank")) as Result.Success).value

            val result = client.update(UpdateUserParam(id = created.id, username = ""))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
            assertThat((error as ValidationError).field).isEqualTo("username")
        }
    }
}
