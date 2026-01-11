# Car Pooling Service - Java Spring Boot

A high-performance car pooling service built with Java Spring Boot, designed to efficiently manage car availability and group assignments for ride‑sharing operations.

## Architecture Overview

### Technology Stack

- **Framework**: Spring Boot 2.7.x
- **Language**: Java 11
- **Storage**: In-memory
- **Build Tool**: Maven
- **Testing**: JUnit 5 + Spring Boot Test
- **Containerization**: Docker

### High-Level Design

The service follows a **layered architecture**:

- **Controller Layer** (`controller`):
  - Exposes the HTTP API.
  - Maps requests/responses to DTOs.
  - Centralized error handling via `GlobalExceptionHandler`.
- **Service Layer** (`service`):
  - Encapsulates business rules and orchestration.
  - Coordinates cars, groups and journeys.
- **Repository Layer** (`repository`):
  - In-memory implementations for cars, groups and journeys.
  - Handles concurrency control and efficient queries.
- **Model & DTO Layer** (`model`, `dto`, `mapper`):
  - `model` contains internal domain entities (e.g. `Car`).
  - `dto` contains public API payloads (`CarDTO`, `JourneyDTO`).
  - `mapper` converts between DTOs and models.

### In-Memory Storage & Concurrency

The system is designed to run **without an external database**, relying on thread-safe in-memory repositories to allow concurrent access without sacrificing data integrity.

#### How concurrency and race conditions are handled

Concurrency is managed in a simple but robust way:
Critical data structures are protected with standard Java synchronization primitives to ensure multiple requests can be handled safely without risking data corruption or inconsistent state. The most important parts:

- **Cars** (`InMemoryCarRepository`):
  - Cars are stored in a `ConcurrentHashMap<ID, Car>`.
  - A bucket system (using `List<Set<Integer>>`) efficiently tracks cars by free seats; `LinkedHashSet` preserves FIFO.
  - All operations that modify car assignment or availability are wrapped using a `ReadWriteLock`, allowing parallel reads but serializing writes. This prevents two threads from assigning the same seat or car at the same time.
  - Thus, if two requests try to assign a group at once, the lock ensures these actions do not overlap or cause race conditions.

- **Groups & Waiting Queue** (`InMemoryGroupRepository`):
  - Active groups are stored in a `ConcurrentHashMap<groupId, people>`.
  - The waiting queue uses a `LinkedHashMap<groupId, people>` to ensure arrival order (FIFO).
  - A `peopleCounter` (`ConcurrentHashMap<people, count>`) speeds up allocation checks.
  - All methods changing the state of the queue or the counter are marked as `synchronized` — only one thread at a time can modify the queue, preventing race conditions when groups are added, assigned, or removed.
  - Read operations can happen in parallel.

- **Journeys** (`InMemoryJourneyRepository`):
  - Manages `groupId → carId` assignments for current journeys.
  - This mapping uses a `ConcurrentHashMap`, allowing safe concurrent get/put operations.

**In summary:**
Each shared structure is protected either with `ConcurrentHashMap`, a `ReadWriteLock`, or `synchronized` methods - depending on its needs.
Any operation that updates several pieces of state (e.g. dequeue a group and assign it to a car) is done atomically inside the synchronization block, so no intermediate or invalid states are exposed.
This allows the service to safely handle many HTTP requests in parallel with consistent, predictable results, and without the risk of assigning the same car or seat twice.

### Performance & Scalability

The solution is optimized for **10^5 – 10^6 cars and waiting groups**:

- **Algorithmic complexity**:
  - Car finding: **O(1)** using bucket-based indexing by available seats.
  - Queue processing: **O(n)** in the worst case, early exit via `peopleCounter`.
  - State updates (assign/release seats, enqueue/dequeue groups): **O(1)**.
- **Memory usage**:
  - Only minimal data is stored for each car, group and journey.
  - Avoids heavyweight frameworks or external caches.
- **Concurrency**:
  - Repositories are explicitly designed for concurrent access.
  - Compound operations are protected with locks or `synchronized` methods.

## API Endpoints

### GET /status

Health check endpoint indicating service readiness.

**Response**: `200 OK`

### PUT /cars

Loads the list of available cars and resets application state.

**Request Body**:
```json
[
  { "id": 1, "seats": 4 },
  { "id": 2, "seats": 6 }
]
```

