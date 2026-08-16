# Clickart Microservices Architecture

## Executive Summary
Clickart is a highly distributed, production-grade e-commerce backend built on Spring Boot 3. It leverages an event-driven architecture to ensure high availability, eventual consistency, and fault tolerance across a decentralized data model. 

## 1. Architectural Patterns & Problem Resolution

This system implements advanced distributed systems patterns to solve specific network and transaction vulnerabilities.

### The Perimeter & Edge Routing
*   **Feature:** Spring Cloud Gateway (WebFlux)
*   **The Problem:** Exposing raw backend microservices directly to the internet creates severe security vulnerabilities, tight coupling with clients, and CORS nightmares.
*   **The Solution:** A unified API Gateway acts as the single entry point (Reverse Proxy). It strips internal routing paths and handles cross-cutting concerns (security, throttling) before traffic ever reaches the blocking Tomcat backends.

### Identity & Access Management (IAM)
*   **Feature:** Keycloak (OAuth2 / OpenID Connect)
*   **The Problem:** Implementing custom password hashing, session management, and role-based access control inside every microservice violates the DRY principle and introduces critical security flaws.
*   **The Solution:** The Gateway operates as an OAuth2 Resource Server. It intercepts inbound requests, validates the cryptographic signature of the Keycloak JWT using a centralized JWK Set URI, and strictly rejects unauthenticated traffic at the edge.

### Cascading Failure Prevention
*   **Feature:** Resilience4j Circuit Breakers
*   **The Problem:** In a distributed network, if the Order Service synchronously calls a dead Inventory Service, threads will block waiting for a TCP timeout. Under high load, this instantly exhausts the Netty/Tomcat thread pools, causing the entire application to crash (Cascading Failure).
*   **The Solution:** The Circuit Breaker wraps inter-service calls. If the error rate (e.g., 503 Service Unavailable) breaches a threshold, the circuit "opens" and instantly aborts outbound network calls. It routes the request to a local `FallbackController`, providing graceful degradation instead of a system crash.

### Volumetric Traffic Control
*   **Feature:** Redis-Backed Token Bucket Rate Limiting
*   **The Problem:** The API Gateway natively accepts infinite traffic. A malicious actor or a rogue frontend script can flood the backend with thousands of requests per second, triggering OutOfMemory (OOM) errors or database lockups.
*   **The Solution:** A reactive Redis cache tracks incoming requests per user (extracting the ID from the JWT). It enforces a strict Token Bucket algorithm (e.g., 20 requests/sec replenish, 50 request burst). Violators are instantly rejected at the edge with a `429 Too Many Requests` status.

### Distributed Transaction Management
*   **Feature:** Choreography Saga Pattern (Apache Kafka)
*   **The Problem:** A monolithic `@Transactional` annotation cannot span across the Order database and the Inventory database. Using Two-Phase Commit (2PC) over a network causes severe latency and database deadlocks.
*   **The Solution:** A decentralized Saga. The `order-service` creates a `PENDING` order and emits an event. The `inventory-service` consumes it. If inventory allocation fails (out of stock), it emits an `InventoryFailedEvent`. The `order-service` consumes this failure and executes a **Compensating Transaction** to update the order status to `CANCELLED`. This ensures **Eventual Consistency** without locking.

### Dual-Write Data Loss Prevention
*   **Feature:** Transactional Outbox Pattern
*   **The Problem:** If a service writes to PostgreSQL and then attempts to publish an event to Kafka, a JVM crash between those two lines of code results in the database committing the data while the Kafka event is lost forever. Downstream services are never notified.
*   **The Solution:** The microservice writes the business entity (Order) and the integration event (OrderPlaced) into an `outbox` table within the **exact same local database transaction**. A separate polling process or Debezium CDC reads the outbox table and guarantees delivery to Kafka.

### Distributed Observability
*   **Feature:** Micrometer Tracing & Zipkin
*   **The Problem:** When an asynchronous process fails across three different microservices and a Kafka broker, searching raw logs without a correlation ID makes debugging impossible.
*   **The Solution:** Micrometer injects a unique `traceId` at the API Gateway. This ID automatically propagates through HTTP headers and Kafka message headers. Zipkin aggregates these spans, providing a visual timeline of the entire request lifecycle.

---

## 2. Technology Stack & Infrastructure

### Core Frameworks
*   **Language:** Java 17
*   **Framework:** Spring Boot 3.x (WebFlux for Edge, WebMVC for Domain Services)
*   **Cloud Native:** Spring Cloud (Gateway, Config, Netflix Eureka)

### Containerization & Orchestration
*   **Engine:** Docker Engine
*   **Orchestrator:** Docker Compose (V2)
*   *(Note: Kubernetes manifests are not yet implemented in this baseline).*
*   **CI/CD Pipeline:** Automated Bash compilation and image-burning script (`build-all.sh`).

### Data Persistence
*   **Relational Databases:** PostgreSQL 15 (Dedicated schemas for `orders_db`, `inventory_db`)
*   **Document Store:** MongoDB 7.0 (`product_db`)
*   **In-Memory Cache:** Redis 7.0-alpine (Rate Limiting state)

### Event Streaming & Telemetry
*   **Message Broker:** Apache Kafka 3.7.0 (with KRaft/Zookeeper)
*   **Tracing:** OpenZipkin
*   **Metrics & Logging:** Prometheus, Grafana Loki, Promtail

---

## 3. Network Topology & Routing

| Service Name | Protocol | Internal Port | Description |
| :--- | :--- | :--- | :--- |
| **API Gateway** | HTTP | `8083` | External Ingress / Edge Security |
| **Eureka Server** | HTTP | `8761` | Dynamic Service Discovery |
| **Config Server** | HTTP | `8888` | Centralized Git-backed YAML distribution |
| **Keycloak** | HTTP | `8080` | IAM and JWT Provider |
| **Product Service**| HTTP | `8080` | Catalog Management |
| **Order Service** | HTTP | `8082` | Transactional Order Processing |
| **Inventory Service**| HTTP | `8082` | Stock Allocation |
| **Kafka Broker** | TCP | `29092` | Asynchronous Event Bus |