# Phase 0 — Testing Toolkit & Build Essentials

> **Goal:** By the end of this phase you can confidently write and run JUnit 5 tests with
> AssertJ assertions, organize them with `@Nested` and parameterized inputs, and apply
> Detroit-school TDD (fakes, not mocks) to drive design.

---

## 0.1 — Maven: Just Enough to Move

Maven is Java's equivalent of npm. The `pom.xml` is your `package.json`.

### Read & Understand

Open `pom.xml`. You'll see:

```xml
<groupId>org.example</groupId>     <!-- ≈ npm scope (@myorg) -->
<artifactId>CrashCourse</artifactId> <!-- ≈ package name -->
<version>1.0-SNAPSHOT</version>      <!-- ≈ "version" in package.json -->

<properties>
    <maven.compiler.source>25</maven.compiler.source>  <!-- Java version -->
    <maven.compiler.target>25</maven.compiler.target>
</properties>
```

There are no `<dependencies>` yet. That's your first task.

### Exercise 0.1a — Add JUnit 5 and AssertJ

Add these two dependencies to `pom.xml` inside a `<dependencies>` block:

1. **JUnit 5** (the test framework):
   - groupId: `org.junit.jupiter`
   - artifactId: `junit-jupiter`
   - version: `5.11.4`
   - scope: `test`

2. **AssertJ** (fluent assertions):
   - groupId: `org.assertj`
   - artifactId: `assertj-core`
   - version: `3.27.3`
   - scope: `test`

**Hints:**
- Dependencies go inside `<dependencies>...</dependencies>` before `</project>`
- Each dependency looks like:
  ```xml
  <dependency>
      <groupId>...</groupId>
      <artifactId>...</artifactId>
      <version>...</version>
      <scope>test</scope>    <!-- means: only available in src/test, not shipped in production -->
  </dependency>
  ```
- After saving, run `mvn compile` to verify it downloads correctly.

### Exercise 0.1b — Create the test directory

Maven expects test code in `src/test/java/` mirroring the main source tree. Create:

```
src/test/java/org/example/
```

This mirrors `src/main/java/org/example/` where `Main.java` lives.

**Hint:** `mkdir -p src/test/java/org/example`

### Key Maven Commands

