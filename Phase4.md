# Phase 4 — Concurrency

> **Goal:** By the end of this phase you'll understand Java's threading model, be able to
> identify and fix race conditions, use `CompletableFuture` for concurrent operations, and
> work with virtual threads. This is the biggest conceptual leap from TypeScript — Java has
> real parallelism, shared mutable state, and all the bugs that come with it.
>
> **Starting point:** You have the full Phase 1–3 domain: `LendingService` (check-then-act
> borrowing logic), `InMemoryLoanRepository` (backed by `HashMap`), `FileLoanRepository`
> (file-backed with Jackson), `LoanReporter` (stream-based queries), `Catalog` (stream-based
> search/filter), and the rest of the library domain.

---

## 4.1 — The Concurrency Mental Model

### Read First

**The TS/C# model you're coming from:**

TypeScript runs on a single-threaded event loop. There's only one thread executing your
code. `async/await` is about *waiting efficiently* — while one request waits for a database
response, another request can run. But two pieces of your code never execute *at the same
time*. This means shared state (a global `Map`, a module-level variable) is always safe —
no two functions can modify it simultaneously.

C# has `async/await` too, but it *can* use multiple threads via `Task.Run()` and the thread
pool. However, most C# web code stays on one thread per request with async I/O, so you
rarely encounter true parallelism.

**The Java model:**

Java uses real OS threads. Multiple threads execute your code *simultaneously* on different
CPU cores. When two threads call `loanRepository.save()` at the same time, both method
bodies execute in parallel — interleaving at any instruction. This means:

- Two threads can read the same `HashMap` entry at the same instant
- One thread can be halfway through adding an entry while another thread iterates the map
- A thread can check a condition (`isBookOnLoan`), find it false, and by the time it acts
  on that result, another thread has already changed the answer

This isn't theoretical. Your existing code has real bugs:

1. **`InMemoryLoanRepository`** uses `HashMap` — which is not thread-safe. Concurrent
   `put()` calls can corrupt the internal structure (lost entries, infinite loops in older
   Java versions, `ConcurrentModificationException`).

2. **`LendingService.borrowBook()`** has a check-then-act race:
   ```java
   if (loanRepository.isBookOnLoan(bookId)) {   // Thread A checks: false
       throw new IllegalStateException(...);
   }
   // Thread B checks: also false (hasn't been saved yet)
   loanRepository.save(memberId, bookId);        // Both threads save — book double-loaned
   ```

These bugs are invisible in single-threaded tests. They show up in production under load,
intermittently, and are extremely difficult to reproduce. That's what makes concurrency
hard — not the API, but the reasoning.

**Immutability as your first defence:**

Your existing design choices already help:

- `BookId`, `MemberId` are records — immutable, always thread-safe
- `Book` has `final` fields — once constructed, safe to share across threads
- `List.of()`, `Map.of()` return immutable collections — safe to read from any thread

The problems arise where you have *mutable shared state*: the `HashMap` inside
`InMemoryLoanRepository`, and the multi-step read-then-write in `LendingService`.

**Three levels of thread safety to think about:**

1. **Object-level safety:** Is a single object safe to use from multiple threads?
   (`HashMap` is not, `ConcurrentHashMap` is)
2. **Operation-level safety:** Is a single method call atomic? (`ConcurrentHashMap.put()`
   is, but check-then-put is not)
3. **Business-logic-level safety:** Is a multi-step operation consistent?
   (`isBookOnLoan()` + `save()` is not atomic even if the repository is thread-safe)

This section tackles all three levels.

---

## 4.2 — Threading & Shared State

### Read First

**Creating threads** (for understanding — you rarely do this directly):
```java
Thread thread = new Thread(() -> {
    System.out.println("Running on: " + Thread.currentThread().getName());
});
thread.start();  // starts a new OS thread
thread.join();   // blocks until the thread finishes
```

`Thread.start()` is truly parallel — the lambda runs on a different CPU core. `thread.join()`
is a blocking wait (≈ `await` in TS, but it blocks the thread instead of yielding).

