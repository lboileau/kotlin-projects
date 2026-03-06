package com.acme.services.camperservice.features.user.controller

import com.acme.services.camperservice.features.user.dto.AuthRequest
import com.acme.services.camperservice.features.user.dto.CreateUserRequest
import com.acme.services.camperservice.features.user.dto.UpdateUserRequest
import com.acme.services.camperservice.features.user.params.GetUserByIdParam
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
        return ResponseEntity.status(501).body("Not implemented")
    }

    @PostMapping("/api/users")
    fun create(@RequestBody request: CreateUserRequest): ResponseEntity<Any> {
        logger.info("POST /api/users")
        return ResponseEntity.status(501).body("Not implemented")
    }

    @PostMapping("/api/auth")
    fun authenticate(@RequestBody request: AuthRequest): ResponseEntity<Any> {
        logger.info("POST /api/auth")
        return ResponseEntity.status(501).body("Not implemented")
    }

    @PutMapping("/api/users/{userId}")
    fun update(
        @PathVariable userId: UUID,
        @RequestHeader("X-User-Id") requestingUserId: UUID,
        @RequestBody request: UpdateUserRequest
    ): ResponseEntity<Any> {
        logger.info("PUT /api/users/{}", userId)
        return ResponseEntity.status(501).body("Not implemented")
    }
}
