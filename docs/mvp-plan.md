Create a minimal e-commerce microservices project in Java 21 / Spring Boot 3.3.x using Maven multi-module layout. This is a learning project — keep it as small as possible, no extras beyond what is listed.

## Repository layout
ecommerce/
├── pom.xml                  (parent, packaging=pom)
├── docker-compose.yml       (infrastructure only)
├── README.md
├── catalog-service/
├── order-service/
└── payment-service/

## Strict rules
- Parent pom: inherits from spring-boot-starter-parent, lists the three modules, sets java.version=21, and uses <dependencyManagement> only. It must NOT declare any <dependencies>.
- NO shared/common module. Services must not depend on each other in Maven. Each service defines its own DTOs and event classes, even if they look identical.
- Each child pom declares its own dependencies and its own spring-boot-maven-plugin so it builds a standalone runnable jar.
- Each service has its own PostgreSQL database and never touches another service's tables.
- Each service has its own Dockerfile (multi-stage, eclipse-temurin:21-jre runtime).
- Package layout: package-by-feature (com.shop.catalog.product, com.shop.order.order, etc.), not by layer.
- Use Java records for DTOs and events. Use Lombok only if needed for JPA entities.
- No API gateway, no service discovery, no auth, no security dependency. Frontend/Postman calls services directly on their ports. User identity is passed as an X-User-Id header, read as a plain Long.

## Infrastructure (docker-compose.yml)
- postgres:16 on port 5432 with an init script that creates three databases: catalog_db, orders_db, payments_db (user/password: shop/shop).
- rabbitmq:3-management on ports 5672 and 15672.
- Services are NOT in docker-compose; they run via `mvn spring-boot:run` locally.

## Services

### catalog-service (port 8081, db catalog_db)
Entity Product(id, name, priceInCents, stock).
Endpoints:
- GET  /api/products            → list all
- GET  /api/products/{id}       → single product (404 if missing)
- POST /api/products            → create (body: name, priceInCents, stock)
Messaging: consumes PaymentSucceeded event and decrements stock for each item in the order. Log a warning (do not fail) if stock would go negative.
Seed 5 sample products on startup via CommandLineRunner (only if table is empty).

### order-service (port 8082, db orders_db)
Entities Order(id, userId, status, totalInCents, createdAt) and OrderItem(id, order, productId, quantity, priceInCents).
OrderStatus enum: CREATED, PAID, CANCELLED.
Endpoints:
- POST /api/orders              → body: items[{productId, quantity}], header X-User-Id. For each item, call catalog-service synchronously (GET http://localhost:8081/api/products/{id}) using Spring RestClient to fetch the price and verify stock >= quantity; return 400 if any product is missing or out of stock. Save order with status CREATED, publish OrderCreated event, return 201 with the order.
- GET  /api/orders/{id}         → order with items and status
- GET  /api/orders?userId=      → orders for a user
Messaging: publishes OrderCreated; consumes PaymentSucceeded → set status PAID, PaymentFailed → set status CANCELLED.
Catalog base URL must come from application.yml (catalog.base-url), not hardcoded.

### payment-service (port 8083, db payments_db)
Entity Payment(id, orderId, amountInCents, status, createdAt). PaymentStatus enum: SUCCESS, FAILED.
No public endpoints required except GET /api/payments/{orderId} → payment for that order (404 if none).
Messaging: consumes OrderCreated, simulates processing (Thread.sleep 2 seconds), randomly succeeds 80% / fails 20%, saves Payment, publishes PaymentSucceeded or PaymentFailed.

## Messaging (RabbitMQ, spring-boot-starter-amqp)
- One topic exchange named "shop.events".
- Routing keys: order.created, payment.succeeded, payment.failed.
- Each consuming service declares its own queue bound to the exchange with the routing keys it needs (e.g. payment-service.order-created, order-service.payment-result, catalog-service.payment-succeeded).
- JSON payloads via Jackson2JsonMessageConverter. Event shapes (each service defines its own copy as a record):
  - OrderCreated { orderId, userId, totalInCents, items[{productId, quantity}] }
  - PaymentSucceeded { orderId, paymentId, items[{productId, quantity}] }
  - PaymentFailed { orderId, reason }
- Do not use Java class names as the __TypeId__ for deserialization; configure the converter so each service deserializes into its own record by routing key / listener parameter type.

## Config
- Each service: application.yml with server.port, its datasource (jdbc:postgresql://localhost:5432/<db>, user shop/shop), spring.jpa.hibernate.ddl-auto=update, spring.rabbitmq.host=localhost.
- Enable basic logging of every consumed and published event (INFO level) so the flow is visible in the console.

## README.md
Include: how to start infrastructure (docker compose up -d), how to run each service (mvn -pl <module> spring-boot:run), and a curl walkthrough: list products → create order → poll GET /api/orders/{id} until status becomes PAID or CANCELLED → check stock decreased.

## Verification
After generating everything, run `mvn -q clean package -DskipTests` from the root and fix any compilation errors before finishing.