**`synchronized` — mutual exclusion:**

The simplest way to make code thread-safe. Only one thread can execute a `synchronized`
block at a time:

```java
synchronized (lockObject) {
    // only one thread in here at a time
    var current = map.get(key);
    map.put(key, current + 1);
}
```

Or on a method — locks on `this`:
```java
public synchronized void borrowBook(MemberId memberId, BookId bookId) {
    // entire method body is mutually exclusive
}
```

**The TS comparison:** There's no equivalent in TS because you never need it — single
thread means no concurrent access. In C#, `lock(obj) { ... }` is the same concept as
`synchronized(obj) { ... }`.

**Thread-safe collections:**

| Unsafe | Safe replacement | Notes |
|--------|-----------------|-------|
| `HashMap` | `ConcurrentHashMap` | Lock-striped, highly concurrent reads |
| `ArrayList` | `CopyOnWriteArrayList` | Copies on every write — good for read-heavy |
| `HashSet` | `ConcurrentHashMap.newKeySet()` | Backed by ConcurrentHashMap |

`ConcurrentHashMap` is the workhorse. It allows multiple threads to read simultaneously
and uses fine-grained locking for writes. But it only makes *individual operations* atomic —
compound operations (check-then-act) still need external synchronization.

**`ConcurrentHashMap` compound operations:**

```java
// NOT atomic — race condition between get and put:
if (!map.containsKey(key)) {
    map.put(key, value);
}

// Atomic — single operation:
map.putIfAbsent(key, value);
map.computeIfAbsent(key, k -> new ArrayList<>());
map.compute(key, (k, v) -> v == null ? 1 : v + 1);
```

These atomic compound operations are the key to writing lock-free concurrent code. You
already use `computeIfAbsent` in your repository — that individual call is safe. But the
surrounding logic may not be.

**`CountDownLatch` — coordinating threads in tests:**

The main challenge in concurrency testing is *timing*. You need multiple threads to hit the
critical section simultaneously. `CountDownLatch` is your tool:

```java
int threadCount = 10;
var latch = new CountDownLatch(1);           // gate: initially closed
var done = new CountDownLatch(threadCount);  // completion signal

for (int i = 0; i < threadCount; i++) {
    new Thread(() -> {
        try {
            latch.await();    // all threads block here
            // do the concurrent work
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            done.countDown(); // signal completion — always, even if work throws
        }
    }).start();
}

latch.countDown();  // open the gate — all threads start simultaneously
done.await();       // wait for all to finish
```

**Checked exceptions inside lambdas:** `latch.await()` throws `InterruptedException`, which
is a checked exception. Normally you'd add `throws InterruptedException` to the method
signature — but here the lambda targets `Runnable`, whose `run()` method doesn't declare any
checked exceptions. You *must* catch it inside the lambda. The `Thread.currentThread().interrupt()`
re-sets the interrupt flag so callers further up know the thread was interrupted — this is
the standard idiom. The outer `done.await()` on the test method *can* use `throws
InterruptedException` because it's directly in the test method body, not inside a lambda.

This pattern maximises the chance of threads colliding on the critical section. Without the
latch, threads start sequentially and the race window is tiny.

**`AtomicInteger`, `AtomicReference` — lock-free counters and references:**

```java
AtomicInteger counter = new AtomicInteger(0);
counter.incrementAndGet();                    // atomic increment
counter.compareAndSet(expected, newValue);    // CAS — the foundation of lock-free code
```

Useful for simple counters and flags. When you need to protect a complex multi-step
operation, `synchronized` or explicit locks are clearer.

**Watch out:** Race conditions are non-deterministic. A test that passes 99 out of 100 runs
still has a bug. Run concurrency tests in a loop (e.g., 100 iterations) to increase
confidence. Even then, some races only show up under specific CPU scheduling — CI machines
with different core counts may behave differently.

### Exercise 4.2a — Exposing the race in InMemoryLoanRepository

Create `src/test/java/org/example/loan/ConcurrentLoanRepositoryTest.java`.

