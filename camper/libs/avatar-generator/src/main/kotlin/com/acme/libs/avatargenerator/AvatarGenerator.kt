package com.acme.libs.avatargenerator

import com.acme.libs.avatargenerator.model.Avatar
import com.acme.libs.avatargenerator.model.ClothingStyle
import com.acme.libs.avatargenerator.model.HairColor
import com.acme.libs.avatargenerator.model.HairStyle
import com.acme.libs.avatargenerator.model.PantsColor
import com.acme.libs.avatargenerator.model.ShirtColor
import com.acme.libs.avatargenerator.model.SkinColor
import java.security.MessageDigest
import java.util.UUID
import kotlin.math.abs

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
        val hash = sha256(seed)
        return Avatar(
            hairStyle = HairStyle.entries[ordinalFromBytes(hash, 0)],
            hairColor = HairColor.entries[ordinalFromBytes(hash, 4)],
            skinColor = SkinColor.entries[ordinalFromBytes(hash, 8)],
            clothingStyle = ClothingStyle.entries[ordinalFromBytes(hash, 12)],
            pantsColor = PantsColor.entries[ordinalFromBytes(hash, 16)],
            shirtColor = ShirtColor.entries[ordinalFromBytes(hash, 20)]
        )
    }

    /**
     * Generate a seed from a user's name.
     * Used for initial avatar seed on registration.
     */
    fun seedFromName(name: String): String {
        return sha256(name.trim().lowercase()).joinToString("") { "%02x".format(it) }
    }

    /**
     * Generate a random seed.
     * Used when the user re-randomizes their avatar.
     */
    fun randomSeed(): String {
        return UUID.randomUUID().toString()
    }

    private fun sha256(input: String): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray())
    }

    private fun ordinalFromBytes(hash: ByteArray, offset: Int): Int {
        val value = (hash[offset].toInt() shl 24) or
            ((hash[offset + 1].toInt() and 0xFF) shl 16) or
            ((hash[offset + 2].toInt() and 0xFF) shl 8) or
            (hash[offset + 3].toInt() and 0xFF)
        return abs(value) % 8
    }
}
