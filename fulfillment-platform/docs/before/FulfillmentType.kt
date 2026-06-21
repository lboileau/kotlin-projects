package com.acme.legacy

// ──────────────────────────────────────────────────────────────────
// THE OLD WORLD: Static enum-based fulfillment types
// ──────────────────────────────────────────────────────────────────

enum class FulfillmentType {
    PICKUP,
    DELIVERY,
    IN_STORE,
    SHIPPING
}

// Settings — one blob per type, only one sub-message populated based on type
data class FulfillmentSettings(
    val type: FulfillmentType,
    val pickupSettings: PickupSettings? = null,
    val deliverySettings: DeliverySettings? = null,
    val shippingSettings: ShippingSettings? = null,
    val inStoreSettings: InStoreSettings? = null,
    // Only one populated ☝️ — adding a new type means a new nullable field here
)

data class PickupSettings(
    val estimatedPrepTimeMinutes: Int,
    val requiresCustomerName: Boolean,
    val allowsScheduling: Boolean,
)

data class DeliverySettings(
    val estimatedPrepTimeMinutes: Int,  // duplicated
    val requiresCustomerName: Boolean,  // duplicated
    val allowsScheduling: Boolean,      // duplicated
    val requiresAddress: Boolean,
    val deliveryRadiusMiles: Double,
)

data class ShippingSettings(
    val requiresCustomerName: Boolean,  // duplicated
    val requiresAddress: Boolean,       // duplicated
    val allowsScheduling: Boolean,      // duplicated
)

data class InStoreSettings(
    val estimatedPrepTimeMinutes: Int,  // duplicated
    val requiresCustomerName: Boolean,  // duplicated
)

// Order info — per-type sub-objects with duplicated fields
data class Fulfillment(
    val type: FulfillmentType,
    val pickupInfo: PickupInfo? = null,
    val deliveryInfo: DeliveryInfo? = null,
    val shippingInfo: ShippingInfo? = null,
    val inStoreInfo: InStoreInfo? = null,
    // Only one populated ☝️
)

data class PickupInfo(
    val customerName: String,
    val phoneNumber: String,
    val pickupAt: String,
)

data class DeliveryInfo(
    val customerName: String,   // duplicated
    val phoneNumber: String,    // duplicated
    val deliveryAddress: String,
    val deliveryAt: String,
)

data class ShippingInfo(
    val customerName: String,   // duplicated
    val phoneNumber: String,    // duplicated
    val shippingAddress: String,
)

data class InStoreInfo(
    val customerName: String,   // duplicated
    val phoneNumber: String,    // duplicated
    val tableNumber: String?,
)

// ──────────────────────────────────────────────────────────────────
// THE PROBLEM: Every code path branches on type
// This pattern exists EVERYWHERE in the codebase.
// Adding a new fulfillment type means touching every single one.
// ──────────────────────────────────────────────────────────────────

fun getCustomerName(fulfillment: Fulfillment): String {
    return when (fulfillment.type) {
        FulfillmentType.PICKUP -> fulfillment.pickupInfo!!.customerName
        FulfillmentType.DELIVERY -> fulfillment.deliveryInfo!!.customerName
        FulfillmentType.IN_STORE -> fulfillment.inStoreInfo!!.customerName
        FulfillmentType.SHIPPING -> fulfillment.shippingInfo!!.customerName
        // Every new type → new branch here, AND in every other when() block
    }
}

fun getPhoneNumber(fulfillment: Fulfillment): String {
    return when (fulfillment.type) {
        FulfillmentType.PICKUP -> fulfillment.pickupInfo!!.phoneNumber
        FulfillmentType.DELIVERY -> fulfillment.deliveryInfo!!.phoneNumber
        FulfillmentType.IN_STORE -> fulfillment.inStoreInfo!!.phoneNumber
        FulfillmentType.SHIPPING -> fulfillment.shippingInfo!!.phoneNumber
    }
}

fun isSchedulingAllowed(settings: FulfillmentSettings): Boolean {
    return when (settings.type) {
        FulfillmentType.PICKUP -> settings.pickupSettings!!.allowsScheduling
        FulfillmentType.DELIVERY -> settings.deliverySettings!!.allowsScheduling
        FulfillmentType.IN_STORE -> false // not supported, but you still need the branch
        FulfillmentType.SHIPPING -> settings.shippingSettings!!.allowsScheduling
    }
}

// ──────────────────────────────────────────────────────────────────
// POS side: Dining Options — totally disconnected from the above
// ──────────────────────────────────────────────────────────────────

data class DiningOption(
    val id: String,
    val label: String, // freeform: "Dine In", "Eat Here", "Para llevar", anything
)

// On the order — just a stamp, no semantic meaning
data class PosOrder(
    val diningOptionId: String,
    val diningOptionLabel: String,
    // Cannot aggregate with web orders — "Dine In" ≠ PICKUP programmatically
)
