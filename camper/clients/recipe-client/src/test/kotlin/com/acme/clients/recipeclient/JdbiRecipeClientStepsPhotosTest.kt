package com.acme.clients.recipeclient

import com.acme.clients.common.Result
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.error.ValidationError
import com.acme.clients.recipeclient.api.AddRecipePhotoParam
import com.acme.clients.recipeclient.api.CreateRecipeParam
import com.acme.clients.recipeclient.api.DeleteRecipeParam
import com.acme.clients.recipeclient.api.GetRecipePhotoByIdParam
import com.acme.clients.recipeclient.api.GetRecipePhotosParam
import com.acme.clients.recipeclient.api.GetRecipeStepsParam
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.clients.recipeclient.api.RemoveRecipePhotoParam
import com.acme.clients.recipeclient.api.ReplaceRecipeStepsParam
import com.acme.clients.recipeclient.model.RecipePhoto
import com.acme.clients.recipeclient.test.RecipeTestDb
import org.assertj.core.api.Assertions.assertThat
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID

/**
 * Integration tests for the `recipe_steps` and `recipe_photos` halves of [RecipeClient] against
 * real PostgreSQL: the delete-then-insert replace inside one transaction, `ORDER BY position`,
 * the computed next position, the unique storage key, and the cascades from `recipes`.
 */
@Testcontainers
class JdbiRecipeClientStepsPhotosTest {

    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("camper_db")
            .withUsername("postgres")
            .withPassword("postgres")

        private lateinit var client: RecipeClient
        private lateinit var jdbi: Jdbi

