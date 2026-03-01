# Java Crash Course — Summary

> This file documents the course structure, design decisions, and current state.
> It is intended to be read by future Claude instances continuing this course.
> **Update this file whenever a phase is completed or a significant design decision is made.**

---

## Learner Profile

- Experienced **TypeScript and Angular** developer
- Some **.NET/C#** experience
- Goal: become fully operational in Java as fast as possible
- Prefers depth over speed — happy with a longer, more detailed course
- Strong opinions on software design (see Design Principles below)

---

## Course Structure

### Files

| File | Purpose |
|------|---------|
| `PLAN.md` | High-level plan for all 8 phases (0–7). Outlines topics per section. |
| `Phase0.md` | Detailed learning text for Phase 0 (exercises, hints, explanations). |
| `Phase1.md` | Detailed learning text for Phase 1 (exercises, hints, explanations). |
| `SUMMARY.md` | This file. Course meta-documentation for continuity across sessions. |

Phase text files follow this pattern and should be created for each new phase:
- **Read First** sections explain concepts, contrasting with TS/C# equivalents
- **Exercises** give specific tasks with hints but NOT solutions
- **Crash Course Asides** explain Java-specific concepts inline when needed

### Phases Overview

| Phase | Topic | Status |
|-------|-------|--------|
| 0 | Testing Toolkit & Build Essentials | **COMPLETED** |
| 1 | Language Mechanics | **IN PROGRESS** (user is on exercise 1.2c) |
| 2 | Collections, Streams & Functional Java | Not started |
| 3 | Error Handling, I/O & Serialization | Not started |
| 4 | Concurrency | Not started |
| 5 | Build System, Ecosystem & Practical Java | Not started |
| 6 | Spring Boot | Not started |
| 7 | Advanced Topics | Not started |

---

## Design Principles

These were established through discussion and should be maintained throughout the course.

### Teaching Approach

1. **The learner does the exercises.** Never write complete solutions. Provide instructions,
   hints, imports, and TS/C# equivalents — but the implementation is theirs.
2. **TDD-first, Detroit school.** No mock frameworks (no Mockito, ever). Dependencies are
   handled with fakes (in-memory implementations of interfaces) and stubs.
3. **Each test should drive new code.** No "reassurance tests" that pass without writing
   anything new. If the existing code already makes a proposed test pass, the test
   doesn't belong in the TDD sequence.
4. **Each test should test one behaviour.** Don't combine "save and retrieve" into one
   test — split into "save (verify via fake)" and "retrieve (set up via fake)".
5. **Don't test exception types or messages.** Just assert that it throws.
   `assertThatThrownBy(() -> ...).isInstanceOf(Exception.class)` is enough. The exception
   class hierarchy and message wording are implementation details.
6. **Exercises must be realistic.** Don't invent artificial scenarios to demonstrate a
   concept. If the exercise feels contrived, rethink it.
7. **Explain annotations explicitly.** The learner didn't have a mental model for Java
   annotations. They're explained as "labels that frameworks scan for" — metadata, not
   code that wraps or modifies (unlike TS decorators).

### OOP & Architecture

1. **Bugayenko-style OOP.** Objects encapsulate their own behaviour. No passive data
   bags acted on by external service/formatter classes. Prefer `receipt.asText()` over
   `formatter.format(receipt)`.
2. **Value objects as records.** Use records for value types (`BookId`, `MemberId`, `Book`).
   Compact constructors for validation.
3. **Programming to interfaces.** Repository interfaces in production code, fakes in test
   code. This emerged naturally from Detroit-school TDD.
4. **Constructor injection.** Dependencies passed via constructor, fields marked `final`.

### Testing Conventions

- **Fakes** (e.g., `InMemoryLoanRepository`) live in `src/test/java/org/example/`
- **Contract tests** verify fake behaviour — same tests can later run against real
  implementations (e.g., file-based or DB-backed repository in Phase 3)
- **Test naming:** `shouldDoSomething` style (no `given_when_then` prefix)
- **`@Nested`** for grouping (≈ Jest `describe`), typically one level deep
- **AssertJ** for all assertions, not JUnit's `assertEquals` etc.

---

## Current Codebase

### Production Code (`src/main/java/org/example/`)

| File | Type | Purpose | Introduced |
|------|------|---------|------------|
| `BookId.java` | Record | Value object for book IDs, validates non-blank | Phase 1.1 |
| `MemberId.java` | Record | Value object for member IDs, validates non-blank | Phase 1.1 |
| `Book.java` | Record | Book with `BookId`, title, author; validates all fields | Phase 1.2 |
| `LoanRepository.java` | Interface | Loan storage contract: save, delete, findBooksByMember, isBookOnLoan | Phase 0 (refactored in 1.1 to use VOs) |
| `LendingService.java` | Class | Business rules: borrowing limits, double-loan prevention | Phase 0 (refactored in 1.1 to use VOs) |
| `LendingServiceConfig.java` | Record | Configuration: lending limit | Phase 0 |
| `Main.java` | Class | Entry point (unused, from project template) | — |

### Test Code (`src/test/java/org/example/`)

