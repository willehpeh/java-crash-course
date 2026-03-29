# Phase 6 — Spring Boot & the Catalog Context

> **Goal:** Build the Catalog bounded context as a full CRUD API with Spring Boot, Spring
> Data JPA, and PostgreSQL. The architecture is hexagonal with package-by-use-case
> organization. Ecosystem tooling — Flyway, Testcontainers, ArchUnit, OpenTelemetry,
> `java.time` — is introduced just-in-time as the domain requires it.
>
> **Everything lives in the e-commerce repo.** The phase file stays here (it's course
> material); all code goes in the `ecommerce` project you scaffolded in Phase 5.
>
> **Starting point:** You have a multi-module Gradle project with `common` and `catalog`
> modules, cross-module dependencies working, and ADRs committed. No Spring Boot yet —
> this phase adds it.
>
> **Testing approach:** Outside-in. Tests run against command/query handlers (the primary
> ports), with real domain objects and fake secondary adapters. Infrastructure adapters
> get their own contract tests to prove they behave like the fakes.

---

## 6.1 — Architecture: Hexagonal + Package-by-Use-Case

### Read First

**Hexagonal architecture (Ports & Adapters):**

The core idea: the domain is at the centre and knows nothing about the outside world. It
defines **ports** (interfaces) that describe what it needs. **Adapters** implement those
ports for specific technologies.

```
                    ┌─────────────────────────────┐
  Primary           │         Domain              │         Secondary
  Adapters          │  (entities, value objects,   │         Adapters
  (drive the app)   │   command/query handlers)     │         (driven by the app)
                    │                             │
  Controller ──────▶│  port: CreateProductHandler  │         port: ProductRepository
  CLI ─────────────▶│  port: FindProductHandler    │────────▶ PostgresProductRepository
  Message listener ▶│                             │────────▶ InMemoryProductRepository (fake)
                    └─────────────────────────────┘
```

| Hexagonal term | Role | Your code |
|---|---|---|
| **Primary adapter** | Drives the application | `@RestController`, CLI, message consumer |
| **Primary port** | Entry point to the domain | Command/query handler (`AddToCatalogHandler`) |
| **Domain** | Business logic, pure Java | Entities, value objects, domain services |
| **Secondary port** | Interface the domain needs | `ProductRepository` (defined by the domain) |
| **Secondary adapter** | Implements infrastructure | `PostgresProductRepository`, Stripe client |

**The TS comparison:**

If you've used NestJS with clean architecture or Angular with a ports-and-adapters pattern,
this is the same concept. The controller is a primary adapter that translates HTTP into
domain calls. The repository is a secondary port that the domain defines and infrastructure
implements.

**Why this matters:** The domain never depends on Spring, JPA, or any framework. You can
test it with plain JUnit and fake adapters. The framework concerns live at the edges.

**Package-by-use-case — not by layer:**

Traditional package-by-layer (`domain/`, `service/`, `controller/`, `repository/`) makes
everything `public` because classes in different packages need to see each other. This
defeats the isolation that hexagonal promises.

Package-by-use-case groups everything for a single command or query together. Each package
owns its handler, its command/query object, and any internal helpers. Internal classes are
**package-private** — genuinely hidden, not just by convention.

**Catalog module structure:**

```
catalog/src/main/java/tech/reactiv/ecommerce/catalog/
  addtocatalog/
    AddToCatalogHandler.java         ← command handler, package-private internals
    AddToCatalogCommand.java         ← command
  discontinue/
    DiscontinueProductHandler.java
  reprice/
    RepriceProductHandler.java
    RepriceCommand.java
  search/
    SearchCatalogHandler.java
    SearchCatalogQuery.java
  lookup/
    LookupProductHandler.java
  product/                           ← vocabulary
    Product.java
    ProductId.java
    ProductRepository.java           ← port (interface)
  category/
    Category.java
    CategoryId.java
  promotion/                         ← vocabulary
    Promotion.java
    PromotionId.java
    PromotionTarget.java             ← sealed: AllProducts, ByCategory, ByProducts
    PromotionRepository.java         ← port (interface)
  createpromotion/
    CreatePromotionHandler.java
    CreatePromotionCommand.java
  api/                               ← REST entry point
    CatalogController.java
    CatalogExceptionHandler.java
  infrastructure/
    persistence/                     ← database implementation
      ProductEntity.java
      CategoryEntity.java
      JpaProductRepository.java
      PostgresProductRepository.java
```

**What goes where:**

- **`product/`, `category/`** — vocabulary packages. Named by business concept, not by
  architectural layer. `Product`, `ProductId`, `ProductRepository` (port interface). Pure
  Java, no framework annotations. Public — used across command/query packages.
