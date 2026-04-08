package com.acme.fulfillmentmethods

// ──────────────────────────────────────────────────────────────────
// Fulfillment Method: The core entity of the new system.
//
// Replaces the old FulfillmentType enum. Instead of a fixed set of
// types with hardcoded settings, a fulfillment method is a container
// that holds a composable set of behaviours.
//
// "Drive Through" isn't an enum value — it's a fulfillment method
// with specific behaviours configured for a specific context.
// ──────────────────────────────────────────────────────────────────

/**
 * A fulfillment method as returned to the client — fully resolved
 * for the requested context. All behaviours are populated with
 * their resolved settings.
 */
data class FulfillmentMethod(
    val id: String,
    val name: String,
    val behaviours: List<BehaviourType>,

    // Typed behaviour settings — populated for each type listed in behaviours
    val recipientDetails: RecipientDetailsBehaviour? = null,
    val scheduling: SchedulingBehaviour? = null,
    val prepTime: PrepTimeBehaviour? = null,
    val orderStatusTracking: OrderStatusTrackingBehaviour? = null,
)

enum class BehaviourType {
    RECIPIENT_DETAILS,
    SCHEDULING,
    PREP_TIME,
    ORDER_STATUS_TRACKING,
}

// ──────────────────────────────────────────────────────────────────
// Typed Behaviour Models
// These are the "view" objects returned to clients, composed from
// one or more contextually-resolved settings.
// ──────────────────────────────────────────────────────────────────

data class RecipientDetailsBehaviour(
    val customerName: String?,
    val phoneNumber: String?,
    val email: String?,
    val customerNameRequired: Boolean,
    val phoneNumberRequired: Boolean,
    val emailRequired: Boolean,
    val guestIdentifier: GuestIdentifierBehaviour?,
)

/**
 * Generic guest identifier — powers drive-thru car details,
 * dine-in table numbers, delivery notes, etc.
 * The same generic model serves many specific use cases
 * without any type-specific code.
 */
data class GuestIdentifierBehaviour(
    val clientId: String,
    val value: String,
    val displayText: String,
)

data class SchedulingBehaviour(
    val enabled: Boolean,
    val minLeadTimeMinutes: Int,
    val maxDaysAhead: Int,
)

data class PrepTimeBehaviour(
    val defaultMinutes: Int,
    val showEstimateToCustomer: Boolean,
)

data class OrderStatusTrackingBehaviour(
    val showWindowNumber: Boolean,
    val showDriverLocation: Boolean,
    val statusUpdatesEnabled: Boolean,
)