**Validations**:
- Cars must have seats between 4 and 6
- Car IDs must be positive integers

**Response**: `200 OK` or `400 Bad Request`

### POST /journey

Registers a group requesting a journey.

**Request Body**:
```json
{
  "id": 1,
  "people": 4
}
```

**Validations**:
- People count must be between 1 and 6
- Group ID must be a positive integer
- Group ID must be unique

**Response**: `200 OK` or `400 Bad Request`

### POST /dropoff

Removes a group from the system (whether traveling or waiting).

**Request Body** (form-urlencoded): `ID=1`

**Response**:
- `200 OK` - Group successfully dropped off
- `404 Not Found` - Group doesn't exist
- `400 Bad Request` - Invalid request

### POST /locate

Returns the car assigned to a group, or indicates they're waiting.

**Request Body** (form-urlencoded): `ID=1`

**Response**:
- `200 OK` with car JSON if assigned
  ```json
  {
    "id": 1,
    "seats": 4
  }
  ```
- `204 No Content` if waiting
- `404 Not Found` if group doesn't exist
- `400 Bad Request` if request is invalid

## Business Logic

### Car Assignment

When a group requests a journey:

1. **Immediate allocation**:
   - Use the **seat bucket index** to find the best-fitting car with at least `people` available seats.
   - If such a car exists, reserve its seats and create a `groupId → carId` mapping.
2. **Queueing**:
   - If no car is available, the group is added to the **waiting queue** (FIFO) and `peopleCounter` is updated.

### Dropoff & Reallocation

When a group is dropped off:

1. **Release seats** from the associated car.
2. **Evaluate the waiting queue**:
   - Check `peopleCounter` to see if any groups could fit in the newly freed seats.
   - Iterate the waiting queue (FIFO), selecting groups that fit in the released capacity.
3. **Assign groups**:
   - For each selected group, reserve seats and create/update the `groupId → carId` mapping.
   - Remove allocated groups from the waiting queue and adjust `peopleCounter`.

### Fairness Strategy

- Groups are **served as fast as possible** while preserving **arrival order when possible**.
- A later group can be served before an earlier group **only if no car can serve the earlier group**.
- This avoids starvation of small groups and keeps utilization high, at the cost of potentially long waits for very large groups.

## Running the Service

### Prerequisites

- **Docker** (required for the `Makefile` workflows).
- **Java 11 + Maven 3.6+** (optional, if you want to run it directly without Docker).

### Quick Start

```bash
# Show all available commands
make help

# Start development server (live reload via Spring DevTools)
make dev

# Start development server with debugger on port 5005
make debug

# Check service health
make status

# Tail application logs
make logs

# Stop the server
make stop
```

The service listens on **port 9091** by default.

### Available Make Commands

- **Development & Execution**
  - `make dev` – Start development server with live reload (bind-mounts source code).
  - `make debug` – Start development server with debugger exposed on port 5005.
  - `make compile` – Run `mvn compile` inside the dev container (use after code changes if your IDE is not auto-compiling).
  - `make restart` – Restart the running dev container.
  - `make logs` – Show container logs (follow mode).
  - `make stop` – Stop and remove the dev container.
  - `make status` – Call `/status` to check if the service is healthy.
  - `make ssh` – Open a shell inside the running container.

- **Testing**
  - `make test` – Run tests. If a dev container is running, it reuses it; otherwise, it builds a test image and runs `mvn test`.
  - `make test-quick` – Run tests **only** in a running dev container (fastest option).
  - `make test-ci` – Clean build for CI/CD: builds the image from scratch and runs `mvn test`.

- **Production**
  - `make build` – Build the production Docker image (multi-stage, optimized JAR).
  - `make run` – Build (no cache) and run the production container on port 9091.

- **Cleanup**
  - `make clean` – Stop/remove containers and delete the dev/test/prod images.

## Configuration

Application configuration lives in `src/main/resources/application.properties`:

```properties
server.port=9091
spring.application.name=car-pooling
logging.level.com.cabify.carpooling=INFO
```

You can override `server.port` or logging levels using standard Spring Boot mechanisms (environment variables, command-line args, etc.).

## Testing

The project includes unit tests and integration tests focusing on both **business logic** and **API behavior**.

### Test Types

- **Unit tests** (`src/test/java/com/cabify/carpooling/service`):
  - `CarServiceTest` – Validates car selection, reservation, and seat management logic.
  - `GroupServiceTest` – Validates waiting queue behavior and group allocation rules.
