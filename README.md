# relaycore

A game online-services backend — player accounts, matchmaking, inventory/store, and leaderboards — built as a **modular monolith** with an **event-driven core** on Apache Kafka.

It models the server-side concerns that sit behind a live multiplayer game: identity and auth, a matchmaking queue, an inventory/economy with correctness guarantees, and ranked leaderboards — wired together by Kafka events rather than direct calls.

## Stack

- **Java 21**, **Spring Boot 3.5**
- **PostgreSQL** (Flyway migrations) · **Redis** (cache + leaderboards) · **Apache Kafka** (event backbone; Redpanda locally, Upstash in prod)
- REST + OpenAPI/Swagger · JWT auth · Micrometer → Prometheus/Grafana
- JUnit 5 + Testcontainers · GitHub Actions CI · Docker

## Architecture

One Spring Boot application, internally split into hard-bounded modules that communicate via in-process calls **and Kafka events**:

```
queue for match (matchmaking)
   -> match completes -> [match-completed] --(Kafka)--+
                                                       |
   inventory grants currency + items <-----------------+ -> [reward-granted]
   leaderboard (Redis sorted set) <--------------------+
   + match-history projection from the event stream
```

This keeps Kafka load-bearing (it carries the reward/leaderboard loop) while staying a single deployable — see the design notes for why a modular monolith over microservices.

## Run locally

```bash
# 1. backing services
docker compose up -d

# 2. the app  (needs JDK 21 + the Maven wrapper)
./mvnw spring-boot:run
```

Then open Swagger UI at `http://localhost:8080/swagger-ui.html` and health at `http://localhost:8080/actuator/health`.

## Status

Early development. See the module map and milestone plan in the design notes.

## License

MIT
