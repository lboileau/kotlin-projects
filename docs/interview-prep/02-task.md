# Task — The Insight

## Why the Enum Architecture Breaks Down

We'd just unified POS onto Fulfillment Types. But the feature roadmap ahead required:

- **Drive-through** ordering (car details, window assignment)
- **Curbside pickup** (different from in-store pickup)
- **Scheduled orders** on POS (not just web)
- **New kiosk product** (different UI, different flow)
- **QR code ordering** (yet another channel)
- **Courier vs. customer pickup** (sub-workflows within PICKUP)

### The Enum Explosion Problem

```
Current state:
    PICKUP, DELIVERY, IN_STORE, SHIPPING
                    │
                    │  Now add drive-through, curbside, kiosk,
                    │  QR ordering, courier pickup...
                    ▼
    PICKUP_IN_STORE, PICKUP_CURBSIDE, PICKUP_DRIVE_THROUGH,
    PICKUP_COURIER, DELIVERY_STANDARD, DELIVERY_SCHEDULED,
    KIOSK_DINE_IN, KIOSK_TAKEOUT, QR_DINE_IN, QR_TAKEOUT...

                    │
                    │  And each needs per-platform behavior...
                    │  (web vs POS vs kiosk)
                    ▼
    PICKUP_CURBSIDE_WEB, PICKUP_CURBSIDE_POS,
    PICKUP_CURBSIDE_KIOSK, PICKUP_DRIVE_THROUGH_POS,
    DELIVERY_SCHEDULED_WEB, DELIVERY_SCHEDULED_POS...
                    │
                    ▼
              💥 Combinatorial explosion
```

### The Settings Problem

With the existing model, every new fulfillment variation requires:

```protobuf
// 1. New enum value
enum FulfillmentType {
    PICKUP = 0;
    DELIVERY = 1;
    IN_STORE = 2;
    SHIPPING = 3;
    DRIVE_THROUGH = 4;    // new!
    CURBSIDE = 5;         // new!
    // ... keeps growing
}

// 2. New settings sub-message
message FulfillmentSettings {
    FulfillmentType type = 1;
    PickupSettings pickup_settings = 2;
    DeliverySettings delivery_settings = 3;
    ShippingSettings shipping_settings = 4;
    InStoreSettings in_store_settings = 5;
    DriveThroughSettings drive_through_settings = 6;   // new!
    CurbsideSettings curbside_settings = 7;            // new!
    // ... keeps growing
}

// 3. New order info sub-message (with duplicated fields again)
message Fulfillment {
    FulfillmentType type = 1;
    PickupInfo pickup_info = 2;
    DeliveryInfo delivery_info = 3;
    // ...
    DriveThroughInfo drive_through_info = 6;   // new! (duplicates customer_name, phone...)
    CurbsideInfo curbside_info = 7;            // new! (duplicates customer_name, phone...)
}
```

### The Code Problem

Every switch statement in the codebase needs a new branch:

```kotlin
// BEFORE: 4 branches — manageable
fun getCustomerName(fulfillment: Fulfillment): String {
    return when (fulfillment.type) {
        PICKUP -> fulfillment.pickupInfo.customerName
        DELIVERY -> fulfillment.deliveryInfo.customerName
        IN_STORE -> fulfillment.inStoreInfo.customerName
        SHIPPING -> fulfillment.shippingInfo.customerName
    }
}

// AFTER adding new types: every when() block across the entire codebase
// must be updated for each new type
fun getCustomerName(fulfillment: Fulfillment): String {
    return when (fulfillment.type) {
        PICKUP -> fulfillment.pickupInfo.customerName
        DELIVERY -> fulfillment.deliveryInfo.customerName
        IN_STORE -> fulfillment.inStoreInfo.customerName
        SHIPPING -> fulfillment.shippingInfo.customerName
        DRIVE_THROUGH -> fulfillment.driveThroughInfo.customerName  // new!
        CURBSIDE -> fulfillment.curbsideInfo.customerName           // new!
        KIOSK_DINE_IN -> fulfillment.kioskDineInInfo.customerName   // new!
        // ... every new type, every switch, every file
    }
}
```

### No Per-Platform Variation

The settings are keyed by `(merchant_id, fulfillment_type)` — there's no concept of platform or channel:

```
┌─────────────┐
│  Merchant A  │
│              │
│  PICKUP ──────► { prep_time: 15, requires_name: true }
│  DELIVERY ───► { prep_time: 30, requires_address: true }
│              │
└─────────────┘

But what we NEED:

┌─────────────┐
│  Merchant A  │
│              │
│  PICKUP      │
│   ├─ Web ────────► { show_scheduling: true, prep_time: 15 }
│   ├─ POS ────────► { show_scheduling: false, prep_time: 10 }
│   ├─ Kiosk ──────► { show_scheduling: false, prep_time: 5 }
│   └─ QR ─────────► { show_scheduling: true, prep_time: 15 }
│              │
└─────────────┘

❌ Not possible with the current model
```

## The Realization

> This architecture can't scale to where product needs to go.
> We don't need more enum values — we need a fundamentally different model.
> One that is **contextual**, **composable**, and **configuration-driven**.
