---
name: service-manager
description: Scaffold and manage Kotlin services, clients, and libraries in the Gradle monorepo. Reference skill for coding patterns and conventions.
user-invocable: true
---

# Kotlin Service & Library Management

You are a Kotlin architect scaffolding production-quality services and libraries in a Gradle monorepo. Follow these instructions precisely.

## Commands

Ask the user which command they want to run:

1. **Create API Service** — Scaffold a new service under `services/`
2. **Create Client** — Scaffold a data access client under `clients/`
3. **Create Library** — Scaffold a shared library under `libs/`
4. **Add Service API** — Add a feature vertical slice to an existing service
5. **Add Client Function** — Add a function to an existing client
6. **Add Library Function** — Add a function to an existing library

---

## Shared Conventions (apply to ALL commands)

### Package Structure
- Base package: ask the user for their root package (e.g., `com.example`). Use it consistently.
- Client common: `<root-package>.clients.common`
- Service common: `<root-package>.services.common`
- Clients: `<root-package>.clients.<client-name>`
- Libraries: `<root-package>.libs.<library-name>`
- Services: `<root-package>.services.<service-name>`
- Features: `<root-package>.services.<service-name>.features.<feature-name>`

### Module Layout
- **clients/common/** (Gradle: `:clients:client-common`) — Shared types for clients: `Result<T,E>`, `ClientContext`, error types (`AppError`, `NotFoundError`, etc.)
- **clients/** — Data access and external API clients (I/O). Things that talk to databases, APIs, or other services.
- **libs/** — Shared utilities, types, and helpers (no I/O). Pure logic, types, logging.
- **services/common/** (Gradle: `:services:service-common`) — Shared types for services: `ApiResponse`, error body types.
- **services/** — Deployable Spring Boot applications.
- **databases/** — Database schemas, migrations, seeds, docker-compose files. Each database is a Gradle module exposing a `MigrationRunner` utility.

Rule of thumb: "Does it do I/O? → `clients/`. Pure logic/types? → `libs/`."

### Gradle Module Naming
Gradle conflates projects with the same leaf directory name. To keep clean directory names (`clients/common/`, `services/common/`) while avoiding collisions, rename them in `settings.gradle.kts`:
```kotlin
include(":clients:common")
project(":clients:common").name = "client-common"
include(":services:common")
project(":services:common").name = "service-common"
```
This keeps directories as `common/` but gives Gradle unique project paths (`:clients:client-common`, `:services:service-common`).

### Dependency Direction
```
services/  →  clients/  →  clients/common/ (:clients:client-common)
           →  services/common/ (:services:service-common)
           →  libs/
clients/   →  databases/  (testFixtures dependency for migration runner)
```
- Services depend on clients, service-common, and libs.
- Clients depend on `:clients:client-common`.
- Clients depend on database modules (via testFixtures) for migration runner in tests.
- `clients/common/` and `services/common/` are leaf modules with no internal dependencies.
- Libs are standalone (no I/O dependencies).
- Features depend on `common/error/` for shared infra. Features NEVER depend on each other.

### Spring Boot Patterns
- `@SpringBootApplication` main class as entry point.
- `@Configuration` classes with `@Bean` methods for wiring clients and services.
- Service classes wired via `@Configuration` beans with direct constructor calls (no `@Service` annotation, no separate factory function).
- `@RestController` for HTTP endpoints.
- Constructor injection (no field injection, no `@Inject`).
- Tests override via `@TestConfiguration` + `@Import`.

### Gradle Patterns
- Root `build.gradle.kts`: plugins with `apply false`, `allprojects` for repositories, `subprojects` for `jvmToolchain(21)`.
- `settings.gradle.kts`: foojay toolchain resolver + module includes with name overrides for `common` directories (see Gradle Module Naming).
- Client/lib modules: `kotlin("jvm")` plugin. Clients use `java-test-fixtures` for fakes and test DB helpers. Database modules: `kotlin("jvm")` plugin with Flyway dependency.
- Service modules: `kotlin("jvm")` + `kotlin("plugin.spring")` + `org.springframework.boot` + `io.spring.dependency-management`.

### Logging
- SLF4J API everywhere: `private val logger = LoggerFactory.getLogger(ClassName::class.java)`
- INFO at controller entry (request received)
- DEBUG for internal logic and data transformations
- WARN for recoverable failures, ERROR for unexpected failures

### Testing Conventions
- Test naming: `` `methodName returns X when Y` `` (backtick style)
- Data access: Testcontainers against real PostgreSQL
- Business logic: Fakes for dependencies (from `testFixtures`), not mocks
- Acceptance: `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate`
- JUnit 5 + AssertJ for assertions
- `@Nested` inner classes to group tests by operation

### Error Handling
- Custom `Result<T, E>` from `clients/client-common/` — sealed class with `Success`/`Failure`
- `AppError` is a regular `interface` (not sealed) — Kotlin 2.x prohibits extending sealed types across Gradle modules
- Sealed error hierarchies at the service layer boundary (e.g., `WorldError`)
- Never throw exceptions for expected failures
- Exceptions only for truly unexpected/programmer errors

### Client Patterns
- **Facade pattern:** Client implementation is a facade delegating to individual operation classes
- **Parameter objects:** All client interface methods take data class params, even for single-field lookups
- **Validation classes:** 1:1 with operations in `internal/validations/`. Every operation gets a paired validator (default returns `success(Unit)`)
- **Row adapters:** Separate `adapters/` directory for DB ResultSet → model mapping
- **Factory function:** `create<Name>Client()` — takes no params, reads DB connection from env vars (`DB_URL`, `DB_USER`, `DB_PASSWORD`), creates its own Jdbi internally. The service never sees JDBI.
- **KDoc:** All interface methods have KDoc documentation
- **Nullable UUID binding in JDBI:** When binding a nullable `UUID?` in JDBI SQL, use `CAST(:param AS uuid)` in the SQL and `.bind("param", value?.toString())` in Kotlin. Without the CAST, JDBI sends null as varchar which causes a PostgreSQL type mismatch error. This applies to any nullable UUID column (FK columns, optional references, etc.).
- **Contract PR stubs:** When creating client contract PRs, add `NotImplementedError` stubs in BOTH the `FakeClient` AND the `JdbiClient` (or any concrete implementation). This ensures the project compiles at the contract stage.

### Service Patterns
- **Action classes:** 1:1 with service methods in `features/<feature>/actions/`. Each action takes a service param, validates, converts to client param, calls client
- **Validation classes:** 1:1 with actions in `features/<feature>/validations/`. Intentionally duplicates client validation at the service boundary
- **Parameter objects:** Service has its own param types, separate from client params
- **Wiring:** Service constructor called directly in `@Configuration` bean (no `@Service` annotation, no separate factory function)
- **Error mapping:** `<Feature>Error.fromClientError()` companion centralizes client→service error conversion

### Model Adaptation
- Services NEVER expose client types in their domain models
- Each service adapts client models → internal domain models at the boundary
- Mappers handle the conversion explicitly
- Mapper method: `fromClient()` (not `fromLibrary()`)

### Docker Compatibility
- Any module using Testcontainers 1.x must include `src/test/resources/docker-java.properties` with `api.version=1.44`
- This is required for Docker Engine 29.x compatibility

---

## Client Common Bootstrap

If `clients/common/` does not exist, create it FIRST before any client. It contains types shared across all clients. Directory is `clients/common/`, Gradle project is `:clients:client-common` (see Gradle Module Naming).

### Structure
```
clients/common/
├── build.gradle.kts
├── src/main/kotlin/<pkg>/clients/common/
│   ├── Result.kt
│   ├── ClientContext.kt
│   └── error/
│       ├── AppError.kt          # interface (NOT sealed)
│       ├── NotFoundError.kt
│       ├── ValidationError.kt
│       └── ConflictError.kt
└── src/test/kotlin/<pkg>/clients/common/
    └── ResultTest.kt
```

### `build.gradle.kts`
```kotlin
plugins {
    kotlin("jvm")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
```

### `Result.kt`
```kotlin
package <pkg.dot>.clients.common

sealed class Result<out T, out E> {
    data class Success<T>(val value: T) : Result<T, Nothing>()
    data class Failure<E>(val error: E) : Result<Nothing, E>()

    fun <R> map(transform: (T) -> R): Result<R, E> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> Failure(error)
    }

    fun <R> flatMap(transform: (T) -> Result<R, @UnsafeVariance E>): Result<R, E> = when (this) {
        is Success -> transform(value)
        is Failure -> Failure(error)
    }

    fun getOrElse(default: () -> @UnsafeVariance T): T = when (this) {
        is Success -> value
        is Failure -> default()
    }

    fun getOrNull(): T? = when (this) {
        is Success -> value
        is Failure -> null
    }

    fun errorOrNull(): E? = when (this) {
        is Success -> null
        is Failure -> error
    }

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure
}

fun <T> success(value: T): Result<T, Nothing> = Result.Success(value)
fun <E> failure(error: E): Result<Nothing, E> = Result.Failure(error)
```

**IMPORTANT:** `@UnsafeVariance` is required on `flatMap` and `getOrElse` for Kotlin 2.x variance rules.

### `ClientContext.kt`
```kotlin
package <pkg.dot>.clients.common

/**
 * Context information passed to client instances.
 * Carries cross-cutting concerns like correlation IDs and caller metadata.
 */
