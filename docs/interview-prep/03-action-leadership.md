# Action Part 1 — Driving the Rebuild

## Timeline

```
 Unify POS         Architect          POC              Socialize &         Productionize      Partner on
 onto Fulfillment  New Solution       Build            Educate Teams       Core               Features
 Types                                                                                        
 ────●──────────────●──────────────────●────────────────●───────────────────●───────────────────●──────►
     │              │                  │                │                   │                   │
     │              │                  │                │                   │                   │
     ▼              ▼                  ▼                ▼                   ▼                   ▼
 Delivered the    Worked with        Core resolution  "Show don't tell"  Clean up code,      Drive-through,
 initial          settings tech      engine +         — demo'd POC       improve patterns,   web delivery,
 unification.     lead. Company      management       while socializing. testing, real        scheduled orders,
 Realized the     ADR review.        APIs + example   Worked with        behaviour pass      kiosk, QR
 architecture                        behaviours +     integrating team   for target           ordering
 was broken.                         admin GUI.       tech leads.        features.
                                                      Did not block
                                                      their roadmaps.
```

## Strategy: Build Conviction Through Demonstration

The key challenge wasn't just technical — it was organizational. I needed buy-in to rebuild a system that had just taken 1.5 years to ship.

**Approach:**
1. **Architect with credibility** — partnered with the tech lead of the existing settings system, not around them
2. **Company ADR** — went through formal architecture decision review for visibility and alignment
3. **Build the POC aggressively** — built in parallel with socialization so conversations were concrete, not theoretical
4. **Embed with integrating teams** — worked closely with tech leads of drive-through, web, and other teams to bring them along
5. **Clear contract: don't slow them down** — committed to not blocking existing team roadmaps while introducing the new platform

## What the POC Included

```
┌──────────────────────────────────────────────────┐
│                    POC Scope                      │
│                                                  │
│  ┌────────────────────────────────────────────┐  │
│  │  Core Resolution Engine                    │  │
│  │  • Context-based settings lookup           │  │
│  │  • Hierarchical scope resolution           │  │
│  │  • Per-behaviour resolution policies       │  │
│  └────────────────────────────────────────────┘  │
│                                                  │
│  ┌────────────────────────────────────────────┐  │
│  │  Management APIs                           │  │
│  │  • CRUD for fulfillment methods            │  │
│  │  • Scope-aware settings management         │  │
│  └────────────────────────────────────────────┘  │
│                                                  │
│  ┌────────────────────────────────────────────┐  │
│  │  Example Behaviours                        │  │
│  │  • Demonstrated composability              │  │
│  └────────────────────────────────────────────┘  │
│                                                  │
│  ┌────────────────────────────────────────────┐  │
│  │  Admin GUI                                 │  │
│  │  • Manage settings across scopes           │  │
│  │  • Visualize inheritance + resolution      │  │
│  │  • "Show don't tell" for stakeholders      │  │
│  └────────────────────────────────────────────┘  │
│                                                  │
└──────────────────────────────────────────────────┘
```
