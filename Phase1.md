# Phase 1 — Language Mechanics

> **Goal:** By the end of this phase you'll understand Java's type system, class model, and
> modern features (enums, sealed types, pattern matching, generics, Optional) — and your
> library domain model will use all of them.
>
> **Starting point:** You already have `LoanRepository`, `InMemoryLoanRepository`,
> `LendingService`, and `LendingServiceConfig` from Phase 0. Phase 1 extends this code.

---

## 1.1 — Equality, Identity & the Type System

This is the single biggest source of bugs for developers coming from TS/JS. In
JavaScript, `===` does what you expect for strings and numbers. In Java, `==` does
something fundamentally different for objects.

### Read First

**The rule:** `==` compares **identity** (are these the exact same object in memory?).
`.equals()` compares **value** (do these objects represent the same thing?).

For primitives (`int`, `boolean`, `double`, etc.), `==` compares values — this works
as you'd expect. But for objects (including `String`, `Integer`, and every class you
write), `==` checks whether two references point to the same object on the heap.

```
TS/JS                         Java
─────────────────────         ──────────────────────────────
=== (value equality)          .equals() (value equality)
                              == (reference identity — NOT what you want for objects)
```

**Autoboxing:** Java automatically converts between primitives and their boxed wrappers:
`int` ↔ `Integer`, `boolean` ↔ `Boolean`, `double` ↔ `Double`, etc. This is called
autoboxing (primitive → object) and unboxing (object → primitive).

The trap: Java caches `Integer` values from -128 to 127. So `Integer a = 127; Integer b = 127;`
gives you the SAME object (same reference), and `a == b` is `true`. But `Integer a = 128;
Integer b = 128;` gives you DIFFERENT objects, and `a == b` is `false`. The value is equal;
the identity is not.

**`.equals()` and `.hashCode()`:** In Phase 0 you used records, which auto-generate
`.equals()` and `.hashCode()` based on all fields. Regular classes do NOT — they inherit
from `Object`, where `.equals()` is the same as `==` (identity). If you want value
equality on a class, you must override both `.equals()` and `.hashCode()`.

The contract: if `a.equals(b)` is true, then `a.hashCode() == b.hashCode()` must also
be true. Breaking this contract breaks `HashMap`, `HashSet`, and anything that uses hashing.

**`null`:** Java has no `undefined`. `null` is the only absence value. Calling any method
on `null` throws `NullPointerException`. Unboxing `null` also crashes:
`Integer x = null; int y = x;` → `NullPointerException`.

### Exercise 1.1a — Exploration tests

Create `src/test/java/org/example/EqualityTest.java`. Write tests that demonstrate:

1. **String identity vs equality:**
   - Create two strings with the same content using `new String("hello")` for both.
     Assert that `==` is `false` but `.equals()` is `true`.
   - Create two string literals `"hello"` (not using `new`). Discover what `==` returns.
     (String literals are *interned* — the JVM reuses the same object.)

2. **Integer cache boundary:**
   - Create two `Integer` variables with value `127` (via autoboxing: `Integer a = 127;`).
     Assert that `==` is `true`.
   - Do the same with `128`. Assert that `==` is `false`.
   - Assert that `.equals()` is `true` in both cases.

3. **Null unboxing:**
   - Create `Integer x = null;`. Assert that attempting `int y = x;` throws
     `NullPointerException`. (Use `assertThatThrownBy`.)

