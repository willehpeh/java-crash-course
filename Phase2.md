# Phase 2 — Collections, Streams & Functional Java

> **Goal:** By the end of this phase you'll be fluent with Java's collections framework,
> lambda expressions, functional interfaces, and the Streams API — and your library domain
> will use all of them for search, filtering, and reporting.
>
> **Starting point:** You have the full Phase 1 domain: `Book`, `BookId`, `MemberId`,
> `BookGenre`, `Loan`, `LoanStatus`, `LoanReceipt`, `LoanRepository`,
> `InMemoryLoanRepository`, `LendingService`, `LibraryEvent` hierarchy, `Result<T>`,
> `Searchable`, and `Displayable`.

---

## 2.1 — Collections Framework

### Read First

You've already used `List`, `Map`, and `ArrayList` in Phase 0/1. This section covers
the full picture.

**The hierarchy:** Java collections are interfaces with multiple implementations. You
program to the interface:

```
Collection
├── List (ordered, duplicates allowed)
│   ├── ArrayList (default — backed by resizable array)
│   └── LinkedList (rarely needed — worse cache locality)
├── Set (unique elements)
│   ├── HashSet (unordered, O(1) lookup)
│   ├── LinkedHashSet (insertion-ordered, O(1) lookup)
│   └── TreeSet (sorted, O(log n) lookup)
└── Queue / Deque (FIFO / double-ended)

Map (not a Collection, but part of the framework)
├── HashMap (unordered, O(1) lookup)
├── LinkedHashMap (insertion-ordered)
└── TreeMap (sorted by key, O(log n))
```

**Choosing an implementation:**
- Need order + duplicates? → `ArrayList`
- Need uniqueness? → `HashSet` (unordered) or `LinkedHashSet` (ordered)
- Need sorted uniqueness? → `TreeSet` (elements must be `Comparable` or you supply a `Comparator`)
- Need key-value lookup? → `HashMap` (unordered) or `LinkedHashMap` (ordered)
- Need sorted keys? → `TreeMap`

**Immutable collections:**
```java
List<String> immutable = List.of("a", "b", "c");     // truly unmodifiable
Set<String> immutableSet = Set.of("a", "b", "c");     // unordered!
Map<String, Integer> immutableMap = Map.of("a", 1, "b", 2);

// Wrapping a mutable list — the wrapper is unmodifiable,
// but if someone holds the original reference, they can still mutate it
List<String> mutable = new ArrayList<>(List.of("a", "b"));
List<String> wrapped = Collections.unmodifiableList(mutable);
```

`List.of()` and friends throw `UnsupportedOperationException` on any mutation attempt
(`.add()`, `.remove()`, `.set()`). This is a runtime check, not a compile-time one —
Java has no `readonly` modifier like TypeScript.

**Copying:** `new ArrayList<>(existingList)` creates a shallow copy. The list is new;
the elements are the same objects. For value types (records, strings) this is fine.
For mutable objects, changes to the element affect both lists.

**`Comparable` vs `Comparator`:**
- `Comparable<T>` — the object defines its own natural ordering: `class Book implements Comparable<Book>`
- `Comparator<T>` — an external ordering you supply: `Comparator.comparing(Book::title)`

```java
// Natural ordering — the class decides
books.sort(null);  // uses Book's compareTo

// External ordering — you decide
books.sort(Comparator.comparing(Book::title));
books.sort(Comparator.comparing(Book::title).reversed());
books.sort(Comparator.comparing(Book::title).thenComparing(Book::author));
```

### Exercise 2.1a — Collection type exploration

Create `src/test/java/org/example/CollectionTest.java`. Write tests that demonstrate:

1. **List ordering and duplicates:**
   - Create an `ArrayList` with duplicate elements. Assert duplicates are preserved and
     order matches insertion order.

2. **Set uniqueness:**
   - Add the same `BookId` twice to a `HashSet`. Assert the size is 1.
   - Create a `LinkedHashSet`, add items, assert iteration order matches insertion order.

3. **Immutability:**
   - Create a `List.of(...)`. Assert that calling `.add()` throws
     `UnsupportedOperationException`.
   - Create a mutable copy with `new ArrayList<>(List.of(...))`. Assert `.add()` succeeds.

4. **Map operations:**
   - Use `Map.of(...)` to create an immutable map. Assert `.get()`, `.containsKey()`,
     `.size()`.
   - Use `HashMap` with `.put()`, `.getOrDefault()`, `.putIfAbsent()`.

