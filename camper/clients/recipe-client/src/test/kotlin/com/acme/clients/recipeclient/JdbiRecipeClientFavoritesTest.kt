package com.acme.clients.recipeclient

import com.acme.clients.common.Result
import com.acme.clients.recipeclient.api.AddRecipeFavoriteParam
import com.acme.clients.recipeclient.api.CreateRecipeParam
import com.acme.clients.recipeclient.api.DeleteRecipeParam
import com.acme.clients.recipeclient.api.GetRecipeFavoriteSummariesParam
import com.acme.clients.recipeclient.api.GetRecipeFavoritesParam
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.clients.recipeclient.api.RemoveRecipeFavoriteParam
import com.acme.clients.recipeclient.model.RecipeFavorite
import com.acme.clients.recipeclient.test.RecipeTestDb
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Integration tests for the `recipe_favorites` half of [RecipeClient], against a real
 * PostgreSQL 16 with the real migrations applied.
 *
 * Everything here is deliberately a *database* behaviour that [com.acme.clients.recipeclient.fake.FakeRecipeClient]
 * cannot prove: `ON CONFLICT DO NOTHING` idempotency, the `uq_recipe_favorites_recipe_user`
 * constraint, `ORDER BY created_at`, the batched `GROUP BY` aggregate (including its
 * empty-`IN ()` guard and its refusal to zero-fill) and the two `ON DELETE CASCADE` FKs.
 *
 * **Ordering note.** `GetRecipeFavorites` orders by `created_at, id`. PostgreSQL compares
 * `uuid` as unsigned bytes while `java.util.UUID.compareTo` compares two *signed* longs, so
 * the two disagree for ids whose high bit differs. Every ordering assertion below therefore
 * uses distinct, explicitly written `created_at` values — except the one test that pins two
 * ids on purpose to nail down the tiebreaker, which asserts the database's unsigned order.
 */
@Testcontainers
class JdbiRecipeClientFavoritesTest {

    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("camper_db")
            .withUsername("postgres")
            .withPassword("postgres")

        private lateinit var client: RecipeClient
        private lateinit var jdbi: Jdbi

        /**
         * Two ids chosen so that Kotlin and PostgreSQL disagree about their order:
         * `ID_HIGH`'s most-significant 64 bits are negative as a signed long, so
         * `ID_HIGH < ID_LOW` in Kotlin, while PostgreSQL's unsigned byte comparison
         * puts `ID_LOW` first. The tiebreaker test asserts the database's answer.
         */
        private val ID_LOW: UUID = UUID.fromString("00000000-0000-4000-8000-000000000001")
        private val ID_HIGH: UUID = UUID.fromString("ffffffff-ffff-4fff-8fff-ffffffffffff")

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
    private lateinit var bob: UUID
    private lateinit var carol: UUID
    private lateinit var soup: UUID
    private lateinit var stew: UUID

    @BeforeEach
    fun truncateAndSeed() {
        jdbi.useHandle<Exception> { handle ->
            handle.createUpdate(
                "TRUNCATE TABLE recipe_favorites, recipe_ingredients, recipes, ingredients, users CASCADE"
            ).execute()
        }
        alice = insertUser("alice")
        bob = insertUser("bob")
        carol = insertUser("carol")
        soup = createRecipe("Soup", alice)
        stew = createRecipe("Stew", alice)
    }

    // ---------------------------------------------------------------- addFavorite

