package com.acme.services.camperservice.features.user.controller

import com.acme.services.camperservice.common.error.toResponseEntity
import com.acme.services.camperservice.features.user.dto.AuthRequest
import com.acme.services.camperservice.features.user.dto.CreateUserRequest
import com.acme.services.camperservice.features.user.dto.UpdateUserRequest
import com.acme.services.camperservice.features.user.mapper.UserMapper
import com.acme.services.camperservice.features.user.params.*
import com.acme.services.camperservice.features.user.service.UserService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
class UserController(private val userService: UserService) {
    private val logger = LoggerFactory.getLogger(UserController::class.java)

    @GetMapping("/api/users/{userId}")
    fun getById(@PathVariable userId: UUID): ResponseEntity<Any> {
        logger.info("GET /api/users/{}", userId)
        val param = GetUserByIdParam(userId = userId)
        return userService.getById(param).toResponseEntity { UserMapper.toResponse(it) }
    }

    @PostMapping("/api/users")
    fun create(@RequestBody request: CreateUserRequest): ResponseEntity<Any> {
        logger.info("POST /api/users")
        val param = CreateUserParam(email = request.email, username = request.username)
        return userService.create(param).toResponseEntity(successStatus = 201) { UserMapper.toResponse(it) }
    }

    @PostMapping("/api/auth")
    fun authenticate(@RequestBody request: AuthRequest): ResponseEntity<Any> {
        logger.info("POST /api/auth")
        val param = AuthenticateUserParam(email = request.email)
        return userService.authenticate(param).toResponseEntity { UserMapper.toAuthResponse(it) }
    }

    @PutMapping("/api/users/{userId}")
    fun update(
        @PathVariable userId: UUID,
        @RequestHeader("X-User-Id") requestingUserId: UUID,
        @RequestBody request: UpdateUserRequest
    ): ResponseEntity<Any> {
        logger.info("PUT /api/users/{}", userId)
        val param = UpdateUserParam(
            userId = userId,
            username = request.username,
            experienceLevel = request.experienceLevel,
            dietaryRestrictions = request.dietaryRestrictions,
            profileCompleted = request.profileCompleted,
            avatarSeed = request.avatarSeed,
            requestingUserId = requestingUserId
        )
        return userService.update(param).toResponseEntity { UserMapper.toResponse(it) }
    }

    @PostMapping("/api/users/{userId}/randomize-avatar")
    fun randomizeAvatar(
        @PathVariable userId: UUID,
        @RequestHeader("X-User-Id") requestingUserId: UUID
    ): ResponseEntity<Any> {
        logger.info("POST /api/users/{}/randomize-avatar", userId)
        val param = RandomizeAvatarParam(userId = userId, requestingUserId = requestingUserId)
        return userService.randomizeAvatar(param).toResponseEntity { it }
    }

    @GetMapping("/api/users/{userId}/avatar")
    fun getAvatar(@PathVariable userId: UUID): ResponseEntity<Any> {
        logger.info("GET /api/users/{}/avatar", userId)
        val param = GetAvatarParam(userId = userId)
        return userService.getAvatar(param).toResponseEntity { it }
    }
}
