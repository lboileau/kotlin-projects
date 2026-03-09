package com.acme.clients.common

object EmailNormalizer {
    fun normalize(email: String): String {
        val parts = email.split("@", limit = 2)
        if (parts.size != 2) return email.lowercase()
        val local = parts[0].replace(".", "").lowercase()
        val domain = parts[1].lowercase()
        return "$local@$domain"
    }

    const val SQL_EXPRESSION = "LOWER(REPLACE(SPLIT_PART(email, '@', 1), '.', '') || '@' || SPLIT_PART(email, '@', 2))"
}
