package com.acme.clientsettings

// ──────────────────────────────────────────────────────────────────
// Settings Domain: Identifiers for each node in the data loading DAG.
//
// When a client requests settings, it passes a domain key.
// The client settings service finds the ConfigManager for that domain,
// discovers all SettingsDataLoader dependencies, builds a DAG,
// loads data in topological order, then calls the ConfigManager.
// ──────────────────────────────────────────────────────────────────

enum class SettingsDomain {
    // Resource loaders — raw data sources
    MERCHANT_INFO,
    LOCATION_INFO,
    CHANNEL_INFO,

    // Fulfillment method hierarchy (loaded in phases)
    FM_PROFILE,             // Phase 1: list of methods for this context
    FM_METHODS,             // Phase 2: method entities with behaviour references

    // Raw contextual settings (Phase 3: loaded from REDB + resolved)
    RECIPIENT_DETAILS,
    GUEST_IDENTIFIER,
    SCHEDULING,
    PREP_TIME,
    ORDER_STATUS_TRACKING,

    // Top-level config manager domain
    CONTEXTUAL_FULFILLMENT_METHODS,
}