Your goal is to write a test that *demonstrates* the race condition in
`InMemoryLoanRepository`. The repository uses `HashMap` — concurrent writes can corrupt it.

1. **Concurrent saves from multiple threads:**
   - Create an `InMemoryLoanRepository`
   - Spin up 10+ threads, each saving a different book for the same member
   - Use a `CountDownLatch` to ensure all threads start simultaneously
   - After all threads complete, check `findBooksByMember()` — are all books present?
   - Run this test in a loop (e.g., 100 times). Some runs should lose entries or throw
     `ConcurrentModificationException`

2. **Concurrent saves for different members:**
   - Same setup, but each thread saves for a *different* member
   - After all threads complete, verify each member has exactly one book
   - This can also fail — `HashMap.computeIfAbsent()` is not atomic on a plain `HashMap`

**Hints:**
- Import `java.util.concurrent.CountDownLatch`
- Use `Thread::start` and `Thread::join` (or `ExecutorService` if you prefer)
- Declare `throws InterruptedException` on your test methods — `latch.await()` and
  `thread.join()` throw it
- The test might not fail every time. That's the nature of race conditions. If you want to
  increase the chance, run the concurrent block in a for-loop inside the test
- Don't assert on specific failure modes (exception type, number of lost entries) — just
  assert on the expected outcome and observe that it sometimes fails

**Watch out:** If the test passes consistently, you might not have enough contention. Try
more threads (50+), more iterations, or add a small `Thread.yield()` between the check and
the save to widen the race window.

### Exercise 4.2b — Thread-safe InMemoryLoanRepository

Fix `InMemoryLoanRepository` to be thread-safe. You have two main approaches — pick one
and justify your choice:

**Option A — `ConcurrentHashMap`:**
Replace `HashMap` with `ConcurrentHashMap`. Individual operations (`computeIfAbsent`,
`getOrDefault`) become atomic. But think about `delete` — is
`getOrDefault(...).remove(bookId)` atomic? What about `allBooksOnLoan()` iterating the map
while another thread writes?

**Option B — `synchronized`:**
Add `synchronized` to every public method. Simple and correct, but all operations are
serialised — only one thread can do anything at a time.

1. **Apply your chosen fix** to `InMemoryLoanRepository`
2. **Re-run the test from 4.2a** — it should now pass consistently
3. **Run the contract tests** — make sure you haven't broken anything

**Hints:**
- If you go with `ConcurrentHashMap`, also wrap the value lists in a thread-safe structure
  (e.g., `CopyOnWriteArrayList`, or `Collections.synchronizedList(new ArrayList<>())`)
- If you go with `synchronized`, consider whether you synchronize on `this` or on the map.
  Both work — `this` is simpler
- Think about which approach `FileLoanRepository` would need. File I/O has its own
  atomicity challenges — but that's out of scope for now

**Watch out:** `ConcurrentHashMap` makes individual operations safe, but compound
operations spanning multiple calls are still unsafe. If your `delete` reads the list and
then modifies it in two steps, you have a race even with `ConcurrentHashMap`. The
`compute` and `merge` methods are your tools for atomic compound operations.

### Exercise 4.2c — The check-then-act race in LendingService

The repository is now thread-safe, but `LendingService.borrowBook()` still has a race:

```java
if (loanRepository.isBookOnLoan(bookId)) {   // check
    throw new IllegalStateException(...);
}
loanRepository.save(memberId, bookId);        // act
```

Two threads can both pass the check and both save — double-loaning the same book.

Create `src/test/java/org/example/lending/ConcurrentLendingTest.java`:

1. **Expose the race:**
   - Create a `LendingService` with a thread-safe `InMemoryLoanRepository`
   - Spin up 10 threads, all trying to borrow the *same book* for different members
   - Use a `CountDownLatch` so they all call `borrowBook()` simultaneously
   - After all threads finish: exactly one should have succeeded, the rest should have
     thrown `IllegalStateException`
   - Count the actual successes. Without a fix, you'll see more than one

2. **Fix the race:**
   - The simplest fix: add `synchronized` to `borrowBook()` (and `returnBook()`, which has
     a similar read-then-write pattern)
   - Re-run the test — exactly one thread should succeed now