**Hint:** `Set.of()` does not guarantee iteration order. `LinkedHashSet` does.

### Exercise 2.1b — Shelf

A library has shelves. A `Shelf` holds books in a defined order determined by a
`Comparator<Book>` passed at construction time.

Create `src/main/java/org/example/book/Shelf.java` and tests in
`src/test/java/org/example/book/ShelfTest.java`. `Shelf` lives in the `book` package
so it can access Book's package-private internals — Book shouldn't have to know about
comparison or expose public getters just for sorting.

**Static factories** (no public constructor):
- `Shelf.sortedByTitle()` / `Shelf.sortedByTitle(List<Book> books)`
- `Shelf.sortedByAuthor()` / `Shelf.sortedByAuthor(List<Book> books)`

**Methods:**
- `addBook(Book book)` — places a book on the shelf
- `books()` — returns the books in shelf order (unmodifiable)

**Design note:** All comparison logic lives inside `Shelf`, using Book's package-private
accessors. Callers never see or construct a `Comparator`. Book stays ignorant of
comparison, and its internals aren't publicly exposed.

**Tests:**
- A title-sorted shelf returns books alphabetically by title
- An author-sorted shelf returns books alphabetically by author's last name
- Adding a book to an existing shelf maintains order
- Pre-populating via the factory produces the same order as adding one by one

**Hint:** Use `Comparator.comparing(...)` inside the factories, referencing Book's
package-private methods. Think about which collection type keeps elements sorted
automatically.

**Gotcha — `TreeSet` and comparator equality:** If you use a `TreeSet`, be aware that
it determines uniqueness using the `Comparator`, **not** `equals()`/`hashCode()`. Two
books by the same author will return `0` from an author-only comparator, and `TreeSet`
will silently drop one as a "duplicate." You must chain enough tiebreaker fields that
only truly identical books compare as `0` — or use an `ArrayList` with sorted insertion
instead, which avoids the uniqueness issue entirely.

---

## 2.2 — Lambdas & Functional Interfaces

### Read First

You've written lambdas since Phase 0: `_ -> new ArrayList<>()`, `book -> book.title()`.
This section covers the full model.

**Lambda syntax variations:**
```java
// Single parameter, single expression
x -> x + 1

// Multiple parameters
(x, y) -> x + y

// Explicit types (rarely needed — the compiler infers them)
(String s) -> s.toUpperCase()

// Multi-line body — needs braces and explicit return
(x, y) -> {
    var sum = x + y;
    return sum * 2;
}

// No parameters
() -> "hello"
```

**Standard functional interfaces** (in `java.util.function`):

| Interface | Signature | Use case |
|-----------|-----------|----------|
| `Function<T, R>` | `R apply(T t)` | Transform a value |
| `Predicate<T>` | `boolean test(T t)` | Test a condition |
| `Consumer<T>` | `void accept(T t)` | Side effect (e.g., logging) |
| `Supplier<T>` | `T get()` | Lazy value production |
| `BiFunction<T, U, R>` | `R apply(T t, U u)` | Two-arg transform |
| `UnaryOperator<T>` | `T apply(T t)` | Transform, same type in and out |
| `BinaryOperator<T>` | `T apply(T t1, T t2)` | Combine two values of same type |

You've already used `Function<T, U>` in `Result.map()`. The others work the same way —
they're just interfaces with a single abstract method.

**Method references** — shorthand for lambdas that just call an existing method:

```java
// Instance method reference — calls .title() on each book
books.stream().map(Book::title)         // same as: book -> book.title()

// Static method reference
strings.stream().map(Integer::parseInt) // same as: s -> Integer.parseInt(s)

// Bound instance method — calls .equals("Java") on the string
strings.stream().filter("Java"::equals) // same as: s -> "Java".equals(s)
```

**`@FunctionalInterface`:** An annotation that tells the compiler "this interface must
have exactly one abstract method." It's a safety net — if someone adds a second abstract
method, the compiler catches it. All the standard functional interfaces use it. You should
too, when writing your own.

**Effectively final:** Lambdas can capture local variables, but only if they're
*effectively final* — never reassigned after initialisation. This is enforced by the
compiler, unlike JavaScript where closures can mutate freely.

```java
int count = 0;
list.forEach(x -> count++);  // COMPILE ERROR — count is not effectively final
```

This is deliberate. Mutating shared state from lambdas leads to concurrency bugs. Use
`.reduce()` or `.collect()` instead of mutation.

### Exercise 2.2a — Functional interface exploration

