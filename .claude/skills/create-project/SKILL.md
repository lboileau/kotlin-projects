---
name: create-project
description: Bootstrap a complete Kotlin project from scratch with Gradle, services, clients, and databases. Use when the user wants to create a new project.
user-invocable: true
---

# Project Bootstrap

You are the **orchestrator** bootstrapping a complete Kotlin project from scratch. You handle root-level project setup and Gradle wrapper yourself, and delegate all code generation to specialized agent teammates. After each teammate delivers, you run a review cycle before moving on.

## Critical Rules

1. **Never reference other projects.** Skills are the sole source of truth for code generation. Never copy from sibling projects.
2. **Always run agents in foreground.** Never set `run_in_background: true`. Background agents cannot prompt for permissions and will silently fail.
3. **Every module gets reviewed.** After each agent delivers code, spawn `code-reviewer` (or `test-reviewer` for tests). Fix cycle until approved.
4. **Build after every step.** Run module or full build. Nothing moves forward until green.

## Agent Team

| Agent | Purpose | When Spawned |
|-------|---------|-------------|
| `kotlin-dev` | Creates clients, libs, services, common modules | Steps 2a-2c, 2e-2f |
| `db-dev` | Creates database schemas, migrations, seeds | Step 2d |
| `test-engineer` | Creates test infrastructure and acceptance tests | Step 2f (tests) |
| `code-reviewer` | Reviews each module against patterns and conventions | After each code step |
| `test-reviewer` | Reviews tests for quality, coverage, authenticity | After test step |

## Review Cycle Protocol

After each agent delivers code:

1. **Build check** — Run `./gradlew :<module>:build`. Must pass.
2. **Spawn reviewer** — `code-reviewer` for implementation, `test-reviewer` for tests. Provide the list of files created.
3. **If CHANGES REQUESTED** — Spawn the developer back with the reviewer's feedback. Rebuild. Re-review. Loop until clean.
4. **If APPROVED** — Move to next step.

## How Delegation Works

Spawn agent teammates using the Agent tool. Each teammate has skills pre-loaded (see `.claude/agents/`). Provide:
1. What to build (module name, purpose, all field/type details)
2. The project root path and root package
3. Any prior reviewer feedback (if re-doing after review)
4. Instruction to write all files without asking questions

---

## Command: Create Project

### Gather Information

Ask the user for:
1. **Project name** (e.g., `hello-world`, `my-app`) — used for directory names and service naming
2. **Root package** (e.g., `com.example`, `com.acme`) — the base Kotlin package
3. **Target directory** — where to create the project root (default: current directory)

Once gathered, everything else is pre-configured. No further prompts needed.

### Entity: World

The sample entity used throughout all layers:

| Layer | Name | Fields |
|-------|------|--------|
| DB table | `worlds` | `name VARCHAR(100) NOT NULL UNIQUE`, `greeting VARCHAR(255) NOT NULL` |
| Client model | `World` | `id: UUID`, `name: String`, `greeting: String`, `createdAt: Instant`, `updatedAt: Instant` |
| Client params | `GetByIdParam` | `id: UUID` |
| | `GetListParam` | `limit: Int?`, `offset: Int?` |
| | `CreateWorldParam` | `name: String`, `greeting: String` |
| | `UpdateWorldParam` | `id: UUID`, `name: String?`, `greeting: String?` |
| | `DeleteWorldParam` | `id: UUID` |
| Service model | `World` | Same as client (adapted at boundary) |
| Service params | `GetWorldByIdParam` | `id: UUID` |
| | `GetAllWorldsParam` | `limit: Int?`, `offset: Int?` |
| | `CreateWorldParam` | `name: String`, `greeting: String` |
| | `UpdateWorldParam` | `id: UUID`, `name: String?`, `greeting: String?` |
| | `DeleteWorldParam` | `id: UUID` |
| DTOs | `CreateWorldRequest` | `name: String`, `greeting: String` |
| | `UpdateWorldRequest` | `name: String?`, `greeting: String?` |
| | `WorldResponse` | `id: String`, `name: String`, `greeting: String`, `createdAt: String`, `updatedAt: String` |
| API routes | | `GET /api/worlds/{id}`, `GET /api/worlds`, `POST /api/worlds`, `PUT /api/worlds/{id}`, `DELETE /api/worlds/{id}` |

### Naming Conventions

Derive these from the user inputs:
- `<project>` = project name (e.g., `hello-world`)
- `<pkg>` = root package as path (e.g., `com/example`)
- `<pkg.dot>` = root package as dotted (e.g., `com.example`)
- `<db-name>` = `<project>-db` (e.g., `hello-world-db`)
- `<db_name>` = `<project>_db` with hyphens to underscores (e.g., `hello_world_db`)
- `<service-name>` = first word of project + `-service` (e.g., `hello-service`)
- `<svc-pkg>` = first word of project (e.g., `hello`) — the service package segment
- DB port: `5433` (check existing `docker-compose.yml` files in `databases/` to avoid conflicts)
- Service port: `8080`

