package com.acme.services.camperservice.features.user.service

import com.acme.clients.common.Result
import com.acme.clients.userclient.fake.FakeUserClient
import com.acme.services.camperservice.features.user.error.UserError
import com.acme.services.camperservice.features.user.params.AuthenticateUserParam
import com.acme.services.camperservice.features.user.params.CreateUserParam
import com.acme.services.camperservice.features.user.params.GetUserByIdParam
import com.acme.services.camperservice.features.user.params.UpdateUserParam
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class UserServiceTest {

    private val fakeUserClient = FakeUserClient()
    private val userService = UserService(fakeUserClient)

    @BeforeEach
    fun setUp() {
        fakeUserClient.reset()
    }

    @Nested
    inner class GetById {
        @Test
        fun `getById returns user when found`() {
            val created = (userService.create(CreateUserParam(email = "alice@example.com", username = "alice")) as Result.Success).value

            val result = userService.getById(GetUserByIdParam(userId = created.id))

            assertThat(result.isSuccess).isTrue()
            val user = (result as Result.Success).value
            assertThat(user.id).isEqualTo(created.id)
            assertThat(user.email).isEqualTo("alice@example.com")
            assertThat(user.username).isEqualTo("alice")
        }

        @Test
        fun `getById returns NotFound when user does not exist`() {
            val result = userService.getById(GetUserByIdParam(userId = UUID.randomUUID()))

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(UserError.NotFound::class.java)
        }
    }

    @Nested
    inner class Create {
        @Test
        fun `create returns success with username`() {
            val result = userService.create(CreateUserParam(email = "alice@example.com", username = "alice"))

            assertThat(result.isSuccess).isTrue()
            val user = (result as Result.Success).value
            assertThat(user.email).isEqualTo("alice@example.com")
            assertThat(user.username).isEqualTo("alice")
        }

        @Test
        fun `create returns success with null username`() {
            val result = userService.create(CreateUserParam(email = "bob@example.com"))

            assertThat(result.isSuccess).isTrue()
            val user = (result as Result.Success).value
            assertThat(user.email).isEqualTo("bob@example.com")
            assertThat(user.username).isNull()
        }

        @Test
        fun `create returns existing user when email already exists`() {
            val first = (userService.create(CreateUserParam(email = "dup@example.com", username = "first")) as Result.Success).value
            val second = (userService.create(CreateUserParam(email = "dup@example.com", username = "second")) as Result.Success).value

            assertThat(second.id).isEqualTo(first.id)
            assertThat(second.username).isEqualTo("first")
        }

        @Test
        fun `create returns Invalid when email is blank`() {
            val result = userService.create(CreateUserParam(email = ""))

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(UserError.Invalid::class.java)
            assertThat((error as UserError.Invalid).field).isEqualTo("email")
        }
    }

    @Nested
    inner class EmailNormalization {
        @Test
        fun `create normalizes email`() {
            val result = userService.create(CreateUserParam(email = "A.Li.Ce@Example.COM", username = "alice"))

            assertThat(result.isSuccess).isTrue()
            val user = (result as Result.Success).value
            assertThat(user.email).isEqualTo("alice@example.com")
        }

        @Test
        fun `authenticate finds user regardless of case and dots`() {
            userService.create(CreateUserParam(email = "bob@example.com", username = "bob"))

            val result = userService.authenticate(AuthenticateUserParam(email = "B.O.B@Example.COM"))

            assertThat(result.isSuccess).isTrue()
            val user = (result as Result.Success).value
            assertThat(user.username).isEqualTo("bob")
        }

        @Test
        fun `create returns existing user when normalized email matches`() {
            val first = (userService.create(CreateUserParam(email = "carol@example.com", username = "carol")) as Result.Success).value
            val second = (userService.create(CreateUserParam(email = "C.A.R.O.L@Example.COM", username = "different")) as Result.Success).value

            assertThat(second.id).isEqualTo(first.id)
            assertThat(second.username).isEqualTo("carol")
        }
    }

    @Nested
    inner class Authenticate {
        @Test
        fun `authenticate returns user when email exists`() {
            userService.create(CreateUserParam(email = "carol@example.com", username = "carol"))

            val result = userService.authenticate(AuthenticateUserParam(email = "carol@example.com"))

            assertThat(result.isSuccess).isTrue()
            val user = (result as Result.Success).value
            assertThat(user.email).isEqualTo("carol@example.com")
        }

        @Test
        fun `authenticate returns NotFound when email does not exist`() {
            val result = userService.authenticate(AuthenticateUserParam(email = "nobody@example.com"))

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(UserError.NotFound::class.java)
        }

        @Test
        fun `authenticate returns Invalid when email is blank`() {
            val result = userService.authenticate(AuthenticateUserParam(email = ""))

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(UserError.Invalid::class.java)
        }
    }

    @Nested
    inner class Update {
        @Test
        fun `update returns success when user updates themselves`() {
            val created = (userService.create(CreateUserParam(email = "dave@example.com", username = "dave")) as Result.Success).value

            val result = userService.update(UpdateUserParam(userId = created.id, username = "david", requestingUserId = created.id))

            assertThat(result.isSuccess).isTrue()
            val user = (result as Result.Success).value
            assertThat(user.username).isEqualTo("david")
        }

        @Test
        fun `update returns Forbidden when user tries to update another user`() {
            val created = (userService.create(CreateUserParam(email = "eve@example.com", username = "eve")) as Result.Success).value
            val otherId = UUID.randomUUID()

            val result = userService.update(UpdateUserParam(userId = created.id, username = "evelyn", requestingUserId = otherId))

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(UserError.Forbidden::class.java)
        }

        @Test
        fun `update returns NotFound when user does not exist`() {
            val userId = UUID.randomUUID()
            val result = userService.update(UpdateUserParam(userId = userId, username = "nope", requestingUserId = userId))

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(UserError.NotFound::class.java)
        }

        @Test
        fun `update returns Invalid when username is blank`() {
            val created = (userService.create(CreateUserParam(email = "frank@example.com", username = "frank")) as Result.Success).value

            val result = userService.update(UpdateUserParam(userId = created.id, username = "", requestingUserId = created.id))

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(UserError.Invalid::class.java)
        }
    }
}
