# Car Pooling Service - Java Spring Boot Implementation

A high-performance car pooling service built with Java Spring Boot, designed to efficiently manage car availability and group assignments for ride-sharing operations.

## Architecture Overview

### Technology Stack

- **Framework**: Spring Boot 3.x
- **Language**: Java 17+
- **Storage**: In-memory (thread-safe with ConcurrentHashMap and synchronized methods)
- **Build Tool**: Maven
- **Testing**: JUnit 5

### Key Architectural Decisions

#### 1. Layered Architecture

The service follows a clean layered architecture:

- **Controller Layer**: Handles HTTP requests and responses
- **Service Layer**: Contains business logic and orchestration
- **Repository Layer**: Manages data persistence (in-memory)
- **Model Layer**: Domain entities (Car, Group, Journey)

#### 2. In-Memory Storage with Thread Safety

The service uses **in-memory storage** with thread-safe implementations:

- `ConcurrentHashMap` for main storage structures
- `synchronized` methods for operations requiring atomicity
- `LinkedHashMap` for maintaining insertion order (FIFO) in the waiting queue

**Storage Components**:
- **CarRepository**: Car definitions and availability tracking
- **GroupRepository**: Registered groups and waiting queue with people counter optimization
- **JourneyRepository**: Active journey assignments (group_id → car_id mapping)

#### 3. Performance Optimizations

1. **Binary Search for Car Selection**: Uses binary search on sorted availability map for O(log n) car finding
2. **Ordered Availability Map**: Cars sorted by available seats (descending) for optimal allocation
3. **People Counter Index**: Fast lookup to determine if groups can be allocated without iterating entire queue
4. **Minimal Data Structures**: Only essential data stored, reducing memory footprint

#### 4. Scalability Approach

The solution is designed to handle **10^4 to 10^5 cars and waiting groups** efficiently:

- **Memory Efficiency**: In-memory storage with efficient data structures
- **Algorithm Efficiency**:
  - Car finding: O(log n) with binary search
  - Queue processing: O(n) but optimized with early termination
  - State updates: O(1) for individual operations
- **Thread Safety**: Proper synchronization for concurrent request handling
- **No External Dependencies**: No database queries or cache network calls

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

### Car Assignment Algorithm

When a group requests a journey:

1. **Check Availability**: Use binary search on sorted availability map to find the best-fitting car
2. **Direct Assignment**: If a suitable car is found, assign immediately and update availability
3. **Queue**: If no car is available, add group to waiting queue (FIFO)

### Dropoff and Reassignment

When a group is dropped off:

1. **Free Seats**: Release the seats occupied by the group
2. **Queue Processing**: Select optimal set of waiting groups that can fit in the freed seats
3. **Assignment**: Assign groups from queue in FIFO order, maximizing seat utilization

### Fairness Strategy

Groups are served in FIFO order when possible, but a smaller group may be served before a larger one if:
- No car can accommodate the larger group
- A car is available that fits the smaller group

This prevents indefinite waiting for large groups while maximizing resource utilization.

## Running the Service

### Prerequisites

- **Docker** (required)
- Java 17 and Maven 3.6+ (optional, only if you want to run without Docker)

### Quick Start (Recommended)

```bash
# See all available commands
make help

# Start development server with live reload (recommended for active development)
make dev-live

# Or start with debugging enabled
make dev-debug

# Or start production-like server (no live reload)
make dev

# Check service health
make status

# View logs
make logs

# Stop the server
make stop
```

The service runs on port `9091` by default.

### Available Make Commands

#### Development
- `make dev` - Start server (production mode)
- `make dev-live` - Start with live reload (auto-reloads on code changes)
- `make dev-debug` - Start with debugging (port 5005)
- `make logs` - Show server logs
- `make stop` - Stop server
- `make status` - Check if server is running

#### Testing
- `make test` - Run all unit and integration tests
- `make test-api` - Test API endpoints (requires running server)

#### Production
- `make build` - Build production Docker image
- `make run` - Run production container

#### Utilities
- `make status` - Check service health
- `make info` - Show project information
- `make clean` - Clean up containers and images
- `make clean-all` - Deep clean including build artifacts

