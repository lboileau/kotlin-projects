package com.acme.services.camperservice.features.recipe.acceptance

import com.acme.services.camperservice.config.TestContainerConfig
import com.acme.services.camperservice.features.recipe.acceptance.fixture.RecipeFixture
import com.acme.services.camperservice.features.recipe.dto.CreateIngredientRequest
import com.acme.services.camperservice.features.recipe.dto.IngredientResponse
import com.acme.services.camperservice.features.recipe.dto.UpdateIngredientRequest
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
class IngredientAcceptanceTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    private lateinit var fixture: RecipeFixture
    private lateinit var userId: UUID

    @BeforeEach
    fun setUp() {
        fixture = RecipeFixture(jdbcTemplate)
        fixture.truncateAll()
        userId = fixture.insertUser(email = "user@example.com", username = "user")
    }

    @Nested
    inner class ListIngredients {

        @Test
        fun `GET returns 200 with empty list when no ingredients`() {
            val response = restTemplate.exchange(
                "/api/ingredients",
                HttpMethod.GET,
                entityWithUser(null, userId),
                Array<IngredientResponse>::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body).isEmpty()
        }

        @Test
        fun `GET returns 200 with all ingredients`() {
            fixture.insertIngredient(name = "Tomato", category = "produce", defaultUnit = "pieces")
            fixture.insertIngredient(name = "Salt", category = "spice", defaultUnit = "pinch")

            val response = restTemplate.exchange(
                "/api/ingredients",
                HttpMethod.GET,
                entityWithUser(null, userId),
                Array<IngredientResponse>::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body).hasSize(2)
            assertThat(response.body!!.map { it.name }).containsExactlyInAnyOrder("Tomato", "Salt")
        }
    }

    @Nested
    inner class CreateIngredient {

        @Test
        fun `POST returns 201 with created ingredient`() {
            val response = restTemplate.exchange(
                "/api/ingredients",
                HttpMethod.POST,
                entityWithUser(CreateIngredientRequest(name = "Garlic", category = "produce", defaultUnit = "clove"), userId),
                IngredientResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            assertThat(response.body!!.name).isEqualTo("Garlic")
            assertThat(response.body!!.category).isEqualTo("produce")
            assertThat(response.body!!.defaultUnit).isEqualTo("clove")
            assertThat(response.body!!.id).isNotNull()
        }

        @Test
        fun `POST returns 400 when name is blank`() {
            val response = restTemplate.exchange(
                "/api/ingredients",
                HttpMethod.POST,
                entityWithUser(CreateIngredientRequest(name = "", category = "produce", defaultUnit = "pieces"), userId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        fun `POST returns 409 when ingredient name already exists`() {
            fixture.insertIngredient(name = "Basil", category = "spice", defaultUnit = "sprig")

            val response = restTemplate.exchange(
                "/api/ingredients",
                HttpMethod.POST,
                entityWithUser(CreateIngredientRequest(name = "Basil", category = "produce", defaultUnit = "bunch"), userId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        }
    }

    @Nested
    inner class UpdateIngredient {

        @Test
        fun `PUT returns 200 with updated ingredient`() {
            val ingredientId = fixture.insertIngredient(name = "Old Name", category = "produce", defaultUnit = "pieces")

            val response = restTemplate.exchange(
                "/api/ingredients/$ingredientId",
                HttpMethod.PUT,
                entityWithUser(UpdateIngredientRequest(name = "New Name", category = null, defaultUnit = null), userId),
                IngredientResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.name).isEqualTo("New Name")
            assertThat(response.body!!.category).isEqualTo("produce")
        }

        @Test
        fun `PUT returns 404 when ingredient not found`() {
            val response = restTemplate.exchange(
                "/api/ingredients/${UUID.randomUUID()}",
                HttpMethod.PUT,
                entityWithUser(UpdateIngredientRequest(name = "Whatever", category = null, defaultUnit = null), userId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    private fun entityWithUser(body: Any?, userId: UUID): HttpEntity<Any?> {
        val headers = HttpHeaders()
        headers.set("X-User-Id", userId.toString())
        return HttpEntity(body, headers)
    }
}
