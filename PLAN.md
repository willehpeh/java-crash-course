# Java Crash Course — Plan

> For an experienced TypeScript/Angular developer with some .NET background.
> Goal: get fully operational in Java as fast as possible, with depth where it matters.
>
> **Approach: TDD-first, Detroit school.**
> - Every concept is learned by writing a failing test, then making it pass.
> - No mock frameworks (no Mockito). Dependencies are handled with **fakes** (in-memory
>   implementations of interfaces) and **stubs** (simple objects returning canned values).
> - This naturally teaches good Java design: programming to interfaces, dependency inversion,
>   and ports & adapters — all driven by the tests, not by framework magic.

### Two Repositories, One Learning Path

This crash course spans **two repositories**:

1. **`java-crash-course`** (this repo) — Phases 0–4 use a **Library Management System** as the
   unifying domain. Standalone concept-exploration tests exist alongside the project tests.
2. **`ecommerce`** (separate repo) — Starting at Phase 5, exercises target an **e-commerce platform**
   that the developer builds independently. The crash course provides structured exercises; the
   developer owns the codebase.

**The transition:** Phases 0–4 teach the language through the Library domain. Phase 5 pivots to the
e-commerce project for ecosystem tooling. Phase 6 builds the first bounded context (Catalog) with
Spring Boot. After Phase 6, the structured course ends — the developer has enough Java fluency to
build independently, using Claude Code as a resource when needed.

---

## Phase 0 — Testing Toolkit & Build Essentials

> Get your tools sharp before you start cutting.

### 0.1 Maven Essentials (just enough to move)
- `pom.xml` anatomy — groupId, artifactId, dependencies, plugins
- Adding dependencies (JUnit 5, AssertJ) — the Java equivalent of `npm install --save-dev`
- Dependency scopes: `compile`, `test`, `provided`, `runtime`
- Running tests: `mvn test`, `mvn test -Dtest=MyTest`, `mvn test -Dtest=MyTest#myMethod`
- The `surefire` plugin — why test classes must end in `*Test.java`
- Project layout: `src/main/java`, `src/test/java`, `src/main/resources`, `src/test/resources`
- Package naming = directory structure (enforced, unlike TS/ES modules)

### 0.2 JUnit 5 Fundamentals
- Your first test class — anatomy, conventions
- `@Test`, `@DisplayName`
- Running from IDE vs Maven
- Contrast with Jest/Jasmine: `describe`/`it` → class/method

### 0.3 Assertions
- JUnit basics: `assertEquals`, `assertTrue`, `assertNotNull`, `assertThrows`, `assertAll`
- AssertJ fluent assertions — closer to Jest's `expect()` style
  - `assertThat(x).isEqualTo(y)`, `.contains()`, `.hasSize()`
  - `.extracting()` for pulling fields out of object lists
  - `.satisfies()`, `.matches()` for custom conditions
- Why AssertJ: readable, discoverable via IDE autocomplete, better error messages

### 0.4 Test Lifecycle & Organization
- `@BeforeEach`, `@AfterEach`, `@BeforeAll`, `@AfterAll`
- `@Nested` classes — grouping tests (≈ `describe` blocks in Jest)
- `@ParameterizedTest` with `@ValueSource`, `@CsvSource`, `@MethodSource`
- `@Disabled`, `@Tag` — skipping and filtering
- Test naming conventions: `shouldDoX_whenY` or `givenX_whenY_thenZ`

### 0.5 Detroit-School TDD in Java
- Red → Green → Refactor — the cycle
- **No mocks.** How we handle dependencies instead:
  - **Fakes**: in-memory implementations of interfaces (e.g., `InMemoryBookRepository implements BookRepository`)
  - **Stubs**: simple objects that return canned data
  - **Self-shunt**: the test class itself implements an interface when convenient
- Why this matters: tests verify *behavior*, not *implementation*
- How this naturally drives you toward interfaces and dependency inversion
- Test source layout mirrors main: `src/test/java/org/example/` ↔ `src/main/java/org/example/`

### 0.6 Debugging in IntelliJ (brief guided tour)
- Setting breakpoints, conditional breakpoints
- Evaluate expression, watches
- Step into/over/out — same concepts as Chrome DevTools, different keys
- Running a single test in debug mode

