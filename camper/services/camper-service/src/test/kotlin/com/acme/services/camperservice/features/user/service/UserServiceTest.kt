package com.acme.services.camperservice.features.user.service

import com.acme.clients.common.Result
import com.acme.clients.userclient.fake.FakeUserClient
import com.acme.clients.userclient.model.User as ClientUser
import com.acme.libs.avatargenerator.AvatarGenerator
import com.acme.services.camperservice.features.user.error.UserError
import com.acme.services.camperservice.features.user.params.AuthenticateUserParam
import com.acme.services.camperservice.features.user.params.CreateUserParam
import com.acme.services.camperservice.features.user.params.GetAvatarParam
import com.acme.services.camperservice.features.user.params.GetUserByIdParam
import com.acme.services.camperservice.features.user.params.RandomizeAvatarParam
import com.acme.services.camperservice.features.user.params.UpdateUserParam
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
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
        fun `create updates username when existing user has no username`() {
            val first = (userService.create(CreateUserParam(email = "invited@example.com")) as Result.Success).value
            assertThat(first.username).isNull()

            val second = (userService.create(CreateUserParam(email = "invited@example.com", username = "now-registered")) as Result.Success).value

            assertThat(second.id).isEqualTo(first.id)
            assertThat(second.username).isEqualTo("now-registered")
        }

        @Test
        fun `create does not update username when existing user already has one`() {
            val first = (userService.create(CreateUserParam(email = "named@example.com", username = "original")) as Result.Success).value

            val second = (userService.create(CreateUserParam(email = "named@example.com", username = "different")) as Result.Success).value

            assertThat(second.id).isEqualTo(first.id)
            assertThat(second.username).isEqualTo("original")
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

        @Test
        fun `authenticate returns RegistrationRequired when user has no username`() {
            userService.create(CreateUserParam(email = "noname@example.com"))

            val result = userService.authenticate(AuthenticateUserParam(email = "noname@example.com"))

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(UserError.RegistrationRequired::class.java)
            assertThat((error as UserError.RegistrationRequired).email).isEqualTo("noname@example.com")
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

        @Test
        fun `update sets dietary restrictions`() {
            val created = (userService.create(CreateUserParam(email = "grace@example.com", username = "grace")) as Result.Success).value

            val result = userService.update(UpdateUserParam(
                userId = created.id,
                username = "grace",
                dietaryRestrictions = listOf("vegetarian", "gluten_free"),
                requestingUserId = created.id
            ))

            assertThat(result.isSuccess).isTrue()
            val user = (result as Result.Success).value
            assertThat(user.dietaryRestrictions).containsExactlyInAnyOrder("vegetarian", "gluten_free")
        }

        @Test
        fun `update sets experience level`() {
            val created = (userService.create(CreateUserParam(email = "hank@example.com", username = "hank")) as Result.Success).value

            val result = userService.update(UpdateUserParam(
                userId = created.id,
                username = "hank",
                experienceLevel = "intermediate",
                requestingUserId = created.id
            ))

            assertThat(result.isSuccess).isTrue()
            val user = (result as Result.Success).value
            assertThat(user.experienceLevel).isEqualTo("intermediate")
        }

        @Test
        fun `update sets profileCompleted`() {
            val created = (userService.create(CreateUserParam(email = "iris@example.com", username = "iris")) as Result.Success).value

            val result = userService.update(UpdateUserParam(
                userId = created.id,
                username = "iris",
                profileCompleted = true,
                requestingUserId = created.id
            ))

            assertThat(result.isSuccess).isTrue()
            val user = (result as Result.Success).value
            assertThat(user.profileCompleted).isTrue()
        }
    }

    @Nested
    inner class Avatar {
        @Test
        fun `create generates avatar seed from username`() {
            val result = userService.create(CreateUserParam(email = "avatar@example.com", username = "trailblazer"))

            assertThat(result.isSuccess).isTrue()
            val user = (result as Result.Success).value
            assertThat(user.avatarSeed).isEqualTo(AvatarGenerator.seedFromName("trailblazer"))
        }

        @Test
        fun `create generates avatar seed from email when no username`() {
            val result = userService.create(CreateUserParam(email = "nousername@example.com"))

            assertThat(result.isSuccess).isTrue()
            val user = (result as Result.Success).value
            assertThat(user.avatarSeed).isEqualTo(AvatarGenerator.seedFromName("nousername@example.com"))
        }

        @Test
        fun `create generates avatar seed for invite-flow user on registration`() {
            val userId = UUID.randomUUID()
            val now = Instant.now()
            fakeUserClient.seed(ClientUser(
                id = userId,
                email = "invited@example.com",
                username = null,
                avatarSeed = null,
                createdAt = now,
                updatedAt = now
            ))

            val username = "now-registered"
            val result = userService.create(CreateUserParam(email = "invited@example.com", username = username))

            assertThat(result.isSuccess).isTrue()
            val user = (result as Result.Success).value
            assertThat(user.avatarSeed).isEqualTo(AvatarGenerator.seedFromName(username))
        }

        @Test
        fun `getAvatar returns avatar for user with seed`() {
            val username = "campergirl"
            val created = (userService.create(CreateUserParam(email = "campergirl@example.com", username = username)) as Result.Success).value

            val result = userService.getAvatar(GetAvatarParam(userId = created.id))

            assertThat(result.isSuccess).isTrue()
            val avatar = (result as Result.Success).value
            val expectedAvatar = AvatarGenerator.generate(AvatarGenerator.seedFromName(username))
            assertThat(avatar.hairStyle).isEqualTo(expectedAvatar.hairStyle.name.lowercase())
            assertThat(avatar.hairColor).isEqualTo(expectedAvatar.hairColor.name.lowercase())
            assertThat(avatar.skinColor).isEqualTo(expectedAvatar.skinColor.name.lowercase())
            assertThat(avatar.clothingStyle).isEqualTo(expectedAvatar.clothingStyle.name.lowercase())
            assertThat(avatar.pantsColor).isEqualTo(expectedAvatar.pantsColor.name.lowercase())
            assertThat(avatar.shirtColor).isEqualTo(expectedAvatar.shirtColor.name.lowercase())
        }

        @Test
        fun `getAvatar returns Invalid when user has no avatar seed`() {
            val userId = UUID.randomUUID()
            val now = Instant.now()
            fakeUserClient.seed(ClientUser(
                id = userId,
                email = "noseed@example.com",
                username = "noseed",
                avatarSeed = null,
                createdAt = now,
                updatedAt = now
            ))

            val result = userService.getAvatar(GetAvatarParam(userId = userId))

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(UserError.Invalid::class.java)
        }

        @Test
        fun `getAvatar returns NotFound when user does not exist`() {
            val result = userService.getAvatar(GetAvatarParam(userId = UUID.randomUUID()))

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(UserError.NotFound::class.java)
        }

        @Test
        fun `randomizeAvatar updates avatar seed`() {
            val created = (userService.create(CreateUserParam(email = "random@example.com", username = "randomizer")) as Result.Success).value
            val originalSeed = created.avatarSeed

            val result = userService.randomizeAvatar(RandomizeAvatarParam(userId = created.id, requestingUserId = created.id))

            assertThat(result.isSuccess).isTrue()
            val preview = (result as Result.Success).value
            assertThat(preview.seed).isNotNull()
            assertThat(preview.seed).isNotEqualTo(originalSeed)
        }

        @Test
        fun `randomizeAvatar returns Forbidden when requesting user differs`() {
            val created = (userService.create(CreateUserParam(email = "owned@example.com", username = "owner")) as Result.Success).value
            val otherId = UUID.randomUUID()

            val result = userService.randomizeAvatar(RandomizeAvatarParam(userId = created.id, requestingUserId = otherId))

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(UserError.Forbidden::class.java)
        }
    }
}
