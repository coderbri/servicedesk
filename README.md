# ServiceDesk

ServiceDesk is a Java REST API for recording and tracking service requests. It demonstrates a small support workflow: create a request, retrieve requests, and update request details and lifecycle status while storing records in Oracle Database Free.

## Current capabilities

- Create a service request with a title, description, and priority.
- Retrieve all requests or one request by ID.
- Update a request's title, description, priority, and status.
- Validate required fields and maximum lengths.
- Enforce the status progression `OPEN` → `IN_PROGRESS` → `RESOLVED` → `CLOSED`; a request may remain in its current status, and unsupported transitions are rejected.
- Return HTTP errors for invalid input or status transitions (`400 Bad Request`) and missing request IDs (`404 Not Found`).

Supported priorities are `LOW`, `MEDIUM`, and `HIGH`. New requests start with `OPEN` status and receive an ID and creation timestamp when persisted.

## Technology

- Java 21
- Spring Boot 4.1.1
- Gradle 9.8.2 (Gradle Wrapper)
- Oracle Database Free and Oracle JDBC driver
- Spring Data JPA
- Jakarta Bean Validation
- JUnit, Mockito, and MockMvc for automated tests

## Architecture

The application uses a layered structure under `com.coderbri.servicedesk`:

| Package | Responsibility |
| --- | --- |
| `controller` | Maps HTTP routes, validates incoming DTOs, and returns HTTP responses. |
| `dto` | Defines create/update request payloads and the response representation. |
| `service` | Coordinates request operations and enforces status transition rules. |
| `repository` | Uses Spring Data JPA to persist and retrieve entities. |
| `entity` | Defines the persisted `ServiceRequest` model and priority/status enums. |

Requests flow from the controller through the DTO and service layers to the repository and Oracle database. Responses are mapped from entities to a response DTO.

## API overview

The API is served under `/api/requests` on the default local port `8080`.

| Method | Endpoint | Behavior | Success response |
| --- | --- | --- | --- |
| `POST` | `/api/requests` | Create a request | `201 Created` with a request response |
| `GET` | `/api/requests` | Retrieve all requests | `200 OK` with an array |
| `GET` | `/api/requests/{id}` | Retrieve one request | `200 OK`; `404 Not Found` if absent |
| `PUT` | `/api/requests/{id}` | Replace editable request fields | `200 OK`; `404 Not Found` if absent; `400 Bad Request` for invalid status transitions or validation |

Create request:

```json
{
  "title": "Laptop Wi-Fi issue",
  "description": "Cannot connect to the office network.",
  "priority": "HIGH"
}
```

Example response shape (the ID and timestamp are generated and vary):

```json
{
  "id": 1,
  "title": "Laptop Wi-Fi issue",
  "description": "Cannot connect to the office network.",
  "status": "OPEN",
  "priority": "HIGH",
  "createdAt": "2026-10-26T17:16:25.571153"
}
```

An update request supplies all editable fields, including the requested status:

```json
{
  "title": "Laptop Wi-Fi issue",
  "description": "Cannot connect to the office network.",
  "priority": "HIGH",
  "status": "IN_PROGRESS"
}
```

## Prerequisites

- Java 21.
- Oracle Database Free available locally with the `FREEPDB1` service on `localhost:1521` and the application database user configured.
- `DB_PASSWORD` available in the environment of the terminal that runs the application or tests.

See [Oracle connection and Gradle test setup](docs/01-oracle-connection-and-gradle-tests.md) for database and `DB_PASSWORD` instructions. The `.env` file is not loaded automatically; keep credentials local and do not commit them.

## Run and verify

From the project root, make sure Oracle is running and `DB_PASSWORD` is available, then start the API:

```bash
./gradlew bootRun
```

The API is available at `http://localhost:8080`. There is no frontend; interact with the REST endpoints using an API client such as Postman or `curl`.

Compile and run the automated tests with:

```bash
./gradlew compileJava
./gradlew test
```

The full test suite includes a Spring application-context test that uses the configured Oracle connection. The service and controller tests use mocked dependencies and do not verify Oracle persistence. See [test verification notes](docs/03-verify-java-compilation-and-tests.md) and [service request test notes](docs/05-understanding-service-request-tests.md) for details.

For request fields, response shapes, status codes, validation rules, status transitions, and cURL examples, see the [ServiceDesk API reference](docs/07-api-reference.md).

## Project status and scope

The current project is a backend API focused on service-request create, read, and update workflows. It has no delete endpoint, authentication, frontend, or deployment configuration. The service and controller tests cover application behavior with mocks; they are not a dedicated Oracle integration-test suite. The root path `/` has no page or route.

For milestone history, see [CHANGELOG.md](CHANGELOG.md). Additional implementation and troubleshooting notes are in [`docs/`](docs/).
