# Microservices Demo — Spring Boot + Spring Cloud

A runnable microservices sample covering **Spring Boot, Spring Cloud, Eureka Server/Client,
Spring Cloud Gateway, OpenFeign, client-side load balancing, REST APIs, DTOs, JPA/Hibernate (H2),
exception handling, correlation IDs, Actuator, and Docker Compose**.

```
                        ┌──────────────┐
                        │   Client     │
                        │ (curl/UI)    │
                        └──────┬───────┘
                               │ :8080  (X-Correlation-ID added here)
                        ┌──────▼───────┐
                        │   Gateway    │─── CircuitBreaker ──▶ /fallback (503)
                        │  (WebFlux)   │
                        └──────┬───────┘
               ┌───────────────┼────────────────┐
               │ lb://         │ lb://          │  Eureka discovery +
               ▼               ▼                │  client-side load balancing
     ┌─────────────────┐ ┌──────────────┐      │
     │ product-service │ │ order-service│◀─────┘
     │ :8081  JPA/H2   │ │ :8082 JPA/H2 │──OpenFeign──▶ product-service
     └─────────────────┘ └──────────────┘      (timeouts, ErrorDecoder,
                                                correlation-ID interceptor)
               └───────────────┬────────────────┘
                        ┌──────▼───────┐
                        │ Eureka Server│ :8761  (service registry + dashboard)
                        └──────────────┘
```

## Services & ports

| Service         | Port | Description                                                        |
|-----------------|------|--------------------------------------------------------------------|
| eureka-server   | 8761 | Service registry + dashboard (`/`)                                 |
| product-service | 8081 | Product CRUD, stock management (JPA/H2, seeded via `data.sql`)     |
| order-service   | 8082 | Order placement; calls product-service via OpenFeign               |
| gateway-service | 8080 | Single entry point: routing, correlation ID, circuit-breaker fallbacks |

## Prerequisites

- Java 17+
- Maven 3.8+ (or the Maven wrapper of your choice)
- Docker (optional, for `docker compose`)

## Quick start (local)

```bash
# 1. Build everything
mvn clean package -DskipTests

# 2. Start in this order (each in its own terminal), waiting ~20s between the first two
java -jar eureka-server/target/eureka-server-1.0.0.jar
java -jar product-service/target/product-service-1.0.0.jar
java -jar order-service/target/order-service-1.0.0.jar
java -jar gateway-service/target/gateway-service-1.0.0.jar
```

Eureka needs ~20–30s on first boot; services register shortly after starting.
Dashboard: http://localhost:8761 — you should see `PRODUCT-SERVICE`, `ORDER-SERVICE`, `GATEWAY-SERVICE`.

## Try it (via the gateway :8080)

```bash
# List products (seeded)
curl http://localhost:8080/api/products

# Create a product
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"USB-C Hub","description":"7-in-1 hub","price":49.99,"stockQuantity":100}'

# Place an order (order-service -> product-service via Feign; stock is decremented)
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"productId":1,"quantity":2}'

# List orders / get one
curl http://localhost:8080/api/orders
curl http://localhost:8080/api/orders/1

# Validation error example
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"productId":999,"quantity":1}'     # -> 404 from Feign ErrorDecoder mapping

# Correlation ID: pass your own or get one generated (echoed on the response)
curl -i http://localhost:8080/api/products -H "X-Correlation-ID: demo-123"

# Actuator
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8081/h2-console   # JDBC URL: jdbc:h2:mem:productdb, user sa
```

## Docker Compose

```bash
mvn clean package -DskipTests   # build the jars first
docker compose up --build
```

Same URLs as above. Compose sets `EUREKA_URL=http://eureka-server:8761/eureka/` and
`EUREKA_HOST=eureka-server` so services find the registry inside the Docker network.

## What's wired where (interview cheat-sheet)

- **Eureka Server** — `@EnableEurekaServer`; `register-with-eureka: false`, `fetch-registry: false`.
- **Eureka Clients** — `spring-cloud-starter-netflix-eureka-client` + `defaultZone` (env-overridable);
  services register by `spring.application.name`.
- **Gateway routing** — `spring.cloud.gateway.routes` with `lb://product-service` /
  `lb://order-service` (Eureka + Spring Cloud LoadBalancer = client-side LB, no hardcoded URLs).
- **Fallback / error handling** — Resilience4j `CircuitBreaker` gateway filter per route with
  `fallbackUri: forward:/fallback` → `FallbackController` returns 503 JSON; tunables under
  `resilience4j.circuitbreaker/timelimiter` in `gateway-service/src/main/resources/application.yml`.
- **OpenFeign** — `@EnableFeignClients`; `@FeignClient(name = "product-service")` resolves via Eureka;
  `FeignConfig` sets `Logger.Level.BASIC`, a custom `ErrorDecoder` (404→`ResourceNotFoundException`,
  409→`InsufficientStockException`, 5xx→`ServiceUnavailableException`), and a `RequestInterceptor`
  that forwards `X-Correlation-ID`. Timeouts in `application.yml`
  (`spring.cloud.openfeign.client.config.default.connect-timeout/read-timeout`).
- **Correlation ID** — `CorrelationIdGlobalFilter` (gateway, `Ordered.HIGHEST_PRECEDENCE`) generates
  or reuses `X-Correlation-ID`, propagates it downstream and echoes it back; each service logs it
  via `CorrelationIdLoggingFilter` (MDC).
- **JPA/Hibernate + H2** — entities, `JpaRepository`, `ddl-auto: create-drop`, `data.sql` seed,
  H2 console enabled per service.
- **Exception handling** — `@RestControllerAdvice` per service returning a uniform `ErrorResponse`
  (timestamp, status, error, message, path, fieldErrors); bean validation via
  `spring-boot-starter-validation`.
- **Actuator** — `health, info, metrics` (plus `gateway`) exposed on every service.

## Suggested extensions

- **Resilience4j on Feign** — add `spring-cloud-starter-circuitbreaker-resilience4j` to
  `order-service` and set `feign.circuitbreaker.enabled: true` to use Feign `fallback` classes.
- **Kafka** — publish an `OrderCreated` event from order-service; product-service consumes it to
  adjust stock asynchronously instead of the synchronous `decrease-stock` call.
- **Spring Cloud Config** — externalize `application.yml` per environment.
- **Postgres/MySQL** — swap the H2 datasource for a real database via Docker Compose.
- **Spring Security + JWT** — secure the gateway as the single auth entry point.