3. **Think about the design:**
   - Should the synchronization live in `LendingService` or in `LoanRepository`?
   - The repository can't own this — the check-then-act spans two repository calls, and
     the business rule ("a book can only be loaned once") belongs to the service
   - This is a key insight: thread-safe data structures don't make your *logic* thread-safe.
     Synchronization must happen at the level where the invariant is enforced

**Hints:**
- To count successes, use an `AtomicInteger` — increment it in each thread when
  `borrowBook()` doesn't throw. After all threads complete, assert the count is 1
- Catch `IllegalStateException` in the thread body — that's the expected failure for
  threads that lose the race
- For the fix, `synchronized(this)` on `borrowBook` and `returnBook` is sufficient. A
  `ReentrantLock` is the more flexible alternative if you want to explore it — but
  `synchronized` is fine
- Import `java.util.concurrent.atomic.AtomicInteger`

---

## 4.3 — `java.util.concurrent`

### Read First

Raw threads and `synchronized` are low-level. In practice, you use the
`java.util.concurrent` abstractions:

**`ExecutorService` — managed thread pools:**

Instead of creating threads manually, submit tasks to a pool:

```java
ExecutorService executor = Executors.newFixedThreadPool(4);

Future<String> future = executor.submit(() -> {
    // runs on a pool thread
    return "result";
});

String result = future.get();  // blocks until done (throws checked exceptions)
executor.shutdown();           // stop accepting tasks, finish running ones
```

`ExecutorService` manages thread lifecycle — creation, reuse, and shutdown. You submit
`Callable<T>` (returns a value) or `Runnable` (no return value).

**`Future<T>` — a handle to an async result:**

```java
Future<String> future = executor.submit(() -> computeExpensiveResult());
// do other work...
String result = future.get();           // block until ready
String result = future.get(5, TimeUnit.SECONDS);  // block with timeout
```

`Future.get()` is a blocking call — like `await` but it freezes the thread. This is fine
when you have a thread pool, but don't call `.get()` on the event loop thread in a UI
framework.

**`CompletableFuture<T>` — the Promise equivalent:**

This is what you'll actually use. It maps closely to TS `Promise`:

| TypeScript | Java |
|-----------|------|
| `Promise.resolve(value)` | `CompletableFuture.completedFuture(value)` |
| `new Promise((resolve) => { ... })` | `CompletableFuture.supplyAsync(() -> { ... })` |
| `.then(x => transform(x))` | `.thenApply(x -> transform(x))` |
| `.then(x => anotherPromise(x))` | `.thenCompose(x -> anotherFuture(x))` |
| `.catch(err => fallback)` | `.exceptionally(err -> fallback)` |
| `Promise.all([p1, p2, p3])` | `CompletableFuture.allOf(cf1, cf2, cf3)` |
| `await promise` | `future.join()` |

Key differences from Promises:

1. **Execution context:** Promises auto-schedule on the microtask queue. `supplyAsync()`
   runs on `ForkJoinPool.commonPool()` by default, or you can pass an explicit executor:
   `supplyAsync(() -> ..., myExecutor)`.

2. **`join()` vs `get()`:** Both block. `join()` throws unchecked `CompletionException`.
   `get()` throws checked `ExecutionException`. Prefer `join()` in most code.

3. **Composition is explicit:** TS awaits Promises automatically in `async` functions.
   Java requires explicit `.thenApply()` / `.thenCompose()` chaining, or blocking with
   `.join()`.

**Combining futures:**

```java
CompletableFuture<List<Book>> searchFiction = CompletableFuture.supplyAsync(
    () -> catalog.booksWithGenre(BookGenre.FICTION)
);
CompletableFuture<List<Book>> searchByAuthor = CompletableFuture.supplyAsync(
    () -> catalog.booksBy("Orwell")
);

// Wait for both and combine results
CompletableFuture.allOf(searchFiction, searchByAuthor).join();
List<Book> fiction = searchFiction.join();
List<Book> byAuthor = searchByAuthor.join();
```

