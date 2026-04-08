package com.acme.contextualsettings

// ──────────────────────────────────────────────────────────────────
// The context object that accompanies every settings request.
// This is how we achieve per-platform, per-location variation
// without exploding the enum space.
// ──────────────────────────────────────────────────────────────────

data class SettingsContext(
    val merchantId: String,
    val locationId: String? = null,
    val channelId: String? = null,           // e.g. "POS", "WEB", "KIOSK", "QR"
    val fulfillmentMethodId: String? = null,
) {
    /**
     * Build the scope key map for a given scope vector.
     * A scope vector is an ordered list of context dimensions to include.
     *
     * Example: scopeVector = [LOCATION_ID, CHANNEL_ID]
     *   → { "location_id": "L1", "channel_id": "POS" }
     *
     * Returns null if any required dimension is not present in this context.
     */
    fun buildScopeKeys(scopeVector: List<ScopeDimension>): Map<String, String>? {
        val keys = mutableMapOf<String, String>()
        keys["merchant_id"] = merchantId // always included

        for (dimension in scopeVector) {
            val value = when (dimension) {
                ScopeDimension.LOCATION_ID -> locationId
                ScopeDimension.CHANNEL_ID -> channelId
                ScopeDimension.FULFILLMENT_METHOD_ID -> fulfillmentMethodId
            } ?: return null // dimension not present in context — skip this scope level

            keys[dimension.key] = value
        }
        return keys
    }
}

enum class ScopeDimension(val key: String) {
    LOCATION_ID("location_id"),
    CHANNEL_ID("channel_id"),
    FULFILLMENT_METHOD_ID("fulfillment_method_id"),
}
