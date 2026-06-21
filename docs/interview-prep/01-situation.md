# Situation — The Before World

## Context

Our POS and Web platforms used two incompatible systems to describe how an order gets fulfilled:

```
┌─────────────────────────────────┐     ┌─────────────────────────────────┐
│           POS (Mobile)          │     │              Web                │
│                                 │     │                                 │
│   "Dining Options"              │     │   "Fulfillment Types"           │
│   ─────────────────             │     │   ─────────────────             │
│   • Freeform string labels      │     │   • Static enum values          │
│   • Merchant-defined            │     │   • Platform-defined            │
│   • No semantic meaning         │     │   • Hardcoded settings per type │
│                                 │     │                                 │
│   Examples:                     │     │   Values:                       │
│   "Dine In", "Eat Here",       │     │   PICKUP, DELIVERY,             │
│   "Take Out", "Para llevar"    │     │   IN_STORE, SHIPPING            │
│                                 │     │                                 │
└───────────────┬─────────────────┘     └───────────────┬─────────────────┘
                │                                       │
                ▼                                       ▼
        ┌───────────────┐                       ┌───────────────┐
        │    Orders      │                       │    Orders      │
        │  dining_option │                       │  fulfillment   │
        │  _id + label   │                       │  _type enum    │
        └───────────────┘                       └───────────────┘
                │                                       │
                └──────────────┐   ┌────────────────────┘
                               ▼   ▼
                    ┌─────────────────────┐
                    │     Reporting?       │
                    │                     │
                    │  "Dine In" = ???     │
                    │  PICKUP   = ???      │
                    │                     │
                    │  Cannot aggregate   │
                    │  across platforms    │
                    └─────────────────────┘
```

## The Dining Option Model (POS)

```protobuf
// Merchant creates whatever they want — no semantic meaning
message DiningOption {
    string id = 1;
    string label = 2;  // "Dine In", "Take Out", "Para llevar", anything
}

// On the order — just a nested dining option
message Order {
    DiningOption dining_option = 1;
    // ... rest of order
}
```

- Merchants freely create any dining option they like
- Client syncs down available options, user selects at checkout
- The label + ID get stamped on the order
- **No way to know that "Dine In" at Merchant A = "Eat Here" at Merchant B**

## The Fulfillment Type Model (Web)

```protobuf
enum FulfillmentType {
    PICKUP = 0;
    DELIVERY = 1;
    IN_STORE = 2;
    SHIPPING = 3;
}

// Settings — one blob per type, only one sub-message populated
message FulfillmentSettings {
    FulfillmentType type = 1;
    PickupSettings pickup_settings = 2;
    DeliverySettings delivery_settings = 3;
    ShippingSettings shipping_settings = 4;
    InStoreSettings in_store_settings = 5;
    // Only one populated based on type
}

// Order — per-type info objects with duplicated fields
message Fulfillment {
    FulfillmentType type = 1;
    PickupInfo pickup_info = 2;
    DeliveryInfo delivery_info = 3;
    ShippingInfo shipping_info = 4;
    InStoreInfo in_store_info = 5;
    // Only one populated based on type
}

// Each info type duplicates common fields
message PickupInfo {
    string customer_name = 1;
    string phone_number = 2;
    string pickup_at = 3;
}

message DeliveryInfo {
    string customer_name = 1;  // duplicated!
    string phone_number = 2;   // duplicated!
    Address delivery_address = 3;
}
```

## The Project I Joined

A cross-platform initiative to unify POS onto Fulfillment Types had been **in flight for 1.5 years** and appeared stalled. Teams involved: iOS, Android, web, and server.

**What I found:**
- No single owner or lead across the full project
- Work was not broken into discrete deliverables
- No one was accountable to specific outcomes
- No one could articulate how far along we were or when we'd deliver

**What I did:**
- Ramped by working closely with tech leads across all four teams
- Decomposed the project into ~8 workstreams with assigned DRIs:

```
Workstreams (each with an assigned DRI):
  - Pre-paid checkouts
  - Open-order checkouts
  - Order state management (paid and open orders)
  - Data migration (dining options → fulfillment types)
  - Reporting
  - Legacy POS systems
  - ...
```

We successfully delivered the unification project.

## The Eureka Moment

In the process of delivering, I realized the architecture we'd just unified onto was fundamentally broken. The team wasn't thinking about this — the focus was purely on shipping the foundation. But the enum-based fulfillment types with their static, per-type settings could not support where the product needed to go.

**Every code path needs to branch on the type:**

```kotlin
// This pattern is EVERYWHERE in the codebase
fun getCustomerName(fulfillment: Fulfillment): String {
    return when (fulfillment.type) {
        PICKUP -> fulfillment.pickupInfo.customerName
        DELIVERY -> fulfillment.deliveryInfo.customerName
        IN_STORE -> fulfillment.inStoreInfo.customerName
        SHIPPING -> fulfillment.shippingInfo.customerName
    }
}
```

> This architecture can't scale to where product needs to go.
> We don't need more enum values — we need a fundamentally different model.
