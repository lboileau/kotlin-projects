package com.example.clients.common

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ResultTest {

    @Nested
    inner class SuccessVariant {

        @Test
        fun `isSuccess returns true`() {
            val result = success("hello")
            assertThat(result.isSuccess).isTrue()
        }

        @Test
        fun `isFailure returns false`() {
            val result = success("hello")
            assertThat(result.isFailure).isFalse()
        }

        @Test
        fun `getOrNull returns the value`() {
            val result = success("hello")
            assertThat(result.getOrNull()).isEqualTo("hello")
        }

        @Test
        fun `errorOrNull returns null`() {
            val result: Result<String, String> = success("hello")
            assertThat(result.errorOrNull()).isNull()
        }

        @Test
        fun `getOrElse returns the value`() {
            val result = success("hello")
            assertThat(result.getOrElse { "default" }).isEqualTo("hello")
        }

        @Test
        fun `map transforms the value`() {
            val result = success("hello")
            val mapped = result.map { it.uppercase() }
            assertThat(mapped.getOrNull()).isEqualTo("HELLO")
        }

        @Test
        fun `flatMap transforms the value`() {
            val result = success("hello")
            val flatMapped = result.flatMap { success(it.length) }
            assertThat(flatMapped.getOrNull()).isEqualTo(5)
        }

        @Test
        fun `flatMap can return failure`() {
            val result: Result<String, String> = success("hello")
            val flatMapped = result.flatMap { failure("oops") }
            assertThat(flatMapped.isFailure).isTrue()
            assertThat(flatMapped.errorOrNull() as String?).isEqualTo("oops")
        }
    }

    @Nested
    inner class FailureVariant {

        @Test
        fun `isSuccess returns false`() {
            val result = failure("error")
            assertThat(result.isSuccess).isFalse()
        }

        @Test
        fun `isFailure returns true`() {
            val result = failure("error")
            assertThat(result.isFailure).isTrue()
        }

        @Test
        fun `getOrNull returns null`() {
            val result: Result<String, String> = failure("error")
            assertThat(result.getOrNull()).isNull()
        }

        @Test
        fun `errorOrNull returns the error`() {
            val result = failure("error")
            assertThat(result.errorOrNull()).isEqualTo("error")
        }

        @Test
        fun `getOrElse returns the default`() {
            val result: Result<String, String> = failure("error")
            assertThat(result.getOrElse { "default" }).isEqualTo("default")
        }

        @Test
        fun `map does not transform`() {
            val result: Result<String, String> = failure("error")
            val mapped = result.map { it.uppercase() }
            assertThat(mapped.isFailure).isTrue()
            assertThat(mapped.errorOrNull()).isEqualTo("error")
        }

        @Test
        fun `flatMap does not transform`() {
            val result: Result<String, String> = failure("error")
            val flatMapped = result.flatMap { success(it.length) }
            assertThat(flatMapped.isFailure).isTrue()
            assertThat(flatMapped.errorOrNull()).isEqualTo("error")
        }
    }
}