- **`addtocatalog/`, `discontinue/`, `reprice/`** — command packages. One per write
  operation. Each contains a command (input), a handler, and any internal helpers
  (package-private). **`search/`, `lookup/`** — query packages. One per read operation.
  Named by what the business does, not CRUD verbs.
- **`api/`** — the REST entry point. The controller translates HTTP to command/query
  handler calls. Depends on handlers, not on domain internals.
- **`infrastructure/persistence/`** — database implementation. JPA entities, Spring Data
  interfaces, `PostgresProductRepository`. The `infrastructure/` parent gives future
  concerns (messaging, external APIs, file storage) a natural home.

**Command and query handlers — not "services":**

Instead of a god-class `ProductService` with every operation, each command or query gets
its own handler. Named by business operation, not CRUD verb. This is CQRS in its simplest
form — separate write path from read path, no event sourcing required:

```java
@Component
public class AddToCatalogHandler {
    private final ProductRepository repository;

    public AddToCatalogHandler(ProductRepository repository) {
        this.repository = repository;
    }

    public ProductId handle(AddToCatalogCommand command) {
        var product = new Product(
            ProductId.generate(),
            command.name(),
            command.description(),
            command.price()
        );
        repository.save(product);
        return product.id();
    }
}
```

**Testing approach:**

Handler tests use fake secondary adapters — no Spring context needed. Controller tests
(`@WebMvcTest`) verify HTTP wiring with mocked handlers. Contract tests prove the fake
repository matches the real implementation.

**For CQRS/ES modules later (Order, Inventory):**

The same structure applies. The only difference is event sourcing behind the command
handlers — the package organization is identical:

```
order/
  placeorder/            ← command handler (+ PlaceOrderCommand)
  cancelorder/           ← command handler
  shiporder/             ← command handler
  orderdetails/          ← query handler (owns its read model/projection)
  orderhistory/          ← query handler
  order/                 ← vocabulary (Order aggregate, OrderId, OrderStatus, events)
  infrastructure/
    persistence/
```

The Catalog already uses the same command/query vocabulary. When Order adds event sourcing,
the handler internals change but the package structure doesn't.

**Watch out:** Package-by-use-case can feel like a lot of packages for simple CRUD. That's
OK — the structure pays off as complexity grows, and the visibility enforcement prevents
accidental coupling from the start.

### Exercise 6.1a — Write the architecture ADR

Before writing code, document the decision.

1. **Create `docs/adr/0005-hexagonal-package-by-use-case.md`** in the e-commerce repo:
   - Why hexagonal over layered architecture?
   - Why package-by-use-case over package-by-layer?
   - Consequence: more packages, but genuine encapsulation via package-private visibility

2. **Create `docs/adr/0006-cqrs-everywhere.md`:**
   - Why CQRS across all modules, not just event-sourced ones?
   - CQRS is just separating the write path (commands) from the read path (queries) — it
     doesn't require event sourcing, separate databases, or eventual consistency
   - Commands: instructions to change state (`AddToCatalogCommand`, `RepriceCommand`)
   - Queries: requests for information (`LookupProductQuery`, `SearchCatalogQuery`)
   - Consequence: consistent vocabulary across all modules — the Catalog uses the same
     command/query pattern that Order and Inventory will use with event sourcing later

---

## 6.2 — Spring Boot Setup & Dependency Injection

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
@Component
public class AddToCatalogHandler {
    private final ProductRepository repository;