data class ClientContext(
    val correlationId: String? = null,
    val callerName: String? = null
)
```

### `error/AppError.kt` — MUST be a regular interface (not sealed) for cross-module use in Kotlin 2.x:
```kotlin
package <pkg.dot>.clients.common.error

interface AppError {
    val message: String
}
```

### `error/NotFoundError.kt`
```kotlin
package <pkg.dot>.clients.common.error

data class NotFoundError(
    val entity: String,
    val id: String,
    override val message: String = "$entity not found: $id"
) : AppError
```

### `error/ValidationError.kt`
```kotlin
package <pkg.dot>.clients.common.error

data class ValidationError(
    val field: String,
    val reason: String,
    override val message: String = "Validation failed for $field: $reason"
) : AppError
```

### `error/ConflictError.kt`
```kotlin
package <pkg.dot>.clients.common.error

data class ConflictError(
    val entity: String,
    val detail: String,
    override val message: String = "Conflict for $entity: $detail"
) : AppError
```

### `ResultTest.kt` — test `map`, `flatMap`, `getOrElse`, `getOrNull`, `errorOrNull`, `isSuccess`, `isFailure` on both Success and Failure variants.

---

## Service Common Bootstrap

If `services/common/` does not exist, create it FIRST before any service. It contains types shared across all services. Directory is `services/common/`, Gradle project is `:services:service-common` (see Gradle Module Naming).

### Structure
```
services/common/
├── build.gradle.kts
├── src/main/kotlin/<pkg>/services/common/
│   └── ApiResponse.kt
└── src/test/kotlin/<pkg>/services/common/
    └── ApiResponseTest.kt