### Configuration

Application configuration can be found in `src/main/resources/application.properties`:

```properties
server.port=9091
spring.application.name=car-pooling
logging.level.com.cabify.carpooling=INFO
```

## Testing

The project includes comprehensive test coverage:

### Unit Tests

- `MapHelperTest`: Tests for binary search and map sorting utilities
- `CarServiceTest`: Tests for car finding and availability management
- `GroupServiceTest`: Tests for group selection and queue management

### Integration Tests

- `CarPoolingApplicationTests`: Basic integration tests
- `CarPoolingControllerIntegrationTest`: Comprehensive API endpoint tests

### Running Tests

```bash
# Run all tests (in Docker)
make test

# Test API endpoints (requires running server)
make dev          # Start server first
make test-api     # Run API tests

# Or use the test script
./test-api.sh
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

# 4. Request journey
curl -X POST http://localhost:9091/journey \
  -H "Content-Type: application/json" \
  -d '{"id":1,"people":4}'

# 5. Locate group
curl -X POST http://localhost:9091/locate \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "ID=1"

# 6. Dropoff
curl -X POST http://localhost:9091/dropoff \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "ID=1"
```

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/cabify/carpooling/
│   │       ├── controller/          # REST controllers
│   │       │   └── CarPoolingController.java
│   │       ├── service/             # Business logic
│   │       │   ├── CarService.java
│   │       │   ├── GroupService.java
│   │       │   └── JourneyService.java
│   │       ├── repository/          # Data access
│   │       │   ├── CarRepository.java
│   │       │   ├── GroupRepository.java
│   │       │   ├── JourneyRepository.java
│   │       │   ├── InMemoryCarRepository.java
│   │       │   ├── InMemoryGroupRepository.java
│   │       │   └── InMemoryJourneyRepository.java
│   │       ├── model/               # Domain entities
│   │       │   ├── Car.java
│   │       │   ├── Group.java
│   │       │   └── Journey.java
│   │       ├── exception/           # Custom exceptions
│   │       │   ├── ExistingGroupException.java
│   │       │   ├── GroupNotFoundException.java
│   │       │   ├── InvalidPayloadException.java
│   │       │   └── DuplicatedIdException.java
│   │       ├── util/                # Utilities
│   │       │   └── MapHelper.java
│   │       └── CarPoolingApplication.java
│   └── resources/
│       └── application.properties
└── test/
    └── java/
        └── com/cabify/carpooling/
            ├── service/             # Unit tests
            ├── controller/          # Integration tests
            └── util/                # Utility tests
```

## Logging and Observability

The service includes structured logging focused on the **business logic layer**:

### Business Events (Service Layer)
- Journey requests and assignments
- Car allocations and updates
- Dropoff processing
- Queue operations
- State resets (car loading)

### Performance Metrics
- Operation timing (car finding, allocation, queue processing)
- Request duration for key operations
- Queue size and allocation statistics

### Log Levels
- **INFO**: Important business operations (journey assignments, dropoffs, state changes)
- **DEBUG**: Detailed operation information (car finding, queue processing, allocations)
- **WARN**: Business rule violations (duplicate groups, already assigned journeys)
- **ERROR**: Exceptions and failures

## Performance Characteristics

- **Car Finding**: O(log n) using binary search on sorted availability map
- **Journey Assignment**: O(1) for direct assignment, O(n) for queue processing (optimized with early termination)
- **State Updates**: O(1) for individual operations
- **Memory**: Efficient in-memory storage with minimal overhead
- **Concurrency**: Thread-safe operations support multiple concurrent requests

## Design Decisions

### Why In-Memory Storage?

- **Performance**: Sub-millisecond response times
- **Simplicity**: No external dependencies, easier to deploy and test
- **Scalability**: Sufficient for the required scale (10^4-10^5 entities)
- **Thread Safety**: Proper synchronization ensures data consistency

### Why Binary Search for Car Finding?

- **Efficiency**: O(log n) vs O(n) linear search
- **Optimal Allocation**: Sorted by available seats ensures best-fit allocation
- **Scalability**: Performance doesn't degrade significantly with large car counts

### Why Separate Repository Layer?

- **Separation of Concerns**: Clear distinction between business logic and data access
- **Testability**: Easy to mock repositories for unit testing
- **Maintainability**: Changes to storage implementation don't affect business logic
- **Flexibility**: Easy to swap storage implementation (e.g., to database) if needed

## Migration from PHP Implementation

This Java implementation is based on a PHP Laravel + Octane solution. Key similarities:

- **Architecture**: Same layered architecture (Controllers, Services, Repositories)
- **Algorithms**: Identical binary search and queue processing algorithms
- **Business Logic**: Same fairness rules and assignment strategy
- **API Contract**: Fully compatible REST API

Key differences:

- **Storage**: ConcurrentHashMap instead of Swoole Tables
- **Concurrency**: Java synchronization instead of Swoole workers
- **Framework**: Spring Boot instead of Laravel Octane

## Development Workflow

### Typical Development Session (with Live Reload)

```bash
# 1. Start the server with live reload
make dev-live

