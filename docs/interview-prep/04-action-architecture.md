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
│              1. Client Settings Service                               │
│                 We built this in partnership with the client           │
│                 platform team — writing a lot of the code.            │
│                                                                      │
│  • Receives request with context + settings domain identifier        │
│  • Builds DAG of data dependencies to load                           │
│  • Loads all required resources in dependency order                  │
│  • Calls settings handler to run business logic → build response     │
│  • Kafka feeds for change detection                                  │
│  • Push notifications for client cache invalidation                  │
│                                                                      │
└────────────────────────────────┬─────────────────────────────────────┘
                                 │
                                 ▼
┌──────────────────────────────────────────────────────────────────────┐
│         2. Hierarchical Settings Resolution Service                  │
│            We built this with the settings platform team —            │
│            writing a lot of the code.                                │
│                                                                      │
│  • Settings registered with resolution policies (defined in code)    │
│  • Each behaviour defines its own scope vector precedence            │
│  • Batch query for all contexts, return first matching result        │
│    based on the policy order                                         │
│  • Returns fully resolved settings for the given context             │
│  • Also returns audit info: query scope + matching scope             │
│                                                                      │
└────────────────────────────────┬─────────────────────────────────────┘
                                 │
                                 ▼
┌──────────────────────────────────────────────────────────────────────┐
│         3. Relationship-Entity Store (REDB / DynamoDB)                │
│            Already existed                                           │
│                                                                      │
│  • Graph-style relational entity storage                             │
│  • Existing sync protocol for mobile clients                         │
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
    // ... explicit named fields
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

Each behaviour registers its own resolution policy — an ordered list of scope vectors. Policies are defined statically in code in the contextual settings module:

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

All scope variants are batch-loaded from REDB in a single read. Resolution then happens in-memory — we walk the policy's scope vectors and return the first matching result. We also return audit information: the query scope and the matching scope.

```
Request: Get recipient_details for
         merchant=M1, location=L1, channel=POS

Step 0: Batch load ALL recipient_details entities for merchant M1
        → Returns entities at scopes: (M1), (M1,L1), (M1,POS), (M1,L1,POS), ...

Policy: [location+channel] → [channel] → [location] → [merchant]

Step 1: In-memory: find entity with scope (M1, L1, POS)
        ┌─────────────────────────┐
        │  (L1, POS) → FOUND     │  ← Most specific scope matches
        └─────────────────────────┘
        Return this value + audit: { query: (M1,L1,POS), match: (M1,L1,POS) }

─── OR if not defined at that scope: ───

Step 1: In-memory: find entity with scope (M1, L1, POS) → not found
Step 2: In-memory: find entity with scope (M1, POS)     → not found
Step 3: In-memory: find entity with scope (M1, L1)      → not found
Step 4: In-memory: find entity with scope (M1)           → FOUND
        ┌─────────────────────────┐
        │  Sparse merchant-level  │
        │  default                │
        └─────────────────────────┘
        Return this value + audit: { query: (M1,L1,POS), match: (M1) }

Result: Merchant defines once, all contexts inherit.
        Override only where behavior should differ.
        Audit trail shows exactly which scope won.
```

## Multi-Phase Data Loading

The client settings service orchestrates a multi-phase load via a ConfigManager and annotated data loaders. There are two types of annotated classes:

- **`@ConfigManager`** — orchestrates the final computed response (e.g., `CONTEXTUAL_FULFILLMENT_METHODS`)
- **`@SettingsDataLoader`** — a specialized data loader that leverages the hierarchical settings service. Each is annotated with a domain identifier, and `dependsOn` references the identifier of other data loaders.

### Dependency Chain

```
Phase 1: Load the PROFILE
          A list of fulfillment methods available for this context.
          (Lets us assign different fulfillment methods per context.)
              │
              ▼
Phase 2: Load the FULFILLMENT METHODS
          Each method contains references to its set of behaviours.
          We now know which behaviour settings we need.
              │
              ▼
Phase 3: Load all BEHAVIOUR SETTINGS
          All individual settings required by the fulfillment methods.
          Each setting is independently resolved via the hierarchical
          settings service for the given context.

All data is fetched BEFORE processing. The ConfigManager receives
all loaded data as resources, then builds the computed fulfillment
methods returned to the client.
```

### Code Example

```kotlin
// ConfigManager — builds the final computed response from pre-loaded data
@ConfigManager(domain = SettingsDomain.CONTEXTUAL_FULFILLMENT_METHODS)
class FulfillmentMethodConfigManager : ConfigHandler<List<FulfillmentMethod>> {

    override fun handle(context: SettingsContext, data: DataResourceMap): List<FulfillmentMethod> {
        val profile = data.get<FulfillmentMethodProfile>(SettingsDomain.FM_PROFILE)
        val methods = data.get<List<FulfillmentMethodEntity>>(SettingsDomain.FM_METHODS)
        val recipientDetails = data.get<ResolvedSetting<RecipientDetailsSettings>>(SettingsDomain.RECIPIENT_DETAILS)
        // ... all data already loaded and resolved

        // Build computed fulfillment methods from the pre-loaded data.
        // The resolved settings include both value and audit info
        // (query context + matching context), which lets us match
        // each setting result to its corresponding fulfillment method
        // (since FM ID is part of the query context).
        return methods.map { method ->
            composeFulfillmentMethod(method, recipientDetails, ...)
        }
    }
}

// SettingsDataLoader — fetches + resolves a setting via the hierarchical service
// The annotation wires up REDB fetching + resolution automatically.
// Resolution POLICY is defined in code in the contextual settings module.
@SettingsDataLoader(
    domain = SettingsDomain.RECIPIENT_DETAILS,
    redbReference = "com.acme.fulfillment.RecipientDetailsSettings",
    dependsOn = [SettingsDomain.FM_METHODS],
)
class RecipientDetailsDataLoader : ContextualSettingLoader<RecipientDetailsSettings>

@SettingsDataLoader(
    domain = SettingsDomain.GUEST_IDENTIFIER,
    redbReference = "com.acme.fulfillment.GuestIdentifierSettings",
    dependsOn = [SettingsDomain.FM_METHODS],
)
class GuestIdentifierDataLoader : ContextualSettingLoader<GuestIdentifierSettings>

@SettingsDataLoader(
    domain = SettingsDomain.FM_METHODS,
    redbReference = "com.acme.fulfillment.FulfillmentMethod",
    dependsOn = [SettingsDomain.FM_PROFILE],
)
class FulfillmentMethodDataLoader : ContextualSettingLoader<FulfillmentMethodEntity>

@SettingsDataLoader(
    domain = SettingsDomain.FM_PROFILE,
    redbReference = "com.acme.fulfillment.FulfillmentMethodProfile",
    dependsOn = [],  // loaded first — no dependencies
)
class FulfillmentMethodProfileDataLoader : ContextualSettingLoader<FulfillmentMethodProfile>
```

### Resolved Setting — Value + Audit

```kotlin
// Each resolved setting includes the value AND audit information
data class ResolvedSetting<T>(
    val value: T,
    val queryContext: Map<String, String>,    // what we queried for
    val matchingContext: Map<String, String>, // what scope actually matched
)

// This lets the ConfigManager match setting results back to
// their corresponding fulfillment method — since fulfillment_method_id
// is part of the query context.
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