---

## Phase 1 — Language Mechanics

> Builds on Phase 0. You already wrote interfaces, records, classes with `final` fields,
> constructors, `List`, `Map`, lambdas, and basic streams. Each section here focuses on
> what you haven't seen yet, and exercises extend the existing library code.

### 1.1 Equality, Identity & the Type System
- `==` vs `.equals()` — the #1 Java trap (== compares references for objects)
- Primitives vs boxed types, autoboxing, the `Integer` cache (127 vs 128)
- `.equals()` and `.hashCode()` contract — why records gave you equality for free
- `null` semantics: no `undefined`, `null` unboxing crashes
- **Domain exercise:** introduce `BookId` / `MemberId` value records, refactor the repository

### 1.2 Strings, Text & Formatting
- String immutability, key methods, `.equals()` reinforcement
- Text blocks (`"""`), `String.format()` / `.formatted()`, `StringBuilder`
- **Domain exercise:** `Book` record with title validation, loan receipt formatter

### 1.3 Classes: What Phase 0 Skipped
- Access modifiers in depth (especially package-private as a design tool)
- `final` on methods/classes, method overloading, `static` members, `@Override`
- Nominal typing — structurally identical classes are NOT interchangeable
- **Domain exercise:** `Book` class with static factory methods, overloaded methods

### 1.4 Interfaces & Abstract Classes: Beyond Basics
- `default` methods, `static` methods on interfaces
- Abstract classes — when and why (shared state/constructor logic)
- `protected` access, multiple interface implementation
- **Domain exercise:** `Searchable` interface with default method, abstract repository base class

### 1.5 Enums & Records in Depth
- Enums as full classes: fields, constructors, methods, per-constant behavior
- `EnumSet`, `EnumMap`, enums in `switch`
- Record compact constructors, custom methods on records
- **Domain exercise:** `LoanStatus` state machine, `BookGenre` with fields

### 1.6 Sealed Types & Pattern Matching
- `sealed` interfaces with `permits`, exhaustive `switch`
- Pattern matching in `instanceof` and `switch`, record destructuring
- **Domain exercise:** `LibraryEvent` sealed hierarchy with pattern-matching event processor

### 1.7 Generics: Writing Your Own
- Writing generic classes/interfaces/methods, type erasure
- Bounded types: `<T extends Comparable<T>>`
- **Domain exercise:** generic `Result<T>` type with `Success`/`Failure` states

### 1.8 Null Safety & Optional
- No built-in null safety, `Optional<T>` conventions (return types only)
- `.map()`, `.flatMap()`, `.orElse()`, `.orElseThrow()`
- **Domain exercise:** `Optional`-returning repository methods, refactor `LendingService`

### 1.9 Capstone — Evolve the Library Domain
- Not a rewrite — refactor Phase 0 code to use everything from 1.1–1.8
- Value object IDs, enums, sealed events, `Result<T>`, `Optional`, enriched domain model

---

## Phase 2 — Collections, Streams & Functional Java

### 2.1 Collections Framework
- **Tests:** model the library's catalog and member lists; assert contents, ordering, uniqueness
- `List`, `Set`, `Map` — interfaces vs implementations (`ArrayList`, `HashSet`, `LinkedHashSet`, `HashMap`, `TreeMap`)
- When to use which: `ArrayList` (default), `HashSet` (unique, unordered), `LinkedHashSet` (unique, ordered), `TreeMap` (sorted keys)
- Immutable collections: `List.of()`, `Map.of()`, `Set.of()`, `Collections.unmodifiable*()`
- `List.of()` returns truly unmodifiable (throws on `.add()`); `Collections.unmodifiableList()` wraps a mutable list
- Iterating: enhanced for-loop, `.forEach()`, iterators
- `Comparable` vs `Comparator` — natural ordering vs custom ordering
- **Watch out:** `List.of(1, 2, 3)` is immutable. `new ArrayList<>(List.of(1, 2, 3))` is mutable. You'll mix these up.

