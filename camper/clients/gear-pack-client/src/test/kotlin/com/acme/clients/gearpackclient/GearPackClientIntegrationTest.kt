package com.acme.clients.gearpackclient

import com.acme.clients.common.Result
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.gearpackclient.api.GetAllGearPacksParam
import com.acme.clients.gearpackclient.api.GetGearPackByIdParam
import com.acme.clients.gearpackclient.api.GearPackClient
import com.acme.clients.gearpackclient.test.GearPackTestDb
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
class GearPackClientIntegrationTest {

    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("camper_db")
            .withUsername("postgres")
            .withPassword("postgres")

        private lateinit var client: GearPackClient
        private lateinit var jdbi: org.jdbi.v3.core.Jdbi

        @BeforeAll
        @JvmStatic
        fun setup() {
            GearPackTestDb.cleanAndMigrate(postgres.jdbcUrl, postgres.username, postgres.password)

            System.setProperty("DB_URL", postgres.jdbcUrl)
            System.setProperty("DB_USER", postgres.username)
            System.setProperty("DB_PASSWORD", postgres.password)
            client = createGearPackClient()
            jdbi = org.jdbi.v3.core.Jdbi.create(postgres.jdbcUrl, postgres.username, postgres.password)
        }
    }

    @BeforeEach
    fun truncate() {
        jdbi.withHandle<Unit, Exception> { handle ->
            handle.createUpdate("TRUNCATE TABLE gear_pack_items, gear_packs CASCADE").execute()
        }
    }

    private fun insertPack(id: UUID, name: String, description: String = "A test pack") {
        jdbi.withHandle<Unit, Exception> { handle ->
            handle.createUpdate(
                "INSERT INTO gear_packs (id, name, description) VALUES (:id, :name, :description)"
            )
                .bind("id", id)
                .bind("name", name)
                .bind("description", description)
                .execute()
        }
    }

    private fun insertItem(
        id: UUID,
        gearPackId: UUID,
        name: String,
        category: String,
        defaultQuantity: Int = 1,
        scalable: Boolean = false,
        sortOrder: Int = 0,
    ) {
        jdbi.withHandle<Unit, Exception> { handle ->
            handle.createUpdate(
                """
                INSERT INTO gear_pack_items (id, gear_pack_id, name, category, default_quantity, scalable, sort_order)
                VALUES (:id, :gearPackId, :name, :category, :defaultQuantity, :scalable, :sortOrder)
                """.trimIndent()
            )
                .bind("id", id)
                .bind("gearPackId", gearPackId)
                .bind("name", name)
                .bind("category", category)
                .bind("defaultQuantity", defaultQuantity)
                .bind("scalable", scalable)
                .bind("sortOrder", sortOrder)
                .execute()
        }
    }

    @Nested
    inner class GetAll {

        @Test
        fun `getAll returns empty list when no gear packs exist`() {
            val result = client.getAll(GetAllGearPacksParam())
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val packs = (result as Result.Success).value
            assertThat(packs).isEmpty()
        }

        @Test
        fun `getAll returns all gear packs ordered by name`() {
            val idZ = UUID.randomUUID()
            val idA = UUID.randomUUID()
            val idM = UUID.randomUUID()
            insertPack(idZ, "Zzz Pack")
            insertPack(idA, "Alpha Pack")
            insertPack(idM, "Mid Pack")

            val result = client.getAll(GetAllGearPacksParam())
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val packs = (result as Result.Success).value
            assertThat(packs).hasSize(3)
            assertThat(packs.map { it.name }).containsExactly("Alpha Pack", "Mid Pack", "Zzz Pack")
        }

        @Test
        fun `getAll returns packs with their items`() {
            val packId = UUID.randomUUID()
            insertPack(packId, "Cooking Equipment")
            insertItem(UUID.randomUUID(), packId, "Cast Iron Pan", "kitchen")

            val result = client.getAll(GetAllGearPacksParam())
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val packs = (result as Result.Success).value
            assertThat(packs).hasSize(1)
            assertThat(packs[0].items).hasSize(1)
            assertThat(packs[0].items[0].name).isEqualTo("Cast Iron Pan")
        }

        @Test
        fun `getAll returns correct pack fields`() {
            val packId = UUID.randomUUID()
            insertPack(packId, "Cooking Equipment", "Essential cooking gear for campfire meals.")

            val result = client.getAll(GetAllGearPacksParam())
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val pack = (result as Result.Success).value[0]
            assertThat(pack.id).isEqualTo(packId)
            assertThat(pack.name).isEqualTo("Cooking Equipment")
            assertThat(pack.description).isEqualTo("Essential cooking gear for campfire meals.")
            assertThat(pack.createdAt).isNotNull()
            assertThat(pack.updatedAt).isNotNull()
        }
    }

    @Nested
    inner class GetById {

        @Test
        fun `getById returns a gear pack with its items ordered by sort_order`() {
            val packId = UUID.randomUUID()
            insertPack(packId, "Cooking Equipment", "Essential cooking gear.")
            insertItem(UUID.randomUUID(), packId, "Plates", "kitchen", defaultQuantity = 1, scalable = true, sortOrder = 3)
            insertItem(UUID.randomUUID(), packId, "Cast Iron Pan", "kitchen", defaultQuantity = 1, scalable = false, sortOrder = 1)
            insertItem(UUID.randomUUID(), packId, "Tongs", "kitchen", defaultQuantity = 1, scalable = false, sortOrder = 2)

            val result = client.getById(GetGearPackByIdParam(packId))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val pack = (result as Result.Success).value
            assertThat(pack.id).isEqualTo(packId)
            assertThat(pack.name).isEqualTo("Cooking Equipment")
            assertThat(pack.description).isEqualTo("Essential cooking gear.")
            assertThat(pack.createdAt).isNotNull()
            assertThat(pack.updatedAt).isNotNull()
            assertThat(pack.items).hasSize(3)
            assertThat(pack.items.map { it.name }).containsExactly("Cast Iron Pan", "Tongs", "Plates")
            assertThat(pack.items.map { it.sortOrder }).containsExactly(1, 2, 3)
        }

        @Test
        fun `getById returns correct item fields`() {
            val packId = UUID.randomUUID()
            val itemId = UUID.randomUUID()
            insertPack(packId, "Test Pack")
            insertItem(itemId, packId, "Spatula", "kitchen", defaultQuantity = 2, scalable = true, sortOrder = 5)

            val result = client.getById(GetGearPackByIdParam(packId))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val item = (result as Result.Success).value.items[0]
            assertThat(item.id).isEqualTo(itemId)
            assertThat(item.gearPackId).isEqualTo(packId)
            assertThat(item.name).isEqualTo("Spatula")
            assertThat(item.category).isEqualTo("kitchen")
            assertThat(item.defaultQuantity).isEqualTo(2)
            assertThat(item.scalable).isTrue()
            assertThat(item.sortOrder).isEqualTo(5)
            assertThat(item.createdAt).isNotNull()
            assertThat(item.updatedAt).isNotNull()
        }

        @Test
        fun `getById returns NotFoundError when pack does not exist`() {
            val result = client.getById(GetGearPackByIdParam(UUID.randomUUID()))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(NotFoundError::class.java)
        }

        @Test
        fun `getById returns pack with empty items list when pack has no items`() {
            val packId = UUID.randomUUID()
            insertPack(packId, "Empty Pack")

            val result = client.getById(GetGearPackByIdParam(packId))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val pack = (result as Result.Success).value
            assertThat(pack.name).isEqualTo("Empty Pack")
            assertThat(pack.items).isEmpty()
        }

    }

    @Nested
    inner class DataIntegrity {

        @Test
        fun `after inserting a pack with items, getAll returns it and getById returns it with items`() {
            val packId = UUID.randomUUID()
            insertPack(packId, "Cooking Equipment", "Essential cooking gear.")
            insertItem(UUID.randomUUID(), packId, "Cast Iron Pan", "kitchen", defaultQuantity = 1, scalable = false, sortOrder = 1)
            insertItem(UUID.randomUUID(), packId, "Plates", "kitchen", defaultQuantity = 1, scalable = true, sortOrder = 2)

            // getAll returns the pack
            val allResult = client.getAll(GetAllGearPacksParam())
            assertThat(allResult).isInstanceOf(Result.Success::class.java)
            val allPacks = (allResult as Result.Success).value
            assertThat(allPacks).hasSize(1)
            assertThat(allPacks[0].id).isEqualTo(packId)
            assertThat(allPacks[0].name).isEqualTo("Cooking Equipment")
            assertThat(allPacks[0].items).hasSize(2) // getAll now includes items

            // getById returns the pack with items
            val byIdResult = client.getById(GetGearPackByIdParam(packId))
            assertThat(byIdResult).isInstanceOf(Result.Success::class.java)
            val pack = (byIdResult as Result.Success).value
            assertThat(pack.id).isEqualTo(packId)
            assertThat(pack.items).hasSize(2)
            assertThat(pack.items[0].name).isEqualTo("Cast Iron Pan")
            assertThat(pack.items[1].name).isEqualTo("Plates")
        }

    }
}
