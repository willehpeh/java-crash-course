# Phase 6 — Spring Boot & the Catalog Context

> **Goal:** Build the Catalog bounded context as a full CRUD API with Spring Boot, Spring
> Data JPA, and PostgreSQL. Ecosystem tooling — logging, Flyway, Testcontainers, ArchUnit,
> OpenTelemetry, `java.time` — is introduced just-in-time as the domain requires it.
>
> **Everything lives in the e-commerce repo.** The phase file stays here (it's course
> material); all code goes in the `ecommerce` project you scaffolded in Phase 5.
>
> **Starting point:** You have a multi-module Gradle project with `common` and `catalog`
> modules, cross-module dependencies working, and ADRs committed. No Spring Boot yet —
> this phase adds it.

---

## 6.1 — Spring Boot Setup & Dependency Injection

### Read First

**The TS comparison:**

| Concept | Angular | Spring Boot |
|---------|---------|-------------|
| DI container | Angular's injector | Spring's `ApplicationContext` |
| Injectable class | `@Injectable()` | `@Component` / `@Service` / `@Repository` |
| Module config | `@NgModule({ providers: [...] })` | `@Configuration` + `@Bean` methods |
| Constructor injection | TypeScript constructor params | Java constructor params (auto-detected) |
| Environment-specific | `environment.ts` / `environment.prod.ts` | `@Profile("test")` / `@Profile("prod")` |
| Auto-wiring | Angular resolves by type | Spring resolves by type (and qualifier) |

**How Spring DI works:**

Spring scans your classpath for classes annotated with stereotype annotations (`@Component`,
`@Service`, `@Repository`, `@Controller`) and registers them as **beans** — managed
instances in the application context. When a bean has a constructor, Spring injects the
required dependencies automatically.

```java
@Service
public class ProductService {
    private final ProductRepository repository;

    // Single constructor — Spring auto-injects. No @Autowired needed.
    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }
}
```

This is constructor injection — the only style you should use. Never use `@Autowired` on
fields (it hides dependencies, prevents immutability, and makes testing harder).

**`@Configuration` + `@Bean` — manual wiring:**

When you need to create a bean from a class you don't own (a library class) or need custom
setup logic:

```java
@Configuration
public class CatalogConfig {
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
```

**`@Profile` — environment-specific beans:**

```java
@Configuration
@Profile("test")
public class TestConfig {
    @Bean
    public ProductRepository productRepository() {
        return new InMemoryProductRepository();
    }
}
```

This lets you swap implementations per environment without changing code. In tests, Spring
loads `InMemoryProductRepository`. In production, it loads the JPA-backed implementation.

**Testing with Spring:**

```java
@SpringBootTest
class ProductServiceTest {
    @Autowired  // OK in tests — injection happens into test class
    private ProductService productService;

    @Test
    void createsProduct() {
        // ...
    }
}
```

`@SpringBootTest` starts the full application context. For faster, focused tests, use
sliced annotations like `@WebMvcTest` (controller layer only) or `@DataJpaTest`
(repository layer only).

**The Detroit-school approach with Spring:**

You already know how to test services with fakes. Spring doesn't change that — it just
manages the wiring. For unit tests, you can still construct services directly:

```java
class ProductServiceTest {
    private final InMemoryProductRepository repository = new InMemoryProductRepository();
    private final ProductService service = new ProductService(repository);
}
```

No Spring context needed for unit tests. Save `@SpringBootTest` for integration tests
where you need the full wiring.

**Watch out:** Spring Boot auto-configuration is powerful but magical. If something isn't
working, check: is the class in a package that Spring is scanning? (It scans the package
of your `@SpringBootApplication` class and below.) Is the dependency on the classpath?
Spring configures beans based on what's available.

### Exercise 6.1a — Add Spring Boot to the project

1. **Add the Spring Boot plugin and dependencies to the version catalog:**
   - `org.springframework.boot` plugin (apply to the `catalog` module)
   - `org.springframework.boot:spring-boot-starter` — the base starter
   - `org.springframework.boot:spring-boot-starter-test` — test support
   - Look up the latest Spring Boot 4.x version
   - Spring Boot manages its own dependency versions — you may want to use its BOM via
     `platform()`

