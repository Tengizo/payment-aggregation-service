# Payment Aggregation Service

A high-performance Spring Boot microservice for processing payment transactions and maintaining daily balance aggregations with atomic operations and thread-safety guarantees.

## Technology Stack
- Java 17
- Framework: Spring Boot 2.7.x
- Docker & Docker Compose
- Database: PostgreSQL with Flyway migrations
- Testing: JUnit 5, Mockito
- API Documentation: OpenAPI 3.0 / Swagger UI
- Build Tool: Maven

## Key Design Decisions

1. CTE-Based Atomic Operations: Single database round-trip ensures consistency
2. Database-Level Concurrency: PostgreSQL row-level locks handle concurrent updates
3. Idempotency via Primary Key: Transaction ID uniqueness prevents duplicates
4. Pre-Aggregated Balances: Fast retrieval without runtime calculations

## 🛠️ Quick Start

### 1. Start DB and APP 
```bash
docker-compose up -d
```
### 2. Start Postgres using docker
```bash
docker-compose up postgres -d
```

### 4. Run the Application
```bash
mvn spring-boot:run
```

The service will start on http://localhost:8080

### 4. Access API Documentation
Open your browser and navigate to:
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

##  API Endpoints

### Process Transaction
```bash
POST /transactions
Content-Type: application/json

{
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "accountId": "ACC-123",
  "amount": 150.75,
  "currency": "USD",
  "timestamp": "2025-01-30T12:34:56Z"
}
```

Responses:
- 201 Created - New transaction processed
- 200 OK - Duplicate transaction (idempotent)
- 400 Bad Request - Validation error
- 409 Conflict - Same transaction id different values

### Get Daily Balance
```bash
GET /balances/{accountId}?date=2025-01-30
```

Response:
```json
{
  "accountId": "ACC-123",
  "date": "2025-01-30",
  "balances": [
    {"currency": "USD", "balance": 1250.50},
    {"currency": "EUR", "balance": 850.00}
  ]
}
```
## Tests
`TransactionControllerIntegrationTest.testConcurrentTransactions_SameAccount_CorrectBalance` can be used to test concurrent transactions.
- the number of threads can be adjusted by modifying threadCount.