# Phase 3 — Error Handling, I/O & Serialization

> **Goal:** By the end of this phase you'll have a `FileLoanRepository` that persists loan
> data as JSON files — a real, file-backed implementation of the `LoanRepository` interface
> you already have. Getting there requires exception handling, `java.nio` file I/O, and
> Jackson serialization. All three sub-topics serve that one domain goal.
>
> **Starting point:** You have the full Phase 1–2 domain: `Book` (class with private
> constructor and static factories), `BookId`, `MemberId`, `BookGenre`, `Catalog`,
> `Shelf`, `LoanRepository` (interface), `InMemoryLoanRepository` (test fake),
> `LoanReporter`, `LendingService`, `LibraryEvent` sealed hierarchy (`BookAdded`,
> `BookBorrowed`, `BookReturned`), `EventSerializer` (manual pipe-delimited
> serialization), `Result<T>`, `Searchable`, `Displayable`.

---

## 3.1 — Exception Handling

### Read First

You've already thrown exceptions — `IllegalArgumentException` in `Book`'s constructor,
`IllegalStateException` in `LendingService`. You've caught them in tests with
`assertThrows` and `assertThatThrownBy`. This section fills in the model.

**Java's exception hierarchy:**
```
Throwable
├── Error (JVM problems — OutOfMemoryError, StackOverflowError. Don't catch these.)
└── Exception
    ├── RuntimeException (unchecked — compiler doesn't care)
    │   ├── IllegalArgumentException
    │   ├── IllegalStateException
    │   ├── NullPointerException
    │   ├── UnsupportedOperationException
    │   └── ...your domain exceptions
    └── IOException, ParseException, ... (checked — compiler forces handling)
```

**Checked vs unchecked — the big concept:**

