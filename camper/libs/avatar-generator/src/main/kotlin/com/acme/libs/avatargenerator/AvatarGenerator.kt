package com.acme.libs.avatargenerator

import com.acme.libs.avatargenerator.model.Avatar

/**
 * Deterministic avatar generator.
 *
 * Generates avatar properties from a seed string using SHA-256 hashing.
 * Same seed always produces the same avatar. Each enum has exactly 8 values,
 * giving 8^6 = 262,144 unique avatar combinations.
 */
object AvatarGenerator {

    /**
     * Generate an avatar deterministically from a seed string.
     * Same seed always produces the same avatar.
     */
    fun generate(seed: String): Avatar {
        throw NotImplementedError("AvatarGenerator.generate() not yet implemented")
    }

    /**
     * Generate a seed from a user's name.
     * Used for initial avatar seed on registration.
     */
    fun seedFromName(name: String): String {
        throw NotImplementedError("AvatarGenerator.seedFromName() not yet implemented")
    }

    /**
     * Generate a random seed.
     * Used when the user re-randomizes their avatar.
     */
    fun randomSeed(): String {
        throw NotImplementedError("AvatarGenerator.randomSeed() not yet implemented")
    }
}
