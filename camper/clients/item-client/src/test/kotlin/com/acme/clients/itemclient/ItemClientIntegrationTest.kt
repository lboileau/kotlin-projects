package com.acme.clients.itemclient

import com.acme.clients.common.Result
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.error.ValidationError
import com.acme.clients.itemclient.api.CreateItemParam
import com.acme.clients.itemclient.api.DeleteItemParam
import com.acme.clients.itemclient.api.GetByIdParam
import com.acme.clients.itemclient.api.GetByPlanIdParam
import com.acme.clients.itemclient.api.GetByUserIdParam
import com.acme.clients.itemclient.api.ItemClient
import com.acme.clients.itemclient.api.UpdateItemParam
import com.acme.clients.itemclient.test.ItemTestDb
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
class ItemClientIntegrationTest {

    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("camper_db")
            .withUsername("postgres")
            .withPassword("postgres")

        private lateinit var client: ItemClient
        private lateinit var jdbi: org.jdbi.v3.core.Jdbi

        @BeforeAll
        @JvmStatic
        fun setup() {
            ItemTestDb.cleanAndMigrate(postgres.jdbcUrl, postgres.username, postgres.password)

            System.setProperty("DB_URL", postgres.jdbcUrl)
            System.setProperty("DB_USER", postgres.username)
            System.setProperty("DB_PASSWORD", postgres.password)
            client = createItemClient()
            jdbi = org.jdbi.v3.core.Jdbi.create(postgres.jdbcUrl, postgres.username, postgres.password)
        }
    }

    private lateinit var testUserId: UUID
    private lateinit var testPlanId: UUID

    @BeforeEach
    fun truncateAndSeed() {
        jdbi.withHandle<Unit, Exception> { handle ->
            handle.createUpdate("TRUNCATE TABLE items, plan_members, plans, users CASCADE").execute()
        }
        testUserId = UUID.randomUUID()
        testPlanId = UUID.randomUUID()
        jdbi.withHandle<Unit, Exception> { handle ->
            handle.createUpdate("INSERT INTO users (id, email, username) VALUES (:id, :email, :username)")
                .bind("id", testUserId).bind("email", "test@example.com").bind("username", "testuser").execute()
            handle.createUpdate("INSERT INTO plans (id, name, visibility, owner_id) VALUES (:id, :name, :visibility, :ownerId)")
                .bind("id", testPlanId).bind("name", "Test Plan").bind("visibility", "private").bind("ownerId", testUserId).execute()
        }
    }

    @Nested
    inner class Create {
        @Test
        fun `create returns created item with plan owner`() {
            val result = client.create(
                CreateItemParam(planId = testPlanId, userId = null, name = "Tent", category = "shelter", quantity = 1, packed = false)
            )
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val item = (result as Result.Success).value
            assertThat(item.name).isEqualTo("Tent")
            assertThat(item.category).isEqualTo("shelter")
            assertThat(item.quantity).isEqualTo(1)
            assertThat(item.packed).isFalse()
            assertThat(item.planId).isEqualTo(testPlanId)
            assertThat(item.userId).isNull()
            assertThat(item.id).isNotNull()
            assertThat(item.createdAt).isNotNull()
        }

        @Test
        fun `create returns created item with user owner`() {
            val result = client.create(
                CreateItemParam(planId = null, userId = testUserId, name = "Flashlight", category = "lighting", quantity = 2, packed = true)
            )
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val item = (result as Result.Success).value
            assertThat(item.name).isEqualTo("Flashlight")
            assertThat(item.userId).isEqualTo(testUserId)
            assertThat(item.planId).isNull()
            assertThat(item.quantity).isEqualTo(2)
            assertThat(item.packed).isTrue()
        }

        @Test
        fun `create returns ValidationError for blank name`() {
            val result = client.create(
                CreateItemParam(planId = testPlanId, userId = null, name = "", category = "shelter", quantity = 1, packed = false)
            )
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
            assertThat((error as ValidationError).field).isEqualTo("name")
        }

        @Test
        fun `create returns ValidationError for quantity less than or equal to 0`() {
            val result = client.create(
                CreateItemParam(planId = testPlanId, userId = null, name = "Tent", category = "shelter", quantity = 0, packed = false)
            )
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
            assertThat((error as ValidationError).field).isEqualTo("quantity")
        }

        @Test
        fun `create returns ValidationError when both planId and userId are set`() {
            val result = client.create(
                CreateItemParam(planId = testPlanId, userId = testUserId, name = "Tent", category = "shelter", quantity = 1, packed = false)
            )
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
            assertThat((error as ValidationError).field).isEqualTo("planId/userId")
        }

        @Test
        fun `create returns ValidationError when neither planId nor userId is set`() {
            val result = client.create(
                CreateItemParam(planId = null, userId = null, name = "Tent", category = "shelter", quantity = 1, packed = false)
            )
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
            assertThat((error as ValidationError).field).isEqualTo("planId/userId")
        }
    }

    @Nested
    inner class GetById {
        @Test
        fun `getById returns item when it exists`() {
            val created = (client.create(
                CreateItemParam(planId = testPlanId, userId = null, name = "Sleeping Bag", category = "sleeping", quantity = 1, packed = false)
            ) as Result.Success).value

            val result = client.getById(GetByIdParam(created.id))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val found = (result as Result.Success).value
            assertThat(found.name).isEqualTo("Sleeping Bag")
            assertThat(found.category).isEqualTo("sleeping")
            assertThat(found.planId).isEqualTo(testPlanId)
        }

        @Test
        fun `getById returns NotFoundError when item does not exist`() {
            val result = client.getById(GetByIdParam(UUID.randomUUID()))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(NotFoundError::class.java)
        }
    }

    @Nested
    inner class GetByPlanId {
        @Test
        fun `getByPlanId returns items belonging to the plan`() {
            client.create(CreateItemParam(planId = testPlanId, userId = null, name = "Tent", category = "shelter", quantity = 1, packed = false))
            client.create(CreateItemParam(planId = testPlanId, userId = null, name = "Stove", category = "cooking", quantity = 1, packed = false))

            val result = client.getByPlanId(GetByPlanIdParam(testPlanId))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val items = (result as Result.Success).value
            assertThat(items).hasSize(2)
            assertThat(items.map { it.name }).containsExactlyInAnyOrder("Tent", "Stove")
        }

        @Test
        fun `getByPlanId returns empty list when plan has no items`() {
            val result = client.getByPlanId(GetByPlanIdParam(testPlanId))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val items = (result as Result.Success).value
            assertThat(items).isEmpty()
        }
    }

    @Nested
    inner class GetByUserId {
        @Test
        fun `getByUserId returns items belonging to the user`() {
            client.create(CreateItemParam(planId = null, userId = testUserId, name = "Headlamp", category = "lighting", quantity = 1, packed = false))

            val result = client.getByUserId(GetByUserIdParam(testUserId))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val items = (result as Result.Success).value
            assertThat(items).hasSize(1)
            assertThat(items[0].name).isEqualTo("Headlamp")
        }

        @Test
        fun `getByUserId returns empty list when user has no items`() {
            val result = client.getByUserId(GetByUserIdParam(testUserId))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val items = (result as Result.Success).value
            assertThat(items).isEmpty()
        }
    }

    @Nested
    inner class Update {
        @Test
        fun `update returns updated item`() {
            val created = (client.create(
                CreateItemParam(planId = testPlanId, userId = null, name = "Old Tent", category = "shelter", quantity = 1, packed = false)
            ) as Result.Success).value

            val result = client.update(UpdateItemParam(id = created.id, name = "New Tent", category = "shelter", quantity = 2, packed = true))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val updated = (result as Result.Success).value
            assertThat(updated.name).isEqualTo("New Tent")
            assertThat(updated.quantity).isEqualTo(2)
            assertThat(updated.packed).isTrue()
        }

        @Test
        fun `update returns NotFoundError when item does not exist`() {
            val result = client.update(UpdateItemParam(id = UUID.randomUUID(), name = "Nope", category = "other", quantity = 1, packed = false))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(NotFoundError::class.java)
        }

        @Test
        fun `update returns ValidationError for blank name`() {
            val created = (client.create(
                CreateItemParam(planId = testPlanId, userId = null, name = "Tent", category = "shelter", quantity = 1, packed = false)
            ) as Result.Success).value

            val result = client.update(UpdateItemParam(id = created.id, name = "", category = "shelter", quantity = 1, packed = false))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
            assertThat((error as ValidationError).field).isEqualTo("name")
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `delete returns success when item exists`() {
            val created = (client.create(
                CreateItemParam(planId = testPlanId, userId = null, name = "Doomed", category = "other", quantity = 1, packed = false)
            ) as Result.Success).value

            val result = client.delete(DeleteItemParam(created.id))
            assertThat(result).isInstanceOf(Result.Success::class.java)

            val getResult = client.getById(GetByIdParam(created.id))
            assertThat(getResult).isInstanceOf(Result.Failure::class.java)
        }

        @Test
        fun `delete returns NotFoundError when item does not exist`() {
            val result = client.delete(DeleteItemParam(UUID.randomUUID()))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(NotFoundError::class.java)
        }
    }
}