    @Nested
    inner class AddFavorite {

        @Test
        fun `addFavorite inserts exactly one row for the recipe and user`() {
            val result = client.addFavorite(AddRecipeFavoriteParam(recipeId = soup, userId = bob))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            assertThat(favoriteRowCount(soup)).isEqualTo(1)
            val rows = favorites(soup)
            assertThat(rows).singleElement().satisfies({ row ->
                assertThat(row.recipeId).isEqualTo(soup)
                assertThat(row.userId).isEqualTo(bob)
                assertThat(row.id).isNotNull()
                assertThat(row.createdAt).isNotNull()
            })
        }

        @Test
        fun `addFavorite twice succeeds and still leaves exactly one row`() {
            val first = client.addFavorite(AddRecipeFavoriteParam(recipeId = soup, userId = bob))
            val second = client.addFavorite(AddRecipeFavoriteParam(recipeId = soup, userId = bob))

            assertThat(first).isInstanceOf(Result.Success::class.java)
            assertThat(second).isInstanceOf(Result.Success::class.java)
            assertThat(favoriteRowCount(soup)).isEqualTo(1)
        }

        @Test
        fun `addFavorite a second time leaves the original id and created_at untouched`() {
            // Back-date the first favourite so a silently re-inserted row would be obvious:
            // a fresh insert would carry "now", not this timestamp.
            client.addFavorite(AddRecipeFavoriteParam(recipeId = soup, userId = bob))
            val backdated = Instant.now().minus(7, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS)
            setCreatedAt(soup, bob, backdated)
            val originalId = favorites(soup).single().id

            client.addFavorite(AddRecipeFavoriteParam(recipeId = soup, userId = bob))

            val row = favorites(soup).single()
            assertThat(row.id).isEqualTo(originalId)
            assertThat(row.createdAt).isEqualTo(backdated)
        }

        @Test
        fun `addFavorite by a second user adds a second row for the same recipe`() {
            client.addFavorite(AddRecipeFavoriteParam(recipeId = soup, userId = bob))
            client.addFavorite(AddRecipeFavoriteParam(recipeId = soup, userId = carol))

            assertThat(favoriteRowCount(soup)).isEqualTo(2)
            assertThat(favorites(soup).map { it.userId }).containsExactlyInAnyOrder(bob, carol)
        }

        @Test
        fun `the unique constraint rejects a duplicate recipe and user pair inserted directly`() {
            client.addFavorite(AddRecipeFavoriteParam(recipeId = soup, userId = bob))

            // Bypass ON CONFLICT DO NOTHING to prove the constraint itself exists and bites —
            // it is the only thing making addFavorite's idempotency safe under concurrency.
            assertThatThrownBy {
                jdbi.useHandle<Exception> { handle ->
                    handle.createUpdate(
                        "INSERT INTO recipe_favorites (recipe_id, user_id) VALUES (:recipeId, :userId)"
                    )
                        .bind("recipeId", soup)
                        .bind("userId", bob)
                        .execute()
                }
            }.hasMessageContaining("uq_recipe_favorites_recipe_user")

            assertThat(favoriteRowCount(soup)).isEqualTo(1)
        }
    }

    // ------------------------------------------------------------- removeFavorite

    @Nested
    inner class RemoveFavorite {

        @Test
        fun `removeFavorite deletes only the calling users row`() {
            client.addFavorite(AddRecipeFavoriteParam(recipeId = soup, userId = bob))
            client.addFavorite(AddRecipeFavoriteParam(recipeId = soup, userId = carol))

            val result = client.removeFavorite(RemoveRecipeFavoriteParam(recipeId = soup, userId = bob))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            assertThat(favorites(soup).map { it.userId }).containsExactly(carol)
        }

        @Test
        fun `removeFavorite succeeds when the user never favourited the recipe`() {
            // 0 rows deleted is success here, not NotFoundError — the deliberate departure
            // from DeleteRecipe's shape that makes un-favouriting idempotent.
            val result = client.removeFavorite(RemoveRecipeFavoriteParam(recipeId = soup, userId = bob))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            assertThat(favoriteRowCount(soup)).isEqualTo(0)
        }

        @Test
        fun `removeFavorite called twice succeeds and the row stays deleted`() {
            client.addFavorite(AddRecipeFavoriteParam(recipeId = soup, userId = bob))

            val first = client.removeFavorite(RemoveRecipeFavoriteParam(recipeId = soup, userId = bob))
            val second = client.removeFavorite(RemoveRecipeFavoriteParam(recipeId = soup, userId = bob))

            assertThat(first).isInstanceOf(Result.Success::class.java)
            assertThat(second).isInstanceOf(Result.Success::class.java)
            assertThat(favoriteRowCount(soup)).isEqualTo(0)
        }

        @Test
        fun `removeFavorite leaves the same users favourite of another recipe alone`() {
            client.addFavorite(AddRecipeFavoriteParam(recipeId = soup, userId = bob))
            client.addFavorite(AddRecipeFavoriteParam(recipeId = stew, userId = bob))

            client.removeFavorite(RemoveRecipeFavoriteParam(recipeId = soup, userId = bob))

            assertThat(favoriteRowCount(soup)).isEqualTo(0)
            assertThat(favorites(stew).map { it.userId }).containsExactly(bob)
        }
    }

    // --------------------------------------------------------------- getFavorites

