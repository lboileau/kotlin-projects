---
name: create-acceptance-tests
description: Create end-to-end acceptance tests for a Kotlin Spring Boot API service using Testcontainers. Use when the user wants to add acceptance tests.
user-invocable: true
---

# Create Acceptance Tests

You are a test engineer creating end-to-end acceptance tests for a Kotlin Spring Boot API service. These tests verify the full request-response cycle including database interactions using a real PostgreSQL instance via Testcontainers.

## Scope

Single-service acceptance tests. Each service tests its own API surface. Multi-service integration tests are out of scope.

## Commands

1. **Create Acceptance Tests** — Scaffold full acceptance test coverage for a feature
2. **Add Test Flow** — Add a custom multi-call workflow test to an existing feature's acceptance tests

---

## Gather Information

Ask the user for:
1. **Which service** (look under `services/`)
2. **Which features to test** (look under `services/<name>/src/main/kotlin/.../features/`)
3. **DB schemas involved** (look under `databases/` for the tables and seed data)

Then read:
- The service's `CLAUDE.md` and `build.gradle.kts`
- Each feature's controller (for routes), DTOs, error hierarchy
- The database schema files

---

## Conventions

### Test Infrastructure Lives at Service Level
```
services/<service-name>/src/test/kotlin/<pkg>/services/<svc-pkg>/
├── config/
│   └── TestContainerConfig.kt
└── features/<feature>/
    ├── service/<Feature>ServiceTest.kt
    └── acceptance/
        ├── fixture/<Feature>Fixture.kt
        └── <Feature>AcceptanceTest.kt
```

### Test Resources
```
services/<service-name>/src/test/resources/
├── docker-java.properties         # api.version=1.44
└── logback-test.xml               # Test logging config
```

### Principles
- **Real stack:** Testcontainers PostgreSQL + Spring Boot + `TestRestTemplate` HTTP calls
- **Clean slate:** Every test starts with an empty database (tables truncated in `@BeforeEach`)
- **Fixtures insert via DB, not API.** Use `JdbcTemplate` direct SQL inserts for test setup. Use the API only for the action under test.
- **Test independence:** No test depends on another test's side effects. No shared mutable state.
- **Feature-organized:** Test files mirror the feature structure.

### Test Naming
```kotlin
`POST creates entity and returns 201`()
`GET returns 404 when entity does not exist`()
`POST returns 409 when email already exists`()
`DELETE removes entity and returns 204`()
`POST then GET returns the created entity`()
`POST then PUT then GET returns the updated entity`()
```

### Coverage Targets
For each endpoint, test:
1. **Happy path** — successful operation
2. **Not found** — entity doesn't exist (404)
3. **Validation errors** — malformed input (400)
4. **Conflicts** — uniqueness violations (409)
5. **Edge cases** — empty collections, boundary values
6. **Read-your-own-writes** — multi-call workflows verifying data consistency

---

## Scaffold

### `config/TestContainerConfig.kt`
```kotlin
package <pkg>.services.<svc-pkg>.config

import <pkg>.clients.<client-pkg>.test.<Name>TestDb
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.PostgreSQLContainer

@TestConfiguration
class TestContainerConfig {

    @Bean
    @ServiceConnection
    fun postgresContainer(): PostgreSQLContainer<*> =
        PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("<db_name>")
            .withUsername("postgres")
            .withPassword("postgres")
            .also { container ->
                container.start()
                <Name>TestDb.migrate(container.jdbcUrl, container.username, container.password)
                System.setProperty("DB_URL", container.jdbcUrl)
                System.setProperty("DB_USER", container.username)
                System.setProperty("DB_PASSWORD", container.password)
            }
}
```

The `@ServiceConnection` annotation auto-configures Spring's DataSource for test fixtures (JdbcTemplate). The `<Name>TestDb.migrate()` call runs migrations via the database module's `MigrationRunner`. System properties are set so the client factory connects to the test container.

### `docker-java.properties`
```properties
api.version=1.44
```

Required for Docker Engine 29.x compatibility with Testcontainers 1.x.

### `logback-test.xml`
```xml
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <logger name="<pkg.dot>" level="DEBUG" />
    <logger name="org.testcontainers" level="WARN" />
    <logger name="com.github.dockerjava" level="WARN" />

    <root level="INFO">
        <appender-ref ref="STDOUT" />
    </root>
</configuration>
```

---

## For Each Feature

### `fixture/<Feature>Fixture.kt`

Fixtures use `JdbcTemplate` and insert directly into the database.

