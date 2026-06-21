package com.acme.contextualsettings

import com.acme.contextualsettings.ScopeDimension.CHANNEL_ID
import com.acme.contextualsettings.ScopeDimension.FULFILLMENT_METHOD_ID
import com.acme.contextualsettings.ScopeDimension.LOCATION_ID

// ──────────────────────────────────────────────────────────────────
// Settings Definitions: The registry of all contextual settings.
//
// Each entry declares:
//   - redbTypeKey: the key in the entity store (REDB)
//   - the proto/model class it deserializes into
//   - the resolution policy (scope vector ordering + strategy)
//
// This is the single source of truth for how each setting behaves.
// Adding a new setting = adding an entry here. No code changes needed
// in the resolution engine.
// ──────────────────────────────────────────────────────────────────

/**
 * Registration metadata for a contextual setting backed by REDB.
 */
data class ContextualSettingDefinition<T : Any>(
    val name: String,
    val redbTypeKey: String,           // full proto descriptor name
    val modelClass: Class<T>,
    val resolutionPolicy: ResolutionPolicy,  // defined here in code, not in annotations
)

/**
 * All registered contextual settings.
 *
 * Each setting defines its own resolution policy — the scope vector
 * ordering and strategy. This means recipient_details can resolve
 * differently than scheduling, which can resolve differently than
 * prep_time. The engine is generic; the policy is per-setting.
 */
object SettingsDefinitions {

    // ── Recipient Details ──────────────────────────────────────
    // Who is the customer? What info do we collect?
    // Resolves bottom-up: most specific context wins entirely.
    val RECIPIENT_DETAILS = ContextualSettingDefinition(
        name = "recipient_details",
        redbTypeKey = "com.acme.fulfillment.RecipientDetailsSettings",
        modelClass = RecipientDetailsProto::class.java,
        resolutionPolicy = ResolutionPolicy(
            scopes = listOf(
                listOf(LOCATION_ID, CHANNEL_ID),   // most specific: this location on this channel
                listOf(CHANNEL_ID),                 // channel-wide default (e.g., all POS locations)
                listOf(LOCATION_ID),                // location-wide default (e.g., all channels at L1)
                listOf(),                           // merchant-wide fallback (empty = merchant only)
            ),
            strategy = ResolutionStrategy.OVERRIDE_WINS,
        ),
    )

    // ── Guest Identifier ───────────────────────────────────────
    // Generic "identify the guest" field — powers car details for
    // drive-thru, table number for dine-in, etc.
    // Same resolution as recipient_details (it's a sub-behaviour).
    val GUEST_IDENTIFIER = ContextualSettingDefinition(
        name = "guest_identifier",
        redbTypeKey = "com.acme.fulfillment.GuestIdentifierSettings",
        modelClass = GuestIdentifierProto::class.java,
        resolutionPolicy = ResolutionPolicy(
            scopes = listOf(
                listOf(LOCATION_ID, CHANNEL_ID),
                listOf(CHANNEL_ID),
                listOf(LOCATION_ID),
                listOf(),
            ),
            strategy = ResolutionStrategy.OVERRIDE_WINS,
        ),
    )

    // ── Scheduling ─────────────────────────────────────────────
    // Can customers schedule orders? Lead times, max days ahead, etc.
    // Resolves bottom-up: a location+channel can override entirely.
    val SCHEDULING = ContextualSettingDefinition(
        name = "scheduling",
        redbTypeKey = "com.acme.fulfillment.SchedulingSettings",
        modelClass = SchedulingProto::class.java,
        resolutionPolicy = ResolutionPolicy(
            scopes = listOf(
                listOf(LOCATION_ID, CHANNEL_ID),
                listOf(CHANNEL_ID),
                listOf(LOCATION_ID),
                listOf(),
            ),
            strategy = ResolutionStrategy.OVERRIDE_WINS,
        ),
    )

    // ── Prep Time ──────────────────────────────────────────────
    // How long to prepare the order.
    // Uses DEFAULTS_WITH_OVERRIDES: merchant sets a base,
    // individual locations can override just the minutes field
    // without re-specifying everything.
    val PREP_TIME = ContextualSettingDefinition(
        name = "prep_time",
        redbTypeKey = "com.acme.fulfillment.PrepTimeSettings",
        modelClass = PrepTimeProto::class.java,
        resolutionPolicy = ResolutionPolicy(
            scopes = listOf(
                listOf(),                           // merchant-wide default first
                listOf(LOCATION_ID),                // location can override
                listOf(CHANNEL_ID),                 // channel can override
                listOf(LOCATION_ID, CHANNEL_ID),    // most specific layered on top
            ),
            strategy = ResolutionStrategy.DEFAULTS_WITH_OVERRIDES,
        ),
    )

    // ── Order Status Tracking ──────────────────────────────────
    // What status updates to show the customer.
    // Per fulfillment method + channel — drive-thru shows window number,
    // delivery shows driver location, etc.
    val ORDER_STATUS_TRACKING = ContextualSettingDefinition(
        name = "order_status_tracking",
        redbTypeKey = "com.acme.fulfillment.OrderStatusTrackingSettings",
        modelClass = OrderStatusTrackingProto::class.java,
        resolutionPolicy = ResolutionPolicy(
            scopes = listOf(
                listOf(FULFILLMENT_METHOD_ID, CHANNEL_ID),
                listOf(FULFILLMENT_METHOD_ID),
                listOf(CHANNEL_ID),
                listOf(),
            ),
            strategy = ResolutionStrategy.OVERRIDE_WINS,
        ),
    )

    /** All registered definitions — used by the resolution engine for discovery. */
    val ALL = listOf(
        RECIPIENT_DETAILS,
        GUEST_IDENTIFIER,
        SCHEDULING,
        PREP_TIME,
        ORDER_STATUS_TRACKING,
    )
}

// ──────────────────────────────────────────────────────────────────
// Proto stand-ins (Kotlin data classes representing proto messages)
// ──────────────────────────────────────────────────────────────────

data class RecipientDetailsProto(
    val customerName: String? = null,
    val phoneNumber: String? = null,
    val email: String? = null,
    val customerNameRequired: Boolean = false,
    val phoneNumberRequired: Boolean = false,
    val emailRequired: Boolean = false,
)

data class GuestIdentifierProto(
    val clientId: String,
    val value: String = "",
    val displayText: String = "",  // e.g. "Car Details" for drive-thru
)

data class SchedulingProto(
    val enabled: Boolean = false,
    val minLeadTimeMinutes: Int = 0,
    val maxDaysAhead: Int = 7,
)

data class PrepTimeProto(
    val defaultMinutes: Int = 15,
    val showEstimateToCustomer: Boolean = true,
)

data class OrderStatusTrackingProto(
    val showWindowNumber: Boolean = false,
    val showDriverLocation: Boolean = false,
    val statusUpdatesEnabled: Boolean = true,
)