### 2.2 Lambdas & Functional Interfaces (before Streams)
- **Tests:** write and test custom functional interfaces, then use standard ones
- Lambda syntax: `(x) -> x + 1`, `(x, y) -> x + y`, multi-line with braces
- Standard functional interfaces: `Function<T,R>`, `Predicate<T>`, `Consumer<T>`, `Supplier<T>`, `BiFunction<T,U,R>`
- Method references: `Book::title`, `String::toLowerCase`, `this::isValid`
- `@FunctionalInterface` — what it enforces
- Effectively final variables in closures — no mutation like JS (compiler enforced)
- **Watch out:** Java lambdas can only capture effectively final variables. No `let count = 0; list.forEach(x -> count++)`.

### 2.3 Streams API (≈ RxJS pipe but synchronous & pull-based)
- **Tests:** library search/filter/aggregate operations — find overdue books, group by genre, etc.
- `.stream()` → intermediate operations → terminal operation
- Intermediate: `.filter()`, `.map()`, `.flatMap()`, `.sorted()`, `.distinct()`, `.peek()`
- Terminal: `.collect()`, `.toList()`, `.forEach()`, `.count()`, `.findFirst()`, `.anyMatch()`, `.reduce()`
- Collectors: `Collectors.toList()`, `.toSet()`, `.toMap()`, `.groupingBy()`, `.joining()`, `.partitioningBy()`
- Streams are lazy and single-use — can't reuse a stream
- Parallel streams — `.parallelStream()` — when they help, when they hurt (almost never worth it)
- **Contrast with RxJS:** no subscription, no async, no backpressure. Streams are synchronous pipelines that terminate immediately.

### 2.4 Capstone — Library Search & Reporting
- **Tests:** complex queries over the library catalog
  - Find all books by a given author, sorted by title
  - Group books by genre, count per genre
  - Find members with overdue loans
  - Generate a formatted report of all active loans
- All driven by tests, using streams and collections

---

## Phase 3 — Error Handling, I/O & Serialization

> **Narrative:** Build a file-based repository implementation for the library. This
> naturally requires I/O, Jackson serialization, and exception handling for edge cases
> (corrupt files, missing data). All three sub-topics serve one domain goal.

### 3.1 Exception Handling
- **Tests:** `assertThrows` + `assertThatThrownBy` (AssertJ) to verify exception behavior
- Checked vs unchecked exceptions — the biggest Java-specific concept
  - Checked (`Exception`): compiler forces you to catch or declare `throws`. Used for recoverable conditions (I/O failures, parse errors).
  - Unchecked (`RuntimeException`): no compiler enforcement. Used for programming errors and business rule violations.
