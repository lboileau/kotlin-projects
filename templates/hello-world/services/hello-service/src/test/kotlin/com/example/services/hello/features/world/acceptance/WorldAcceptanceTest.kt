package com.example.services.hello.features.world.acceptance

import com.example.services.hello.config.TestContainerConfig
import com.example.services.hello.features.world.acceptance.fixture.WorldFixture
import com.example.services.hello.features.world.dto.CreateWorldRequest
import com.example.services.hello.features.world.dto.UpdateWorldRequest
import com.example.services.hello.features.world.dto.WorldResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Import
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestContainerConfig::class)
class WorldAcceptanceTest {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private val mapper = jacksonObjectMapper().apply {
        findAndRegisterModules()
    }

    private lateinit var fixture: WorldFixture

    @BeforeEach
    fun setUp() {
        fixture = WorldFixture(jdbcTemplate)
        fixture.truncate()
    }

    @Nested
    inner class GetById {
        @Test
        fun `GET worlds by id returns world when it exists`() {
            val id = fixture.createWorld(name = "Test World", greeting = "Hello!")

            val response = restTemplate.getForEntity("/api/worlds/$id", WorldResponse::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body?.name).isEqualTo("Test World")
            assertThat(response.body?.greeting).isEqualTo("Hello!")
        }

        @Test
        fun `GET worlds by id returns 404 when world does not exist`() {
            val response = restTemplate.getForEntity("/api/worlds/${UUID.randomUUID()}", Map::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @Nested
    inner class GetAll {
        @Test
        fun `GET worlds returns all worlds`() {
            fixture.createWorld(name = "Alpha", greeting = "Hi!")
            fixture.createWorld(name = "Beta", greeting = "Hey!")

            val response = restTemplate.getForEntity("/api/worlds", Array<WorldResponse>::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body).hasSize(2)
        }

        @Test
        fun `GET worlds returns empty list when no worlds exist`() {
            val response = restTemplate.getForEntity("/api/worlds", Array<WorldResponse>::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body).isEmpty()
        }
    }

    @Nested
    inner class Create {
        @Test
        fun `POST worlds creates a new world`() {
            val request = CreateWorldRequest(name = "New World", greeting = "Welcome!")

            val response = restTemplate.postForEntity("/api/worlds", request, WorldResponse::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            assertThat(response.body?.name).isEqualTo("New World")
            assertThat(response.body?.greeting).isEqualTo("Welcome!")
            assertThat(response.body?.id).isNotNull()
        }

        @Test
        fun `POST worlds returns 409 when name already exists`() {
            fixture.createWorld(name = "Existing", greeting = "Hi!")
            val request = CreateWorldRequest(name = "Existing", greeting = "Hello!")

            val response = restTemplate.postForEntity("/api/worlds", request, Map::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        }

        @Test
        fun `POST worlds returns 400 when name is blank`() {
            val request = CreateWorldRequest(name = "", greeting = "Hello!")

            val response = restTemplate.postForEntity("/api/worlds", request, Map::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @Nested
    inner class Update {
        @Test
        fun `PUT worlds updates an existing world`() {
            val id = fixture.createWorld(name = "Old Name", greeting = "Old Greeting")
            val request = UpdateWorldRequest(name = "New Name", greeting = "New Greeting")

            val response = restTemplate.exchange(
                "/api/worlds/$id", HttpMethod.PUT, HttpEntity(request), WorldResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body?.name).isEqualTo("New Name")
            assertThat(response.body?.greeting).isEqualTo("New Greeting")
        }

        @Test
        fun `PUT worlds returns 404 when world does not exist`() {
            val request = UpdateWorldRequest(name = "Name")

            val response = restTemplate.exchange(
                "/api/worlds/${UUID.randomUUID()}", HttpMethod.PUT, HttpEntity(request), Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `DELETE worlds deletes an existing world`() {
            val id = fixture.createWorld(name = "To Delete", greeting = "Bye!")

            val response = restTemplate.exchange(
                "/api/worlds/$id", HttpMethod.DELETE, null, Void::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        }

        @Test
        fun `DELETE worlds returns 404 when world does not exist`() {
            val response = restTemplate.exchange(
                "/api/worlds/${UUID.randomUUID()}", HttpMethod.DELETE, null, Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @Nested
    inner class ReadYourOwnWrites {
        @Test
        fun `POST then GET returns the created world`() {
            // 1. POST to create
            val createRequest = mapper.writeValueAsString(mapOf("name" to "Earth", "greeting" to "Hello!"))
            val createResponse = restTemplate.exchange(
                "/api/worlds", HttpMethod.POST, jsonEntity(createRequest), String::class.java
            )
            assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
            val created = mapper.readValue<Map<String, Any>>(createResponse.body!!)
            val id = created["id"] as String

            // 2. GET to verify
            val getResponse = restTemplate.getForEntity("/api/worlds/$id", String::class.java)
            assertThat(getResponse.statusCode).isEqualTo(HttpStatus.OK)
            val body = mapper.readValue<Map<String, Any>>(getResponse.body!!)
            assertThat(body["name"]).isEqualTo("Earth")
            assertThat(body["greeting"]).isEqualTo("Hello!")
            assertThat(body["id"]).isEqualTo(id)
        }

        @Test
        fun `POST then PUT then GET returns the updated world`() {
            // 1. POST to create
            val createRequest = mapper.writeValueAsString(mapOf("name" to "Earth", "greeting" to "Hello!"))
            val createResponse = restTemplate.exchange(
                "/api/worlds", HttpMethod.POST, jsonEntity(createRequest), String::class.java
            )
            assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
            val created = mapper.readValue<Map<String, Any>>(createResponse.body!!)
            val id = created["id"] as String

            // 2. PUT to update
            val updateRequest = mapper.writeValueAsString(mapOf("name" to "Terra", "greeting" to "Hola!"))
            val updateResponse = restTemplate.exchange(
                "/api/worlds/$id", HttpMethod.PUT, jsonEntity(updateRequest), String::class.java
            )
            assertThat(updateResponse.statusCode).isEqualTo(HttpStatus.OK)

            // 3. GET to verify
            val getResponse = restTemplate.getForEntity("/api/worlds/$id", String::class.java)
            assertThat(getResponse.statusCode).isEqualTo(HttpStatus.OK)
            val body = mapper.readValue<Map<String, Any>>(getResponse.body!!)
            assertThat(body["name"]).isEqualTo("Terra")
            assertThat(body["greeting"]).isEqualTo("Hola!")
            assertThat(body["id"]).isEqualTo(id)
        }

        @Test
        fun `POST then DELETE then GET returns 404`() {
            // 1. POST to create
            val createRequest = mapper.writeValueAsString(mapOf("name" to "Earth", "greeting" to "Hello!"))
            val createResponse = restTemplate.exchange(
                "/api/worlds", HttpMethod.POST, jsonEntity(createRequest), String::class.java
            )
            assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
            val created = mapper.readValue<Map<String, Any>>(createResponse.body!!)
            val id = created["id"] as String

            // 2. DELETE
            val deleteResponse = restTemplate.exchange(
                "/api/worlds/$id", HttpMethod.DELETE, null, String::class.java
            )
            assertThat(deleteResponse.statusCode).isEqualTo(HttpStatus.NO_CONTENT)

            // 3. GET to verify gone
            val getResponse = restTemplate.getForEntity("/api/worlds/$id", String::class.java)
            assertThat(getResponse.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    private fun jsonEntity(body: String): HttpEntity<String> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        return HttpEntity(body, headers)
    }
}