4. **Record equality vs class equality:**
   - Create two `LendingServiceConfig` records with the same value. Assert `.equals()` is `true`.
   - Create two `LendingService` instances with the same arguments. Discover what `.equals()` returns.
     (It's `false` — classes without custom `.equals()` use identity.)

### Exercise 1.1b — Value objects for the library domain

Raw `String` IDs are error-prone — nothing stops you from passing a memberId where a
bookId is expected. Typed value objects prevent this at compile time.

Create a `BookId` record (`src/main/java/org/example/`, single `String value` field).
Tests in `BookIdTest.java`:

- Two instances with the same value are `.equals()`
- Two instances with different values are not
- `.toString()` includes the value
- Blank value is rejected
- Null value is rejected

Repeat for `MemberId` — same pattern, fewer tests needed.

Then **refactor `LoanRepository`** to use `BookId` and `MemberId` instead of raw `String`.
Update `InMemoryLoanRepository`, `LendingService`, and all tests. Let the compiler guide
you. All existing tests should still pass.

**Hint:** A compact constructor on a record looks like:
```java
public record BookId(String value) {
    public BookId {
        // no parentheses — this is the compact form
        // 'value' is already assigned, you can validate it
    }
}
```

---

## 1.2 — Strings, Text & Formatting

### Read First

Java strings are **immutable**. Every method that "modifies" a string returns a new one.
There's no equivalent of TS template literals (`${var}`) — Java uses `.formatted()` or
`String.format()` instead.

**Key string methods:** `.substring(start, end)`, `.contains()`, `.split()`, `.strip()`
(≈ `.trim()` but Unicode-aware), `.startsWith()`, `.endsWith()`, `.indexOf()`,
`.replace()`, `.toUpperCase()`, `.toLowerCase()`, `.isBlank()`, `.isEmpty()`.

**Text blocks** (multi-line strings):
```java
String json = """
        {
            "title": "Effective Java",
            "author": "Joshua Bloch"
        }
        """;
```
The indentation is relative to the closing `"""`. No interpolation — just a multi-line literal.

**Formatting:**
```java
"Hello %s, you have %d books".formatted(name, count)
```
Common format specifiers: `%s` (string), `%d` (integer), `%f` (float), `%n` (newline).

**`StringBuilder`:** String concatenation with `+` in a loop creates a new `String` object
every iteration. For building strings in loops, use `StringBuilder`:
```java
var sb = new StringBuilder();
for (...) {
    sb.append("something");
}
String result = sb.toString();
```
For simple one-line concatenation, `+` is fine.

### Exercise 1.2a — String exploration

Create `StringTest.java`. Write tests that explore:

1. Prove that strings are immutable: call `.toUpperCase()` on a string, then assert
   the original is unchanged.
2. Use `.split()` to break `"java,typescript,csharp"` into an array. Assert the
   array length and contents. (Java arrays use `assertThat(array).containsExactly(...)`.)
3. Use `.strip()` on a string with leading/trailing whitespace. Assert the result.
4. Use a text block to define a multi-line string. Assert it `.contains()` expected lines.
5. Use `.formatted()` to build a string with variables. Assert the result.

### Exercise 1.2b — Book record with validation

Create a `Book` record with fields `BookId id`, `String title`, `String author`.
Tests in `BookTest.java`:

- Blank title is rejected
- Null title is rejected
- Title of exactly 200 characters is accepted
- Title of 201 characters is rejected
- Blank author is rejected
- Null author is rejected

Use `@Nested` classes to group title tests and author tests (like Jest `describe` blocks).

**Hint:** `String.isBlank()` returns true for whitespace-only strings but throws
on actual `null`. Check for `null` separately, or use: `title == null || title.isBlank()`.

### Crash Course Aside: Objects Own Their Behaviour

In TS/Angular you'll often see a pattern like `FormatterService.format(data)` — a
stateless service operating on passive data. Yegor Bugayenko argues (and this course
agrees) that this is procedural programming wearing an OOP hat.

Instead: objects should encapsulate their own behaviour. A `LoanReceipt` knows how to
represent itself — you don't hand its data to a separate `LoanReceiptFormatter`. The
object IS the thing, and it can speak for itself.

This means: prefer `receipt.asText()` over `formatter.format(receipt)`. The data and the
behaviour that operates on it live together.

### Exercise 1.2c — Loan receipt object

The receipt IS the object — not data acted on by a separate formatter.

Create a `LoanReceipt` class (constructor takes `MemberId` and `List<BookId>`, method
`String asText()`). Tests in `LoanReceiptTest.java`:

- Displays the member ID in its text output
- Displays the total book count (e.g. "Books on loan: 2")
- Lists each book ID
- Rejects an empty book list

Example output for a complete receipt:
```
Loan Receipt
Member: M-001
Books on loan: 3
- B-001
- B-002
- B-003
```

**Hint:** `String.join()` can join a list with a delimiter. You'll need to convert the
`BookId` list to strings first — you can use a loop or (if you want to peek ahead at
Phase 2) `.stream().map()`.

---

## 1.3 — Classes: What Phase 0 Skipped

### Read First

You already wrote `LendingService` with `private final` fields and constructor injection.
That was correct style. This section covers what you used instinctively but didn't examine.

**Access modifiers:**

| Modifier | Class | Package | Subclass | World |
|----------|-------|---------|----------|-------|
| `public` | yes | yes | yes | yes |
| `protected` | yes | yes | yes | no |
| *(none — package-private)* | yes | yes | no | no |
| `private` | yes | no | no | no |

**Package-private** (no keyword) is the default and it's a deliberate design tool. Your
tests in `src/test/java/org/example/` can access package-private members in
`src/main/java/org/example/` because they share the same package. This is by design.

**`final`:** You've used it on fields. It also applies to:
- **Methods:** `final` methods can't be overridden by subclasses.
- **Classes:** `final` classes can't be extended. `String` is final. Records are implicitly final.

**Method overloading:** Java has no default parameter values. Instead, you define
multiple methods with the same name but different parameter lists:
```java
void borrow(BookId bookId) { ... }
void borrow(BookId bookId, int durationDays) { ... }
```
The compiler picks the right one based on the arguments you pass.

**`static`:** Class-level members. No instance needed. Common for factory methods:
```java
public static Book of(String title, String author) {
    return new Book(BookId.generate(), title, author);
}
```

**`@Override`:** Tells the compiler "I intend to override a superclass/interface method."
If you misspell the method name or get the signature wrong, the compiler catches it.
Always use it.

**Nominal typing:** Two classes with identical fields and methods are NOT interchangeable.
Java doesn't care about structure — only the declared type matters. This is the opposite of
TypeScript's structural typing.

### Exercise 1.3a — Access modifiers exploration

Create `AccessModifierTest.java`:

1. Create a class `Library` in `src/main/java/org/example/` with:
   - A `public` method
   - A `private` method
   - A package-private method (no modifier)
   - A `static` factory method
2. In your test (same package), verify which methods you can call.
3. Try creating a test in a **different** package (e.g., `org.example.other`) and see
   which methods are still accessible. (You'll need to create the directory.)

### Exercise 1.3b — Method overloading and static factory

Add static factory methods to `Book`. Tests in `BookTest.java`:

- `Book.of("Effective Java", "Joshua Bloch")` creates a book with a non-null auto-generated ID
- Two calls to `Book.of(...)` produce different IDs
- `Book.of(someBookId, "Effective Java", "Joshua Bloch")` creates a book with the given ID
  (overloaded factory method)

**Hint:** For generating IDs, `java.util.UUID.randomUUID().toString()` gives you a
unique string. Import `java.util.UUID`.

### Exercise 1.3c — Nominal typing

Write a test that demonstrates nominal typing:

1. Create two record types with identical structure: `record Celsius(double value) {}`
   and `record Fahrenheit(double value) {}`.
2. Write a method that takes `Celsius` — try passing a `Fahrenheit`. It won't compile.
3. Contrast this with TS, where `{ value: number }` would be interchangeable.

This is a quick one — just to cement the mental model.

---

## 1.4 — Interfaces & Abstract Classes: Beyond Basics

### Read First

You already defined `LoanRepository` and implemented it. This section covers what
interfaces and abstract classes can do beyond simple contracts.

**`default` methods:** Interfaces can provide method implementations. All implementors
get the behaviour for free, but can override it:
```java
interface Searchable {
    String getSearchableText();

    default boolean matches(String query) {
        return getSearchableText().toLowerCase().contains(query.toLowerCase());
    }
}
```
This is Java's limited form of mixins. A class can implement multiple interfaces with
default methods — if two defaults conflict, the class must resolve the ambiguity.

**`static` methods on interfaces:** Utility methods that belong to the interface concept:
```java
interface LoanRepository {
    static LoanRepository inMemory() {
        return new InMemoryLoanRepository();
    }
}
```

**Abstract classes:** Midpoint between interface and class. Can have constructors, fields,
and both abstract and concrete methods. Use when you need shared state or constructor logic
that interfaces can't provide.

```java
abstract class BaseRepository {
    private int operationCount = 0;

    protected void trackOperation() { operationCount++; }
    public int getOperationCount() { return operationCount; }

    abstract void save(String id);  // subclasses must implement
}
```

**When to use which:**
- **Interface** — the default choice. Use for contracts, capabilities, and roles.
- **Abstract class** — when you need shared state (fields) or constructor logic across
  implementations. A class can only extend one abstract class but implement many interfaces.

**`protected`:** Visible to subclasses and to other classes in the same package. You'll
use it in abstract classes to expose methods to subclasses without making them public.

### Exercise 1.4a — Default methods

Make `Book` searchable. Tests in `SearchableTest.java`:

- A book matches a query that appears in its title
- A book matches a query that appears in its author
- Matching is case-insensitive
- A book does not match an unrelated query

Once green, **refactor:** extract the matching logic into a `Searchable` interface with
a `default` method. The interface declares `String getSearchableText()` (abstract) and
`default boolean matches(String query)` (does the case-insensitive search). `Book`
implements `getSearchableText()`. All tests should still pass.

### Exercise 1.4b — Abstract class

Add save-counting to `InMemoryLoanRepository`. Test in `InMemoryLoanRepositoryTest.java`:

- Save three loans, then assert `getSaveCount()` returns 3

**Refactor** to an abstract class `AuditedRepository` with a `private int saveCount`
field, a `protected recordSave()` method, and a `public getSaveCount()` method. Make
`InMemoryLoanRepository` extend it. All existing tests should still pass.

**Note:** This introduces a design tension — `InMemoryLoanRepository` now both
`implements LoanRepository` AND `extends AuditedRepository`. Java allows this.
Think about whether this is a good design. (There's no right answer at this point —
it's worth noticing the tradeoff.)

### Exercise 1.4c — Multiple interfaces

Create a `Displayable` interface with a `String display()` method. Make `Book`
implement both `Searchable` and `Displayable`:

- `book.display()` returns a human-readable string containing the title and author
- A single `Book` is accessible through both interface types

---

## 1.5 — Enums & Records in Depth

### Read First: Enums

Java enums are nothing like TypeScript enums. TS enums are just named constants
(`enum Color { Red, Green }`). Java enums are **full classes** — each constant is a
singleton instance that can have fields, constructors, and methods.

```java
public enum LoanStatus {
    ACTIVE, OVERDUE, RETURNED
}
```

That's the simplest form. But they can do much more:

```java
public enum BookGenre {
    FICTION("Fiction", 21),
    NON_FICTION("Non-Fiction", 14),
    REFERENCE("Reference", 7);

    private final String displayName;
    private final int maxLoanDays;

    BookGenre(String displayName, int maxLoanDays) {
        this.displayName = displayName;
        this.maxLoanDays = maxLoanDays;
    }

    public String getDisplayName() { return displayName; }
    public int getMaxLoanDays() { return maxLoanDays; }
}
```

Each constant calls the constructor with its own arguments. The constructor is implicitly
private — you can never `new BookGenre(...)`.

**Enums with behaviour:** each constant can override a method:
```java
public enum LoanStatus {
    ACTIVE {
        @Override public boolean canTransitionTo(LoanStatus target) {
            return target == OVERDUE || target == RETURNED;
        }
    },
    OVERDUE {
        @Override public boolean canTransitionTo(LoanStatus target) {
            return target == RETURNED;
        }
    },
    RETURNED {
        @Override public boolean canTransitionTo(LoanStatus target) {
            return false;  // terminal state
        }
    };

    public abstract boolean canTransitionTo(LoanStatus target);
}
```

**`EnumSet` and `EnumMap`:** Specialised, high-performance collections for enums. Use
them instead of `HashSet<MyEnum>` or `HashMap<MyEnum, ...>`.

### Read First: Records — Beyond Phase 0

You've used records for simple data (`LendingServiceConfig`, `Person`, `BookId`, `MemberId`).
Here's what you haven't seen:

**Compact constructors** validate and normalise input:
```java
public record BookId(String value) {
    public BookId {  // no parentheses — compact form
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("BookId must not be blank");
        }
        value = value.strip();  // normalisation — reassigns the field
    }
}
```

**Custom methods:** Records can have additional methods:
```java
public record Book(BookId id, String title, String author) {
    public String displayTitle() {
        return "%s by %s".formatted(title, author);
    }
}
```

**Records are final** — you can't extend them. They implicitly extend `java.lang.Record`.

### Exercise 1.5a — Loan with status

Create a `Loan` class that tracks the status of a single loan. Constructor takes
`MemberId`, `BookId`. A new loan starts as `ACTIVE`. The `LoanStatus` enum (`ACTIVE`,
`OVERDUE`, `RETURNED`) should emerge as an implementation detail, not be tested in
isolation. Tests in `LoanTest.java`:

- A new loan is active
- An active loan can be returned
- An active loan can be marked overdue
- An overdue loan can be returned
- A returned loan cannot be returned again
- A returned loan cannot be marked overdue
- An overdue loan cannot be marked overdue again

Start with the transition logic wherever feels natural (probably `Loan` itself). Once
all tests are green, **refactor:** move the transition rules into the `LoanStatus` enum
by adding an abstract `canTransitionTo(LoanStatus target)` method with per-constant
implementations (see the Read First section above for the syntax). `Loan` delegates to the
enum instead of containing the rules directly. All tests should still pass.

### Exercise 1.5b — Book genre and loan duration

Add a `BookGenre genre` field to `Book` (fix all existing construction sites). The
`BookGenre` enum (`FICTION`, `NON_FICTION`, `REFERENCE`) with `displayName` and
`maxLoanDays` fields should emerge from making these tests pass in `BookTest.java`:

- A fiction book is a long loan (`isLongLoan()` returns true when `maxLoanDays > 14`)
- A reference book is not a long loan
- A book exposes its genre's display name (e.g. `"Fiction"`)

---

## 1.6 — Sealed Types & Pattern Matching

### Read First

TypeScript has discriminated unions:
```typescript
type LibraryEvent = { type: 'bookAdded', title: string }
                  | { type: 'bookBorrowed', memberId: string, bookId: string }
```

Java's sealed types serve the same purpose, but enforced at the class level:
```java
public sealed interface LibraryEvent permits BookAdded, BookBorrowed, BookReturned {}
public record BookAdded(BookId bookId, String title) implements LibraryEvent {}
public record BookBorrowed(MemberId memberId, BookId bookId) implements LibraryEvent {}
public record BookReturned(MemberId memberId, BookId bookId) implements LibraryEvent {}
```

`sealed` means: only the types listed in `permits` can implement this interface. The compiler
knows the complete set of subtypes, which enables exhaustive `switch`:

```java
String serialize(LibraryEvent event) {
    return switch (event) {
        case BookAdded a -> "BOOK_ADDED|" + a.bookId() + "|" + a.title();
        case BookBorrowed b -> "BOOK_BORROWED|" + b.memberId() + "|" + b.bookId();
        case BookReturned r -> "BOOK_RETURNED|" + r.memberId() + "|" + r.bookId();
        // no default needed — the compiler knows this is exhaustive
    };
}
```

If you add a new event type to the `permits` list, every `switch` on `LibraryEvent` that
isn't exhaustive will fail to compile. The compiler forces you to handle it.

**Record destructuring in patterns:**
```java
case BookAdded(var bookId, var title) -> "Added: " + title;
```

**`when` guards:**
```java
case BookBorrowed b when b.memberId().equals(currentUser) -> "You borrowed: " + b.bookId();
```

**Where does exhaustive switch belong?** Each domain object should own its own behaviour
— a `BookAdded` event knows how to describe itself via `asText()`, not via an external
`EventProcessor.describe()` switch. So where *does* exhaustive switch shine?

At **infrastructure boundaries**: serialisation, deserialisation, routing, factory methods.
These are places where you're translating between your domain and an external format or
protocol. The switch belongs in the code that maps between worlds — not in code that could
be a polymorphic method on the object itself.

### Exercise 1.6a — Event serialiser/deserialiser

This exercise drives the creation of the event types themselves. You'll need:

- A `sealed interface LibraryEvent` with a `permits` clause
- `BookAdded(BookId bookId, String title, String author)`
- `BookBorrowed(MemberId memberId, BookId bookId)`
- `BookReturned(MemberId memberId, BookId bookId)`

Create an `EventSerializer` with a `String serialize(LibraryEvent event)` method that
converts events to a pipe-delimited string format using an exhaustive switch — no
`default` needed. Create an `EventDeserializer` with a `LibraryEvent deserialize(String raw)`
method that parses them back. Tests in `EventSerializerTest.java`:

Serialiser:
- `BookAdded` serialises to `BOOK_ADDED|bookId|title|author`
- `BookBorrowed` serialises to `BOOK_BORROWED|memberId|bookId`
- `BookReturned` serialises to `BOOK_RETURNED|memberId|bookId`

Deserialiser:
- `BOOK_ADDED|...` string deserialises to a `BookAdded` record with correct fields
- `BOOK_BORROWED|...` string deserialises to a `BookBorrowed` record
- Round-trip: serialise then deserialise produces an equal event

**Hints:**
- `String.split("\\|")` splits on pipe (pipe is a regex special char, needs escaping)
- `String.join("|", parts)` joins with pipe
- The deserialiser can switch on the first element of the split array (the tag string)

### Exercise 1.6b — Events own their description

Each event should have a `String asText()` method — add it to the `LibraryEvent` interface.
Tests in `LibraryEventTest.java`:

- `BookAdded` asText contains the title
- `BookBorrowed` asText contains the member ID and book ID
- `BookReturned` asText contains the member ID and book ID

### Exercise 1.6c — Compiler-enforced exhaustiveness

Add `MemberRegistered(MemberId memberId, String name)` to the sealed interface's `permits`
list. **Don't** update the serialiser or deserialiser yet — try compiling. See what the
compiler tells you. Then add the missing cases and tests.

---

## 1.7 — Generics: Writing Your Own

### Read First

You've consumed generics since Phase 0: `List<String>`, `Map<String, List<String>>`,
`Optional<T>`. Now you write your own.

**Generic classes:**
```java
public class Box<T> {
    private final T value;
    Box(T value) { this.value = value; }
    T getValue() { return value; }
}
```
`T` is a type parameter — it's a placeholder that the caller fills in: `Box<String>`,
`Box<Integer>`, etc.

**Generic methods:**
```java
public static <T> List<T> repeat(T item, int times) { ... }
```
The `<T>` before the return type declares the type parameter for this method.

**Type erasure:** Java generics are a compile-time feature only. At runtime, `Box<String>`
and `Box<Integer>` are both just `Box`. The compiler checks types, then erases them.
Consequences:
- You can't do `new T()` — the JVM doesn't know what `T` is at runtime.
- You can't do `instanceof T` — same reason.
- You can't do `new T[]` — arrays need the type at runtime.

**Bounded types:** Restrict what `T` can be:
```java
<T extends Comparable<T>>  // T must implement Comparable
```

### Exercise 1.7a — Generic Result type

This is similar to TS's `Result<T, E>`. Create a `sealed interface Result<T>` with
`record Success<T>(T value)` and `record Failure<T>(String message)`. Tests in
`ResultTest.java`:

- `Success.isSuccess()` is true, `Failure.isSuccess()` is false
- `Success.getOrElse(default)` returns the value; `Failure` returns the default
- `Success.map(fn)` transforms the value; `Failure.map(fn)` passes the failure through
  unchanged (`import java.util.function.Function;`)

**Hints:**
- You'll need to think about how `Success<T>` and `Failure<T>` provide the methods
  declared on `Result<T>`. Either make them `default` methods on the sealed interface
  using pattern matching on `this`, or declare them `abstract` and implement in each record.
- `Function<T, U>` is Java's built-in functional interface: takes `T`, returns `U`.
  Apply it with `.apply(value)`.

### Exercise 1.7b — Type erasure exploration

Write tests that demonstrate type erasure:

1. Create a `Box<String>` and a `Box<Integer>`. Assert that `.getClass()` returns the
   same class for both. (Because at runtime, they're both just `Box`.)
2. Try writing a method that does `if (value instanceof T)` — it won't compile.
   Comment it out and add a note about why.

### Exercise 1.7c — Bounded types

Write a generic method `<T extends Comparable<T>> T max(T a, T b)` (put it in the test
class as a `private static` helper). Tests in `BoundedTypeTest.java`:

- `max(3, 7)` returns `7`
- `max("apple", "banana")` returns `"banana"`
- `max(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31))` returns the later date
  (import `java.time.LocalDate`)
- Try calling `max` with a type that doesn't implement `Comparable` — see the compiler error

---

## 1.8 — Null Safety & Optional

### Read First

TypeScript has `?` for optional properties and `??` for nullish coalescing. Java has neither
in the language. `Optional<T>` is the standard library's answer, but it's intentionally
limited.

**When to use `Optional<T>`:**
- **YES:** return types where absence is a normal outcome (`findById` returns `Optional<Book>`)
- **NO:** fields, method parameters, collections (use empty collections instead)

**Key methods:**

| Method | What it does | TS equivalent |
|--------|-------------|---------------|
| `Optional.of(value)` | Wraps a non-null value | — |
| `Optional.empty()` | Empty optional | `undefined` / `null` |
| `Optional.ofNullable(x)` | Wraps x, or empty if x is null | — |
| `.isPresent()` | Is there a value? | `!= null` |
| `.orElse(default)` | Get value or default | `?? default` |
| `.orElseThrow()` | Get value or throw | `!` (non-null assertion) |
| `.map(fn)` | Transform if present | `?.` chaining |
| `.flatMap(fn)` | Transform if present, fn returns Optional | nested `?.` |
| `.filter(predicate)` | Keep value only if predicate passes | — |
| `.ifPresent(consumer)` | Run side-effect if present | `if (x != null)` |

**The anti-patterns:**
```java
// DON'T — defeats the purpose
if (optional.isPresent()) {
    return optional.get();
}

// DO — use the API
return optional.orElse(defaultValue);
return optional.orElseThrow();
return optional.map(v -> transform(v));
```

### Exercise 1.8a — Optional exploration

Create `OptionalTest.java`:

1. Create an `Optional.of("hello")` and an `Optional.empty()`. Assert `.isPresent()`.
2. Use `.map()` to transform a present optional. Assert the transformed value.
3. Use `.map()` on an empty optional. Assert it's still empty (map doesn't crash on empty).
4. Use `.orElse()` to provide a default for an empty optional.
5. Use `.orElseThrow()` on an empty optional. Assert it throws.
6. Use `.filter()` to keep or discard a value. Test both paths.
7. Chain `.map().filter().orElse()` in a single expression.

### Exercise 1.8b — Optional in the repository

Before you start — a design note:

**When to return `Optional` vs empty collection:**
- "Does this member have any books?" → return `List<BookId>` (empty list = no books)
- "Is this specific book on loan?" → return `Optional<MemberId>` (the borrower, or empty)

Add `Optional<MemberId> findBorrower(BookId bookId)` to `LoanRepository`. Contract tests
in `InMemoryLoanRepositoryTest.java`:

- Book on loan returns the borrower
- Book not on loan returns `Optional.empty()`

Then refactor `LendingService.returnBook` to use `findBorrower` — verify the member
returning the book is the actual borrower. Existing tests should still pass.

### Exercise 1.8c — Optional chaining

Write a test that demonstrates chaining:

1. Given a `BookId`, find the borrower (`Optional<MemberId>`), then look up that borrower's
   total loan count. If the book isn't on loan, return 0.
2. Express this as a single chain: `.map().map().orElse()`.

---

## 1.9 — Capstone: Evolve the Library Domain

This is not a separate exercise — it's a checkpoint. If you've done the domain exercises
in each section, your library code should now have:

- [ ] `BookId` and `MemberId` value records with validation (1.1)
- [ ] `Book` record with title/author validation and `BookGenre` (1.2, 1.5)
- [ ] `LoanStatus` enum with state machine transitions (1.5)
- [ ] `Searchable` interface with default method on `Book` (1.4)
- [ ] `LibraryEvent` sealed hierarchy with `asText()` and serialiser/deserialiser (1.6)
- [ ] `Result<T>` generic type (1.7)
- [ ] `Optional`-returning repository methods (1.8)
- [ ] `LendingService` refactored to use typed IDs and `Optional` lookups (1.1, 1.8)
- [ ] All tests passing

### Review exercise

Look at your Phase 0 code vs your Phase 1 code. Consider:

1. What would happen in TS if you accidentally swapped a `memberId` and a `bookId`?
   What happens in Java now?
2. What happens if you add a new `LibraryEvent` type but forget to handle it somewhere?
3. How does `Result<T>` compare to throwing exceptions? When would you prefer each?
4. Where did you use `Optional` and where did you use empty collections? Could you
   articulate the rule to someone else?

If you can answer these, you understand Java's type system well enough to be productive.
Move on to Phase 2.