```

### `build.gradle.kts`
```kotlin
plugins {
    kotlin("jvm")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
```

### `ApiResponse.kt`
```kotlin
package <pkg.dot>.services.common

data class ApiResponse<T>(
    val status: Int,
    val body: T? = null,
    val error: ErrorBody? = null
) {
    data class ErrorBody(
        val code: String,
        val message: String,
        val details: Map<String, Any>? = null
    )

    companion object {
        fun <T> ok(body: T) = ApiResponse(status = 200, body = body)
        fun <T> created(body: T) = ApiResponse(status = 201, body = body)
        fun noContent() = ApiResponse<Unit>(status = 204)
        fun notFound(message: String) = ApiResponse<Nothing>(status = 404, error = ErrorBody("NOT_FOUND", message))
        fun conflict(message: String) = ApiResponse<Nothing>(status = 409, error = ErrorBody("CONFLICT", message))
        fun badRequest(message: String) = ApiResponse<Nothing>(status = 400, error = ErrorBody("BAD_REQUEST", message))
        fun validationError(message: String, details: Map<String, Any>) =
            ApiResponse<Nothing>(status = 422, error = ErrorBody("VALIDATION_ERROR", message, details))
        fun internalError(message: String) = ApiResponse<Nothing>(status = 500, error = ErrorBody("INTERNAL_ERROR", message))
    }
}
```

### `ApiResponseTest.kt` — test factory methods (`ok`, `created`, `noContent`, `notFound`, `conflict`, `badRequest`, `validationError`, `internalError`) produce correct status codes and bodies.

---

## Common Library Bootstrap

If `libs/common/` does not exist, create it FIRST. It contains shared utilities with no I/O.

### Structure
```
libs/common/
├── CLAUDE.md
├── build.gradle.kts
├── src/main/kotlin/<pkg>/libs/common/
│   └── logging/
│       └── LoggingUtils.kt
```

### `build.gradle.kts`
```kotlin
plugins {
    kotlin("jvm")
}

dependencies {
    implementation("org.slf4j:slf4j-api:2.0.16")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
```

### `logging/LoggingUtils.kt`
```kotlin
package <pkg.dot>.libs.common.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory

inline fun <reified T> T.logger(): Logger = LoggerFactory.getLogger(T::class.java)
```

---

## Module CLAUDE.md Templates

Every module gets a `CLAUDE.md` file documenting its purpose, public API, and key patterns. These are created during scaffolding.

### Library CLAUDE.md
```markdown
# <Library Name> — AI Context

## Overview
<one-line description>

## Package
`<pkg.dot>.libs.<lib-pkg>`

## Public API
- <list of public types/functions with one-line descriptions>

## Gradle Module
`:<module-path>`
```

### Client CLAUDE.md
```markdown
# <client-name>

<one-line description>

## Package
`<pkg.dot>.clients.<client-pkg>`

## Public API (`<Name>Client` interface)
- <list of methods with params and one-line descriptions>

## Model
<model class with fields>

## Database
- Database: `<db-name>` (port <port>, database `<db_name>`)
- Table: `<table>`
- Key constraints: <unique constraints>

## Architecture
- **Facade pattern:** `Jdbi<Name>Client` delegates to individual operation classes
- **Validation classes:** 1:1 with operations in `internal/validations/`
- **Parameter objects:** All methods take dedicated data class params
- **Row adapter:** `<Entity>RowAdapter` maps ResultSet to `<Entity>`
- **Factory:** `create<Name>Client()` creates the client (reads DB config from env vars)

## Error Handling
- Returns `Result<T, AppError>` — never throws for expected failures
- `NotFoundError` for missing entities
- `ConflictError` for duplicate constraint violations

## Testing
- Integration tests: Testcontainers PostgreSQL + migrations via `<Name>TestDb`
- `Fake<Name>Client` (testFixtures) for consumer testing — references actual validators
- `<Name>TestDb` (testFixtures) wraps `MigrationRunner` from database module
```

### Service CLAUDE.md
```markdown
# <service-name>

<one-line description>

## Package
`<pkg.dot>.services.<svc-pkg>`

## Architecture
- Spring Boot <version> application on port <port>
- Consumes `<client-name>` for data access
- Database: `<db-name>` (port <port>, database `<db_name>`)

## Features

### <Feature> (`features/<feature>/`)
- **Model:** <model fields>
- **DTOs:** <request/response DTOs>
- **Error:** `<Feature>Error` sealed class — <variants>
- **Service params:** <param types>
- **Validations:** 1:1 with actions in `validations/`
- **Actions:** 1:1 with service methods in `actions/`
- **Service:** `<Feature>Service` facade (no `@Service`, wired via `@Configuration` bean)
- **Routes:** <list of endpoints>

## Key Patterns
- `<Feature>Mapper.fromClient()` adapts client model to service model
- Service param objects for all service calls
- Action classes validate, convert params, call client
- `GlobalExceptionHandler` catches `RuntimeException::class`
- `ResultExtensions.kt` maps errors to HTTP responses

## Configuration
- `<Client>Config` — creates client via factory function
- `<Feature>ServiceConfig` — wires service via `@Configuration` bean

## Testing
- **Unit:** `<Feature>ServiceTest` uses `Fake<Name>Client` from testFixtures
- **Acceptance:** `<Feature>AcceptanceTest` with `@SpringBootTest(RANDOM_PORT)` + Testcontainers
- **Fixture:** `<Feature>Fixture` uses direct SQL for test setup
- **Clean slate:** Tables truncated via `@BeforeEach`
```

---

## Command: Create Client

### Gather Information

Ask the user for:
1. **Client name** (e.g., `inventory-client`, `user-client`)
2. **Purpose** — what does this client do?
3. **Public API** — what operations does it expose?
4. **Database** — which database does it connect to?
5. **Root package** (if not already established)

### Scaffold

```
clients/<client-name>/
├── CLAUDE.md
├── build.gradle.kts
├── src/main/kotlin/<pkg>/clients/<client-pkg>/
│   ├── <Name>ClientFactory.kt        # Factory function
│   ├── model/
│   │   └── <ModelName>.kt
│   ├── api/
│   │   ├── <Name>Client.kt           # Interface with KDoc
│   │   └── <Name>ClientParams.kt     # Parameter objects
│   └── internal/
│       ├── Jdbi<Name>Client.kt       # Facade
│       ├── adapters/
│       │   └── <Model>RowAdapter.kt  # ResultSet → model
│       ├── operations/
│       │   ├── Get<Model>ById.kt
│       │   ├── Get<Model>List.kt
│       │   ├── Create<Model>.kt
│       │   ├── Update<Model>.kt
│       │   └── Delete<Model>.kt
│       └── validations/              # 1:1 with operations
│           ├── ValidateGet<Model>ById.kt
│           ├── ValidateGet<Model>List.kt
│           ├── ValidateCreate<Model>.kt
│           ├── ValidateUpdate<Model>.kt
│           └── ValidateDelete<Model>.kt
├── src/testFixtures/kotlin/<pkg>/clients/<client-pkg>/fake/
│   └── Fake<Name>Client.kt           # Exported fake (references validators)
├── src/testFixtures/kotlin/<pkg>/clients/<client-pkg>/test/
│   └── <Name>TestDb.kt               # Test DB helper (wraps MigrationRunner)
├── src/test/kotlin/<pkg>/clients/<client-pkg>/
│   └── Jdbi<Name>ClientTest.kt       # Integration tests
└── src/test/resources/
    └── docker-java.properties         # api.version=1.44
```

### `build.gradle.kts`
```kotlin
plugins {
    kotlin("jvm")
    `java-test-fixtures`
}

dependencies {
    api(project(":clients:client-common"))  // directory: clients/common/

    implementation("org.jdbi:jdbi3-core:3.47.0")
    implementation("org.slf4j:slf4j-api:2.0.16")
    runtimeOnly("org.postgresql:postgresql:42.7.4")

    testFixturesApi(project(":clients:client-common"))  // directory: clients/common/
    testFixturesImplementation(project(":databases:<db-name>"))

    testRuntimeOnly(project(":databases:<db-name>"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("org.testcontainers:postgresql:1.20.4")
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")
    testImplementation("org.postgresql:postgresql:42.7.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("ch.qos.logback:logback-classic:1.5.16")
}

tasks.withType<Test> {
    systemProperty("project.root", rootProject.projectDir.absolutePath)
}
```

### Key Patterns

**Parameter objects** — Every method takes a dedicated data class param, even for single-field lookups:
```kotlin
package <pkg>.clients.<client-pkg>.api

import java.util.UUID

/** Parameter for retrieving a <entity> by its unique identifier. */
data class GetByIdParam(val id: UUID)

/** Parameter for listing <entities>. Extensible for future filtering/pagination. */
data class GetListParam(
    val limit: Int? = null,
    val offset: Int? = null
)

/** Parameter for creating a new <entity>. */
data class Create<Entity>Param(val name: String, val greeting: String)

/** Parameter for updating an existing <entity>. Null fields are left unchanged. */
data class Update<Entity>Param(
    val id: UUID,
    val name: String? = null,
    val greeting: String? = null
)

/** Parameter for deleting a <entity> by its unique identifier. */
data class Delete<Entity>Param(val id: UUID)
```

**Interface** — all operations return `Result<T, AppError>`, with KDoc on every method:
```kotlin
package <pkg>.clients.<client-pkg>.api

import <pkg>.clients.common.Result
import <pkg>.clients.common.error.AppError
import <pkg>.clients.<client-pkg>.model.<Entity>

/**
 * Client interface for <entity> entity CRUD operations.
 *
 * All operations return [Result] to represent success or typed failure
 * without throwing exceptions for expected error conditions.
 */
interface <Name>Client {
    /** Retrieve a single <entity> by its unique identifier. */
    fun getById(param: GetByIdParam): Result<<Entity>, AppError>

    /** Retrieve a list of all <entities>, ordered by name. */
    fun getList(param: GetListParam): Result<List<<Entity>>, AppError>

    /** Create a new <entity> with the given fields. */
    fun create(param: Create<Entity>Param): Result<<Entity>, AppError>

    /** Update an existing <entity>. Null fields are left unchanged. */
    fun update(param: Update<Entity>Param): Result<<Entity>, AppError>

    /** Delete a <entity> by its unique identifier. */
    fun delete(param: Delete<Entity>Param): Result<Unit, AppError>
}
```

**Row adapter** — separate class for DB ResultSet → model mapping:
```kotlin
package <pkg>.clients.<client-pkg>.internal.adapters

import <pkg>.clients.<client-pkg>.model.<Entity>
import java.sql.ResultSet
import java.util.UUID

/**
 * Adapts database rows to [<Entity>] domain objects.
 * Handles any drift between DB schema and data class over time.
 */
object <Entity>RowAdapter {

    fun fromResultSet(rs: ResultSet): <Entity> = <Entity>(
        id = rs.getObject("id", UUID::class.java),
        name = rs.getString("name"),
        // ... map all fields
        createdAt = rs.getTimestamp("created_at").toInstant(),
        updatedAt = rs.getTimestamp("updated_at").toInstant()
    )
}
```

**Facade implementation** — delegates to individual operation classes:
```kotlin
package <pkg>.clients.<client-pkg>.internal

import <pkg>.clients.common.Result
import <pkg>.clients.common.error.AppError
import <pkg>.clients.<client-pkg>.api.*
import <pkg>.clients.<client-pkg>.internal.operations.*
import <pkg>.clients.<client-pkg>.model.<Entity>
import org.jdbi.v3.core.Jdbi

/**
 * Facade that delegates to individual operation classes.
 */
internal class Jdbi<Name>Client(jdbi: Jdbi) : <Name>Client {

    private val get<Entity>ById = Get<Entity>ById(jdbi)
    private val get<Entity>List = Get<Entity>List(jdbi)
    private val create<Entity> = Create<Entity>(jdbi)
    private val update<Entity> = Update<Entity>(jdbi, get<Entity>ById)
    private val delete<Entity> = Delete<Entity>(jdbi)

    override fun getById(param: GetByIdParam) = get<Entity>ById.execute(param)
    override fun getList(param: GetListParam) = get<Entity>List.execute(param)
    override fun create(param: Create<Entity>Param) = create<Entity>.execute(param)
    override fun update(param: Update<Entity>Param) = update<Entity>.execute(param)
    override fun delete(param: Delete<Entity>Param) = delete<Entity>.execute(param)
}
```

**Validation classes** — 1:1 with operations. Every operation gets a paired validator in `internal/validations/`. Validators with real checks use `validate()` + logging pattern; default validators return `success(Unit)`. Ask the user what validation checks each operation needs; if none specified, create a default validator that returns `success(Unit)`.

Validation class with checks:
```kotlin
package <pkg>.clients.<client-pkg>.internal.validations

import <pkg>.clients.common.Result
import <pkg>.clients.common.error.AppError
import <pkg>.clients.common.error.ValidationError
import <pkg>.clients.common.failure
import <pkg>.clients.common.success
import <pkg>.clients.<client-pkg>.api.Create<Entity>Param
import org.slf4j.LoggerFactory

internal class ValidateCreate<Entity> {
    private val logger = LoggerFactory.getLogger(ValidateCreate<Entity>::class.java)

    fun execute(param: Create<Entity>Param): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: Create<Entity>Param): Result<Unit, AppError> {
        if (param.name.isBlank()) return failure(ValidationError("name", "must not be blank"))
        // ... additional checks
        return success(Unit)
    }
}
```

Default validation class (no checks):
```kotlin
internal class ValidateGet<Entity>ById {
    private val logger = LoggerFactory.getLogger(ValidateGet<Entity>ById::class.java)

    fun execute(param: GetByIdParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: GetByIdParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
```

**Operation classes** — each operation is its own `internal class`. Every operation instantiates its paired validator and calls it before executing:

`Get<Entity>ById.kt`:
```kotlin
package <pkg>.clients.<client-pkg>.internal.operations

import <pkg>.clients.common.Result
import <pkg>.clients.common.success
import <pkg>.clients.common.failure
import <pkg>.clients.common.error.AppError
import <pkg>.clients.common.error.NotFoundError
import <pkg>.clients.<client-pkg>.api.GetByIdParam
import <pkg>.clients.<client-pkg>.internal.adapters.<Entity>RowAdapter
import <pkg>.clients.<client-pkg>.model.<Entity>
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class Get<Entity>ById(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(Get<Entity>ById::class.java)
    private val validate = ValidateGet<Entity>ById()

    fun execute(param: GetByIdParam): Result<<Entity>, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Finding <entity> by id={}", param.id)
        val entity = jdbi.withHandle<<Entity>?, Exception> { handle ->
            handle.createQuery("SELECT id, <columns> FROM <table> WHERE id = :id")
                .bind("id", param.id)
                .map { rs, _ -> <Entity>RowAdapter.fromResultSet(rs) }
                .findOne()
                .orElse(null)
        }
        return if (entity != null) success(entity) else failure(NotFoundError("<Entity>", param.id.toString()))
    }
}
```

`Get<Entity>List.kt` — enforces a hard max limit via `MAX_LIMIT` constant. Always applies a limit (defaults to `MAX_LIMIT` when caller doesn't specify one):
```kotlin
internal class Get<Entity>List(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(Get<Entity>List::class.java)
    private val validate = ValidateGet<Entity>List()

    private companion object {
        const val MAX_LIMIT = 100
    }

    fun execute(param: GetListParam): Result<List<<Entity>>, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        val effectiveLimit = minOf(param.limit ?: MAX_LIMIT, MAX_LIMIT)

        logger.debug("Finding all <entities>")
        val entities = jdbi.withHandle<List<<Entity>>, Exception> { handle ->
            val sql = buildString {
                append("SELECT id, <columns> FROM <table> ORDER BY name")
                append(" LIMIT :limit")
                if (param.offset != null) append(" OFFSET :offset")
            }
            val query = handle.createQuery(sql)
            query.bind("limit", effectiveLimit)
            if (param.offset != null) query.bind("offset", param.offset)
            query.map { rs, _ -> <Entity>RowAdapter.fromResultSet(rs) }.list()
        }
        return success(entities)
    }
}
```

`Create<Entity>.kt` — validate then catch unique constraint violations and return `ConflictError`:
```kotlin
internal class Create<Entity>(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(Create<Entity>::class.java)
    private val validate = ValidateCreate<Entity>()

    fun execute(param: Create<Entity>Param): Result<<Entity>, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Creating <entity> name={}", param.name)
        return try {
            val entity = jdbi.withHandle<<Entity>, Exception> { handle ->
                val id = UUID.randomUUID()
                val now = Instant.now()
                handle.createUpdate("""
                    INSERT INTO <table> (id, <columns>, created_at, updated_at)
                    VALUES (:id, <bindings>, :createdAt, :updatedAt)
                """.trimIndent())
                    .bind("id", id)
                    // ... bind all fields from param
                    // For nullable UUIDs: use CAST(:param AS uuid) in SQL + .bind("param", value?.toString())
                    .bind("createdAt", now)
                    .bind("updatedAt", now)
                    .execute()
                <Entity>(id = id, /* fields from param */, createdAt = now, updatedAt = now)
            }
            success(entity)
        } catch (e: Exception) {
            if (e.message?.contains("uq_<table>_name") == true || e.message?.contains("duplicate key") == true) {
                failure(ConflictError("<Entity>", "name '${param.name}' already exists"))
            } else {
                throw e
            }
        }
    }
}
```

`Update<Entity>.kt` — validate then delegate to GetById for fetch-before-update:
```kotlin
internal class Update<Entity>(
    private val jdbi: Jdbi,
    private val get<Entity>ById: Get<Entity>ById
) {
    private val validate = ValidateUpdate<Entity>()

    fun execute(param: Update<Entity>Param): Result<<Entity>, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        return when (val existing = get<Entity>ById.execute(GetByIdParam(param.id))) {
            is Result.Failure -> existing
            is Result.Success -> {
                val updatedName = param.name ?: existing.value.name
                // ... merge nullable fields
                val now = Instant.now()
                try {
                    jdbi.withHandle<Unit, Exception> { handle ->
                        handle.createUpdate("""
                            UPDATE <table> SET name = :name, ..., updated_at = :updatedAt
                            WHERE id = :id
                        """.trimIndent())
                            .bind("id", param.id)
                            // ... bind merged fields
                            .bind("updatedAt", now)
                            .execute()
                    }
                    success(existing.value.copy(/* merged fields */, updatedAt = now))
                } catch (e: Exception) {
                    if (e.message?.contains("uq_<table>_name") == true || e.message?.contains("duplicate key") == true) {
                        failure(ConflictError("<Entity>", "name '$updatedName' already exists"))
                    } else {
                        throw e
                    }
                }
            }
        }
    }
}
```

`Delete<Entity>.kt`:
```kotlin
internal class Delete<Entity>(private val jdbi: Jdbi) {
    private val validate = ValidateDelete<Entity>()

    fun execute(param: Delete<Entity>Param): Result<Unit, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation


        val rowsAffected = jdbi.withHandle<Int, Exception> { handle ->
            handle.createUpdate("DELETE FROM <table> WHERE id = :id")
                .bind("id", param.id)
                .execute()
        }
        return if (rowsAffected > 0) success(Unit) else failure(NotFoundError("<Entity>", param.id.toString()))
    }
}
```

**Factory function** (reads DB connection from env vars):
```kotlin
package <pkg>.clients.<client-pkg>

import <pkg>.clients.<client-pkg>.api.<Name>Client
import <pkg>.clients.<client-pkg>.internal.Jdbi<Name>Client
import org.jdbi.v3.core.Jdbi

fun create<Name>Client(): <Name>Client {
    val url = System.getProperty("DB_URL") ?: System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:<port>/<db_name>"
    val user = System.getProperty("DB_USER") ?: System.getenv("DB_USER") ?: "postgres"
    val password = System.getProperty("DB_PASSWORD") ?: System.getenv("DB_PASSWORD") ?: "postgres"
    val jdbi = Jdbi.create(url, user, password)
    return Jdbi<Name>Client(jdbi)
}
```

Place the factory in the same package as the internal implementation so it can access `internal` classes.

**Fake** — in `src/testFixtures/` source set. Uses `ConcurrentHashMap` for in-memory storage. **References the actual validation classes** from `internal/validations/` (testFixtures can access `internal` classes within the same module). Provides `reset()` and `seed()` helpers:
```kotlin
package <pkg>.clients.<client-pkg>.fake

import <pkg>.clients.common.Result
import <pkg>.clients.common.success
import <pkg>.clients.common.failure
import <pkg>.clients.common.error.AppError
import <pkg>.clients.common.error.NotFoundError
import <pkg>.clients.common.error.ConflictError
import <pkg>.clients.<client-pkg>.api.*
import <pkg>.clients.<client-pkg>.internal.validations.*
import <pkg>.clients.<client-pkg>.model.<Entity>
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class Fake<Name>Client : <Name>Client {
    private val store = ConcurrentHashMap<UUID, <Entity>>()

    private companion object {
        const val MAX_LIMIT = 100
    }

    private val validateGetById = ValidateGet<Entity>ById()
    private val validateGetList = ValidateGet<Entity>List()
    private val validateCreate = ValidateCreate<Entity>()
    private val validateUpdate = ValidateUpdate<Entity>()
    private val validateDelete = ValidateDelete<Entity>()

    override fun getById(param: GetByIdParam): Result<<Entity>, AppError> {
        val validation = validateGetById.execute(param)
        if (validation is Result.Failure) return validation

        val entity = store[param.id]
        return if (entity != null) success(entity) else failure(NotFoundError("<Entity>", param.id.toString()))
    }

    override fun getList(param: GetListParam): Result<List<<Entity>>, AppError> {
        val validation = validateGetList.execute(param)
        if (validation is Result.Failure) return validation

        val effectiveLimit = minOf(param.limit ?: MAX_LIMIT, MAX_LIMIT)
        var entities = store.values.sortedBy { it.name }
        if (param.offset != null) entities = entities.drop(param.offset)
        entities = entities.take(effectiveLimit)
        return success(entities)
    }

    override fun create(param: Create<Entity>Param): Result<<Entity>, AppError> {
        val validation = validateCreate.execute(param)
        if (validation is Result.Failure) return validation

        if (store.values.any { it.name == param.name }) {
            return failure(ConflictError("<Entity>", "name '${param.name}' already exists"))
        }
        val entity = <Entity>(
            id = UUID.randomUUID(),
            name = param.name,
            // ... fields from param
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        store[entity.id] = entity
        return success(entity)
    }

    override fun update(param: Update<Entity>Param): Result<<Entity>, AppError> {
        val validation = validateUpdate.execute(param)
        if (validation is Result.Failure) return validation

        val existing = store[param.id] ?: return failure(NotFoundError("<Entity>", param.id.toString()))
        val updatedName = param.name ?: existing.name
        if (param.name != null && store.values.any { it.name == updatedName && it.id != param.id }) {
            return failure(ConflictError("<Entity>", "name '$updatedName' already exists"))
        }
        val updated = existing.copy(
            name = updatedName,
            // ... merge nullable fields
            updatedAt = Instant.now()
        )
        store[param.id] = updated
        return success(updated)
    }

    override fun delete(param: Delete<Entity>Param): Result<Unit, AppError> {
        val validation = validateDelete.execute(param)
        if (validation is Result.Failure) return validation

        return if (store.remove(param.id) != null) success(Unit) else failure(NotFoundError("<Entity>", param.id.toString()))
    }

    fun reset() = store.clear()

    fun seed(vararg entities: <Entity>) {
        entities.forEach { store[it.id] = it }
    }
}
```

**Test DB helper** — in `src/testFixtures/` source set. Thin wrapper over the database module's `MigrationRunner`:
```kotlin
package <pkg>.clients.<client-pkg>.test

import <pkg>.databases.<db-pkg>.MigrationRunner

object <Name>TestDb {

    fun migrate(jdbcUrl: String, username: String, password: String) {
        MigrationRunner.migrate(jdbcUrl, username, password)
    }

    fun cleanAndMigrate(jdbcUrl: String, username: String, password: String) {
        MigrationRunner.cleanAndMigrate(jdbcUrl, username, password)
    }
}
```

**Integration test** — Uses Testcontainers PostgreSQL + migrations via `<Name>TestDb`:
```kotlin
<Name>TestDb.cleanAndMigrate(postgres.jdbcUrl, postgres.username, postgres.password)

System.setProperty("DB_URL", postgres.jdbcUrl)
System.setProperty("DB_USER", postgres.username)
System.setProperty("DB_PASSWORD", postgres.password)
client = create<Name>Client()
```

**IMPORTANT: `@BeforeEach` table cleanup must use `CASCADE`** to handle foreign key references from other tables. As the schema grows, new tables may add FK constraints referencing existing tables (e.g., a `world_people` join table referencing `worlds`). Always use `TRUNCATE TABLE <table> CASCADE` in test cleanup, not plain `TRUNCATE TABLE <table>`. For clients that own multiple related tables, use a single statement: `TRUNCATE TABLE <join_table>, <table_a>, <table_b>`.

---

## Command: Create Library

### Gather Information

Ask the user for:
1. **Library name** (e.g., `auth`, `validation`)
2. **Purpose** — what does this library do?
3. **Public API** — what types/functions does it expose?
4. **Root package** (if not already established)

### Scaffold

```
libs/<library-name>/
├── CLAUDE.md
├── build.gradle.kts
├── src/main/kotlin/<pkg>/libs/<lib-pkg>/
│   └── (source files)
└── src/test/kotlin/<pkg>/libs/<lib-pkg>/
    └── (test files)
```

### `build.gradle.kts`
```kotlin
plugins {
    kotlin("jvm")
}

dependencies {
    implementation("org.slf4j:slf4j-api:2.0.16")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
```

---

## Command: Create API Service

### Gather Information

Ask the user for:
1. **Service name** (e.g., `tracking-service`, `admin-service`)
2. **Purpose** — what does this service do?
3. **Initial features** — list the feature vertical slices
4. **Clients consumed** — which `clients/` does it depend on?
5. **Port** — HTTP port (default 8080)
6. **Root package** (if not already established)

### Scaffold

```
services/<service-name>/
├── CLAUDE.md
├── build.gradle.kts
└── src/
    ├── main/
    │   ├── kotlin/<pkg>/services/<svc-pkg>/
    │   │   ├── <SvcName>ServiceApplication.kt
    │   │   ├── config/
    │   │   │   ├── <Client>Config.kt
    │   │   │   └── <Feature>ServiceConfig.kt
    │   │   ├── common/error/
    │   │   │   ├── GlobalExceptionHandler.kt
    │   │   │   └── ResultExtensions.kt
    │   │   └── features/<feature>/
    │   │       ├── model/<Model>.kt
    │   │       ├── error/<Feature>Error.kt
    │   │       ├── params/<Feature>ServiceParams.kt
    │   │       ├── validations/
    │   │       │   ├── ValidateGet<Model>ById.kt
    │   │       │   ├── ValidateGetAll<Models>.kt
    │   │       │   ├── ValidateCreate<Model>.kt
    │   │       │   ├── ValidateUpdate<Model>.kt
    │   │       │   └── ValidateDelete<Model>.kt
    │   │       ├── actions/
    │   │       │   ├── Get<Model>ByIdAction.kt
    │   │       │   ├── GetAll<Models>Action.kt
    │   │       │   ├── Create<Model>Action.kt
    │   │       │   ├── Update<Model>Action.kt
    │   │       │   └── Delete<Model>Action.kt
    │   │       ├── service/
    │   │       │   └── <Feature>Service.kt           # Facade
    │   │       ├── dto/<Feature>Request.kt
    │   │       ├── dto/<Feature>Response.kt
    │   │       ├── controller/<Feature>Controller.kt
    │   │       └── mapper/<Feature>Mapper.kt
    │   └── resources/
    │       └── application.yml
    └── test/
        ├── kotlin/<pkg>/services/<svc-pkg>/
        │   ├── config/TestContainerConfig.kt
        │   └── features/<feature>/
        │       ├── service/<Feature>ServiceTest.kt
        │       └── acceptance/
        │           ├── fixture/<Feature>Fixture.kt
        │           └── <Feature>AcceptanceTest.kt
        └── resources/
            ├── application-test.yml
            ├── docker-java.properties
            └── logback-test.xml
```

### Key File Templates

**`<SvcName>ServiceApplication.kt`** — `@SpringBootApplication` + `runApplication<>()` main function:
```kotlin
package <pkg.dot>.services.<svc-pkg>

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class <SvcName>ServiceApplication

fun main(args: Array<String>) {
    runApplication<<SvcName>ServiceApplication>(*args)
}
```

**`config/<Client>Config.kt`** — `@Configuration` with `@Bean` that calls the client factory function:
```kotlin
@Configuration
class <Client>Config {
    @Bean
    fun <client>(): <Name>Client = create<Name>Client()
}
```

**`common/error/GlobalExceptionHandler.kt`** — `@RestControllerAdvice` catching `RuntimeException::class` (NOT `Exception::class` — let Spring handle 4xx errors like missing request body):
```kotlin
@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(RuntimeException::class)
    fun handleRuntimeException(ex: RuntimeException): ResponseEntity<ApiResponse.ErrorBody> {
        logger.error("Unexpected error", ex)
        return ResponseEntity.status(500)
            .body(ApiResponse.ErrorBody("INTERNAL_ERROR", "An unexpected error occurred"))
    }
}
```

**`common/error/ResultExtensions.kt`** — Extension functions mapping errors to HTTP responses.

**IMPORTANT: `@JvmName` is required** on `Result<T, E>.toResponseEntity()` extensions. Due to JVM type erasure, `Result<T, WorldError>.toResponseEntity()` and `Result<T, PersonError>.toResponseEntity()` compile to the same JVM signature. Each feature's `Result` extension MUST have a unique `@JvmName` annotation (e.g., `@JvmName("<feature>ResultToResponseEntity")`). The `<Feature>Error.toResponseEntity()` extensions (non-generic) do NOT need `@JvmName`.

```kotlin
package <pkg.dot>.services.<svc-pkg>.common.error

import <pkg.dot>.clients.common.Result
import <pkg.dot>.services.common.ApiResponse
import <pkg.dot>.services.<svc-pkg>.features.<feature>.error.<Feature>Error
import org.springframework.http.ResponseEntity

fun <Feature>Error.toResponseEntity(): ResponseEntity<Any> = when (this) {
    is <Feature>Error.NotFound -> ResponseEntity.status(404)
        .body(ApiResponse.ErrorBody("NOT_FOUND", message))
    is <Feature>Error.AlreadyExists -> ResponseEntity.status(409)
        .body(ApiResponse.ErrorBody("CONFLICT", message))
    is <Feature>Error.Invalid -> ResponseEntity.status(400)
        .body(ApiResponse.ErrorBody("BAD_REQUEST", message))
}

@JvmName("<feature>ResultToResponseEntity")
fun <T> Result<T, <Feature>Error>.toResponseEntity(
    successStatus: Int = 200,
    transform: (T) -> Any = { it as Any }
): ResponseEntity<Any> = when (this) {
    is Result.Success -> ResponseEntity.status(successStatus).body(transform(value))
    is Result.Failure -> error.toResponseEntity()
}
```
Add more `toResponseEntity()` overloads as new feature error types are added. Each `Result<T, E>` overload MUST include a unique `@JvmName` annotation.

**`features/<feature>/controller/<Feature>Controller.kt`** — `@RestController @RequestMapping("/api/<features>")`. Creates service param objects from request DTOs/path variables:
```kotlin
package <pkg.dot>.services.<svc-pkg>.features.<feature>.controller

import <pkg.dot>.clients.common.Result
import <pkg.dot>.services.<svc-pkg>.common.error.toResponseEntity
import <pkg.dot>.services.<svc-pkg>.features.<feature>.dto.*
import <pkg.dot>.services.<svc-pkg>.features.<feature>.mapper.<Feature>Mapper
import <pkg.dot>.services.<svc-pkg>.features.<feature>.params.*
import <pkg.dot>.services.<svc-pkg>.features.<feature>.service.<Feature>Service
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/<features>")
class <Feature>Controller(private val <feature>Service: <Feature>Service) {
    private val logger = LoggerFactory.getLogger(<Feature>Controller::class.java)

    @GetMapping("/{id}")
    fun getById(@PathVariable id: UUID): ResponseEntity<Any> {
        logger.info("GET /api/<features>/{}", id)
        val param = Get<Feature>ByIdParam(id = id)
        return <feature>Service.getById(param).toResponseEntity { <Feature>Mapper.toResponse(it) }
    }

    @GetMapping
    fun getAll(): ResponseEntity<Any> {
        logger.info("GET /api/<features>")
        val param = GetAll<Features>Param()
        return <feature>Service.getAll(param).toResponseEntity { list -> list.map { <Feature>Mapper.toResponse(it) } }
    }

    @PostMapping
    fun create(@RequestBody request: Create<Feature>Request): ResponseEntity<Any> {
        logger.info("POST /api/<features>")
        val param = Create<Feature>Param(name = request.name, greeting = request.greeting)
        return <feature>Service.create(param).toResponseEntity(successStatus = 201) { <Feature>Mapper.toResponse(it) }
    }

    @PutMapping("/{id}")
    fun update(@PathVariable id: UUID, @RequestBody request: Update<Feature>Request): ResponseEntity<Any> {
        logger.info("PUT /api/<features>/{}", id)
        val param = Update<Feature>Param(id = id, name = request.name, greeting = request.greeting)
        return <feature>Service.update(param).toResponseEntity { <Feature>Mapper.toResponse(it) }
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: UUID): ResponseEntity<Any> {
        logger.info("DELETE /api/<features>/{}", id)
        val param = Delete<Feature>Param(id = id)
        return <feature>Service.delete(param).toResponseEntity(successStatus = 204) { }
    }
}
```

**`features/<feature>/error/<Feature>Error.kt`** — Sealed class with typed fields + `fromClientError()` companion:
```kotlin
package <pkg.dot>.services.<svc-pkg>.features.<feature>.error

import <pkg.dot>.clients.common.error.AppError
import <pkg.dot>.clients.common.error.ConflictError
import <pkg.dot>.clients.common.error.NotFoundError
import <pkg.dot>.clients.common.error.ValidationError

sealed class <Feature>Error(override val message: String) : AppError {
    data class NotFound(val entityId: String) : <Feature>Error("<Feature> not found: $entityId")
    data class AlreadyExists(val name: String) : <Feature>Error("<Feature> already exists: $name")
    data class Invalid(val field: String, val reason: String) : <Feature>Error("Invalid <feature> $field: $reason")

    companion object {
        fun fromClientError(error: AppError): <Feature>Error = when (error) {
            is NotFoundError -> NotFound(error.id)
            is ConflictError -> AlreadyExists(error.detail)
            is ValidationError -> Invalid(error.field, error.reason)
            else -> Invalid("unknown", error.message)
        }
    }
}
```

**`features/<feature>/params/<Feature>ServiceParams.kt`** — Service-layer param types (separate from client params):
```kotlin
package <pkg.dot>.services.<svc-pkg>.features.<feature>.params

import java.util.UUID

data class Get<Feature>ByIdParam(val id: UUID)
data class GetAll<Features>Param(val limit: Int? = null, val offset: Int? = null)
data class Create<Feature>Param(val name: String, val greeting: String)
data class Update<Feature>Param(val id: UUID, val name: String? = null, val greeting: String? = null)
data class Delete<Feature>Param(val id: UUID)
```

**Service validation classes** — 1:1 with actions in `features/<feature>/validations/`. Same `execute()/validate()` pattern as client validators, but error type is `<Feature>Error` (not `AppError`). Ask the user for checks; default to `success(Unit)`.

Validation class with checks:
```kotlin
package <pkg.dot>.services.<svc-pkg>.features.<feature>.validations

import <pkg.dot>.clients.common.Result
import <pkg.dot>.clients.common.failure
import <pkg.dot>.clients.common.success
import <pkg.dot>.services.<svc-pkg>.features.<feature>.error.<Feature>Error
import <pkg.dot>.services.<svc-pkg>.features.<feature>.params.Create<Feature>Param
import org.slf4j.LoggerFactory

internal class ValidateCreate<Feature> {
    private val logger = LoggerFactory.getLogger(ValidateCreate<Feature>::class.java)

    fun execute(param: Create<Feature>Param): Result<Unit, <Feature>Error> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: Create<Feature>Param): Result<Unit, <Feature>Error> {
        if (param.name.isBlank()) return failure(<Feature>Error.Invalid("name", "must not be blank"))
        if (param.greeting.isBlank()) return failure(<Feature>Error.Invalid("greeting", "must not be blank"))
        return success(Unit)
    }
}
```

Default validation class (no checks):
```kotlin
internal class ValidateGet<Feature>ById {
    private val logger = LoggerFactory.getLogger(ValidateGet<Feature>ById::class.java)

    fun execute(param: Get<Feature>ByIdParam): Result<Unit, <Feature>Error> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: Get<Feature>ByIdParam): Result<Unit, <Feature>Error> {
        return success(Unit)
    }
}
```

**Service action classes** — 1:1 with service methods in `features/<feature>/actions/`. Each action takes a service param, validates, converts to client param (using import alias), and calls the client:
```kotlin
package <pkg.dot>.services.<svc-pkg>.features.<feature>.actions

