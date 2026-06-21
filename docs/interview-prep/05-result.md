# Result — Impact

## Feature Velocity: Before vs. After

```
┌────────────────────────────────────────────────────────────────────┐
│                                                                    │
│  BEFORE: Enum-based architecture                                   │
│  Adding a new fulfillment flow requires:                           │
│                                                                    │
│    1. New enum value                                               │
│    2. New settings proto sub-message                               │
│    3. New order info proto sub-message                             │
│    4. Update every switch statement across the codebase            │
│    5. Duplicated field definitions                                 │
│    6. Cross-team coordination for each addition                    │
│                                                                    │
│  Timeline: weeks to months per new fulfillment type                │
│                                                                    │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│  AFTER: Composable config-based architecture                       │
│  Adding a new fulfillment flow requires:                           │
│                                                                    │
│    1. Create a new fulfillment method (config, not code)           │
│    2. Select which behaviours apply                                │
│    3. Set defaults per context scope                               │
│    4. Done — no new enums, protos, or switch statements            │
│                                                                    │
│  Timeline: days, often hours                                       │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

## What We Delivered

```
                        Original Plan                  Actual
                     ┌──────────────────────────────────────────────┐
                     │                                              │
  H1 (6 months)     │  • Drive-through feature        ✅ Delivered │
                     │  • Web delivery + scheduling    ✅ Delivered │
                     │                                              │
                     ├──────────────────────────────────────────────┤
                     │                                              │
  H2 (6 months)     │  • Scheduled ordering on POS    ✅ Started   │
                     │  • Kiosk product                ✅ Started   │
                     │  • QR code ordering             ✅ Started   │
                     │                                              │
                     └──────────────────────────────────────────────┘

     A quarter's worth of features delivered in a month.
     H2 work pulled forward into H1.
```

## Why It Worked

```
┌───────────────────────────────┬──────────────────────────────────────┐
│  Before (Enum)                │  After (Composable)                  │
├───────────────────────────────┼──────────────────────────────────────┤
│  New type = code change       │  New method = configuration          │
│  Settings are monolithic      │  Settings are composable behaviours  │
│  One size per merchant        │  Per location, channel, combination  │
│  Duplicated fields            │  Shared behaviours                   │
│  Switch statements everywhere │  Behaviours resolve generically      │
│  Platform-blind               │  Context-aware                       │
│  Enum explosion with scale    │  Scales compositionally              │
└───────────────────────────────┴──────────────────────────────────────┘
```

## Key Takeaways

1. **Organizational clarity unlocks delivery** — the initial project was stalled for 1.5 years. Breaking it into workstreams with DRIs got it across the line.

2. **Build to convince** — the POC running alongside socialization turned abstract architecture discussions into concrete demonstrations.

3. **Don't block, enable** — committing to not slowing down integrating teams earned buy-in and trust.

4. **Design for the roadmap, not just today** — the enum model worked for 4 types. The composable model works for N types across M contexts without code changes.

5. **Control the surface area** — full flexibility under the hood, but merchants only see what they're ready for (templated methods, constrained admin).
