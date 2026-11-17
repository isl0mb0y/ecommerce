# Learning log — what has been learned so far and how

Read this before mentoring. It records not just *what* was done but *what the owner
already understands*, so you can build on it instead of re-explaining.

## Step 1 — MVP (done, committed)

Built from `mvp-prompt.md` with an agent, then studied. Owner understands:

- Service boundaries: catalog owns product/price/stock, order owns orders, payment owns payments.
- DB per service — one Postgres container, three databases, no cross-DB access.
  Knows *why there is no FK* on `order_item.product_id`: FK = DB-level coupling = shared-DB
  anti-pattern; integrity moves to the application (validate via API, snapshot price, soft delete).
- Sync vs async: order→catalog is REST (needs answer now); order↔payment is RabbitMQ events.
  Understands the trade-off vs doing payment synchronously (thread held, partial failure, fan-out).
- RabbitMQ topology: one topic exchange `shop.events`, routing keys `order.created`,
  `payment.succeeded`, `payment.failed`; each consumer has its own queue; `payment.succeeded`
  fans out to order-service and catalog-service.
- Payment is a mock: 2s sleep, 80/20 random — the 20% CANCELLED path is intentional.
- Why RabbitMQ first, Kafka later (queue vs log; Kafka migration planned as step 6).
- JSON is snake_case (`product_id`) — owner's own change. Event contracts are per-service copies.

Break-it exercise done: stopping payment-service → order stays CREATED, message waits in queue,
resumes when payment comes back.

## Step 2 — API Gateway (done, committed)

Owner wrote it himself in IntelliJ, step by step:

- `api-gateway` module, `spring-cloud-starter-gateway` only (no `starter-web` — WebFlux/Netty).
- Spring Cloud BOM (`2023.0.x` for Boot 3.3.x) added to parent pom — understands release trains.
- Three routes, path not rewritten, hardcoded `localhost:808x` (discovery comes later).
- CORS only at the gateway; understands same-origin policy, preflight, why it's a browser rule.
- Actuator `/actuator/gateway/routes`.
- Understands *why* the gateway is reactive: proxy = mostly waiting; event loop ≠ thread per request;
  one slow downstream must not consume the shared pool; never block inside a gateway filter.
- Knows the difference between a gateway 404 (no route) and a downstream 404 (resource missing).

Break-it: catalog stopped → gateway returns 500 Connection refused. Noted as motivation for step 3.

## Step 3 — Resilience (done, committed)

### 3.1 Seeing the problem
- Catalog has a chaos toggle `chaos.delay-ms` (0 = off). With a small Tomcat pool the owner ran
  10 parallel POST /api/orders + 1 GET /api/orders/1 and saw POSTs queue in waves and the GET
  (no catalog dependency) wait for a free thread: **hidden coupling via the shared thread pool**,
  the start of cascading failure. "Increase the pool" is the wrong fix.

### 3.2 Timeout — `resilience(timeout)`
- `CatalogClient` uses `SimpleClientHttpRequestFactory`, connect 500ms / read 2000ms.
- Understands connect vs read timeout, and at TCP level: connection refused (RST, process down),
  connect timeout (no answer to SYN, host unreachable), read timeout (connected, no response, degraded).
- Timeout bounds damage but does not isolate it.

### 3.3 Retry — `resilience(retry)`
- Resilience4j `@Retry(name = "catalog")`, 3 attempts, 200ms/400ms exponential backoff.
- Mechanics understood: AOP proxy re-invokes the method; the only signal is an exception that
  escapes the method (an inner catch that swallows it disables retry); no self-invocation.
- Three conditions: transient + idempotent + does not hurt a slow service.
- `CatalogRetryPredicate` (`retry-exception-predicate`): retry ONLY on connection refused
  (`ResourceAccessException` with cause `ConnectException`), never on timeouts. Measured:
  degraded catalog went from 6.9s/3 hits to 2s/1 hit per request.
- Lesson learned the hard way: `retry-exceptions` is a list of exception classes, the predicate
  needs its own key; list and predicate are OR-ed.

### 3.4 Circuit breaker — `resilience(circuit-breaker)`
- `@CircuitBreaker(name = "catalog")`: COUNT_BASED window 10, minimum 5 calls, threshold 50%,
  10s OPEN, 2 probes in HALF_OPEN, automatic transition, health indicator.
- Understands CLOSED / OPEN / HALF_OPEN, sliding window vs minimum-number-of-calls, that each
  retry attempt is a separate call for the breaker, probe cost, and that the window is cleared
  after recovery. Observed via `/actuator/circuitbreakers`, `/circuitbreakerevents`, `/health`.
- `CallNotPermittedException` → 503 in `ApiExceptionHandler`.

### 3.5 Bulkhead — `resilience(bulkhead)`
- Semaphore `@Bulkhead(name = "catalog")`, `max-wait-duration: 0`; `BulkheadFullException` → 503
  and listed in the breaker's `ignore-exceptions`.
- Key insight: with catalog answering in 1.5s (below the timeout) timeout, retry and breaker all
  see nothing, yet the pool starves. Only the bulkhead limits concurrency. Measured: GET /orders/1
  no longer waits while POSTs are stuck. Rejected requests = load shedding / fail fast; the limit
  must be sized from real load.