**`allOf` returns `CompletableFuture<Void>`** — it doesn't give you the results directly.
You call `.join()` on each individual future after `allOf` completes. This is less ergonomic
than TS's `Promise.all()` which returns an array of results. It's a common pain point.

**Error handling:**

```java
CompletableFuture.supplyAsync(() -> riskyOperation())
    .thenApply(result -> transform(result))
    .exceptionally(ex -> {
        // ex is the Throwable — handle or return a fallback
        return fallbackValue;
    });
```

Exceptions propagate through the chain, just like rejected Promises. `exceptionally()`
catches them. `handle()` receives both the result (if successful) and the exception (if
failed) — like a combined `.then()` and `.catch()`.

**Watch out:** `CompletableFuture` chains can silently swallow exceptions if you never call
`.join()`, `.get()`, or `.exceptionally()`. Unlike Promises, there's no unhandled rejection
warning. Always terminate a chain with something that observes the result.

### Exercise 4.3a — CompletableFuture basics

Create `src/test/java/org/example/CompletableFutureTest.java`.

1. **supplyAsync + thenApply:**
   - Use `supplyAsync` to compute a value on another thread. Chain `.thenApply()` to
     transform it. Call `.join()` and assert the result.

2. **thenCompose (flatMap):**
   - Write two async steps where the second depends on the first's result. Use
     `.thenCompose()` (not `.thenApply()` — that would give you a nested
     `CompletableFuture<CompletableFuture<T>>`).
   - Assert the final result.

3. **allOf — parallel independent operations:**
   - Launch 3 independent `supplyAsync` calls. Use `allOf` to wait for all of them. Collect
     the results. Assert all are present.

4. **exceptionally:**
   - Create a future that throws. Chain `.exceptionally()` to provide a fallback. Assert the
     fallback value is returned.

**Hints:**
- `CompletableFuture.supplyAsync(() -> ...)` returns `CompletableFuture<T>`
- `.join()` blocks and returns the value (or throws `CompletionException` on failure)
- For `allOf`, remember it returns `Void` — you need to `.join()` each individual future
  to get the values
- Import from `java.util.concurrent.CompletableFuture`

### Exercise 4.3b — Parallel catalog operations

Create `src/test/java/org/example/book/ConcurrentCatalogTest.java`.

Your `Catalog` has several query methods. In a real application, you might want to run
multiple independent queries in parallel — for example, building a dashboard that shows
genre counts, a specific author's books, and search results simultaneously.

