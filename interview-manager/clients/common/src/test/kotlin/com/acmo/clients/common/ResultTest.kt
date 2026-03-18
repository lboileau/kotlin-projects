package com.acmo.clients.common

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ResultTest {

    @Nested
    inner class SuccessVariant {

        private val result: Result<String, String> = success("hello")

        @Test
        fun `isSuccess returns true`() {
            assertThat(result.isSuccess).isTrue()
        }

        @Test
        fun `isFailure returns false`() {
            assertThat(result.isFailure).isFalse()
        }

        @Test
        fun `getOrNull returns the value`() {
            assertThat(result.getOrNull()).isEqualTo("hello")
        }

        @Test
        fun `errorOrNull returns null`() {
            assertThat(result.errorOrNull()).isNull()
        }

        @Test
        fun `getOrElse returns the value`() {
            assertThat(result.getOrElse { "default" }).isEqualTo("hello")
        }

        @Test
        fun `map transforms the value`() {
            val mapped = result.map { it.uppercase() }
            assertThat(mapped.getOrNull()).isEqualTo("HELLO")
        }

        @Test
        fun `flatMap transforms the value`() {
            val mapped = result.flatMap { success(it.length) }
            assertThat(mapped.getOrNull()).isEqualTo(5)
        }

        @Test
        fun `flatMap propagates failure from transform`() {
            val mapped = result.flatMap { failure("oops") }
            assertThat(mapped.isFailure).isTrue()
            assertThat(mapped.errorOrNull()).isEqualTo("oops")
        }
    }

    @Nested
    inner class FailureVariant {

        private val result: Result<String, String> = failure("error")

        @Test
        fun `isSuccess returns false`() {
            assertThat(result.isSuccess).isFalse()
        }

        @Test
        fun `isFailure returns true`() {
            assertThat(result.isFailure).isTrue()
        }

        @Test
        fun `getOrNull returns null`() {
            assertThat(result.getOrNull()).isNull()
        }

        @Test
        fun `errorOrNull returns the error`() {
            assertThat(result.errorOrNull()).isEqualTo("error")
        }

        @Test
        fun `getOrElse returns the default`() {
            assertThat(result.getOrElse { "default" }).isEqualTo("default")
        }

        @Test
        fun `map does not transform`() {
            val mapped = result.map { it.uppercase() }
            assertThat(mapped.isFailure).isTrue()
            assertThat(mapped.errorOrNull()).isEqualTo("error")
        }

        @Test
        fun `flatMap does not transform`() {
            val mapped = result.flatMap { success(it.length) }
            assertThat(mapped.isFailure).isTrue()
            assertThat(mapped.errorOrNull()).isEqualTo("error")
        }
    }
}
