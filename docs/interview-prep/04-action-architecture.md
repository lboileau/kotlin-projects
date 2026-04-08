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
    string customer_name = 1;
    string phone_number = 2;
    string email = 3;
    bool customer_name_required = 4;
    bool phone_number_required = 5;
    // ... explicit named fields, not generic FieldConfig
    GuestIdentifierSettings guest_identifier = 10;  // sub-behaviour
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

All scope variants are batch-loaded from REDB in a single read. Resolution then happens in-memory — we walk the policy's scope vectors and return the first match:

```
Request: Get recipient_details for
         merchant=M1, location=L1, channel=POS

Step 0: Batch load ALL recipient_details entities for merchant M1
        → Returns entities at scopes: (M1), (M1,L1), (M1,POS), (M1,L1,POS), ...

Policy: [location+channel] → [channel] → [location] → [merchant]

Step 1: In-memory: find entity with scope (M1, L1, POS)
        ┌─────────────────────────┐
        │  (L1, POS) → FOUND ✓   │  ← Most specific scope matches
        └─────────────────────────┘
        Return this value.

─── OR if not defined at that scope: ───

Step 1: In-memory: find entity with scope (M1, L1, POS) → not found
Step 2: In-memory: find entity with scope (M1, POS)     → not found
Step 3: In-memory: find entity with scope (M1, L1)      → not found
Step 4: In-memory: find entity with scope (M1)           → FOUND ✓
        ┌─────────────────────────┐
        │  Sparse merchant-level  │
        │  default                │
        └─────────────────────────┘
        Return this value.

Result: Merchant defines once, all contexts inherit.
        Override only where behavior should differ.
```

## Annotation-Based Loader Registration

### DAG Dependency Flow

The dependency graph flows:
**raw setting loaders → fulfillment methods → fulfillment methods profile (collection)**

```kotlin
// General resource loader — declares dependencies by enum reference
// The Client Settings Service builds a DAG and loads in order
@SettingsLoader(
    domain = SettingsDomain.CONTEXTUAL_FULFILLMENT_METHODS,
    dependsOn = [SettingsDomain.MERCHANT_INFO, SettingsDomain.LOCATION_INFO,
                 SettingsDomain.RECIPIENT_DETAILS, SettingsDomain.SCHEDULING]
)
class FulfillmentMethodLoader : Loader<FulfillmentMethodsResponse> {

    override fun load(context: SettingsContext, dependencies: DependencyMap): FulfillmentMethodsResponse {
        val merchant = dependencies.get<MerchantInfo>(SettingsDomain.MERCHANT_INFO)
        val recipientDetails = dependencies.get<RecipientDetailsSettings>(SettingsDomain.RECIPIENT_DETAILS)
        // ... compose behaviours from pre-loaded settings
    }
}
```

### Registered Setting — REDB-Backed with Built-In Resolution

```kotlin
// For contextual settings backed by REDB — annotation wires up
// REDB fetching + deserialization automatically.
// The resolution POLICY lives in the contextual settings module (in code),
// not in the annotation — policies are defined statically alongside the
// setting definitions.
@RegisteredSetting(
    redbReference = "com.acme.fulfillment.RecipientDetailsSettings",  // full proto descriptor
)
class RecipientDetailsSetting : ContextualSetting<RecipientDetailsSettings>

// Complex behaviours: adapter composes multiple registered settings
@SettingsLoader(
    domain = SettingsDomain.RECIPIENT_DETAILS_BEHAVIOUR,
    dependsOn = [SettingsDomain.RECIPIENT_DETAILS, SettingsDomain.GUEST_IDENTIFIER]
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

The client never sees scoping — it just gets the resolved values for its context:

```
Client request: { merchant: M1, location: L1, channel: POS }

Response: Fulfillment Method "Drive Through"
┌──────────────────────────────────────────────────────────┐
│  Behaviours (fully resolved for this context):            │
│  ├── recipient_details                                    │
│  │   └── guest_identifier: { display_text: "Car Details" }│
│  │       (generic field → specific UX)                    │
│  ├── prep_time: { default: 3 min }                        │
│  │   (resolved: L1+POS override won over merchant default)│
│  ├── order_status_tracking: { show_window_number: true }  │
│  └── scheduling: { enabled: false }                       │
└──────────────────────────────────────────────────────────┘

Behind the scenes (invisible to client):
  Merchant M1 default:    prep_time = 7 min
  Location L1 + POS:      prep_time = 3 min  ← winning scope
  Location L2:            (no override → inherits 7 min)
```

No DRIVE_THROUGH enum value.
No DriveThroughSettings proto.
No DriveThroughInfo on the order.
No new switch statement branches.

Just a fulfillment method with composed behaviours
and context-scoped configuration.

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