2. **Create the application entry point** in `catalog`:
   - `CatalogApplication.java` with `@SpringBootApplication` and a `main` method
   - Package: your base package (e.g., `tech.reactiv.ecommerce.catalog`)

3. **Verify the app starts:** `./gradlew :catalog:bootRun` — you should see the Spring
   banner and a startup log message. It'll fail to serve requests (no web layer yet), but
   the context should load.

**Hints:**
- The Spring Boot Gradle plugin provides `bootRun` and `bootJar` tasks
- `@SpringBootApplication` is a shortcut for `@Configuration` + `@EnableAutoConfiguration`
  + `@ComponentScan`
- If the app fails to start, read the error carefully — Spring Boot error messages are
  usually descriptive

### Exercise 6.1b — Dependency injection with a service and fake

1. **Create a `ProductRepository` interface** in the catalog module's domain package:
   - `save(Product)`, `findById(ProductId)`, `findAll()`, `findByCategory(CategoryId)`
   - Same pattern as your library's `LoanRepository`

2. **Create `InMemoryProductRepository`** in the test source tree:
   - Implements `ProductRepository` with a `Map<ProductId, Product>` backing store
   - Same fake pattern you used in the library project

3. **Create `ProductService`** annotated with `@Service`:
   - Takes `ProductRepository` via constructor injection
   - Methods: `createProduct(...)`, `findProduct(ProductId)`, `listProducts()`

4. **Write a unit test** (no Spring context) that constructs `ProductService` with the
   fake repository and tests basic behavior.

5. **Write a Spring integration test** with `@SpringBootTest`:
   - Register `InMemoryProductRepository` as a `@Bean` via a test `@Configuration` class
   - Inject `ProductService` and verify it works within the Spring context

**Hints:**
- For the integration test, create an inner `@TestConfiguration` class:
  ```java
  @SpringBootTest
  class ProductServiceIntegrationTest {
      @TestConfiguration
      static class Config {
          @Bean
          ProductRepository productRepository() {
              return new InMemoryProductRepository();
          }
      }
  }
  ```
- The unit test and integration test should test the same behavior — the difference is
  whether Spring manages the wiring

---

## 6.2 — Domain Model & Value Objects

### Read First

**`java.time` — the date/time API:**

JavaScript's `Date` is famously awful. Java had the same problem — `java.util.Date` and
`Calendar` were terrible. Then Java 8 introduced `java.time`, which is excellent.

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

- **`Instant`** — a point on the UTC timeline. Nanosecond precision. Use for timestamps:
  "when did this event happen?"
- **`LocalDate`** — a date without time or timezone: `2026-03-18`. Use for "the promotion
  runs from March 18 to March 25."
- **`LocalDateTime`** — date + time, no timezone. **Not for timestamps** — without a
  timezone, the same `LocalDateTime` means different instants in different time zones.
- **`ZonedDateTime`** — date + time + timezone. Use when displaying to users in their
  timezone.
- **`Duration`** — time-based: "3 hours 30 minutes." For timeouts, shipping estimates.
- **`Period`** — date-based: "2 months 5 days." For subscription lengths, age.

All `java.time` objects are **immutable and thread-safe** — operations return new instances:

```java
LocalDate today = LocalDate.now();
LocalDate nextWeek = today.plusWeeks(1);  // today is unchanged
```

**`Clock` — testable time:**

`LocalDate.now()` uses the system clock. In tests, inject a `Clock`:

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

In production: `Clock.systemDefaultZone()`
In tests: `Clock.fixed(Instant.parse("2026-03-18T00:00:00Z"), ZoneId.of("UTC"))`

Same pattern as injecting a fake repository.

**`BigDecimal` — money without floating-point errors:**

`double` has precision errors: `0.1 + 0.2 != 0.3`. Financial calculations must use
`BigDecimal`. It's immutable — operations return new instances:

```java
BigDecimal price = new BigDecimal("19.99");
BigDecimal total = price.multiply(BigDecimal.valueOf(3));  // 59.97
```