import <pkg.dot>.clients.common.Result
import <pkg.dot>.clients.<client-pkg>.api.<Client>Client
import <pkg.dot>.clients.<client-pkg>.api.Create<Entity>Param as ClientCreate<Entity>Param
import <pkg.dot>.services.<svc-pkg>.features.<feature>.error.<Feature>Error
import <pkg.dot>.services.<svc-pkg>.features.<feature>.mapper.<Feature>Mapper
import <pkg.dot>.services.<svc-pkg>.features.<feature>.model.<Feature>
import <pkg.dot>.services.<svc-pkg>.features.<feature>.params.Create<Feature>Param
import <pkg.dot>.services.<svc-pkg>.features.<feature>.validations.ValidateCreate<Feature>
import org.slf4j.LoggerFactory

internal class Create<Feature>Action(private val <client>Client: <Client>Client) {
    private val logger = LoggerFactory.getLogger(Create<Feature>Action::class.java)
    private val validate = ValidateCreate<Feature>()

    fun execute(param: Create<Feature>Param): Result<<Feature>, <Feature>Error> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Creating <feature> name={}", param.name)
        return when (val result = <client>Client.create(ClientCreate<Entity>Param(name = param.name, greeting = param.greeting))) {
            is Result.Success -> Result.Success(<Feature>Mapper.fromClient(result.value))
            is Result.Failure -> Result.Failure(<Feature>Error.fromClientError(result.error))
        }
    }
}
```

Follow the same pattern for all 5 actions: `Get<Feature>ByIdAction`, `GetAll<Features>Action`, `Create<Feature>Action`, `Update<Feature>Action`, `Delete<Feature>Action`. Each uses an import alias for the corresponding client param type.

**`features/<feature>/service/<Feature>Service.kt`** — Facade delegating to action classes. No `@Service` annotation (wired via factory):
```kotlin
package <pkg.dot>.services.<svc-pkg>.features.<feature>.service

