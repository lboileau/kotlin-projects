package com.acme.clients.gearpackclient

import com.acme.clients.common.Result
import com.acme.clients.common.error.ConflictError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.error.ValidationError
import com.acme.clients.gearpackclient.api.AddGearPackItemParam
import com.acme.clients.gearpackclient.api.CreateGearPackParam
import com.acme.clients.gearpackclient.api.DeleteGearPackParam
import com.acme.clients.gearpackclient.api.GetAllGearPacksParam
import com.acme.clients.gearpackclient.api.GetGearPackByIdParam
import com.acme.clients.gearpackclient.api.GearPackClient
import com.acme.clients.gearpackclient.api.RemoveGearPackItemParam
import com.acme.clients.gearpackclient.api.SearchGearPackItemsParam
import com.acme.clients.gearpackclient.api.UpdateGearPackItemParam
import com.acme.clients.gearpackclient.api.UpdateGearPackParam
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
            handle.createUpdate("TRUNCATE TABLE gear_pack_items, items, gear_packs, plans, users CASCADE").execute()
        }
    }

    private fun insertUser(id: UUID, email: String = "user@example.com") {
        jdbi.withHandle<Unit, Exception> { handle ->
            handle.createUpdate("INSERT INTO users (id, email) VALUES (:id, :email)")
                .bind("id", id)
                .bind("email", email)
                .execute()
        }
    }

    private fun insertPlan(id: UUID, ownerId: UUID, name: String = "Test Plan") {
        jdbi.withHandle<Unit, Exception> { handle ->
            handle.createUpdate("INSERT INTO plans (id, name, owner_id) VALUES (:id, :name, :ownerId)")
                .bind("id", id)
                .bind("name", name)
                .bind("ownerId", ownerId)
                .execute()
        }
    }

    private fun insertPlanItem(id: UUID, planId: UUID, name: String, category: String, gearPackId: UUID?) {
        jdbi.withHandle<Unit, Exception> { handle ->
            handle.createUpdate(
                "INSERT INTO items (id, plan_id, name, category, gear_pack_id) VALUES (:id, :planId, :name, :category, :gearPackId)"
            )
                .bind("id", id)
                .bind("planId", planId)
                .bind("name", name)
                .bind("category", category)
                .bind("gearPackId", gearPackId)
                .execute()
        }
    }

    private fun insertPack(id: UUID, name: String, description: String = "A test pack", createdBy: UUID? = null) {
        jdbi.withHandle<Unit, Exception> { handle ->
            if (createdBy != null) {
                handle.createUpdate(
                    "INSERT INTO gear_packs (id, name, description, created_by) VALUES (:id, :name, :description, :createdBy)"
                )
                    .bind("id", id)
                    .bind("name", name)
                    .bind("description", description)
                    .bind("createdBy", createdBy)
                    .execute()
            } else {
                handle.createUpdate(
                    "INSERT INTO gear_packs (id, name, description) VALUES (:id, :name, :description)"
                )
                    .bind("id", id)
                    .bind("name", name)
                    .bind("description", description)
                    .execute()
            }
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
            val userId = UUID.randomUUID()
            insertUser(userId)
            insertPack(packId, "Cooking Equipment", "Essential cooking gear for campfire meals.", createdBy = userId)

            val result = client.getAll(GetAllGearPacksParam())
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val pack = (result as Result.Success).value[0]
            assertThat(pack.id).isEqualTo(packId)
            assertThat(pack.name).isEqualTo("Cooking Equipment")
            assertThat(pack.description).isEqualTo("Essential cooking gear for campfire meals.")
            assertThat(pack.createdBy).isEqualTo(userId)
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

        @Test
        fun `getById returns null createdBy for system-created pack`() {
            val packId = UUID.randomUUID()
            insertPack(packId, "System Pack")

            val result = client.getById(GetGearPackByIdParam(packId))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val pack = (result as Result.Success).value
            assertThat(pack.createdBy).isNull()
        }

    }

    @Nested
    inner class Create {

        @Test
        fun `create successfully creates a gear pack`() {
            val userId = UUID.randomUUID()
            insertUser(userId)

            val result = client.create(CreateGearPackParam(name = "Wilderness Kit", description = "For remote trips", createdBy = userId))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val pack = (result as Result.Success).value
            assertThat(pack.id).isNotNull()
            assertThat(pack.name).isEqualTo("Wilderness Kit")
            assertThat(pack.description).isEqualTo("For remote trips")
            assertThat(pack.createdBy).isEqualTo(userId)
            assertThat(pack.items).isEmpty()
            assertThat(pack.createdAt).isNotNull()
            assertThat(pack.updatedAt).isNotNull()
        }

        @Test
        fun `create persists pack so it is retrievable by getById`() {
            val userId = UUID.randomUUID()
            insertUser(userId)

            val created = (client.create(CreateGearPackParam(name = "My Pack", createdBy = userId)) as Result.Success).value

            val fetched = client.getById(GetGearPackByIdParam(created.id))
            assertThat(fetched).isInstanceOf(Result.Success::class.java)
            assertThat((fetched as Result.Success).value.name).isEqualTo("My Pack")
        }

        @Test
        fun `create returns ConflictError for duplicate name`() {
            val userId = UUID.randomUUID()
            insertUser(userId)
            insertPack(UUID.randomUUID(), "Existing Pack", createdBy = userId)

            val result = client.create(CreateGearPackParam(name = "Existing Pack", createdBy = userId))

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).error).isInstanceOf(ConflictError::class.java)
        }

        @Test
        fun `create returns ValidationError for blank name`() {
            val userId = UUID.randomUUID()
            insertUser(userId)

            val result = client.create(CreateGearPackParam(name = "  ", createdBy = userId))

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
            assertThat((error as ValidationError).field).isEqualTo("name")
        }

        @Test
        fun `create returns ValidationError for name exceeding 100 characters`() {
            val userId = UUID.randomUUID()
            insertUser(userId)

            val result = client.create(CreateGearPackParam(name = "A".repeat(101), createdBy = userId))

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
            assertThat((error as ValidationError).field).isEqualTo("name")
        }

        @Test
        fun `create returns ValidationError for description exceeding 500 characters`() {
            val userId = UUID.randomUUID()
            insertUser(userId)

            val result = client.create(CreateGearPackParam(name = "Valid Name", description = "D".repeat(501), createdBy = userId))

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
            assertThat((error as ValidationError).field).isEqualTo("description")
        }
    }

    @Nested
    inner class Update {

        @Test
        fun `update successfully updates name only`() {
            val packId = UUID.randomUUID()
            insertPack(packId, "Old Name", "Original description")

            val result = client.update(UpdateGearPackParam(id = packId, name = "New Name"))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val pack = (result as Result.Success).value
            assertThat(pack.name).isEqualTo("New Name")
            assertThat(pack.description).isEqualTo("Original description")
        }

        @Test
        fun `update successfully updates description only`() {
            val packId = UUID.randomUUID()
            insertPack(packId, "My Pack", "Old description")

            val result = client.update(UpdateGearPackParam(id = packId, description = "New description"))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val pack = (result as Result.Success).value
            assertThat(pack.name).isEqualTo("My Pack")
            assertThat(pack.description).isEqualTo("New description")
        }

        @Test
        fun `update successfully updates both name and description`() {
            val packId = UUID.randomUUID()
            insertPack(packId, "Old Name", "Old description")

            val result = client.update(UpdateGearPackParam(id = packId, name = "New Name", description = "New description"))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val pack = (result as Result.Success).value
            assertThat(pack.name).isEqualTo("New Name")
            assertThat(pack.description).isEqualTo("New description")
        }

        @Test
        fun `update returns NotFoundError for non-existent pack`() {
            val result = client.update(UpdateGearPackParam(id = UUID.randomUUID(), name = "Whatever"))

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).error).isInstanceOf(NotFoundError::class.java)
        }

        @Test
        fun `update returns ConflictError when name collides with another pack`() {
            val packId1 = UUID.randomUUID()
            val packId2 = UUID.randomUUID()
            insertPack(packId1, "Pack One")
            insertPack(packId2, "Pack Two")

            val result = client.update(UpdateGearPackParam(id = packId2, name = "Pack One"))

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).error).isInstanceOf(ConflictError::class.java)
        }

        @Test
        fun `update returns ValidationError for blank name`() {
            val packId = UUID.randomUUID()
            insertPack(packId, "Valid Pack")

            val result = client.update(UpdateGearPackParam(id = packId, name = ""))

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
            assertThat((error as ValidationError).field).isEqualTo("name")
        }

        @Test
        fun `update returns ValidationError for name exceeding 100 characters`() {
            val packId = UUID.randomUUID()
            insertPack(packId, "Valid Pack")

            val result = client.update(UpdateGearPackParam(id = packId, name = "A".repeat(101)))

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).error).isInstanceOf(ValidationError::class.java)
        }

        @Test
        fun `update returns ValidationError for description exceeding 500 characters`() {
            val packId = UUID.randomUUID()
            insertPack(packId, "Valid Pack")

            val result = client.update(UpdateGearPackParam(id = packId, description = "D".repeat(501)))

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
            assertThat((error as ValidationError).field).isEqualTo("description")
        }
    }

    @Nested
    inner class Delete {

        @Test
        fun `delete successfully deletes a pack`() {
            val packId = UUID.randomUUID()
            insertPack(packId, "Doomed Pack")

            val result = client.delete(DeleteGearPackParam(id = packId))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val fetchResult = client.getById(GetGearPackByIdParam(packId))
            assertThat(fetchResult).isInstanceOf(Result.Failure::class.java)
            assertThat((fetchResult as Result.Failure).error).isInstanceOf(NotFoundError::class.java)
        }

        @Test
        fun `delete returns NotFoundError for non-existent pack`() {
            val result = client.delete(DeleteGearPackParam(id = UUID.randomUUID()))

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).error).isInstanceOf(NotFoundError::class.java)
        }

        @Test
        fun `delete sets gear_pack_id to NULL on plan items referencing the deleted pack`() {
            val userId = UUID.randomUUID()
            val planId = UUID.randomUUID()
            val packId = UUID.randomUUID()
            val planItemId = UUID.randomUUID()
            insertUser(userId)
            insertPlan(planId, userId)
            insertPack(packId, "Pack To Delete")
            insertPlanItem(planItemId, planId, "Tent", "shelter", packId)

            client.delete(DeleteGearPackParam(id = packId))

            val gearPackId = jdbi.withHandle<UUID?, Exception> { handle ->
                handle.createQuery("SELECT gear_pack_id FROM items WHERE id = :id")
                    .bind("id", planItemId)
                    .mapTo(UUID::class.java)
                    .findOne()
                    .orElse(null)
            }
            assertThat(gearPackId).isNull()
        }
    }

    @Nested
    inner class AddItem {

        @Test
        fun `addItem successfully adds first item with sort_order 1`() {
            val packId = UUID.randomUUID()
            insertPack(packId, "Camping Kit")

            val result = client.addItem(
                AddGearPackItemParam(gearPackId = packId, name = "Sleeping Bag", category = "shelter")
            )

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val item = (result as Result.Success).value
            assertThat(item.id).isNotNull()
            assertThat(item.gearPackId).isEqualTo(packId)
            assertThat(item.name).isEqualTo("Sleeping Bag")
            assertThat(item.category).isEqualTo("shelter")
            assertThat(item.sortOrder).isEqualTo(1)
            assertThat(item.defaultQuantity).isEqualTo(1)
            assertThat(item.scalable).isFalse()
            assertThat(item.createdAt).isNotNull()
            assertThat(item.updatedAt).isNotNull()
        }

        @Test
        fun `addItem assigns sequential sort_order for subsequent items`() {
            val packId = UUID.randomUUID()
            insertPack(packId, "Camping Kit")

            val first = (client.addItem(AddGearPackItemParam(gearPackId = packId, name = "Sleeping Bag", category = "shelter")) as Result.Success).value
            val second = (client.addItem(AddGearPackItemParam(gearPackId = packId, name = "Tent", category = "shelter")) as Result.Success).value

            assertThat(first.sortOrder).isEqualTo(1)
            assertThat(second.sortOrder).isEqualTo(2)
        }

        @Test
        fun `addItem returns ConflictError for duplicate item name (case-insensitive) within same pack`() {
            val packId = UUID.randomUUID()
            insertPack(packId, "Camping Kit")
            insertItem(UUID.randomUUID(), packId, "Sleeping Bag", "shelter", sortOrder = 1)

            val result = client.addItem(
                AddGearPackItemParam(gearPackId = packId, name = "sleeping bag", category = "shelter")
            )

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).error).isInstanceOf(ConflictError::class.java)
        }

        @Test
        fun `addItem allows same item name in different packs`() {
            val packId1 = UUID.randomUUID()
            val packId2 = UUID.randomUUID()
            insertPack(packId1, "Pack One")
            insertPack(packId2, "Pack Two")
            insertItem(UUID.randomUUID(), packId1, "Sleeping Bag", "shelter", sortOrder = 1)

            val result = client.addItem(
                AddGearPackItemParam(gearPackId = packId2, name = "Sleeping Bag", category = "shelter")
            )

            assertThat(result).isInstanceOf(Result.Success::class.java)
        }

        @Test
        fun `addItem returns ValidationError for blank name`() {
            val packId = UUID.randomUUID()
            insertPack(packId, "Camping Kit")

            val result = client.addItem(AddGearPackItemParam(gearPackId = packId, name = "  ", category = "shelter"))

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
            assertThat((error as ValidationError).field).isEqualTo("name")
        }

        @Test
        fun `addItem returns ValidationError for blank category`() {
            val packId = UUID.randomUUID()
            insertPack(packId, "Camping Kit")

            val result = client.addItem(AddGearPackItemParam(gearPackId = packId, name = "Tent", category = ""))

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
            assertThat((error as ValidationError).field).isEqualTo("category")
        }

        @Test
        fun `addItem returns ValidationError for defaultQuantity less than 1`() {
            val packId = UUID.randomUUID()
            insertPack(packId, "Camping Kit")

            val result = client.addItem(AddGearPackItemParam(gearPackId = packId, name = "Tent", category = "shelter", defaultQuantity = 0))

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
            assertThat((error as ValidationError).field).isEqualTo("defaultQuantity")
        }
    }

    @Nested
    inner class UpdateItem {

        @Test
        fun `updateItem successfully updates name only`() {
            val packId = UUID.randomUUID()
            val itemId = UUID.randomUUID()
            insertPack(packId, "Camping Kit")
            insertItem(itemId, packId, "Old Name", "shelter", sortOrder = 1)

            val result = client.updateItem(UpdateGearPackItemParam(id = itemId, gearPackId = packId, name = "New Name"))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val item = (result as Result.Success).value
            assertThat(item.name).isEqualTo("New Name")
            assertThat(item.category).isEqualTo("shelter")
        }

        @Test
        fun `updateItem successfully updates all fields`() {
            val packId = UUID.randomUUID()
            val itemId = UUID.randomUUID()
            insertPack(packId, "Camping Kit")
            insertItem(itemId, packId, "Tent", "shelter", defaultQuantity = 1, scalable = false, sortOrder = 1)

            val result = client.updateItem(
                UpdateGearPackItemParam(
                    id = itemId,
                    gearPackId = packId,
                    name = "Big Tent",
                    category = "accommodation",
                    defaultQuantity = 2,
                    scalable = true,
                )
            )

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val item = (result as Result.Success).value
            assertThat(item.name).isEqualTo("Big Tent")
            assertThat(item.category).isEqualTo("accommodation")
            assertThat(item.defaultQuantity).isEqualTo(2)
            assertThat(item.scalable).isTrue()
        }

        @Test
        fun `updateItem returns NotFoundError for non-existent item`() {
            val packId = UUID.randomUUID()
            insertPack(packId, "Camping Kit")

            val result = client.updateItem(UpdateGearPackItemParam(id = UUID.randomUUID(), gearPackId = packId, name = "Whatever"))

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).error).isInstanceOf(NotFoundError::class.java)
        }

        @Test
        fun `updateItem returns NotFoundError when item belongs to different pack`() {
            val packId1 = UUID.randomUUID()
            val packId2 = UUID.randomUUID()
            val itemId = UUID.randomUUID()
            insertPack(packId1, "Pack One")
            insertPack(packId2, "Pack Two")
            insertItem(itemId, packId1, "Tent", "shelter", sortOrder = 1)

            val result = client.updateItem(UpdateGearPackItemParam(id = itemId, gearPackId = packId2, name = "New Name"))

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).error).isInstanceOf(NotFoundError::class.java)
        }

        @Test
        fun `updateItem returns ConflictError when new name collides with existing item in same pack`() {
            val packId = UUID.randomUUID()
            val itemId1 = UUID.randomUUID()
            val itemId2 = UUID.randomUUID()
            insertPack(packId, "Camping Kit")
            insertItem(itemId1, packId, "Sleeping Bag", "shelter", sortOrder = 1)
            insertItem(itemId2, packId, "Tent", "shelter", sortOrder = 2)

            val result = client.updateItem(UpdateGearPackItemParam(id = itemId2, gearPackId = packId, name = "sleeping bag"))

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).error).isInstanceOf(ConflictError::class.java)
        }

        @Test
        fun `updateItem returns ValidationError for blank name`() {
            val packId = UUID.randomUUID()
            val itemId = UUID.randomUUID()
            insertPack(packId, "Camping Kit")
            insertItem(itemId, packId, "Tent", "shelter", sortOrder = 1)

            val result = client.updateItem(UpdateGearPackItemParam(id = itemId, gearPackId = packId, name = "  "))

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
            assertThat((error as ValidationError).field).isEqualTo("name")
        }

        @Test
        fun `updateItem returns ValidationError for blank category`() {
            val packId = UUID.randomUUID()
            val itemId = UUID.randomUUID()
            insertPack(packId, "Camping Kit")
            insertItem(itemId, packId, "Tent", "shelter", sortOrder = 1)

            val result = client.updateItem(UpdateGearPackItemParam(id = itemId, gearPackId = packId, category = ""))

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
            assertThat((error as ValidationError).field).isEqualTo("category")
        }

        @Test
        fun `updateItem returns ValidationError for name exceeding length limit`() {
            val packId = UUID.randomUUID()
            val itemId = UUID.randomUUID()
            insertPack(packId, "Camping Kit")
            insertItem(itemId, packId, "Tent", "shelter", sortOrder = 1)

            val result = client.updateItem(UpdateGearPackItemParam(id = itemId, gearPackId = packId, name = "A".repeat(101)))

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
            assertThat((error as ValidationError).field).isEqualTo("name")
        }
    }

    @Nested
    inner class RemoveItem {

        @Test
        fun `removeItem successfully removes an item from a pack`() {
            val packId = UUID.randomUUID()
            val itemId = UUID.randomUUID()
            insertPack(packId, "Camping Kit")
            insertItem(itemId, packId, "Sleeping Bag", "shelter", sortOrder = 1)

            val result = client.removeItem(RemoveGearPackItemParam(id = itemId, gearPackId = packId))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val pack = (client.getById(GetGearPackByIdParam(packId)) as Result.Success).value
            assertThat(pack.items).isEmpty()
        }

        @Test
        fun `removeItem returns NotFoundError for non-existent item`() {
            val packId = UUID.randomUUID()
            insertPack(packId, "Camping Kit")

            val result = client.removeItem(RemoveGearPackItemParam(id = UUID.randomUUID(), gearPackId = packId))

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).error).isInstanceOf(NotFoundError::class.java)
        }

        @Test
        fun `removeItem returns NotFoundError when item belongs to different pack`() {
            val packId1 = UUID.randomUUID()
            val packId2 = UUID.randomUUID()
            val itemId = UUID.randomUUID()
            insertPack(packId1, "Pack One")
            insertPack(packId2, "Pack Two")
            insertItem(itemId, packId1, "Tent", "shelter", sortOrder = 1)

            val result = client.removeItem(RemoveGearPackItemParam(id = itemId, gearPackId = packId2))

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).error).isInstanceOf(NotFoundError::class.java)
        }
    }

    @Nested
    inner class SearchItems {

        @Test
        fun `searchItems finds items by case-insensitive substring match`() {
            val packId = UUID.randomUUID()
            insertPack(packId, "Camping Kit")
            insertItem(UUID.randomUUID(), packId, "Sleeping Bag", "shelter", sortOrder = 1)
            insertItem(UUID.randomUUID(), packId, "Sleeping Pad", "shelter", sortOrder = 2)
            insertItem(UUID.randomUUID(), packId, "Tent", "shelter", sortOrder = 3)

            val result = client.searchItems(SearchGearPackItemsParam("sleeping"))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val items = (result as Result.Success).value
            assertThat(items).hasSize(2)
            assertThat(items.map { it.name }).containsExactlyInAnyOrder("Sleeping Bag", "Sleeping Pad")
        }

        @Test
        fun `searchItems is case-insensitive`() {
            val packId = UUID.randomUUID()
            insertPack(packId, "Camping Kit")
            insertItem(UUID.randomUUID(), packId, "Sleeping Bag", "shelter", sortOrder = 1)

            val result = client.searchItems(SearchGearPackItemsParam("SLEEPING"))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            assertThat((result as Result.Success).value).hasSize(1)
        }

        @Test
        fun `searchItems returns empty list when no items match`() {
            val packId = UUID.randomUUID()
            insertPack(packId, "Camping Kit")
            insertItem(UUID.randomUUID(), packId, "Tent", "shelter", sortOrder = 1)

            val result = client.searchItems(SearchGearPackItemsParam("nonexistent"))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            assertThat((result as Result.Success).value).isEmpty()
        }

        @Test
        fun `searchItems includes gearPackName in results`() {
            val packId = UUID.randomUUID()
            insertPack(packId, "Wilderness Kit")
            insertItem(UUID.randomUUID(), packId, "Compass", "navigation", defaultQuantity = 3, scalable = true, sortOrder = 1)

            val result = client.searchItems(SearchGearPackItemsParam("compass"))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val item = (result as Result.Success).value[0]
            assertThat(item.gearPackName).isEqualTo("Wilderness Kit")
            assertThat(item.gearPackId).isEqualTo(packId)
            assertThat(item.name).isEqualTo("Compass")
            assertThat(item.category).isEqualTo("navigation")
            assertThat(item.defaultQuantity).isEqualTo(3)
            assertThat(item.scalable).isTrue()
        }

        @Test
        fun `searchItems returns results from multiple packs`() {
            val packId1 = UUID.randomUUID()
            val packId2 = UUID.randomUUID()
            insertPack(packId1, "Pack One")
            insertPack(packId2, "Pack Two")
            insertItem(UUID.randomUUID(), packId1, "Map", "navigation", sortOrder = 1)
            insertItem(UUID.randomUUID(), packId2, "Map Holder", "navigation", sortOrder = 1)

            val result = client.searchItems(SearchGearPackItemsParam("map"))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            assertThat((result as Result.Success).value).hasSize(2)
        }

        @Test
        fun `searchItems returns ValidationError for blank query`() {
            val result = client.searchItems(SearchGearPackItemsParam("  "))

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
            assertThat((error as ValidationError).field).isEqualTo("query")
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

        @Test
        fun `create then update then getById reflects updated state`() {
            val userId = UUID.randomUUID()
            insertUser(userId)

            val created = (client.create(CreateGearPackParam(name = "Original", description = "Desc", createdBy = userId)) as Result.Success).value
            client.update(UpdateGearPackParam(id = created.id, name = "Updated", description = "New desc"))
            val fetched = (client.getById(GetGearPackByIdParam(created.id)) as Result.Success).value

            assertThat(fetched.name).isEqualTo("Updated")
            assertThat(fetched.description).isEqualTo("New desc")
        }

        @Test
        fun `addItem then removeItem leaves pack with no items`() {
            val packId = UUID.randomUUID()
            insertPack(packId, "My Pack")

            val item = (client.addItem(AddGearPackItemParam(gearPackId = packId, name = "Flashlight", category = "lighting")) as Result.Success).value
            client.removeItem(RemoveGearPackItemParam(id = item.id, gearPackId = packId))

            val pack = (client.getById(GetGearPackByIdParam(packId)) as Result.Success).value
            assertThat(pack.items).isEmpty()
        }
    }
}
