# Project Bootstrap

You are a project architect bootstrapping a complete Kotlin project from scratch. You handle root-level project setup yourself, and delegate all client, service, database, and test code generation to sub-agents that invoke the appropriate skills.

## Critical Rules

1. **Never reference other projects.** The skills are the sole source of truth for how to generate code. Never read, copy from, or use existing projects in the monorepo as reference. If a skill doesn't produce the right output, fix the skill — don't work around it by copying from another project.
2. **Always run sub-agents in foreground.** Never set `run_in_background: true` on sub-agents. Background agents cannot prompt the user for tool permissions (Write, Bash, etc.) and will silently fail. Foreground agents can prompt interactively and will succeed.

## How Delegation Works

This skill uses the **Agent tool** to spawn sub-agents in **foreground** mode. Each sub-agent invokes a skill (via the Skill tool) and provides the answers to that skill's interactive prompts. This way:
- Each sub-skill runs through its real logic and templates
- Sub-skills can be updated independently — this skill automatically picks up changes
- Sub-agents run in foreground so they can prompt for tool permissions when needed

### Skills Used

| Skill | Invoked via | Purpose |
|-------|------------|---------|
| `/service-manager` | Sub-agent | Creates clients, libs, services, and their Gradle configs |
| `/db-manager` | Sub-agent | Creates database schemas, migrations, docker-compose, flyway |
| `/create-acceptance-tests` | Sub-agent | Creates test infrastructure, fixtures, acceptance tests |

### Sub-Agent Prompt Pattern

When spawning a sub-agent, provide a prompt like:

> Use the Skill tool to invoke `/service-manager`. When it asks which command, answer "Create Client". When it asks for information, provide these answers:
> - Client name: world-client
> - Purpose: Data access client for the worlds table
> - ...
>
> The project root is at `<target>/<project>/`. The root package is `<pkg.dot>`.
> Write all files. Do not ask any questions — all answers are provided above.

Always include:
1. Which skill to invoke and which command to run
2. All answers to the skill's "Gather Information" prompts
3. The project root path and root package
4. Instruction to write files without asking questions

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

#### Step 2: Delegate to Sub-Skills (via sub-agents)

Spawn sub-agents (in **foreground**, never background) to create the remaining modules. Run each sub-agent sequentially — foreground agents block until complete. Steps 2a-2d have no dependencies on each other. Steps 2e-2f depend on earlier steps.

##### Step 2a: Client Common

Spawn a sub-agent that invokes `/service-manager`:
- **Command:** Client Common Bootstrap
- **Root package:** `<pkg.dot>`
- **Project root:** `<target>/<project>/`
- **Instruction:** Create the `clients/common/` module with Result.kt, ClientContext.kt, error types, and ResultTest.kt

##### Step 2b: Service Common

Spawn a sub-agent that invokes `/service-manager`:
- **Command:** Service Common Bootstrap
- **Root package:** `<pkg.dot>`
- **Project root:** `<target>/<project>/`
- **Instruction:** Create the `services/common/` module with ApiResponse.kt and ApiResponseTest.kt

##### Step 2c: Common Library

Spawn a sub-agent that invokes `/service-manager`:
- **Command:** Common Library Bootstrap
- **Root package:** `<pkg.dot>`
- **Project root:** `<target>/<project>/`
- **Instruction:** Create the `libs/common/` module with logging utilities and CLAUDE.md

##### Step 2d: Database

Spawn a sub-agent that invokes `/db-manager`:
- **Command:** Create DB
- **Database name:** `<db-name>`
- **Port:** `5433` (or next available)
- **Tables:** `worlds` with columns: `name VARCHAR(100) NOT NULL` (UNIQUE constraint `uq_worlds_name`), `greeting VARCHAR(255) NOT NULL`. Index: `idx_worlds_name` on `name`.
- **Seed data:** 3 rows — Earth, Mars, Vulcan with greetings and fixed UUIDs
- **Project root:** `<target>/<project>/`

##### Step 2e: World Client (after 2a completes)

Spawn a sub-agent that invokes `/service-manager`:
- **Command:** Create Client
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

##### Step 2f: Service + Tests (after 2a, 2b, 2e complete)

Spawn a sub-agent that invokes `/service-manager`:
- **Command:** Create API Service
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

Then spawn another sub-agent that invokes `/create-acceptance-tests`:
- **Command:** Create Acceptance Tests
- **Service:** `<service-name>`
- **Features to test:** `world`
- **DB schemas:** `worlds` table from `<db-name>`
- **Root package:** `<pkg.dot>`
- **Project root:** `<target>/<project>/`
- **Endpoints to cover:** all 5 world routes
- **Include:** read-your-own-writes tests (POST then GET, POST then PUT then GET, POST then DELETE then GET)

---

#### Step 3: Documentation (you do this directly)

After all sub-agents complete, create these files:

##### Root `CLAUDE.md`

Document the project with these sections:
- **Overview** — one-line description
- **Project Structure** — directory tree showing all modules
- **Module Layout** — explain clients/, libs/, services/, databases/ and their roles
- **Tech Stack** — Gradle, Kotlin, Spring Boot, JDBI, Flyway, Testcontainers versions
- **Architecture** — dependency direction, DI approach, error handling, testing strategy, client patterns (facade, param objects, client context)
- **Key Conventions** — root package, package naming for each module type, DB naming, Kotlin naming, client interface + fake pattern
- **Quick Start** — docker-compose, flyway, seed, bootRun commands

##### Root `README.md`

Structure with these sections:
- **What's Included** — bullet list of each module
- **Prerequisites** — JDK 21+, Docker
- **Initial Setup** — one-time: docker-compose, flyway migrate, seed
- **Quick Start** — `./gradlew :services:<service-name>:bootRun`
- **API** — endpoints table with method, path, description
- **Example** — curl commands for create and list
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
- [ ] `./gradlew clean build` passes with all tests green
