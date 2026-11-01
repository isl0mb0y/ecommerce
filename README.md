# E-commerce microservices

A minimal learning project: Java 21, Spring Boot 3.3, PostgreSQL and RabbitMQ.

| Service         | Port | Database    |
|-----------------|------|-------------|
| catalog-service | 8081 | catalog_db  |
| order-service   | 8082 | orders_db   |
| payment-service | 8083 | payments_db |

## Prerequisites

Docker, Maven, and JDK 21 as `JAVA_HOME`. The walkthrough also uses `jq`.

## Start the infrastructure

```bash
docker compose up -d
```

The RabbitMQ management UI is at http://localhost:15672 (guest/guest).

## Run the services

Run each one in its own terminal, from the repository root. Each service declares its own queue on startup, and
events published before a queue exists are lost, so start all three before creating orders.

```bash
mvn -pl catalog-service spring-boot:run
```

```bash
mvn -pl order-service spring-boot:run
```

```bash
mvn -pl payment-service spring-boot:run
```

## Swagger UI

Each service has its own page, which covers HTTP endpoints only, not RabbitMQ events:

- catalog-service: http://localhost:8081/swagger-ui.html
- order-service: http://localhost:8082/swagger-ui.html
- payment-service: http://localhost:8083/swagger-ui.html

## Walkthrough

List the products and note the stock of product 1:

```bash
curl -s http://localhost:8081/api/products | jq
```

Create an order:

```bash
ORDER_ID=$(curl -s -X POST http://localhost:8082/api/orders \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: 1' \
  -d '{"items":[{"product_id":1,"quantity":2}]}' | jq -r .id)
echo "$ORDER_ID"
```

Poll until the status becomes `PAID` or `CANCELLED` (about 2 seconds; payments fail 20% of the time):

```bash
until STATUS=$(curl -s "http://localhost:8082/api/orders/$ORDER_ID" | jq -r .status); [ "$STATUS" != "CREATED" ]; do sleep 1; done; echo "$STATUS"
```

Check the payment:

```bash
curl -s "http://localhost:8083/api/payments/$ORDER_ID" | jq
```

Check the stock. It drops by 2 if the order is `PAID` and stays the same if it is `CANCELLED`:

```bash
curl -s http://localhost:8081/api/products/1 | jq
```

List the orders for a user:

```bash
curl -s "http://localhost:8082/api/orders?user_id=1" | jq
```
