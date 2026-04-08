# Action Part 2 — The Architecture

## Three-Service Architecture

```
┌──────────────────────────────────────────────────────────────────────┐
│                        Client Request                                │
│         context: { merchant_id, location_id, channel_id }            │
│         domain:  CONTEXTUAL_FULFILLMENT_METHODS                      │
└────────────────────────────────┬─────────────────────────────────────┘
                                 │
                                 ▼
┌──────────────────────────────────────────────────────────────────────┐
│              1. Client Settings Service                              │
│                                                                      │
│  • Receives request with context + settings domain enum              │
│  • Builds DAG of dependencies from annotated loader classes          │
│  • Loads all required resources in dependency order                  │
│  • Calls loaders to run business logic → build response              │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────┐      │
│  │  @SettingsLoader(                                          │      │
│  │      domain = CONTEXTUAL_FULFILLMENT_METHODS,              │      │
│  │      dependsOn = [MERCHANT_INFO, LOCATION_INFO]            │      │
│  │  )                                                         │      │
│  │  class FulfillmentMethodLoader : Loader<...> { ... }       │      │
│  └────────────────────────────────────────────────────────────┘      │
└────────────────────────────────┬─────────────────────────────────────┘
                                 │
                                 ▼
┌──────────────────────────────────────────────────────────────────────┐
│         2. Hierarchical Settings Resolution Service                  │
│                      ⭐ NEW — we built this                          │
│                                                                      │
│  • Settings registered with resolution policies                      │
│  • Each behaviour defines its own scope vector precedence            │
│  • Resolves values by walking the hierarchy                          │
│  • Returns fully resolved settings for the given context             │
│                                                                      │
└────────────────────────────────┬─────────────────────────────────────┘
                                 │
                                 ▼
┌──────────────────────────────────────────────────────────────────────┐
│         3. Relationship-Entity Store (REDB / DynamoDB)                │
│                      📦 Already existed                              │
│                                                                      │
│  • Graph-style relational entity storage                             │
│  • Existing sync protocol for mobile clients                         │
│  • Kafka feeds for change detection                                  │
│  • Push notifications for client cache invalidation                  │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

## The New Data Model

### Fulfillment Method — The Entity

```protobuf
enum BehaviourType {
    RECIPIENT_DETAILS = 0;
    SCHEDULING = 1;
    PREP_TIME = 2;
    // ... composable set of capabilities
}

message FulfillmentMethod {
    string id = 1;                          // initially reused dining option ID
    string name = 2;
    repeated BehaviourType behaviours = 3;  // manifest of active behaviours
    RecipientDetailsSettings recipient_details = 4;
    SchedulingSettings scheduling = 5;
    PrepTimeSettings prep_time = 6;
    // each populated if listed in behaviours
}
```

### Behaviours — Composable Capabilities

```protobuf
message RecipientDetailsSettings {
    repeated FieldConfig fields = 1;        // which fields to collect
    GuestIdentifierSettings guest_identifier = 2;  // sub-behaviour
}

message FieldConfig {
    string field_name = 1;
    bool required = 2;
    bool visible = 3;
}

// Generic guest identifier — powers drive-thru car details,
// delivery notes, etc. without type-specific code
message GuestIdentifierSettings {
    string client_id = 1;
    string value = 2;
    string display_text = 3;
}

message SchedulingSettings {
    bool enabled = 1;
    int32 min_lead_time_minutes = 2;
    int32 max_days_ahead = 3;
    // ...
}
```

### Key Insight: EAV Pattern

```
┌─────────────────────────────────────────────────────────┐
│                  EAV Analogy                             │
│                                                          │
│  Entity     →  Fulfillment Method (the container)        │
│  Attribute  →  Behaviour (recipient_details, scheduling) │
│  Value      →  Settings stored against a context scope   │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

## Context-Scoped Resolution

### The Context Object

```protobuf
message SettingsContext {
    string merchant_id = 1;
    string location_id = 2;
    string channel_id = 3;
    string fulfillment_method_id = 4;
}
```

### Hierarchical Resolution — Per Behaviour

Each behaviour registers its own resolution policy — an ordered list of scope vectors:

```kotlin
// Resolution policy: ordered list of scope vectors to check
// First match wins (most specific → least specific)

val recipientDetailsPolicy = ResolutionPolicy(
    scopes = listOf(
        listOf(LOCATION_ID, CHANNEL_ID),  // most specific: this location + this channel
        listOf(CHANNEL_ID),                // channel-wide default
        listOf(LOCATION_ID),               // location-wide default
        listOf(MERCHANT_ID)                // merchant-wide fallback
    )
)

val brandingPolicy = ResolutionPolicy(
    scopes = listOf(
        listOf(MERCHANT_ID),               // top-down: merchant sets the brand
        listOf(LOCATION_ID),               // location can override
        listOf(LOCATION_ID, CHANNEL_ID)    // most specific override
    )
)
```

### Resolution Flow

