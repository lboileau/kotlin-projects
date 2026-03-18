package com.acmo.services.common

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ApiResponseTest {

    @Nested
    inner class Ok {
        @Test
        fun `ok returns status 200 with body`() {
            val response = ApiResponse.ok("hello")

            assertThat(response.status).isEqualTo(200)
            assertThat(response.body).isEqualTo("hello")
            assertThat(response.error).isNull()
        }
    }

    @Nested
    inner class Created {
        @Test
        fun `created returns status 201 with body`() {
            val response = ApiResponse.created("new-item")

            assertThat(response.status).isEqualTo(201)
            assertThat(response.body).isEqualTo("new-item")
            assertThat(response.error).isNull()
        }
    }

    @Nested
    inner class NoContent {
        @Test
        fun `noContent returns status 204 with no body or error`() {
            val response = ApiResponse.noContent()

            assertThat(response.status).isEqualTo(204)
            assertThat(response.body as Any?).isNull()
            assertThat(response.error).isNull()
        }
    }

    @Nested
    inner class NotFound {
        @Test
        fun `notFound returns status 404 with error body`() {
            val response = ApiResponse.notFound("item not found")

            assertThat(response.status).isEqualTo(404)
            assertThat(response.body as Any?).isNull()
            val error = response.error!!
            assertThat(error.code).isEqualTo("NOT_FOUND")
            assertThat(error.message).isEqualTo("item not found")
            assertThat(error.details).isNull()
        }
    }

    @Nested
    inner class Conflict {
        @Test
        fun `conflict returns status 409 with error body`() {
            val response = ApiResponse.conflict("already exists")

            assertThat(response.status).isEqualTo(409)
            assertThat(response.body as Any?).isNull()
            val error = response.error!!
            assertThat(error.code).isEqualTo("CONFLICT")
            assertThat(error.message).isEqualTo("already exists")
            assertThat(error.details).isNull()
        }
    }

    @Nested
    inner class BadRequest {
        @Test
        fun `badRequest returns status 400 with error body`() {
            val response = ApiResponse.badRequest("invalid input")

            assertThat(response.status).isEqualTo(400)
            assertThat(response.body as Any?).isNull()
            val error = response.error!!
            assertThat(error.code).isEqualTo("BAD_REQUEST")
            assertThat(error.message).isEqualTo("invalid input")
            assertThat(error.details).isNull()
        }
    }

    @Nested
    inner class ValidationError {
        @Test
        fun `validationError returns status 422 with error body and details`() {
            val details = mapOf<String, Any>("field" to "name", "reason" to "must not be blank")
            val response = ApiResponse.validationError("validation failed", details)

            assertThat(response.status).isEqualTo(422)
            assertThat(response.body as Any?).isNull()
            val error = response.error!!
            assertThat(error.code).isEqualTo("VALIDATION_ERROR")
            assertThat(error.message).isEqualTo("validation failed")
            assertThat(error.details).isEqualTo(details)
        }
    }

    @Nested
    inner class InternalError {
        @Test
        fun `internalError returns status 500 with error body`() {
            val response = ApiResponse.internalError("something went wrong")

            assertThat(response.status).isEqualTo(500)
            assertThat(response.body as Any?).isNull()
            val error = response.error!!
            assertThat(error.code).isEqualTo("INTERNAL_ERROR")
            assertThat(error.message).isEqualTo("something went wrong")
            assertThat(error.details).isNull()
        }
    }
}