    @Nested
    inner class GetFavorites {

        @Test
        fun `getFavorites returns favourites oldest first by created_at`() {
            // Insert in one order, then write created_at in a different order, so the result
            // can only be right if ORDER BY created_at — not insertion order — drives it.
            client.addFavorite(AddRecipeFavoriteParam(recipeId = soup, userId = alice))
            client.addFavorite(AddRecipeFavoriteParam(recipeId = soup, userId = bob))
            client.addFavorite(AddRecipeFavoriteParam(recipeId = soup, userId = carol))

            val base = Instant.parse("2026-01-01T00:00:00Z")
            setCreatedAt(soup, alice, base.plus(3, ChronoUnit.DAYS))
            setCreatedAt(soup, bob, base.plus(1, ChronoUnit.DAYS))
            setCreatedAt(soup, carol, base.plus(2, ChronoUnit.DAYS))

            val result = client.getFavorites(GetRecipeFavoritesParam(soup))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val rows = (result as Result.Success).value
            assertThat(rows.map { it.userId }).containsExactly(bob, carol, alice)
            assertThat(rows.map { it.createdAt }).containsExactly(
                base.plus(1, ChronoUnit.DAYS),
                base.plus(2, ChronoUnit.DAYS),
                base.plus(3, ChronoUnit.DAYS)
            )
        }

        @Test
        fun `getFavorites breaks a created_at tie on id using the databases unsigned uuid order`() {
            val sharedCreatedAt = Instant.parse("2026-01-01T00:00:00Z")
            insertFavorite(id = ID_HIGH, recipeId = soup, userId = bob, createdAt = sharedCreatedAt)
            insertFavorite(id = ID_LOW, recipeId = soup, userId = carol, createdAt = sharedCreatedAt)

            // Kotlin would sort these the other way round: UUID.compareTo treats the leading
            // 64 bits as a signed long, so ID_HIGH (0xffff…) looks negative and sorts first.
            assertThat(ID_HIGH.compareTo(ID_LOW)).isLessThan(0)

            val rows = (client.getFavorites(GetRecipeFavoritesParam(soup)) as Result.Success).value

            assertThat(rows.map { it.id }).containsExactly(ID_LOW, ID_HIGH)
        }

        @Test
        fun `getFavorites returns an empty list for a recipe nobody favourited`() {
            client.addFavorite(AddRecipeFavoriteParam(recipeId = stew, userId = bob))

            val result = client.getFavorites(GetRecipeFavoritesParam(soup))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            assertThat((result as Result.Success).value).isEmpty()
        }

        @Test
        fun `getFavorites returns only the requested recipes favourites`() {
            client.addFavorite(AddRecipeFavoriteParam(recipeId = soup, userId = bob))
            client.addFavorite(AddRecipeFavoriteParam(recipeId = stew, userId = carol))

            val rows = (client.getFavorites(GetRecipeFavoritesParam(soup)) as Result.Success).value

            assertThat(rows.map { it.userId }).containsExactly(bob)
        }
    }

    // ------------------------------------------------------- getFavoriteSummaries

