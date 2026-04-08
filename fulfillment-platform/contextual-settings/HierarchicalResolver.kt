package com.acme.contextualsettings

import com.acme.redb.RedbBatchQuery
import com.acme.redb.RedbClient
import com.acme.redb.RedbEntity

// ──────────────────────────────────────────────────────────────────
// THE CORE: Hierarchical Settings Resolution Engine
//
// This is the heart of the contextual settings service.
// It resolves a setting for a given context by walking the scope
// hierarchy defined in the setting's resolution policy.
//
// The engine is generic — it doesn't know about fulfillment methods,
// recipient details, or scheduling. It just resolves settings
// according to their registered policies.
// ──────────────────────────────────────────────────────────────────

class HierarchicalResolver(
    private val redbClient: RedbClient,
    private val deserializer: SettingsDeserializer,
) {

    /**
     * Resolve a single setting for a given context.
     *
     * 1. Batch-fetch all scope variants of this setting from REDB
     * 2. Walk the policy's scope vectors in order
     * 3. Return the first (or merged) match based on the strategy
     */
    fun <T : Any> resolve(
        definition: ContextualSettingDefinition<T>,
        context: SettingsContext,
    ): T? {
        // Fetch all scope variants for this setting in one batch call
        val entities = redbClient.batchQuery(
            RedbBatchQuery(
                typeKey = definition.redbTypeKey,
                scopeFilter = mapOf("merchant_id" to context.merchantId),
            )
        )

        return when (definition.resolutionPolicy.strategy) {
            ResolutionStrategy.OVERRIDE_WINS -> resolveOverrideWins(definition, context, entities)
            ResolutionStrategy.DEFAULTS_WITH_OVERRIDES -> resolveDefaultsWithOverrides(definition, context, entities)
        }
    }

    /**
     * Resolve multiple settings in a single batch call.
     * Used when a behaviour is composed of multiple registered settings.
     */
    fun resolveMultiple(
        definitions: List<ContextualSettingDefinition<*>>,
        context: SettingsContext,
    ): Map<String, Any?> {
        // Batch-fetch all settings in one round trip
        val queries = definitions.map { def ->
            RedbBatchQuery(
                typeKey = def.redbTypeKey,
                scopeFilter = mapOf("merchant_id" to context.merchantId),
            )
        }

        val allEntities = redbClient.batchQueryMultiple(queries)

        return definitions.associate { def ->
            val entities = allEntities[def.redbTypeKey] ?: emptyList()
            def.name to resolveForDefinition(def, context, entities)
        }
    }

    // ── Resolution Strategies ────────────────────────────────────

    /**
     * OVERRIDE_WINS: Walk scope vectors from most specific to least.
     * First scope level that has a value wins entirely.
     *
     * Example for recipient_details with policy:
     *   [location+channel] → [channel] → [location] → [merchant]
     *
     *   Request: merchant=M1, location=L1, channel=POS
     *
     *   Step 1: Look for entity with scope { merchant_id=M1, location_id=L1, channel_id=POS }
     *           → Found? Return it. Done.
     *   Step 2: Look for entity with scope { merchant_id=M1, channel_id=POS }
     *           → Found? Return it. Done.
     *   Step 3: Look for entity with scope { merchant_id=M1, location_id=L1 }
     *           → Found? Return it. Done.
     *   Step 4: Look for entity with scope { merchant_id=M1 }
     *           → Found? Return it. Done.
     *   Step 5: No match at any level → return null.
     */
    private fun <T : Any> resolveOverrideWins(
        definition: ContextualSettingDefinition<T>,
        context: SettingsContext,
        entities: List<RedbEntity>,
    ): T? {
        val policy = definition.resolutionPolicy

        for (scopeVector in policy.scopes) {
            val scopeKeys = context.buildScopeKeys(scopeVector) ?: continue

            val match = entities.find { entity ->
                entity.scopeKeys == scopeKeys
            }

            if (match != null) {
                return deserializer.deserialize(match, definition.modelClass)
            }
        }

        return null
    }

    /**
     * DEFAULTS_WITH_OVERRIDES: Start from broadest scope, layer narrower scopes on top.
     * Each subsequent scope level merges its fields over the accumulated result.
     *
     * Example for prep_time with policy:
     *   [merchant] → [location] → [channel] → [location+channel]
     *
     *   Request: merchant=M1, location=L1, channel=POS
     *
     *   Step 1: Merchant M1 → { defaultMinutes=15, showEstimate=true }
     *   Step 2: Location L1 → { defaultMinutes=10 }
     *           Merged: { defaultMinutes=10, showEstimate=true }
     *   Step 3: Channel POS → no entry, skip
     *   Step 4: L1+POS → { defaultMinutes=5 }
     *           Merged: { defaultMinutes=5, showEstimate=true }
     *
     * This lets merchants set rich defaults and locations/channels
     * override only what's different — sparse configuration.
     */
    private fun <T : Any> resolveDefaultsWithOverrides(
        definition: ContextualSettingDefinition<T>,
        context: SettingsContext,
        entities: List<RedbEntity>,
    ): T? {
        val policy = definition.resolutionPolicy
        var accumulated: Map<String, Any>? = null

        for (scopeVector in policy.scopes) {
            val scopeKeys = context.buildScopeKeys(scopeVector) ?: continue

            val match = entities.find { entity ->
                entity.scopeKeys == scopeKeys
            }

            if (match != null) {
                accumulated = if (accumulated == null) {
                    match.data
                } else {
                    // Merge: narrower scope fields override broader scope fields
                    accumulated + match.data
                }
            }
        }

        return accumulated?.let {
            deserializer.deserializeFromMap(it, definition.modelClass)
        }
    }

    private fun <T : Any> resolveForDefinition(
        definition: ContextualSettingDefinition<*>,
        context: SettingsContext,
        entities: List<RedbEntity>,
    ): Any? {
        @Suppress("UNCHECKED_CAST")
        val typed = definition as ContextualSettingDefinition<Any>
        return when (definition.resolutionPolicy.strategy) {
            ResolutionStrategy.OVERRIDE_WINS -> resolveOverrideWins(typed, context, entities)
            ResolutionStrategy.DEFAULTS_WITH_OVERRIDES -> resolveDefaultsWithOverrides(typed, context, entities)
        }
    }
}

/**
 * Deserializer interface — abstracts proto deserialization.
 * In practice this was proto-based; here we use a simple interface.
 */
interface SettingsDeserializer {
    fun <T : Any> deserialize(entity: RedbEntity, modelClass: Class<T>): T
    fun <T : Any> deserializeFromMap(data: Map<String, Any>, modelClass: Class<T>): T
}
