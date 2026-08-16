# Clickart Microservices Architecture

## Executive Summary
Clickart is a highly distributed, production-grade e-commerce backend built on Spring Boot 3. It leverages an event-driven architecture to ensure high availability, eventual consistency, and fault tolerance across a decentralized data model.

---

## 1. Architectural Patterns & Problem Resolution

### The Perimeter & Edge Routing
* **Feature:** Spring Cloud Gateway (WebFlux)
* **The Problem:** Exposing raw backend microservices directly to the internet creates severe security vulnerabilities, tight coupling with clients, and CORS nightmares.
* **The Solution:** A unified API Gateway acts as the single entry point (Reverse Proxy). It strips internal routing paths and handles cross-cutting concerns (security, throttling) before traffic reaches downstream Tomcat backends.

### Identity & Access Management (IAM)
* **Feature:** Keycloak (OAuth2 / OpenID Connect)
* **The Problem:** Implementing custom password hashing, session management, and role-based access control inside every microservice violates the DRY principle and introduces critical security flaws.
* **The Solution:** The Gateway operates as an OAuth2 Resource Server. It intercepts inbound requests, validates the cryptographic signature of the Keycloak JWT using a centralized JWK Set URI, and strictly rejects unauthenticated traffic at the edge.

### Cascading Failure Prevention
* **Feature:** Resilience4j Circuit Breakers
* **The Problem:** In a distributed network, if the Order Service synchronously calls a dead Inventory Service, threads will block waiting for a TCP timeout. Under high load, this instantly exhausts the Netty/Tomcat thread pools, causing the entire application to crash.
* **The Solution:** The Circuit Breaker wraps inter-service calls. If the error rate breaches a threshold, the circuit "opens" and instantly aborts outbound network calls, routing to a fallback mechanism.

### Volumetric Traffic Control
* **Feature:** Redis-Backed Token Bucket Rate Limiting
* **The Problem:** The API Gateway natively accepts infinite traffic, leaving backend services vulnerable to Denial of Service (DoS) attacks and OutOfMemory (OOM) errors.
* **The Solution:** A reactive Redis cache tracks incoming requests per user (extracting identity from the JWT) and enforces a strict Token Bucket algorithm, instantly rejecting non-compliant requests with HTTP 429 Too Many Requests.

### Distributed Transaction Management
* **Feature:** Choreography-Based Saga Pattern (Apache Kafka)
* **The Problem:** A standard `@Transactional` annotation cannot span distributed databases, and Two-Phase Commit (2PC) creates heavy network latency and blocking locks.
* **The Solution:** A decentralized Saga where services publish and consume domain events independently, using compensating transactions to roll back states when intermediate failures occur.

### Dual-Write Data Loss Prevention
* **Feature:** Transactional Outbox Pattern
* **The Problem:** Writing to a local database and publishing to Kafka sequentially risks dual-write inconsistency if the service crashes between the two actions.
* **The Solution:** The service persists both the domain entity and an outbox event record atomically in a single ACID database transaction. A separate relay process reads the table and guarantees delivery to Kafka.

### Distributed Observability
* **Feature:** Micrometer Tracing & Zipkin
* **The Problem:** Tracking requests and identifying failure points across decoupled asynchronous services is impossible without unified request correlation.
* **The Solution:** Micrometer generates and injects a unified `traceId` at the edge gateway that propagates through HTTP and Kafka headers, centralized in Zipkin for distributed tracing.

---

## 2. Technology Stack & Infrastructure

### Core Frameworks
* **Language:** Java 17 (Strict Typing)
* **Framework:** Spring Boot 3.x (WebFlux for Gateway, WebMVC for Domain Services)
* **Cloud Native:** Spring Cloud (Gateway, Config Server, Netflix Eureka)

### Persistence & In-Memory Storage
* **Relational Persistence:** PostgreSQL 15 (Dedicated schemas: `orders_db`, `inventory_db`)
* **Document Persistence:** MongoDB 7.0 (`product_db`)
* **Distributed Caching:** Redis 7.0-alpine (Rate-limiting state)

### Messaging & Observability
* **Message Broker:** Apache Kafka 3.6.2 (KRaft mode)
* **Distributed Tracing:** OpenZipkin & Micrometer
* **Metrics & Aggregation:** Prometheus, Grafana Loki, Promtail

---

## 3. Network Topology & Routing

| Service Name | Protocol | Internal Port | Description |
| :--- | :--- | :--- | :--- |
| **API Gateway** | HTTP | `8083` | External Ingress, Reverse Proxy, Edge Authentication |
| **Eureka Server** | HTTP | `8761` | Service Registry & Dynamic Discovery |
| **Config Server** | HTTP | `8888` | Centralized Configuration Provider |
| **Keycloak** | HTTP | `8080` | OpenID Connect / OAuth2 Identity Provider |
| **Product Service** | HTTP | `8081` | Product Catalog Management (MongoDB) |
| **Order Service** | HTTP | `8082` | Transactional Order Processing & Outbox Publisher |
| **Inventory Service** | HTTP | `8084` | Stock Management, Idempotent Consumer & DLQ |
| **Kafka Broker** | TCP | `29092` | Distributed Asynchronous Event Stream |

---

## 4. Production Architectural Implementation Matrix

### Implemented Architectural Features & Patterns
* **API Gateway Pattern:** Unified edge reverse proxy handling security, request rewriting, and ingress routing.
* **Centralized Identity & Access Management (IAM):** Stateless JWT cryptographic validation at the edge gateway via Keycloak OIDC.
* **Token Bucket Rate Limiting:** Volumetric traffic throttling per user backed by an in-memory Redis cluster.
* **Client-Side Service Discovery:** Dynamic service lookup and client-side load balancing via Netflix Eureka.
* **Externalized Central Configuration:** Git-backed environment configuration management via Spring Cloud Config Server.
* **Circuit Breaker Pattern:** Fail-fast network degradation and cascading failure prevention using Resilience4j.
* **Database-per-Service Pattern:** Isolated, non-shared persistent stores per domain to ensure microservice autonomy.
* **Choreography-Based Saga Pattern:** Decentralized asynchronous transaction workflows governed by Kafka event reactions.
* **Transactional Outbox Pattern:** Atomic dual-write protection using local SQL tables and polling event relays.
* **Consumer Idempotency:** Exactly-once business processing over at-least-once Kafka delivery via `ProcessedEvent` tracking.
* **Dead Letter Queues (DLQ) & Poison Pill Isolation:** Automated rerouting of fatal consumer exceptions via `DefaultErrorHandler` and `DeadLetterPublishingRecoverer`.
* **Distributed Distributed Tracing:** Cross-service request tracing across HTTP and Kafka transport layers using Micrometer and Zipkin.

### Pending Architecture & Upskilling Roadmap
* **Kubernetes (K8s) Orchestration:** Container orchestration to enable declarative rollouts, state management, and Horizontal Pod Autoscaling (HPA).
* **CI/CD Pipelines:** Automated build, test, and container image publishing using GitHub Actions workflows.
* **Ephemeral Integration Testing (Testcontainers):** True-to-prod automated testing using containerized PostgreSQL and Kafka instances instead of in-memory mocks.
* **Consumer-Driven Contract Testing:** Verification of inter-service API and message schemas using Spring Cloud Contract or Pact.
* **Zero-Trust Network Security (mTLS):** Mutual cryptographic TLS authentication for all inter-service HTTP and Kafka TCP traffic.
* **Orchestration-Based Saga (State Machine):** Centralized workflow coordinator using engines like Temporal or Camunda for multi-step compensation workflows.