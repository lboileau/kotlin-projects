package com.acme.services.camperservice.features.user.acceptance

import com.acme.services.camperservice.config.TestContainerConfig
import com.acme.services.camperservice.features.plan.dto.PlanMemberResponse
import com.acme.services.camperservice.features.user.acceptance.fixture.UserFixture
import com.acme.services.camperservice.features.user.dto.AuthRequest
import com.acme.services.camperservice.features.user.dto.AuthResponse
import com.acme.services.camperservice.features.user.dto.AvatarPreviewResponse
import com.acme.services.camperservice.features.user.dto.AvatarResponse
import com.acme.services.camperservice.features.user.dto.CreateUserRequest
import com.acme.services.camperservice.features.user.dto.UpdateUserRequest
import com.acme.services.camperservice.features.user.dto.UserResponse
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
class UserAcceptanceTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    private lateinit var fixture: UserFixture

    @BeforeEach
    fun setUp() {
        fixture = UserFixture(jdbcTemplate)
        fixture.truncateAll()
    }

    @Nested
    inner class GetUser {

        @Test
        fun `GET returns 200 with user when found`() {
            val userId = fixture.insertUser(email = "alice@example.com", username = "alice")

            val response = restTemplate.getForEntity("/api/users/$userId", UserResponse::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.id).isEqualTo(userId)
            assertThat(response.body!!.email).isEqualTo("alice@example.com")
            assertThat(response.body!!.username).isEqualTo("alice")
        }

        @Test
        fun `GET returns 200 with profile fields when set`() {
            val userId = fixture.insertUser(
                email = "profile@example.com",
                username = "profile-user",
                experienceLevel = "intermediate",
                avatarSeed = "abc123",
                profileCompleted = true
            )
            fixture.insertDietaryRestriction(userId, "vegetarian")
            fixture.insertDietaryRestriction(userId, "gluten_free")

            val response = restTemplate.getForEntity("/api/users/$userId", UserResponse::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.experienceLevel).isEqualTo("intermediate")
            assertThat(response.body!!.avatarSeed).isEqualTo("abc123")
            assertThat(response.body!!.profileCompleted).isTrue()
            assertThat(response.body!!.dietaryRestrictions).containsExactlyInAnyOrder("vegetarian", "gluten_free")
            assertThat(response.body!!.avatar).isNotNull
            assertThat(response.body!!.avatar!!.hairStyle).isNotBlank()
            assertThat(response.body!!.avatar!!.hairColor).isNotBlank()
            assertThat(response.body!!.avatar!!.skinColor).isNotBlank()
            assertThat(response.body!!.avatar!!.clothingStyle).isNotBlank()
            assertThat(response.body!!.avatar!!.pantsColor).isNotBlank()
            assertThat(response.body!!.avatar!!.shirtColor).isNotBlank()
        }

        @Test
        fun `GET returns 200 with default profile fields when not set`() {
            val userId = fixture.insertUser(email = "default@example.com", username = "default-user")

            val response = restTemplate.getForEntity("/api/users/$userId", UserResponse::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.experienceLevel).isNull()
            assertThat(response.body!!.avatarSeed).isNull()
            assertThat(response.body!!.profileCompleted).isFalse()
            assertThat(response.body!!.dietaryRestrictions).isEmpty()
            assertThat(response.body!!.avatar).isNull()
        }

        @Test
        fun `GET returns 404 when user does not exist`() {
            val response = restTemplate.getForEntity("/api/users/${UUID.randomUUID()}", Map::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @Nested
    inner class CreateUser {

        @Test
        fun `POST returns 201 with created user`() {
            val request = CreateUserRequest(email = "alice@example.com", username = "alice")
            val response = restTemplate.postForEntity("/api/users", request, UserResponse::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            assertThat(response.body!!.email).isEqualTo("alice@example.com")
            assertThat(response.body!!.username).isEqualTo("alice")
            assertThat(response.body!!.id).isNotNull()
        }

        @Test
        fun `POST generates avatarSeed on creation`() {
            val request = CreateUserRequest(email = "seed-test@example.com", username = "seeduser")
            val response = restTemplate.postForEntity("/api/users", request, UserResponse::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            assertThat(response.body!!.avatarSeed).isNotNull()
            assertThat(response.body!!.avatarSeed).isNotBlank()
            assertThat(response.body!!.avatar).isNotNull
        }

        @Test
        fun `POST returns 201 with null username`() {
            val request = CreateUserRequest(email = "bob@example.com")
            val response = restTemplate.postForEntity("/api/users", request, UserResponse::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            assertThat(response.body!!.email).isEqualTo("bob@example.com")
            assertThat(response.body!!.username).isNull()
        }

        @Test
        fun `POST returns existing user when email already exists`() {
            val userId = fixture.insertUser(email = "existing@example.com", username = "existing")

            val request = CreateUserRequest(email = "existing@example.com", username = "different")
            val response = restTemplate.postForEntity("/api/users", request, UserResponse::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            assertThat(response.body!!.id).isEqualTo(userId)
            assertThat(response.body!!.username).isEqualTo("existing")
        }

        @Test
        fun `POST updates username when existing user has no username`() {
            fixture.insertUser(email = "invited@example.com", username = null)

            val request = CreateUserRequest(email = "invited@example.com", username = "now-registered")
            val response = restTemplate.postForEntity("/api/users", request, UserResponse::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            assertThat(response.body!!.username).isEqualTo("now-registered")
            assertThat(response.body!!.avatarSeed).isNotNull()
        }

        @Test
        fun `POST returns 400 when email is blank`() {
            val request = CreateUserRequest(email = "")
            val response = restTemplate.postForEntity("/api/users", request, Map::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @Nested
    inner class EmailNormalization {

        @Test
        fun `POST normalizes email on creation`() {
            val request = CreateUserRequest(email = "A.Li.Ce@Example.COM", username = "alice")
            val response = restTemplate.postForEntity("/api/users", request, UserResponse::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            assertThat(response.body!!.email).isEqualTo("alice@example.com")
        }

        @Test
        fun `POST auth finds user regardless of case and dots`() {
            restTemplate.postForEntity("/api/users", CreateUserRequest(email = "bob@example.com", username = "bob"), UserResponse::class.java)

            val authRequest = AuthRequest(email = "B.O.B@Example.COM")
            val response = restTemplate.postForEntity("/api/auth", authRequest, AuthResponse::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.username).isEqualTo("bob")
        }

        @Test
        fun `POST returns existing user when normalized email matches`() {
            val first = restTemplate.postForEntity("/api/users", CreateUserRequest(email = "carol@example.com", username = "carol"), UserResponse::class.java)

            val second = restTemplate.postForEntity("/api/users", CreateUserRequest(email = "C.A.R.O.L@Example.COM", username = "different"), UserResponse::class.java)

            assertThat(second.statusCode).isEqualTo(HttpStatus.CREATED)
            assertThat(second.body!!.id).isEqualTo(first.body!!.id)
            assertThat(second.body!!.username).isEqualTo("carol")
        }
    }

    @Nested
    inner class Authenticate {

        @Test
        fun `POST returns 200 with auth response when user exists`() {
            fixture.insertUser(email = "carol@example.com", username = "carol")

            val request = AuthRequest(email = "carol@example.com")
            val response = restTemplate.postForEntity("/api/auth", request, AuthResponse::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.email).isEqualTo("carol@example.com")
            assertThat(response.body!!.username).isEqualTo("carol")
        }

        @Test
        fun `POST returns auth response with profile fields`() {
            val userId = fixture.insertUser(
                email = "auth-profile@example.com",
                username = "auth-user",
                avatarSeed = "seed123",
                profileCompleted = true
            )

            val request = AuthRequest(email = "auth-profile@example.com")
            val response = restTemplate.postForEntity("/api/auth", request, AuthResponse::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.avatarSeed).isEqualTo("seed123")
            assertThat(response.body!!.profileCompleted).isTrue()
            assertThat(response.body!!.avatar).isNotNull
        }

        @Test
        fun `POST returns auth response with profileCompleted false by default`() {
            fixture.insertUser(email = "new-auth@example.com", username = "new-auth")

            val request = AuthRequest(email = "new-auth@example.com")
            val response = restTemplate.postForEntity("/api/auth", request, AuthResponse::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.profileCompleted).isFalse()
            assertThat(response.body!!.avatarSeed).isNull()
            assertThat(response.body!!.avatar).isNull()
        }

        @Test
        fun `POST returns 404 when user does not exist`() {
            val request = AuthRequest(email = "nobody@example.com")
            val response = restTemplate.postForEntity("/api/auth", request, Map::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @Test
        fun `POST returns 400 when email is blank`() {
            val request = AuthRequest(email = "")
            val response = restTemplate.postForEntity("/api/auth", request, Map::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        fun `POST returns 403 when user has no username`() {
            fixture.insertUser(email = "noname@example.com", username = null)

            val request = AuthRequest(email = "noname@example.com")
            val response = restTemplate.postForEntity("/api/auth", request, Map::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        }
    }

    @Nested
    inner class UpdateUser {

        @Test
        fun `PUT returns 200 with updated user`() {
            val userId = fixture.insertUser(email = "dave@example.com", username = "dave")

            val response = restTemplate.exchange(
                "/api/users/$userId",
                HttpMethod.PUT,
                jsonEntityWithUserId(UpdateUserRequest(username = "david"), userId),
                UserResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.username).isEqualTo("david")
            assertThat(response.body!!.email).isEqualTo("dave@example.com")
        }

        @Test
        fun `PUT updates experienceLevel and returns it in response`() {
            val userId = fixture.insertUser(email = "exp@example.com", username = "exp-user")

            val response = restTemplate.exchange(
                "/api/users/$userId",
                HttpMethod.PUT,
                jsonEntityWithUserId(
                    UpdateUserRequest(username = "exp-user", experienceLevel = "advanced"),
                    userId
                ),
                UserResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.experienceLevel).isEqualTo("advanced")
        }

        @Test
        fun `PUT returns 400 for invalid experienceLevel`() {
            val userId = fixture.insertUser(email = "bad-exp@example.com", username = "bad-exp")

            val response = restTemplate.exchange(
                "/api/users/$userId",
                HttpMethod.PUT,
                jsonEntityWithUserId(
                    UpdateUserRequest(username = "bad-exp", experienceLevel = "godlike"),
                    userId
                ),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        fun `PUT updates dietaryRestrictions and returns them in response`() {
            val userId = fixture.insertUser(email = "diet@example.com", username = "diet-user")

            val response = restTemplate.exchange(
                "/api/users/$userId",
                HttpMethod.PUT,
                jsonEntityWithUserId(
                    UpdateUserRequest(
                        username = "diet-user",
                        dietaryRestrictions = listOf("vegetarian", "gluten_free")
                    ),
                    userId
                ),
                UserResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.dietaryRestrictions).containsExactlyInAnyOrder("vegetarian", "gluten_free")
        }

        @Test
        fun `PUT clears dietary restrictions with empty list`() {
            val userId = fixture.insertUser(email = "clear-diet@example.com", username = "clear-diet")
            fixture.insertDietaryRestriction(userId, "vegetarian")

            val response = restTemplate.exchange(
                "/api/users/$userId",
                HttpMethod.PUT,
                jsonEntityWithUserId(
                    UpdateUserRequest(username = "clear-diet", dietaryRestrictions = emptyList()),
                    userId
                ),
                UserResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.dietaryRestrictions).isEmpty()
        }

        @Test
        fun `PUT with null dietary restrictions preserves existing`() {
            val userId = fixture.insertUser(email = "keep-diet@example.com", username = "keep-diet")
            fixture.insertDietaryRestriction(userId, "vegetarian")

            val response = restTemplate.exchange(
                "/api/users/$userId",
                HttpMethod.PUT,
                jsonEntityWithUserId(
                    UpdateUserRequest(username = "keep-diet"),
                    userId
                ),
                UserResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.dietaryRestrictions).containsExactly("vegetarian")
        }

        @Test
        fun `PUT returns 400 for invalid dietary restriction`() {
            val userId = fixture.insertUser(email = "bad-diet@example.com", username = "bad-diet")

            val response = restTemplate.exchange(
                "/api/users/$userId",
                HttpMethod.PUT,
                jsonEntityWithUserId(
                    UpdateUserRequest(
                        username = "bad-diet",
                        dietaryRestrictions = listOf("vegetarian", "paleo")
                    ),
                    userId
                ),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        fun `PUT sets profileCompleted to true`() {
            val userId = fixture.insertUser(email = "complete@example.com", username = "complete")

            val response = restTemplate.exchange(
                "/api/users/$userId",
                HttpMethod.PUT,
                jsonEntityWithUserId(
                    UpdateUserRequest(username = "complete", profileCompleted = true),
                    userId
                ),
                UserResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.profileCompleted).isTrue()
        }

        @Test
        fun `PUT profileCompleted is one-way - cannot go back to false`() {
            val userId = fixture.insertUser(
                email = "oneway@example.com",
                username = "oneway",
                profileCompleted = true
            )

            val response = restTemplate.exchange(
                "/api/users/$userId",
                HttpMethod.PUT,
                jsonEntityWithUserId(
                    UpdateUserRequest(username = "oneway", profileCompleted = false),
                    userId
                ),
                UserResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.profileCompleted).isTrue()
        }

        @Test
        fun `PUT returns 403 when requesting user is different from target`() {
            val userId = fixture.insertUser(email = "eve@example.com", username = "eve")
            val otherUserId = UUID.randomUUID()

            val response = restTemplate.exchange(
                "/api/users/$userId",
                HttpMethod.PUT,
                jsonEntityWithUserId(UpdateUserRequest(username = "evelyn"), otherUserId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        }

        @Test
        fun `PUT returns 400 when username is blank`() {
            val userId = fixture.insertUser(email = "frank@example.com", username = "frank")

            val response = restTemplate.exchange(
                "/api/users/$userId",
                HttpMethod.PUT,
                jsonEntityWithUserId(UpdateUserRequest(username = ""), userId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @Nested
    inner class RandomizeAvatar {

        @Test
        fun `POST returns 200 with user with new avatar seed`() {
            val userId = fixture.insertUser(
                email = "randomize@example.com",
                username = "randomize",
                avatarSeed = "old-seed"
            )

            val response = restTemplate.exchange(
                "/api/users/$userId/randomize-avatar",
                HttpMethod.POST,
                jsonEntityWithUserId(null, userId),
                AvatarPreviewResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.seed).isNotNull()
            assertThat(response.body!!.seed).isNotEqualTo("old-seed")
            assertThat(response.body!!.avatar).isNotNull
        }

        @Test
        fun `POST returns 403 when requesting user is different from target`() {
            val userId = fixture.insertUser(email = "target@example.com", username = "target")
            val otherUserId = fixture.insertUser(email = "other@example.com", username = "other")

            val response = restTemplate.exchange(
                "/api/users/$userId/randomize-avatar",
                HttpMethod.POST,
                jsonEntityWithUserId(null, otherUserId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        }

        @Test
        fun `POST returns 404 when user does not exist`() {
            val nonexistentId = UUID.randomUUID()

            val response = restTemplate.exchange(
                "/api/users/$nonexistentId/randomize-avatar",
                HttpMethod.POST,
                jsonEntityWithUserId(null, nonexistentId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @Nested
    inner class GetAvatar {

        @Test
        fun `GET returns 200 with all avatar fields`() {
            val userId = fixture.insertUser(
                email = "avatar@example.com",
                username = "avatar-user",
                avatarSeed = "test-seed-123"
            )

            val response = restTemplate.getForEntity("/api/users/$userId/avatar", AvatarResponse::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.hairStyle).isNotBlank()
            assertThat(response.body!!.hairColor).isNotBlank()
            assertThat(response.body!!.skinColor).isNotBlank()
            assertThat(response.body!!.clothingStyle).isNotBlank()
            assertThat(response.body!!.pantsColor).isNotBlank()
            assertThat(response.body!!.shirtColor).isNotBlank()
        }

        @Test
        fun `GET returns deterministic avatar for same seed`() {
            val userId = fixture.insertUser(
                email = "deterministic@example.com",
                username = "deterministic",
                avatarSeed = "fixed-seed"
            )

            val response1 = restTemplate.getForEntity("/api/users/$userId/avatar", AvatarResponse::class.java)
            val response2 = restTemplate.getForEntity("/api/users/$userId/avatar", AvatarResponse::class.java)

            assertThat(response1.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response2.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response1.body).isEqualTo(response2.body)
        }

        @Test
        fun `GET returns 400 when user has no avatar seed`() {
            val userId = fixture.insertUser(
                email = "no-seed@example.com",
                username = "no-seed",
                avatarSeed = null
            )

            val response = restTemplate.getForEntity("/api/users/$userId/avatar", Map::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        fun `GET returns 404 when user does not exist`() {
            val response = restTemplate.getForEntity("/api/users/${UUID.randomUUID()}/avatar", Map::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @Nested
    inner class PlanMemberAvatar {

        @Test
        fun `GET plan members includes avatarSeed and avatar for member with seed`() {
            val userId = fixture.insertUser(
                email = "member-avatar@example.com",
                username = "member-avatar",
                avatarSeed = "member-seed-123"
            )
            val planId = fixture.insertPlan(name = "Avatar Trip", ownerId = userId)
            fixture.insertPlanMember(planId = planId, userId = userId)

            val response = restTemplate.exchange(
                "/api/plans/$planId/members",
                HttpMethod.GET,
                jsonEntityWithUserId(null, userId),
                Array<PlanMemberResponse>::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body).hasSize(1)
            val member = response.body!![0]
            assertThat(member.avatarSeed).isEqualTo("member-seed-123")
            assertThat(member.avatar).isNotNull
            assertThat(member.avatar!!.hairStyle).isNotBlank()
            assertThat(member.avatar!!.hairColor).isNotBlank()
            assertThat(member.avatar!!.skinColor).isNotBlank()
            assertThat(member.avatar!!.clothingStyle).isNotBlank()
            assertThat(member.avatar!!.pantsColor).isNotBlank()
            assertThat(member.avatar!!.shirtColor).isNotBlank()
        }

        @Test
        fun `GET plan members returns null avatar for member without seed`() {
            val userId = fixture.insertUser(
                email = "no-avatar@example.com",
                username = "no-avatar",
                avatarSeed = null
            )
            val planId = fixture.insertPlan(name = "No Avatar Trip", ownerId = userId)
            fixture.insertPlanMember(planId = planId, userId = userId)

            val response = restTemplate.exchange(
                "/api/plans/$planId/members",
                HttpMethod.GET,
                jsonEntityWithUserId(null, userId),
                Array<PlanMemberResponse>::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body).hasSize(1)
            val member = response.body!![0]
            assertThat(member.avatarSeed).isNull()
            assertThat(member.avatar).isNull()
        }
    }

    @Nested
    inner class ReadYourOwnWrites {

        @Test
        fun `POST user then GET returns the created user`() {
            val createRequest = CreateUserRequest(email = "getme@example.com", username = "getme")
            val createResponse = restTemplate.postForEntity("/api/users", createRequest, UserResponse::class.java)
            assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
            val userId = createResponse.body!!.id

            val getResponse = restTemplate.getForEntity("/api/users/$userId", UserResponse::class.java)
            assertThat(getResponse.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(getResponse.body!!.email).isEqualTo("getme@example.com")
            assertThat(getResponse.body!!.username).isEqualTo("getme")
        }

        @Test
        fun `POST user then POST auth returns the created user`() {
            val createRequest = CreateUserRequest(email = "ryw@example.com", username = "ryw")
            val createResponse = restTemplate.postForEntity("/api/users", createRequest, UserResponse::class.java)
            assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)

            val authRequest = AuthRequest(email = "ryw@example.com")
            val authResponse = restTemplate.postForEntity("/api/auth", authRequest, AuthResponse::class.java)
            assertThat(authResponse.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(authResponse.body!!.email).isEqualTo("ryw@example.com")
            assertThat(authResponse.body!!.id).isEqualTo(createResponse.body!!.id)
        }

        @Test
        fun `POST user then PUT update then POST auth returns updated username`() {
            val createResponse = restTemplate.postForEntity(
                "/api/users",
                CreateUserRequest(email = "update-flow@example.com", username = "old"),
                UserResponse::class.java
            )
            assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
            val userId = createResponse.body!!.id

            val updateResponse = restTemplate.exchange(
                "/api/users/$userId",
                HttpMethod.PUT,
                jsonEntityWithUserId(UpdateUserRequest(username = "new"), userId),
                UserResponse::class.java
            )
            assertThat(updateResponse.statusCode).isEqualTo(HttpStatus.OK)

            val authResponse = restTemplate.postForEntity(
                "/api/auth",
                AuthRequest(email = "update-flow@example.com"),
                AuthResponse::class.java
            )
            assertThat(authResponse.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(authResponse.body!!.username).isEqualTo("new")
        }

        @Test
        fun `POST user then PUT with profile fields then GET returns all profile fields`() {
            val createResponse = restTemplate.postForEntity(
                "/api/users",
                CreateUserRequest(email = "profile-ryw@example.com", username = "profile-ryw"),
                UserResponse::class.java
            )
            assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
            val userId = createResponse.body!!.id

            val updateResponse = restTemplate.exchange(
                "/api/users/$userId",
                HttpMethod.PUT,
                jsonEntityWithUserId(
                    UpdateUserRequest(
                        username = "profile-ryw",
                        experienceLevel = "expert",
                        dietaryRestrictions = listOf("vegan", "nut_allergy"),
                        profileCompleted = true
                    ),
                    userId
                ),
                UserResponse::class.java
            )
            assertThat(updateResponse.statusCode).isEqualTo(HttpStatus.OK)

            val getResponse = restTemplate.getForEntity("/api/users/$userId", UserResponse::class.java)
            assertThat(getResponse.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(getResponse.body!!.username).isEqualTo("profile-ryw")
            assertThat(getResponse.body!!.experienceLevel).isEqualTo("expert")
            assertThat(getResponse.body!!.dietaryRestrictions).containsExactlyInAnyOrder("vegan", "nut_allergy")
            assertThat(getResponse.body!!.profileCompleted).isTrue()
            assertThat(getResponse.body!!.avatarSeed).isNotNull()
            assertThat(getResponse.body!!.avatar).isNotNull
        }
    }

    private fun jsonEntityWithUserId(body: Any?, userId: UUID): HttpEntity<Any?> {
        val headers = HttpHeaders()
        headers.set("X-User-Id", userId.toString())
        return HttpEntity(body, headers)
    }
}
