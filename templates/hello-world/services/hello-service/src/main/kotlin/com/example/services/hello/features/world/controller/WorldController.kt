package com.example.services.hello.features.world.controller

import com.example.services.hello.common.error.toResponseEntity
import com.example.services.hello.features.world.dto.CreateWorldRequest
import com.example.services.hello.features.world.dto.UpdateWorldRequest
import com.example.services.hello.features.world.mapper.WorldMapper
import com.example.services.hello.features.world.params.*
import com.example.services.hello.features.world.service.WorldService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/worlds")
class WorldController(private val worldService: WorldService) {
    private val logger = LoggerFactory.getLogger(WorldController::class.java)

    @GetMapping("/{id}")
    fun getById(@PathVariable id: UUID): ResponseEntity<Any> {
        logger.info("GET /api/worlds/{}", id)
        val param = GetWorldByIdParam(id = id)
        return worldService.getById(param).toResponseEntity { WorldMapper.toResponse(it) }
    }

    @GetMapping
    fun getAll(): ResponseEntity<Any> {
        logger.info("GET /api/worlds")
        val param = GetAllWorldsParam()
        return worldService.getAll(param).toResponseEntity { list -> list.map { WorldMapper.toResponse(it) } }
    }

    @PostMapping
    fun create(@RequestBody request: CreateWorldRequest): ResponseEntity<Any> {
        logger.info("POST /api/worlds")
        val param = CreateWorldParam(name = request.name, greeting = request.greeting)
        return worldService.create(param).toResponseEntity(successStatus = 201) { WorldMapper.toResponse(it) }
    }

    @PutMapping("/{id}")
    fun update(@PathVariable id: UUID, @RequestBody request: UpdateWorldRequest): ResponseEntity<Any> {
        logger.info("PUT /api/worlds/{}", id)
        val param = UpdateWorldParam(id = id, name = request.name, greeting = request.greeting)
        return worldService.update(param).toResponseEntity { WorldMapper.toResponse(it) }
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: UUID): ResponseEntity<Any> {
        logger.info("DELETE /api/worlds/{}", id)
        val param = DeleteWorldParam(id = id)
        return worldService.delete(param).toResponseEntity(successStatus = 204) { }
    }
}
