package com.acme.services.camperservice.features.recipe.controller

import com.acme.clients.common.Result
import com.acme.services.camperservice.common.error.toResponseEntity
import com.acme.services.camperservice.features.recipe.params.GetStoredPhotoParam
import com.acme.services.camperservice.features.recipe.service.RecipeService
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.TimeUnit

/**
 * Serves stored photos by key. In production the recipe response carries presigned bucket URLs
 * and this route is a fallback; with the local file store it is what those URLs point at.
 * No `X-User-Id`: an `<img>` can't send one. Keys hold two UUIDs, which is the same
 * unguessability `GET /api/recipes/{id}` relies on. A photo's bytes never change, so the
 * response is cacheable for a year.
 */
@RestController
@RequestMapping("/api/photo-store")
class PhotoStoreController(
    private val recipeService: RecipeService
) {
    private val logger = LoggerFactory.getLogger(PhotoStoreController::class.java)

    @GetMapping("/**")
    fun get(request: HttpServletRequest): ResponseEntity<Any> {
        val key = request.requestURI.removePrefix("/api/photo-store/")
        logger.debug("GET /api/photo-store/{}", key)
        return when (val result = recipeService.getStoredPhoto(GetStoredPhotoParam(key))) {
            is Result.Success -> ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.value.mediaType))
                .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePrivate().immutable())
                .body(result.value.bytes)
            is Result.Failure -> if (result.error is com.acme.services.camperservice.features.recipe.error.RecipeError.Invalid) {
                ResponseEntity.notFound().build()
            } else {
                result.error.toResponseEntity()
            }
        }
    }
}
