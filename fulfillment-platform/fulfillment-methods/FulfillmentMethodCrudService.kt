package com.acme.fulfillmentmethods

import com.acme.redb.RedbClient
import com.acme.redb.RedbEntity

// ──────────────────────────────────────────────────────────────────
// CRUD Service: Writes fulfillment method configuration to REDB.
//
// This is the admin/management side — merchants (or internal tools)
// create fulfillment methods, assign behaviours, and set values
// at various scope levels.
//
// Reads go through the contextual settings service (resolution).
// Writes go directly to REDB.
// ──────────────────────────────────────────────────────────────────

class FulfillmentMethodCrudService(
    private val redbClient: RedbClient,
) {

    /**
     * Create a new fulfillment method for a merchant.
     * Initially created from a template with sensible defaults.
     */
    fun create(params: CreateFulfillmentMethodParams): FulfillmentMethodEntity {
        // Write the fulfillment method entity to REDB
        val entity = RedbEntity(
            id = generateId(),
            typeKey = "com.acme.fulfillment.FulfillmentMethod",
            scopeKeys = mapOf("merchant_id" to params.merchantId),
            data = mapOf(
                "name" to params.name,
                "behaviours" to params.behaviours.map { it.name },
            ),
        )

        // Write default behaviour settings at merchant scope
        for (behaviourDefault in params.behaviourDefaults) {
            val settingEntity = RedbEntity(
                id = generateId(),
                typeKey = behaviourDefault.redbTypeKey,
                scopeKeys = mapOf("merchant_id" to params.merchantId),
                data = behaviourDefault.defaultValues,
            )
            // redbClient.put(settingEntity)
        }

        return FulfillmentMethodEntity(
            id = entity.id,
            name = params.name,
            behaviours = params.behaviours,
        )
    }

    /**
     * Update a behaviour setting at a specific scope.
     * This is how per-location or per-channel overrides are set.
     *
     * Example: Set drive-thru prep_time to 3 minutes at Location L1 on POS
     *   updateBehaviourSetting(
     *       redbTypeKey = "prep_time_v1",
     *       scopeKeys = { merchant_id: M1, location_id: L1, channel_id: POS },
     *       values = { defaultMinutes: 3 }
     *   )
     */
    fun updateBehaviourSetting(params: UpdateBehaviourSettingParams) {
        val entity = RedbEntity(
            id = params.settingId ?: generateId(),
            typeKey = params.redbTypeKey,
            scopeKeys = params.scopeKeys,
            data = params.values,
        )
        // redbClient.put(entity)
        // → Kafka change event is published automatically by REDB
        // → Push notification sent to clients to re-fetch
    }

    private fun generateId(): String = java.util.UUID.randomUUID().toString()
}

data class CreateFulfillmentMethodParams(
    val merchantId: String,
    val name: String,
    val behaviours: List<BehaviourType>,
    val behaviourDefaults: List<BehaviourDefault>,
)

data class BehaviourDefault(
    val redbTypeKey: String,
    val defaultValues: Map<String, Any>,
)

data class UpdateBehaviourSettingParams(
    val settingId: String?,
    val redbTypeKey: String,
    val scopeKeys: Map<String, String>,
    val values: Map<String, Any>,
)

data class FulfillmentMethodEntity(
    val id: String,
    val name: String,
    val behaviours: List<BehaviourType>,
)
