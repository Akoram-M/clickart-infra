# Clickart Microservices Architecture

## 1. Architectural Topology
This repository defines a highly distributed, containerized e-commerce microservices architecture based on Spring Boot 3. The system enforces strict synchronous ingress routing at the edge and asynchronous event-driven eventual consistency in the backend.

### Traffic Flow Matrix
1. **Ingress:** External clients hit the **API Gateway** (Port 8083).
2. **Security Edge:** Gateway intercepts the request, validates the Keycloak JWT against the internal Identity Provider.
3. **Throttling & Resilience:** Gateway applies Redis-backed Token Bucket rate limiting and prepares Resilience4j Circuit Breakers.
4. **Service Discovery:** Gateway queries **Eureka Registry** to resolve dynamic backend IP addresses.
5. **Reverse Proxy:** Gateway strips prefixes and routes HTTP traffic to blocking Tomcat backends (Product, Order, Inventory).
6. **Event Broker:** Synchronous writes (e.g., Order creation) trigger asynchronous Kafka producers (`order-placed-topic`), consumed downstream (Inventory/Notification) for state alignment.

---

## 2. Technology Stack & Strict Versioning

### Core Frameworks
* **Java:** 17
* **Framework:** Spring Boot 3.x (WebFlux for Gateway, WebMVC for Backend)
* **Cloud Native:** Spring Cloud (Gateway, Config, Netflix Eureka)

### Infrastructure & Data Layer (Dockerized)
* **API Gateway Engine:** Netty (Non-blocking)
* **Backend Engines:** Apache Tomcat (Blocking)
* **Identity & Access Management:** Keycloak `23.0`
* **Message Broker:** Apache Kafka `3.7.0` (with Zookeeper/KRaft)
* **Relational Database:** PostgreSQL `15-alpine` (Order/Inventory)
* **NoSQL Database:** MongoDB `7.0` (Product Catalog)
* **In-Memory Cache:** Redis `7.0-alpine` (Rate Limiting/Caching)

### Observability & Telemetry
* **Tracing:** Micrometer + Zipkin `latest`
* **Metrics:** Prometheus `v2.45.0`
* **Log Aggregation:** Loki `2.9.0` + Promtail `2.9.0`
* **Visualization:** Grafana `10.0.3`

---

## 3. Dependency Matrix (POM Level)

### API Gateway (Reactive / WebFlux)
* `spring-boot-starter-webflux` (Netty server)
* `spring-cloud-starter-gateway` (Routing filter chain)
* `spring-boot-starter-oauth2-resource-server` (JWT validation)
* `spring-cloud-starter-netflix-eureka-client` (Registry polling)
* `spring-cloud-starter-config` (Centralized config polling)
* `spring-cloud-starter-circuitbreaker-reactor-resilience4j` (Reactive fail-fast mechanism)
* `spring-boot-starter-data-redis-reactive` (Token bucket rate limiter)
* `springdoc-openapi-starter-webflux-ui` `2.5.0` (Swagger UI aggregation)

### Backend Microservices (Blocking / WebMVC)
* `spring-boot-starter-web` (Tomcat server)
* `spring-kafka` (Producer/Consumer configurations)
* `spring-boot-starter-data-jpa` / `spring-boot-starter-data-mongodb` (Persistence)
* `springdoc-openapi-starter-webmvc-ui` `2.5.0` (OpenAPI schema generation)

---

## 4. Network & Port Mapping Registry

| Service | Internal Protocol | Host Port | Internal Port | Description |
| :--- | :--- | :--- | :--- | :--- |
| **API Gateway** | HTTP / Netty | `8083` | `8083` | Primary external entry point. |
| **Config Server** | HTTP | `8888` | `8888` | Binds to GitHub for YAML distribution. |
| **Eureka Server** | HTTP | `8761` | `8761` | Internal DNS / IP Registry. |
| **Keycloak** | HTTP | `8081` | `8080` | OIDC Provider. Validates tokens natively. |
| **Product Service** | HTTP / Tomcat | N/A | `8080` | Dynamically assigned via Eureka. |
| **Order Service** | HTTP / Tomcat | N/A | `8082` | Dynamically assigned via Eureka. |
| **Inventory Service**| HTTP / Tomcat | N/A | `8082` | Dynamically assigned via Eureka. |
| **Kafka Broker** | TCP | `9092` | `29092`| Event streaming backbone. |
| **PostgreSQL** | TCP | `5434` | `5432` | Relational storage. |
| **MongoDB** | TCP | `27018` | `27017` | Document storage. |
| **Redis** | TCP | `6379` | `6379` | Ephemeral state / Rate Limit cache. |
| **Zipkin** | HTTP | `9411` | `9411` | Distributed trace ingestion. |
| **Grafana** | HTTP | `3000` | `3000` | Dashboard UI. |

---

## 5. Security & Resilience Definitions

### OAuth2 / JWT Validation Layer
The Gateway acts as an OAuth2 Resource Server. 
* **Issuer URI constraint:** `http://localhost:8081/realms/clickart-realm` (Validates external client tokens).
* **JWK Set URI:** `http://clickart-keycloak-1:8080/...` (Internal Docker network call to fetch public keys).
* **Whitelist:** Deep wildcard paths (`/*/v3/api-docs/**`) and fallback endpoints (`/fallback/**`) bypass the security chain to allow Swagger UI aggregation and error rendering.

### Circuit Breaker (Resilience4j)
Implements fail-fast architecture at the Gateway layer to prevent thread exhaustion.
* Trips on: `HTTP 503` returned by the RoundRobinLoadBalancer.
* Action: Aborts connection to dead backend, executes `forward:` directive to `FallbackController`.
* Preserves original HTTP methods (GET/POST) while rewriting the URI path to the fallback method.

### Request Rate Limiter (Redis)
Utilizes the Token Bucket algorithm to throttle abusive traffic.
* **Key Resolver:** Extracts the `Principal::getName` from the validated Keycloak JWT to limit per-user, not per-IP.
* **Replenish Rate:** 20 requests per second.
* **Burst Capacity:** 50 requests max.
* **Rejection:** Returns `429 Too Many Requests` when bucket drops to 0.