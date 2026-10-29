# Changelog

## October 29, 2026

- Added service-layer unit tests with JUnit and Mockito for create, read, update, missing-record handling, valid status transitions, and invalid status transitions.
- Added controller tests with MockMvc for POST, GET, GET-by-ID, and PUT behavior, including response status and JSON checks and blank-title validation.
- Investigated the full-context `ServicedeskApplicationTests` failure and identified it as an Oracle/JDBC metadata connection issue. `ServiceRequestServiceTest` and `ServiceRequestControllerTest` each passed when run independently; after applying the existing documented `DB_PASSWORD` setup, the complete `./gradlew test` suite passed.

## October 28, 2026

- Added `PUT /api/requests/{id}` to update a saved service request, validating request fields and allowing only the defined status transitions `OPEN` → `IN_PROGRESS` → `RESOLVED` → `CLOSED`; invalid transitions return `400 Bad Request`, and an unknown request ID returns `404 Not Found`.
- Added read endpoints for service requests: `GET /api/requests` returns all saved requests, and `GET /api/requests/{id}` returns one request by ID or `404 Not Found` when no matching request exists.
- Reused the existing response DTO and Spring Data JPA repository retrieval methods; no new persistence or DTO files were needed.

## October 26, 2026

- Added the first service-request creation endpoint: `POST /api/requests` validates the input, saves the request through the service and JPA repository, and returns `201 Created` with the generated ID, `OPEN` status, creation timestamp, and request fields.
- Added the service-request entity and lifecycle/priority enums, repository, service, request/response DTOs, and REST controller. Added brief JavaDoc descriptions to identify each file's role.
- Resolved the Gradle test startup failure caused by `ORA-01017`. The terminal Gradle process did not have the correct `DB_PASSWORD`; checking for the variable and loading the ignored `.env` with `set -a; source .env; set +a` made it available. Hibernate's dialect/metadata error was downstream of the failed database connection. The Gradle build and tests then succeeded.
- Resolved the API's HTTP 500 on insert caused by Oracle `ORA-01950` (insufficient quota on `USERS`) by connecting as SYSDBA to `FREEPDB1` and granting `SERVICEDESK` a `100M` quota on `USERS`. Retrying the Postman request returned `201 Created` and a saved request.
- Added four guides in `docs/`: Oracle connection and Gradle test troubleshooting, the service-request API and package roles, compile/test verification, and Oracle quota troubleshooting with Postman steps.
- Security reminder: `.env` is not loaded automatically by Gradle or Spring Boot. Keep it local, do not commit it, and never print or share its contents. The `.gitignore` excludes `.env`.

## October 24, 2026

- Set up the Spring Boot application with Java 21 and the Gradle wrapper.
- Configured an Oracle Free datasource for `localhost:1521/FREEPDB1`, with the password supplied through `DB_PASSWORD`.
- Built and started the application successfully; startup logs confirmed a database connection.
- Confirmed the root URL currently returns 404 because no page or route is defined yet.
