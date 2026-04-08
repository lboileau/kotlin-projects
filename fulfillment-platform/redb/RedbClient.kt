package com.acme.redb

// ──────────────────────────────────────────────────────────────────
// REDB: Relationship-Entity Database (DynamoDB-backed graph store)
// This layer already existed — we built on top of it.
// ──────────────────────────────────────────────────────────────────

/**
 * An entity in the REDB graph store.
 * Entities are stored with a type key and a set of scope identifiers
 * that define the context in which they exist.
 */
data class RedbEntity(
    val id: String,
    val typeKey: String,
    val scopeKeys: Map<String, String>,  // e.g. { "merchant_id": "M1", "channel_id": "POS" }
    val data: Map<String, Any>,
)

/**
 * A batch query request — fetch all entities of a given type
 * matching a partial scope (e.g., all settings for merchant M1).
 */
data class RedbBatchQuery(
    val typeKey: String,
    val scopeFilter: Map<String, String>,
)

/**
 * Interface for the REDB entity store.
 * Supports batch querying to load all scope variants of a setting in one call.
 */
interface RedbClient {

    /**
     * Batch fetch all entities matching the given type key and scope filter.
     * Returns all scope variants — the caller (contextual settings service)
     * is responsible for resolution.
     *
     * Example: query for typeKey="recipient_details_v1", scopeFilter={ merchant_id: "M1" }
     * Returns entities at all scopes: merchant-level, location-level, channel-level, etc.
     */
    fun batchQuery(query: RedbBatchQuery): List<RedbEntity>

    /**
     * Batch query for multiple type keys in a single call.
     * Used when a behaviour requires multiple registered settings.
     */
    fun batchQueryMultiple(queries: List<RedbBatchQuery>): Map<String, List<RedbEntity>>
}
