# Phase 7 — Order Management & Gateway

> **This isn't a structured phase.** You have enough Java fluency to build independently.
> This guide provides architecture context, exercise prompts, and signposts — not a
> curriculum. Work through it at your own pace, skip what's obvious, dig into what isn't.
>
> **Two modules in parallel:** Order Management is the richest domain — event sourcing with
> a hand-built infrastructure that keeps the domain pure. The Gateway introduces
> authentication and authorization with Spring Security + JWT. They're independent enough to
> interleave.
>
> **Everything lives in the e-commerce repo.** Code goes in the `order/` and `gateway/`
> modules (you may need to add `gateway` to `settings.gradle.kts`).
>
> **Infrastructure approach:** In-memory implementations first. Drive the design through
> the use cases, outside-in. PostgreSQL implementations come later, behind the same port
> interfaces — same pattern as the library project's `InMemoryLoanRepository` →
> `FileLoanRepository`, and the Catalog's `InMemoryProducts` → `PostgresProducts`.
>
> **Testing approach:** Same as Phase 6 — outside-in. Start from the use case handler test.
> The handler test drives the aggregate, events, event store port, and value objects into
> existence. Don't build domain types in isolation first.

---

## Part A — Order Management (Event Sourcing)

### 7.1 — Event Sourcing: The Mental Model Shift

#### Read First

**What changes from Catalog:**

The Catalog is CQRS without event sourcing — commands update state directly, queries read
current state. The Order module adds event sourcing: **state is derived from a sequence of
events**, not stored directly.

```
Catalog (CQRS, no ES):
  Command → Handler → mutate entity → save to DB

Order (CQRS + ES):
  Command → Handler → load events → reconstitute aggregate
          → call domain method → event returned
          → append to event store → publish to projections
```

**The TS/C# comparison:**

If you've used Redux or NgRx, event sourcing is the same idea applied to persistence:

| Concept | Redux/NgRx | Event Sourcing |
|---------|-----------|----------------|
| State container | Store | Aggregate |
| State change | Action (dispatched) | Event (applied) |
| State derivation | Reducer (pure function) | Reconstitution (fold events over initial state) |
| Current state | `store.getState()` | Replay all events through handlers |
| Read optimization | Selectors / derived state | Projections (separate read models) |
| Side effects | Effects / Epics | Process managers (long-running workflows) |

**Why event source the Order?**

Orders have a rich lifecycle: placed → confirmed → paid → shipped → delivered (or cancelled
at various points). Each transition matters for business reasons — audit trail, compensation
on failure, analytics. Storing the full event history is natural here. The Catalog doesn't
need this — a product's current price is all that matters.

**Why build your own?**

The Java ecosystem's event sourcing frameworks (Axon, OpenCQRS) want to own the aggregate
lifecycle, command routing, and handler discovery — infrastructure reaching into your domain.
Your domain already has its own mediator, its own command/query separation, its own hexagonal
boundaries. What you actually need is:

1. An **event store** — append events, load events by aggregate ID, optimistic locking
2. A **projection delivery mechanism** — notify read-model updaters when new events arrive
3. An **aggregate reconstitution pattern** — fold events into state

These are small, well-defined infrastructure ports. Building them keeps the domain pure and
teaches you the machinery.

**The event store interface:**

An event store does two things: **append** events to a stream (identified by aggregate ID)
and **load** all events for a stream. Optimistic locking ensures that concurrent writers
to the same stream don't silently overwrite each other.

Each event in a stream has a **sequence number** (0, 1, 2, ...). When appending, the caller
passes the **expected version** — the sequence number of the last event it saw. If the
store's current version doesn't match, someone else wrote first and the append is rejected.

```
Stream: order-abc123
  [0] OrderPlacedEvent
  [1] OrderConfirmedEvent
  [2] OrderPaidEvent       ← current version: 2

Append(streamId, expectedVersion=2, [OrderShippedEvent])
  → succeeds, version now 3

Concurrent append(streamId, expectedVersion=2, [OrderCancelledEvent])
  → rejected — expected 2, actual is now 3
```

This is like an optimistic concurrency check on a database row version, or an ETag-based
conditional PUT.

**Event envelope:**

Events in the store are wrapped in an envelope that carries metadata:

```
EventEnvelope
├── streamId (aggregate ID)
├── sequenceNumber (position in the stream)
├── eventType (discriminator for deserialization)
├── payload (the domain event, serialized)
├── occurredAt (timestamp)
```

The domain event (`OrderPlacedEvent`) is the payload. The envelope is an infrastructure
concern — the domain never sees it.

**Aggregate as pure Java:**

No annotations, no framework base class. An aggregate is a plain class that:
- Has a static factory that creates from a command and returns an event
- Has instance methods that handle subsequent commands and return an event
- Has a static reconstitution method that folds events over initial state
- Never calls framework methods — command methods return an `OrderEvent`, the caller appends