- `throws` declarations — part of the method signature contract
- When to use which: most modern Java (and this course) prefers unchecked for domain exceptions. You already throw `IllegalStateException` — that's the right instinct.
- `Result<T>` vs exceptions — when does each approach make sense? You already have `Result<T>` for expected domain outcomes. Exceptions are for unexpected failures (I/O errors, corrupt data).
- **Checked exceptions inside lambdas** — the real TS→Java gotcha. `stream.map(item -> methodThatThrowsChecked(item))` doesn't compile. How to handle: wrap in unchecked, extract method, or use a helper.
- Try-with-resources: `try (var reader = Files.newBufferedReader(path)) { ... }` — auto-closes `AutoCloseable` resources (≈ C# `using`). Belongs here because it shows up immediately in 3.2.
- Exception chaining — `throw new ServiceException("msg", cause)` — preserving the original stack trace
- **Watch out:** checked exceptions will feel annoying coming from TS/C#. They exist for a reason, but you'll mostly write unchecked ones. The pain point is when they show up inside streams.

### 3.2 I/O with `java.nio`
- **Tests:** use JUnit 5's `@TempDir` for test isolation — write/read files, assert contents
- `Path` and `Files` — the modern API (ignore legacy `java.io.File`)
- Reading: `Files.readString()`, `Files.readAllLines()`, `Files.newBufferedReader()`
- Writing: `Files.writeString()`, `Files.write()`, `Files.newBufferedWriter()`
- Walking directories: `Files.walk()`, `Files.list()` — return Streams (tie-in to Phase 2)
- Try-with-resources for anything that opens a handle
- `InputStream`/`OutputStream` — byte-level; `Reader`/`Writer` — character-level (brief, for when you encounter them)
- **Domain exercise:** read/write library data to files using `@TempDir` — this feeds directly into the capstone

### 3.3 Serialization with Jackson
- **Tests:** serialize domain objects to JSON and back; assert round-trip fidelity
- Add Jackson dependency to `pom.xml`
- `ObjectMapper` — the central API
- Serializing: `mapper.writeValueAsString(book)` → JSON string
- Deserializing: `mapper.readValue(json, Book.class)` → object
- **`Book` is a class with a private constructor and static factories** — Jackson needs `@JsonCreator` to work with this. Records work out of the box, classes with restricted constructors don't.
- `@JsonProperty`, `@JsonIgnore`, `@JsonCreator` — when defaults aren't enough
- **Sealed interfaces + Jackson:** polymorphic serialization of `LibraryEvent` hierarchy using `@JsonTypeInfo` and `@JsonSubTypes`. You already have `EventSerializer` — extend it to use Jackson instead of manual serialization.
- **Watch out:** Jackson errors at runtime, not compile time. A missing `@JsonCreator` or wrong `@JsonTypeInfo` will blow up on the first deserialization attempt.

### 3.4 Capstone — File-Based Persistence
- **Tests:** implement `FileLoanRepository implements LoanRepository` — saves/loads loan data as JSON files
- Uses the same `LoanRepository` interface you already have
- Tests use `@TempDir` for isolation
- Handle edge cases: corrupt JSON file, missing file, empty file
- The Detroit-school payoff: extract your existing `InMemoryLoanRepository` tests into an **interface contract test** that runs against both implementations — same tests, two implementations, verified identical behavior

---

## Phase 4 — Concurrency (the biggest gap from TS)

### 4.1 Why Concurrency Matters (conceptual shift)
- JS/TS: single-threaded event loop, everything is non-blocking by convention
- Java: real OS threads, true parallelism, shared mutable state
- The fundamental problem: two threads writing to the same variable
- **Immutability as a concurrency strategy** — your existing design preferences (records, final fields, unmodifiable collections) already make objects thread-safe. This section connects that intuition to the concurrency model.

### 4.2 Threading Fundamentals
- **Tests:** use `CountDownLatch`, `CyclicBarrier` to coordinate threads in tests
- `Thread` and `Runnable` — brief conceptual overview (you won't create raw threads in practice)
- `synchronized` blocks and methods — mutual exclusion
- `volatile` — visibility guarantee across threads
- `Atomic*` types: `AtomicInteger`, `AtomicReference` — lock-free thread safety
- Thread-safe collections: `ConcurrentHashMap`, `CopyOnWriteArrayList`
- **Domain exercise:** `InMemoryLoanRepository` uses `HashMap` — not thread-safe. `LendingService.borrowBook()` has a check-then-act race condition (check `isBookOnLoan`, then `save`). Discover and fix these through tests that expose the race.
- **Watch out:** race conditions are silent and intermittent. Tests might pass 99% of the time.

### 4.3 `java.util.concurrent`
- **Tests:** `CompletableFuture` chains, assert composed results
- `ExecutorService` and thread pools — the standard way to run work concurrently
- `Executors.newFixedThreadPool()`, `.newCachedThreadPool()`
- `Future<T>` — submit work, get result later (blocking `.get()`)
- `CompletableFuture<T>` (≈ `Promise<T>`)
  - `.supplyAsync()`, `.thenApply()` (≈ `.then()`), `.thenCompose()` (≈ `.then()` returning Promise)
  - `.exceptionally()` (≈ `.catch()`), `.handle()`, `.thenCombine()`
  - `CompletableFuture.allOf()` (≈ `Promise.all()`)
- **Domain exercise:** parallel catalog searches or concurrent loan report generation
- **Contrast with Promises:** Promises auto-schedule on microtask queue. CompletableFuture runs on an executor you choose.

### 4.4 Virtual Threads (Java 21+)
- **Tests:** spawn thousands of virtual threads, aggregate results, assert correctness
- `Thread.ofVirtual().start(runnable)` — lightweight, JVM-managed threads
- `Executors.newVirtualThreadPerTaskExecutor()` — drop-in replacement for thread pools
- Structured concurrency (`StructuredTaskScope`) — scope-bound thread management. Worth knowing about; API may still evolve.
- Why this changes everything: one-thread-per-request is viable again, no need for reactive frameworks
- **Watch out:** virtual threads are cheap to create but still share mutable state. Thread safety still applies.

---

## Phase 5 — E-Commerce Project Bootstrap

> **Domain shift.** The Library domain is complete. From here, exercises target the
> e-commerce platform in a separate repository. This phase is deliberately slim — just the
> project skeleton and build tooling. Everything else (logging, database migrations,
> observability, architecture tests, `java.time`) gets introduced just-in-time in Phase 6
> when you have a real reason to need it.

### 5.1 Multi-Module Gradle
- **Exercise:** scaffold the e-commerce multi-module project from scratch
- Root `build.gradle.kts` + `settings.gradle.kts`, Kotlin DSL
- Version catalog (`gradle/libs.versions.toml`) — central dependency version management
- Module structure: `common`, `catalog`, `order`, `inventory`, `cart`, `payment`, `search`, `notification`
- Inter-module dependencies: `implementation(project(":common"))`, etc.
- Cross-module test: `catalog` imports a type from `common`, test passes
- **Watch out:** multi-module dependency cycles are a build error. Design module boundaries carefully up front.

### 5.2 Architecture Decision Records
- **Exercise:** write the initial ADR set for the e-commerce project in `docs/adr/`
- ADR format: Context / Decision / Consequences (keep it simple)
- Initial ADRs: Java 25, Spring Boot 4, Gradle, OpenTelemetry observability strategy
- Future ADRs written as decisions are made (architecture, CQRS/ES boundaries, event store, etc.)

### 5.3 Capstone
- `./gradlew clean build` passes, cross-module dependencies work, ADRs committed
- The e-commerce project is ready for feature development in Phase 6

---

## Phase 6 — Spring Boot & the Catalog Context

> **The Catalog bounded context.** Standard CRUD with Spring Data JPA — deliberately not
> event-sourced. Ecosystem tooling (logging, Flyway, Testcontainers, ArchUnit, `java.time`,
> OpenTelemetry) is introduced just-in-time as the Catalog context needs it, not as
> standalone exercises. Same detailed phase file style.

### 6.1 Core Concepts & Dependency Injection
- **Tests:** wire up beans with `@SpringBootTest`, assert DI works. Register fakes (e.g., `InMemoryProductRepository`) as beans for testing.
- Spring's DI container (≈ Angular's injector)
- `@Component`, `@Service`, `@Repository`, `@Controller` — stereotypes (≈ `@Injectable()`)
- Constructor injection — preferred (no `@Autowired` needed when single constructor)
- `@Configuration` + `@Bean` — manual wiring for complex setup
- `@Profile` — swap implementations per environment (e.g., in-memory for tests, Postgres for prod)
- **Detroit-school approach:** register your `InMemoryProductRepository` as a `@Bean` in test configuration. Real objects, real behavior.

### 6.2 Domain Model & Value Objects
- `java.time` introduced here — `Instant` for timestamps, `LocalDate` for date ranges, `Clock` for testable time
- `Money` value object (`BigDecimal` + `Currency`) — learned when the domain requires prices
- `DateRange` for promotions — `contains()`, `overlaps()`, `duration()`
- Product domain model: `Product`, `Category`, `ProductId`
- **Watch out:** `BigDecimal` comparison — `equals()` considers scale, use `compareTo() == 0`

### 6.3 Spring Web — REST APIs
- **Tests:** `MockMvc` to test controllers without starting a server. Fakes for the service layer.
- `@RestController` + `@RequestMapping`
- `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`
- `@PathVariable`, `@RequestParam`, `@RequestBody`
- Response types: `ResponseEntity<T>` for control over status codes
- Exception handling with `@ControllerAdvice` + `@ExceptionHandler`
- Input validation: `@Valid` + Jakarta Bean Validation (`@NotNull`, `@Size`, `@Email`, etc.)
- **E-commerce exercise:** Product CRUD endpoints — create product, get by ID, list by category, update, soft-delete

### 6.4 Spring Data JPA & Flyway
- **Tests:** `@DataJpaTest` with Testcontainers PostgreSQL — real DB, Flyway migrations applied
- **Flyway introduced here:** first migration when you need a database table — `V1__create_product_table.sql`
- **Testcontainers introduced here:** real PostgreSQL in tests, not H2
- JPA entities: `@Entity`, `@Id`, `@GeneratedValue`, `@Column`
- Catalog domain model: `Product`, `Category`, `ProductImage`
- Relationships: `@ManyToOne` (product → category), `@OneToMany` (product → images)
- Spring Data repositories: `JpaRepository<Product, UUID>` — interface only, Spring generates the implementation
- Query methods by naming convention: `findByCategoryIdAndActiveTrue(UUID categoryId)`
- Custom queries: `@Query("SELECT p FROM Product p WHERE ...")`
- **Watch out:** JPA entities need a no-arg constructor (can be `protected`). They are not the same as your domain model — keep them in an infrastructure/persistence package and map to domain objects.

### 6.5 ArchUnit — Enforcing Architecture
- **Introduced here:** now that you have real layers (api, domain, persistence), ArchUnit
  rules have something to enforce
- Domain must not depend on Spring, JPA, or Jackson annotations
- No `java.util.logging` — enforce SLF4J usage
- No field injection (`@Autowired` on fields)
- Controllers in `..api..` packages, domain classes framework-free
- **Watch out:** start with a few critical rules. Add more as patterns emerge.

### 6.6 Observability
- Logback config for ecosystem output (Spring Boot bundles SLF4J + Logback — no manual deps)
- OpenTelemetry API + SDK — manual instrumentation at service boundaries
- Wide events: attach business attributes (product ID, category, cache hit/miss) to spans
- Your own code instruments with OTel spans, not `log.info()` — per ADR 0004
- Console exporter for development, OTLP exporter for production

### 6.7 Layered Architecture & Integration Testing
- **Tests:** full integration tests with `@SpringBootTest` + `TestRestTemplate` or `WebTestClient`
- Controller → Service → Repository — standard layering within the catalog module
- How Detroit-school testing applies at each layer:
  - **Unit tests:** service with `InMemoryProductRepository` (fake)
  - **Integration tests:** real Spring context, Testcontainers PostgreSQL, actual HTTP calls
  - **Contract tests:** same interface tests run against fake and real repository

### 6.8 Capstone — Catalog API
- Full CRUD API for products and categories
- Product listing with filtering (by category, active status, price range)
- Test pyramid: unit tests (fakes) → integration tests (Testcontainers) → API tests (MockMvc)
- All driven by TDD from the start
- ArchUnit rules pass with the new code
- OTel instrumentation with wide events, Logback configured for ecosystem output
- The Catalog context is complete and production-shaped — ready to integrate with Order and Inventory contexts later

---

## After Phase 6 — Guided Build

> **The course format ends here.** You have enough Java fluency to build independently.
> No more phase files, exercises, or structured curriculum.

From here, you build the remaining e-commerce bounded contexts on your own, using Claude Code
as a resource when you choose. The order below is a suggested progression, not a prescription:

1. **Order Management** — CQRS/ES via Axon 5. Aggregates, command handlers, event handlers, stateful event handlers (Axon 5's replacement for sagas). The richest domain and the primary learning ground for event sourcing.
2. **Inventory** — CQRS/ES via Axon 5. Stock movements as events. Concurrency challenges (two orders reserving the last item).
3. **Payment** — Integration layer with Stripe test mode. Anti-corruption layer pattern. Not event-sourced.
4. **Cart** — Redis-backed, short-lived state.
5. **Search** — Elasticsearch projections from catalog/inventory events.
6. **Notification** — Pure event consumer. Listens to domain events, sends emails/webhooks.
7. **Gateway** — API layer, authentication (Spring Security + JWT), routing.

### Key Technical Challenges to Expect

These will come up naturally as you build. Research them when you hit them, not before:

- **Jackson polymorphic deserialization** (`@JsonTypeInfo`, `@JsonSubTypes`) for sealed event hierarchies — needed when Axon serializes/deserializes events
- **Generics and type erasure** in reusable command/event handler infrastructure
- **Spring transaction propagation** + `@TransactionalEventListener` interactions with Axon
- **Hibernate BYTEA dialect fix** for PostgreSQL — Axon's JPA event store uses `@Lob`, which maps incorrectly without a custom dialect
- **Axon 5 API differences** — most online content targets Axon 4. Use official Axon 5 documentation as the source of truth
- **Order fulfillment saga** — Order placed → payment authorized → inventory reserved → payment captured → shipped → inventory decremented. Every step can fail. Compensation at each step.

---

## Reference Appendix — Advanced Java Topics

> Not a structured phase. Consult these topics as they become relevant during the build.
> Each topic includes enough context to know *when* you need it.

### Generics — The Deep End
- Wildcards: `<? extends Foo>` (producer), `<? super Foo>` (consumer)
- PECS: Producer Extends, Consumer Super
- Recursive bounds: `<T extends Comparable<T>>`
- Type tokens and `Class<T>` — working around type erasure
- **When you'll need this:** writing reusable Axon command/event handler infrastructure, generic repository base classes

### Reflection & Custom Annotations
- `@Retention(RUNTIME)`, `@Target(METHOD)` — annotation meta-annotations
- `Class.getDeclaredMethods()`, `Method.getAnnotation()` — reading at runtime
- Why frameworks (Spring, Hibernate, Jackson, Axon) are built on reflection
- **When you'll need this:** debugging framework behavior, understanding how Axon discovers handlers, custom validation

### Design Patterns — Java Idioms
- **Builder** — very common in Java (no object spread like TS). Used heavily in Axon command/event construction.
- **Factory Method / Abstract Factory** — static factory methods (`List.of()`, `Optional.of()`)
- **Strategy** — interface + implementations (natural fit for DI and fakes)
- **Decorator** — wrapping interfaces (e.g., logging decorator around a repository)
- **Observer** — the pattern behind Spring events and Axon's event handling

### Performance & JVM Internals
- JVM architecture: classloading → bytecode verification → JIT compilation
- Memory model: heap (objects), stack (method frames), metaspace (class metadata)
- Garbage collection: G1 (default), ZGC (low-latency), Shenandoah
- Profiling: JFR (Java Flight Recorder) + JMC (Mission Control) — built into the JDK
- **When you'll need this:** diagnosing slow event projections, optimizing query-side read models, production performance tuning
- **Watch out:** premature optimization is the root of all evil. Profile first, optimize second.

---

## Approach for Each Section

Each section follows the Detroit-school TDD cycle:

1. **Failing test** — write a test that describes the desired behavior (RED)
2. **Brief explanation** — how this concept differs from TS/C#, taught in context
3. **Make it pass** — implement the simplest code that works (GREEN)
4. **Refactor** — improve design while tests stay green (REFACTOR)
5. **"Watch out"** — common traps for TS/JS developers

**Testing philosophy:**
- No mock frameworks. Ever. (Mockito only for genuinely external concerns: HTTP clients, third-party APIs.)
- **Fakes:** in-memory implementations of interfaces (e.g., `InMemoryProductRepository`). These are real objects with real behavior, kept in `src/test/java`.
- **Stubs:** simple objects returning canned values. Use when behavior doesn't matter for the test.
- **Self-shunt:** the test class itself implements an interface when convenient.
- **Contract tests:** same test suite runs against fake and real implementations to ensure they behave identically.

---

## Suggested Order of Attack

| Phase | Sessions | Focus |
|-------|----------|-------|
| **0** | 1 | JUnit 5 + AssertJ + Maven + TDD workflow |
| **1** | 2-3 | Language mechanics — you'll be writing confident Java |
| **2** | 1-2 | Collections, lambdas, streams — the functional toolkit |
| **3** | 1 | Exceptions, I/O, Jackson — file-based persistence |
| **4** | 1-2 | Concurrency — the big conceptual leap from TS |
| **5** | 1 | E-commerce project bootstrap — Gradle multi-module, ADRs |
| **6** | 3-4 | Spring Boot — Catalog bounded context + ecosystem tooling (logging, Flyway, ArchUnit, OTel) |

**Total: ~10-15 sessions to complete the structured course.**
After that, you're building independently.
