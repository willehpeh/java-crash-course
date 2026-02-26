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
>
> **Unifying project: a Library Management System.**
> Starting from Phase 1.2, concepts are applied to a running domain model (books, members,
> loans, search, persistence) that grows with each phase. Standalone "concept exploration" tests
> exist alongside the project tests.

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

> Standalone "concept exploration" tests. The library project starts in 1.2.

### 1.1 Type System & Primitives
- **Tests:** demonstrate and assert `int` vs `Integer` autoboxing, `==` vs `.equals()` traps, `null` behavior
- Primitives vs boxed types — `int`, `long`, `double`, `boolean` vs `Integer`, `Long`, `Double`, `Boolean`
- Autoboxing/unboxing pitfalls: `Integer a = 127; Integer b = 127; a == b` → true. `Integer a = 128; Integer b = 128; a == b` → false. Why?
- `String` is immutable and interned; `==` vs `.equals()` (the #1 Java trap)
- Type inference with `var` — local variables only, no `let`/`const` at class level
- No union types, no `undefined` — `null` is the only absence value
- **Watch out:** coming from TS, you'll instinctively use `==`. In Java, always `.equals()` for objects.

### 1.2 Strings & Text
- **Tests:** string manipulation, formatting, comparison
- `String` methods: `.substring()`, `.contains()`, `.split()`, `.strip()`, `.startsWith()`
- Text blocks (`"""`) — multi-line strings (≈ JS template literals, but no interpolation)
- `String.format()` and `.formatted()` — `"Hello %s, you are %d".formatted(name, age)`
- `StringBuilder` — when and why (loop concatenation)
- `String` comparison: `.equals()`, `.equalsIgnoreCase()`, `.compareTo()`
- **Watch out:** no string interpolation like TS `${var}`. Use `.formatted()` or `+` concatenation.

### 1.3 Classes & OOP (what's different from TS/C#)
- **Tests:** build the library domain model — `Book`, `Member`, `Loan` — assert behavior
- Everything lives in a class — no top-level functions (until recent `void main()`)
- One public class per file (filename must match class name)
- Access modifiers: `public`, `private`, `protected`, **package-private** (default — no keyword, and it's a deliberate design tool)
- `final` keyword — on fields (≈ `readonly`), on methods (can't override), on classes (can't extend)
- No structural typing — Java is 100% nominal. Two identical classes are NOT interchangeable.
- Constructors, `this`, method overloading (no default parameter values like TS)
- `static` — class-level methods and fields, static factory methods
- `@Override` annotation — why you should always use it
- **Watch out:** no object literals `{ title: "..." }`. You must define a class or record for everything.

### 1.4 Interfaces & Abstract Classes
- **Tests:** define `BookRepository` interface, implement `InMemoryBookRepository`, test through the interface
- Interfaces — pure contracts (≈ TS interfaces, but enforced at runtime)
- `default` methods on interfaces — implementation in interfaces (≈ mixin-ish)
- Abstract classes — partial implementation + contract
- When to use which: prefer interfaces; use abstract classes when you need shared state/constructor logic
- Multiple interface implementation (Java's answer to no multiple inheritance)
- **This is where Detroit-school TDD shines:** you define the interface in the test, implement a fake, then build the real implementation later.

### 1.5 Enums (they're full classes)
- **Tests:** model `LoanStatus` (ACTIVE, OVERDUE, RETURNED), `BookGenre` with behavior
- Enums can have fields, constructors, and methods
- Enum with abstract methods — each constant provides its own implementation
- `EnumSet`, `EnumMap` — specialized collections for enums
- **Watch out:** Java enums are nothing like TS enums. They're closer to a sealed class with singleton instances.

### 1.6 Records & Sealed Types (modern Java)
- **Tests:** convert `Book` to a record, assert auto-generated equality/accessors; model domain events as sealed hierarchy
- `record` — immutable data carriers with auto-generated `equals()`, `hashCode()`, `toString()`, accessors
- Compact constructors for validation
- Sealed classes/interfaces — `sealed interface DomainEvent permits BookAdded, BookBorrowed, BookReturned`
- Pattern matching in `instanceof` and `switch`
- Destructuring records in patterns
- **Watch out:** records are final and immutable. No setters, no inheritance. Think "value objects."

### 1.7 Generics (the practical parts)
- **Tests:** write a generic `Result<T>` type (≈ TS `Result<T, E>`), test with different types
- Generic classes, interfaces, methods
- Type erasure — generics exist at compile-time only. No `new T()`, no `instanceof T`.
- Bounded types: `<T extends Comparable<T>>`
- Why you can't do `List<int>` — primitives aren't objects (until Valhalla)
- **Deferred to Phase 7:** Wildcards (`<? extends Foo>`, `<? super Foo>`) and PECS — you rarely need these day-to-day.

### 1.8 Null Safety & Optionals
- **Tests:** `Optional` behavior — map, flatMap, orElse, orElseThrow, empty; use in repository `findById`
- Java has no built-in null safety (no `?` operator like TS/C#)
- `Optional<T>` — when to use it:
  - YES: return types that might be absent (`findById` returns `Optional<Book>`)
  - NO: fields, parameters, collections
- `Optional` methods: `.map()`, `.flatMap()`, `.filter()`, `.orElse()`, `.orElseThrow()`
- `@Nullable` / `@NonNull` annotations — tooling hints, not compiler-enforced
- **Watch out:** don't use `Optional` like TS's `?:` on everything. It's for return types.

### 1.9 Capstone — Library Domain Model
- **Bring it together:** `Book` (record), `Member` (class), `Loan` (class), `BookRepository` (interface), `InMemoryBookRepository` (fake)
- All tested, all working, no framework — pure Java + JUnit + AssertJ
- Review: what's different from how you'd model this in TS?

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

### 3.1 Exception Handling
- **Tests:** `assertThrows` + `assertThatThrownBy` (AssertJ) to verify exception behavior
- Checked vs unchecked exceptions — the biggest Java-specific concept
  - Checked (`Exception`): compiler forces you to catch or declare `throws`. Used for recoverable conditions.
  - Unchecked (`RuntimeException`): no compiler enforcement. Used for programming errors and business rule violations.
- `throws` declarations — part of the method signature contract
- When to use which: most modern Java (and this course) prefers unchecked for domain exceptions
- Custom exception classes — `BookNotFoundException extends RuntimeException`
- Try-with-resources: `try (var reader = Files.newBufferedReader(path)) { ... }` — auto-closes resources (≈ C# `using`)
- Exception chaining — `throw new ServiceException("msg", cause)`
- **Watch out:** checked exceptions will feel annoying coming from TS/C#. They exist for a reason, but you'll mostly write unchecked ones.

### 3.2 I/O with `java.nio`
- **Tests:** use JUnit 5's `@TempDir` for test isolation — write/read files, assert contents
- `Path` and `Files` — the modern API (ignore legacy `java.io.File`)
- Reading: `Files.readString()`, `Files.readAllLines()`, `Files.newBufferedReader()`
- Writing: `Files.writeString()`, `Files.write()`, `Files.newBufferedWriter()`
- Walking directories: `Files.walk()`, `Files.list()` — return Streams
- Try-with-resources for anything that opens a handle
- `InputStream`/`OutputStream` — byte-level; `Reader`/`Writer` — character-level (brief, for when you encounter them)

### 3.3 Serialization with Jackson
- **Tests:** serialize `Book` records to JSON and back; assert round-trip fidelity
- Add Jackson dependency to `pom.xml`
- `ObjectMapper` — the central API
- Serializing: `mapper.writeValueAsString(book)` → JSON string
- Deserializing: `mapper.readValue(json, Book.class)` → object
- Records + Jackson = clean DTOs with zero boilerplate
- `@JsonProperty`, `@JsonIgnore`, `@JsonCreator` — when defaults aren't enough
- **Watch out:** Jackson needs a no-arg constructor OR `@JsonCreator` for classes. Records work out of the box.

### 3.4 Capstone — File-Based Persistence
- **Tests:** implement `FileBookRepository implements BookRepository` — saves/loads books as JSON files
- Uses the same `BookRepository` interface from Phase 1
- Tests use `@TempDir` for isolation
- The Detroit-school payoff: your existing tests for `InMemoryBookRepository` can be extracted into an **interface contract test** that runs against both implementations

---

## Phase 4 — Concurrency (the biggest gap from TS)

### 4.1 Why Concurrency Matters (conceptual shift)
- JS/TS: single-threaded event loop, everything is non-blocking by convention
- Java: real OS threads, true parallelism, shared mutable state
- The fundamental problem: two threads writing to the same variable

### 4.2 Threading Fundamentals
- **Tests:** use `CountDownLatch`, `CyclicBarrier` to coordinate threads in tests
- `Thread` and `Runnable` — creating threads
- `synchronized` blocks and methods — mutual exclusion
- `volatile` — visibility guarantee across threads
- `Atomic*` types: `AtomicInteger`, `AtomicReference` — lock-free thread safety
- Thread-safe collections: `ConcurrentHashMap`, `CopyOnWriteArrayList`
- **Watch out:** race conditions are silent and intermittent. Tests might pass 99% of the time.

### 4.3 `java.util.concurrent`
- **Tests:** `CompletableFuture` chains, assert composed results
- `ExecutorService` and thread pools — never create raw threads in production
- `Executors.newFixedThreadPool()`, `.newCachedThreadPool()`
- `Future<T>` — submit work, get result later (blocking `.get()`)
- `CompletableFuture<T>` (≈ `Promise<T>`)
  - `.supplyAsync()`, `.thenApply()` (≈ `.then()`), `.thenCompose()` (≈ `.then()` returning Promise)
  - `.exceptionally()` (≈ `.catch()`), `.handle()`, `.thenCombine()`
  - `CompletableFuture.allOf()` (≈ `Promise.all()`)
- **Contrast with Promises:** Promises auto-schedule on microtask queue. CompletableFuture runs on an executor you choose.

### 4.4 Virtual Threads (Project Loom — Java 21+)
- **Tests:** spawn thousands of virtual threads, aggregate results, assert correctness
- `Thread.ofVirtual().start(runnable)` — lightweight, JVM-managed threads
- `Executors.newVirtualThreadPerTaskExecutor()` — drop-in replacement for thread pools
- Structured concurrency (preview): `StructuredTaskScope` — scope-bound thread management
- Why this changes everything: one-thread-per-request is viable again, no need for reactive frameworks
- **Watch out:** virtual threads are cheap to create but still share mutable state. Thread safety still applies.

---

## Phase 5 — Build System, Ecosystem & Practical Java

> Reference and orientation — lighter on TDD, heavier on "things you need to know."

### 5.1 Maven Deep Dive
- `pom.xml` structure: parent POM, properties, dependency management
- Plugins: `compiler`, `surefire`, `failsafe` (integration tests), `shade` (fat JAR)
- Profiles — environment-specific builds
- Multi-module projects — when and why
- Maven vs Gradle — brief comparison (Gradle ≈ build scripts with a Groovy/Kotlin DSL)

### 5.2 Java Ecosystem Orientation
- **Logging:** SLF4J + Logback — the universal standard. Equivalent of `console.log` but structured and configurable.
- **Lombok:** `@Data`, `@Builder`, `@Slf4j`, `@RequiredArgsConstructor` — code generation via annotations. Controversial but ubiquitous. You'll see it everywhere, decide later if you like it.
- **Jackson:** already covered in Phase 3 — the JSON library.
- **MapStruct:** compile-time object mapping (DTO ↔ entity). Mention only.
- **Flyway/Liquibase:** database migration tools (≈ EF migrations). Mention only.
- **Testcontainers:** spin up Docker containers in tests (databases, queues). Covered in Phase 6.

### 5.3 Date & Time API (`java.time`)
- **Tests:** create, manipulate, format, compare dates and times
- `LocalDate`, `LocalTime`, `LocalDateTime` — no timezone
- `ZonedDateTime`, `Instant`, `Duration`, `Period`
- `DateTimeFormatter` — parsing and formatting
- Immutable and thread-safe (unlike old `java.util.Date`)
- **Watch out:** nothing like JS `Date`. Much better designed. `LocalDate.now()` gives you today, not a timestamp.

### 5.4 Common Annotations You'll Encounter
- `@Override` — already covered, always use it
- `@Deprecated` — marks APIs for removal
- `@SuppressWarnings` — silence specific warnings
- `@FunctionalInterface` — already covered
- `@SafeVarargs` — suppresses heap pollution warnings on generic varargs
- Jakarta annotations: `@Inject`, `@Named`, `@PostConstruct` — framework-agnostic DI (Spring supports these too)

---

## Phase 6 — Spring Boot (the Angular of Java)

> The library project becomes a web application.

### 6.1 Core Concepts & Dependency Injection
- **Tests:** wire up beans with `@SpringBootTest`, assert DI works. Use fakes: register your `InMemoryBookRepository` as a bean for testing.
- Spring's DI container (≈ Angular's injector)
- `@Component`, `@Service`, `@Repository`, `@Controller` — stereotypes (≈ `@Injectable()`)
- Constructor injection — preferred (no `@Autowired` needed when single constructor)
- `@Configuration` + `@Bean` — manual wiring for complex setup
- `@Profile` — swap implementations per environment (e.g., in-memory for tests, Postgres for prod)
- **Detroit-school approach:** instead of mocking the repository, register your `InMemoryBookRepository` as a `@Bean` in test configuration. Real objects, real behavior.

### 6.2 Spring Web — REST APIs
- **Tests:** `MockMvc` to test controllers without starting a server. Fakes for the service layer.
- `@RestController` + `@RequestMapping`
- `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`
- `@PathVariable`, `@RequestParam`, `@RequestBody`
- Response types: `ResponseEntity<T>` for control over status codes
- Exception handling with `@ControllerAdvice` + `@ExceptionHandler`
- Input validation: `@Valid` + Jakarta Bean Validation (`@NotNull`, `@Size`, `@Email`, etc.)
- **Contrast with Angular:** you're building the API that your Angular app would call.

### 6.3 Spring Data JPA
- **Tests:** `@DataJpaTest` with in-memory H2 database — real DB, no fakes needed (the DB *is* the fake)
- JPA entities: `@Entity`, `@Id`, `@GeneratedValue`, `@Column`
- Relationships: `@OneToMany`, `@ManyToOne`, `@ManyToMany`
- Spring Data repositories: `JpaRepository<Book, Long>` — interface only, Spring generates the implementation
- Query methods by naming convention: `findByTitleContainingIgnoreCase(String title)`
- Custom queries: `@Query("SELECT b FROM Book b WHERE ...")`
- **Contrast with TypeORM / Entity Framework:** similar concepts, annotation-driven instead of decorator-driven.

### 6.4 Layered Architecture & Integration Testing
- **Tests:** full integration tests with `@SpringBootTest` + `TestRestTemplate` or `WebTestClient`
- Controller → Service → Repository — standard layering
- How Detroit-school testing applies at each layer:
  - **Unit tests:** service with `InMemoryBookRepository` (fake)
  - **Integration tests:** real Spring context, H2 database, actual HTTP calls
  - **Contract tests:** same interface tests run against fake and real repository
- Testcontainers — when you need a real Postgres instead of H2

### 6.5 Spring Security (essentials only)
- **Tests:** `@WithMockUser`, security filter chain tests
- Authentication vs Authorization
- Security filter chain configuration
- Method-level security: `@PreAuthorize("hasRole('ADMIN')")`
- JWT basics — reference only, not a full implementation

### 6.6 Capstone — Library REST API
- Full CRUD API for books and members
- Loan management (borrow, return, overdue detection)
- Test pyramid: unit tests (fakes) → integration tests (H2) → API tests (MockMvc)
- All driven by TDD from the start

---

## Phase 7 — Advanced Topics

### 7.1 Generics — The Deep End
- Wildcards: `<? extends Foo>` (producer), `<? super Foo>` (consumer)
- PECS: Producer Extends, Consumer Super
- Recursive bounds: `<T extends Comparable<T>>`
- Type tokens and `Class<T>` — working around type erasure
- When you actually need this: writing libraries, generic data structures

### 7.2 Reflection & Custom Annotations
- **Tests:** write custom annotations, assert they're discoverable and functional
- `@Retention(RUNTIME)`, `@Target(METHOD)` — annotation meta-annotations
- `Class.getDeclaredMethods()`, `Method.getAnnotation()` — reading at runtime
- Build a simple annotation-driven validator (educational, not production)
- Why frameworks (Spring, Hibernate, Jackson) are built on reflection

### 7.3 Design Patterns — Java Idioms
- **Tests:** each pattern TDD'd
- **Builder** — very common in Java (no object spread like TS)
- **Factory Method / Abstract Factory** — static factory methods (`List.of()`, `Optional.of()`)
- **Strategy** — interface + implementations (natural fit for DI and fakes)
- **Decorator** — wrapping interfaces (e.g., logging decorator around `BookRepository`)
- **Observer** — `java.util.Observer` is deprecated, but the pattern is everywhere in Spring events

### 7.4 Performance & JVM Internals
- JVM architecture: classloading → bytecode verification → JIT compilation
- Memory model: heap (objects), stack (method frames), metaspace (class metadata)
- Garbage collection: G1 (default), ZGC (low-latency), Shenandoah
- When to care about GC: high-throughput or low-latency requirements
- Profiling: JFR (Java Flight Recorder) + JMC (Mission Control) — built into the JDK
- **Watch out:** premature optimization is the root of all evil. Profile first, optimize second.

### 7.5 Reactive Programming (optional)
- Project Reactor: `Mono<T>`, `Flux<T>` (≈ RxJS `Observable`)
- Spring WebFlux — reactive web stack
- Testing with `StepVerifier`
- **Honest assessment:** with virtual threads (Phase 4.4), reactive is increasingly niche. Learn it if your team uses it, skip it otherwise.

---

## Approach for Each Section

Each section follows the Detroit-school TDD cycle:

1. **Failing test** — write a test that describes the desired behavior (RED)
2. **Brief explanation** — how this concept differs from TS/C#, taught in context
3. **Make it pass** — implement the simplest code that works (GREEN)
4. **Refactor** — improve design while tests stay green (REFACTOR)
5. **"Watch out"** — common traps for TS/JS developers

**Testing philosophy:**
- No mock frameworks. Ever.
- **Fakes:** in-memory implementations of interfaces (e.g., `InMemoryBookRepository`). These are real objects with real behavior, kept in `src/test/java`.
- **Stubs:** simple objects returning canned values. Use when behavior doesn't matter for the test.
- **Self-shunt:** the test class itself implements an interface when convenient.
- **Contract tests:** same test suite runs against fake and real implementations to ensure they behave identically.

**Code lives in:**
- `src/main/java/org/example/` — production code, organized by package per domain concept
- `src/test/java/org/example/` — tests and fakes

---

## Suggested Order of Attack

| Phase | Sessions | Focus |
|-------|----------|-------|
| **0** | 1 | JUnit 5 + AssertJ + Maven + TDD workflow |
| **1** | 2-3 | Language mechanics — you'll be writing confident Java |
| **2** | 1-2 | Collections, lambdas, streams — the functional toolkit |
| **3** | 1 | Exceptions, I/O, Jackson — file-based persistence |
| **4** | 1-2 | Concurrency — the big conceptual leap from TS |
| **5** | 0.5 | Ecosystem orientation, date/time, logging |
| **6** | 3-4 | Spring Boot — the library becomes a real web app |
| **7** | as needed | Advanced topics based on what you're building |

**Total: ~10-14 sessions to be fully operational.**
