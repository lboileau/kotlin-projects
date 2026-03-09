package com.acme.services.camperservice.features.user.acceptance

import com.acme.services.camperservice.config.TestContainerConfig
import com.acme.services.camperservice.features.user.acceptance.fixture.UserFixture
import com.acme.services.camperservice.features.user.dto.AuthRequest
import com.acme.services.camperservice.features.user.dto.AuthResponse
import com.acme.services.camperservice.features.user.dto.CreateUserRequest
import com.acme.services.camperservice.features.user.dto.UpdateUserRequest
import com.acme.services.camperservice.features.user.dto.UserResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Import
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestContainerConfig::class)
class UserAcceptanceTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    private lateinit var fixture: UserFixture

    @BeforeEach
    fun setUp() {
        fixture = UserFixture(jdbcTemplate)
        fixture.truncateAll()
    }

    @Nested
    inner class GetUser {

        @Test
        fun `GET returns 200 with user when found`() {
            val userId = fixture.insertUser(email = "alice@example.com", username = "alice")

            val response = restTemplate.getForEntity("/api/users/$userId", UserResponse::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.id).isEqualTo(userId)
            assertThat(response.body!!.email).isEqualTo("alice@example.com")
            assertThat(response.body!!.username).isEqualTo("alice")
        }

        @Test
        fun `GET returns 404 when user does not exist`() {
            val response = restTemplate.getForEntity("/api/users/${UUID.randomUUID()}", Map::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @Nested
    inner class CreateUser {

        @Test
        fun `POST returns 201 with created user`() {
            val request = CreateUserRequest(email = "alice@example.com", username = "alice")
            val response = restTemplate.postForEntity("/api/users", request, UserResponse::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            assertThat(response.body!!.email).isEqualTo("alice@example.com")
            assertThat(response.body!!.username).isEqualTo("alice")
            assertThat(response.body!!.id).isNotNull()
        }

        @Test
        fun `POST returns 201 with null username`() {
            val request = CreateUserRequest(email = "bob@example.com")
            val response = restTemplate.postForEntity("/api/users", request, UserResponse::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            assertThat(response.body!!.email).isEqualTo("bob@example.com")
            assertThat(response.body!!.username).isNull()
        }

        @Test
        fun `POST returns existing user when email already exists`() {
            val userId = fixture.insertUser(email = "existing@example.com", username = "existing")

            val request = CreateUserRequest(email = "existing@example.com", username = "different")
            val response = restTemplate.postForEntity("/api/users", request, UserResponse::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            assertThat(response.body!!.id).isEqualTo(userId)
            assertThat(response.body!!.username).isEqualTo("existing")
        }

        @Test
        fun `POST updates username when existing user has no username`() {
            fixture.insertUser(email = "invited@example.com", username = null)

            val request = CreateUserRequest(email = "invited@example.com", username = "now-registered")
            val response = restTemplate.postForEntity("/api/users", request, UserResponse::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            assertThat(response.body!!.username).isEqualTo("now-registered")
        }

        @Test
        fun `POST returns 400 when email is blank`() {
            val request = CreateUserRequest(email = "")
            val response = restTemplate.postForEntity("/api/users", request, Map::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @Nested
    inner class EmailNormalization {

        @Test
        fun `POST normalizes email on creation`() {
            val request = CreateUserRequest(email = "A.Li.Ce@Example.COM", username = "alice")
            val response = restTemplate.postForEntity("/api/users", request, UserResponse::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            assertThat(response.body!!.email).isEqualTo("alice@example.com")
        }

        @Test
        fun `POST auth finds user regardless of case and dots`() {
            restTemplate.postForEntity("/api/users", CreateUserRequest(email = "bob@example.com", username = "bob"), UserResponse::class.java)

            val authRequest = AuthRequest(email = "B.O.B@Example.COM")
            val response = restTemplate.postForEntity("/api/auth", authRequest, AuthResponse::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.username).isEqualTo("bob")
        }

        @Test
        fun `POST returns existing user when normalized email matches`() {
            val first = restTemplate.postForEntity("/api/users", CreateUserRequest(email = "carol@example.com", username = "carol"), UserResponse::class.java)

            val second = restTemplate.postForEntity("/api/users", CreateUserRequest(email = "C.A.R.O.L@Example.COM", username = "different"), UserResponse::class.java)

            assertThat(second.statusCode).isEqualTo(HttpStatus.CREATED)
            assertThat(second.body!!.id).isEqualTo(first.body!!.id)
            assertThat(second.body!!.username).isEqualTo("carol")
        }
    }

    @Nested
    inner class Authenticate {

        @Test
        fun `POST returns 200 with auth response when user exists`() {
            fixture.insertUser(email = "carol@example.com", username = "carol")

            val request = AuthRequest(email = "carol@example.com")
            val response = restTemplate.postForEntity("/api/auth", request, AuthResponse::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.email).isEqualTo("carol@example.com")
            assertThat(response.body!!.username).isEqualTo("carol")
        }

        @Test
        fun `POST returns 404 when user does not exist`() {
            val request = AuthRequest(email = "nobody@example.com")
            val response = restTemplate.postForEntity("/api/auth", request, Map::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @Test
        fun `POST returns 400 when email is blank`() {
            val request = AuthRequest(email = "")
            val response = restTemplate.postForEntity("/api/auth", request, Map::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        fun `POST returns 403 when user has no username`() {
            fixture.insertUser(email = "noname@example.com", username = null)

            val request = AuthRequest(email = "noname@example.com")
            val response = restTemplate.postForEntity("/api/auth", request, Map::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        }
    }

    @Nested
    inner class UpdateUser {

        @Test
        fun `PUT returns 200 with updated user`() {
            val userId = fixture.insertUser(email = "dave@example.com", username = "dave")

            val headers = HttpHeaders()
            headers.set("X-User-Id", userId.toString())
            val request = HttpEntity(UpdateUserRequest(username = "david"), headers)

            val response = restTemplate.exchange("/api/users/$userId", HttpMethod.PUT, request, UserResponse::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.username).isEqualTo("david")
            assertThat(response.body!!.email).isEqualTo("dave@example.com")
        }

        @Test
        fun `PUT returns 403 when requesting user is different from target`() {
            val userId = fixture.insertUser(email = "eve@example.com", username = "eve")
            val otherUserId = UUID.randomUUID()

            val headers = HttpHeaders()
            headers.set("X-User-Id", otherUserId.toString())
            val request = HttpEntity(UpdateUserRequest(username = "evelyn"), headers)

            val response = restTemplate.exchange("/api/users/$userId", HttpMethod.PUT, request, Map::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        }

        @Test
        fun `PUT returns 400 when username is blank`() {
            val userId = fixture.insertUser(email = "frank@example.com", username = "frank")

            val headers = HttpHeaders()
            headers.set("X-User-Id", userId.toString())
            val request = HttpEntity(UpdateUserRequest(username = ""), headers)

            val response = restTemplate.exchange("/api/users/$userId", HttpMethod.PUT, request, Map::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @Nested
    inner class ReadYourOwnWrites {

        @Test
        fun `POST user then GET returns the created user`() {
            val createRequest = CreateUserRequest(email = "getme@example.com", username = "getme")
            val createResponse = restTemplate.postForEntity("/api/users", createRequest, UserResponse::class.java)
            assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
            val userId = createResponse.body!!.id

            val getResponse = restTemplate.getForEntity("/api/users/$userId", UserResponse::class.java)
            assertThat(getResponse.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(getResponse.body!!.email).isEqualTo("getme@example.com")
            assertThat(getResponse.body!!.username).isEqualTo("getme")
        }

        @Test
        fun `POST user then POST auth returns the created user`() {
            val createRequest = CreateUserRequest(email = "ryw@example.com", username = "ryw")
            val createResponse = restTemplate.postForEntity("/api/users", createRequest, UserResponse::class.java)
            assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)

            val authRequest = AuthRequest(email = "ryw@example.com")
            val authResponse = restTemplate.postForEntity("/api/auth", authRequest, AuthResponse::class.java)
            assertThat(authResponse.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(authResponse.body!!.email).isEqualTo("ryw@example.com")
            assertThat(authResponse.body!!.id).isEqualTo(createResponse.body!!.id)
        }

        @Test
        fun `POST user then PUT update then POST auth returns updated username`() {
            val createResponse = restTemplate.postForEntity(
                "/api/users",
                CreateUserRequest(email = "update-flow@example.com", username = "old"),
                UserResponse::class.java
            )
            assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
            val userId = createResponse.body!!.id

            val headers = HttpHeaders()
            headers.set("X-User-Id", userId.toString())
            val updateResponse = restTemplate.exchange(
                "/api/users/$userId",
                HttpMethod.PUT,
                HttpEntity(UpdateUserRequest(username = "new"), headers),
                UserResponse::class.java
            )
            assertThat(updateResponse.statusCode).isEqualTo(HttpStatus.OK)

            val authResponse = restTemplate.postForEntity(
                "/api/auth",
                AuthRequest(email = "update-flow@example.com"),
                AuthResponse::class.java
            )
            assertThat(authResponse.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(authResponse.body!!.username).isEqualTo("new")
        }
    }

    private fun jsonEntityWithUserId(body: Any, userId: UUID): HttpEntity<Any> {
        val headers = HttpHeaders()
        headers.set("X-User-Id", userId.toString())
        return HttpEntity(body, headers)
    }
}