---

### Execution Steps

#### Step 1: Project Root (you do this directly)

Create the project root directory `<target>/<project>/` with these files:

##### `settings.gradle.kts`
```kotlin
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

rootProject.name = "<project>"

include(":libs:common")
include(":clients:common")
project(":clients:common").name = "client-common"
include(":clients:world-client")
include(":services:common")
project(":services:common").name = "service-common"
include(":services:<service-name>")
include(":databases:<db-name>")
```

##### `build.gradle.kts`
```kotlin
plugins {
    kotlin("jvm") version "2.1.10" apply false
    kotlin("plugin.spring") version "2.1.10" apply false
    id("org.springframework.boot") version "3.4.3" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

allprojects {
    group = "<pkg.dot>"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    plugins.withType<org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper> {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
            jvmToolchain(21)
            compilerOptions {
                freeCompilerArgs.add("-Xjsr305=strict")
            }
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
```

##### `gradle.properties`
```properties
kotlin.code.style=official
org.gradle.jvmargs=-Xmx1024m
org.gradle.parallel=true
org.gradle.java.installations.auto-download=true
```

##### `.gitignore`
```
# Gradle
.gradle/
build/
!gradle/wrapper/gradle-wrapper.jar

# IDE
.idea/
*.iml
*.ipr
*.iws
.vscode/

# OS
.DS_Store

# Kotlin
*.class
```

