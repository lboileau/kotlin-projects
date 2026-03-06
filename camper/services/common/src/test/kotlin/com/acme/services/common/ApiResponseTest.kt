package com.acme.services.common

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
            assertThat(response.error as Any?).isNull()
        }
    }

    @Nested
    inner class Created {
        @Test
        fun `created returns status 201 with body`() {
            val response = ApiResponse.created("new-item")

            assertThat(response.status).isEqualTo(201)
            assertThat(response.body).isEqualTo("new-item")
            assertThat(response.error as Any?).isNull()
        }
    }

    @Nested
    inner class NoContent {
        @Test
        fun `noContent returns status 204 with no body or error`() {
            val response = ApiResponse.noContent()

            assertThat(response.status).isEqualTo(204)
            assertThat(response.body as Any?).isNull()
            assertThat(response.error as Any?).isNull()
        }
    }

    @Nested
    inner class NotFound {
        @Test
        fun `notFound returns status 404 with NOT_FOUND error`() {
            val response = ApiResponse.notFound("Item not found")

            assertThat(response.status).isEqualTo(404)
            assertThat(response.body as Any?).isNull()
            val error = response.error
            assertThat(error as Any?).isNotNull
            assertThat(error!!.code).isEqualTo("NOT_FOUND")
            assertThat(error.message).isEqualTo("Item not found")
            assertThat(error.details as Any?).isNull()
        }
    }

    @Nested
    inner class Conflict {
        @Test
        fun `conflict returns status 409 with CONFLICT error`() {
            val response = ApiResponse.conflict("Already exists")

            assertThat(response.status).isEqualTo(409)
            assertThat(response.body as Any?).isNull()
            val error = response.error
            assertThat(error as Any?).isNotNull
            assertThat(error!!.code).isEqualTo("CONFLICT")
            assertThat(error.message).isEqualTo("Already exists")
            assertThat(error.details as Any?).isNull()
        }
    }

    @Nested
    inner class BadRequest {
        @Test
        fun `badRequest returns status 400 with BAD_REQUEST error`() {
            val response = ApiResponse.badRequest("Invalid input")

            assertThat(response.status).isEqualTo(400)
            assertThat(response.body as Any?).isNull()
            val error = response.error
            assertThat(error as Any?).isNotNull
            assertThat(error!!.code).isEqualTo("BAD_REQUEST")
            assertThat(error.message).isEqualTo("Invalid input")
            assertThat(error.details as Any?).isNull()
        }
    }

    @Nested
    inner class ValidationError {
        @Test
        fun `validationError returns status 422 with VALIDATION_ERROR and details`() {
            val details = mapOf<String, Any>("field" to "name", "reason" to "must not be blank")
            val response = ApiResponse.validationError("Validation failed", details)

            assertThat(response.status).isEqualTo(422)
            assertThat(response.body as Any?).isNull()
            val error = response.error
            assertThat(error as Any?).isNotNull
            assertThat(error!!.code).isEqualTo("VALIDATION_ERROR")
            assertThat(error.message).isEqualTo("Validation failed")
            assertThat(error.details).isEqualTo(details)
        }
    }

    @Nested
    inner class InternalError {
        @Test
        fun `internalError returns status 500 with INTERNAL_ERROR`() {
            val response = ApiResponse.internalError("Something went wrong")

            assertThat(response.status).isEqualTo(500)
            assertThat(response.body as Any?).isNull()
            val error = response.error
            assertThat(error as Any?).isNotNull
            assertThat(error!!.code).isEqualTo("INTERNAL_ERROR")
            assertThat(error.message).isEqualTo("Something went wrong")
            assertThat(error.details as Any?).isNull()
        }
    }
}