# 2. In another terminal, watch logs
make logs

# 3. Make code changes in your IDE...
#    - Edit Java files
#    - Save → Spring Boot DevTools auto-reloads (5-10 seconds)
#    - No need to restart!

# 4. Test your changes
curl http://localhost:9091/status

# 5. Run unit tests
make test

# 6. When finished
make stop
```

### Debugging

#### Remote Debugging (Recommended)

```bash
# 1. Start server with debugging enabled
make dev-debug

# 2. In your IDE (VS Code, IntelliJ, Eclipse):
#    - Connect to localhost:5005
#    - Set breakpoints
#    - Make requests → Debugger stops at breakpoints

# VS Code: Press F5 (already configured in .vscode/launch.json)
# IntelliJ: Run > Edit Configurations > Remote JVM Debug (localhost:5005)
```

#### Container Debugging

```bash
# Enter the container
make ssh

# Inside the container you can:
# - Check Java version: java -version
# - View application files: ls -la /app
# - Check processes: ps aux
# - Exit: exit
```

### Development Modes

- **`make dev`**: Production-like mode (JAR, no live reload) - fastest startup
- **`make dev-live`**: Development mode with live reload - auto-reloads on code changes
- **`make dev-debug`**: Development mode with debugging - connect IDE to port 5005

### Docker Compose (Optional)

The project includes a `docker-compose.yml` for CI/CD integration with Cabify's test harness. For local development, you don't need it - just use `make dev`.

The harness service is commented out because the image (`cabify/challenge:latest`) is only available in Cabify's internal registry. It will be used automatically in GitLab CI.

## Production Readiness Recommendations

1. **Monitoring & Alerting**
   - Integrate with APM tools (New Relic, Datadog, etc.)
   - Set up alerts for error rates, response times, queue sizes
   - Monitor memory usage and thread pool health

2. **Metrics Collection**
   - Expose Prometheus metrics endpoint
   - Track: request rate, latency, queue size, car utilization
   - Monitor JVM metrics (heap, GC, threads)

3. **High Availability**
   - Run multiple instances behind load balancer
   - Consider distributed cache (Redis) for shared state if multi-instance is needed
   - Implement graceful shutdown handling

4. **Security**
   - Add authentication/authorization if needed
   - Implement rate limiting
   - Input validation and sanitization

5. **Performance Tuning**
   - Tune thread pool sizes based on load
   - Monitor and adjust JVM memory settings
   - Profile and optimize hot paths

## Troubleshooting

### Port Already in Use

```bash
# Find what's using port 9091
lsof -i :9091

# Kill the process or change PORT in Makefile
```

### Container Won't Start

```bash
# Check logs for errors
make logs

# Rebuild from scratch
make clean-all
make dev
```

### Changes Not Reflected

```bash
# Full restart
make restart
```

## Quick Reference

| Command | Description |
|---------|-------------|
| `make help` | Show all available commands |
| `make dev` | Start development server |
| `make logs` | View server logs |
| `make status` | Check if service is running |
| `make test-api` | Test all endpoints |
| `make test` | Run unit tests |
| `make ssh` | Enter container |
| `make stop` | Stop server |
| `make clean` | Clean up |

## License

This project is part of a coding challenge for Cabify.