**Watch out:** `new BigDecimal("1.0").equals(new BigDecimal("1.00"))` returns `false` —
they have different scales. Use `compareTo() == 0` for value equality.

### Exercise 6.2a — `Money` value object

1. **Create a `Money` record** in `common` (it'll be used across modules):
   - Fields: `BigDecimal amount`, `Currency currency` (use `java.util.Currency`)
   - Methods: `add(Money)`, `subtract(Money)`, `multiply(int quantity)`
   - Throw if currencies don't match on add/subtract
   - Compact constructor: validate amount is not null

2. **Write tests.** Think about:
   - Adding/subtracting same currency
   - Currency mismatch throws
   - Multiplying by quantity
   - Zero and negative amounts
   - Scale comparison edge case

**Hints:**
- `Currency.getInstance("USD")` gets a currency by ISO code
- `BigDecimal` arithmetic: `amount.add(other)`, `amount.subtract(other)`,
  `amount.multiply(BigDecimal.valueOf(quantity))`
- For equality in tests, either use `compareTo` or `stripTrailingZeros()` before comparing

### Exercise 6.2b — `DateRange` value object

1. **Create a `DateRange` record** in `common`:
   - Fields: `LocalDate start`, `LocalDate end`
   - Validation: `start` must not be after `end`
   - Methods: `contains(LocalDate)`, `overlaps(DateRange)`, `duration()` (returns `Period`)

2. **Write tests** for:
   - Containment (inside, outside, on boundaries)
   - Overlap detection (overlapping, non-overlapping, adjacent, contained)
   - Same start/end (single-day range)

**Hints:**
- Two ranges overlap if `start1 <= end2 AND start2 <= end1`
- `Period.between(start, end)` for duration
- Use `isBefore()` / `isAfter()` — don't compare with `<` / `>`

### Exercise 6.2c — `Promotion` with time-aware logic

1. **Create a `Promotion` class** in `catalog`:
   - Fields: description, discount percentage (`BigDecimal`), `DateRange`
   - Method: `isActive(Clock)` — is the promotion currently active?

2. **Write tests** using `Clock.fixed(...)`:
   - Before the promotion starts
   - During the promotion
   - After the promotion ends
   - On the exact start and end dates (boundary)

**Hints:**
- `Clock.fixed(Instant.parse("2026-03-18T00:00:00Z"), ZoneId.of("UTC"))` creates a fixed
  clock
- `LocalDate.now(clock)` uses the injected clock
- The promotion delegates to `DateRange.contains()` — keep the logic there

---

## 6.3 — Spring Web — REST APIs

### Read First

**The TS comparison:**

| Concept | Express / NestJS | Spring Web |
|---------|-----------------|------------|
| Route handler | `app.get('/products/:id')` / `@Get(':id')` | `@GetMapping("/{id}")` |
| Request body | `req.body` / `@Body()` | `@RequestBody` |
| Path param | `req.params.id` / `@Param('id')` | `@PathVariable("id")` |
| Query param | `req.query.page` / `@Query('page')` | `@RequestParam("page")` |
| Response status | `res.status(201).json(...)` | `ResponseEntity.status(201).body(...)` |
| Error handling | Error middleware | `@ControllerAdvice` + `@ExceptionHandler` |
| Validation | class-validator / Joi | `@Valid` + Jakarta Bean Validation |
| Controller class | NestJS `@Controller()` | `@RestController` |

**Anatomy of a controller:**

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable UUID id) {
        return productService.findProduct(new ProductId(id))
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(@Valid @RequestBody CreateProductRequest request) {
        Product product = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }
}
```

**Exception handling — centralized:**

```java
@ControllerAdvice
public class CatalogExceptionHandler {
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ProductNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(e.getMessage()));
    }
}
```

This is the equivalent of Express error middleware or NestJS exception filters. One class
handles all exception-to-response mapping.

**Input validation:**

```java
public record CreateProductRequest(
    @NotBlank String name,
    @NotNull @Positive BigDecimal price,
    String description
) {}
```

`@Valid` on the `@RequestBody` parameter triggers validation. Spring returns 400 with error
details automatically.

**Testing controllers with `MockMvc`:**

```java
@WebMvcTest(ProductController.class)
class ProductControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean  // one of the few places Mockito is acceptable
    private ProductService productService;

    @Test
    void returnsProductById() throws Exception {
        when(productService.findProduct(any()))
            .thenReturn(Optional.of(testProduct));

        mockMvc.perform(get("/api/products/{id}", productId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Widget"));
    }
}
```

**Why Mockito here:** `@WebMvcTest` loads only the web layer. The service isn't in the
context — you mock it because the test's concern is HTTP handling (routing, serialization,
status codes), not business logic. This is one of the rare cases where mocking is
appropriate in Detroit-school TDD: the test is about the boundary adapter, not the domain.

**Watch out:** `@WebMvcTest` is a *sliced* test — it only loads controllers, not the full
context. This makes it fast but means you need to provide (or mock) any dependencies the
controller needs.

### Exercise 6.3a — Product CRUD endpoints

1. **Add the Spring Web starter** (`spring-boot-starter-web`) to the catalog module.

2. **Create `CreateProductRequest`** — a record with validation annotations:
   - `@NotBlank name`, `@NotNull @Positive price`, optional `description`,
     optional `categoryId`

3. **Create `ProductController`** with these endpoints:
   - `POST /api/products` — create a product (returns 201)
   - `GET /api/products/{id}` — get by ID (returns 200 or 404)
   - `GET /api/products` — list all (returns 200)
   - `GET /api/products?category={id}` — filter by category
   - `PUT /api/products/{id}` — update (returns 200 or 404)
   - `DELETE /api/products/{id}` — soft-delete (returns 204 or 404)

4. **Create `CatalogExceptionHandler`** with `@ControllerAdvice`:
   - Handle not-found exceptions → 404
   - Handle validation errors → 400 with details

5. **Write `MockMvc` tests** for each endpoint:
   - Happy paths: create, read, list, update, delete
   - Error paths: not found, invalid input (missing name, negative price)

**Hints:**
- Add `spring-boot-starter-validation` for Jakarta Bean Validation
- `ResponseEntity.created(URI.create("/api/products/" + id)).body(product)` for 201
  responses with Location header
- For soft-delete, set an `active` flag rather than removing the entity
- `@RequestParam(required = false) UUID category` for optional query params
- `MockMvc` assertions: `status().isCreated()`, `jsonPath("$.id").exists()`,
  `content().contentType(MediaType.APPLICATION_JSON)`

---

## 6.4 — Spring Data JPA & Flyway

### Read First

**Flyway — database migrations:**

| Concept | TypeScript | Java |
|---------|-----------|------|
| Migration tool | Prisma Migrate / TypeORM migrations | Flyway |
| Migration format | TS/JS files or generated SQL | SQL files |
| Naming convention | Timestamp-based | `V1__description.sql`, `V2__description.sql` |
| Version tracking | `_prisma_migrations` table | `flyway_schema_history` table |

Flyway runs on startup. It checks `flyway_schema_history` to see which migrations have run,
then executes new ones in order. It checksums each file — if you edit an already-applied
migration, Flyway refuses to run. Migrations are immutable once applied.

File naming: `V1__create_product_table.sql` — `V` (versioned), `1` (version number),
`__` (double underscore separator), description, `.sql`.

**Testcontainers — real databases in tests:**

H2 never quite matches PostgreSQL. The modern approach: run a real PostgreSQL in Docker
during tests.

```java
@Testcontainers
class ProductRepositoryTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");
}
```

The container starts before tests, provides a JDBC URL, and tears down after.

**Spring Data JPA:**

Spring Data generates repository implementations from interfaces:

```java
public interface JpaProductRepository extends JpaRepository<ProductEntity, UUID> {
    List<ProductEntity> findByCategoryIdAndActiveTrue(UUID categoryId);