| File | Type | Tests | Introduced |
|------|------|-------|------------|
| `InMemoryLoanRepository.java` | Fake | — (test infrastructure) | Phase 0 (refactored in 1.1) |
| `InMemoryLoanRepositoryTest.java` | Test | 5 tests: save, empty list, multiple loans, isBookOnLoan, delete | Phase 0 |
| `LendingServiceTest.java` | Test | 6 tests: borrow, get loans, return, double-loan, no-loan return, limit | Phase 0 |
| `FirstTest.java` | Test | 7 tests: basic assertions, lists, extracting, parameterized | Phase 0 |
| `BookIdTest.java` | Test | 5 tests: equality, inequality, toString, blank, null | Phase 1.1 |
| `MemberIdTest.java` | Test | 2 tests: blank, null | Phase 1.1 |
| `BookTest.java` | Test | 6 tests (nested): title blank/null/200/201, author blank/null | Phase 1.2 |
| `LoanReceiptTest.java` | Test | **IN PROGRESS** — user is currently working on this | Phase 1.2 |

### Key Refactorings Done

1. **Phase 1.1:** `LoanRepository`, `InMemoryLoanRepository`, `LendingService`, and all
   tests were refactored from raw `String` IDs to `BookId`/`MemberId` value objects.
   This was the first significant refactor and taught the learner to follow compiler errors.

---

## Decisions & Rationale Log

Record significant decisions here so future sessions don't re-litigate them.

| Decision | Rationale | Phase |
|----------|-----------|-------|
| No Mockito, ever | Detroit-school TDD tests behaviour not implementation. Fakes break less during refactoring. | 0 |
| Don't test exception types/messages | Exception class hierarchy and message wording are implementation details. Just assert it throws. | 0 |
| Unifying library domain project | Concepts applied to a growing domain model, not isolated throwaway exercises. | 0 |
| Value objects for IDs | Prevents mixing up `memberId` and `bookId` at compile time. Records provide correct equality. | 1.1 |
| Bugayenko-style OOP | Objects own their behaviour. `LoanReceipt.asText()` not `LoanReceiptFormatter.format(data)`. | 1.2 |
| `var` usage: conservative | Java convention is more conservative than C#. Use explicit types in most cases; `var` when the type is obvious from the right-hand side. | 0 |
| Records for value objects | Standard modern Java approach. Only use classes when mutability, inheritance, or selective equality is needed. | 1.1 |
| Repository uses VOs not primitives | Domain layer should speak the domain's language. Primitive-to-VO conversion happens at infrastructure boundaries. | 1.1 |

---

## Concepts the Learner Has Encountered

Track what's been covered so later phases don't re-explain it.

### Covered (learner has used these)
- JUnit 5: `@Test`, `@DisplayName`, `@Nested`, `@BeforeEach`, `@ParameterizedTest` (`@ValueSource`, `@CsvSource`, `@MethodSource`)
- AssertJ: `assertThat`, `assertThatThrownBy`, `.isEqualTo()`, `.contains()`, `.containsExactly()`, `.containsExactlyInAnyOrder()`, `.isEmpty()`, `.isTrue()`, `.isFalse()`, `.hasSize()`, `.extracting()`, `.isInstanceOf()`
- Records: definition, accessors, compact constructors with validation, value equality
- Interfaces: definition, implementation, programming to interfaces
- Classes: `private final` fields, constructors, constructor injection, `this.`
- Collections: `List`, `Map`, `HashMap`, `ArrayList`, `List.of()`, `Map.of()` (immutable), `computeIfAbsent`, `getOrDefault`
- Streams: `.stream().anyMatch()` with lambda
- Lambdas: `_ -> new ArrayList<>()`, `books -> books.contains(bookId)`
- Annotations: understood as "labels that frameworks scan for" (not TS decorators)
- Maven: `pom.xml`, dependencies, `mvn test`, surefire plugin
- `String.repeat()`, `.isBlank()`, `.length()`
- `var` for local type inference

### Explained but not yet exercised
- `==` vs `.equals()` (covered in Phase1.md 1.1, may have been read but exploration tests not yet written)
- Autoboxing / Integer cache
- `hashCode()` contract
- Annotations: `@Override`

### Not yet covered
- `Optional`, `static` methods, `final` on methods/classes, method overloading, `protected`, package-private
- Abstract classes, `default` methods on interfaces
- Enums, sealed types, pattern matching
- Generics (writing, not consuming), type erasure, bounded types
- Exceptions (checked vs unchecked), `try-with-resources`
- `StringBuilder`, text blocks, `.formatted()`
- `java.time`, logging, Lombok
- Collections in depth, Streams API, functional interfaces
- Concurrency, Spring Boot

---

## Notes for Future Sessions

1. **Always read the learner's current code** before writing new exercises. The domain
   model evolves — exercises must build on what exists, not start fresh.
2. **Before writing a phase's learning text**, review the plan in `PLAN.md` for that phase,
   then check what the learner already knows (the "Concepts Encountered" section above).
   Adjust the plan to skip what's redundant.
3. **When the learner asks a question**, consider whether the answer should be added to
   the current phase's `.md` file as a "Crash Course Aside" for future reference.
4. **Update this file** when a phase is completed or a significant decision is made.
5. **The learner has strong design opinions.** Don't propose patterns they've rejected
   (mocks, passive data + formatters, testing exception messages). When in doubt, ask.
6. **LoanReceipt exercise is in progress.** The `LoanReceipt` class and `LoanReceiptTest`
   exist but are incomplete — the learner is currently working on exercise 1.2c.
