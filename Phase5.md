# Phase 5 — E-Commerce Project Bootstrap

> **Goal:** By the end of this phase you'll have a multi-module Gradle project for an
> e-commerce platform, with structured logging, database migrations, architecture tests,
> and foundational design decisions documented. No Spring Boot yet — this phase is about
> the project skeleton and ecosystem tooling.
>
> **Domain shift.** The Library domain is complete. From here, all exercises target the
> e-commerce platform in a **separate repository**. The phase file lives here (it's course
> material), but all code you write goes in the new `ecommerce` repo.
>
> **Starting point:** You've built a full domain in a single-module Maven project. You know
> JUnit 5, AssertJ, Jackson, TDD, streams, concurrency, and Java's type system. What you
> haven't seen: how real Java projects are structured at scale — and the build tool that
> most modern Java projects actually use.

---

## 5.1 — Multi-Module Gradle

### Read First

**What you know:** A single `pom.xml` with `<dependencies>` that pulls in JUnit, AssertJ,
and Jackson. Everything lives in one `src/main/java` tree.

**What changes:**

Two things change at once. First, real Java projects split into modules — each with its own
source tree and dependencies. Second, the build tool changes. Maven served you well for
learning, but Gradle is the default for modern Spring Boot projects, and the one you'll use
from here.

**Why Gradle over Maven:**

- Spring Boot defaults to Gradle (Spring Initializr, official guides, most tutorials)
- Kotlin DSL gives you IDE autocomplete and type checking in build files — XML gives you
  neither
- Less verbose: a multi-module Gradle build is roughly a third the line count of the
  equivalent Maven setup
- Incremental builds and build cache — Gradle only re-runs what changed, Maven rebuilds
  everything
- Version catalogs (`libs.versions.toml`) are a cleaner central version management story
  than `<dependencyManagement>` XML

You already understand Maven's model: dependency scopes, transitive dependencies, build
lifecycle. Those concepts transfer directly — the syntax changes, the semantics don't.

**The TS comparison:**

| Concept | TypeScript | Java (Gradle) |
|---------|-----------|--------------|
| Monorepo structure | `packages/` with Nx/Turborepo | `include()` in `settings.gradle.kts` |
| Per-package config | `package.json` per package | `build.gradle.kts` per module |
| Shared dependency versions | root `package.json` + hoisting | Version catalog (`libs.versions.toml`) |
| Build order | Nx dependency graph | Gradle task graph (auto-detects from `dependencies`) |
| Inter-package imports | `@myorg/common` | `implementation(project(":common"))` |
| Shared config | Nx generators / root `tsconfig` | `subprojects {}` or convention plugins |

**Gradle project structure:**

A Gradle multi-module project has:

