# ClickArt: Production-Grade Distributed E-Commerce Architecture

## Executive Summary
ClickArt is an event-driven, fault-tolerant backend system built on Spring Boot 3 designed to handle high-throughput, asynchronous business workflows. It strictly eliminates single points of failure, data loss during partial outages, and distributed transaction inconsistencies.

---

## 1. Architectural Patterns & Problem Resolution

### 1. Perimeter Security & Edge Routing
* **Pattern:** API Gateway Pattern (`Spring Cloud Gateway` - Reactive WebFlux)
* **Problem Solved:** Direct client-to-service communication exposes internal topologies, complicates CORS policies, and causes thread starvation under unauthenticated DDoS loads.
* **Implementation:** Acts as a reverse proxy, terminating external HTTP requests, validating JWTs via Keycloak at the edge, and dynamically routing traffic to internal Tomcat backends.

### 2. Edge Identity & Access Management (IAM)
* **Pattern:** Centralized OAuth2/OIDC Resource Server (`Keycloak`)
* **Problem Solved:** Redundant, vulnerable authentication implementations across discrete domain services.
* **Implementation:** Gateway cryptographically validates JWT access tokens against Keycloak’s JWKS endpoint statelessly, passing user identity downstream via sanitized HTTP headers.

### 3. Edge Rate Limiting & Denial of Service Protection
* **Pattern:** Distributed Token Bucket Algorithm (`Redis`)
* **Problem Solved:** Volumetric API abuse leading to database lockups and JVM Out-Of-Memory (OOM) crashes.
* **Implementation:** Reactive Redis cluster tracking request counts per authenticated subject (`sub` claim), enforcing replenishment rates and shedding excess load with HTTP `429 Too Many Requests`.

### 4. Cascading Failure Prevention
* **Pattern:** Circuit Breaker & Fallback (`Resilience4j`)
* **Problem Solved:** Synchronous network latency causing thread pool exhaustion across upstream calling services.
* **Implementation:** Wraps inter-service network boundaries to monitor error rates. Bypasses doomed calls when thresholds fail, invoking local fallback degradations.

### 5. Distributed Transaction Management
* **Pattern:** Choreography-Based Saga (`Apache Kafka`)
* **Problem Solved:** Distributed ACID transactions and Two-Phase Commit (2PC) introduce extreme latency and single points of coordination failure across isolated microservice databases.
* **Implementation:** Decentralized, event-driven state transitions where services independently emit and consume domain events, executing local compensating transactions upon downstream business failures.

### 6. Dual-Write Consistency Guarantee
* **Pattern:** Transactional Outbox Pattern (`PostgreSQL` + Polling Relay)
* **Problem Solved:** Non-atomic dual writes (DB write followed by Kafka publish) risk permanent data loss if the application crashes mid-execution.
* **Implementation:** Persists business state and domain events atomically within a single local ACID database transaction. An asynchronous relay polls and publishes to Kafka with at-least-once delivery guarantees.

### 7. Consumer Deduplication
* **Pattern:** Idempotent Consumer (`SQL-level Deduplication`)
* **Problem Solved:** Kafka’s at-least-once delivery semantics cause duplicate message processing during network retries or consumer group rebalances, corrupting financial/inventory state.
* **Implementation:** Tracks incoming unique event identifiers (`eventId`) within a transactional `ProcessedEvent` table, dropping duplicate deliveries before executing business logic.

### 8. Poison Pill Isolation
* **Pattern:** Dead Letter Queue (DLQ) & Error Handler (`Spring Kafka`)
* **Problem Solved:** Unhandled runtime or deserialization exceptions cause consumers to fail continuously, blocking the entire partition and halting valid message processing.
* **Implementation:** Intercepts failures via `DefaultErrorHandler`. Retries transient errors via `FixedBackOff` while immediately routing fatal data structural faults (e.g., `MessageConversionException`) to an `.DLQ` topic.

### 9. Distributed Tracing & Observability
* **Pattern:** Distributed Context Propagation (`Micrometer` + `OpenZipkin`)
* **Problem Solved:** Inability to correlate requests, measure latency, and identify failure root causes across asynchronous network and broker boundaries.
* **Implementation:** Injects unique `traceId` and `spanId` values at the Gateway, propagating them across HTTP headers and Kafka record headers for visual lifecycle aggregation in Zipkin.

---

## 2. Infrastructure & Network Topology

| Service Name | Protocol | Port | Persistence / Role |
| :--- | :--- | :--- | :--- |
| **API Gateway** | HTTP | `8083` | Edge Security, Ingress Routing, Redis Rate Limiting |
| **Keycloak** | HTTP | `8080` | IAM, OAuth2/OIDC Identity Provider |
| **Config Server** | HTTP | `8888` | Centralized External Git-Backed Configuration |
| **Eureka Server** | HTTP | `8761` | Dynamic Service Discovery Registry |
| **Product Service** | HTTP | `8081` | MongoDB 7.0 (Product Catalog Domain) |
| **Order Service** | HTTP | `8082` | PostgreSQL 15 (`orders_db`), Outbox Publisher |
| **Inventory Service** | HTTP | `8084` | PostgreSQL 15 (`inventory_db`), Idempotent Consumer & DLQ |
| **Kafka Broker** | TCP | `29092` | Event Bus (KRaft Mode, Topic Partitioning) |

---

## 3. SMTS Technical Depth & Interview Defense Matrix

| Architectural Area | Level 1: Junior/Mid-Level Understanding | Level 2: SMTS / Senior Architectural Depth (Required) |
| :--- | :--- | :--- |
| **Kafka Messaging** | "I use `@KafkaListener` to read messages from a topic." | Understands partition leader election, consumer group rebalancing protocols (Eager vs Cooperative Sticky), offset commit strategies (`ackMode`), ISR mechanics, and consumer lag monitoring. |
| **Distributed Transactions** | "I use Kafka events to connect services together." | Explains the trade-offs of Choreography vs Orchestration (temporal coupling vs management overhead), compensating transactions, and why distributed locks (2PC) fail at high scale. |
| **Transactional Outbox** | "I save the event to a database table first." | Analyzes the throughput trade-offs between Polling Relays (DB query load) vs Log-based CDC (Debezium reading WAL engine files directly), and how to prevent outbox table bloat. |
| **Fault Tolerance** | "I configured a DLQ to catch bad messages." | Diagnoses the difference between transient infrastructure failures (DB timeouts requiring backoff) and non-retryable poison pills (malformed payloads), preventing head-of-line blocking. |
| **Testing** | "I write unit tests with `@Mock` and Mockito." | Implements **Testcontainers** to validate real database queries, concurrency locks, and Kafka consumer group interactions against real, containerized dependencies. |

---

## 4. Production Upskilling Roadmap

1. **Automated Integration Testing (Testcontainers):**
   * Eliminate brittle in-memory mocks (`H2`).
   * Write integration test suites spinning up dynamic PostgreSQL and Kafka containers during Maven builds.
2. **Container Orchestration (Kubernetes/EKS):**
   * Write declarative Kubernetes manifests (`Deployments`, `StatefulSets`, `Services`, `ConfigMaps`, `Secrets`).
   * Implement Liveness and Readiness probes aligned with Spring Boot Actuator health indicators.
3. **CI/CD Pipeline Automation (GitHub Actions):**
   * Automate Maven compilation, static code analysis, image building, and multi-stage container deployments.
4. **Zero-Trust Network Security (mTLS):**
   * Configure mutual TLS authentication for inter-service communication within the service mesh.