- **Checked exceptions** (`Exception` subclasses that aren't `RuntimeException`): the
  compiler forces every caller to either `catch` them or declare `throws` on their own
  method signature. They propagate upward through every method in the call chain. Common
  examples: `IOException`, `FileNotFoundException`, `ParseException`.

- **Unchecked exceptions** (`RuntimeException` subclasses): no compiler enforcement.
  They fly up the stack silently until something catches them or the program crashes.
  You've been throwing these all along.

**The TS/C# comparison:** TypeScript and C# have no checked exceptions at all — everything
is unchecked. Java is the only mainstream language that enforces exception handling at
compile time. This is deliberate: I/O operations genuinely can fail (disk full, file
locked, network timeout), and the compiler forces you to acknowledge that.

**When to use which:**
- **Unchecked** for domain logic violations (`IllegalArgumentException`,
  `IllegalStateException`) and programming errors. These represent bugs or contract
  violations — the caller can't meaningfully recover.
- **Checked** for recoverable external failures (file not found, network error, parse
  failure). The caller *can* do something: retry, use a fallback, report to the user.
- **`Result<T>`** for expected domain outcomes with two valid paths (success/failure).
  You already use this for lending operations. Exceptions are for *unexpected* failures.

**Try-with-resources** (≈ C# `using`):
```java
// The reader is automatically closed when the block exits — even on exception.
try (var reader = Files.newBufferedReader(path)) {
    String line = reader.readLine();
    // ...
}
// reader.close() called automatically here
```

Any class implementing `AutoCloseable` works with try-with-resources. You'll use this
constantly in 3.2 for file handles and streams.

**Exception chaining — preserving the root cause:**
```java
try {
    var data = Files.readString(path);
} catch (IOException e) {
    // Wrap in unchecked, but keep the original exception as the cause
    throw new UncheckedIOException("Failed to read loan file: " + path, e);
}
```

Always pass the original exception as the `cause` parameter. Without it, you lose the
stack trace that tells you *why* the I/O failed. This is the Java equivalent of TS's
`new Error("msg", { cause: originalError })`.

**Checked exceptions inside lambdas — the real gotcha:**

This doesn't compile:
```java
paths.stream()
    .map(path -> Files.readString(path))  // COMPILE ERROR — readString throws IOException
    .toList();
```

`Function<T, R>` doesn't declare `throws`. The compiler won't let you throw a checked
exception from a lambda that doesn't expect one. You have three options:

1. **Wrap in a try-catch inside the lambda:**
   ```java
   paths.stream()
       .map(path -> {
           try {
               return Files.readString(path);
           } catch (IOException e) {
               throw new UncheckedIOException(e);
           }
       })
       .toList();
   ```

2. **Extract to a helper method** that wraps the checked exception:
   ```java
   private String readFile(Path path) {
       try {
           return Files.readString(path);
       } catch (IOException e) {
           throw new UncheckedIOException(e);
       }
   }

   // Then:
   paths.stream().map(this::readFile).toList();
   ```

3. **Avoid the stream entirely** — use a for-loop when the I/O-in-lambda gets ugly.

Option 2 is the most common in practice. You'll use it in the capstone.

**Watch out:** Checked exceptions feel like friction after TS/C#. They exist because I/O
genuinely fails in production. The compiler is making you think about failure paths you'd
otherwise forget. Embrace the annoyance — it'll save you from silent failures.

### Exercise 3.1a — Custom domain exceptions

Create `src/main/java/org/example/loan/LoanPersistenceException.java` — an unchecked
exception for loan persistence failures. It should:

- Extend `RuntimeException`
- Have constructors for `(String message)` and `(String message, Throwable cause)`

Tests in `src/test/java/org/example/loan/LoanPersistenceExceptionTest.java`:

- Throwing with a message produces the expected message
- Throwing with a message and cause preserves the cause

Keep the tests simple — just verify the exception carries the right information. Don't
test the message text itself, just that the exception is throwable and chainable.

**Hint:** This is deliberately simple. The exception exists so you have something
meaningful to throw in the capstone instead of raw `UncheckedIOException`. Two constructors,
both calling `super(...)`.

### Exercise 3.1b — Checked exceptions in streams

Create `src/test/java/org/example/CheckedExceptionTest.java`. This is a short exploration
exercise to experience the compiler friction firsthand.

1. Write a method `readFileContent(Path path)` that calls `Files.readString(path)` — a
   method that throws `IOException`. Notice the compiler forces you to handle it.
2. Write a helper that wraps the checked exception in an unchecked one.
3. Use the helper in a stream: create a `@TempDir`, write 3 small files to it, then
   stream the paths and read each file's content using your helper. Assert the contents.

**Hint:** Use `Files.writeString(dir.resolve("file1.txt"), "content1")` to create test
files. JUnit's `@TempDir` gives you a temporary directory that's cleaned up after the test.

---

## 3.2 — I/O with `java.nio`

### Read First

Java has two I/O APIs. Ignore `java.io.File` — it's legacy. Use `java.nio.file.Path`
and `java.nio.file.Files` for everything.

**`Path`** — represents a file or directory location:
```java
Path path = Path.of("data", "loans.json");      // relative: data/loans.json
Path abs = Path.of("/tmp", "loans.json");         // absolute
Path resolved = dir.resolve("loans.json");        // append to existing path
Path parent = path.getParent();                   // data/
String filename = path.getFileName().toString();  // loans.json
```

**`Files`** — static utility methods for all file operations:
```java
// Read entire file as a string (small files only)
String content = Files.readString(path);

// Read all lines
List<String> lines = Files.readAllLines(path);

// Write a string to a file (creates or overwrites)
Files.writeString(path, content);

// Write with options
Files.writeString(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

// Check existence
boolean exists = Files.exists(path);

// Create directories (including parents)
Files.createDirectories(path);

// List directory contents — returns a Stream (must be closed!)
try (var entries = Files.list(dir)) {
    entries.filter(p -> p.toString().endsWith(".json"))
           .forEach(System.out::println);
}

// Delete
Files.delete(path);              // throws if doesn't exist
Files.deleteIfExists(path);      // no-op if doesn't exist
```

**`Files.list()` and `Files.walk()` return Streams that hold file handles.** You *must*
close them — use try-with-resources. This is different from collection streams which are
just in-memory pipelines.

**`@TempDir`** — JUnit 5's test isolation:
```java
@Test
void readsFileContents(@TempDir Path tempDir) throws IOException {
    Path file = tempDir.resolve("test.txt");
    Files.writeString(file, "hello");

    String content = Files.readString(file);
    assertThat(content).isEqualTo("hello");
}
```

`@TempDir` creates a fresh temporary directory for each test (when on a parameter) or
each test class (when on a field). The directory is deleted after the test finishes.
This is your primary tool for testing I/O — no shared state, no cleanup needed.

**The TS comparison:** Node.js `fs.readFileSync()` → `Files.readString()`.
`fs.writeFileSync()` → `Files.writeString()`. `fs.readdirSync()` → `Files.list()`. The
concepts map directly — Java just forces you to handle the checked `IOException`.

**Watch out:** All `Files` methods that access the filesystem throw `IOException`. Every
call site must handle it. This is where checked exceptions show their teeth — and why
the wrapping pattern from 3.1 is essential in streams.

### Exercise 3.2a — File read/write basics

Create `src/test/java/org/example/FileIOTest.java`. All tests use `@TempDir`.

1. **Write and read a string:**
   - Write "Hello, Library!" to a file. Read it back. Assert equality.

2. **Write and read lines:**
   - Write a `List<String>` of 3 titles to a file (one per line). Read them back with
     `Files.readAllLines()`. Assert the list matches.

3. **File existence:**
   - Assert a file doesn't exist. Write to it. Assert it exists. Delete it. Assert it
     doesn't exist again.

4. **Directory creation:**
   - Use `Files.createDirectories()` to create nested directories
     (`tempDir/data/loans/`). Assert the directory exists.

5. **List directory contents:**
   - Write 3 `.json` files and 1 `.txt` file into the temp dir. Use `Files.list()` with
     a filter to find only `.json` files. Assert the count is 3.
   - Remember: `Files.list()` returns a stream that must be closed.

**Hint:** Declare the test method with `throws IOException` — since these are tests, it's
fine to let checked exceptions propagate to JUnit (which will report them as failures).

### Exercise 3.2b — Reading structured data from files

This exercise bridges I/O and the upcoming Jackson section. Don't use Jackson yet — just
read and write pipe-delimited strings, similar to how `EventSerializer` works.

Create `src/test/java/org/example/loan/LoanFileTest.java`. Use `@TempDir`.

1. **Write loan data to a file:**
   - Serialise several `Loan` objects to pipe-delimited strings (e.g.,
     `memberId|bookId`). Write them as lines to a file.
   - Read the file back. Parse each line into a loan record. Assert the data round-trips
     correctly.

2. **Handle a missing file gracefully:**
   - Attempt to read from a path that doesn't exist. Verify the appropriate exception
     is thrown (`NoSuchFileException`).

3. **Handle an empty file:**
   - Create an empty file. Read it. Assert you get zero loans.

**Hint:** This is intentionally manual serialisation. You'll feel the pain of parsing
pipe-delimited strings — that's the motivation for Jackson in the next section.

---

## 3.3 — Serialization with Jackson

### Read First

Jackson is Java's dominant JSON library (≈ JSON.stringify/parse but with type mapping).
You'll add it as a Maven dependency and use it to serialize your domain objects.

**Add to `pom.xml`:**
```xml
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.21.1</version>
</dependency>
```

**`ObjectMapper`** — the central API:
```java
ObjectMapper mapper = new ObjectMapper();

// Object → JSON string
String json = mapper.writeValueAsString(book);

// JSON string → Object
Book book = mapper.readValue(json, Book.class);

// Object → File
mapper.writeValue(path.toFile(), book);

// File → Object
Book book = mapper.readValue(path.toFile(), Book.class);

// For collections, use TypeReference:
List<Loan> loans = mapper.readValue(json, new TypeReference<List<Loan>>() {});
```

**Records work out of the box.** Jackson can serialize and deserialize records with no
configuration — it uses the canonical constructor and component accessors automatically.
`BookId`, `MemberId`, `BookBorrowed`, `BookReturned` — all records — will just work.

**Classes with private constructors don't.** `Book` has a private constructor and static
factories. Jackson can't instantiate it without help. You have two options:

1. **`@JsonCreator` on a static factory:**
   ```java
   @JsonCreator
   public static Book of(
       @JsonProperty("id") BookId id,
       @JsonProperty("title") String title,
       @JsonProperty("author") String author,
       @JsonProperty("genre") BookGenre genre
   ) {
       return new Book(id, title, author, genre);
   }
   ```

2. **`@JsonCreator` on the constructor** (but yours is private — you'd need to make it
   package-private, or stick with the static factory approach).

Option 1 is the natural fit for your design. You annotate the factory method that takes
all fields, and Jackson uses it for deserialization. The `@JsonProperty` annotations tell
Jackson which JSON field maps to which parameter.

**Serialization of `Book`:** Jackson will try to call getters to find properties. Your
`title()` and `author()` are package-private — Jackson can still see them (it uses
reflection), but it won't recognize `title()` as a getter by default. You need
`@JsonProperty` on the accessors or configure the mapper. The simplest approach: add
`@JsonProperty` to the fields, or annotate the accessor methods.

**Sealed interfaces and polymorphic serialization:**

Your `LibraryEvent` is a sealed interface with three record implementations. To serialize
and deserialize these correctly, Jackson needs to know which concrete type each JSON
object represents. This requires two annotations:

```java
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = BookAdded.class, name = "BOOK_ADDED"),
    @JsonSubTypes.Type(value = BookBorrowed.class, name = "BOOK_BORROWED"),
    @JsonSubTypes.Type(value = BookReturned.class, name = "BOOK_RETURNED")
})
public sealed interface LibraryEvent permits BookAdded, BookBorrowed, BookReturned {
```

Jackson adds a `"type"` field to the JSON when serializing, and reads it back to pick the
right class when deserializing. The `name` values become the discriminator — like a
TypeScript discriminated union's literal type field.

**Watch out:** Jackson errors happen at runtime, not compile time. A missing
`@JsonCreator`, wrong `@JsonProperty` name, or misconfigured `@JsonTypeInfo` will blow
up on the first serialization/deserialization attempt. Write tests early and run them
often.

### Exercise 3.3a — Record serialization

Create `src/test/java/org/example/JacksonTest.java`. Explore Jackson basics.

1. **Record round-trip:**
   - Create a `BookId`. Serialize to JSON. Deserialize back. Assert equality.
   - Do the same for `MemberId`.

2. **Nested record round-trip:**
   - Create a `BookBorrowed` (record containing `MemberId` and `BookId`). Serialize.
     Deserialize. Assert equality.

3. **Collection round-trip:**
   - Create a `List<BookId>`. Serialize. Deserialize using `TypeReference<List<BookId>>`.
     Assert equality.

4. **Pretty printing:**
   - Use `mapper.writerWithDefaultPrettyPrinter().writeValueAsString(...)` to produce
     formatted JSON. Assert it contains newlines.

**Hint:** Create the `ObjectMapper` once as a field. Records need zero configuration.
Jackson's `writeValueAsString()` and `readValue()` throw `JsonProcessingException` — a
checked exception. Declare `throws JsonProcessingException` on your test methods (same
pattern as `throws IOException` in the I/O tests). Import from `com.fasterxml.jackson.core`.

### Exercise 3.3b — Book serialization (class with private constructor)

This is the exercise where Jackson's defaults stop working. Add tests to `JacksonTest.java`
or create a separate `BookSerializationTest.java`.

1. **Attempt to serialize a `Book` without any Jackson annotations.** What happens?
   Jackson can't see the package-private accessors — you'll get an error or empty JSON.

2. **Add `@JsonCreator` and `@JsonProperty` annotations** to `Book.java`:
   - Annotate the 4-argument `of(BookId, String, String, BookGenre)` factory with
     `@JsonCreator`
   - Add `@JsonProperty` to each parameter
   - You'll also need Jackson to see the properties for serialization — annotate the
     relevant accessor methods with `@JsonProperty`, or annotate the fields directly

3. **Test the round-trip:**
   - Create a `Book` via its static factory. Serialize to JSON. Deserialize back. Assert
     the deserialized book has the same id, title, author, and genre.

4. **Verify validation still fires:**
   - Attempt to deserialize JSON with a blank title. Assert that `Book`'s constructor
     validation still throws.

**Hint:** Jackson uses `@JsonCreator` to find the factory, and `@JsonProperty` on its
parameters to map JSON keys. For serialization, Jackson needs to *read* the values back
out — it'll look for getters, public fields, or `@JsonProperty`-annotated methods. Think
about which of Book's accessors are currently public vs package-private.

**Watch out — two Jackson gotchas you'll hit:**

1. **`@JsonCreator` without `@JsonProperty` on parameters:** If you put `@JsonCreator` on
   the factory method but forget the `@JsonProperty` annotations on each parameter, Jackson
   treats it as a *delegating creator* — meaning it expects a single argument representing
   the entire JSON value. With 4 unannotated parameters, you'll get:
   `InvalidDefinitionException: More than one argument left as delegating for Creator`.
   The fix: add `@JsonProperty("fieldName")` to every parameter.

2. **`isLongLoan()` leaks into JSON:** Jackson treats any public `is*()` method as a
   boolean property (JavaBean convention). So `isLongLoan()` produces a `"longLoan"` field
   during serialization. On deserialization, Jackson finds `"longLoan"` in the JSON but has
   no matching constructor parameter — `UnrecognizedPropertyException`. Fix it with
   `@JsonIgnore` on the method. Alternative approaches:
   - `@JsonAutoDetect(isGetterVisibility = Visibility.NONE)` on the class — disables
     `is*()` auto-detection entirely
   - Configure the `ObjectMapper` globally:
     `mapper.setVisibility(PropertyAccessor.IS_GETTER, Visibility.NONE)`
   - Use `@JsonIgnoreProperties("longLoan")` on the class

   The `@JsonIgnore` approach is simplest for one-off cases. The visibility approaches are
   better when you have many derived methods you want to keep out of serialization — they
   let you opt *in* to what gets serialized rather than opting *out* method by method.

Don't annotate every `of()` overload — just the one with all fields. Jackson only needs
one path in and one path out.

### Exercise 3.3c — Sealed interface serialization (LibraryEvent)

Extend `EventSerializer`'s capabilities — but with Jackson instead of manual
pipe-delimited parsing.

1. **Add `@JsonTypeInfo` and `@JsonSubTypes`** to `LibraryEvent.java` (see the Read First
   section for the exact annotations).

2. **Test polymorphic round-trip:**
   - Serialize a `BookAdded`. The JSON should contain a `"type": "BOOK_ADDED"` field.
     Deserialize it back to a `LibraryEvent`. Assert it's equal to the original.
   - Do the same for `BookBorrowed` and `BookReturned`.

3. **Test heterogeneous list:**
   - Create a `List<LibraryEvent>` containing one of each type. Serialize the list.
     Deserialize it back as `List<LibraryEvent>` (using `TypeReference`). Assert each
     element is the correct type and has the correct data.

4. **Compare with `EventSerializer`:**
   - Serialize the same event with both Jackson and your existing `EventSerializer`.
     The formats will differ — that's fine. What matters is that both round-trip correctly.

**Hint:** `BookAdded` contains a `BookId` — a record with a single `String value`
component. Jackson will serialize this as `{"value": "..."}` by default. If you want it
flattened to just the string, look at `@JsonValue` on the `value()` accessor and
`@JsonCreator` on the record's constructor. This is optional — the nested form works fine
for this exercise.

---

## 3.4 — Capstone: File-Based Persistence

This is where everything comes together. You'll build a `FileLoanRepository` that
implements the same `LoanRepository` interface as `InMemoryLoanRepository`, but persists
data as JSON files on disk.

### Exercise 3.4a — Contract tests

Before writing the file-based implementation, extract your existing
`InMemoryLoanRepository` tests into a shared contract that can run against any
`LoanRepository` implementation.

**The idea:** You have tests in `LoanReporterTest` that exercise `InMemoryLoanRepository`
indirectly. But you need tests that exercise the *repository interface contract* directly
— save, find, delete, etc. These are the tests that should pass for *any* correct
implementation.

Create `src/test/java/org/example/loan/LoanRepositoryContractTest.java` — an
**abstract test class:**

```java
public abstract class LoanRepositoryContractTest {
    abstract LoanRepository createRepository();
    // tests go here...
}
```

Subclasses provide the implementation:

```java
public class InMemoryLoanRepositoryTest extends LoanRepositoryContractTest {
    @Override
    LoanRepository createRepository() {
        return new InMemoryLoanRepository();
    }
}
```

**Contract tests to write** (in the abstract class):

1. Saving a loan and finding it by member returns the book
2. Saving multiple loans for the same member returns all books
3. Deleting a loan removes it from the member's books
4. `isBookOnLoan` returns true for a loaned book, false otherwise
5. `borrowerOfBook` returns the correct member for a loaned book
6. `borrowerOfBook` returns empty for a book not on loan
7. `allBooksOnLoan` returns all books across all members

Use `@BeforeEach` to call `createRepository()` and store the result in a field. All
tests use that field.

**Run the tests.** Both `InMemoryLoanRepositoryTest` and (once you create it)
`FileLoanRepositoryTest` should pass the same contract.

**Hint:** The abstract class holds all the test methods. The concrete subclasses are
tiny — just the factory method and any implementation-specific setup (like creating a
temp directory for the file-based version).

### Exercise 3.4b — FileLoanRepository

Create `src/main/java/org/example/loan/FileLoanRepository.java`. It implements
`LoanRepository` and persists data as a JSON file.

**Design decisions** (make these yourself, but here's the space to think about):

- **Storage format:** one JSON file containing all loans? One file per member? One file
  per loan? Each has trade-offs. Start simple — a single JSON file is easiest. You can
  always change it later.
- **When to read/write:** read the file on every query? Cache in memory and flush on
  every write? Think about consistency vs performance. For a learning exercise, reading
  on every query is fine — it keeps the implementation simple and tests obvious.
- **What to store:** you need enough data to reconstruct all `LoanRepository` operations.
  At minimum: a mapping of member IDs to lists of book IDs.

**Constructor:** takes a `Path` (the file or directory to store data in) and an
`ObjectMapper`.

**Error handling:**
- Missing file on first read → treat as empty (no loans yet)
- Corrupt/unparseable JSON → throw `LoanPersistenceException` with the `IOException` as
  the cause
- I/O failure on write → throw `LoanPersistenceException`

**Tests:** create `src/test/java/org/example/loan/FileLoanRepositoryTest.java` extending
`LoanRepositoryContractTest`:

```java
public class FileLoanRepositoryTest extends LoanRepositoryContractTest {
    @TempDir
    Path tempDir;

    @Override
    LoanRepository createRepository() {
        // ...
    }
}
```

The contract tests should pass with no modifications. If they don't, your implementation
has a bug — or the contract test makes assumptions specific to the in-memory
implementation.

**Hint:** `Map<String, List<String>>` serializes cleanly with Jackson — member ID strings
as keys, lists of book ID strings as values. You can convert between `MemberId`/`BookId`
and strings at the repository boundary.

### Exercise 3.4c — Edge cases

Add additional tests to `FileLoanRepositoryTest` (not the contract — these are
file-specific):

1. **Empty file:** create an empty file at the storage path. Call `findBooksByMember()`.
   Assert it returns an empty list (not a crash).

2. **Corrupt JSON:** write garbage (`"not json at all"`) to the storage path. Call any
   read method. Assert `LoanPersistenceException` is thrown.

3. **Missing parent directory:** construct a `FileLoanRepository` with a path whose
   parent directory doesn't exist. Call `save()`. What should happen? Either create the
   directories automatically, or throw a clear error. Test your choice.

4. **Concurrent reads after write:** save a loan. Create a *second* `FileLoanRepository`
   instance pointing at the same file. Read from the second instance. Assert it sees the
   data written by the first. (This verifies that data actually hits the filesystem, not
   just an in-memory cache.)

**Hint:** For the corrupt JSON test, `Files.writeString(path, "not json")` before
constructing the repository. For the concurrent read test, two repository instances with
the same `Path` and separate `ObjectMapper` instances.

---

## Review

After completing Phase 3, your library should have:

- [ ] `LoanPersistenceException` — a custom unchecked exception with cause chaining
- [ ] `FileLoanRepository implements LoanRepository` — file-backed persistence with Jackson
- [ ] `@JsonCreator` + `@JsonProperty` on `Book` for Jackson deserialization
- [ ] `@JsonTypeInfo` + `@JsonSubTypes` on `LibraryEvent` for polymorphic serialization
- [ ] `LoanRepositoryContractTest` — abstract test class running against both implementations
- [ ] Understanding of checked vs unchecked exceptions and when to use each
- [ ] Comfort with `java.nio.file.Path` and `Files` for file operations
- [ ] `@TempDir` for test isolation with file I/O
- [ ] All tests passing

**Consider:**
1. How does checked exception handling change the way you write code compared to TS/C#?
   (Answer: every I/O call site forces a decision — handle or propagate. In TS you'd
   forget to handle it until production.)
2. Where did the wrapper pattern (checked → unchecked) show up, and when is it appropriate?
3. What's the value of contract tests? How confident are you that `FileLoanRepository`
   behaves identically to `InMemoryLoanRepository`?
4. Compare `EventSerializer`'s manual pipe-delimited format to Jackson's JSON. Which is
   easier to maintain? Which handles schema evolution better?