Create `src/test/java/org/example/FunctionalTest.java`. Tests:

1. **Predicate:**
   - Write a `Predicate<Book>` that tests whether a book is a long loan. Apply it to
     a fiction book and a reference book. Assert the results.
   - Combine predicates with `.and()`, `.or()`, `.negate()`:
     create a predicate that matches fiction books by a specific author.

2. **Function composition:**
   - Write a `Function<Book, String>` that extracts the title. Write another
     `Function<String, String>` that converts to uppercase. Chain them with
     `.andThen()`. Assert the composed function works.

3. **Consumer:**
   - Create a `List<String>` to accumulate results. Write a `Consumer<Book>` that adds
     the book's title to the list. Apply it to several books. Assert the list contents.

4. **Supplier:**
   - Write a `Supplier<Book>` that creates a default book. Call `.get()` twice and
     assert both books are equal (same factory input = same record values).

5. **Effectively final:**
   - Write a test that demonstrates the effectively-final rule. Declare a variable,
     use it in a lambda (works). Then add a reassignment before the lambda — see the
     compiler error. Comment it out with a note explaining why.

**Hint:** For the predicate exercise, `Book::isLongLoan` is a method reference that
matches `Predicate<Book>`.

### Exercise 2.2b — Custom functional interface

Create a `@FunctionalInterface` called `BookMatcher` in `src/main/java/org/example/book/`:

```java
@FunctionalInterface
public interface BookMatcher {
    boolean matches(Book book);
}
```

Write tests in `BookMatcherTest.java`:

- A `BookMatcher` implemented with a lambda matches the expected book
- A `BookMatcher` implemented with a method reference (`Book::isLongLoan`) works
- `BookMatcher` can be composed — add `default BookMatcher and(BookMatcher other)` and
  `default BookMatcher or(BookMatcher other)` methods to the interface, test them

Then reflect: `BookMatcher` is functionally identical to `Predicate<Book>`. When would
you create a domain-specific functional interface vs using `Predicate<T>` directly?
(Answer: when the name adds clarity to the domain. `BookMatcher` reads better in a
library context than `Predicate<Book>`. But for one-off lambdas, `Predicate` is fine.)

---

## 2.3 — Streams API

### Read First

If you know RxJS, you already understand the mental model — but Java streams are simpler:
**synchronous, pull-based, single-use, and finite.**

**The pipeline pattern:**
```java
source.stream()             // create a stream from a collection
    .filter(predicate)      // intermediate — lazy, returns a new stream
    .map(function)          // intermediate
    .sorted(comparator)     // intermediate
    .collect(Collectors.toList());  // terminal — triggers execution
```

Nothing happens until a **terminal operation** is called. Intermediate operations are lazy
— they build a pipeline description. The terminal operation pulls elements through the
pipeline one at a time.

**Key intermediate operations:**

| Operation | What it does | RxJS equivalent |
|-----------|-------------|-----------------|
| `.filter(predicate)` | Keep matching elements | `filter()` |
| `.map(function)` | Transform each element | `map()` |
| `.flatMap(function)` | Transform + flatten (1-to-many) | `mergeMap()`/`switchMap()` |
| `.sorted()` / `.sorted(comparator)` | Sort elements | — |
| `.distinct()` | Remove duplicates (uses `.equals()`) | `distinct()` |
| `.peek(consumer)` | Side effect without consuming (debugging) | `tap()` |
| `.limit(n)` | Take first n elements | `take()` |
| `.skip(n)` | Skip first n elements | `skip()` |

**Key terminal operations:**

| Operation | What it does | Returns |
|-----------|-------------|---------|
| `.toList()` | Collect to unmodifiable list | `List<T>` |
| `.collect(collector)` | Collect to any structure | varies |
| `.forEach(consumer)` | Side effect on each element | `void` |
| `.count()` | Count elements | `long` |
| `.findFirst()` | First element (if any) | `Optional<T>` |
| `.findAny()` | Any element (for parallel) | `Optional<T>` |
| `.anyMatch(predicate)` | Does any element match? | `boolean` |
| `.allMatch(predicate)` | Do all elements match? | `boolean` |
| `.noneMatch(predicate)` | Does no element match? | `boolean` |
| `.reduce(identity, accumulator)` | Fold elements into one value | `T` |
| `.min(comparator)` / `.max(comparator)` | Smallest/largest element | `Optional<T>` |

