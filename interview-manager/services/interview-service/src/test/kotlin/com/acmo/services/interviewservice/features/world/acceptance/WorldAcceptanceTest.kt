package com.acmo.services.interviewservice.features.world.acceptance

import com.acmo.services.interviewservice.config.TestContainerConfig
import com.acmo.services.interviewservice.features.world.acceptance.fixture.WorldFixture
import com.acmo.services.interviewservice.features.world.dto.CreateWorldRequest
import com.acmo.services.interviewservice.features.world.dto.UpdateWorldRequest
import com.acmo.services.interviewservice.features.world.dto.WorldResponse
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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

            val response = restTemplate.getForEntity("/api/worlds/$id", String::class.java)

            assertThat(response.statusCode.value()).isEqualTo(200)
            val body = mapper.readValue<Map<String, Any>>(response.body!!)
            assertThat(body["id"]).isEqualTo(id.toString())
            assertThat(body["name"]).isEqualTo("Test World")
            assertThat(body["greeting"]).isEqualTo("Hello!")
            assertThat(body["createdAt"]).isNotNull()
            assertThat(body["updatedAt"]).isNotNull()
        }

        @Test
        fun `GET worlds by id returns 404 when world does not exist`() {
            val response = restTemplate.getForEntity("/api/worlds/${UUID.randomUUID()}", String::class.java)

            assertThat(response.statusCode.value()).isEqualTo(404)
            val body = mapper.readValue<Map<String, Any>>(response.body!!)
            assertThat(body["code"]).isEqualTo("NOT_FOUND")
            assertThat(body["message"] as String).isNotBlank()
        }

        @Test
        fun `GET worlds by id returns 400 for invalid UUID path parameter`() {
            val response = restTemplate.getForEntity("/api/worlds/not-a-uuid", String::class.java)

            assertThat(response.statusCode.value()).isEqualTo(400)
            val body = mapper.readValue<Map<String, Any>>(response.body!!)
            assertThat(body["code"]).isEqualTo("BAD_REQUEST")
            assertThat(body["message"] as String).isNotBlank()
        }
    }

    @Nested
    inner class GetAll {
        @Test
        fun `GET worlds returns all worlds`() {
            fixture.createWorld(name = "Alpha", greeting = "Hi!")
            fixture.createWorld(name = "Beta", greeting = "Hey!")

            val response = restTemplate.getForEntity("/api/worlds", String::class.java)

            assertThat(response.statusCode.value()).isEqualTo(200)
            val body = mapper.readValue<List<Map<String, Any>>>(response.body!!)
            assertThat(body).hasSize(2)
            val names = body.map { it["name"] as String }
            assertThat(names).containsExactlyInAnyOrder("Alpha", "Beta")
            val greetings = body.map { it["greeting"] as String }
            assertThat(greetings).containsExactlyInAnyOrder("Hi!", "Hey!")
        }

        @Test
        fun `GET worlds returns empty list when no worlds exist`() {
            val response = restTemplate.getForEntity("/api/worlds", String::class.java)

            assertThat(response.statusCode.value()).isEqualTo(200)
            val body = mapper.readValue<List<Map<String, Any>>>(response.body!!)
            assertThat(body).isEmpty()
        }

        @Test
        fun `GET worlds returns worlds sorted by name`() {
            fixture.createWorld(name = "Zeta", greeting = "Hi!")
            fixture.createWorld(name = "Alpha", greeting = "Hey!")
            fixture.createWorld(name = "Mu", greeting = "Hello!")

            val response = restTemplate.getForEntity("/api/worlds", String::class.java)

            assertThat(response.statusCode.value()).isEqualTo(200)
            val body = mapper.readValue<List<Map<String, Any>>>(response.body!!)
            assertThat(body).hasSize(3)
            val names = body.map { it["name"] as String }
            assertThat(names).isEqualTo(listOf("Alpha", "Mu", "Zeta"))
        }
    }

    @Nested
    inner class Create {
        @Test
        fun `POST worlds creates a new world and returns 201`() {
            val request = CreateWorldRequest(name = "New World", greeting = "Welcome!")

            val response = restTemplate.exchange(
                "/api/worlds", HttpMethod.POST, jsonEntity(mapper.writeValueAsString(request)), String::class.java
            )

            assertThat(response.statusCode.value()).isEqualTo(201)
            val body = mapper.readValue<Map<String, Any>>(response.body!!)
            assertThat(body["name"]).isEqualTo("New World")
            assertThat(body["greeting"]).isEqualTo("Welcome!")
            assertThat(body["id"]).isNotNull()
            assertThat(body["createdAt"]).isNotNull()
            assertThat(body["updatedAt"]).isNotNull()
        }

        @Test
        fun `POST worlds returns 409 when name already exists`() {
            fixture.createWorld(name = "Existing", greeting = "Hi!")
            val request = CreateWorldRequest(name = "Existing", greeting = "Hello!")

            val response = restTemplate.exchange(
                "/api/worlds", HttpMethod.POST, jsonEntity(mapper.writeValueAsString(request)), String::class.java
            )

            assertThat(response.statusCode.value()).isEqualTo(409)
            val body = mapper.readValue<Map<String, Any>>(response.body!!)
            assertThat(body["code"]).isEqualTo("CONFLICT")
            assertThat(body["message"] as String).isNotBlank()
        }

        @Test
        fun `POST worlds returns 400 when name is blank`() {
            val request = CreateWorldRequest(name = "", greeting = "Hello!")

            val response = restTemplate.exchange(
                "/api/worlds", HttpMethod.POST, jsonEntity(mapper.writeValueAsString(request)), String::class.java
            )

            assertThat(response.statusCode.value()).isEqualTo(400)
            val body = mapper.readValue<Map<String, Any>>(response.body!!)
            assertThat(body["code"]).isEqualTo("BAD_REQUEST")
            assertThat(body["message"] as String).isNotBlank()
        }

        @Test
        fun `POST worlds returns 400 when greeting is blank`() {
            val request = CreateWorldRequest(name = "World", greeting = "")

            val response = restTemplate.exchange(
                "/api/worlds", HttpMethod.POST, jsonEntity(mapper.writeValueAsString(request)), String::class.java
            )

            assertThat(response.statusCode.value()).isEqualTo(400)
            val body = mapper.readValue<Map<String, Any>>(response.body!!)
            assertThat(body["code"]).isEqualTo("BAD_REQUEST")
        }

        @Test
        fun `POST worlds returns 400 when greeting field is missing`() {
            val response = restTemplate.exchange(
                "/api/worlds", HttpMethod.POST, jsonEntity("""{"name": "World"}"""), String::class.java
            )

            assertThat(response.statusCode.value()).isEqualTo(400)
            val body = mapper.readValue<Map<String, Any>>(response.body!!)
            assertThat(body["code"]).isEqualTo("BAD_REQUEST")
            assertThat(body["message"] as String).isNotBlank()
        }

        @Test
        fun `POST worlds returns 400 when body is empty JSON object`() {
            val response = restTemplate.exchange(
                "/api/worlds", HttpMethod.POST, jsonEntity("""{}"""), String::class.java
            )

            assertThat(response.statusCode.value()).isEqualTo(400)
            val body = mapper.readValue<Map<String, Any>>(response.body!!)
            assertThat(body["code"]).isEqualTo("BAD_REQUEST")
        }
    }

    @Nested
    inner class Update {
        @Test
        fun `PUT worlds updates name and greeting`() {
            val id = fixture.createWorld(name = "Old Name", greeting = "Old Greeting")

            val before = restTemplate.getForEntity("/api/worlds/$id", String::class.java)
            val beforeBody = mapper.readValue<Map<String, Any>>(before.body!!)
            val originalUpdatedAt = beforeBody["updatedAt"] as String

            Thread.sleep(50) // ensure timestamp differs

            val request = UpdateWorldRequest(name = "New Name", greeting = "New Greeting")
            val response = restTemplate.exchange(
                "/api/worlds/$id", HttpMethod.PUT, jsonEntity(mapper.writeValueAsString(request)), String::class.java
            )

            assertThat(response.statusCode.value()).isEqualTo(200)
            val body = mapper.readValue<Map<String, Any>>(response.body!!)
            assertThat(body["name"]).isEqualTo("New Name")
            assertThat(body["greeting"]).isEqualTo("New Greeting")
            assertThat(body["id"]).isEqualTo(id.toString())
            assertThat(body["updatedAt"] as String).isNotEqualTo(originalUpdatedAt)
        }

        @Test
        fun `PUT worlds updates only name when greeting is null`() {
            val id = fixture.createWorld(name = "Original Name", greeting = "Original Greeting")
            val request = UpdateWorldRequest(name = "Updated Name")

            val response = restTemplate.exchange(
                "/api/worlds/$id", HttpMethod.PUT, jsonEntity(mapper.writeValueAsString(request)), String::class.java
            )

            assertThat(response.statusCode.value()).isEqualTo(200)
            val body = mapper.readValue<Map<String, Any>>(response.body!!)
            assertThat(body["name"]).isEqualTo("Updated Name")
            assertThat(body["greeting"]).isEqualTo("Original Greeting")
        }

        @Test
        fun `PUT worlds updates only greeting when name is null`() {
            val id = fixture.createWorld(name = "Original Name", greeting = "Original Greeting")
            val request = UpdateWorldRequest(greeting = "Updated Greeting")

            val response = restTemplate.exchange(
                "/api/worlds/$id", HttpMethod.PUT, jsonEntity(mapper.writeValueAsString(request)), String::class.java
            )

            assertThat(response.statusCode.value()).isEqualTo(200)
            val body = mapper.readValue<Map<String, Any>>(response.body!!)
            assertThat(body["name"]).isEqualTo("Original Name")
            assertThat(body["greeting"]).isEqualTo("Updated Greeting")
        }

        @Test
        fun `PUT worlds with both fields null returns 200 with unchanged data`() {
            val id = fixture.createWorld(name = "Stable Name", greeting = "Stable Greeting")
            val request = UpdateWorldRequest(name = null, greeting = null)

            val response = restTemplate.exchange(
                "/api/worlds/$id", HttpMethod.PUT, jsonEntity(mapper.writeValueAsString(request)), String::class.java
            )

            assertThat(response.statusCode.value()).isEqualTo(200)
            val body = mapper.readValue<Map<String, Any>>(response.body!!)
            assertThat(body["name"]).isEqualTo("Stable Name")
            assertThat(body["greeting"]).isEqualTo("Stable Greeting")
            assertThat(body["id"]).isEqualTo(id.toString())
        }

        @Test
        fun `PUT worlds returns 404 when world does not exist`() {
            val request = UpdateWorldRequest(name = "Name")

            val response = restTemplate.exchange(
                "/api/worlds/${UUID.randomUUID()}", HttpMethod.PUT, jsonEntity(mapper.writeValueAsString(request)), String::class.java
            )

            assertThat(response.statusCode.value()).isEqualTo(404)
            val body = mapper.readValue<Map<String, Any>>(response.body!!)
            assertThat(body["code"]).isEqualTo("NOT_FOUND")
        }

        @Test
        fun `PUT worlds returns 400 when name is blank`() {
            val id = fixture.createWorld(name = "World", greeting = "Hi!")
            val request = UpdateWorldRequest(name = "")

            val response = restTemplate.exchange(
                "/api/worlds/$id", HttpMethod.PUT, jsonEntity(mapper.writeValueAsString(request)), String::class.java
            )

            assertThat(response.statusCode.value()).isEqualTo(400)
            val body = mapper.readValue<Map<String, Any>>(response.body!!)
            assertThat(body["code"]).isEqualTo("BAD_REQUEST")
        }

        @Test
        fun `PUT worlds returns 400 when greeting is blank`() {
            val id = fixture.createWorld(name = "World", greeting = "Hi!")
            val request = UpdateWorldRequest(greeting = "")

            val response = restTemplate.exchange(
                "/api/worlds/$id", HttpMethod.PUT, jsonEntity(mapper.writeValueAsString(request)), String::class.java
            )

            assertThat(response.statusCode.value()).isEqualTo(400)
            val body = mapper.readValue<Map<String, Any>>(response.body!!)
            assertThat(body["code"]).isEqualTo("BAD_REQUEST")
        }

        @Test
        fun `PUT worlds returns 409 when updating name to existing name`() {
            fixture.createWorld(name = "Taken Name", greeting = "Hi!")
            val id = fixture.createWorld(name = "My World", greeting = "Hello!")
            val request = UpdateWorldRequest(name = "Taken Name")

            val response = restTemplate.exchange(
                "/api/worlds/$id", HttpMethod.PUT, jsonEntity(mapper.writeValueAsString(request)), String::class.java
            )

            assertThat(response.statusCode.value()).isEqualTo(409)
            val body = mapper.readValue<Map<String, Any>>(response.body!!)
            assertThat(body["code"]).isEqualTo("CONFLICT")
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `DELETE worlds deletes an existing world and returns 204`() {
            val id = fixture.createWorld(name = "To Delete", greeting = "Bye!")

            val response = restTemplate.exchange(
                "/api/worlds/$id", HttpMethod.DELETE, null, String::class.java
            )

            assertThat(response.statusCode.value()).isEqualTo(204)
        }

        @Test
        fun `DELETE worlds returns 404 when world does not exist`() {
            val response = restTemplate.exchange(
                "/api/worlds/${UUID.randomUUID()}", HttpMethod.DELETE, null, String::class.java
            )

            assertThat(response.statusCode.value()).isEqualTo(404)
            val body = mapper.readValue<Map<String, Any>>(response.body!!)
            assertThat(body["code"]).isEqualTo("NOT_FOUND")
            assertThat(body["message"] as String).isNotBlank()
        }

        @Test
        fun `DELETE worlds removes the world from the database`() {
            val id = fixture.createWorld(name = "Ephemeral", greeting = "Brief!")

            restTemplate.exchange("/api/worlds/$id", HttpMethod.DELETE, null, String::class.java)

            val getResponse = restTemplate.getForEntity("/api/worlds/$id", String::class.java)
            assertThat(getResponse.statusCode.value()).isEqualTo(404)
        }
    }

    @Nested
    inner class ReadYourOwnWrites {
        @Test
        fun `POST then GET returns the created world`() {
            val createRequest = mapper.writeValueAsString(mapOf("name" to "Earth", "greeting" to "Hello!"))
            val createResponse = restTemplate.exchange(
                "/api/worlds", HttpMethod.POST, jsonEntity(createRequest), String::class.java
            )
            assertThat(createResponse.statusCode.value()).isEqualTo(201)
            val created = mapper.readValue<Map<String, Any>>(createResponse.body!!)
            val id = created["id"] as String

            val getResponse = restTemplate.getForEntity("/api/worlds/$id", String::class.java)
            assertThat(getResponse.statusCode.value()).isEqualTo(200)
            val body = mapper.readValue<Map<String, Any>>(getResponse.body!!)
            assertThat(body["name"]).isEqualTo("Earth")
            assertThat(body["greeting"]).isEqualTo("Hello!")
            assertThat(body["id"]).isEqualTo(id)
        }

        @Test
        fun `POST then PUT then GET returns the updated world`() {
            val createRequest = mapper.writeValueAsString(mapOf("name" to "Earth", "greeting" to "Hello!"))
            val createResponse = restTemplate.exchange(
                "/api/worlds", HttpMethod.POST, jsonEntity(createRequest), String::class.java
            )
            assertThat(createResponse.statusCode.value()).isEqualTo(201)
            val created = mapper.readValue<Map<String, Any>>(createResponse.body!!)
            val id = created["id"] as String

            val updateRequest = mapper.writeValueAsString(mapOf("name" to "Terra", "greeting" to "Hola!"))
            val updateResponse = restTemplate.exchange(
                "/api/worlds/$id", HttpMethod.PUT, jsonEntity(updateRequest), String::class.java
            )
            assertThat(updateResponse.statusCode.value()).isEqualTo(200)

            val getResponse = restTemplate.getForEntity("/api/worlds/$id", String::class.java)
            assertThat(getResponse.statusCode.value()).isEqualTo(200)
            val body = mapper.readValue<Map<String, Any>>(getResponse.body!!)
            assertThat(body["name"]).isEqualTo("Terra")
            assertThat(body["greeting"]).isEqualTo("Hola!")
            assertThat(body["id"]).isEqualTo(id)
        }

        @Test
        fun `POST then DELETE then GET returns 404`() {
            val createRequest = mapper.writeValueAsString(mapOf("name" to "Earth", "greeting" to "Hello!"))
            val createResponse = restTemplate.exchange(
                "/api/worlds", HttpMethod.POST, jsonEntity(createRequest), String::class.java
            )
            assertThat(createResponse.statusCode.value()).isEqualTo(201)
            val created = mapper.readValue<Map<String, Any>>(createResponse.body!!)
            val id = created["id"] as String

            val deleteResponse = restTemplate.exchange(
                "/api/worlds/$id", HttpMethod.DELETE, null, String::class.java
            )
            assertThat(deleteResponse.statusCode.value()).isEqualTo(204)

            val getResponse = restTemplate.getForEntity("/api/worlds/$id", String::class.java)
            assertThat(getResponse.statusCode.value()).isEqualTo(404)
        }

        @Test
        fun `POST two worlds then GET all returns both`() {
            val req1 = mapper.writeValueAsString(mapOf("name" to "World A", "greeting" to "Hi A!"))
            val req2 = mapper.writeValueAsString(mapOf("name" to "World B", "greeting" to "Hi B!"))

            val resp1 = restTemplate.exchange("/api/worlds", HttpMethod.POST, jsonEntity(req1), String::class.java)
            assertThat(resp1.statusCode.value()).isEqualTo(201)

            val resp2 = restTemplate.exchange("/api/worlds", HttpMethod.POST, jsonEntity(req2), String::class.java)
            assertThat(resp2.statusCode.value()).isEqualTo(201)

            val getResponse = restTemplate.getForEntity("/api/worlds", String::class.java)
            assertThat(getResponse.statusCode.value()).isEqualTo(200)
            val body = mapper.readValue<List<Map<String, Any>>>(getResponse.body!!)
            assertThat(body).hasSize(2)
            val names = body.map { it["name"] as String }
            assertThat(names).containsExactlyInAnyOrder("World A", "World B")
        }
    }

    private fun jsonEntity(body: String): HttpEntity<String> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        return HttpEntity(body, headers)
    }
}