Try each of these now (some will do nothing yet, that's fine):

| Command | What it does | npm equivalent |
|---------|-------------|----------------|
| `mvn compile` | Compiles `src/main/java` | `tsc` |
| `mvn test` | Compiles + runs all tests in `src/test/java` | `npm test` |
| `mvn test -Dtest=MyTest` | Runs a single test class | `jest MyTest` |
| `mvn test -Dtest="MyTest#myMethod"` | Runs a single test method | `jest -t "myMethod"` |
| `mvn clean` | Deletes `target/` (build output) | `rm -rf dist` |
| `mvn package` | Compile + test + build JAR | `npm run build` |

**Important:** Maven's surefire plugin (which runs tests) requires test classes to end in
`*Test.java` or `*Tests.java`. If your class is `MyStuff.java`, surefire won't find it.

---

## 0.2 — Your First Test

### Read First

A JUnit 5 test is just a class with methods annotated `@Test`. No magic.

The mapping from Jest:
```
Jest                          JUnit 5
─────────────────────────     ──────────────────────────────
describe('Calculator', ...)   class CalculatorTest { ... }
it('should add', ...)         @Test void shouldAdd() { ... }
expect(result).toBe(3)        assertThat(result).isEqualTo(3)   // AssertJ
beforeEach(...)               @BeforeEach void setUp() { ... }
```

Key imports you'll need:
```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.assertj.core.api.Assertions.assertThat;
```

### Exercise 0.2 — Write your first test class

Create `src/test/java/org/example/FirstTest.java` and write:

1. A test that adds two integers and asserts the result
2. A test that concatenates two strings and makes multiple assertions about the result
   (try `.isEqualTo()`, `.startsWith()`, `.contains()`, `.hasSize()`)
3. A test that creates a `boolean` and asserts it's `true`

Run with `mvn test` and make sure all 3 pass.

**Things to discover on your own:**
- What happens if you forget `@Test`? Does the method run?
- What happens if an assertion fails? Change an expected value and read the error message.
  (AssertJ's error messages are one of its best features.)
- What does `@DisplayName("my readable name")` do in the test output?
  **Note:** `@DisplayName` won't show up in `mvn test` by default — Maven's console output
  only shows class-level summaries. You'll see it in two places:
  - **IntelliJ:** right-click a test class → Run. The test runner panel shows display names
    in the tree view. This is where most Java developers run tests during development.
  - **Maven:** add a surefire plugin configuration to `pom.xml` to get verbose tree output:
    ```xml
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.5.2</version>
                <configuration>
                    <statelessTestsetReporter implementation="org.apache.maven.plugin.surefire.extensions.junit5.JUnit5Xml30StatelessReporter">
                        <usePhrasedTestCaseMethodName>true</usePhrasedTestCaseMethodName>
                        <usePhrasedTestSuiteClassName>true</usePhrasedTestSuiteClassName>
                    </statelessTestsetReporter>
                    <consoleOutputReporter implementation="org.apache.maven.plugin.surefire.extensions.junit5.JUnit5ConsoleOutputReporter">
                        <usePhrasedFileName>true</usePhrasedFileName>
                    </consoleOutputReporter>
                    <statelessTestsetInfoReporter implementation="org.apache.maven.plugin.surefire.extensions.junit5.JUnit5StatelessTestsetInfoTreeReporter">
                        <usePhrasedClassNameInRunning>true</usePhrasedClassNameInRunning>
                        <usePhrasedClassNameInTestCaseSummary>true</usePhrasedClassNameInTestCaseSummary>
                    </statelessTestsetInfoReporter>
                </configuration>
            </plugin>
        </plugins>
    </build>
    ```
- What happens if the method isn't `void`? What if it's `private`?

---

## 0.3 — Assertions Deep Dive

### Read First

AssertJ's power is that `assertThat()` returns a type-specific assertion object.
`assertThat(aString)` gives you string methods. `assertThat(aList)` gives you collection methods.
Type `.` after `assertThat(x)` in IntelliJ and browse what's available — that's the intended
way to learn the API.

### Exercise 0.3a — String assertions

Write a test class `StringAssertionsTest.java` with tests that explore:

- `.isEqualTo()`, `.isNotEqualTo()`
- `.startsWith()`, `.endsWith()`, `.contains()`, `.doesNotContain()`
- `.isBlank()`, `.isNotBlank()`, `.isEmpty()`, `.isNotEmpty()`
- `.hasSize()`
- `.isEqualToIgnoringCase()`
- `.containsPattern()` with a simple regex

**Hint:** Create a few string variables (a URL, an email, an empty string) and write
assertions about them. Try to use at least 8 different assertion methods.

### Exercise 0.3b — Number assertions

In the same or a new test class, explore:

- `.isEqualTo()`, `.isGreaterThan()`, `.isLessThan()`
- `.isBetween()`
- `.isPositive()`, `.isNegative()`, `.isZero()`

### Exercise 0.3c — Collection assertions

Java's `List.of("a", "b", "c")` creates an immutable list (≈ `Object.freeze(["a","b","c"])`).
`Map.of("key", "value")` creates an immutable map.

Write tests that explore:

- `assertThat(aList).hasSize()`, `.contains()`, `.containsExactly()`, `.doesNotContain()`
- `assertThat(aList).startsWith()`, `.endsWith()`
- `assertThat(aList).filteredOn(predicate).containsExactly(...)` — filter then assert
- `assertThat(aMap).containsKey()`, `.containsEntry()`, `.hasSize()`

You'll need these imports:
```java
import java.util.List;
import java.util.Map;
```

### Exercise 0.3d — Exception assertions

Write a test that verifies an exception is thrown. Use:

```java
import static org.assertj.core.api.Assertions.assertThatThrownBy;
```

The pattern:
```java
assertThatThrownBy(() -> {
    // code that should throw
})
    .isInstanceOf(SomeException.class)
    .hasMessageContaining("some text");
```

**Try these:**
1. Assert that `Integer.parseInt("not a number")` throws `NumberFormatException`
2. Assert that accessing `List.of(1, 2, 3).get(10)` throws `IndexOutOfBoundsException`
3. Assert the exception messages contain useful text

### Exercise 0.3e — `extracting()` (the killer feature)

This is AssertJ's best trick. Given a list of objects, extract a field from each and assert on the results.

1. Define a `record` inside your test class: `record Person(String name, int age) {}`
   (Records are covered properly in Phase 1, for now just know they're simple data classes)
2. Create a `List<Person>` with 3-4 people
3. Use `assertThat(people).extracting(Person::name)` to assert just the names
4. Use `.extracting(Person::name, Person::age)` to assert tuples

**Hint:** For tuples you'll need `import org.assertj.core.groups.Tuple;` and use
`Tuple.tuple("Alice", 30)` as expected values.

---

## 0.4 — Test Lifecycle & Organization

### Read First: What are annotations?

You'll see `@Something` everywhere in Java. They look like TypeScript decorators (`@Component`)
but work differently:

- **TS decorators** are functions that run at class creation time and can modify the class.
- **Java annotations** are metadata tags. They don't DO anything by themselves — they just
  mark the code. Some other piece of code (a framework, the compiler, a test runner) reads
  the annotations and decides what to do.

So when you write `@Test` on a method, you're not changing the method — you're putting a label
on it. JUnit's test runner scans for methods labelled `@Test` and calls them. `@BeforeEach`
is another label that tells JUnit "call this method before each `@Test` method."

This is why `@Test void shouldAdd()` is just a normal method — the annotation is metadata
that JUnit reads, not code that wraps the method. Annotations are covered properly in Phase 7,
but this mental model is enough for now: **annotations = labels that frameworks look for.**

### Lifecycle

JUnit 5 creates a **new instance** of your test class for every `@Test` method.
This means instance fields are fresh each time — even without `@BeforeEach`. But `@BeforeEach`
is still useful for shared setup.

```
@BeforeAll  → runs once before ALL tests (must be static)
@BeforeEach → runs before EACH test
@Test       → the test itself
@AfterEach  → runs after EACH test
@AfterAll   → runs once after ALL tests (must be static)
```

**@Nested:** groups tests into inner classes, like Jest's `describe()`. Each `@Nested` class
inherits the outer class's lifecycle. You can nest multiple levels deep.

### Exercise 0.4a — Lifecycle

Create `LifecycleTest.java`:

1. Add a `List<String>` instance field (use `new ArrayList<>()`)
2. In `@BeforeEach`, add `"setup"` to the list
3. Write two tests that each add something to the list, then assert the list contents
4. **Verify isolation:** prove that test 2 doesn't see what test 1 added.
   (In Jest, tests can accidentally leak state. In JUnit 5, they can't — each test gets
   a new object. But `@BeforeEach` still runs, so you'll see `"setup"` in each test.)

### Exercise 0.4b — @Nested

Create `NestedTest.java` with this structure:

```
class NestedTest {
    // @BeforeEach sets up some shared state

    @Nested class WhenEmpty {
        // tests about the empty state
    }

    @Nested class WhenPopulated {
        // @BeforeEach adds data

        @Nested class AndFiltered {
            // tests about the filtered state
        }
    }
}
```

Use any domain — a shopping cart, a to-do list, whatever. The point is to practice
the nesting and see how `@BeforeEach` chains from outer to inner.

### Exercise 0.4c — Parameterized Tests

Parameterized tests run the same logic with different inputs. ≈ Jest's `test.each`.

You'll need this import:
```java
import org.junit.jupiter.params.ParameterizedTest;
```

Write three parameterized tests:

1. **`@ValueSource`** — test that several strings are all non-empty:
   ```java
   @ParameterizedTest
   @ValueSource(strings = {"hello", "world", "java"})
   void shouldBeNonEmpty(String input) { ... }
   ```
   Import: `org.junit.jupiter.params.provider.ValueSource`

2. **`@CsvSource`** — test an addition function with multiple input/output pairs:
   ```java
   @ParameterizedTest
   @CsvSource({"1, 1, 2", "2, 3, 5", "0, 0, 0"})
   void shouldAdd(int a, int b, int expected) { ... }
   ```
   Import: `org.junit.jupiter.params.provider.CsvSource`

3. **`@MethodSource`** — for complex inputs that can't be expressed as simple CSV strings
   (e.g., objects, lists, or when you want readable variable names).

   The idea: you write a `static` method that returns a list of test cases, and JUnit calls
   your test method once per case. Each case is a set of arguments wrapped in `Arguments.of()`.

   `Stream<Arguments>` just means "a sequence of argument sets." Think of it like a typed
   array of tuples. `Stream.of(...)` creates one from individual items — similar to
   `[].of(...)` but lazily evaluated (covered properly in Phase 2).

   ```java
   @ParameterizedTest
   @MethodSource("testCases")   // must match the static method name below
   void shouldReverse(String input, String expected) { ... }

   // This method provides the test data. Each Arguments.of() becomes one test run.
   // The values are passed positionally to the test method parameters.
   static Stream<Arguments> testCases() {
       return Stream.of(
           Arguments.of("hello", "olleh"),   // 1st run: input="hello", expected="olleh"
           Arguments.of("Java", "avaJ"),     // 2nd run: input="Java",  expected="avaJ"
           Arguments.of("", "")              // 3rd run: input="",      expected=""
       );
   }
   ```

   Imports:
   ```java
   import org.junit.jupiter.params.provider.MethodSource;
   import org.junit.jupiter.params.provider.Arguments;
   import java.util.stream.Stream;
   ```

   **Hint:** `new StringBuilder(input).reverse().toString()` reverses a string in Java.

---

## 0.5 — Detroit-School TDD

This is the most important section of Phase 0. It's the approach we'll use for the
entire course.

### Read First: The Philosophy

**London school (mocks):** Replace dependencies with mock objects. Assert that the
right methods were called with the right arguments. "Did you call `repository.save()`?"

**Detroit school (fakes):** Replace dependencies with real, working in-memory implementations.
Assert observable behavior. "If I save and then load, do I get the right thing back?"

**Why Detroit for this course:**
- Mock-based tests break when you refactor internals. Fake-based tests don't.
- Fakes are real code — they force you to define clear interfaces.
- Writing fakes teaches you Java interfaces, generics, and collections naturally.
- It's the approach favored by Kent Beck (who invented TDD).

**The pattern:**

```
1. Define an INTERFACE for the dependency (e.g., BookRepository)
2. Write a FAKE — a real in-memory implementation (e.g., InMemoryBookRepository)
3. Inject the fake into the class under test via constructor
4. Test BEHAVIOR, not method calls
```

### Java Crash Course Aside: Fields & Constructors

Coming from TypeScript, you're used to `constructor(private name: string)` which declares
the field and assigns it in one shot. Java doesn't have that shorthand. Here's what you
need to know:

**Naming:** private fields use plain camelCase — no `_` prefix, no `m_` prefix:
```java
private String firstName;
private int count;
```

**Default values on fields — yes, this works:**
```java
private int count = 0;
private String name = "unknown";
private final List<String> items = new ArrayList<>();
```

**Constructor injection — write it out:**
```java
private final String name;
private final int age;

MyClass(String name, int age) {
    this.name = name;   // this. is needed because parameter shadows the field
    this.age = age;
}
```

**Best practices:**
- Mark fields `final` whenever possible (≈ `readonly`). Strongly encouraged in Java.
- Initialize in the constructor if the value comes from outside (dependency injection).
- Initialize at the declaration site if it's a sensible default (like `new ArrayList<>()`).
- `record` types eliminate this boilerplate entirely for data classes (Phase 1).
- Lombok's `@RequiredArgsConstructor` generates constructors for `final` fields (Phase 5).

### Exercise 0.5 — TDD a Book Lending Service

Build this from scratch using the red-green-refactor cycle. **Write the test first,
see it fail, then write just enough code to make it pass.**

This is the start of the library project that continues through the rest of the course.

**The domain:** A `LendingService` manages book loans. Members borrow and return books.
The repository stores who has which book; the service enforces the business rules.

**There are two things to test here, and they're tested separately:**

#### Part A — The Fake (repository contract tests)

The fake is test infrastructure, but it needs to work correctly or your service tests
are meaningless. Write a small test class that verifies the fake behaves as a real
repository should.

**Step 1 — Define the interface (in `src/main/java`):**

Create an interface `LoanRepository` with these methods:
- `void save(String memberId, String bookId)` — record that a member borrowed a book
- `void delete(String memberId, String bookId)` — record that a book was returned
- `List<String> findBooksByMember(String memberId)` — which books does this member have?
- `boolean isBookOnLoan(String bookId)` — is this book currently lent out?

**Step 2 — Write the fake (in `src/test/java`):**

Create `InMemoryLoanRepository implements LoanRepository`.
Choose whatever internal data structure makes sense to you.

**Step 3 — Test the fake:**

Create `InMemoryLoanRepositoryTest.java`. These tests verify the storage contract:

1. `shouldRecordALoan` — save a loan, verify the book appears in the member's list
2. `shouldReturnEmptyListForMemberWithNoLoans`
3. `shouldTrackMultipleLoansPerMember` — one member borrows 3 books
4. `shouldKnowWhenBookIsOnLoan` — after saving, `isBookOnLoan` returns true
5. `shouldRemoveLoanOnDelete` — after delete, the book is gone from the member's list
   and `isBookOnLoan` returns false

#### Part B — The Service (business logic tests)

Now TDD the service. The `LendingService` takes a `LoanRepository` in its constructor
and enforces business rules.

Create `LendingServiceTest.java`. In `@BeforeEach`, create an
`InMemoryLoanRepository` and pass it to a new `LendingService`.

Tests to write (one at a time, red → green → refactor):

1. `shouldBorrowBook` — borrow a book via the service, then check the fake directly
   to verify the loan was stored.
2. `shouldGetLoansForMember` — insert a loan directly into the fake, then ask the
   service for the member's loans. Verify it finds it.
3. `shouldReturnBook` — borrow then return. Verify it's no longer in the member's loans.
4. `shouldNotBorrowBookAlreadyOnLoan` — Alice borrows a book, then Bob tries to borrow
   the same book. Should throw.
5. `shouldNotReturnBookNotOnLoan` — returning a book nobody borrowed should throw.
6. `shouldEnforceBorrowingLimit` — a member can borrow at most N books.
   Borrowing beyond the limit should throw. Pass the limit to the service
   constructor (don't hardcode it) — use a small number like 3 so the test
   is concise.
**The point:** the fake tests verify that storage works. The service tests verify business
rules — borrowing limits, preventing double-loans, return validation. These are different
responsibilities. The service tests trust the fake (which has its own tests).

**Rules:**
- Write ONE test at a time. Don't look ahead.
- Each test must fail (RED) before you write production code.
- Write the minimum code to make it pass (GREEN).
- Then look for opportunities to clean up (REFACTOR).
- The service class and interface live in `src/main/java/org/example/`
- The test and fake live in `src/test/java/org/example/`

**Things to notice as you go:**
- The fake has its own tests — it's real code, not throwaway scaffolding.
- Service tests verify *business rules*, not storage mechanics.
- You never check "was `.save()` called?" — you check observable outcomes.
- The design emerged from the tests: the interface, the constructor injection, the
  separation of concerns — all driven by what the tests needed.
- Later (Phase 3), you'll write a real file-based `LoanRepository` and run Part A's
  tests against it too. If both pass, you know the fake is accurate.

---

## Checklist — Phase 0 Complete

When you can check all of these, you're ready for Phase 1:

- [ ] `pom.xml` has JUnit 5 and AssertJ as test-scoped dependencies
- [ ] `mvn test` runs and passes all your tests
- [ ] You've written tests using `assertThat()` with strings, numbers, lists, and maps
- [ ] You've used `assertThatThrownBy()` to test exceptions
- [ ] You've used `extracting()` on a list of objects
- [ ] You've used `@Nested` to group tests (at least 2 levels deep)
- [ ] You've written parameterized tests with `@ValueSource`, `@CsvSource`, and `@MethodSource`
- [ ] You've TDD'd a service + interface + fake from scratch (Exercise 0.5)
- [ ] You can explain why your tests verify behavior, not implementation
- [ ] You've debugged at least one test using IntelliJ's debugger (breakpoint + step through)