import <pkg.dot>.clients.<client-pkg>.api.<Client>Client
import <pkg.dot>.services.<svc-pkg>.features.<feature>.actions.*
import <pkg.dot>.services.<svc-pkg>.features.<feature>.params.*

class <Feature>Service(<client>Client: <Client>Client) {
    private val get<Feature>ById = Get<Feature>ByIdAction(<client>Client)
    private val getAll<Features> = GetAll<Features>Action(<client>Client)
    private val create<Feature> = Create<Feature>Action(<client>Client)
    private val update<Feature> = Update<Feature>Action(<client>Client)
    private val delete<Feature> = Delete<Feature>Action(<client>Client)

    fun getById(param: Get<Feature>ByIdParam) = get<Feature>ById.execute(param)
    fun getAll(param: GetAll<Features>Param) = getAll<Features>.execute(param)
    fun create(param: Create<Feature>Param) = create<Feature>.execute(param)
    fun update(param: Update<Feature>Param) = update<Feature>.execute(param)
    fun delete(param: Delete<Feature>Param) = delete<Feature>.execute(param)
}
```

**`config/<Feature>ServiceConfig.kt`** — Spring `@Configuration` wiring the service directly:
```kotlin
package <pkg.dot>.services.<svc-pkg>.config

import <pkg.dot>.clients.<client-pkg>.api.<Client>Client
import <pkg.dot>.services.<svc-pkg>.features.<feature>.service.<Feature>Service
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class <Feature>ServiceConfig {
    @Bean
    fun <feature>Service(<client>Client: <Client>Client): <Feature>Service = <Feature>Service(<client>Client)
}
```

**`features/<feature>/mapper/<Feature>Mapper.kt`** — Object with:
- `fromClient(ClientModel): ServiceModel` — adapts client model to service model (import alias: `ClientModel`)
- `toResponse(ServiceModel): ResponseDTO` — converts to DTO

### Service `build.gradle.kts`
```kotlin
plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation(project(":clients:client-common"))
    implementation(project(":clients:<client-name>"))
    implementation(project(":services:service-common"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-jdbc")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:postgresql:1.20.4")
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")
    testImplementation("org.postgresql:postgresql:42.7.4")
    testImplementation(testFixtures(project(":clients:<client-name>")))
}

