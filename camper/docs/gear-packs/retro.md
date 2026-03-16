# Gear Packs — Build Retrospective

## Build Summary

**What was built:** A gear pack template system that lets users browse predefined equipment bundles and bulk-apply them to a plan's shared gear list, with optional quantity scaling by group size.

**PRs in the stack (12):**

| # | Type | Title |
|---|------|-------|
| 1 | plan | feat(gear-packs): plan — gear pack template system |
| 2 | db | feat(gear-packs): db contracts — gear_packs and gear_pack_items tables |
| 3 | client | feat(gear-packs): client contracts — GearPackClient interface, params, models, fake |
| 4 | service | feat(gear-packs): service contracts — DTOs, errors, action stubs, controller stubs |
| 5 | db-impl | feat(gear-packs): db implementation — cooking equipment seed data |
| 6 | client-impl | feat(gear-packs): client implementation — JDBI operations, factory, fake |
| 7 | service-impl | feat(gear-packs): service implementation — actions, mapper, controller, config |
| 8 | webapp | feat(gear-packs): webapp — GearPacksPanel component, API methods, GearModal integration |
| 9 | client-test | feat(gear-packs): client tests — integration tests with Testcontainers |
| 10 | service-test | feat(gear-packs): service tests — unit tests for actions and validations |
| 11 | acceptance | feat(gear-packs): acceptance tests — end-to-end API tests |
| 12 | docs | feat(gear-packs): update documentation — CLAUDE.md files |

**New files:** ~50 across database, client, service, webapp, and tests

**Tests:** 48+ total (9 client integration, 22 service unit, 17 acceptance)

## Issues Found During Reviews

| Issue | Found By | Resolution |
|-------|----------|------------|
| **itemCount=0 in list endpoint** — `getAll` returned packs with empty items list, so `GearPackMapper.toSummaryResponse` computed `itemCount=0` | service-impl reviewer, acceptance tester, acceptance reviewer | Fixed `getAll` to also fetch items (2 queries in one JDBI handle, grouped by packId) |
| **Missing GearPackClientConfig** — Spring couldn't wire GearPackClient bean | service-dev | Created `config/GearPackClientConfig.kt` with factory function |
| **Duplicate/redundant client tests** | client-test-reviewer | Removed 2 redundant tests, added pack-level field assertions to getById |
| **Weak service test assertions** | service-test-reviewer | Added body assertions to manager test, ApplyFailed error path test, non-member authorization test |
| **Error tests missing body assertions** | acceptance-reviewer | Added error body assertions to all 5 error tests, added 2 missing error scenarios |
| **docker-java.properties needed** | test-engineer | Added `src/test/resources/docker-java.properties` with `api.version=1.44` |

## Plan vs Reality

The architect's plan was **highly accurate**. All entity definitions, API contracts, client interfaces, service actions, and webapp components matched the plan with minimal deviation.

**Key divergences:**
- **getAll items fetch:** Plan said "Fetches packs without items (items list empty)" but the service needed item counts for the summary response. Fixed by having getAll also fetch items.
- **GearPackClientConfig:** Plan specified it but it was missing from the contracts PR and had to be created during implementation.

## Recommendations for Future Builds

### Process
1. **Include config classes in contracts PRs** — `GearPackClientConfig` was missing from the service contracts PR. The architect should explicitly include all Spring `@Configuration` classes in the contracts PR.
2. **Specify getAll-with-counts pattern upfront** — When a list endpoint needs a computed field derived from a child relation (like `itemCount`), the plan should specify whether the client fetches children or uses a SQL COUNT.

### Testing
3. **Add `docker-java.properties` to client module template** — Every new client test module needs this file.
4. **FakeClient limitation:** Fakes don't support failure injection for `create` operations. Tested via validation-path failure instead. Consider adding `failNextOperation()` capability to fakes for more thorough error testing.

### Skills/Agents
5. **Shut down idle agents immediately** — Finished agents (db-dev, client-dev, service-dev, web-dev, reviewers) lingered as idle tmux panes long after their work was done. The orchestrator should send shutdown requests as soon as an agent's work is complete and reviewed, not wait until pane space runs out.
6. **Always save retro doc as a file** — The retro was initially only presented in conversation and not committed as `docs/<feature>/retro.md`. The orchestrator should always write and commit the retro document.
7. **Always create the retro follow-up PR** — Phase 10 (retro follow-up PR addressing recommendations) was skipped. This phase is mandatory per the orchestrator workflow.

## Architecture Observations

- **Template-as-migration pattern:** Gear packs are seeded via Flyway migration (V032), making them part of the schema. Appropriate for read-only reference data.
- **Acceptance tests depend on migration data:** `GearPackAcceptanceTest` relies on V032 seed data surviving `truncateAll()`. Sound pattern for reference data.
- **Clean feature isolation:** The gear pack feature touches only its own files plus `ResultExtensions.kt`, `settings.gradle.kts`, `build.gradle.kts`, and the GearModal integration point.