```kotlin
package <pkg>.services.<svc-pkg>.features.<feature>.acceptance.fixture

import org.springframework.jdbc.core.JdbcTemplate
import java.time.Instant
import java.util.UUID

class <Feature>Fixture(private val jdbcTemplate: JdbcTemplate) {

    fun insert<Entity>(
        id: UUID = UUID.randomUUID(),
        // all fields with unique defaults to prevent collisions between tests
        // use UUID prefix for string fields: e.g. name = "<Entity>-${UUID.randomUUID().toString().take(8)}"
        createdAt: Instant = Instant.now(),
        updatedAt: Instant = Instant.now()
    ): UUID {
        jdbcTemplate.update(
            """
            INSERT INTO <table> (id, <columns>, created_at, updated_at)
            VALUES (?, <placeholders>, ?, ?)
            """.trimIndent(),
            id, /* field values */, java.sql.Timestamp.from(createdAt), java.sql.Timestamp.from(updatedAt)
        )
        return id
    }

    fun truncateAll() {
        jdbcTemplate.execute("TRUNCATE TABLE <table> CASCADE")
    }

    companion object {
        val KNOWN_ID_1: UUID = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
        val KNOWN_ID_2: UUID = UUID.fromString("b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22")
    }
}
```

### `<Feature>AcceptanceTest.kt`

```kotlin
package <pkg>.services.<svc-pkg>.features.<feature>.acceptance

import <pkg>.services.<svc-pkg>.config.TestContainerConfig
import <pkg>.services.<svc-pkg>.features.<feature>.acceptance.fixture.<Feature>Fixture
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Import
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestContainerConfig::class)
class <Feature>AcceptanceTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    private val mapper = jacksonObjectMapper()
    private lateinit var fixture: <Feature>Fixture

    @BeforeEach
    fun setUp() {
        fixture = <Feature>Fixture(jdbcTemplate)
        fixture.truncateAll()
    }

    @Nested
    inner class Get<Entity> {
        @Test
        fun `GET returns 200 with entity when found`() { ... }

        @Test
        fun `GET returns 404 when entity does not exist`() { ... }
    }

    @Nested
    inner class GetAll<Entity> {
        @Test
        fun `GET returns 200 with empty list when no entities exist`() { ... }

        @Test
        fun `GET returns 200 with all entities`() { ... }
    }

    @Nested
    inner class Create<Entity> {
        @Test
        fun `POST creates entity and returns 201`() { ... }

        @Test
        fun `POST returns 409 when unique constraint violated`() { ... }

        @Test
        fun `POST returns 400 when required field is blank`() { ... }
    }

    @Nested
    inner class Update<Entity> {
        @Test
        fun `PUT updates entity and returns 200`() { ... }

        @Test
        fun `PUT returns 404 when entity does not exist`() { ... }

        @Test
        fun `PUT returns 409 when renaming to existing name`() { ... }
    }

    @Nested
    inner class Delete<Entity> {
        @Test
        fun `DELETE removes entity and returns 204`() { ... }

        @Test
        fun `DELETE returns 404 when entity does not exist`() { ... }
    }

    @Nested
    inner class ReadYourOwnWrites {
        @Test
        fun `POST then GET returns the created entity`() {
            // 1. POST to create
            val createRequest = mapper.writeValueAsString(mapOf("name" to "Earth", "greeting" to "Hello!"))
            val createResponse = restTemplate.exchange("/api/<entities>", HttpMethod.POST, jsonEntity(createRequest), String::class.java)
            assertThat(createResponse.statusCode.value()).isEqualTo(201)
            val id = mapper.readValue<Map<String, Any>>(createResponse.body!!)["id"] as String

            // 2. GET to verify
            val getResponse = restTemplate.getForEntity("/api/<entities>/$id", String::class.java)
            assertThat(getResponse.statusCode.value()).isEqualTo(200)
            val body = mapper.readValue<Map<String, Any>>(getResponse.body!!)
            assertThat(body["name"]).isEqualTo("Earth")
        }

        @Test
        fun `POST then PUT then GET returns the updated entity`() {
            // 1. POST to create
            val createRequest = mapper.writeValueAsString(mapOf("name" to "Earth", "greeting" to "Hello!"))
            val createResponse = restTemplate.exchange("/api/<entities>", HttpMethod.POST, jsonEntity(createRequest), String::class.java)
            val id = mapper.readValue<Map<String, Any>>(createResponse.body!!)["id"] as String

            // 2. PUT to update
            val updateRequest = mapper.writeValueAsString(mapOf("name" to "Terra", "greeting" to "Hola!"))
            val updateResponse = restTemplate.exchange("/api/<entities>/$id", HttpMethod.PUT, jsonEntity(updateRequest), String::class.java)
            assertThat(updateResponse.statusCode.value()).isEqualTo(200)

            // 3. GET to verify
            val getResponse = restTemplate.getForEntity("/api/<entities>/$id", String::class.java)
            val body = mapper.readValue<Map<String, Any>>(getResponse.body!!)
            assertThat(body["name"]).isEqualTo("Terra")
            assertThat(body["greeting"]).isEqualTo("Hola!")
        }

        @Test
        fun `POST then DELETE then GET returns 404`() {
            // 1. POST to create
            val createRequest = mapper.writeValueAsString(mapOf("name" to "Earth", "greeting" to "Hello!"))
            val createResponse = restTemplate.exchange("/api/<entities>", HttpMethod.POST, jsonEntity(createRequest), String::class.java)
            val id = mapper.readValue<Map<String, Any>>(createResponse.body!!)["id"] as String

            // 2. DELETE
            val deleteResponse = restTemplate.exchange("/api/<entities>/$id", HttpMethod.DELETE, null, String::class.java)
            assertThat(deleteResponse.statusCode.value()).isEqualTo(204)

            // 3. GET to verify gone
            val getResponse = restTemplate.getForEntity("/api/<entities>/$id", String::class.java)
            assertThat(getResponse.statusCode.value()).isEqualTo(404)
        }
    }

    private fun jsonEntity(body: String): HttpEntity<String> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        return HttpEntity(body, headers)
    }
}
```