**Collectors** — the power tools (and why `reduce` isn't):

In TypeScript, `reduce` is the Swiss army knife — you use it for grouping, counting,
building objects, joining strings. In Java, `Collectors` provides purpose-built operations
for all of those. `reduce` still exists for folding into a single value (sums, products),
but reach for a `Collector` first — it's more readable and handles mutable accumulation
(like building a `Map`) more efficiently than `reduce` can.

```java
// Group books by genre → Map<BookGenre, List<Book>>
books.stream().collect(Collectors.groupingBy(Book::genre));

// Group + count → Map<BookGenre, Long>
books.stream().collect(Collectors.groupingBy(Book::genre, Collectors.counting()));

// Join strings
titles.stream().collect(Collectors.joining(", "));

// Partition by predicate → Map<Boolean, List<Book>>
books.stream().collect(Collectors.partitioningBy(Book::isLongLoan));

// Collect to a specific map type
books.stream().collect(Collectors.toMap(Book::id, Function.identity()));
```

**`.toList()` vs `Collectors.toList()`:** Since Java 16, `.toList()` exists directly on
Stream. It returns an unmodifiable list. `Collectors.toList()` returns a mutable
`ArrayList`. Prefer `.toList()` unless you need to mutate the result.

**`flatMap` — the one that trips people up:**
```java
// Each member has a list of borrowed books.
// flatMap flattens the nested lists into a single stream of BookIds.
members.stream()
    .map(member -> repository.findBooksByMember(member))  // Stream<List<BookId>>
    .flatMap(List::stream)                                 // Stream<BookId>
    .toList();
```

Use `flatMap` when each element maps to zero-or-more results and you want a flat stream,
not a stream of collections.

**Streams are single-use:**
```java
var stream = books.stream().filter(Book::isLongLoan);
stream.count();     // works
stream.toList();    // throws IllegalStateException — stream already consumed
```

If you need the same pipeline twice, call `.stream()` again on the source collection.

### Exercise 2.3a — Stream basics

Create `src/test/java/org/example/StreamTest.java`. Build a list of 5-6 books with
varying titles, authors, and genres. Tests:

1. **filter:** Find all fiction books. Assert the count.
2. **map:** Extract all titles into a `List<String>`. Assert the contents.
3. **filter + map:** Find titles of all non-fiction books.
4. **sorted:** Sort books by title, extract titles. Assert the order.
5. **distinct:** Create a list of genres with duplicates (from the books). Use `.distinct()`
   to get unique genres. Assert the count.
6. **findFirst:** Find the first book whose title contains "Java" (or whatever title you
   used). Assert it's present and correct.
7. **anyMatch / noneMatch:** Assert that at least one book is fiction. Assert that no book
   has a blank title.
8. **count:** Count books with max loan days greater than 14.

**Hint:** You'll need a way to get a book's genre. If `Book` doesn't expose it yet, add
a `genre()` accessor method.

### Exercise 2.3b — Collectors

Add to `StreamTest.java` or create `CollectorTest.java`:

1. **groupingBy:** Group the books by genre. Assert the map has the expected keys and
   the right number of books per genre.
2. **groupingBy + counting:** Group by genre and count. Assert each genre's count.
3. **partitioningBy:** Partition books into long-loan and short-loan. Assert both
   partitions have the expected books.
4. **joining:** Collect all titles into a single comma-separated string. Assert the
   result.
5. **toMap:** Create a `Map<BookId, Book>` from the list. Assert you can look up a
   book by its ID.
6. **reduce:** Compute the total max loan days across all books using `.reduce()`.
   Assert the sum.

**Hint:** `Collectors.groupingBy(Book::genre)` returns `Map<BookGenre, List<Book>>`.
Import `java.util.stream.Collectors` or use static import for readability.

### Exercise 2.3c — flatMap

Add these tests:

1. Create a `Map<MemberId, List<BookId>>` representing loans (simulate what the
   repository holds). Use `flatMap` to collect *all* borrowed book IDs across all
   members into a single list. Assert the total count.
2. Create a list of sentences (strings). Use `flatMap` with `Arrays.stream(s.split(" "))`
   to get a flat stream of all words. Assert the word count.

**Hint:** For the first test, `map.values().stream()` gives you `Stream<List<BookId>>`,
then `.flatMap(List::stream)` flattens it to `Stream<BookId>`.

---

## 2.4 — Capstone: Library Search & Reporting

This capstone extends the library domain with real stream-based operations. You'll add
a `Catalog` that holds the library's book collection and supports queries.

### Exercise 2.4a — Catalog

Create `src/main/java/org/example/book/Catalog.java`. A `Catalog` holds a collection of
`Book` objects and provides query methods. Constructor takes a `List<Book>`. Tests in
`src/test/java/org/example/book/CatalogTest.java`.

Build a test fixture class (`TestBooks`) with 8-10 books spanning all three genres,
multiple authors, and varied titles. Use uneven numbers per genre so that counting
tests are meaningful. Use `@BeforeEach` or a field initialiser to set up the catalog.

Query methods and their tests:

1. **`List<Book> booksBy(String author)`** — all books by the given author, sorted by title
   - Assert correct books returned for a known author
   - Assert empty list for an unknown author

2. **`List<Book> booksWithGenre(BookGenre genre)`** — all books of the given genre
   - Assert correct count and contents for each genre

3. **`Map<BookGenre, List<Book>> booksGroupedByGenre()`** — all books grouped by genre
   - Assert all genres present as keys
   - Assert correct books in each group

4. **`Map<BookGenre, Long> countByGenre()`** — count of books per genre
   - Assert counts match (uneven genre sizes make this test meaningful)

5. **`List<Book> search(String query)`** — books matching the query (use the `Searchable`
   interface you built in Phase 1)
   - Assert matching by title
   - Assert matching by author
   - Assert case-insensitive matching
   - Assert empty result for non-matching query

6. **`Optional<Book> findById(BookId id)`** — look up a single book
   - Assert found for a known ID — AssertJ's `assertThat(optional).contains(value)`
     works directly on `Optional`
   - Assert empty for an unknown ID

**Hint:** Each method should be a one-liner stream pipeline internally. The `Catalog` is
a good place to see how streams replace what would be loops in imperative code. Prefer
asking the book a question (`book.hasGenre(genre)`, `book.matches(query)`) over
extracting its fields and comparing externally.

### Exercise 2.4b — Loan reporting

Create `src/main/java/org/example/loan/LoanReporter.java`. A `LoanReporter` takes a
`LoanRepository` and a `Catalog`, and generates aggregate data about current loans.
Tests in `src/test/java/org/example/loan/LoanReporterTest.java`.

Set up a test fixture: populate the `InMemoryLoanRepository` with several loans across
multiple members, and a `Catalog` built from `TestBooks`.

Methods and tests:

1. **`List<Book> booksOnLoanTo(MemberId memberId)`** — the actual `Book` objects
   (not just IDs) currently on loan to a member
   - Uses `repository.findBooksByMember()` then maps each `BookId` to a `Book` via
     the catalog
   - Assert correct books returned

2. **`Map<BookGenre, Long> loanCountByGenre()`** — across all loans, how many books
   of each genre are out
   - This requires getting all loans, resolving book IDs to books, then grouping
   - Assert the genre distribution

3. **`String summary()`** — a human-readable summary (the object owns its representation)
   - Should include total books on loan and the per-genre breakdown
   - Assert the summary contains expected text fragments

**Hint:** For `booksOnLoanTo`, you'll need all borrowed `BookId`s, then `.map()` each
through `catalog.findById()`. Think about what to do when a `BookId` isn't in the catalog
— `flatMap` with `Optional::stream` is the idiomatic way to silently drop missing entries.

### Exercise 2.4c — Putting it together

Write a test that exercises a full workflow:

1. Create a catalog, a repository, a `LendingService`, and a `LoanReporter`
2. Borrow some books via `LendingService`
3. Assert the reporter's `summary()` contains the right data
4. Return a book, assert the summary updated

All real objects, no mocks.

---

## Review

After completing Phase 2, your library should have:

- [ ] `Catalog` with stream-based search, filter, group, and lookup operations
- [ ] `LoanReport` with aggregate loan data using streams and collectors
- [ ] `BookMatcher` functional interface with composition
- [ ] Familiarity with `Comparator.comparing()` for sorting
- [ ] Comfort with `filter`, `map`, `flatMap`, `collect`, `groupingBy`, `reduce`
- [ ] Understanding of `Predicate`, `Function`, `Consumer`, `Supplier`
- [ ] All tests passing

**Consider:**
1. How do Java streams compare to RxJS pipes? What's missing? (Answer: async,
   backpressure, multicasting, error channels. Streams are simpler by design.)
2. Where did `flatMap` show up, and why couldn't `map` do the job?
3. When would you use `Predicate<Book>` vs `BookMatcher`? Is the extra type worth it?
4. Look at your `Catalog` methods — could any of them be expressed as a single
   `default` method on a new interface? (Don't do it — just notice the option.)