# ServiceDesk Portfolio Demo Scope

**Status:** Implemented backend API demonstration

## Purpose

ServiceDesk demonstrates a small service-request workflow through a Java REST API. It shows how a request can be created, retrieved, validated, persisted in Oracle Database Free, and advanced through a controlled lifecycle. The project is scoped as a portfolio demonstration of backend development and testing, not as a complete or production-ready help desk.

## Implemented capabilities

- Create a request with a title, description, and priority using `POST /api/requests`.
- Retrieve all requests with `GET /api/requests`, or one request by ID with `GET /api/requests/{id}`.
- Update title, description, priority, and status with `PUT /api/requests/{id}`.
- Validate nonblank titles and descriptions, their maximum lengths, and required enum fields.
- Assign each new request an initial `OPEN` status, generated ID, and creation timestamp when persisted.
- Allow a request to stay in its current status or advance one step: `OPEN` → `IN_PROGRESS` → `RESOLVED` → `CLOSED`.
- Return `400 Bad Request` for invalid input or status transitions and `404 Not Found` for missing IDs.
- Persist requests with Spring Data JPA and Oracle Database Free.

## Technical demonstration

The application uses Java 21, Spring Boot, Gradle, Spring MVC, Jakarta Bean Validation, Spring Data JPA, and Oracle JDBC. Its controller, DTO, service, repository, and entity layers demonstrate separation of responsibilities. JUnit, Mockito, and MockMvc tests cover service rules and HTTP behavior.

GitHub Actions runs the isolated service and controller test classes. It does not run the Oracle-dependent full-context test or prove Oracle persistence; see [CI coverage notes](notes/08-github-actions-ci-and-authentication-notes.md).

## Out of scope for this demonstration

- A web frontend or user interface.
- Application authentication, authorization, or user accounts.
- Delete, assignment, filtering, pagination, comments, attachments, or notifications.
- Deployment and production operations.
- A dedicated Oracle persistence integration test in CI.

Do not treat GitHub push authentication instructions as application authentication; the API itself has no authentication.

## Running and trying the project

Start with the [setup guide](02-SETUP.md), use the [API reference](04-API-REFERENCE.md) for the implemented contract, and follow the [Oracle/Postman guide](05-ORACLE-QUOTA-AND-POSTMAN.md) for a manual persistence check. For test choices and database configuration, see [database and tests](03-DATABASE-AND-TESTS.md).

## Possible future extensions

Features such as requester details, assignment, filters, a frontend, authentication, and Oracle-backed integration tests could expand the project later. They are ideas beyond the current demo scope, not implemented capabilities or committed roadmap items.