tasks.withType<Test> {
    systemProperty("project.root", rootProject.projectDir.absolutePath)
    systemProperty("spring.profiles.active", "test")
}
```

### `application.yml`

**IMPORTANT:** Do NOT include `spring.datasource` or `spring.flyway` config here. The client factory reads DB connection details from environment variables directly — Spring's auto-configured DataSource is not used by the main application.

```yaml
spring:
  application:
    name: <service-name>

management:
  endpoints:
    web:
      exposure:
        include: health,info
```

### `application-test.yml`

**IMPORTANT:** Must include `spring.main.allow-bean-definition-overriding: true` because `TestContainerConfig` defines a `@Primary` bean that overrides the main `@Configuration` bean for the client. Must include `spring.datasource` so Spring can auto-configure a `JdbcTemplate` for test fixtures.

```yaml
spring:
  main:
    allow-bean-definition-overriding: true
  datasource:
    url: ${DB_URL}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
```

---

## Command: Add Service API

### Gather Information

Ask the user for:
1. **Which service** (look under `services/`)
2. **Feature name** (e.g., `tracking`, `shipment`)
3. **Domain model** — entity name and fields
4. **API endpoints** — list of routes
5. **Client dependencies** — which clients does this feature use?
6. **Error cases** — expected failure modes

### Steps

1. Read the service's `CLAUDE.md` and existing code to understand structure.
2. Create the feature directory under `features/<name>/` with:
   - `model/` — domain model
   - `error/` — `<Feature>Error` sealed class with `fromClientError()` companion
   - `params/` — service param objects
   - `validations/` — 1:1 with actions (ask user for checks; default to `success(Unit)`)
   - `actions/` — 1:1 with service methods (validate → convert params → call client)
   - `service/` — facade class (no `@Service`, wired via `@Configuration` bean)
   - `dto/` — request/response DTOs
   - `controller/` — REST controller creating service params from requests
   - `mapper/` — `fromClient()` + `toResponse()`
3. Create `config/<Feature>ServiceConfig.kt` wiring the factory via `@Bean`.
4. Add `<Feature>Error.toResponseEntity()` and `@JvmName("<feature>ResultToResponseEntity") Result.toResponseEntity()` to `ResultExtensions.kt`.
5. Create tests: service test (with fakes) and acceptance test (with fixture).
6. Update the service-level `CLAUDE.md` with the new feature.

---

## Command: Add Client Function

### Gather Information

Ask the user for:
1. **Which client** (look under `clients/`)
2. **Function signature** — name, parameters, return type
3. **Behavior** — what should it do?
4. **Error cases** — what can go wrong?

### Steps

1. Read the client's `CLAUDE.md` and existing `api/<Name>Client.kt` interface.
2. Add the param data class to `<Name>ClientParams.kt`.
3. Add the method (with KDoc) to the interface.
4. Create a new validation class in `internal/validations/` (ask the user for checks; default to `success(Unit)`).
5. Create a new operation class in `internal/operations/` that calls the validator.
6. Wire it through the facade in `Jdbi<Name>Client.kt`.
7. Add to `fake/Fake<Name>Client.kt` (in testFixtures), referencing the validator.
8. Write tests for the new function (include validation tests if checks exist).
9. Update the client's `CLAUDE.md`.

---

## Command: Add Library Function

### Gather Information

Ask the user for:
1. **Which library** (look under `libs/`)
2. **What to add** — types, functions, etc.
3. **Behavior** — what should it do?

### Steps

1. Read the library's `CLAUDE.md` and existing code.
2. Add the new types/functions.
3. Write tests.
4. Update the library's `CLAUDE.md`.
