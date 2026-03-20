# Phase 5 — E-Commerce Project Bootstrap

> **Goal:** By the end of this phase you'll have a multi-module Gradle project for an
> e-commerce platform, with foundational design decisions documented. This phase is
> deliberately slim — just the project skeleton and build tooling. Everything else
> (logging, database migrations, observability, architecture tests) gets introduced
> just-in-time in Phase 6 when you have a real reason to need it.
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

## 5.3 — Capstone

### Exercise 5.3a — Verify the skeleton

Run through this checklist to make sure everything is wired correctly:

1. **`./gradlew clean build` from the root** — all modules compile, all tests pass
2. **Cross-module dependency works** — `catalog` can import types from `common`
3. **ADRs are committed** — `docs/adr/` contains your 4 initial decision records

If all of this works, the e-commerce project is ready for Phase 6.

---

## Review

After completing Phase 5, your e-commerce project should have:

- [ ] Multi-module Gradle project with version catalog and per-module `build.gradle.kts`
  files
- [ ] 8 modules (most empty, `common` and `catalog` with initial code)
- [ ] Cross-module dependencies working (`catalog` → `common`)
- [ ] 4 ADRs documenting foundational technology decisions
- [ ] All tests passing with `./gradlew clean build`

**Consider:**
1. How does multi-module Gradle compare to Nx/Turborepo? What's better, what's worse?
2. Where do you draw module boundaries? The current split (by bounded context) is one
   approach. What are the trade-offs vs splitting by layer (api, domain, persistence)?
3. How would you add a new module? What changes in `settings.gradle.kts` vs the new
   module's `build.gradle.kts`?