### Key patterns in acceptance tests:
- Use `restTemplate.getForEntity()` for GET requests
- Use `restTemplate.exchange()` with `HttpMethod.POST/PUT/DELETE` + `jsonEntity()` for bodies
- Parse responses with `jacksonObjectMapper().readValue<Map<String, Any>>()` or `readValue<List<Map<String, Any>>>()`
- Assert on `response.statusCode.value()` (not `statusCodeValue` which is deprecated)

---

## Command: Add Test Flow

### Gather Information

Ask the user for:
1. **Which service and feature** — identifies the acceptance test file
2. **Test flow description** — describe the multi-call workflow (e.g., "create two entities, verify list returns both, delete one, verify list returns only the remaining one")
3. **Expected assertions** — what should be verified at each step

### Steps

1. Read the existing acceptance test file for the feature.
2. Add a new `@Nested` inner class or add tests to the existing `ReadYourOwnWrites` class.
3. Each step in the workflow should:
   - Make an API call
   - Assert the response status code
   - If the response is needed for the next step, parse it and extract the relevant data
   - If a step fails, the test fails immediately (no try-catch, let AssertJ fail naturally)

### Multi-Call Workflow Pattern
```kotlin
@Test
fun `description of the full workflow`() {
    // Step 1: <action>
    val step1Response = restTemplate.exchange(...)
    assertThat(step1Response.statusCode.value()).isEqualTo(<expected>)
    val step1Data = mapper.readValue<...>(step1Response.body!!)

    // Step 2: <action using step1 data>
    val step2Response = restTemplate.exchange(...)
    assertThat(step2Response.statusCode.value()).isEqualTo(<expected>)

    // Step 3: <verify final state>
    val verifyResponse = restTemplate.getForEntity(...)
    assertThat(verifyResponse.statusCode.value()).isEqualTo(<expected>)
    val verifyBody = mapper.readValue<...>(verifyResponse.body!!)
    assertThat(verifyBody[...]).isEqualTo(...)
}
```

Key principles for multi-call workflows:
- Each workflow is a single test method — if any step fails, the whole test fails
- Parse response bodies to extract IDs and data needed for subsequent calls
- Always verify the final state with a GET call
- No shared state between test methods — each test sets up its own data via API calls or fixtures

---

## Steps Summary

1. **Read** the service structure, features, controllers, DTOs, and DB schemas.
2. **Create** test infrastructure if it doesn't exist (`TestContainerConfig`, resource files).
3. **For each feature:**
   a. Create `fixture/<Feature>Fixture.kt` with insert and truncate methods.
   b. Create `<Feature>AcceptanceTest.kt` with nested test classes per endpoint.
   c. Cover: happy path, not found, validation errors, conflicts, read-your-own-writes.
4. **Verify** `build.gradle.kts` has `systemProperty("project.root", rootProject.projectDir.absolutePath)` and `docker-java.properties` exists.
5. **Update** service CLAUDE.md with test information.