    @Query("SELECT p FROM ProductEntity p WHERE p.price BETWEEN :min AND :max")
    List<ProductEntity> findByPriceRange(@Param("min") BigDecimal min,
                                         @Param("max") BigDecimal max);
}
```

You write the interface. Spring writes the implementation. Query methods are derived from
the method name — `findByCategoryIdAndActiveTrue` becomes
`SELECT ... WHERE category_id = ? AND active = true`.

**JPA entities vs domain model:**

JPA entities are persistence concerns — they need `@Entity`, a no-arg constructor, mutable
setters (or field access). Your domain model should be clean Java — records, immutable,
no framework annotations. Map between them at the repository boundary:

```java
@Repository
public class PostgresProductRepository implements ProductRepository {
    private final JpaProductRepository jpa;

    public PostgresProductRepository(JpaProductRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Product> findById(ProductId id) {
        return jpa.findById(id.value()).map(ProductEntity::toDomain);
    }
}
```

This keeps your domain free of persistence concerns — exactly what your ArchUnit rules will
enforce.

**Watch out:** JPA entities need a no-arg constructor (can be `protected`). Fields must not
be `final` if you use field access. This feels wrong after building immutable domain
objects — that's why you map between entity and domain model rather than using one class
for both.

### Exercise 6.4a — Flyway migrations

1. **Add dependencies to the version catalog:**
   - `org.flywaydb:flyway-core`, `org.flywaydb:flyway-database-postgresql`
   - `org.testcontainers:testcontainers`, `org.testcontainers:junit-jupiter`,
     `org.testcontainers:postgresql`
   - `org.postgresql:postgresql` (JDBC driver)

2. **Write your first migration:**
   `catalog/src/main/resources/db/migration/V1__create_product_table.sql`
   ```sql
   CREATE TABLE product (
       id UUID PRIMARY KEY,
       name VARCHAR(255) NOT NULL,
       description TEXT,
       price DECIMAL(10,2) NOT NULL,
       active BOOLEAN NOT NULL DEFAULT true,
       created_at TIMESTAMP NOT NULL DEFAULT NOW()
   );
   ```

3. **Write `V2__create_category_table.sql`:**
   - `id UUID PRIMARY KEY`, `name VARCHAR(255) NOT NULL UNIQUE`, `description TEXT`

4. **Write `V3__add_category_to_product.sql`:**
   - `ALTER TABLE product ADD COLUMN category_id UUID REFERENCES category(id)`

5. **Write a test** using Testcontainers that runs the migrations and verifies the tables
   exist.

**Hints:**
- Flyway programmatic setup (Spring Boot will auto-configure this later, but for now):
  ```java
  Flyway flyway = Flyway.configure()
      .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
      .load();
  flyway.migrate();
  ```
- Verify with JDBC: query `information_schema.tables` for table names
- `@Container` on a `static` field = one container per test class (shared across methods)
- Migration files go in `src/main/resources/db/migration/` — Flyway's default location
- Testcontainers needs Docker running

**Experiment:** Edit `V1__create_product_table.sql` after running the test. Re-run and
observe Flyway's checksum error. This is why migrations are immutable.

### Exercise 6.4b — JPA entities and Spring Data repositories

1. **Create `ProductEntity`** in a `persistence` package:
   - `@Entity`, `@Table(name = "product")`
   - Fields matching the migration schema, plus a `toDomain()` method
   - A `static fromDomain(Product)` factory method
   - Protected no-arg constructor for JPA

2. **Create `CategoryEntity`** similarly.

3. **Create `JpaProductRepository`** extending `JpaRepository<ProductEntity, UUID>`:
   - `findByCategoryIdAndActiveTrue(UUID categoryId)`
   - `findByActiveTrue()`

4. **Create `PostgresProductRepository`** implementing your `ProductRepository` interface:
   - Wraps `JpaProductRepository`
   - Maps between `ProductEntity` and `Product` domain objects

5. **Write a `@DataJpaTest`** with Testcontainers:
   - Configure Spring to use the Testcontainer's PostgreSQL URL
   - Flyway runs automatically (Spring Boot auto-configures it)
   - Save a product, retrieve it, assert the domain object is correct

**Hints:**
- For `@DataJpaTest` with Testcontainers, use `@DynamicPropertySource`:
  ```java
  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
      registry.add("spring.datasource.url", postgres::getJdbcUrl);
      registry.add("spring.datasource.username", postgres::getUsername);
      registry.add("spring.datasource.password", postgres::getPassword);
  }
  ```
- `@DataJpaTest` only loads JPA-related beans. If you need your
  `PostgresProductRepository`, add `@Import(PostgresProductRepository.class)`
- Spring Boot auto-detects Flyway on the classpath and runs migrations on startup
- The JPA entity package must be scannable from `@SpringBootApplication`

### Exercise 6.4c — Contract tests

Now you have two `ProductRepository` implementations — `InMemoryProductRepository` (fake)
and `PostgresProductRepository` (real). Prove they behave identically.

1. **Extract a `ProductRepositoryContract`** — an abstract test class with all the
   repository behavior tests.

2. **`InMemoryProductRepositoryTest`** extends the contract, provides the fake.

3. **`PostgresProductRepositoryTest`** extends the contract, provides the real
   implementation with Testcontainers.

4. **Both test classes run the exact same tests** — if they pass on both, your fake is a
   faithful substitute for the real thing.

**Hints:**
- The contract class has an abstract method: `protected abstract ProductRepository repository()`
- Each subclass implements it — one returns the fake, one returns the Postgres-backed impl
- This is the same pattern you used for `LoanRepository` in the library project

---

## 6.5 — ArchUnit — Enforcing Architecture

### Read First

**What ArchUnit does:** It's a test library that asserts things about your code's
*structure* — which packages depend on which, what naming conventions are followed, where
annotations are allowed. It runs as a normal JUnit test.

Think of it as ESLint rules for architectural constraints. ESLint enforces "no unused
variables." ArchUnit enforces "the `domain` package must not import from
`infrastructure`."

Now that you have real layers (api, domain, persistence), these rules have teeth.

**Basic API:**

```java
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "tech.reactiv.ecommerce")
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainMustNotDependOnSpring =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("org.springframework..");
}
```

Rules read like English. `@AnalyzeClasses` scans once, reuses across all `@ArchTest` fields.

### Exercise 6.5a — Architecture rules

1. **Add `com.tngtech.archunit:archunit-junit5`** to the version catalog and as a
   `testImplementation` dependency.

2. **Create `ArchitectureTest`** in the catalog module's test source.

3. **Write these rules:**

   a. **Domain independence:** Classes in `..domain..` must not depend on Spring, JPA, or
      Jackson annotations. The domain model is pure Java.

   b. **No `java.util.logging`:** All logging through SLF4J.

   c. **No field injection:** No field annotated with `@Autowired`.

   d. **Controllers in api packages:** Classes annotated with `@RestController` must reside
      in `..api..` packages.

   e. **Entities in persistence packages:** Classes annotated with `@Entity` must reside
      in `..persistence..` packages.

4. **Run the tests.** They should pass — if they don't, you have an architectural violation
   to fix.

**Hints:**
- `@AnalyzeClasses(packages = "tech.reactiv.ecommerce")` scans all modules on the test
  classpath
- Rules are `static final` fields, not test methods — ArchUnit caches the class scan
- Start with these few rules. Add more as patterns emerge.
- Import from `com.tngtech.archunit.lang.syntax.ArchRuleDefinition`

**Watch out:** ArchUnit rules that are too strict early on will slow you down. The goal is
to prevent architectural decay, not to micromanage class placement.

---

## 6.6 — Observability

### Read First

**Your observability strategy (from ADR 0004):**

You chose the unified observability model — OTel spans with high-cardinality attributes as
the primary observability primitive, not separate logs/metrics/traces. Your own code
instruments with spans; SLF4J/Logback exists for ecosystem compatibility.

**What's already on your classpath:**

Spring Boot Starter bundles `spring-boot-starter-logging` (SLF4J + Logback). You don't need
to add logging dependencies manually. Spring, Hibernate, Flyway, and every other library in
the ecosystem logs through SLF4J — Logback handles their output automatically.

You only need to:
1. **Configure Logback** so the ecosystem's log output is readable
2. **Add OpenTelemetry** for your own instrumentation
3. Optionally bridge SLF4J logs into trace context (the OTel Java agent does this
   automatically in production)

**The TS comparison:**

| Concept | TypeScript | Java |
|---------|-----------|------|
| Instrumentation API | `@opentelemetry/api` | `io.opentelemetry:opentelemetry-api` |
| Auto-instrumentation | `@opentelemetry/auto-instrumentations-node` | OpenTelemetry Java Agent |
| Manual spans | `tracer.startSpan("op")` | `tracer.spanBuilder("op").startSpan()` |
| Span attributes | `span.setAttribute("key", val)` | `span.setAttribute("key", val)` |
| Context propagation | `context.with(span)` | `try (Scope scope = span.makeCurrent())` |

**Manual instrumentation:**

```java
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
```

**Wide events — the key practice:**

The value isn't in creating spans. It's in what you attach. A span for a product lookup
might carry:
- `product.id`, `product.category` (what)
- `cache.hit`, `db.query_count` (how the system behaved)
- `user.tier` (who)
- `response.status`, `duration_ms` (outcome)

This enables queries like "show me all requests where `cache.hit=false` and
`db.query_count > 5`." You can't do that with log lines.

**Why not `log.info()` for your own code?**

If you're attaching `product.id` to a span, writing `log.info("Product created: id={}", id)`
is redundant — the span already captures the event. Reach for SLF4J when:
- You need to see something during local development before OTel is wired up
- A library is logging something you want to control the level of

Otherwise, span attributes are your primary instrument.

**SLF4J key concepts (for reading ecosystem output):**
- SLF4J is the facade (API), Logback is the implementation
- Parameterized messages: `log.info("Order {}", orderId)` — NOT string concatenation
- Log levels: `ERROR` > `WARN` > `INFO` > `DEBUG` > `TRACE`
- Spring Boot's defaults are sensible — you may not need custom config immediately
- `logback-spring.xml` (not `logback.xml`) lets you use Spring profiles in the config
- Never log sensitive data (passwords, tokens, PII)

### Exercise 6.6a — Configure Logback for ecosystem output

1. **Create `logback-spring.xml`** in `catalog/src/main/resources/`:
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
   </configuration>
   ```