    // Single constructor — Spring auto-injects. No @Autowired needed.
    public AddToCatalogHandler(ProductRepository repository) {
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
This is hexagonal in action — the port (`ProductRepository`) stays the same, only the
adapter changes.

**Testing with Spring:**

```java
@SpringBootTest
class AddToCatalogHandlerIntegrationTest {
    @Autowired  // OK in tests — injection happens into test class
    private AddToCatalogHandler handler;

    @Test
    void createsProduct() {
        // ...
    }
}
```

`@SpringBootTest` starts the full application context. For faster, focused tests, use
sliced annotations like `@WebMvcTest` (controller layer only) or `@DataJpaTest`
(repository layer only).

For handler unit tests, construct handlers directly — no Spring context needed. Save
`@SpringBootTest` for integration tests where you need the full wiring.

**Watch out:** Spring Boot auto-configuration is powerful but magical. If something isn't
working, check: is the class in a package that Spring is scanning? (It scans the package
of your `@SpringBootApplication` class and below.) Is the dependency on the classpath?
Spring configures beans based on what's available.

### Exercise 6.2a — Add Spring Boot to the project

1. **Add the Spring Boot plugin and dependencies to the version catalog:**
   - `org.springframework.boot` plugin (apply to the `catalog` module)
   - `org.springframework.boot:spring-boot-starter-web` — Spring MVC + embedded Tomcat +
     Jackson (includes the base starter transitively, so DI and auto-configuration come
     for free)
   - `org.springframework.boot:spring-boot-starter-test` — test support
   - Look up the latest Spring Boot 4.x version
   - Spring Boot manages its own dependency versions — you may want to use its BOM via
     `platform()`

2. **Create `CatalogApplication.java`** in your base package (e.g.,
   `tech.reactiv.ecommerce.catalog`):
   - Annotate with `@SpringBootApplication`
   - No `main` method needed yet — `@SpringBootTest` bootstraps the context for tests.
     You'll add `main` in 6.4a when you have endpoints to hit.

**Hints:**
- `@SpringBootApplication` is a shortcut for `@Configuration` + `@EnableAutoConfiguration`
  + `@ComponentScan`
- Spring scans the package of this class and everything below it — make sure your use case
  and vocabulary packages are descendants

### Exercise 6.2b — First command: add to catalog

1. **Create the vocabulary packages:**
   - `product/` — `ProductId` (record), `Product`, `ProductRepository` (port interface
     with `save`, `findById`, `findAll`, `findByCategory`)
   - `category/` — `CategoryId` (record), `Category`

2. **Create `InMemoryProductRepository`** in the test source tree — expose the backing
   `Map<ProductId, Product>` as a public accessor.

3. **Create the `addtocatalog/` package:**
   - `AddToCatalogCommand` (record with name, description, `BigDecimal price`)
   - `AddToCatalogHandler` (annotated `@Component`, takes `ProductRepository` via
     constructor, returns `ProductId`)
   - `Product.price()` returns `BigDecimal` for now — you'll refactor to `Money` in 6.3b

4. **Test it** — verify the handler saves a product and returns its ID. Assert against
   the fake's backing map, not through repository query methods.

**Hints:**
- `ProductId.generate()` can wrap `UUID.randomUUID()`
- The handler creates the domain object and delegates to the repository — keep it thin

### Exercise 6.2c — Remaining commands and queries

**Commands (write operations):**
1. **`reprice/`** — `RepriceProductHandler` takes a `RepriceCommand` (product ID + new
   price). Throws if product not found.
2. **`discontinue/`** — `DiscontinueProductHandler` takes a product ID, sets `active` to
   false. Throws if product not found.

**Queries (read operations):**
3. **`lookup/`** — `LookupProductHandler` takes a `ProductId`, returns `Optional<Product>`.
4. **`search/`** — `SearchCatalogHandler` takes a `SearchCatalogQuery` (optional category
   filter), returns `List<Product>`.

Test each handler against the `InMemoryProductRepository`.

**Hints:**
- Share the same `InMemoryProductRepository` instance across handlers in tests — seed it
  with products in `@BeforeEach`

---

## 6.3 — Promotions

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

You'll use `LocalDate` for promotion active periods and `Clock` for testable time — both
introduced in the exercises below.

**`Clock` — testable time:**

`LocalDate.now()` uses the system clock. In tests, inject a `Clock`:

```java
public class Promotion {
    private final PromotionId id;
    private final String description;
    private final BigDecimal discountPercentage;
    private final DateRange activePeriod;
    private final PromotionTarget target;

    public boolean isActive(Clock clock) {
        return activePeriod.contains(LocalDate.now(clock));
    }

    public Money apply(Money originalPrice) {
        var discount = originalPrice.multiply(discountPercentage)
            .divide(new BigDecimal("100"));
        return originalPrice.subtract(discount);
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

### Exercise 6.3a — Create a promotion (handler first)

The Catalog needs promotions — discounts that apply to products for a limited time. Start
from the use case.

1. **Test `CreatePromotionHandler`** — assert it saves a promotion and returns a
   `PromotionId`.

2. **Create the types the test needs:**
   - `createpromotion/` use-case package: `CreatePromotionCommand` (record: description,
     discount percentage as `BigDecimal`, start date, end date, `PromotionTarget`) and
     `CreatePromotionHandler` (`@Component`, takes `PromotionRepository`, returns
     `PromotionId`)
   - `promotion/` vocabulary package: `PromotionId` (record wrapping `UUID`, same pattern
     as `ProductId`), `PromotionTarget`, `Promotion`, `PromotionRepository`
   - `PromotionTarget` — a sealed interface with data-only record implementations:
     ```java
     public sealed interface PromotionTarget
         permits AllProducts, ByCategory, ByProducts {
         record AllProducts() implements PromotionTarget {}
         record ByCategory(CategoryId categoryId) implements PromotionTarget {}
         record ByProducts(Set<ProductId> productIds) implements PromotionTarget {}
     }
     ```
     `PromotionTarget` is pure data — no `appliesTo(Product)` method. Filtering logic
     lives in the repository implementations (Java for the fake, SQL for the real one),
     keeping matching logic in one place per adapter rather than duplicated across both.
   - `Promotion` — class with `PromotionId`, description, discount percentage
     (`BigDecimal`), `DateRange`, and `PromotionTarget`
   - `PromotionRepository` — port interface with `save(Promotion)` (more methods later)
   - `DateRange` record in `common` — fields: `LocalDate start`, `LocalDate end`. Minimal
     for now; you'll add methods in the next exercise.

3. **Create `InMemoryPromotionRepository`** in test source — `save` and a backing map.

**Hints:**
- The handler generates a `PromotionId`, builds a `Promotion` from the command, saves it,
  and returns the ID. Keep it thin.

### Exercise 6.3b — DateRange and isActive

Promotions need to know whether they're active. Drive `DateRange` and
`Promotion.isActive()` through tests.

1. **Test `DateRange`:**
   - Compact constructor: validate `start` is not after `end`
   - `contains(LocalDate)`: inside, outside, on start boundary, on end boundary
   - `overlaps(DateRange)`: overlapping, non-overlapping, adjacent, one contained within
     the other
   - `duration()` (returns `Period`): normal range, single-day range (same start and end)

2. **Add `isActive(Clock)` to `Promotion`** — delegates to `DateRange.contains()`.

3. **Test `isActive`** with `Clock.fixed(...)`:
   - Before the promotion starts
   - During the promotion
   - After the promotion ends
   - On the exact start and end dates (boundary)

**Hints:**
- `Clock.fixed(Instant.parse("2026-03-18T00:00:00Z"), ZoneId.of("UTC"))` creates a fixed
  clock — this is the same pattern as injecting a fake repository
- `LocalDate.now(clock)` uses the injected clock instead of the system clock
- Two ranges overlap if `start1 <= end2 AND start2 <= end1`
- `Period.between(start, end)` for duration
- Use `isBefore()` / `isAfter()` — don't compare with `<` / `>`

### Exercise 6.3c — Money and discount calculation

`Promotion` needs to calculate discounted prices. Raw `BigDecimal` doesn't carry currency.

1. **Test `Money`:**
   - Fields: `BigDecimal amount`, `Currency currency` (use `java.util.Currency`)
   - Compact constructor: validate amount is not null
   - `add(Money)`, `subtract(Money)`: same currency works, mismatched currencies throw
   - `multiply(int quantity)`, `multiply(BigDecimal factor)`
   - Zero and negative amounts, scale comparison edge case

2. **Test `Promotion.apply(Money)`:**
   - 10% off a known price (e.g., 10% off $100 → $90)
   - 0% discount (no change)
   - 100% discount (free)
   - Rounding behavior (e.g., 10% off $19.99)

3. **Refactor `Product.price()`** from `BigDecimal` to `Money`. Update
   `AddToCatalogCommand`, the handler, and tests. This is a deliberate refactoring moment —
   the type was raw `BigDecimal` because you didn't need `Money` yet.

**Hints:**
- `Currency.getInstance("USD")` gets a currency by ISO code
- `BigDecimal` arithmetic: `amount.add(other)`, `amount.subtract(other)`,
  `amount.multiply(BigDecimal.valueOf(quantity))`
- For equality in tests, either use `compareTo` or `stripTrailingZeros()` before comparing
- Use `RoundingMode.HALF_UP` for financial rounding — `setScale(2, RoundingMode.HALF_UP)`

### Exercise 6.3d — Look up a product with promotions

A product lookup should return the effective price with any active promotions applied. No
separate "find promotions for product" handler — update the existing
`LookupProductHandler`.

1. **Test the updated handler:**
   - No active promotions → original price
   - One active promotion → discounted price
   - Multiple promotions → best discount wins
   - Expired promotion → ignored
   - Promotion targeting a different category → ignored

2. **Add `findActiveForProduct(ProductId, CategoryId, Clock)` to `PromotionRepository`.**

3. **Implement in `InMemoryPromotionRepository`** — filter the backing map by inspecting
   `PromotionTarget`:
   - `AllProducts` → always matches
   - `ByCategory(categoryId)` → matches if category matches
   - `ByProducts(productIds)` → matches if product ID is in the set
   - Then filter by `isActive(clock)`

4. **Update `LookupProductHandler`** to take `PromotionRepository` and `Clock` as
   constructor dependencies. When looking up a product:
   - Find active promotions for that product
   - Apply the best (highest) discount
   - Return the product with its effective price

**Hints:**
- The handler already returns `Optional<Product>` — you might return a richer response
  type or add the effective price to the product. Design choice is yours.
- `InMemoryPromotionRepository.findActiveForProduct` uses pattern matching on the sealed
  `PromotionTarget` — this is where the sealed interface pays off
- The real `PostgresPromotionRepository` will do this filtering in SQL (6.5) — that's why
  the matching logic isn't on `PromotionTarget` itself

---

## 6.4 — Spring Web — REST APIs (Primary Adapter)

### Read First

The controller is the **REST entry point** — it translates HTTP requests into command/query
handler calls and domain responses back into HTTP responses. It should be thin: parse the
request, call the handler, format the response.

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
public class CatalogController {
    private final AddToCatalogHandler addToCatalog;
    private final LookupProductHandler lookupProduct;
    private final SearchCatalogHandler searchCatalog;

    public CatalogController(AddToCatalogHandler addToCatalog,
                             LookupProductHandler lookupProduct,
                             SearchCatalogHandler searchCatalog) {
        this.addToCatalog = addToCatalog;
        this.lookupProduct = lookupProduct;
        this.searchCatalog = searchCatalog;
    }

    @PostMapping
    public ResponseEntity<ProductId> add(@Valid @RequestBody AddToCatalogCommand command) {
        ProductId id = addToCatalog.handle(command);
        return ResponseEntity.created(URI.create("/api/products/" + id.value())).body(id);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getById(@PathVariable UUID id) {
        return lookupProduct.handle(new ProductId(id))
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
```

The controller injects command and query handlers — not a monolithic service. Each endpoint
delegates to the appropriate handler.

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
public record AddToCatalogCommand(
    @NotBlank String name,
    @NotNull @Positive BigDecimal price,
    String description
) {}
```

`@Valid` on the `@RequestBody` parameter triggers validation. Spring returns 400 with error
details automatically.

**Testing controllers with `MockMvc`:**

```java
@WebMvcTest(CatalogController.class)
class CatalogControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AddToCatalogHandler addToCatalogHandler;

    @MockitoBean
    private LookupProductHandler lookupProductHandler;

    @MockitoBean
    private SearchCatalogHandler searchCatalogHandler;

    @Test
    void returnsProductById() throws Exception {
        when(lookupProductHandler.handle(any()))
            .thenReturn(Optional.of(testProduct));

        mockMvc.perform(get("/api/products/{id}", productId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Widget"));
    }
}
```

**Why Mockito here:** `@WebMvcTest` loads only the web layer. The handlers aren't in the
context — you mock them because the test's concern is HTTP handling (routing, serialization,
status codes), not business logic.

**Watch out:** `@WebMvcTest` is a *sliced* test — it only loads controllers, not the full
context. This makes it fast but means you need to provide (or mock) any dependencies the
controller needs.

### Exercise 6.4a — Product endpoints

1. **Add a `main` method** to `CatalogApplication`:
   ```java
   public static void main(String[] args) {
       SpringApplication.run(CatalogApplication.class, args);
   }
   ```

2. **Create `CatalogController`** in `api/` with these endpoints:
   - `POST /api/products` — delegates to `AddToCatalogHandler` (returns 201)
   - `GET /api/products/{id}` — delegates to `LookupProductHandler` (returns 200 or 404)
   - `GET /api/products` — delegates to `SearchCatalogHandler` (returns 200)
   - `GET /api/products?category={id}` — delegates to `SearchCatalogHandler` with filter
   - `PUT /api/products/{id}/price` — delegates to `RepriceProductHandler` (returns 200
     or 404)
   - `DELETE /api/products/{id}` — delegates to `DiscontinueProductHandler` (returns 204
     or 404)
   - `POST /api/promotions` — delegates to `CreatePromotionHandler` (returns 201)
   - Note: `GET /api/products/{id}` now returns effective price (from `LookupProductHandler`
     which queries active promotions — wired in 6.3c)

3. **Create `CatalogExceptionHandler`** in `api/` with `@ControllerAdvice` — maps
   exceptions to HTTP status codes (not-found → 404, validation → 400).

4. **Add validation annotations** to `AddToCatalogCommand`: `@NotBlank name`,
   `@NotNull @Positive price`.

5. **Test with `MockMvc`** — happy paths (create, read, list, filter, update, delete) and
   error paths (not found, invalid input).

**Hints:**
- Add `spring-boot-starter-validation` for Jakarta Bean Validation
- `ResponseEntity.created(URI.create("/api/products/" + id)).body(product)` for 201
  responses with Location header
- For soft-delete, the `DeleteProductHandler` sets `active` to false
- `@RequestParam(required = false) UUID category` for optional query params
- `MockMvc` assertions: `status().isCreated()`, `jsonPath("$.id").exists()`,
  `content().contentType(MediaType.APPLICATION_JSON)`
- Verify `./gradlew :catalog:bootRun` starts the app and you can hit the endpoints with
  `curl` or your browser

---

## 6.5 — Spring Data JPA & Flyway (Infrastructure)

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
setters (or field access). Your domain model is pure Java — records, immutable, no
framework annotations. `PostgresProductRepository` in `infrastructure/persistence/` maps
between them at the boundary:

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

### Exercise 6.5a — Flyway migrations

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

5. **Write `V4__create_promotion_table.sql`:**
   - `id UUID PRIMARY KEY`, `description VARCHAR(255) NOT NULL`,
     `discount_percentage DECIMAL(5,2) NOT NULL`, `start_date DATE NOT NULL`,
     `end_date DATE NOT NULL`, `target_type VARCHAR(50) NOT NULL`,
     `target_data JSONB` (holds category ID or product ID set, null for `AllProducts`)

6. **Write a test** using Testcontainers that runs the migrations and verifies the tables
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

### Exercise 6.5b — JPA entities and the persistence infrastructure

1. **Create `ProductEntity`** in `infrastructure/persistence/`:
   - `@Entity`, `@Table(name = "product")`
   - Fields matching the migration schema, plus `toDomain()` method and
     `static fromDomain(Product)` factory
   - Protected no-arg constructor for JPA

2. **Create `CategoryEntity`** similarly.

3. **Create `PromotionEntity`** in `infrastructure/persistence/`:
   - Fields matching the V4 migration schema
   - `target_type` stores the sealed type name (`ALL_PRODUCTS`, `BY_CATEGORY`,
     `BY_PRODUCTS`), `target_data` stores the JSONB payload
   - `toDomain()` reconstructs the sealed `PromotionTarget` from `target_type` +
     `target_data`
   - `fromDomain(Promotion)` maps in the other direction

4. **Create `JpaProductRepository`** extending `JpaRepository<ProductEntity, UUID>`:
   - `findByCategoryIdAndActiveTrue(UUID categoryId)`
   - `findByActiveTrue()`

5. **Create `PostgresProductRepository`** implementing `ProductRepository`:
   - Wraps `JpaProductRepository`, maps between entity and domain objects

6. **Create `PostgresPromotionRepository`** implementing `PromotionRepository`:
   - `findActiveForProduct(ProductId, CategoryId, Clock)` uses SQL to filter by target
     type, target data, and active date range — the SQL equivalent of the Java filtering
     in `InMemoryPromotionRepository`

7. **Test with `@DataJpaTest`** + Testcontainers — save a product, retrieve it, assert the
   domain object round-trips correctly. Test category filtering. Test promotion round-trip
   including `PromotionTarget` serialization.

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

### Exercise 6.5c — Contract tests

1. **Extract a `ProductRepositoryContract`** — an abstract test class with all the
   repository behavior tests.

2. **`InMemoryProductRepositoryTest`** extends the contract, provides the fake.

3. **`PostgresProductRepositoryTest`** extends the contract, provides the real
   implementation with Testcontainers.

4. **Do the same for `PromotionRepository`** — extract a `PromotionRepositoryContract`,
   run it against both `InMemoryPromotionRepository` and `PostgresPromotionRepository`.
   Key scenarios: save/retrieve, `findActiveForProduct` with each `PromotionTarget` variant,
   expired promotions excluded.

Same pattern as `LoanRepository` in the library project.

**Hints:**
- The contract class has an abstract method: `protected abstract ProductRepository repository()`
- Each subclass implements it — one returns the fake, one returns the Postgres-backed impl

---

## 6.6 — ArchUnit — Enforcing Hexagonal Boundaries

### Read First

**What ArchUnit does:** It's a test library that asserts things about your code's
*structure* — which packages depend on which, what naming conventions are followed, where
annotations are allowed. It runs as a normal JUnit test.

Think of it as ESLint rules for architectural constraints. Now that you have hexagonal
architecture with real boundaries (vocabulary, commands, queries, adapters), these rules enforce the
dependency direction.

**The key hexagonal rule:** Dependencies point inward. Infrastructure and API depend on the
vocabulary and commands/queries. The vocabulary depends on nothing external.

```
api/  ──▶  commands/queries  ──▶  vocabulary (product/, category/)  ◀──  infrastructure/persistence/
                                       │
                                       ▼
                                   (nothing)
```

### Exercise 6.6a — Architecture rules

1. **Add `com.tngtech.archunit:archunit-junit5`** to the version catalog and as a
   `testImplementation` dependency.

2. **Create `ArchitectureTest`** in the catalog module's test source.

3. **Write these rules:**

   a. **Vocabulary independence:** Classes in `..product..` and `..category..` (vocabulary
      packages) must not depend on Spring, JPA, or Jackson annotations. Pure Java.

   b. **Dependency direction:** Vocabulary packages must not depend on classes in
      `..infrastructure..` or `..api..`. Vocabulary defines ports; it never reaches out.

   c. **Commands/queries don't depend on infrastructure:** Classes in command/query
      packages (e.g., `..addtocatalog..`) must not depend on classes in
      `..infrastructure..`.

   d. **No `java.util.logging`:** All logging through SLF4J.

   e. **No field injection:** No field annotated with `@Autowired`.

   f. **Controllers in api packages:** Classes annotated with `@RestController` must
      reside in `..api..` packages.

   g. **Entities in infrastructure packages:** Classes annotated with `@Entity` must reside
      in `..infrastructure..` packages.

4. **Run the tests.** They should pass — if they don't, you have an architectural violation
   to fix.

**Hints:**
- `@AnalyzeClasses(packages = "tech.reactiv.ecommerce")` scans all modules on the test
  classpath
- Rules are `static final` fields, not test methods — ArchUnit caches the class scan
- For the dependency direction rules, `noClasses().that().resideInAPackage("..product..")
  .should().dependOnClassesThat().resideInAPackage("..infrastructure..")`
- Start with these rules. Add more as patterns emerge.
- Import from `com.tngtech.archunit.lang.syntax.ArchRuleDefinition`

**Watch out:** ArchUnit rules that are too strict early on will slow you down. The goal is
to prevent architectural decay, not to micromanage class placement.

---

## 6.7 — Observability

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
Span span = tracer.spanBuilder("AddToCatalogHandler.handle")
    .setAttribute("product.name", command.name())
    .startSpan();
try (Scope scope = span.makeCurrent()) {
    ProductId id = // ...
    span.setAttribute("product.id", id.value().toString());
    return id;
} catch (Exception e) {
    span.recordException(e);
    span.setStatus(StatusCode.ERROR, e.getMessage());
    throw e;
} finally {
    span.end();
}
```

**Wide events — the key practice:**

The value isn't in creating spans. It's in what you attach. A span for a command might
carry:
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

### Exercise 6.7a — Configure Logback for ecosystem output

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

### Exercise 6.7b — Add OpenTelemetry to command/query handlers

1. **Add the OpenTelemetry BOM** to the version catalog, then add dependencies to the
   catalog module:
   - `io.opentelemetry:opentelemetry-api` — `implementation`
   - `io.opentelemetry:opentelemetry-sdk` — `testImplementation` (SDK configured in tests
     for now)
   - `io.opentelemetry:opentelemetry-exporter-logging` — `testImplementation` (console
     output)
   - Only the BOM needs a version entry in the catalog. Individual artifacts use string
     coordinates with `platform()` resolving versions.

2. **Inject a `Tracer`** into command/query handlers (constructor parameter).

3. **Instrument `AddToCatalogHandler`** and `LookupProductHandler` with spans:
   - Span name: `"AddToCatalogHandler.handle"`
   - Attributes: `product.id`, `product.found`, `product.name`

4. **Write a test** that configures the SDK, creates the handler with a real tracer and
   fake repository, and runs an operation. Check the console for span output.

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
- Don't over-instrument. Command/query handlers and key domain decision points, not every method.
- The `Tracer` should be injected (constructor parameter), same pattern as `Clock`

---

## 6.8 — Integration Testing

### Read First

You now have all the hexagonal pieces:
- **Primary adapters** (`@RestController` in `adapter/in/api/`) — HTTP in/out
- **Command/query handlers** (`addtocatalog/`, `lookup/`, etc.) — business logic entry points
- **Domain** (`domain/`) — entities, value objects, ports
- **Secondary adapters** (`adapter/out/persistence/`) — database access

**Testing strategy:**

| What | Test style | What it proves |
|------|-----------|---------------|
| Domain value objects | Plain JUnit, no Spring | `Money`, `DateRange`, `Promotion` work |
| Command/query handlers | JUnit with fake adapters | Business logic works (your primary tests) |
| Primary adapter | `@WebMvcTest` with mocked handlers | HTTP routing, serialization, status codes |
| Secondary adapter | `@DataJpaTest` + Testcontainers | SQL, JPA mappings, query derivation |
| Adapter contract | Same tests on fake + real | Fake is a faithful substitute |
| End-to-end | `@SpringBootTest` + Testcontainers | Everything wires together |

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
        var command = new AddToCatalogCommand("Widget", "A fine widget", new BigDecimal("19.99"));
        var created = restTemplate.postForEntity("/api/products", command, ProductId.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        var retrieved = restTemplate.getForEntity(
            "/api/products/" + created.getBody().value(), Product.class);
        assertThat(retrieved.getBody().name()).isEqualTo("Widget");
    }
}
```

This starts the full application — web server, Spring context, real database — and tests
the entire request path from HTTP through command handler to database and back.

### Exercise 6.8a — Integration tests

1. **Write full integration tests** with `@SpringBootTest` and Testcontainers:
   - Create a product via POST, retrieve via GET
   - List products, filter by category
   - Update a product, verify changes persist
   - Soft-delete, verify it no longer appears in listings
   - Create a promotion, look up a product, verify effective price reflects the discount

2. **Verify the full test coverage:**
   - Domain: `Money`, `DateRange`, `Promotion` tests (no Spring)
   - Command/query handlers: tests with `InMemoryProductRepository` (no Spring)
   - Primary adapter: `@WebMvcTest` with mocked handlers
   - Secondary adapter: `@DataJpaTest` + Testcontainers
   - Contract: same tests on fake + real repository
   - Integration: `@SpringBootTest` + Testcontainers (end-to-end)

**Hints:**
- `TestRestTemplate` is auto-configured by `@SpringBootTest` with `RANDOM_PORT`
- Use `@DynamicPropertySource` to point Spring at the Testcontainer's PostgreSQL
- Integration tests are slow — focus on proving the wiring, not re-testing business logic

---

## 6.9 — Capstone

### Exercise 6.9a — Verify the full Catalog context

Run through this checklist:

1. **`./gradlew clean build`** — all modules compile, all tests pass
2. **CRUD API works** — create, read, list, filter, update, soft-delete products
3. **Promotions work** — create a promotion, look up a product with effective price,
   sealed `PromotionTarget` drives filtering, `Money` used for prices
4. **Hexagonal structure** — domain defines ports, adapters implement them, dependencies
   point inward
5. **Package-by-use-case** — each command/query owns its handler, internal classes are
   package-private
6. **Flyway migrations** run against Testcontainers PostgreSQL (including promotion table)
7. **Contract tests** pass on both fake and real repository implementations (Product and
   Promotion)
8. **ArchUnit rules** pass — vocabulary is clean, infrastructure isolated, dependency
   direction enforced
9. **Observability** — OTel spans appear with meaningful attributes, Logback output is
   readable
10. **Value objects** (`Money`, `DateRange`, `ProductId`, `PromotionId`) have test coverage
11. **Integration tests** prove the full request path works (including promotions)

If all of this passes, the Catalog context is complete and production-shaped.

---

## Review

After completing Phase 6, your e-commerce project should have:

- [ ] Spring Boot application in the `catalog` module with DI wired up
- [ ] Hexagonal architecture with package-by-use-case organization
- [ ] Command handlers (`AddToCatalogHandler`, `RepriceProductHandler`, `DiscontinueProductHandler`, `CreatePromotionHandler`)
- [ ] Query handlers (`LookupProductHandler` with effective price, `SearchCatalogHandler`)
- [ ] Vocabulary packages (`product/`, `category/`, `promotion/`) with ports (`ProductRepository`, `PromotionRepository`)
- [ ] Sealed `PromotionTarget` (`AllProducts`, `ByCategory`, `ByProducts`) — data only, filtering in repository adapters
- [ ] `Money` value object in `common` — `Product.price()` refactored from `BigDecimal` to `Money`
- [ ] `DateRange` value object in `common` — `Promotion.isActive(Clock)` delegates to `DateRange.contains()`
- [ ] REST entry point: `CatalogController` in `api/` (including `POST /api/promotions`)
- [ ] Product lookup returns effective price (original price with best active promotion applied)
- [ ] Database implementation: `PostgresProductRepository`, `PostgresPromotionRepository` in `infrastructure/persistence/`
- [ ] Full CRUD REST API for products with validation and error handling
- [ ] JPA entities (`ProductEntity`, `CategoryEntity`, `PromotionEntity`) mapped to domain objects at the boundary
- [ ] Flyway migrations for product, category, and promotion tables
- [ ] Testcontainers PostgreSQL in all database tests
- [ ] Contract tests proving fake and real repositories behave identically (Product and Promotion)
- [ ] ArchUnit rules enforcing dependency direction (vocabulary → nothing, infrastructure → vocabulary)
- [ ] Logback configured for ecosystem output, OTel spans for your own instrumentation
- [ ] Outside-in test pyramid: handlers (fakes) → adapters → contract → integration
- [ ] All tests passing with `./gradlew clean build`

**Consider:**
1. How does Spring's DI compare to Angular's? What's better, what's worse?
2. The entity-to-domain mapping at the adapter boundary — is it worth the overhead? What
   would break if you used JPA entities as your domain model?
3. The vocabulary/use-case/infrastructure split gives you real encapsulation via
   package-private. Did you find cases where it felt like overhead? Where did it pay off?
4. How does the `Clock` injection pattern compare to mocking `Date.now()` in TS?
5. What ADRs should you write for decisions you made in this phase?
