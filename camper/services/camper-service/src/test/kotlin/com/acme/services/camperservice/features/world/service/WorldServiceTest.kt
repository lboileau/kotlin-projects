package com.acme.services.camperservice.features.world.service

import com.acme.clients.common.Result
import com.acme.clients.worldclient.fake.FakeWorldClient
import com.acme.services.camperservice.features.world.error.WorldError
import com.acme.services.camperservice.features.world.params.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class WorldServiceTest {

    private val fakeWorldClient = FakeWorldClient()
    private val worldService = WorldService(fakeWorldClient)

    @BeforeEach
    fun setUp() {
        fakeWorldClient.reset()
    }

    @Nested
    inner class Create {

        @Test
        fun `create returns success when valid`() {
            val param = CreateWorldParam(name = "Earth", greeting = "Hello")
            val result = worldService.create(param)

            assertThat(result.isSuccess).isTrue()
            val world = (result as Result.Success).value
            assertThat(world.name).isEqualTo("Earth")
            assertThat(world.greeting).isEqualTo("Hello")
        }

        @Test
        fun `create returns Invalid when name is blank`() {
            val param = CreateWorldParam(name = "", greeting = "Hello")
            val result = worldService.create(param)

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(WorldError.Invalid::class.java)
            assertThat((error as WorldError.Invalid).field).isEqualTo("name")
        }

        @Test
        fun `create returns Invalid when greeting is blank`() {
            val param = CreateWorldParam(name = "Earth", greeting = "")
            val result = worldService.create(param)

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(WorldError.Invalid::class.java)
            assertThat((error as WorldError.Invalid).field).isEqualTo("greeting")
        }

        @Test
        fun `create returns AlreadyExists when name is duplicate`() {
            worldService.create(CreateWorldParam(name = "Earth", greeting = "Hello"))
            val result = worldService.create(CreateWorldParam(name = "Earth", greeting = "Hi"))

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(WorldError.AlreadyExists::class.java)
        }
    }

    @Nested
    inner class GetById {

        @Test
        fun `getById returns success when world exists`() {
            val created = (worldService.create(CreateWorldParam(name = "Earth", greeting = "Hello")) as Result.Success).value
            val result = worldService.getById(GetWorldByIdParam(id = created.id))

            assertThat(result.isSuccess).isTrue()
            val world = (result as Result.Success).value
            assertThat(world.name).isEqualTo("Earth")
        }

        @Test
        fun `getById returns NotFound when world does not exist`() {
            val result = worldService.getById(GetWorldByIdParam(id = UUID.randomUUID()))

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(WorldError.NotFound::class.java)
        }
    }

    @Nested
    inner class GetAll {

        @Test
        fun `getAll returns empty list when no worlds exist`() {
            val result = worldService.getAll(GetAllWorldsParam())

            assertThat(result.isSuccess).isTrue()
            val worlds = (result as Result.Success).value
            assertThat(worlds).isEmpty()
        }

        @Test
        fun `getAll returns all worlds`() {
            worldService.create(CreateWorldParam(name = "Alpha", greeting = "Hi"))
            worldService.create(CreateWorldParam(name = "Beta", greeting = "Hey"))

            val result = worldService.getAll(GetAllWorldsParam())

            assertThat(result.isSuccess).isTrue()
            val worlds = (result as Result.Success).value
            assertThat(worlds).hasSize(2)
        }
    }

    @Nested
    inner class Update {

        @Test
        fun `update returns success when valid`() {
            val created = (worldService.create(CreateWorldParam(name = "Earth", greeting = "Hello")) as Result.Success).value
            val result = worldService.update(UpdateWorldParam(id = created.id, name = "Terra"))

            assertThat(result.isSuccess).isTrue()
            val world = (result as Result.Success).value
            assertThat(world.name).isEqualTo("Terra")
            assertThat(world.greeting).isEqualTo("Hello")
        }

        @Test
        fun `update returns NotFound when world does not exist`() {
            val result = worldService.update(UpdateWorldParam(id = UUID.randomUUID(), name = "Terra"))

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(WorldError.NotFound::class.java)
        }

        @Test
        fun `update returns Invalid when name is blank`() {
            val created = (worldService.create(CreateWorldParam(name = "Earth", greeting = "Hello")) as Result.Success).value
            val result = worldService.update(UpdateWorldParam(id = created.id, name = ""))

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(WorldError.Invalid::class.java)
        }
    }

    @Nested
    inner class Delete {

        @Test
        fun `delete returns success when world exists`() {
            val created = (worldService.create(CreateWorldParam(name = "Earth", greeting = "Hello")) as Result.Success).value
            val result = worldService.delete(DeleteWorldParam(id = created.id))

            assertThat(result.isSuccess).isTrue()
        }

        @Test
        fun `delete returns NotFound when world does not exist`() {
            val result = worldService.delete(DeleteWorldParam(id = UUID.randomUUID()))

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(WorldError.NotFound::class.java)
        }
    }
}