2. **Run your existing tests** and verify you see Spring/Hibernate output at the right
   levels. Adjust if needed (e.g., set Hibernate SQL logging to `DEBUG` for development).

This is configuration, not an exercise — just get the ecosystem's output readable.

### Exercise 6.6b — Add OpenTelemetry to the catalog service

1. **Add the OpenTelemetry BOM** to the version catalog, then add dependencies to the
   catalog module:
   - `io.opentelemetry:opentelemetry-api` — `implementation`
   - `io.opentelemetry:opentelemetry-sdk` — `testImplementation` (SDK configured in tests
     for now)
   - `io.opentelemetry:opentelemetry-exporter-logging` — `testImplementation` (console
     output)
   - Only the BOM needs a version entry in the catalog. Individual artifacts use string
     coordinates with `platform()` resolving versions.

2. **Inject a `Tracer`** into `ProductService` (constructor parameter).

3. **Instrument `findProduct`** and `createProduct` with spans:
   - Span name: `"ProductService.findProduct"`
   - Attributes: `product.id`, `product.found`, `product.name`, `product.category`

4. **Write a test** that configures the SDK, creates the service with a real tracer, and
   runs an operation. Check the console for span output.

**Hints:**
- SDK setup in tests:
  ```java
  SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
      .addSpanProcessor(SimpleSpanProcessor.create(LoggingSpanExporter.create()))
      .build();
  OpenTelemetry otel = OpenTelemetrySdk.builder()
      .setTracerProvider(tracerProvider)
      .build();
  Tracer tracer = otel.getTracer("catalog-test");
  ```