1. **Parallel queries:**
   - Create a `Catalog` with a decent number of books (use your `TestBooks` fixture or
     build a larger one)
   - Run three queries concurrently using `CompletableFuture.supplyAsync()`:
     - `catalog.booksBy("some author")`
     - `catalog.booksWithGenre(BookGenre.FICTION)`
     - `catalog.search("some query")`
   - Collect all results after all three complete
   - Assert each result is correct (same as if you'd run them sequentially)

2. **Parallel with executor:**
   - Create a fixed thread pool with `Executors.newFixedThreadPool(3)`
   - Pass it as the second argument to `supplyAsync()`
   - Run the same queries. Assert the same results.
   - **Shut down the executor** after the test — use try-with-resources
     (`ExecutorService` implements `AutoCloseable` in Java 19+)

3. **Timed comparison (optional):**
   - Add a `Thread.sleep(100)` inside each catalog method call (simulating slow I/O)
   - Time the sequential version vs the parallel version
   - The parallel version should complete in ~100ms, the sequential version in ~300ms
   - Use `System.nanoTime()` or `Instant.now()` for timing

**Hints:**
- `Catalog` is already thread-safe — it holds an immutable `List<Book>` and its methods
  create new streams each time. No mutation, no shared state, safe to call from any thread.
  This is the immutability payoff.
- For the executor shutdown, `executor.close()` (Java 19+) or the traditional
  `executor.shutdown()` + `executor.awaitTermination()` pattern both work
- The timing test is for intuition, not a strict assertion — don't assert on exact
  milliseconds, just that parallel is faster than sequential

**Watch out:** For an in-memory `Catalog` with a small dataset, parallel execution is
*slower* than sequential — the overhead of thread scheduling dwarfs the computation time.
Parallelism pays off when individual operations are slow (I/O, network calls, heavy
computation). The sleep-based test makes this visible.

---

## 4.4 — Virtual Threads

### Read First

Traditional Java threads (now called *platform threads*) are expensive. Each one maps 1:1
to an OS thread, consuming ~1MB of stack memory. A server with 10,000 concurrent requests
needs 10,000 threads — that's 10GB just for stacks. This is why Java developed thread
pools, reactive frameworks (Project Reactor, RxJava), and complex async patterns.

**Virtual threads change everything.** Introduced in Java 21, virtual threads are
lightweight, JVM-managed threads that don't map 1:1 to OS threads. You can create millions
of them:

```java
Thread.ofVirtual().start(() -> {
    // runs on a virtual thread
    System.out.println(Thread.currentThread());
});
```

**The executor shortcut — what you'll use in practice:**

```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -> handleRequest(request1));
    executor.submit(() -> handleRequest(request2));
    // each task gets its own virtual thread — no pooling, no limit config
}
```

No pool sizing, no tuning. One virtual thread per task. The JVM schedules them onto a small
number of platform threads (called *carrier threads*) under the hood.

**Why this matters:**

Before virtual threads, you had two options for high-concurrency Java:
1. **Thread pool** — limited threads, blocking I/O wastes them, need to tune pool sizes
2. **Reactive** (Project Reactor, RxJava) — non-blocking, but completely changes your
   programming model (like going from sync to async/await, but worse — callback chains
   everywhere)

Virtual threads give you a third option: write simple, blocking code (read file, call
database, wait for response) and the JVM handles the scheduling. When a virtual thread
blocks on I/O, the JVM parks it and runs another virtual thread on the same carrier thread.
It's like Node.js's event loop, but you write synchronous code.

**The TS comparison:**

| Concept | TypeScript | Java (virtual threads) |
|---------|-----------|----------------------|
| Concurrency model | Event loop + async/await | Virtual threads + blocking I/O |
| Cost per concurrent task | ~cheap (just a callback) | ~cheap (virtual thread, ~few KB) |
| Programming style | Must use `async/await` | Write normal synchronous code |
| I/O during wait | Event loop handles other callbacks | JVM schedules other virtual threads |

The key insight: virtual threads let you write code that *looks* sequential but *behaves*
concurrently. This is what TS does with `async/await`, but without the function coloring
problem — you don't need to mark methods as `async` or return `Promise`.

**Thread safety still applies:**

Virtual threads are still threads. They share memory, they can race, they need
synchronization. Everything from 4.2 still applies. The only thing that changed is the
*cost* of creating threads — the *rules* of concurrent access are identical.

**`synchronized` and virtual threads — a caveat:**

When a virtual thread enters a `synchronized` block, it *pins* to its carrier thread — no
other virtual thread can use that carrier until the `synchronized` block exits. This isn't
a correctness issue, but it reduces concurrency. If you have long-running `synchronized`
blocks with I/O inside them, consider using `ReentrantLock` instead, which doesn't pin.

For the exercises in this course, this pinning effect is negligible. But it's worth knowing
for production code with high concurrency.

**Structured concurrency (preview):**

Java is also developing `StructuredTaskScope` — a way to treat a group of concurrent tasks
as a unit (start together, fail together, cancel together). The API is still evolving and
may change. It's worth knowing it exists, but we won't build exercises around it.

### Exercise 4.4a — Virtual threads in action

Create `src/test/java/org/example/VirtualThreadTest.java`.

1. **Create virtual threads:**
   - Spin up 1,000 virtual threads, each incrementing an `AtomicInteger`
   - Wait for all to complete. Assert the counter equals 1,000
   - Note how fast this is — try the same with `Thread.ofPlatform()` and feel the
     difference (or don't — 1,000 platform threads is still fine, but try 100,000)

2. **Virtual thread executor with catalog queries:**
   - Use `Executors.newVirtualThreadPerTaskExecutor()` to submit 100 catalog search tasks
   - Each task searches for a different query
   - Collect all `Future` results. Assert each returned the expected books
   - Use try-with-resources for the executor

3. **Verify thread names:**
   - Inside a virtual thread, call `Thread.currentThread().isVirtual()` — assert it's true
   - Compare `Thread.currentThread().toString()` for virtual vs platform threads — virtual
     threads have names like `VirtualThread[#42]/runnable`

**Hints:**
- `Thread.ofVirtual().start(runnable)` creates and starts a virtual thread in one call.
  It returns a `Thread` — call `.join()` to wait for it
- For 1,000 threads, create a `List<Thread>` and join them all at the end
- `Executors.newVirtualThreadPerTaskExecutor()` is `AutoCloseable` — use
  try-with-resources and it will wait for all tasks to complete on close
- Import `java.util.concurrent.Executors`, `java.util.concurrent.atomic.AtomicInteger`

### Exercise 4.4b — Virtual threads meet the repository

Create `src/test/java/org/example/loan/VirtualThreadLoanTest.java`.

This exercise verifies that your thread-safe `InMemoryLoanRepository` (from 4.2b) and
synchronized `LendingService` (from 4.2c) work correctly under high virtual-thread
concurrency.

1. **High-concurrency borrowing:**
   - Use `newVirtualThreadPerTaskExecutor()` to submit 100 borrow requests, each for a
     different book and member
   - After all complete, verify all 100 books are on loan, each to the correct member

2. **Contention test:**
   - Submit 50 virtual threads, all trying to borrow the *same book*
   - Exactly one should succeed. The rest should throw `IllegalStateException`
   - This is the same test as 4.2c, but with virtual threads instead of platform threads —
     it should behave identically

3. **Mixed operations:**
   - Submit a mix of borrow and return operations concurrently
   - After all complete, verify the repository state is consistent
   - This tests that `synchronized` on `LendingService` correctly serializes both
     `borrowBook` and `returnBook`

**Hints:**
- Use `executor.submit(() -> { ... })` and collect the `Future` results
- To distinguish success from expected failure, catch `IllegalStateException` inside the
  submitted lambda and use an `AtomicInteger` to count successes
- For the mixed operations test, borrow some books first (sequentially), then concurrently
  submit returns for some and borrows for others
- All of these tests exercise the same synchronization you built in 4.2 — if those tests
  pass, these should too. If they don't, you have a bug in your synchronization

---

## Review

After completing Phase 4, your library should have:

- [ ] Understanding of Java's threading model vs TS's event loop
- [ ] `InMemoryLoanRepository` upgraded to be thread-safe (ConcurrentHashMap or synchronized)
- [ ] `LendingService` with synchronized borrowing/returning to prevent check-then-act races
- [ ] Tests that *demonstrate* race conditions before fixes (even if intermittent)
- [ ] Tests that verify thread safety under concurrent access
- [ ] Comfort with `CompletableFuture` as Java's `Promise` equivalent
- [ ] Experience with virtual threads and `newVirtualThreadPerTaskExecutor()`
- [ ] All existing tests still passing (contract tests, Phase 1–3 tests)

**Consider:**
1. How does the concurrency model affect the way you design objects? Your records and final
   fields were already thread-safe — that's not an accident. Immutability is the simplest
   concurrency strategy.
2. Where does synchronization belong? The repository owns its own data structure safety.
   The service owns the business rule atomicity. Putting synchronization at the wrong level
   either doesn't fix the bug or kills performance.
3. When would you use `CompletableFuture` vs virtual threads? CompletableFuture is for
   composing async results (fan-out, combine, transform). Virtual threads are for
   simplifying blocking code that would otherwise need complex async patterns. They're
   complementary, not competing.
4. How does this compare to your experience in TS? The event loop model avoids all of
   these problems by disallowing parallelism. Java trades that simplicity for true
   multi-core performance. Which trade-off is right depends on the workload.