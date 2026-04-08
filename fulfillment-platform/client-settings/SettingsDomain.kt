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

    // Raw contextual settings — loaded from REDB + resolved
    RECIPIENT_DETAILS,
    GUEST_IDENTIFIER,
    SCHEDULING,
    PREP_TIME,
    ORDER_STATUS_TRACKING,

    // Composed behaviours — adapt raw settings into client-facing models
    RECIPIENT_DETAILS_BEHAVIOUR,

    // Top-level — depends on methods + all raw settings
    FULFILLMENT_METHODS,
    CONTEXTUAL_FULFILLMENT_METHODS,  // the collection (profile)
}