- Don't over-instrument. Service boundaries and key decision points, not every method.
- The `Tracer` should be injected (constructor parameter), same pattern as `Clock`

---

## 6.7 — Layered Architecture & Integration Testing

### Read First

You now have all the layers:
- **API** (`@RestController`) — HTTP in/out
- **Service** (`@Service`) — business logic
- **Persistence** (`@Repository`) — database access
- **Domain** — pure Java, no framework dependencies

**Testing at each layer:**

| Layer | Test style | What it proves |
|-------|-----------|---------------|
| Domain | Plain JUnit, no Spring | Value objects, business rules work |
| Service | JUnit with fakes | Business logic works with correct dependencies |
| Controller | `@WebMvcTest` with mocked service | HTTP routing, serialization, status codes |
| Repository | `@DataJpaTest` + Testcontainers | SQL, JPA mappings, query derivation |
| Integration | `@SpringBootTest` + Testcontainers | Everything works together |

**Full integration tests:**

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class CatalogIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void createAndRetrieveProduct() {
        var request = new CreateProductRequest("Widget", new BigDecimal("19.99"), null, null);
        var created = restTemplate.postForEntity("/api/products", request, Product.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        var retrieved = restTemplate.getForEntity(
            "/api/products/" + created.getBody().id(), Product.class);
        assertThat(retrieved.getBody().name()).isEqualTo("Widget");
    }
}
```

This starts the full application, including web server, Spring context, and a real database.
It tests the entire request path from HTTP to database and back.

### Exercise 6.7a — Integration tests

1. **Write full integration tests** with `@SpringBootTest` and Testcontainers:
   - Create a product via POST, retrieve via GET
   - List products, filter by category
   - Update a product, verify changes persist
   - Soft-delete, verify it no longer appears in listings

2. **Verify the full test pyramid:**
   - Domain: `Money`, `DateRange`, `Promotion` tests (no Spring)
   - Service: `ProductService` with `InMemoryProductRepository` (no Spring)
   - Controller: `@WebMvcTest` with mocked service
   - Repository: `@DataJpaTest` + Testcontainers (contract tests)
   - Integration: `@SpringBootTest` + Testcontainers (end-to-end)

**Hints:**
- `TestRestTemplate` is auto-configured by `@SpringBootTest` with `RANDOM_PORT`
- Use `@DynamicPropertySource` to point Spring at the Testcontainer's PostgreSQL
- Integration tests are slow — keep them focused on proving the layers connect, not
  re-testing business logic

---

## 6.8 — Capstone

### Exercise 6.8a — Verify the full Catalog context

Run through this checklist:

1. **`./gradlew clean build`** — all modules compile, all tests pass
2. **CRUD API works** — create, read, list, filter, update, soft-delete products
3. **Flyway migrations** run against Testcontainers PostgreSQL
4. **Contract tests** pass on both fake and real repository implementations
5. **ArchUnit rules** pass — domain is clean, controllers in api, entities in persistence
6. **Observability** — OTel spans appear with meaningful attributes, Logback output is
   readable
8. **Value objects** (`Money`, `DateRange`, `ProductId`) have test coverage
9. **Integration tests** prove the full request path works

If all of this passes, the Catalog context is complete and production-shaped.

---

## Review

After completing Phase 6, your e-commerce project should have:

- [ ] Spring Boot application in the `catalog` module with DI wired up
- [ ] `ProductService` with constructor-injected dependencies
- [ ] Full CRUD REST API for products with validation and error handling
- [ ] JPA entities mapped to domain objects, kept in persistence packages
- [ ] Flyway migrations for product and category tables
- [ ] Testcontainers PostgreSQL in all database tests
- [ ] Contract tests proving fake and real repositories behave identically
- [ ] ArchUnit rules enforcing architectural boundaries
- [ ] Logback configured for ecosystem output, OTel spans for your own instrumentation
- [ ] `Money`, `DateRange`, `Promotion` value objects with test coverage
- [ ] Full test pyramid: domain → service → controller → repository → integration
- [ ] All tests passing with `./gradlew clean build`

**Consider:**
1. How does Spring's DI compare to Angular's? What's better, what's worse?
2. The entity-to-domain mapping at the repository boundary — is it worth the overhead?
   What would break if you used JPA entities as your domain model?
3. Where in the test pyramid did you find bugs? Which layer's tests gave you the most
   confidence?
4. How does the `Clock` injection pattern compare to mocking `Date.now()` in TS?
5. What ADRs should you write for decisions you made in this phase?