- **Integration tests** (`src/test/java/com/cabify/carpooling/controller`):
  - `CarPoolingControllerIntegrationTest` – Exercises the REST API contract end‑to‑end.
- **Application smoke test**:
  - `CarPoolingApplicationTests` – Basic Spring Boot context and smoke tests.

### Running Tests

```bash
# Run all tests using Docker (smart behavior, reuses dev container when available)
make test

# Fast tests using an already running dev container
make dev      # if not running yet
make test-quick

# CI-style clean test run
make test-ci
```

### Manual Testing with curl

```bash
# 1. Start the server
make dev

# 2. Health check
curl http://localhost:9091/status

# 3. Load cars
curl -X PUT http://localhost:9091/cars \
  -H "Content-Type: application/json" \
  -d '[{"id":1,"seats":4},{"id":2,"seats":6}]'

# 4. Request a journey
curl -X POST http://localhost:9091/journey \
  -H "Content-Type: application/json" \
  -d '{"id":1,"people":4}'

# 5. Locate the group
curl -X POST http://localhost:9091/locate \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "ID=1"

# 6. Dropoff
curl -X POST http://localhost:9091/dropoff \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "ID=1"
```

## Project Structure

```text
src/
├── main/
│   ├── java/
│   │   └── com/cabify/carpooling/
│   │       ├── controller/
│   │       │   ├── CarPoolingController.java
│   │       │   └── GlobalExceptionHandler.java
│   │       ├── dto/
│   │       │   ├── CarDTO.java
│   │       │   └── JourneyDTO.java
│   │       ├── mapper/
│   │       │   └── CarMapper.java
│   │       ├── model/
│   │       │   └── Car.java
│   │       ├── repository/
│   │       │   ├── CarRepository.java
│   │       │   ├── GroupRepository.java
│   │       │   ├── JourneyRepository.java
│   │       │   └── inmemory/
│   │       │       ├── InMemoryCarRepository.java
│   │       │       ├── InMemoryGroupRepository.java
│   │       │       └── InMemoryJourneyRepository.java
│   │       ├── service/
│   │       │   └── CarPoolingService.java
│   │       ├── exception/
│   │       │   ├── ExistingGroupException.java
│   │       │   ├── GroupNotFoundException.java
│   │       │   └── InvalidPayloadException.java
│   │       └── CarPoolingApplication.java
│   └── resources/
│       └── application.properties
└── test/
    └── java/
        └── com/cabify/carpooling/
            ├── CarPoolingApplicationTests.java
            ├── controller/
            │   └── CarPoolingControllerIntegrationTest.java
            └── service/
                ├── CarPoolingServiceTest.java
                └── CarPoolingServiceConcurrencyTest.java
```

## Design & Trade-offs

### Why In-Memory Storage?

- **Performance**: Sub‑millisecond operations for typical workloads.
- **Simplicity**: No external stateful services required; easy to run and test anywhere Docker is available.
- **Determinism**: The service can be reset deterministically using `PUT /cars`.
- **Challenge constraints**: Keeps the solution fully self-contained, as requested in the original brief.

### Why Bucket-Based Car Allocation Instead of Binary Search?

- **Constant-time lookup**: For a small, fixed domain of seat counts (1–6), bucket indexing provides O(1) lookup instead of O(log n).
- **Fairness**: Combined with `LinkedHashSet`, it preserves insertion order within a bucket.
- **Simplicity**: Logic is easy to reason about and well-suited for the constraints of the problem.

### Separation of Concerns

- Controllers stay thin and focused on HTTP concerns.
- Services encapsulate business rules and orchestration.
- Repositories encapsulate data access and concurrency details.
- DTOs prevent leaking internal models through the public API.

## Production Readiness Notes

This project is a coding challenge; however, for a real production deployment you would typically add:

- **Monitoring & Metrics**: Expose application and business metrics (e.g. via Micrometer/Prometheus) and integrate with APM tools.
- **High availability**: Replicate state or move to an external data store if multiple instances are required.
- **Security**: Authentication/authorization, rate limiting and input hardening.
- **Configuration management**: Use environment-based configuration for ports, logging, etc.

## License / Context

This repository is part of a **Cabify coding challenge**. It is intended as an example of high-quality code, architecture and documentation under the constraints of the exercise.