### 3.6 Gateway — `resilience(gateway)`
- `spring-cloud-starter-circuitbreaker-reactor-resilience4j`, one breaker per route
  (`catalogCb`, `ordersCb`, `paymentsCb`) with `fallbackUri: forward:/fallback/{service}` → 503 JSON.
- TimeLimiter raised to 3s (default 1s); outer timeout must exceed inner ones.
- Understands: gateway breaker fires only when the gateway cannot reach a service; a 5xx produced
  by the service itself passes through. Cold start explains a slow first request.
- Generic `CircuitBreakerEventLogger` attaches to every breaker via `onEntryAdded`.

## Step 4 — Data consistency (done, committed)

Before this step the owner studied the RabbitMQ model in depth: exchange `shop.events` (topic),
routing key vs binding key, queue per consumer, bindings declared by the consumer through
`Queue`/`Binding` beans (RabbitAdmin, idempotent, never deletes), fan-out vs competing consumers,
ack after processing, redelivery, at-least-once, consumer_timeout. Old queues must be deleted
by hand when topology changes.

Services were repackaged by layer (owner's preference, replaces package-by-feature):
`api/rest (+dto)`, `client/<svc> (+dto)`, `config`, `domain`, `messaging/{config,event,listener,publisher}`,
`repository`, `service`.

### 4.1 Idempotent consumer — `consistency(idempotency)`
- Reproduced: duplicate `payment.succeeded` decremented stock twice.
- Fix: `processed_events(event_id PK)` + `insert ... on conflict do nothing` (`markProcessed`
  returns 1 = new, 0 = duplicate) in the same transaction as the business change.
- Event ids are deterministic (`payment-succeeded-<paymentId>`), never random, because a
  redelivered trigger makes the publisher re-publish.

### 4.2 Outbox — `consistency(outbox)`
- Reproduced dual write: RabbitMQ down → 500 to client, order stuck in CREATED forever.
- Fix in order-service: `outbox` table (status PENDING/PUBLISHED/FAILED + published_at, owner chose
  an enum over a nullable timestamp), order + outbox row saved in one `TransactionTemplate` block
  (catalog HTTP call stays outside the transaction), `OutboxPublisher` polls every 1s and sends raw
  JSON with `rabbitTemplate.send`. Needs `@EnableScheduling`.
- Understands: outbox is at-least-once, hence needs idempotent consumers; flows triggered by a
  message self-heal through redelivery, flows triggered by an HTTP request need an outbox.

### 4.3 Saga (choreography) — `consistency(saga)`
- Reproduced overselling with two parallel orders on stock = 1.
- Flow now: `order.created` → catalog reserves (atomic conditional UPDATE, all-or-nothing per order,
  `stock_reservations` RESERVED/CONFIRMED/RELEASED) → `stock.reserved` → payment charges →
  `payment.succeeded` (order PAID, catalog confirm) or `payment.failed` (order CANCELLED, catalog
  release). `stock.rejected` → order CANCELLED without payment.
- Understands choreography vs orchestration, that compensation is a new forward action and not a
  rollback, that order → CANCELLED is also a compensation, and why refund would be the payment one.
- Queues: `catalog-service.order-created`, `catalog-service.payment-succeeded`,
  `catalog-service.payment-failed`, `payment-service.stock-reserved`, `order-service.events`.

### 4.4 Optimistic locking — `consistency(optimistic-locking)`
- Stock concurrency was already solved by the atomic conditional UPDATE (DB row lock re-checks the
  WHERE clause), safe across threads and instances.
- Reproduced a lost update instead: `PUT /api/products/{id}/price` (read-modify-write, Hibernate
  dirty checking writes all columns) overwrote a concurrent reservation.
- Fix: `@Version` on `Product` (column default 0), bulk `@Modifying` queries bump the version
  themselves, `ObjectOptimisticLockingFailureException` → 409 Conflict.
- Knows the limits: only protects within one request unless the version travels through the DTO;
  for non-overlapping columns a targeted atomic update avoids the conflict altogether.

### Next — Step 5: Observability
Correlation id (gateway → MDC → RabbitMQ headers), structured JSON logging, distributed tracing
(Micrometer Tracing + OpenTelemetry → Jaeger/Tempo), metrics (Actuator + Prometheus + Grafana:
breaker state, outbox lag, queue depth, HTTP p99), readiness vs liveness.

Known hardening left open on purpose: outbox in catalog/payment publishers, `SKIP LOCKED` for
multi-instance outbox polling, outbox cleanup job, timeout job for stale RESERVED reservations,
order state-transition guards.

## Vocabulary agreement
Explanations are in Uzbek (formal "siz"), but technical terms stay in English and are not
translated: hit, down, up, degraded, behavior, observability, probe, probe cost, failure rate,
fallback, load shedding, process (a message), compensation, etc.

## Teaching style that has worked

- Show the pain first (reproduce with curl/`for` loop), then the fix, then re-measure.
- One small step at a time; owner types the code, mentor reviews.
- Always answer "why" with a concrete number the owner just saw (2s vs 10s, 12ms vs 0.6s).
- ASCII diagrams for flows; tables for comparisons.
- End each step with a break-it exercise and a commit message `<topic>(<subtopic>): <what>`.