    @Nested
    inner class GetFavoriteSummaries {

        @Test
        fun `getFavoriteSummaries counts favourites per recipe across several users`() {
            client.addFavorite(AddRecipeFavoriteParam(recipeId = soup, userId = alice))
            client.addFavorite(AddRecipeFavoriteParam(recipeId = soup, userId = bob))
            client.addFavorite(AddRecipeFavoriteParam(recipeId = soup, userId = carol))
            client.addFavorite(AddRecipeFavoriteParam(recipeId = stew, userId = bob))

            val result = client.getFavoriteSummaries(
                GetRecipeFavoriteSummariesParam(recipeIds = listOf(soup, stew), userId = alice)
            )

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val byRecipe = (result as Result.Success).value.associateBy { it.recipeId }
            assertThat(byRecipe.keys).containsExactlyInAnyOrder(soup, stew)
            assertThat(byRecipe.getValue(soup).favoriteCount).isEqualTo(3)
            assertThat(byRecipe.getValue(stew).favoriteCount).isEqualTo(1)
        }

        @Test
        fun `getFavoriteSummaries sets favoritedByMe only for the asking user`() {
            client.addFavorite(AddRecipeFavoriteParam(recipeId = soup, userId = alice))
            client.addFavorite(AddRecipeFavoriteParam(recipeId = soup, userId = bob))
            client.addFavorite(AddRecipeFavoriteParam(recipeId = stew, userId = bob))

            val asAlice = summariesFor(listOf(soup, stew), alice)
            val asBob = summariesFor(listOf(soup, stew), bob)
            val asCarol = summariesFor(listOf(soup, stew), carol)

            assertThat(asAlice.getValue(soup).favoritedByMe).isTrue()
            assertThat(asAlice.getValue(stew).favoritedByMe).isFalse()

            assertThat(asBob.getValue(soup).favoritedByMe).isTrue()
            assertThat(asBob.getValue(stew).favoritedByMe).isTrue()

            assertThat(asCarol.getValue(soup).favoritedByMe).isFalse()
            assertThat(asCarol.getValue(stew).favoritedByMe).isFalse()

            // Counts are caller-independent; only favoritedByMe moves.
            assertThat(asAlice.getValue(soup).favoriteCount).isEqualTo(2)
            assertThat(asBob.getValue(soup).favoriteCount).isEqualTo(2)
            assertThat(asCarol.getValue(soup).favoriteCount).isEqualTo(2)
        }

        @Test
        fun `getFavoriteSummaries omits recipes nobody favourited instead of zero filling`() {
            client.addFavorite(AddRecipeFavoriteParam(recipeId = soup, userId = bob))

            val summaries = (
                client.getFavoriteSummaries(
                    GetRecipeFavoriteSummariesParam(recipeIds = listOf(soup, stew), userId = bob)
                ) as Result.Success
                ).value

            // The service defaults a missing id to 0/false; if this ever started zero-filling,
            // that default would be dead code and a real zero would be indistinguishable.
            assertThat(summaries.map { it.recipeId }).containsExactly(soup)
        }

        @Test
        fun `getFavoriteSummaries returns an empty list for empty recipeIds without erroring`() {
            client.addFavorite(AddRecipeFavoriteParam(recipeId = soup, userId = bob))

            // An empty IN () is a PostgreSQL syntax error, so the guard must short-circuit
            // before the query runs. Without it this call throws.
            val result = client.getFavoriteSummaries(
                GetRecipeFavoriteSummariesParam(recipeIds = emptyList(), userId = bob)
            )

            assertThat(result).isInstanceOf(Result.Success::class.java)
            assertThat((result as Result.Success).value).isEmpty()
        }

        @Test
        fun `getFavoriteSummaries returns one summary per recipe when recipeIds repeats an id`() {
            client.addFavorite(AddRecipeFavoriteParam(recipeId = soup, userId = alice))
            client.addFavorite(AddRecipeFavoriteParam(recipeId = soup, userId = bob))

            val summaries = (
                client.getFavoriteSummaries(
                    GetRecipeFavoriteSummariesParam(recipeIds = listOf(soup, soup, soup), userId = alice)
                ) as Result.Success
                ).value

            assertThat(summaries).hasSize(1)
            assertThat(summaries.single().recipeId).isEqualTo(soup)
            assertThat(summaries.single().favoriteCount).isEqualTo(2)
            assertThat(summaries.single().favoritedByMe).isTrue()
        }

        @Test
        fun `getFavoriteSummaries ignores favourites of recipes outside recipeIds`() {
            client.addFavorite(AddRecipeFavoriteParam(recipeId = soup, userId = bob))
            client.addFavorite(AddRecipeFavoriteParam(recipeId = stew, userId = bob))

            val summaries = (
                client.getFavoriteSummaries(
                    GetRecipeFavoriteSummariesParam(recipeIds = listOf(soup), userId = bob)
                ) as Result.Success
                ).value

            assertThat(summaries.map { it.recipeId }).containsExactly(soup)
        }

        @Test
        fun `getFavoriteSummaries returns an empty list when none of the recipes are favourited`() {
            val result = client.getFavoriteSummaries(
                GetRecipeFavoriteSummariesParam(recipeIds = listOf(soup, stew), userId = bob)
            )

            assertThat(result).isInstanceOf(Result.Success::class.java)
            assertThat((result as Result.Success).value).isEmpty()
        }
    }

    // ------------------------------------------------------------- FK cascades