        @BeforeAll
        @JvmStatic
        fun setup() {
            RecipeTestDb.cleanAndMigrate(postgres.jdbcUrl, postgres.username, postgres.password)
            System.setProperty("DB_URL", postgres.jdbcUrl)
            System.setProperty("DB_USER", postgres.username)
            System.setProperty("DB_PASSWORD", postgres.password)
            client = createRecipeClient()
            jdbi = Jdbi.create(postgres.jdbcUrl, postgres.username, postgres.password)
        }
    }

    private lateinit var alice: UUID
    private lateinit var soup: UUID

    @BeforeEach
    fun truncateAndSeed() {
        jdbi.useHandle<Exception> { handle ->
            handle.createUpdate("TRUNCATE TABLE recipe_photos, recipe_steps, recipe_favorites, recipe_ingredients, recipes, ingredients, users CASCADE").execute()
        }
        alice = insertUser("alice")
        soup = createRecipe("Soup", alice)
    }

    @Nested
    inner class Steps {

        @Test
        fun `a recipe starts with no steps`() {
            assertThat((client.getSteps(GetRecipeStepsParam(soup)) as Result.Success).value).isEmpty()
        }

        @Test
        fun `replaceSteps stores the texts in order, trimmed, and getSteps reads them back by position`() {
            val result = client.replaceSteps(ReplaceRecipeStepsParam(soup, listOf("  Chop.  ", "Simmer.", "Serve.")))

            val steps = (result as Result.Success).value
            assertThat(steps.map { it.position }).containsExactly(0, 1, 2)
            assertThat(steps.map { it.text }).containsExactly("Chop.", "Simmer.", "Serve.")
            assertThat(steps).allSatisfy { assertThat(it.recipeId).isEqualTo(soup) }
            assertThat((client.getSteps(GetRecipeStepsParam(soup)) as Result.Success).value.map { it.text })
                .containsExactly("Chop.", "Simmer.", "Serve.")
        }

        @Test
        fun `replaceSteps replaces the whole list - old rows are gone, positions restart at 0`() {
            client.replaceSteps(ReplaceRecipeStepsParam(soup, listOf("One", "Two", "Three")))

            client.replaceSteps(ReplaceRecipeStepsParam(soup, listOf("Only")))

            val steps = (client.getSteps(GetRecipeStepsParam(soup)) as Result.Success).value
            assertThat(steps.map { it.position to it.text }).containsExactly(0 to "Only")
            assertThat(stepRowCount(soup)).isEqualTo(1)
        }

        @Test
        fun `replaceSteps with an empty list clears them`() {
            client.replaceSteps(ReplaceRecipeStepsParam(soup, listOf("One")))

            val result = client.replaceSteps(ReplaceRecipeStepsParam(soup, emptyList()))

            assertThat((result as Result.Success).value).isEmpty()
            assertThat(stepRowCount(soup)).isEqualTo(0)
        }

        @Test
        fun `replaceSteps rejects a blank step and leaves the existing list untouched`() {
            client.replaceSteps(ReplaceRecipeStepsParam(soup, listOf("Keep me")))

            val result = client.replaceSteps(ReplaceRecipeStepsParam(soup, listOf("One", "   ")))

            assertThat((result as Result.Failure).error).isInstanceOf(ValidationError::class.java)
            assertThat((client.getSteps(GetRecipeStepsParam(soup)) as Result.Success).value.map { it.text }).containsExactly("Keep me")
        }

        @Test
        fun `deleting the recipe cascades to its steps`() {
            client.replaceSteps(ReplaceRecipeStepsParam(soup, listOf("One", "Two")))

            client.delete(DeleteRecipeParam(soup))

            assertThat(stepRowCount(soup)).isEqualTo(0)
        }
    }

    @Nested
    inner class Photos {

        private fun photoParam(id: UUID = UUID.randomUUID(), key: String = "recipes/$soup/$id.jpg", role: String? = null, source: String = RecipePhoto.SOURCE_UPLOAD) =
            AddRecipePhotoParam(
                id = id, recipeId = soup, storageKey = key, mediaType = "image/jpeg", byteSize = 1234,
                width = 1176, height = 1568, source = source, role = role, createdBy = alice
            )

        @Test
        fun `addPhoto stores the metadata and takes position 0 then 1 then 2`() {
            val first = (client.addPhoto(photoParam()) as Result.Success).value
            val second = (client.addPhoto(photoParam(role = RecipePhoto.ROLE_INGREDIENTS, source = RecipePhoto.SOURCE_IMPORT)) as Result.Success).value
            val third = (client.addPhoto(photoParam()) as Result.Success).value

            assertThat(listOf(first, second, third).map { it.position }).containsExactly(0, 1, 2)
            assertThat(second.role).isEqualTo("ingredients")
            assertThat(second.source).isEqualTo("import")
            assertThat(first.width).isEqualTo(1176)
            assertThat(first.createdBy).isEqualTo(alice)
            assertThat((client.getPhotos(GetRecipePhotosParam(soup)) as Result.Success).value.map { it.id })
                .containsExactly(first.id, second.id, third.id)
        }

        @Test
        fun `a removed photo's position is not reused, so order never shifts`() {
            val a = (client.addPhoto(photoParam()) as Result.Success).value
            val b = (client.addPhoto(photoParam()) as Result.Success).value
            client.removePhoto(RemoveRecipePhotoParam(b.id))

            val c = (client.addPhoto(photoParam()) as Result.Success).value

            assertThat(c.position).isEqualTo(1)
            assertThat((client.getPhotos(GetRecipePhotosParam(soup)) as Result.Success).value.map { it.id }).containsExactly(a.id, c.id)
        }

        @Test
        fun `getPhotoById and removePhoto are NotFound for an unknown id`() {
            val id = UUID.randomUUID()
            assertThat((client.getPhotoById(GetRecipePhotoByIdParam(id)) as Result.Failure).error).isInstanceOf(NotFoundError::class.java)
            assertThat((client.removePhoto(RemoveRecipePhotoParam(id)) as Result.Failure).error).isInstanceOf(NotFoundError::class.java)
        }

        @Test
        fun `null width and height round-trip as null`() {
            val photo = (client.addPhoto(photoParam().copy(width = null, height = null)) as Result.Success).value
            val read = (client.getPhotoById(GetRecipePhotoByIdParam(photo.id)) as Result.Success).value
            assertThat(read.width).isNull()
            assertThat(read.height).isNull()
        }

        @Test
        fun `addPhoto rejects a bad source or role before touching the database`() {
            val bad = client.addPhoto(photoParam(source = "email"))
            val badRole = client.addPhoto(photoParam(role = "cover"))
            assertThat((bad as Result.Failure).error).isInstanceOf(ValidationError::class.java)
            assertThat((badRole as Result.Failure).error).isInstanceOf(ValidationError::class.java)
            assertThat(photoRowCount(soup)).isEqualTo(0)
        }

        @Test
        fun `deleting the recipe cascades to its photo rows`() {
            client.addPhoto(photoParam())

            client.delete(DeleteRecipeParam(soup))

            assertThat(photoRowCount(soup)).isEqualTo(0)
        }
    }

    private fun stepRowCount(recipeId: UUID): Int = jdbi.withHandle<Int, Exception> { handle ->
        handle.createQuery("SELECT count(*) FROM recipe_steps WHERE recipe_id = :id").bind("id", recipeId).mapTo(Int::class.java).one()
    }

    private fun photoRowCount(recipeId: UUID): Int = jdbi.withHandle<Int, Exception> { handle ->
        handle.createQuery("SELECT count(*) FROM recipe_photos WHERE recipe_id = :id").bind("id", recipeId).mapTo(Int::class.java).one()
    }

    private fun insertUser(name: String): UUID {
        val id = UUID.randomUUID()
        jdbi.useHandle<Exception> { handle ->
            handle.createUpdate("INSERT INTO users (id, email, username) VALUES (:id, :email, :username)")
                .bind("id", id).bind("email", "$name-${id.toString().take(8)}@example.com").bind("username", name).execute()
        }
        return id
    }

    private fun createRecipe(name: String, createdBy: UUID): UUID {
        val result = client.create(CreateRecipeParam(name = name, description = null, webLink = null, baseServings = 4, status = "published", createdBy = createdBy))
        return (result as Result.Success).value.id
    }
}
