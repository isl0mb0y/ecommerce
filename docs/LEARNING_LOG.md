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

## Step 3 — Resilience (in progress)

### 3.1 Seeing the problem (done)
- Catalog has a chaos toggle `chaos.delay-ms` (0 = off); set to 10000 to simulate slowness.
- order-service `server.tomcat.threads.max` lowered (5, now 10) to reproduce starvation with
  10 parallel requests.
- Owner ran 10 parallel POST /api/orders + 1 GET /api/orders/1 and **saw**:
  - POSTs queue in waves (pool=5: five at 2s, five at 4s).
  - GET /orders/1 (no catalog dependency) waited ~2s for a free thread → **hidden coupling via
    shared thread pool**. Understands this is where cascading failure starts and that
    "increase the pool" is the wrong fix.

### 3.2 Timeouts (done, committed as `resilience(timeout): ...`)
- `CatalogClient` builds `RestClient` with `SimpleClientHttpRequestFactory`,
  connect-timeout 500ms, read-timeout 2000ms (from `catalog.*` properties).
- Owner understands connect vs read timeout and saw both: slow catalog → 503 after ~2s;
  stopped catalog → 503 almost instantly.
- Understands timeout *bounds* damage but does not *isolate* it (threads still busy, queue remains).

### 3.3 Retry (in progress — NOT yet verified)
- Added `resilience4j-spring-boot3` 2.2.0 + `starter-aop` + `starter-actuator`.
- `@Retry(name = "catalog")` on `CatalogClient.findProduct`.
- yml: `resilience4j.retry.instances.catalog`: max-attempts 3, wait 200ms, exponential ×2,
  retry-exceptions ResourceAccessException, ignore-exceptions HttpClientErrorException.
- `api/ApiExceptionHandler` maps `ResourceAccessException` → 503.
- `config/RetryEventLogger` logs onRetry/onError/onSuccess/onIgnoredError.
- Owner understands: retry only for transient + idempotent + downstream-can-take-it;
  retry on timeout is usually harmful (3× load on an already slow service); backoff + jitter;
  3 attempts = 1 call + 2 retries; no log on first-try success.

**Open issue:** first test returned 503 in ~12ms with no retry log. Diagnosis given: a
`catch (RestClientException)` inside `findProduct` was converting `ResourceAccessException`
to `ResponseStatusException` before `@Retry` could see it. Owner says he removed it — **verify**:
with catalog stopped, POST must take ~0.6s and log 3× "Calling catalog-service" + 2 WARN + 1 ERROR
from RetryEventLogger.

Also done during this step: order-service repackaged (package-by-feature):
`order/` (+`order/dto/`), `catalog/`, `messaging/` (+`messaging/event/`), `api/`, `config/`.
Owner understands package-by-feature vs by-layer, and that this is DDD-friendly but not DDD.

### 3.4 Next (not started)
1. Circuit breaker (Resilience4j `@CircuitBreaker` on the same method, sliding window, OPEN →
   fail fast without calling catalog, HALF_OPEN probe; watch via `/actuator/circuitbreakers`).
   Explain order of decorators (retry wraps CB or CB wraps retry — and why).
2. Bulkhead (separate thread pool / semaphore for catalog calls) — the fix for the GET-waiting
   observation; expected result: GET /orders/1 ~10ms while POSTs fail fast.
3. Gateway circuit breaker (`spring-cloud-starter-circuitbreaker-reactor-resilience4j`) with
   fallback → 503 + readable body instead of 500 Connection refused.
4. Cleanup: remove `threads.max` override or keep with comment; chaos delay back to 0.

## Teaching style that has worked

- Show the pain first (reproduce with curl/`for` loop), then the fix, then re-measure.
- One small step at a time; owner types the code, mentor reviews.
- Always answer "why" with a concrete number the owner just saw (2s vs 10s, 12ms vs 0.6s).
- ASCII diagrams for flows; tables for comparisons.
- End each step with a break-it exercise and a commit message `<topic>(<subtopic>): <what>`.
