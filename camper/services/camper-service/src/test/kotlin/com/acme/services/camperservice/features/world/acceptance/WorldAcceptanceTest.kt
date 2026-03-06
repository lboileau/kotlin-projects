package com.acme.services.camperservice.features.world.acceptance

import com.acme.services.camperservice.config.TestContainerConfig
import com.acme.services.camperservice.features.world.acceptance.fixture.WorldFixture
import com.acme.services.camperservice.features.world.dto.CreateWorldRequest
import com.acme.services.camperservice.features.world.dto.UpdateWorldRequest
import com.acme.services.camperservice.features.world.dto.WorldResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Import
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestContainerConfig::class)
class WorldAcceptanceTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    private lateinit var fixture: WorldFixture

    @BeforeEach
    fun setUp() {
        fixture = WorldFixture(jdbcTemplate)
        fixture.truncate()
    }

    @Nested
    inner class CreateWorld {

        @Test
        fun `POST returns 201 with created world`() {
            val request = CreateWorldRequest(name = "Earth", greeting = "Hello")
            val response = restTemplate.postForEntity("/api/worlds", request, WorldResponse::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            assertThat(response.body!!.name).isEqualTo("Earth")
            assertThat(response.body!!.greeting).isEqualTo("Hello")
            assertThat(response.body!!.id).isNotNull()
        }

        @Test
        fun `POST returns 400 when name is blank`() {
            val request = CreateWorldRequest(name = "", greeting = "Hello")
            val response = restTemplate.postForEntity("/api/worlds", request, Map::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        fun `POST returns 400 when greeting is blank`() {
            val request = CreateWorldRequest(name = "Earth", greeting = "")
            val response = restTemplate.postForEntity("/api/worlds", request, Map::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        fun `POST returns 409 when name already exists`() {
            fixture.insertWorld(name = "Earth", greeting = "Hello")

            val request = CreateWorldRequest(name = "Earth", greeting = "Hi")
            val response = restTemplate.postForEntity("/api/worlds", request, Map::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        }
    }

    @Nested
    inner class GetWorldById {

        @Test
        fun `GET by id returns 200 with world`() {
            val id = fixture.insertWorld(name = "Earth", greeting = "Hello")

            val response = restTemplate.getForEntity("/api/worlds/$id", WorldResponse::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.name).isEqualTo("Earth")
        }

        @Test
        fun `GET by id returns 404 when not found`() {
            val response = restTemplate.getForEntity("/api/worlds/${UUID.randomUUID()}", Map::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @Nested
    inner class GetAllWorlds {

        @Test
        fun `GET returns 200 with empty list`() {
            val response = restTemplate.getForEntity("/api/worlds", Array<WorldResponse>::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body).isEmpty()
        }

        @Test
        fun `GET returns 200 with all worlds`() {
            fixture.insertWorld(name = "Alpha", greeting = "Hi")
            fixture.insertWorld(name = "Beta", greeting = "Hey")

            val response = restTemplate.getForEntity("/api/worlds", Array<WorldResponse>::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body).hasSize(2)
        }
    }

    @Nested
    inner class UpdateWorld {

        @Test
        fun `PUT returns 200 with updated world`() {
            val id = fixture.insertWorld(name = "Earth", greeting = "Hello")
            val request = UpdateWorldRequest(name = "Terra")

            val response = restTemplate.exchange(
                "/api/worlds/$id",
                HttpMethod.PUT,
                HttpEntity(request),
                WorldResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.name).isEqualTo("Terra")
            assertThat(response.body!!.greeting).isEqualTo("Hello")
        }

        @Test
        fun `PUT returns 404 when not found`() {
            val request = UpdateWorldRequest(name = "Terra")
            val response = restTemplate.exchange(
                "/api/worlds/${UUID.randomUUID()}",
                HttpMethod.PUT,
                HttpEntity(request),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @Test
        fun `PUT returns 409 when renaming to existing name`() {
            fixture.insertWorld(name = "Earth", greeting = "Hello")
            val id = fixture.insertWorld(name = "Mars", greeting = "Hi")

            val request = UpdateWorldRequest(name = "Earth")
            val response = restTemplate.exchange(
                "/api/worlds/$id",
                HttpMethod.PUT,
                HttpEntity(request),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        }

        @Test
        fun `PUT returns 400 when name is blank`() {
            val id = fixture.insertWorld(name = "Earth", greeting = "Hello")

            val request = UpdateWorldRequest(name = "")
            val response = restTemplate.exchange(
                "/api/worlds/$id",
                HttpMethod.PUT,
                HttpEntity(request),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        fun `PUT returns 400 when greeting is blank`() {
            val id = fixture.insertWorld(name = "Earth", greeting = "Hello")

            val request = UpdateWorldRequest(greeting = "")
            val response = restTemplate.exchange(
                "/api/worlds/$id",
                HttpMethod.PUT,
                HttpEntity(request),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @Nested
    inner class DeleteWorld {

        @Test
        fun `DELETE returns 204 when world exists`() {
            val id = fixture.insertWorld(name = "Earth", greeting = "Hello")

            val response = restTemplate.exchange(
                "/api/worlds/$id",
                HttpMethod.DELETE,
                null,
                Void::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        }

        @Test
        fun `DELETE returns 404 when not found`() {
            val response = restTemplate.exchange(
                "/api/worlds/${UUID.randomUUID()}",
                HttpMethod.DELETE,
                null,
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @Nested
    inner class ReadYourOwnWrites {

        @Test
        fun `POST then GET returns the created world`() {
            val createRequest = CreateWorldRequest(name = "Earth", greeting = "Hello!")
            val createResponse = restTemplate.postForEntity("/api/worlds", createRequest, WorldResponse::class.java)
            assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
            val id = createResponse.body!!.id

            val getResponse = restTemplate.getForEntity("/api/worlds/$id", WorldResponse::class.java)
            assertThat(getResponse.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(getResponse.body!!.name).isEqualTo("Earth")
            assertThat(getResponse.body!!.greeting).isEqualTo("Hello!")
            assertThat(getResponse.body!!.id).isEqualTo(id)
        }

        @Test
        fun `POST then PUT then GET returns the updated world`() {
            val createRequest = CreateWorldRequest(name = "Earth", greeting = "Hello!")
            val createResponse = restTemplate.postForEntity("/api/worlds", createRequest, WorldResponse::class.java)
            assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
            val id = createResponse.body!!.id

            val updateRequest = UpdateWorldRequest(name = "Terra", greeting = "Hola!")
            val updateResponse = restTemplate.exchange(
                "/api/worlds/$id",
                HttpMethod.PUT,
                HttpEntity(updateRequest),
                WorldResponse::class.java
            )
            assertThat(updateResponse.statusCode).isEqualTo(HttpStatus.OK)

            val getResponse = restTemplate.getForEntity("/api/worlds/$id", WorldResponse::class.java)
            assertThat(getResponse.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(getResponse.body!!.name).isEqualTo("Terra")
            assertThat(getResponse.body!!.greeting).isEqualTo("Hola!")
            assertThat(getResponse.body!!.id).isEqualTo(id)
        }

        @Test
        fun `POST then DELETE then GET returns 404`() {
            val createRequest = CreateWorldRequest(name = "Earth", greeting = "Hello!")
            val createResponse = restTemplate.postForEntity("/api/worlds", createRequest, WorldResponse::class.java)
            assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
            val id = createResponse.body!!.id

            val deleteResponse = restTemplate.exchange(
                "/api/worlds/$id",
                HttpMethod.DELETE,
                null,
                Void::class.java
            )
            assertThat(deleteResponse.statusCode).isEqualTo(HttpStatus.NO_CONTENT)

            val getResponse = restTemplate.getForEntity("/api/worlds/$id", Map::class.java)
            assertThat(getResponse.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }
}