- `settings.gradle.kts` — declares which subdirectories are modules (≈ Maven's `<modules>`)
- `build.gradle.kts` at the root — shared configuration for all modules
- `build.gradle.kts` in each module — module-specific dependencies and plugins
- `gradle/libs.versions.toml` — central version catalog (optional but strongly recommended)

```kotlin
// settings.gradle.kts
rootProject.name = "ecommerce"

include("common")
include("catalog")
include("order")
```

```kotlin
// build.gradle.kts (root)
plugins {
    java
}

subprojects {
    apply(plugin = "java")

    group = "com.example.ecommerce"
    version = "0.1.0-SNAPSHOT"

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }

    repositories {
        mavenCentral()
    }

    dependencies {
        testImplementation(libs.junit.jupiter)
        testImplementation(libs.assertj.core)
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }

    tasks.test {
        useJUnitPlatform()
    }
}
```

```kotlin
// catalog/build.gradle.kts
dependencies {
    implementation(project(":common"))
}
```

```toml
# gradle/libs.versions.toml
[versions]
junit = "5.11.4"
assertj = "3.27.7"

[libraries]
junit-jupiter = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
assertj-core = { module = "org.assertj:assertj-core", version.ref = "assertj" }
```

**Version catalogs — the key concept:**

The `libs.versions.toml` file is Gradle's answer to Maven's `<dependencyManagement>`.
Versions are declared once; modules reference them by alias:

- `libs.versions.toml` defines `junit-jupiter = { module = "...", version.ref = "junit" }`
- Any `build.gradle.kts` references it as `libs.junit.jupiter`
- The version is resolved from the catalog — no version strings scattered across build files

The TS mental model: version catalogs are like pinning versions in the root `package.json`
of an Nx monorepo. Each package declares what it uses; versions come from the catalog.

**Dependency configurations (≈ Maven scopes):**

| Gradle | Maven equivalent | When to use |
|--------|-----------------|-------------|
| `implementation` | `compile` | Standard dependency, not exposed to consumers |
| `api` | `compile` | Dependency exposed through your public API |
| `runtimeOnly` | `runtime` | Needed at runtime, not compile (e.g., JDBC drivers) |
| `testImplementation` | `test` | Test-only dependency |
| `compileOnly` | `provided` | Compile-time only (e.g., annotation processors) |

The `implementation` vs `api` distinction has no Maven equivalent — it's stricter.
`implementation` means "I use this internally but don't leak it to my consumers." This
prevents accidental transitive dependency chains. Use `implementation` by default; only
use `api` when the dependency appears in your module's public types.

**Build commands:**

| Command | What it does |
|---------|-------------|
| `./gradlew build` | Compiles + runs tests (≈ `mvn verify`) |
| `./gradlew test` | Compiles + runs tests |
| `./gradlew :catalog:test` | Runs tests in just the `catalog` module |
| `./gradlew clean` | Deletes all `build/` directories |
| `./gradlew dependencies` | Shows dependency tree (≈ `mvn dependency:tree`) |
| `./gradlew build --continuous` | Watches for changes and rebuilds (no Maven equivalent) |

**Task graph:** Gradle reads the `project(":common")` dependencies between modules and
figures out the build order automatically. If `catalog` depends on `common`, Gradle
compiles `common` first. Circular dependencies are a build error.

**Gradle wrapper (`./gradlew`):** The `gradlew` script (and `gradle/wrapper/` directory)
pin the Gradle version for the project. Everyone — CI, teammates, you — uses the same
Gradle version without installing it globally. Maven has an equivalent (`mvnw`) but it's
less universally adopted. Always use `./gradlew`, never a globally installed `gradle`.

**Watch out:** IntelliJ has excellent Gradle support — it auto-imports `build.gradle.kts`
changes. If things get stale, use the Gradle tool window → reload button. The Kotlin DSL
gives you red squiggles on build file errors, which Maven's XML never could.

### Exercise 5.1a — Scaffold the e-commerce project

Create a new repository (outside this crash course repo) for the e-commerce platform.

1. **Generate a Gradle project:**
   - The easiest way: run `gradle init` and choose "application", "Java", "Kotlin DSL",
     then restructure. Or scaffold it manually — the files are simple enough.
   - Alternatively, use Spring Initializr (start.spring.io) to generate a Gradle project
     and strip out the Spring parts you don't need yet — this gives you a working Gradle
     wrapper and build structure.
   - Either way, make sure you have the Gradle wrapper (`gradlew`, `gradlew.bat`,
     `gradle/wrapper/`)

2. **Set up the root `settings.gradle.kts`:**
   - `rootProject.name = "ecommerce"`
   - `include("common", "catalog", "order", "inventory", "cart", "payment", "search", "notification")`

3. **Set up the root `build.gradle.kts`:**
   - Apply the `java` plugin to all subprojects
   - Set Java toolchain to 25
   - Configure `mavenCentral()` as the repository
   - Set up common test dependencies (JUnit, AssertJ) in `subprojects {}`

4. **Create a version catalog (`gradle/libs.versions.toml`):**
   - Pin versions for JUnit Jupiter and AssertJ
   - Reference them in the root `build.gradle.kts`

5. **Create modules as subdirectories, each with its own `build.gradle.kts`:**
   - `common` — shared value objects, interfaces, base types
   - `catalog` — product catalog (the first bounded context you'll build)
   - `order` — order management (empty for now)
   - `inventory` — stock management (empty for now)
   - `cart` — shopping cart (empty for now)
   - `payment` — payment processing (empty for now)
   - `search` — product search (empty for now)
   - `notification` — event-driven notifications (empty for now)

6. **Wire up inter-module dependencies** in each module's `build.gradle.kts`:
   - `catalog` depends on `common`: `implementation(project(":common"))`
   - `order` depends on `common`
   - `inventory` depends on `common`
   - Other modules: just `common` for now

7. **Verify:** `./gradlew clean build` from the root should compile all modules and run
   any tests (there won't be tests yet, but the build should succeed)

**Hints:**
- Each module directory needs `src/main/java/com/example/ecommerce/<module>/` and
  `src/test/java/com/example/ecommerce/<module>/`
- Empty modules need at least a `build.gradle.kts` file (can be empty or just have
  `// intentionally empty`)
- For inter-module dependencies, use `implementation(project(":modulename"))`
- If a module's `build.gradle.kts` is empty, it still inherits everything from the root's
  `subprojects {}` block

**Watch out:** Two things must be in your shared build configuration for JUnit 5 tests to
run (the root `build.gradle.kts` example above includes both):
- `tasks.test { useJUnitPlatform() }` — tells Gradle to use the JUnit 5 test engine
- `testRuntimeOnly("org.junit.platform:junit-platform-launcher")` — the launcher that
  JUnit 5 needs on the classpath at runtime

Without either, you'll get "No matching tests found" even though the tests are there.

**Watch out:** Unlike Maven, Gradle doesn't require `install` before cross-module
references work. The project references resolve directly within the build. No local
repository step needed.

### Exercise 5.1b — A first test across modules

Verify that cross-module dependencies actually work.

1. **In `common`:** Create a value object — something like `ProductId` (a record, same
   pattern as `BookId`/`MemberId` in the library). Put it in
   `com.example.ecommerce.common`.

2. **In `catalog`:** Create a test that imports `ProductId` from `common` and does
   something trivial with it (construct one, assert its value).

3. **Run `./gradlew test` from the root.** Both modules should compile, the test should
   pass.

This proves the multi-module dependency wiring works. If it fails, the error message will
tell you whether it's a dependency declaration problem or a build order problem.

**Hints:**
- If you get "package does not exist" — check that `catalog/build.gradle.kts` has
  `implementation(project(":common"))`
- Gradle's error messages for dependency resolution are generally more helpful than Maven's

---

## 5.2 — Architecture Decision Records

### Read First

An ADR is a short document that captures a significant technical decision. Format:

```markdown
# [number]. [Title]

**Date:** YYYY-MM-DD
**Status:** Accepted | Superseded | Deprecated

## Context
What is the issue that we're seeing that is motivating this decision or change?

## Decision
What is the change that we're proposing and/or doing?

## Consequences
What becomes easier or more difficult to do because of this change?
```

**Why bother:** Six months from now, someone (including future you) will ask "why Gradle
instead of Maven?" or "why didn't we use Axon Server?" The commit history shows *what*
changed but not *why*. ADRs capture the reasoning — the trade-offs you considered, the
constraints that drove the choice.

**The TS comparison:** You might have seen RFCs or decision docs in larger TS projects.
ADRs are a lighter-weight version — one decision per file, stored in the repo alongside
the code. No approval workflow, just documentation.

ADRs are numbered and immutable. If a decision changes, you write a new ADR that supersedes
the old one (and update the old one's status to "Superseded by ADR-00XX"). This gives you
a timeline of how the architecture evolved.

### Exercise 5.2a — Write the initial ADRs

Create `docs/adr/` in the e-commerce repo. ADRs should be written **when you make a
decision**, not before. The only decisions that are genuinely made before writing code are
the technology choices — everything else (architecture patterns, CQRS vs CRUD boundaries,
communication strategies) should be captured as you encounter them.

Write these four now — they're prerequisites for starting the project:

1. **`0001-use-java-25.md`**
   - Why Java 25 over an LTS release (21)?
   - Consequence: access to latest language features, but may need to update when the next
     LTS lands

2. **`0002-use-spring-boot-4.md`**
   - Why Spring Boot over alternatives (Quarkus, Micronaut, plain Java)?
   - Consequence: massive ecosystem and community, but heavyweight compared to alternatives

3. **`0003-use-gradle.md`**
   - Why Gradle over Maven?
   - Think about: Kotlin DSL with IDE support, Spring Boot ecosystem alignment, build
     performance (incremental builds, build cache), version catalogs
   - Consequence: more expressive and faster, but a Turing-complete build language means
     more rope to hang yourself with — keep build files simple

4. **`0004-unified-observability-with-opentelemetry.md`**
   - Why OpenTelemetry with wide structured events over traditional logging-centric
     observability?
   - Think about: the unified observability model (Charity Majors et al) — structured
     events with high-cardinality attributes as the primary observability primitive, not
     three separate pillars (logs, metrics, traces)
   - SLF4J/Logback still present for ecosystem compatibility (libraries log through it),
     but your own code instruments with OTel spans and attributes, not log statements
   - Consequence: richer debugging and querying capability, but requires discipline to
     attach meaningful attributes to spans rather than reaching for `log.info()`

Write them yourself — the exercise is thinking through the consequences, not just filling
in a template.

**Future ADRs** (write these when you actually make the decision, not before):
- Module architecture (monolith vs microservices, hexagonal structure) — when you set up
  the module structure
- CQRS/ES vs CRUD per bounded context — when you build each context
- Event store implementation (Axon Server vs JPA) — when you add event sourcing
- Inter-module communication (events vs direct calls) — when two modules first need to
  talk
- Message broker (or lack thereof) — when in-process events stop being sufficient

**Watch out:** Don't overthink these. ADRs are a paragraph or two each, not essays. The
point is to capture the decision and the key trade-offs, not to write a thesis. You can
always update them later.

---

## 5.3 — Logging with SLF4J + Logback

### Read First

**The TS comparison:**

In TS/Node, you probably use `console.log()` or a library like Winston or Pino. Java has a
similar split: a logging *facade* (the API you code against) and a logging *implementation*
(the engine that actually writes logs).

| Concept | TypeScript | Java |
|---------|-----------|------|
| Logger API | `console` or Winston's `logger` | SLF4J (`Logger`) |
| Implementation | Winston/Pino transport config | Logback (or Log4j2) |
| Structured output | Pino JSON format | Logback JSON encoder |
| Per-module log levels | Winston category levels | Logback `<logger>` elements |

**Why a facade?** Your code imports `org.slf4j.Logger`. The runtime classpath determines
which implementation handles those calls. Swap Logback for Log4j2? Change one dependency,
zero code changes. This matters because libraries you import also use SLF4J — everyone
logs through the same facade.

**Getting a logger:**

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProductService {
    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    public void createProduct(String name) {
        log.info("Creating product: {}", name);
    }
}
```

Passing the class to `getLogger` means the logger name matches the fully-qualified class
name — `com.example.ecommerce.catalog.ProductService`. This is how you configure
per-package or per-class log levels.

**Parameterized messages — this is important:**

```java
// GOOD — parameter substitution, message only built if level is enabled
log.debug("Processing order {} for member {}", orderId, memberId);

// BAD — string concatenation happens even if debug is disabled
log.debug("Processing order " + orderId + " for member " + memberId);
```

The `{}` placeholders avoid string concatenation overhead when the log level is disabled.
With the bad version, Java builds the string every time, even if debug logging is off. In
a hot path, this adds up.

**Log levels:**

| Level | When to use |
|-------|-------------|
| `ERROR` | Something failed and needs attention. A request couldn't be fulfilled, data is corrupted, an integration is down. |
| `WARN` | Something unexpected but recoverable. Fallback used, retry triggered, deprecated API called. |
| `INFO` | Significant business events. Order placed, payment captured, user registered. The level you'd monitor in production. |
| `DEBUG` | Detailed flow for troubleshooting. Method entry/exit, intermediate values, decision points. Off in production. |
| `TRACE` | Very granular. Raw payloads, loop iterations, detailed timing. Almost never enabled. |

**The rule of thumb:** If you'd `console.log()` it during development, it's `DEBUG`. If you
want to see it in production dashboards, it's `INFO`. If it wakes someone up at 3am, it's
`ERROR`.

**Logback configuration:**

Logback looks for `logback.xml` (or `logback-test.xml` in test classpath) on the classpath.
Place it in `src/main/resources/` (or `src/test/resources/` for test-specific config):

```xml
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="STDOUT" />
    </root>

    <!-- Set debug level for your own code -->
    <logger name="com.example.ecommerce" level="DEBUG" />
</configuration>
```

The pattern format:
- `%d{HH:mm:ss.SSS}` — timestamp
- `[%thread]` — thread name (useful for concurrency debugging — you've seen why)
- `%-5level` — log level, left-padded to 5 characters
- `%logger{36}` — logger name, abbreviated to 36 characters
- `%msg%n` — the message + newline

**MDC (Mapped Diagnostic Context):**

MDC lets you attach context to every log line within a scope — like a request ID that
appears in every log message for that request, without passing it through every method:

```java
MDC.put("requestId", UUID.randomUUID().toString());
try {
    // every log.info(), log.debug() etc. in this scope includes the requestId
    processOrder(order);
} finally {
    MDC.clear();
}
```

Then in the pattern: `%d [%thread] [%X{requestId}] %-5level %logger{36} - %msg%n`

Every log line in that scope gets `[abc-123-def]` prepended. In production, this lets you
grep all logs for a single request across multiple classes and services.

**Watch out:** Never log sensitive data — passwords, tokens, credit card numbers, PII. A
log statement like `log.info("User logged in: {}", user)` that calls `user.toString()` might
dump the entire object including password hashes. Be explicit about what you log.

### Exercise 5.3a — Add logging to the e-commerce project

1. **Add dependencies to the version catalog (`gradle/libs.versions.toml`):**
   - `org.slf4j:slf4j-api`
   - `ch.qos.logback:logback-classic` (this pulls in `logback-core` transitively)
   - Look up the latest stable versions

2. **Add these as dependencies in the `common` module's `build.gradle.kts`** (other modules
   will use them too — you can add them to `subprojects {}` in the root, or keep them
   per-module)

3. **Create a `logback-test.xml`** in `common/src/test/resources/`:
   - Console appender with a clear pattern that includes timestamp, thread, level, logger,
     and message
   - Root level `INFO`
   - `com.example.ecommerce` level `DEBUG`

4. **Write a test** that creates a logger and logs at different levels. This isn't a test
   in the assertion sense — it's verification that logging is configured correctly. Look at
   the console output and verify you see `INFO` and `DEBUG` messages from your code, but
   not `TRACE`.

**Hints:**
- `LoggerFactory.getLogger(YourTest.class)` gives you a logger in the test
- You won't assert on log output (that's fragile). Just eyeball the console.
- The test's value is proving the config works and that you understand the level hierarchy

### Exercise 5.3b — MDC and contextual logging

1. **Write a small service class** in `common` (or `catalog`) that takes an operation and
   logs its start and end. Before the operation, put a correlation ID into MDC. After, clear
   it.

2. **Write a test** that calls the service and check the console output — the correlation
   ID should appear in every log line within the scope.

3. **Update your logback pattern** to include `%X{correlationId}` (or whatever key you
   chose).

**Hints:**
- `MDC.put("correlationId", value)` and `MDC.remove("correlationId")` in a `finally` block
- `UUID.randomUUID().toString()` for the correlation ID
- In a real Spring app, this would live in a filter/interceptor — for now, do it manually
- Think about what happens with virtual threads and MDC — each virtual thread has its own
  MDC copy (since Java 21's ScopedValue is still evolving, MDC uses ThreadLocal)

---

## 5.4 — Observability with OpenTelemetry

### Read First

**The shift from logs to events:**

Traditional observability is built on three pillars — logs, metrics, traces — each collected
and queried separately. The unified observability model (as described in *Observability
Engineering* by Charity Majors, Liz Fong-Jones, and George Miranda) argues this is
backwards. Instead of correlating three separate data streams after the fact, you emit
**structured events** — wide, high-cardinality events that capture everything about a unit
of work in one place.

The key insight: a trace span *is* a structured event. If you attach enough context to each
span (user ID, product ID, query count, cache hit/miss, feature flags, error details), you
don't need separate log lines or metric counters — you query your traces directly.

**The TS comparison:**

| Concept | TypeScript | Java |
|---------|-----------|------|
| Instrumentation API | `@opentelemetry/api` | `io.opentelemetry:opentelemetry-api` |
| Auto-instrumentation | `@opentelemetry/auto-instrumentations-node` | OpenTelemetry Java Agent (javaagent JAR) |
| Manual spans | `tracer.startSpan("operation")` | `tracer.spanBuilder("operation").startSpan()` |
| Span attributes | `span.setAttribute("key", value)` | `span.setAttribute("key", value)` |
| Context propagation | `context.with(span)` | `try (Scope scope = span.makeCurrent())` |
| Exporter | `OTLPTraceExporter` | `OtlpGrpcSpanExporter` |

The API surface is nearly identical. If you've used OpenTelemetry in Node, the Java SDK
will feel familiar.

**OpenTelemetry architecture:**

- **API** — the interfaces you code against (`Tracer`, `Span`, `SpanBuilder`). Like SLF4J
  for tracing — your code depends only on this.
- **SDK** — the implementation that processes and exports telemetry. You configure this at
  application startup.
- **Exporters** — send data to a backend (Jaeger, Honeycomb, Grafana Tempo, console).
- **Java Agent** — a JVM agent that auto-instruments common libraries (Spring, JDBC,
  HTTP clients) without code changes. Attaches via `-javaagent:opentelemetry-javaagent.jar`.

**Manual instrumentation:**

```java
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;

public class CatalogService {
    private final Tracer tracer;

    public Product findProduct(ProductId id) {
        Span span = tracer.spanBuilder("CatalogService.findProduct")
            .setAttribute("product.id", id.value().toString())
            .startSpan();
        try (Scope scope = span.makeCurrent()) {
            Product product = repository.findById(id);
            span.setAttribute("product.found", product != null);
            return product;
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
}
```

**Wide events — the key practice:**

The value isn't in creating spans. It's in what you attach to them. A span for an HTTP
request handler might carry:

- `user.id`, `user.tier` (who)
- `product.id`, `product.category` (what)
- `db.query_count`, `cache.hit` (how the system behaved)
- `feature_flag.new_search` (what code path ran)
- `duration_ms`, `response.status` (outcome)

This is what makes high-cardinality querying possible: "show me all requests where
`user.tier=premium` and `db.query_count > 10` and `cache.hit=false`." You can't do that
with log lines.

**SLF4J bridge:**

OpenTelemetry can capture SLF4J log output as log events attached to the active span. This
means your existing `log.info(...)` calls aren't wasted — they become part of the trace
context. The Java agent does this automatically.

### Exercise 5.4a — OpenTelemetry with console exporter

Set up OpenTelemetry in the `common` module with a console exporter — no external
infrastructure needed.

1. **Add dependencies to the version catalog:**
   - `io.opentelemetry:opentelemetry-bom` — import as a platform dependency. This manages
     versions for all OpenTelemetry artifacts.
   - In the root `build.gradle.kts` or the version catalog, set up the BOM so child modules
     can depend on individual artifacts without version numbers

2. **Add dependencies to `common`'s `build.gradle.kts`:**
   - `io.opentelemetry:opentelemetry-api`
   - `io.opentelemetry:opentelemetry-sdk` (test scope for now — you'll configure the SDK
     in tests)
   - `io.opentelemetry:opentelemetry-exporter-logging` (test scope — exports spans to
     stdout)

3. **Write a test** that:
   - Configures an OpenTelemetry SDK instance with a simple span processor and logging
     exporter
   - Creates a tracer
   - Starts a span, adds attributes, ends it
   - Verifies the span appears in console output

**Hints:**
- SDK setup in a test:
  ```java
  SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
      .addSpanProcessor(SimpleSpanProcessor.create(LoggingSpanExporter.create()))
      .build();
  OpenTelemetry otel = OpenTelemetrySdk.builder()
      .setTracerProvider(tracerProvider)
      .build();
  Tracer tracer = otel.getTracer("test");
  ```
- Gradle BOMs are imported with `platform()`:
  ```kotlin
  dependencies {
      implementation(platform(libs.opentelemetry.bom))
      implementation("io.opentelemetry:opentelemetry-api")  // no version needed
  }
  ```
- Like the logging exercise, this is about seeing it work, not asserting on output
- Look at what the logging exporter prints — you'll see span name, trace ID, attributes

### Exercise 5.4b — Wide events in the domain

1. **Create a service class** in `catalog` (or `common`) that performs a multi-step
   operation — for example, a product lookup that checks a cache, queries a repository,
   and applies a promotion.

2. **Instrument it with spans** that carry meaningful attributes:
   - The operation name
   - Business identifiers (product ID, category)
   - Behavioural data (cache hit/miss, items found, duration)
   - Use nested spans for sub-operations (the cache check, the DB query)

3. **Write a test** that runs the operation and inspect the console output. You should see
   parent-child span relationships and the attributes you attached.

**Hints:**
- Use `Span.current()` to access the active span and add attributes mid-operation
- Nested spans: start a new span while a parent is current — OpenTelemetry links them
  automatically via context
- Think about what fields you'd want to query on if debugging a slow product page —
  those are your attributes
- The `Tracer` should be injected (constructor parameter), same pattern as `Clock`

**Watch out:** Don't over-instrument. In production, you'd instrument at service boundaries
and key decision points, not every method. The goal is events rich enough to answer
arbitrary questions, not a trace of every line of code.

---

## 5.5 — Database Migrations with Flyway

### Read First

**The TS comparison:**

| Concept | TypeScript | Java |
|---------|-----------|------|
| Migration tool | Prisma Migrate / TypeORM migrations | Flyway |
| Migration format | TS/JS files or generated SQL | SQL files (or Java classes) |
| Naming convention | Timestamp-based | `V1__description.sql`, `V2__description.sql` |
| Execution | CLI or on app startup | On app startup (Spring Boot auto-config) or CLI |
| Version tracking | `_prisma_migrations` table | `flyway_schema_history` table |

**How Flyway works:**

1. You write SQL migration files with a naming convention: `V1__create_product_table.sql`
2. On startup, Flyway checks the `flyway_schema_history` table to see which migrations
   have already run
3. It runs any new migrations in order
4. It checksums each migration file — if you change a migration that's already been applied,
   Flyway refuses to run (this prevents silent schema drift)

The naming convention matters:
- `V` = versioned (runs once, in order)
- `1` = the version number
- `__` = double underscore separator
- `create_product_table` = description (for humans)
- `.sql` = plain SQL

**Testcontainers — real databases in tests:**

H2 and other in-memory databases have compatibility modes, but they're never quite the same
as PostgreSQL. You'll hit differences in SQL syntax, type handling, and behavior. The modern
approach: run a real PostgreSQL instance in a Docker container during tests.

Testcontainers handles this:

```java
@Testcontainers
class ProductRepositoryTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");
}
```

The container starts before tests, provides a JDBC URL, and is torn down after. Your tests
run against real PostgreSQL with real Flyway migrations — exactly what production runs.

**Dependencies you'll need:**

- `org.flywaydb:flyway-core` — the migration engine
- `org.flywaydb:flyway-database-postgresql` — PostgreSQL support
- `org.testcontainers:testcontainers` — the core Testcontainers library
- `org.testcontainers:junit-jupiter` — JUnit 5 integration
- `org.testcontainers:postgresql` — PostgreSQL container support
- `org.postgresql:postgresql` — the PostgreSQL JDBC driver

**Watch out:** Testcontainers needs Docker running on your machine. If Docker Desktop isn't
running, the tests will fail with a connection error. Also, the first run downloads the
PostgreSQL Docker image — that takes a minute but only happens once.

### Exercise 5.5a — Set up Flyway with Testcontainers

This exercise is in the `catalog` module, since that's the first bounded context you'll
build with a database.

1. **Add dependencies to the version catalog:**
   - Flyway core + PostgreSQL module
   - Testcontainers core + JUnit 5 + PostgreSQL
   - PostgreSQL JDBC driver
   - Look up compatible versions — Testcontainers and Flyway both have BOMs you can import
     with `platform()` in Gradle to manage version alignment

2. **Add the dependencies to `catalog/build.gradle.kts`:**
   - Flyway: `implementation` (needed at runtime)
   - PostgreSQL driver: `runtimeOnly` (only needed at runtime, not compile time)
   - Testcontainers: `testImplementation`

3. **Write your first migration:**
   - `catalog/src/main/resources/db/migration/V1__create_product_table.sql`
   - A simple `CREATE TABLE product (...)` with columns: `id UUID PRIMARY KEY`,
     `name VARCHAR(255) NOT NULL`, `description TEXT`, `price DECIMAL(10,2) NOT NULL`,
     `active BOOLEAN NOT NULL DEFAULT true`, `created_at TIMESTAMP NOT NULL DEFAULT NOW()`
   - Use PostgreSQL types — you're not targeting H2 compatibility

4. **Write a test** that:
   - Starts a PostgreSQL Testcontainer
   - Runs Flyway migrations against it programmatically
   - Verifies the `product` table exists (query `information_schema.tables`)

**Hints:**
- Create Flyway programmatically for now (Spring Boot will auto-configure it later):
  ```java
  Flyway flyway = Flyway.configure()
      .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
      .load();
  flyway.migrate();
  ```
- To verify the table exists, use JDBC directly:
  ```java
  try (var conn = DriverManager.getConnection(
          postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
       var stmt = conn.createStatement();
       var rs = stmt.executeQuery(
          "SELECT table_name FROM information_schema.tables WHERE table_name = 'product'")) {
      assertThat(rs.next()).isTrue();
  }
  ```
- The `@Testcontainers` and `@Container` annotations handle lifecycle — the container
  starts before the test class and stops after
- `@Container` on a `static` field means one container per test class (shared across test
  methods). On an instance field, it's one container per test method (slower but more
  isolated).

**Watch out:** Migration files go in `src/main/resources/db/migration/` — this is Flyway's
default classpath location. If you put them elsewhere, you need to configure
`flyway.locations`.

### Exercise 5.5b — A second migration

1. **Write `V2__create_category_table.sql`:**
   - `category` table with `id UUID PRIMARY KEY`, `name VARCHAR(255) NOT NULL UNIQUE`,
     `description TEXT`

2. **Write `V3__add_category_to_product.sql`:**
   - Add a `category_id UUID` column to `product`
   - Add a foreign key constraint referencing `category`

3. **Update your test** to verify both tables exist and the foreign key relationship works
   (insert a category, then a product referencing it).

4. **Experiment:** Edit `V1__create_product_table.sql` (change a column name) and re-run
   the test. Observe Flyway's checksum validation error. Undo your edit.

**Hints:**
- Use `ALTER TABLE product ADD COLUMN category_id UUID REFERENCES category(id)` in V3
- The checksum experiment is important — it demonstrates why migrations are immutable once
  applied. This is the same principle as not editing a published npm package.

---

## 5.6 — ArchUnit — Enforcing Architecture

### Read First

**What ArchUnit does:** It's a test library that asserts things about your code's
*structure* — which packages depend on which, what naming conventions are followed, where
annotations are allowed. It runs as a normal JUnit test.

**The TS comparison:** Think of it as ESLint rules, but for architectural constraints
instead of code style. ESLint can enforce "no unused variables." ArchUnit enforces "the
`domain` package must not import from `infrastructure`."

**Why this matters:** In a multi-module project, module boundaries are your primary
defence against accidental coupling. Gradle enforces that module A can't use module B's
classes unless it declares a dependency. But *within* a module, packages are just
directories — nothing stops your domain model from importing a Spring annotation, or
your controller from reaching into the repository directly. ArchUnit fills that gap.

**Basic API:**

```java
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.example.ecommerce")
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainMustNotDependOnSpring =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("org.springframework..");

    @ArchTest
    static final ArchRule repositoriesShouldBeInPersistencePackage =
        classes()
            .that().haveSimpleNameEndingWith("Repository")
            .and().areNotInterfaces()
            .should().resideInAPackage("..persistence..");
}
```

`@AnalyzeClasses` tells ArchUnit which packages to scan. `@ArchTest` marks each rule.
Rules read almost like English — "no classes that reside in domain should depend on classes
that reside in springframework."

**Rule patterns you'll use:**

- **Package dependency rules:** `noClasses().that().resideInAPackage("..domain..").should().dependOnClassesThat().resideInAPackage("..infrastructure..")`
- **Naming conventions:** `classes().that().implement(Repository.class).should().haveSimpleNameEndingWith("Repository")`
- **Annotation restrictions:** `noClasses().that().resideInAPackage("..domain..").should().beAnnotatedWith(Entity.class)`
- **Layer rules:** `layeredArchitecture().layer("domain").definedBy("..domain..").layer("api").definedBy("..api..").whereLayer("domain").mayNotAccessAnyLayer()`

### Exercise 5.6a — ArchUnit rules for the e-commerce project

Add ArchUnit to the project and write rules that will guide the architecture as you build.

1. **Add `com.tngtech.archunit:archunit-junit5` to the version catalog**, then add it as a
   `testImplementation` dependency in modules where you want architecture tests.

2. **Create `src/test/java/com/example/ecommerce/ArchitectureTest.java`** in the `common`
   module (or a dedicated test module if you prefer).

3. **Write these rules:**

   a. **Domain independence:** Classes in `..domain..` packages must not depend on Spring
      framework classes, JPA annotations, or Jackson annotations. The domain model should
      be pure Java.

   b. **No `java.util.logging`:** All logging must go through SLF4J. No class in the
      project should import `java.util.logging`.

   c. **No field injection:** No field should be annotated with `@Autowired`. Constructor
      injection only. (You'll write Spring code in Phase 6 — this rule will be ready.)

   d. **Package structure:** Classes annotated with `@RestController` should reside in
      `..api..` packages. (This won't have matches yet, but the rule is ready for Phase 6.)

4. **Run the tests.** They should all pass (trivially — there's not much code yet). The
   value is that they'll catch violations as you build.

**Hints:**
- `@AnalyzeClasses(packages = "com.example.ecommerce")` scans all modules on the test
  classpath
- Start simple. You can always add more rules as the architecture takes shape.
- ArchUnit has a caching mechanism — `@AnalyzeClasses` scans classes once and reuses the
  result across all `@ArchTest` fields in the class. This is why rules are `static final`
  fields, not test methods.
- Import from `com.tngtech.archunit.lang.syntax.ArchRuleDefinition` and
  `com.tngtech.archunit.library.Architectures`

**Watch out:** ArchUnit rules that are too strict early on will slow you down. Start with
the few critical rules above. Add more as you discover patterns worth enforcing. The goal
is to prevent architectural decay, not to micromanage every class placement.

---

## 5.7 — Date & Time API (`java.time`)

### Read First

**The TS comparison:**

JavaScript's `Date` is famously awful. You probably use `date-fns`, `dayjs`, or `luxon`.
Java had the same problem — `java.util.Date` and `Calendar` were terrible. Then Java 8
introduced `java.time`, which is excellent. It was designed by the same person who wrote
Joda-Time (the Java equivalent of Moment.js).

| Concept | TypeScript (date-fns/luxon) | Java (`java.time`) |
|---------|---------------------------|-------------------|
| Date only | `startOfDay()` / Luxon `DateTime` | `LocalDate` |
| Time only | Not common | `LocalTime` |
| Date + time, no timezone | `new Date()` (but it has TZ...) | `LocalDateTime` |
| Date + time + timezone | Luxon `DateTime` with zone | `ZonedDateTime` |
| UTC instant | `Date.now()` | `Instant` |
| Duration (hours/minutes) | `intervalToDuration()` | `Duration` |
| Period (days/months/years) | `differenceInDays()` etc. | `Period` |
| Formatting | `format(date, 'yyyy-MM-dd')` | `DateTimeFormatter.ofPattern("yyyy-MM-dd")` |

**The key types:**

- **`Instant`** — a point on the UTC timeline. Nanosecond precision. Use this for
  timestamps: "when did this event happen?" Equivalent to `Date.now()` but immutable.

- **`LocalDate`** — a date without time or timezone: `2026-03-18`. Use for "the promotion
  runs from March 18 to March 25." No concept of what time zone you're in.

- **`LocalTime`** — a time without date: `14:30:00`. Rare in business logic, but useful for
  "store opens at 09:00."

- **`LocalDateTime`** — date + time, no timezone: `2026-03-18T14:30:00`. Use when the
  timezone doesn't matter or is implied. **Not for timestamps** — without a timezone, the
  same `LocalDateTime` means different instants in different time zones.

- **`ZonedDateTime`** — date + time + timezone: `2026-03-18T14:30:00+13:00[Pacific/Auckland]`.
  Use when you need to display a time to a user in their timezone.

- **`Duration`** — time-based amount: "3 hours and 30 minutes." Use for timeouts, shipping
  estimates, elapsed time.

- **`Period`** — date-based amount: "2 months and 5 days." Use for subscription lengths,
  age calculation.

**Immutable and thread-safe:**

Every `java.time` object is immutable. Operations return new instances:

```java
LocalDate today = LocalDate.now();
LocalDate nextWeek = today.plusWeeks(1);    // today is unchanged
LocalDate birthday = LocalDate.of(1990, Month.JUNE, 15);
```

This means they're automatically thread-safe — exactly like your records and value objects.

**Parsing and formatting:**

```java
DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
LocalDate parsed = LocalDate.parse("18/03/2026", formatter);
String formatted = parsed.format(formatter);  // "18/03/2026"

// ISO formats work out of the box:
Instant.parse("2026-03-18T14:30:00Z");
LocalDate.parse("2026-03-18");
```

**Comparing and calculating:**

```java
LocalDate start = LocalDate.of(2026, 3, 1);
LocalDate end = LocalDate.of(2026, 3, 18);
Period between = Period.between(start, end);    // P17D (17 days)
long days = ChronoUnit.DAYS.between(start, end); // 17

boolean isAfter = end.isAfter(start);           // true
boolean isInRange = !today.isBefore(start) && !today.isAfter(end);
```

**`Clock` — testable time:**

`LocalDate.now()` and `Instant.now()` use the system clock. In tests, you want
deterministic time. Inject a `Clock`:

```java
public class PromotionService {
    private final Clock clock;

    public PromotionService(Clock clock) {
        this.clock = clock;
    }

    public boolean isActive(LocalDate start, LocalDate end) {
        LocalDate today = LocalDate.now(clock);
        return !today.isBefore(start) && !today.isAfter(end);
    }
}
```

In production: `new PromotionService(Clock.systemDefaultZone())`
In tests: `new PromotionService(Clock.fixed(Instant.parse("2026-03-18T00:00:00Z"), ZoneId.of("UTC")))`

This is dependency injection for time — same pattern as injecting a fake repository. No
mocks needed.

### Exercise 5.7a — Date/time in the e-commerce domain

These exercises live in the e-commerce project. Put them wherever makes sense — `common`
for shared types, `catalog` or `order` for domain-specific ones.

1. **`Money` value object:**
   - A record with `BigDecimal amount` and `Currency currency` (use `java.util.Currency`)
   - Why `BigDecimal`? Because `double` has floating-point errors: `0.1 + 0.2 != 0.3`.
     Financial calculations must use `BigDecimal`.
   - Add methods: `add(Money)`, `subtract(Money)`, `multiply(int quantity)`
   - Throw if currencies don't match on add/subtract
   - Write tests. Think about edge cases: different currencies, zero, negative amounts.

2. **`DateRange` value object:**
   - A record with `LocalDate start` and `LocalDate end`
   - Validation: `start` must not be after `end`
   - Methods: `contains(LocalDate)`, `overlaps(DateRange)`, `duration()` (returns `Period`)
   - Write tests for: containment, overlap detection, edge cases (same start/end, adjacent
     ranges)

3. **`Promotion` with time-aware logic:**
   - A class that holds a description, a discount percentage, and a `DateRange`
   - Method: `isActive(Clock)` — is the promotion currently active?
   - Write tests using `Clock.fixed(...)` to control the date. Test: before promotion,
     during, after, on boundary dates.

**Hints:**
- `BigDecimal` arithmetic: `amount.add(other)`, `amount.subtract(other)`,
  `amount.multiply(BigDecimal.valueOf(quantity))`. Like `java.time`, `BigDecimal` is
  immutable — operations return new instances.
- `java.util.Currency.getInstance("USD")` gets a currency by ISO code
- For `DateRange.overlaps()`, two ranges overlap if `start1 <= end2 AND start2 <= end1`
- `Clock.fixed(instant, zone)` creates a clock that always returns the same time
- `LocalDate.now(clock)` uses the injected clock instead of the system clock
- Import from `java.time.*` — everything you need is in this package

**Watch out:** `BigDecimal` comparison is tricky. `new BigDecimal("1.0").equals(new
BigDecimal("1.00"))` returns `false` — they have different *scales*. Use
`compareTo() == 0` for value equality, or `stripTrailingZeros()` before comparing.

---

## 5.8 — Capstone

### Exercise 5.8a — Verify the full skeleton

Run through this checklist to make sure everything is wired correctly:

1. **`./gradlew clean build` from the root** — all modules compile, all tests pass
2. **Cross-module dependency works** — `catalog` can import types from `common`
3. **Logging works** — tests produce formatted log output with correct levels
4. **OpenTelemetry works** — spans with attributes appear in console output
5. **Flyway migrations run** — Testcontainers PostgreSQL starts, tables are created
6. **ArchUnit rules pass** — all architecture constraints are satisfied
7. **Value objects are tested** — `Money`, `DateRange`, `ProductId` all have test coverage
8. **ADRs are committed** — `docs/adr/` contains your 4 initial decision records (more as the architecture evolves)

If all of this works, the e-commerce project is ready for Spring Boot in Phase 6.

---

## Review

After completing Phase 5, your e-commerce project should have:

- [ ] Multi-module Gradle project with version catalog, shared `subprojects {}` config, and
  per-module `build.gradle.kts` files
- [ ] 8 modules (most empty, `common` and `catalog` with initial code)
- [ ] Cross-module dependencies working (`catalog` → `common`)
- [ ] SLF4J + Logback configured with proper log levels and MDC
- [ ] OpenTelemetry SDK with manual instrumentation and wide events
- [ ] Flyway migrations running against Testcontainers PostgreSQL
- [ ] ArchUnit rules enforcing architectural boundaries
- [ ] `java.time` fluency — `LocalDate`, `Instant`, `Duration`, `Clock` for testable time
- [ ] `Money` and `DateRange` value objects with full test coverage
- [ ] 4 ADRs documenting foundational decisions
- [ ] All tests passing with `./gradlew clean build`

**Consider:**
1. How does multi-module Gradle compare to Nx/Turborepo? What's better, what's worse?
2. Where do you draw module boundaries? The current split (by bounded context) is one
   approach. What are the trade-offs vs splitting by layer (api, domain, persistence)?
3. How would you add a new module? What changes in `settings.gradle.kts` vs the new
   module's `build.gradle.kts`?
4. The `Clock` injection pattern for testable time — how does this compare to mocking
   `Date.now()` in TS tests? Which approach do you prefer?