##### `Dockerfile`
```dockerfile
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradle gradle
COPY gradlew build.gradle.kts settings.gradle.kts gradle.properties ./
COPY libs libs
COPY clients clients
COPY services services
RUN ./gradlew :services:<service-name>:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/services/<service-name>/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

##### Gradle Wrapper
Generate the Gradle 8.12 wrapper:
```bash
cd <target>/<project> && gradle wrapper --gradle-version 8.12
```
This creates `gradlew`, `gradlew.bat`, and `gradle/wrapper/` with `gradle-wrapper.jar` and `gradle-wrapper.properties`.

---

#### Step 2: Delegate to Agent Teammates

Spawn agent teammates (in **foreground**, never background) to create the remaining modules. After each teammate delivers, run the review cycle (build → review → fix → re-review) before proceeding.

Steps 2a-2d have no dependencies on each other. Steps 2e-2f depend on earlier steps.

##### Step 2a: Client Common

Spawn `kotlin-dev`:
- **Task:** Client Common Bootstrap
- **Root package:** `<pkg.dot>`
- **Project root:** `<target>/<project>/`
- **Instruction:** Create the `clients/common/` module with Result.kt, ClientContext.kt, error types, and ResultTest.kt

Build check → Spawn `code-reviewer` → review cycle.

##### Step 2b: Service Common

Spawn `kotlin-dev`:
- **Task:** Service Common Bootstrap
- **Root package:** `<pkg.dot>`
- **Project root:** `<target>/<project>/`
- **Instruction:** Create the `services/common/` module with ApiResponse.kt and ApiResponseTest.kt

Build check → Spawn `code-reviewer` → review cycle.

##### Step 2c: Common Library

Spawn `kotlin-dev`:
- **Task:** Common Library Bootstrap
- **Root package:** `<pkg.dot>`
- **Project root:** `<target>/<project>/`
- **Instruction:** Create the `libs/common/` module with logging utilities and CLAUDE.md

Build check → Spawn `code-reviewer` → review cycle.

##### Step 2d: Database

Spawn `db-dev`:
- **Task:** Create DB
- **Database name:** `<db-name>`
- **Port:** `5433` (or next available)
- **Tables:** `worlds` with columns: `name VARCHAR(100) NOT NULL` (UNIQUE constraint `uq_worlds_name`), `greeting VARCHAR(255) NOT NULL`. Index: `idx_worlds_name` on `name`.
- **Seed data:** 3 rows — Earth, Mars, Vulcan with greetings and fixed UUIDs
- **Project root:** `<target>/<project>/`

Build check → Spawn `code-reviewer` → review cycle.

##### Step 2e: World Client (after 2a completes)

Spawn `kotlin-dev`:
- **Task:** Create Client
- **Client name:** `world-client`
- **Purpose:** Data access client for the worlds table CRUD operations
- **Root package:** `<pkg.dot>`
- **Database:** `<db-name>` (port `5433`, database `<db_name>`)
- **Public API:** getById, getList, create, update, delete for World entity
- **Model fields:** `id: UUID`, `name: String`, `greeting: String`, `createdAt: Instant`, `updatedAt: Instant`
- **Param objects:** `GetByIdParam(id)`, `GetListParam(limit?, offset?)`, `CreateWorldParam(name, greeting)`, `UpdateWorldParam(id, name?, greeting?)`, `DeleteWorldParam(id)`
- **Validation checks:**
  - `ValidateCreateWorld`: name must not be blank, greeting must not be blank
  - `ValidateUpdateWorld`: name must not be blank (if provided), greeting must not be blank (if provided)
  - `ValidateGetWorldById`, `ValidateGetWorldList`, `ValidateDeleteWorld`: default (return `success(Unit)`)
- **FakeWorldClient:** Must reference the actual validation classes from `internal/validations/`
- **Project root:** `<target>/<project>/`

Build check → Spawn `code-reviewer` → review cycle.

##### Step 2f: Service (after 2a, 2b, 2e complete)

Spawn `kotlin-dev`:
- **Task:** Create API Service
- **Service name:** `<service-name>`
- **Purpose:** API service for world CRUD
- **Root package:** `<pkg.dot>`
- **Initial features:** `world`
- **Clients consumed:** `world-client`
- **Port:** `8080`
- **Database:** `<db-name>` (port `5433`, database `<db_name>`)
- **Project root:** `<target>/<project>/`
- **Key patterns:** `WorldMapper.fromClient()`, `GlobalExceptionHandler` catches `RuntimeException::class`
- **World feature details:**
  - Model: `World(id, name, greeting, createdAt, updatedAt)`
  - DTOs: `CreateWorldRequest(name, greeting)`, `UpdateWorldRequest(name?, greeting?)`, `WorldResponse(id, name, greeting, createdAt, updatedAt)`
  - Error: `WorldError` sealed class with typed fields (`NotFound(entityId)`, `AlreadyExists(name)`, `Invalid(field, reason)`) + `fromClientError()` companion
  - Service params: `GetWorldByIdParam(id)`, `GetAllWorldsParam(limit?, offset?)`, `CreateWorldParam(name, greeting)`, `UpdateWorldParam(id, name?, greeting?)`, `DeleteWorldParam(id)`
  - Service validations (1:1 with actions):
    - `ValidateCreateWorld`: name must not be blank, greeting must not be blank (returns `WorldError.Invalid`)
    - `ValidateUpdateWorld`: name must not be blank (if provided), greeting must not be blank (if provided)
    - `ValidateGetWorldById`, `ValidateGetAllWorlds`, `ValidateDeleteWorld`: default (return `success(Unit)`)
  - Actions: `GetWorldByIdAction`, `GetAllWorldsAction`, `CreateWorldAction`, `UpdateWorldAction`, `DeleteWorldAction` — each validates, converts to client param (using import alias), calls client
  - Service: `WorldService` facade delegating to actions (no `@Service` annotation)
  - Wiring: `WorldService(worldClient)` called directly in `WorldServiceConfig` `@Configuration` bean (no separate factory function)
  - Controller: Creates service param objects from request DTOs/path variables, uses `Result.toResponseEntity()` extension
  - ResultExtensions: `WorldError.toResponseEntity()` + `Result<T, WorldError>.toResponseEntity()` extension
  - Routes: `GET /api/worlds/{id}`, `GET /api/worlds`, `POST /api/worlds`, `PUT /api/worlds/{id}`, `DELETE /api/worlds/{id}`

Build check → Spawn `code-reviewer` → review cycle.

##### Step 2g: Acceptance Tests (after 2f complete)

Spawn `test-engineer`:
- **Task:** Create Acceptance Tests
- **Service:** `<service-name>`
- **Features to test:** `world`
- **DB schemas:** `worlds` table from `<db-name>`
- **Root package:** `<pkg.dot>`
- **Project root:** `<target>/<project>/`
- **Endpoints to cover:** all 5 world routes
- **Include:** read-your-own-writes tests (POST then GET, POST then PUT then GET, POST then DELETE then GET)

Build check → Spawn `test-reviewer` → review cycle.

---

#### Step 3: Documentation (you do this directly)

After all teammates complete and reviews pass, create these files:

##### Root `CLAUDE.md`

Document the project with these sections:
- **Overview** — one-line description
- **Project Structure** — directory tree showing all modules
- **Module Layout** — explain clients/, libs/, services/, databases/ and their roles
- **Tech Stack** — Gradle, Kotlin, Spring Boot, JDBI, Flyway, Testcontainers versions
- **Architecture** — dependency direction, DI approach, error handling, testing strategy, client patterns (facade, param objects, client context)
- **Key Conventions** — root package, package naming for each module type, DB naming, Kotlin naming, client interface + fake pattern
- **Quick Start** — docker-compose, flyway, seed, bootRun commands

##### `start.sh` and `stop.sh`

Create convenience scripts at the project root:

**`start.sh`** — Starts the database (docker compose), waits for readiness, runs Flyway migrations, seeds dev data, starts the API service and webapp (if present) in the background, waits for the webapp to be ready, opens the browser, then waits (Ctrl+C to stop). Uses a trap to clean up background processes on exit.

**`stop.sh`** — Kills running service and webapp processes (`pkill -f`), stops the database container (`docker compose down`).

Both scripts should be executable (`chmod +x`).

##### Root `README.md`

Structure with these sections:
- **Quick Start** — `./start.sh` to bring everything up (opens browser), `./stop.sh` to tear down
- **Prerequisites** — JDK 21+, Docker, Node.js (if webapp exists), Flyway CLI, psql
- **Project Structure** — directory tree showing all modules
- **Manual Setup** — step-by-step: docker-compose, flyway migrate, seed, bootRun, npm run dev
- **API** — endpoints table with method, path, description
- **Testing** — `./gradlew test`, per-module commands, `./gradlew clean build`
- **Docker** — build and run commands with env vars

---

### Verification Checklist

After completing all steps, verify:

- [ ] `settings.gradle.kts` includes all six modules (libs, clients/common, clients/world-client, services/common, services/<service-name>, databases/<db-name>)
- [ ] `build.gradle.kts` declares plugins with `apply false`, sets `jvmToolchain(21)`
- [ ] `gradle.properties` has `auto-download=true`
- [ ] Gradle wrapper (gradlew, gradle-wrapper.jar, gradle-wrapper.properties) exists
- [ ] `clients/common/` has Result.kt (with `@UnsafeVariance`), ClientContext.kt, error types (AppError is `interface` not `sealed`)
- [ ] `services/common/` has ApiResponse.kt with factory methods
- [ ] `libs/common/` has logging utilities
- [ ] `databases/<db-name>/` has schema, migration, rollback, seed, docker-compose, flyway.conf
- [ ] `databases/<db-name>/` is a Gradle module with `MigrationRunner` utility
- [ ] `clients/world-client/` testFixtures has `WorldTestDb` wrapping `MigrationRunner`
- [ ] `clients/world-client/` has interface with KDoc, param objects, facade, operations, adapters, factory (no params, reads env vars), fake (in testFixtures), model, tests
- [ ] `clients/world-client/` has validation classes 1:1 with operations (`ValidateCreateWorld` with checks, others default)
- [ ] `clients/world-client/` operations call validators before executing
- [ ] `clients/world-client/` FakeWorldClient references actual validation classes from `internal/validations/`
- [ ] `clients/world-client/` has `docker-java.properties` with `api.version=1.44`
- [ ] `services/<service-name>/` has Spring Boot app, configs, controller, DTOs, mapper, error handling
- [ ] `services/<service-name>/` has service param objects in `features/world/params/`
- [ ] `services/<service-name>/` has validation classes 1:1 with actions (`ValidateCreateWorld`/`ValidateUpdateWorld` with checks, others default)
- [ ] `services/<service-name>/` has action classes 1:1 with service methods (validate → convert params → call client)
- [ ] `services/<service-name>/` has `WorldService` facade (no `@Service`) + `WorldServiceConfig` (no separate factory)
- [ ] `services/<service-name>/` has `WorldError` with typed fields + `fromClientError()` companion
- [ ] `services/<service-name>/` has `ResultExtensions.kt` with both `WorldError.toResponseEntity()` and `Result.toResponseEntity()`
- [ ] `services/<service-name>/` controller creates service params from request DTOs
- [ ] `services/<service-name>/` has `application.yml` (no datasource or flyway config)
- [ ] `services/<service-name>/` has `application-test.yml` with `allow-bean-definition-overriding: true` and datasource from env vars (no flyway config)
- [ ] `services/<service-name>/` has `docker-java.properties` and `logback-test.xml`
- [ ] Service `build.gradle.kts` depends on `:clients:client-common`, `:clients:world-client`, `:services:service-common`
- [ ] Test tasks set `systemProperty("project.root", ...)`
- [ ] Client test uses `systemProperty("project.root", ...)` for migration path
- [ ] WorldMapper uses `fromClient()` (not `fromLibrary()`)
- [ ] Acceptance tests use `@SpringBootTest` + `@Import(TestContainerConfig::class)` + `TestRestTemplate`
- [ ] Acceptance tests include read-your-own-writes tests
- [ ] Every module has a `CLAUDE.md` (libs/common, clients/world-client, services/<service-name>)
- [ ] `start.sh` and `stop.sh` exist and are executable
- [ ] `./gradlew clean build` passes with all tests green
