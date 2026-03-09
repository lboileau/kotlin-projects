package com.acme.clients.userclient

import com.acme.clients.common.EmailNormalizer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EmailNormalizerTest {

    @Test
    fun `lowercases entire email`() {
        assertThat(EmailNormalizer.normalize("Alice@Example.COM")).isEqualTo("alice@example.com")
    }

    @Test
    fun `removes dots from local part`() {
        assertThat(EmailNormalizer.normalize("a.li.ce@example.com")).isEqualTo("alice@example.com")
    }

    @Test
    fun `preserves dots in domain`() {
        assertThat(EmailNormalizer.normalize("alice@sub.example.com")).isEqualTo("alice@sub.example.com")
    }

    @Test
    fun `handles combined case and dots`() {
        assertThat(EmailNormalizer.normalize("A.Li.Ce@Example.COM")).isEqualTo("alice@example.com")
    }

    @Test
    fun `handles email without dots`() {
        assertThat(EmailNormalizer.normalize("alice@example.com")).isEqualTo("alice@example.com")
    }

    @Test
    fun `handles email without at sign gracefully`() {
        assertThat(EmailNormalizer.normalize("not-an-email")).isEqualTo("not-an-email")
    }

    @Test
    fun `does not strip dots from domain part`() {
        assertThat(EmailNormalizer.normalize("alice@mail.sub.example.com")).isEqualTo("alice@mail.sub.example.com")
    }
}