**Watch out:** The simplicity of the pattern is deceptive. The happy path is straightforward.
The subtlety is in: optimistic locking (two concurrent commands on the same aggregate),
projection delivery guarantees (at-least-once, idempotent handlers), and process manager
state tracking. Each of these has a clean solution — just don't skip them.

#### Exercise 7.1a — Write the ADRs

Before writing code, document the decisions.

1. **Create `docs/adr/0011-event-sourcing-for-order.md`:**
   - Context: Order has a rich lifecycle with business-significant transitions
   - Decision: Event source the Order aggregate
   - Consequence: Events are the source of truth, state is derived, full audit trail

2. **Create `docs/adr/0012-custom-event-sourcing-infrastructure.md`:**
   - Context: Axon and similar frameworks own aggregate lifecycle and command routing, which
     violates the hexagonal boundary — infrastructure reaching into domain and application
     layers
   - Decision: Build event sourcing infrastructure as port interfaces with in-memory and
     PostgreSQL implementations. Aggregates are pure Java. Command handlers use the existing
     mediator.
   - Consequence: More code to write and maintain. Full control over the domain model. Event
     store, projection delivery, and aggregate reconstitution are infrastructure concerns
     behind ports.

---

### 7.2 — Place an Order (First Use Case, Outside-In)

#### Read First

**The outside-in flow:**

Start from the use case test. The test for `PlaceOrderHandler` will pull everything into
existence:

```
Test PlaceOrderHandler
  → needs PlaceOrderCommand (create it)
  → needs EventStore port (define it)
  → needs InMemoryEventStore (create it)
  → needs EventPublisher port (define it)
  → needs InMemoryEventPublisher (create it)
  → handler calls Order.place(command)
      → needs Order class, OrderEvent, OrderPlacedEvent
      → needs OrderId, CustomerId, OrderLine, Money
  → handler wraps result in EventEnvelopes, appends to store, publishes
      → needs EventEnvelope, ConcurrentStreamModificationException
  → assert: event store contains an OrderPlacedEvent for this stream
  → assert: publisher received the event
```

You'll work through compiler errors one at a time, creating types as the test demands —
same approach as Phase 6's `AddToCatalogHandler`.

**The handler's shape:**

```java
public class PlaceOrderHandler implements CommandHandler<PlaceOrderCommand, Void> {
    private final EventStore eventStore;
    private final EventPublisher publisher;

    public Void handle(PlaceOrderCommand command) {
        var event = Order.place(command);

        var streamId = streamIdFor(command.orderId());
        var envelope = wrap(streamId, 0, event);  // sequence 0 = first event
        eventStore.append(streamId, -1, envelope);  // -1 = new stream
        publisher.publish(envelope);
        return null;
    }
}
```

The handler is the orchestrator: it calls the domain, wraps the result in an infrastructure
envelope, and persists. The aggregate never sees the event store or publisher.

**One command, one event:** Each command method on the aggregate returns a single
`OrderEvent`, not a list. A command represents one decision, and that decision is one fact.
If a command appears to produce two events, either they're one event with richer data, or
the second is a reaction that belongs in a process manager.

#### Exercise 7.2a — Test PlaceOrderHandler

Write the handler test first. Work through compiler errors to create each type as needed.

1. **Write the test:**
   - Construct a `PlaceOrderCommand` with an order ID, customer ID, and a list of order
     lines
   - Create `InMemoryEventStore` and `InMemoryEventPublisher`
   - Create `PlaceOrderHandler` with those dependencies
   - Call `handler.handle(command)`
   - Assert: event store stream for this order contains one event, and it's an
     `OrderPlacedEvent` with the correct fields
   - Assert: publisher received the same event

