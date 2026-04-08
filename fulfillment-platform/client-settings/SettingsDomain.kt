package com.acme.clientsettings

// ──────────────────────────────────────────────────────────────────
// Settings Domain: The top-level enum of loadable resource domains.
//
// When a client requests settings, it passes a domain key.
// The client settings service finds all loaders annotated for that
// domain, builds a dependency DAG, and loads them in order.
// ──────────────────────────────────────────────────────────────────

enum class SettingsDomain {
    // Resource loaders — raw data sources
    MERCHANT_INFO,
    LOCATION_INFO,
    CHANNEL_INFO,

    // Composed settings — depend on resource loaders
    CONTEXTUAL_FULFILLMENT_METHODS,
}