    @Nested
    inner class ForeignKeyCascades {

        @Test
        fun `deleting a recipe cascades its favourites away and leaves other recipes favourites`() {
            client.addFavorite(AddRecipeFavoriteParam(recipeId = soup, userId = bob))
            client.addFavorite(AddRecipeFavoriteParam(recipeId = soup, userId = carol))
            client.addFavorite(AddRecipeFavoriteParam(recipeId = stew, userId = bob))

            val deleted = client.delete(DeleteRecipeParam(soup))

            assertThat(deleted).isInstanceOf(Result.Success::class.java)
            assertThat(favoriteRowCount(soup)).isEqualTo(0)
            assertThat(favoriteRowCount(stew)).isEqualTo(1)
        }

        @Test
        fun `deleting a user cascades their favourites away and leaves other users favourites`() {
            client.addFavorite(AddRecipeFavoriteParam(recipeId = soup, userId = bob))
            client.addFavorite(AddRecipeFavoriteParam(recipeId = soup, userId = carol))
            client.addFavorite(AddRecipeFavoriteParam(recipeId = stew, userId = bob))

            // bob authored no recipes, so recipes.created_by ON DELETE RESTRICT does not block this.
            jdbi.useHandle<Exception> { handle ->
                handle.createUpdate("DELETE FROM users WHERE id = :id").bind("id", bob).execute()
            }

            assertThat(favorites(soup).map { it.userId }).containsExactly(carol)
            assertThat(favoriteRowCount(stew)).isEqualTo(0)
        }
    }

    // ------------------------------------------------------------------- helpers

    private fun insertUser(name: String): UUID {
        val id = UUID.randomUUID()
        jdbi.useHandle<Exception> { handle ->
            handle.createUpdate("INSERT INTO users (id, email, username) VALUES (:id, :email, :username)")
                .bind("id", id)
                .bind("email", "$name-${id.toString().take(8)}@example.com")
                .bind("username", name)
                .execute()
        }
        return id
    }

    private fun createRecipe(name: String, createdBy: UUID): UUID {
        val result = client.create(
            CreateRecipeParam(
                name = name,
                description = null,
                webLink = null,
                baseServings = 4,
                status = "published",
                createdBy = createdBy
            )
        )
        return (result as Result.Success).value.id
    }

    private fun insertFavorite(id: UUID, recipeId: UUID, userId: UUID, createdAt: Instant) {
        jdbi.useHandle<Exception> { handle ->
            handle.createUpdate(
                """
                INSERT INTO recipe_favorites (id, recipe_id, user_id, created_at)
                VALUES (:id, :recipeId, :userId, :createdAt)
                """.trimIndent()
            )
                .bind("id", id)
                .bind("recipeId", recipeId)
                .bind("userId", userId)
                .bind("createdAt", createdAt)
                .execute()
        }
    }

    private fun setCreatedAt(recipeId: UUID, userId: UUID, createdAt: Instant) {
        val updated = jdbi.withHandle<Int, Exception> { handle ->
            handle.createUpdate(
                "UPDATE recipe_favorites SET created_at = :createdAt WHERE recipe_id = :recipeId AND user_id = :userId"
            )
                .bind("createdAt", createdAt)
                .bind("recipeId", recipeId)
                .bind("userId", userId)
                .execute()
        }
        check(updated == 1) { "expected exactly one favourite row to back-date, updated=$updated" }
    }

    private fun favoriteRowCount(recipeId: UUID): Int =
        jdbi.withHandle<Int, Exception> { handle ->
            handle.createQuery("SELECT count(*)::int FROM recipe_favorites WHERE recipe_id = :recipeId")
                .bind("recipeId", recipeId)
                .mapTo(Int::class.java)
                .one()
        }

    /** Reads rows straight from the table, independently of the operation under test. */
    private fun favorites(recipeId: UUID): List<RecipeFavorite> =
        jdbi.withHandle<List<RecipeFavorite>, Exception> { handle ->
            handle.createQuery(
                "SELECT id, recipe_id, user_id, created_at FROM recipe_favorites WHERE recipe_id = :recipeId"
            )
                .bind("recipeId", recipeId)
                .map { rs, _ ->
                    RecipeFavorite(
                        id = rs.getObject("id", UUID::class.java),
                        recipeId = rs.getObject("recipe_id", UUID::class.java),
                        userId = rs.getObject("user_id", UUID::class.java),
                        createdAt = rs.getTimestamp("created_at").toInstant()
                    )
                }
                .list()
        }

    private fun summariesFor(recipeIds: List<UUID>, userId: UUID) =
        (
            client.getFavoriteSummaries(
                GetRecipeFavoriteSummariesParam(recipeIds = recipeIds, userId = userId)
            ) as Result.Success
            ).value.associateBy { it.recipeId }
}