2. **Work through the types the test needs to compile (in the order you'll hit them):**

   **Command and value objects** (in `placeorder/` and `order/`):
   - `PlaceOrderCommand` — record: `OrderId`, `CustomerId`, `List<OrderLine>`
   - `OrderId` — record wrapping UUID, with `generate()` factory
   - `CustomerId` — record wrapping UUID
   - `OrderLine` — record: `ProductId`, `ProductName`, `Money`, `int quantity`
     (validate quantity > 0, `lineTotal()` method)

   **Event store port** (in `common` or a shared `eventsourcing` package):
   - `EventStore` — interface: `append(String streamId, long expectedVersion,
     EventEnvelope event)`, `load(String streamId) → EventStream`
   - `EventStream` — record: `List<EventEnvelope> events`, `long version` (-1 for empty)
   - `EventEnvelope` — record: `streamId`, `sequenceNumber`, `eventType`, `payload`
     (Object), `occurredAt`
   - `ConcurrentStreamModificationException`

   **Event publisher port:**
   - `EventPublisher` — interface: `publish(EventEnvelope event)`

   **In-memory fakes** (test source):
   - `InMemoryEventStore` — `Map<String, List<EventEnvelope>>`, checks expected version
     against current list size, throws on mismatch, appends the single envelope
   - `InMemoryEventPublisher` — collects published events, exposes for assertions

   **Domain** (in `order/`):
   - `OrderEvent` — sealed interface
   - `OrderPlacedEvent` — record: `OrderId`, `CustomerId`, `List<OrderLine>`,
     `Instant occurredAt`
   - `Order.place(PlaceOrderCommand)` → `OrderEvent` — static factory, validates
     non-empty items, returns an `OrderPlacedEvent`

3. **Make the test pass.**

**Hints:**
- Stream ID convention: `"order-" + orderId.value()`
- Expected version -1 means "stream doesn't exist yet" — the first append creates it
- The `InMemoryEventStore` is a real fake with real behavior (version checking), not a
  stub. Thread safety: `synchronized` or `ConcurrentHashMap` with locking.
- `Order.place()` is a static method — no existing aggregate to reconstitute for creation
- The handler wraps the domain event in an `EventEnvelope` — adding stream ID, sequence
  number 0, event type (simple class name), and `Instant.now(clock)`
- Don't create types before the test needs them. Let the compiler guide you.

#### Exercise 7.2b — Test the event store directly

Now that `InMemoryEventStore` exists, test its behavior in isolation — these become the
contract tests when you add `PostgresEventStore` later.

1. **Test scenarios:**
   - Append to new stream (expected version -1) → succeeds
   - Load → returns events in order with correct sequence numbers
   - Append with correct expected version → succeeds, version incremented
   - Append with wrong expected version → throws
     `ConcurrentStreamModificationException`
   - Load empty/nonexistent stream → empty list, version -1
   - Multiple streams don't interfere with each other

2. **Structure these as an abstract contract test** (`EventStoreContract`) with an abstract
   `eventStore()` method — same pattern as `ProductsContractTest` in the Catalog.
   `InMemoryEventStoreTest` extends it and provides the fake.

---

### 7.3 — Confirm, Pay, Ship (Subsequent Commands)

#### Read First

**What changes for subsequent commands:**

Creation was a special case — no existing stream. For all subsequent commands, the handler
must:

1. **Load** events from the store
2. **Reconstitute** the aggregate from those events
3. **Call** the command method on the aggregate (returns a single event)
4. **Append** with the current version as expected version
5. **Publish**

This is where `Order.reconstitute(events)` comes in — a left fold over the event history
that rebuilds the aggregate's state.

**The `apply` dispatch:**

`reconstitute` needs to dispatch each event to the right state-mutation logic. Since
`OrderEvent` is sealed, use pattern matching:

```java
private void apply(OrderEvent event) {
    switch (event) {
        case OrderPlacedEvent e -> { /* set orderId, status, items */ }
        case OrderConfirmedEvent e -> { /* set status */ }
        // exhaustive — sealed interface guarantees it
    }
}
```

#### Exercise 7.3a — Test ConfirmOrderHandler

1. **Write the test:**
   - Seed the event store with an `OrderPlacedEvent` in the stream (simulating an order
     that was already placed)
   - Create `ConfirmOrderHandler`, call `handle(new ConfirmOrderCommand(orderId))`
   - Assert: event store stream now contains `[OrderPlacedEvent, OrderConfirmedEvent]`
   - Assert: publisher received only the new `OrderConfirmedEvent`

2. **Work through what's needed:**
   - `ConfirmOrderCommand` — record: `OrderId`
   - `OrderConfirmedEvent` — record: `OrderId`, `Instant occurredAt`
   - `Order.reconstitute(List<OrderEvent>)` — static method, folds events via `apply`
   - `Order.confirm()` → `OrderEvent` — checks status is PLACED, returns event
   - `OrderStatus` enum — `PLACED`, `CONFIRMED`, `PAID`, `SHIPPED`, `DELIVERED`,
     `CANCELLED`

3. **Test the invalid transition:**
   - Seed with `[OrderPlacedEvent, OrderConfirmedEvent]` (already confirmed)
   - Call confirm → throws (aggregate rejects it)
   - Assert: event store unchanged, publisher received nothing

**Hints:**
- To seed the event store, call `eventStore.append(streamId, -1, envelopes)` in test setup
- The handler loads the stream, extracts domain events from envelopes, passes them to
  `Order.reconstitute()`, then calls the domain method
- `reconstitute` creates a blank `Order` (private constructor) and applies each event
- The confirm handler and place handler share the load → reconstitute → act → append →
  publish pattern. You'll feel the duplication — resist extracting until 7.3c.

#### Exercise 7.3b — Remaining commands

For each, write the handler test first, then build the command, event, and domain method.

1. **PayOrderHandler:**
   - Seed: `[Placed, Confirmed]` → pay → `OrderPaidEvent(OrderId, Money, Instant)`
   - Invalid: pay when not confirmed → throws

2. **ShipOrderHandler:**
   - Seed: `[Placed, Confirmed, Paid]` → ship → `OrderShippedEvent(OrderId, String
     trackingNumber, Instant)`
   - Invalid: ship when not paid → throws

3. **DeliverOrderHandler:**
   - Seed: `[Placed, Confirmed, Paid, Shipped]` → deliver →
     `OrderDeliveredEvent(OrderId, Instant)`

4. **CancelOrderHandler:**
   - Seed: `[Placed]` → cancel → `OrderCancelledEvent(OrderId, String reason, Instant)`
   - Also valid from CONFIRMED and PAID
   - Invalid: cancel when SHIPPED, DELIVERED, or already CANCELLED → throws

**Hints:**
- Each handler test seeds the event store with the right history, calls the handler,
  asserts the single new event was appended and published
- Each handler follows the same load → reconstitute → act → append → publish flow
- The aggregate's `apply` method grows as you add event types — each new event gets a
  case in the `switch`
- `cancel()` is valid from multiple statuses — a good place for a Set or a boolean check
  rather than `status == X`

#### Exercise 7.3c — Extract the handler pattern

You've now written 5+ handlers with the same orchestration logic. Extract it.

1. **Identify what varies** between handlers:
   - The command type
   - How to extract the `OrderId` from the command
   - Which domain method to call (and with what arguments)
   - Creation (`Order.place`) vs subsequent (`order.confirm`, `order.pay(amount)`)

2. **Extract a helper** that encapsulates load → reconstitute → act → append → publish.
   After extraction, a handler might look like:

   ```java
   public Void handle(ConfirmOrderCommand command) {
       processCommand(command.orderId(), Order::confirm);
       return null;
   }
   ```

   Design the extraction yourself — the shape depends on how your handlers differ.

3. **Verify all existing tests still pass** after the refactor.

**Hints:**
- The helper might be a base class, a standalone utility method, or a functional approach
  (accepts `Function<Order, OrderEvent>`)
- `PlaceOrderHandler` is a special case — static factory, expected version -1, no
  reconstitution. It may not fit cleanly. That's fine — don't force it.
- Some methods take arguments (`pay(amount)`, `ship(tracking)`, `cancel(reason)`). You
  may need a `Function<Order, OrderEvent>` that captures those arguments via lambda.

---

### 7.4 — Projections (Read Side)

#### Read First

**Why projections?**

The event store holds a stream of events per aggregate. It can answer "what happened to
order X?" but it can't efficiently answer "show me all orders for customer Y" or "how many
orders are in SHIPPED status?" — those require scanning every stream.

Projections build **denormalized read models** optimized for specific queries. They subscribe
to events and maintain their own state.

```
Event Store (write side)          Projection (read side)
┌──────────────────────┐         ┌─────────────────────────┐
│ OrderPlacedEvent     │────────▶│ order_summary            │
│ OrderConfirmedEvent  │────────▶│   id, customer, status,  │
│ OrderPaidEvent       │────────▶│   total, item_count,     │
│ OrderShippedEvent    │────────▶│   placed_at, updated_at  │
└──────────────────────┘         └─────────────────────────┘
```

**Eventual consistency:**

Projections update asynchronously — the write side commits events, and projections catch up
independently. There's a window where the write has committed but the projection hasn't
processed it yet. This is a feature, not a bug: the write side is never blocked by slow
projections, and projections can be rebuilt from scratch by replaying from event 0.

**Idempotent handlers:**

At-least-once delivery means an event might be delivered twice (e.g., crash between handling
and checkpointing). Projection handlers must tolerate this — processing the same event
twice should produce the same result. Upsert-by-ID is naturally idempotent.

#### Exercise 7.4a — Order summary projection (use-case-driven)

Start from the query handler test. The query handler needs the projection, which needs the
event publisher wiring.

1. **Test `FindOrderSummariesHandler`:**
   - Place an order (via `PlaceOrderHandler`) → query by customer ID → returns summary
     with status PLACED, correct total and item count
   - Place, then confirm (via handlers) → query → status is CONFIRMED
   - Place two orders for the same customer → query returns both

   The test wires up the full flow: event store + publisher + projection + query handler.
   All in-memory. This proves the write-side events flow through to the read-side query.

2. **Work through what the test needs:**
   - `FindOrderSummariesQuery` — record: `CustomerId`
   - `FindOrderSummariesHandler` — query handler, queries the projection's view store
   - `OrderSummary` — record or class: `OrderId`, `CustomerId`, `OrderStatus`,
     `Money total`, `int itemCount`, `Instant placedAt`, `Instant lastUpdatedAt`
   - `OrderSummaryViews` — port interface: `save`, `findById`, `findByCustomerId`
   - `InMemoryOrderSummaryViews` — in test source
   - `OrderSummaryProjection` — listens to events, updates the view store. Has an
     `on(OrderEvent)` method dispatching via pattern matching.
   - Wiring: register the projection as a listener on the `InMemoryEventPublisher`

3. **Handle each event type:**
   - `OrderPlacedEvent` → create summary with PLACED status, total, item count
   - Status events (confirmed, paid, shipped, delivered, cancelled) → update status and
     `lastUpdatedAt`

**Hints:**
- Total calculation: `items.stream().map(OrderLine::lineTotal).reduce(Money.ZERO,
  Money::add)` — you may need a `Money.ZERO` constant
- The projection never rejects events. Events are facts.
- The full-flow test is the key artifact — it proves write→publish→project→query works.
  Individual projection `on()` tests are optional if the flow test covers the cases.
- The `InMemoryEventPublisher` needs a way to register listeners. A simple
  `addListener(Consumer<EventEnvelope>)` works. The projection adapts: extract `payload`,
  cast to `OrderEvent`, dispatch.

#### Exercise 7.4b — Order detail projection

A second projection optimized for a richer query.

1. **Test `FindOrderDetailHandler`:**
   - Place and ship an order → query detail → includes items, tracking number, timeline
     with all transitions

2. **Build the types:**
   - `OrderDetail` — includes `List<OrderLine>`, tracking number (nullable), cancellation
     reason (nullable), `List<StatusChange>` timeline where
     `StatusChange(OrderStatus, Instant)`
   - `OrderDetailViews` — port interface
   - `OrderDetailProjection` — listens to events, builds detail view

**Hints:**
- The timeline is append-only — each event adds an entry. Full audit trail in the read
  model without querying the event store.

---

### 7.5 — Order Fulfillment (Process Manager)

#### Read First

**The problem:**

Placing an order kicks off a workflow that spans multiple modules:

```
Order Placed
  → Request payment authorization (Payment module)
  → Payment authorized
  → Reserve inventory (Inventory module)
  → Inventory reserved
  → Capture payment (Payment module)
  → Payment captured
  → Confirm order
```

Every step can fail. Every failure needs compensation:
- Payment authorization fails → cancel order
- Inventory reservation fails → release payment authorization, cancel order
- Payment capture fails → release inventory, release payment authorization, cancel order

**Process manager vs projection:**

A projection passively builds a read model. A process manager actively **reacts** to events
by sending commands. It's a stateful workflow coordinator.

| Concern | Projection | Process Manager |
|---------|-----------|----------------|
| Purpose | Build read model | Coordinate workflow |
| Receives | Events | Events |
| Produces | Read model updates | Commands |
| Side effects? | No | Yes (sends commands) |

**The TS analogy:** An NgRx Effect or Redux Saga — listens for actions, maintains state,
dispatches further actions. The difference: the process manager's state must be durable.

**Design decision — scope for now:**

Payment and Inventory don't exist yet. Define the contracts (commands and events) that
Order expects from them. Use fake handlers in tests that auto-succeed or auto-fail. When
you build those modules later, they implement what Order already expects.

#### Exercise 7.5a — Cross-module contracts

These live in `common` since they cross module boundaries.

1. **Payment contracts:**
   - `AuthorizePaymentCommand(PaymentId, OrderId, Money amount)`
   - `CapturePaymentCommand(PaymentId)`
   - `ReleasePaymentCommand(PaymentId)`
   - `PaymentAuthorizedEvent(PaymentId, OrderId)`
   - `PaymentAuthorizationFailedEvent(PaymentId, OrderId, String reason)`
   - `PaymentCapturedEvent(PaymentId, OrderId)`

2. **Inventory contracts:**
   - `ReserveInventoryCommand(ReservationId, OrderId, List<OrderLine> items)`
   - `ReleaseInventoryCommand(ReservationId)`
   - `InventoryReservedEvent(ReservationId, OrderId)`
   - `InsufficientInventoryEvent(ReservationId, OrderId, List<ProductId> unavailable)`

3. **New value objects:**
   - `PaymentId` — record wrapping UUID
   - `ReservationId` — record wrapping UUID

**Hints:**
- These are API contracts — pure records. When Payment and Inventory are built, they
  consume these commands and emit these events.
- Put them in a package that both Order and future modules can depend on without circular
  dependencies.

#### Exercise 7.5b — The fulfillment process manager

1. **Test the happy path (outside-in):**
   - Feed `OrderPlacedEvent` to the process manager → it returns
     `AuthorizePaymentCommand`
   - Feed `PaymentAuthorizedEvent` → returns `ReserveInventoryCommand`
   - Feed `InventoryReservedEvent` → returns `CapturePaymentCommand`
   - Feed `PaymentCapturedEvent` → returns `ConfirmOrderCommand`

   Assert each step produces the correct command with the correct data.

2. **Test compensation paths:**
   - `PaymentAuthorizationFailedEvent` → returns `CancelOrderCommand`
   - `InsufficientInventoryEvent` → returns `[ReleasePaymentCommand,
     CancelOrderCommand]`

3. **Build `OrderFulfillmentProcess`:**
   - Pure object, like the aggregate — receives events, returns commands
   - Tracks state: `OrderId`, `PaymentId`, `ReservationId`, which steps completed
   - The application layer (a handler wired to the event publisher) calls it and
     dispatches returned commands via the mediator

4. **Test state tracking:**
   - Compensation only releases what was acquired: only release payment if authorized,
     only release inventory if reserved

**Hints:**
- The process manager follows the same "receive input, return output" pattern as the
  aggregate. The caller handles side effects (dispatching commands).
- The process manager needs its own persistence (must survive restarts). For now, an
  in-memory store keyed by `OrderId` is fine.
- Start with just the happy path. Add compensation one step at a time.
- The process manager is correlated by `OrderId` — every event it handles carries one.

#### Exercise 7.5c — Integration test with stubs

Wire it all together with fake payment and inventory handlers.

1. **Create stub command handlers** (test source):
   - `StubPaymentHandler` — handles `AuthorizePaymentCommand`, immediately publishes
     `PaymentAuthorizedEvent`. Handles `CapturePaymentCommand`, publishes
     `PaymentCapturedEvent`.
   - `StubInventoryHandler` — handles `ReserveInventoryCommand`, publishes
     `InventoryReservedEvent`.

2. **Write an integration test** — all in-memory, no Spring:
   - Wire: event store + publisher + command handlers (including stubs) + process manager
   - Place an order
   - Assert: the process runs to completion — order ends up CONFIRMED
   - The stubs auto-succeed, the process manager reacts to each event, the mediator
     dispatches each command

3. **Write a failure test:**
   - Configure the payment stub to publish `PaymentAuthorizationFailedEvent` instead
   - Place an order → order ends up CANCELLED

---

### 7.6 — Order REST API

#### Exercise 7.6a — Order controller

Follow the Catalog's pattern. Test from the outside.

1. **Acceptance test first:**
   - `POST /api/orders` with order lines → 201
   - `GET /api/orders/{id}` → 200 with order detail
   - `GET /api/orders?customer={id}` → 200 with summaries
   - `POST /api/orders/{id}/cancel` with reason → 204
   - Non-existent order → 404

2. **Build the controller:**
   - Uses the mediator for command and query dispatch
   - `@ControllerAdvice` for exception mapping:
     - `ConcurrentStreamModificationException` → 409 Conflict
     - `IllegalStateException` from aggregate → 422 Unprocessable Entity
     - Order not found → 404

3. **For now**, wire in-memory implementations via `@Profile("test")` configuration.

---

### 7.7 — PostgreSQL Infrastructure (When Ready)

> **This section is for when you're ready to add persistence.** Everything above works with
> in-memory implementations. Come back here when the domain and application layers are solid.

#### Exercise 7.7a — PostgreSQL event store

1. **Flyway migration** — `V5__create_domain_event_table.sql`:
   ```sql
   CREATE TABLE domain_event (
       aggregate_id     UUID NOT NULL,
       sequence_number  BIGINT NOT NULL,
       event_type       VARCHAR(255) NOT NULL,
       payload          JSONB NOT NULL,
       occurred_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
       PRIMARY KEY (aggregate_id, sequence_number)
   );
   ```
   The composite primary key is the optimistic lock — two concurrent appends with the same
   `(aggregate_id, sequence_number)` result in a unique constraint violation.

2. **Jackson serialization tests** — round-trip each event type through JSON before wiring
   to the store. Test sealed interface deserialization (`@JsonTypeInfo` / `@JsonSubTypes`
   on `OrderEvent`). You've done this with `PromotionTarget` and `LibraryEvent`.

3. **Build `PostgresEventStore`** implementing `EventStore`:
   - `append`: INSERT events. Catch the unique constraint violation and translate to
     `ConcurrentStreamModificationException`.
   - `load`: SELECT ordered by sequence number. Deserialize JSONB payload via Jackson.

4. **Run the contract tests** — `PostgresEventStoreTest` extends `EventStoreContract`,
   provides the Postgres-backed implementation with Testcontainers.

#### Exercise 7.7b — Projection infrastructure

1. **Flyway migrations** for projection read models:
   - `V6__create_order_summary_table.sql`
   - `V7__create_order_detail_table.sql`
   - `V8__create_projection_checkpoint_table.sql`:
     ```sql
     CREATE TABLE projection_checkpoint (
         projection_name  VARCHAR(255) PRIMARY KEY,
         last_sequence     BIGINT NOT NULL
     );
     ```

2. **Build a polling projection updater:**
   - Reads new events from the event store since the projection's last checkpoint
   - Delivers to the projection handler
   - Updates the checkpoint
   - Runs on a scheduled interval (`@Scheduled`)

3. **Build Postgres view repositories** — JPA entities and adapters, same pattern as
   Catalog.

4. **Contract tests** for the view repositories.

**Hints:**
- For polling, you may need a `global_sequence` auto-increment column on `domain_event`
  (separate from per-stream `sequence_number`) — gives a total ordering across all streams.
  Alternatively, poll per-stream with per-stream checkpoints.
- Idempotent handlers + at-least-once delivery: if the process crashes between handling and
  checkpointing, events are redelivered. Handlers must tolerate this — upsert-by-ID is
  naturally idempotent.

---

## Part B — Gateway & Authentication

### 7.8 — Spring Security Fundamentals

#### Read First

**What Spring Security does:**

Spring Security is a filter chain that intercepts every HTTP request before it reaches your
controllers. It handles authentication (who are you?) and authorization (what can you do?).

```
HTTP Request
  → SecurityFilterChain
    → AuthenticationFilter (extract + validate credentials)
    → AuthorizationFilter (check permissions)
  → Controller
  → Response
```

**The TS comparison:**

| Concept | Express / NestJS | Spring Security |
|---------|-----------------|-----------------|
| Middleware chain | `app.use(authMiddleware)` | `SecurityFilterChain` |
| Auth guard | NestJS `@UseGuards(AuthGuard)` | `@PreAuthorize` / URL rules |
| Token extraction | Custom middleware | `BearerTokenAuthenticationFilter` |
| User context | `req.user` | `SecurityContextHolder.getContext().getAuthentication()` |
| Role check | `@Roles('admin')` | `@PreAuthorize("hasRole('ADMIN')")` |
| Public routes | No guard applied | `.permitAll()` |
| Config | Middleware ordering | `SecurityFilterChain` bean |

**Security filter chain configuration:**

Spring Security 6+ (Spring Boot 4) uses a lambda-based DSL:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())  // stateless REST API
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 ->
                oauth2.jwt(jwt -> jwt.decoder(jwtDecoder()))
            )
            .build();
    }
}
```

**Stateless sessions:** REST APIs don't use HTTP sessions. Every request carries its own
credentials (JWT token). `SessionCreationPolicy.STATELESS` tells Spring Security not to
create sessions.

**Watch out:** Spring Security has a steep learning curve because the filter chain is
implicit — you configure it declaratively, and the framework wires the filters. When
something doesn't work, enable debug logging (`logging.level.org.springframework.security=
DEBUG`) to see exactly which filters run and where the request gets rejected.

### 7.9 — JWT Authentication

#### Read First

**JWT (JSON Web Token) — the flow:**

```
1. Client authenticates (username/password) → POST /auth/login
2. Server validates credentials, creates JWT, returns it
3. Client stores JWT, sends it on every request: Authorization: Bearer <token>
4. Server validates JWT signature on each request (no database lookup needed)
```

**JWT structure:** `header.payload.signature` — three base64-encoded sections.

The payload carries **claims** — key-value pairs:
- `sub` (subject) — user ID
- `iat` (issued at) — timestamp
- `exp` (expiration) — timestamp
- Custom claims: `roles`, `email`, etc.

**Symmetric vs asymmetric keys:**

- **Symmetric (HMAC):** One shared secret signs and verifies. Simpler. Fine for a monolith.
- **Asymmetric (RSA/EC):** Private key signs, public key verifies. Required when different
  services create vs validate tokens.

For this project (modular monolith), symmetric HMAC is sufficient.

#### Exercise 7.9a — Add the Gateway module

1. **Add `gateway` to `settings.gradle.kts`**

2. **Create `gateway/build.gradle.kts`:**
   - Dependencies: `spring-boot-starter-web`, `spring-boot-starter-security`,
     `spring-boot-starter-oauth2-resource-server`
   - For JWT creation: `io.jsonwebtoken:jjwt-api`, `jjwt-impl`, `jjwt-jackson` (look up
     the latest version)

3. **Write ADR `docs/adr/0013-jwt-authentication.md`:**
   - Context: REST API needs authentication. Modular monolith, single deployable.
   - Decision: JWT with HMAC-SHA256 signing. Stateless sessions.
   - Consequence: No server-side session state. Token expiration is the primary revocation
     mechanism.

#### Exercise 7.9b — Token service (use-case-driven)

Start from the test. The login use case needs a token service.

1. **Test `TokenService`:**
   - Create a token for a user with roles → non-empty string
   - Parse a valid token → returns correct user ID and roles
   - Parse an expired token → throws
   - Parse a tampered token → throws
   - Parse a token signed with a different key → throws

2. **Build `TokenService`:**
   - `createToken(UserId userId, Set<Role> roles)` → `String`
   - `parseToken(String token)` → `TokenClaims` record (userId, roles, expiration)
   - Constructor: signing key (`SecretKey`), token duration (`Duration`), `Clock`

3. **Supporting types:**
   - `UserId` — record wrapping UUID
   - `Role` — enum: `CUSTOMER`, `ADMIN`
   - `TokenClaims` — record

**Hints:**
- JJWT creation:
  ```java
  Jwts.builder()
      .subject(userId.value().toString())
      .claim("roles", roles.stream().map(Role::name).toList())
      .issuedAt(Date.from(clock.instant()))
      .expiration(Date.from(clock.instant().plus(duration)))
      .signWith(signingKey)
      .compact();
  ```
- JJWT parsing:
  ```java
  Jwts.parser().verifyWith(signingKey).build()
      .parseSignedClaims(token).getPayload();
  ```
- `Keys.hmacShaKeyFor(secretBytes)` for the signing key
- Use `Clock.fixed(...)` in tests for deterministic expiration testing

#### Exercise 7.9c — Login endpoint

1. **Acceptance test first:**
   - `POST /auth/login` with valid credentials → 200 with token
   - Invalid password → 401
   - Unknown user → 401 (same response — don't leak user existence)

2. **Build the pieces:**
   - `AuthController` in `gateway/api/`
   - `InMemoryUserStore` — `Map<String, UserRecord>` with test users
   - `UserRecord(UserId, String username, String passwordHash, Set<Role> roles)`
   - `BCryptPasswordEncoder` for password hashing

**Hints:**
- The login endpoint is not behind the security filter — it's how you get a token
- `@Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }`

#### Exercise 7.9d — Security filter chain

1. **Acceptance test first:**
   - `GET /api/products` without token → 401
   - `GET /api/products` with valid token → 200
   - `GET /api/products` with expired token → 401
   - Admin endpoint with CUSTOMER token → 403

2. **Build `SecurityConfig`:**
   - CSRF disabled
   - Stateless sessions
   - `/auth/**` → permit all
   - `/api/**` → authenticated
   - JWT resource server with signing key

3. **Add `@PreAuthorize` to sensitive endpoints:**
   - Product management → admin-only
   - Order placement and lookup → any authenticated user
   - Order cancellation → owner or admin

**Hints:**
- `@EnableMethodSecurity` for `@PreAuthorize` support
- JWT decoder:
  ```java
  @Bean
  public JwtDecoder jwtDecoder() {
      return NimbusJwtDecoder.withSecretKey(signingKey).build();
  }
  ```
- Custom `JwtAuthenticationConverter` to map your `roles` claim to Spring Security
  granted authorities
- `@PreAuthorize("hasRole('ADMIN')")` checks for authority `ROLE_ADMIN` — Spring prepends
  `ROLE_`. Either strip it in your converter or use `hasAuthority('ADMIN')`
- Signing key shared via config property between `TokenService` and `JwtDecoder`

---

## Suggested Sequence

| Step | Section | What you learn |
|------|---------|---------------|
| 1 | 7.1 | ADRs |
| 2 | 7.2 | Place order — event store port, aggregate, handler (outside-in) |
| 3 | 7.3a–b | Subsequent commands — reconstitution, state machine |
| 4 | 7.3c | Extract handler pattern — refactor |
| 5 | 7.9a–b | Gateway module + TokenService — break from ES |
| 6 | 7.4a–b | Projections — wired to publisher, query handlers |
| 7 | 7.9c–d | Login endpoint + security filter chain |
| 8 | 7.5 | Order fulfillment process manager |
| 9 | 7.6 | Order REST API |
| 10 | 7.7 | PostgreSQL infrastructure (when ready) |

Interleaving Order and Gateway prevents fatigue. PostgreSQL is deliberately last —
everything works in-memory first.

---

## Key Technical Challenges to Expect

- **Jackson `@JsonTypeInfo` on the sealed event hierarchy** — needed when events hit the
  PostgreSQL store. You've done this with `PromotionTarget` and `LibraryEvent`.
- **Global vs per-stream sequencing for projection delivery** — per-stream is simpler but
  limits cross-aggregate projections. Think about this when building the polling updater.
- **Process manager durability** — in-memory is fine for learning, but fulfillment must
  survive restarts in production.
- **Idempotent projection handlers** — at-least-once delivery means events may be
  redelivered. Upsert-by-ID is naturally idempotent.
- **Spring Security filter ordering** — if auth doesn't work, enable security debug logging.
- **JWT claim → Spring Security authority mapping** — requires a custom converter.