```
Request: Get recipient_details for
         merchant=M1, location=L1, channel=POS

Policy: [location+channel] → [channel] → [location] → [merchant]

Step 1: Look up recipient_details @ (L1, POS)
        ┌─────────────────────────┐
        │  (L1, POS) → FOUND ✓   │  ← Most specific scope matches
        └─────────────────────────┘
        Return this value.

─── OR if not defined at that scope: ───

Step 1: Look up recipient_details @ (L1, POS)
        ┌─────────────────────────┐
        │  (L1, POS) → not found  │
        └─────────────────────────┘

Step 2: Look up recipient_details @ (POS)
        ┌─────────────────────────┐
        │  (POS)     → not found  │
        └─────────────────────────┘

Step 3: Look up recipient_details @ (L1)
        ┌─────────────────────────┐
        │  (L1)      → not found  │
        └─────────────────────────┘

Step 4: Look up recipient_details @ (M1)
        ┌─────────────────────────┐
        │  (M1)      → FOUND ✓   │  ← Sparse merchant-level default
        └─────────────────────────┘
        Return this value.

Result: Merchant defines once, all contexts inherit.
        Override only where behavior should differ.
```

## Annotation-Based Loader Registration

### Settings Loader — DAG Dependencies

```kotlin
// General resource loader — declares dependencies by enum reference
// The Client Settings Service builds a DAG and loads in order
@SettingsLoader(
    domain = SettingsDomain.CONTEXTUAL_FULFILLMENT_METHODS,
    dependsOn = [SettingsDomain.MERCHANT_INFO, SettingsDomain.LOCATION_INFO]
)
class FulfillmentMethodLoader : Loader<FulfillmentMethodsResponse> {

    override fun load(context: SettingsContext, dependencies: DependencyMap): FulfillmentMethodsResponse {
        val merchant = dependencies.get<MerchantInfo>(SettingsDomain.MERCHANT_INFO)
        val location = dependencies.get<LocationInfo>(SettingsDomain.LOCATION_INFO)
        // ... build response using resolved settings
    }
}
```

### Registered Setting — REDB-Backed with Built-In Resolution

```kotlin
// For contextual settings backed by REDB — annotation wires up
// all resolution internals automatically
@RegisteredSetting(
    redbReference = "recipient_details_v1",
    resolutionPolicy = recipientDetailsPolicy
)
class RecipientDetailsSetting : ContextualSetting<RecipientDetailsSettings>

// Complex behaviours: adapter composes multiple registered settings
@SettingsLoader(
    domain = SettingsDomain.CONTEXTUAL_FULFILLMENT_METHODS,
    dependsOn = [SettingsDomain.MERCHANT_INFO]
)
class RecipientDetailsBehaviourLoader : Loader<RecipientDetailsBehaviour> {

    @Inject lateinit var recipientDetails: RecipientDetailsSetting
    @Inject lateinit var guestIdentifier: GuestIdentifierSetting

    override fun load(context: SettingsContext, dependencies: DependencyMap): RecipientDetailsBehaviour {
        val details = recipientDetails.resolve(context)
        val guest = guestIdentifier.resolve(context)
        return RecipientDetailsBehaviourAdapter.adapt(details, guest)
    }
}
```

## Composability in Action: Drive-Through Example

```
┌──────────────────────────────────────────────────────────┐
│  Fulfillment Method: "Drive Through"                      │
│                                                            │
│  Behaviours:                                               │
│  ├── recipient_details                                     │
│  │   └── guest_identifier: { display_text: "Car Details" } │
│  │       (generic field → specific UX)                     │
│  ├── prep_time: { default: 5 min }                         │
│  ├── order_status_tracking: { show_window_number: true }   │
│  └── scheduling: { enabled: false }                        │
│                                                            │
│  Context overrides:                                        │
│  ├── Merchant M1 (all locations):                          │
│  │   └── prep_time: { default: 7 min }                     │
│  ├── Location L1, Channel POS:                             │
│  │   └── prep_time: { default: 3 min }  ← override        │
│  └── Location L2:                                          │
│       └── (inherits merchant default → 7 min)              │
│                                                            │
└──────────────────────────────────────────────────────────┘

No DRIVE_THROUGH enum value.
No DriveThroughSettings proto.
No DriveThroughInfo on the order.
No new switch statement branches.

Just a fulfillment method with composed behaviours
and context-scoped configuration.
```

## Controlled Rollout: Templates + Constrained Admin

```
┌──────────────────────────────────────────────────────────┐
│             Full system capability                        │
│  ┌────────────────────────────────────────────────────┐  │
│  │  Any behaviour, any scope, any combination          │  │
│  │                                                      │  │
│  │  ┌────────────────────────────────────────────────┐  │  │
│  │  │  What merchants actually see (v1)              │  │  │
│  │  │                                                │  │  │
│  │  │  • Pre-canned fulfillment method templates     │  │  │
│  │  │  • Sensible defaults for each behaviour        │  │  │
│  │  │  • Explicit admin controls for select settings │  │  │
│  │  │  • No need to understand the EAV model         │  │  │
│  │  │                                                │  │  │
│  │  └────────────────────────────────────────────────┘  │  │
│  └────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────┘

Full flexibility under the hood.
Controlled surface area for merchants.
Expand admin exposure as merchants are ready.
```
