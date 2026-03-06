package com.example.services.hello.features.world.service

import com.example.clients.common.Result
import com.example.clients.worldclient.fake.FakeWorldClient
import com.example.clients.worldclient.model.World as ClientWorld
import com.example.services.hello.features.world.error.WorldError
import com.example.services.hello.features.world.params.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class WorldServiceTest {

    private val fakeWorldClient = FakeWorldClient()
    private val worldService = WorldService(fakeWorldClient)

    @BeforeEach
    fun setUp() {
        fakeWorldClient.reset()
    }

    @Nested
    inner class GetById {
        @Test
        fun `getById returns world when it exists`() {
            val world = ClientWorld(
                id = UUID.randomUUID(),
                name = "Test World",
                greeting = "Hello!",
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            fakeWorldClient.seed(world)

            val result = worldService.getById(GetWorldByIdParam(id = world.id))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val success = result as Result.Success
            assertThat(success.value.id).isEqualTo(world.id)
            assertThat(success.value.name).isEqualTo("Test World")
            assertThat(success.value.greeting).isEqualTo("Hello!")
        }

        @Test
        fun `getById returns not found when world does not exist`() {
            val result = worldService.getById(GetWorldByIdParam(id = UUID.randomUUID()))

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val failure = result as Result.Failure
            assertThat(failure.error).isInstanceOf(WorldError.NotFound::class.java)
        }
    }

    @Nested
    inner class GetAll {
        @Test
        fun `getAll returns all worlds`() {
            fakeWorldClient.seed(
                ClientWorld(UUID.randomUUID(), "Alpha", "Hi!", Instant.now(), Instant.now()),
                ClientWorld(UUID.randomUUID(), "Beta", "Hey!", Instant.now(), Instant.now())
            )

            val result = worldService.getAll(GetAllWorldsParam())

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val success = result as Result.Success
            assertThat(success.value).hasSize(2)
        }

        @Test
        fun `getAll returns empty list when no worlds exist`() {
            val result = worldService.getAll(GetAllWorldsParam())

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val success = result as Result.Success
            assertThat(success.value).isEmpty()
        }
    }

    @Nested
    inner class Create {
        @Test
        fun `create returns created world`() {
            val result = worldService.create(CreateWorldParam(name = "New World", greeting = "Welcome!"))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val success = result as Result.Success
            assertThat(success.value.name).isEqualTo("New World")
            assertThat(success.value.greeting).isEqualTo("Welcome!")
        }

        @Test
        fun `create returns error when name is blank`() {
            val result = worldService.create(CreateWorldParam(name = "", greeting = "Welcome!"))

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val failure = result as Result.Failure
            assertThat(failure.error).isInstanceOf(WorldError.Invalid::class.java)
        }

        @Test
        fun `create returns error when greeting is blank`() {
            val result = worldService.create(CreateWorldParam(name = "World", greeting = ""))

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val failure = result as Result.Failure
            assertThat(failure.error).isInstanceOf(WorldError.Invalid::class.java)
        }

        @Test
        fun `create returns conflict when name already exists`() {
            fakeWorldClient.seed(
                ClientWorld(UUID.randomUUID(), "Existing", "Hi!", Instant.now(), Instant.now())
            )

            val result = worldService.create(CreateWorldParam(name = "Existing", greeting = "Hello!"))

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val failure = result as Result.Failure
            assertThat(failure.error).isInstanceOf(WorldError.AlreadyExists::class.java)
        }
    }

    @Nested
    inner class Update {
        @Test
        fun `update returns updated world`() {
            val world = ClientWorld(UUID.randomUUID(), "Old Name", "Old Greeting", Instant.now(), Instant.now())
            fakeWorldClient.seed(world)

            val result = worldService.update(UpdateWorldParam(id = world.id, name = "New Name", greeting = "New Greeting"))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val success = result as Result.Success
            assertThat(success.value.name).isEqualTo("New Name")
            assertThat(success.value.greeting).isEqualTo("New Greeting")
        }

        @Test
        fun `update returns not found when world does not exist`() {
            val result = worldService.update(UpdateWorldParam(id = UUID.randomUUID(), name = "Name"))

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val failure = result as Result.Failure
            assertThat(failure.error).isInstanceOf(WorldError.NotFound::class.java)
        }

        @Test
        fun `update returns error when name is blank`() {
            val world = ClientWorld(UUID.randomUUID(), "World", "Hi!", Instant.now(), Instant.now())
            fakeWorldClient.seed(world)

            val result = worldService.update(UpdateWorldParam(id = world.id, name = ""))

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val failure = result as Result.Failure
            assertThat(failure.error).isInstanceOf(WorldError.Invalid::class.java)
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `delete returns success when world exists`() {
            val world = ClientWorld(UUID.randomUUID(), "World", "Hi!", Instant.now(), Instant.now())
            fakeWorldClient.seed(world)

            val result = worldService.delete(DeleteWorldParam(id = world.id))

            assertThat(result).isInstanceOf(Result.Success::class.java)
        }

        @Test
        fun `delete returns not found when world does not exist`() {
            val result = worldService.delete(DeleteWorldParam(id = UUID.randomUUID()))

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val failure = result as Result.Failure
            assertThat(failure.error).isInstanceOf(WorldError.NotFound::class.java)
        }
    }
}
