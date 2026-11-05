# ServiceDesk

ServiceDesk is a Java REST API that demonstrates a small service-request workflow. It creates and retrieves requests, validates updates, and enforces a controlled lifecycle while persisting records in Oracle Database Free.

## What it demonstrates

- A layered Spring Boot application with controller, DTO, service, repository, and entity responsibilities.
- REST endpoints for creating, listing, retrieving, and updating requests.
- Bean Validation for required fields and maximum lengths, plus enum-based priority and status values.
- A status lifecycle that allows a request to remain in its current status or advance one step: `OPEN` → `IN_PROGRESS` → `RESOLVED` → `CLOSED`.
- Service tests with Mockito and HTTP-level controller tests with MockMvc.

Invalid input or status transitions return `400 Bad Request`; a request ID that does not exist returns `404 Not Found`. New requests begin in `OPEN` status. The full scope and explicit non-goals are in [01-MVP-SCOPE-BACKEND.md](docs/01-MVP-SCOPE-BACKEND.md).

## Technology

Java 21 · Spring Boot 4.1.1 · Gradle 9.8.2 · Spring MVC · Spring Data JPA · Jakarta Bean Validation · Oracle Database Free · JUnit · Mockito · MockMvc

## API

All routes use `/api/requests`. See the [API reference](docs/04-API-REFERENCE.md) for request bodies, response examples, validation rules, and cURL commands.

| Method | Endpoint | Purpose | Success |
| --- | --- | --- | --- |
| `POST` | `/api/requests` | Create a request | `201 Created` |
| `GET` | `/api/requests` | List requests | `200 OK` |
| `GET` | `/api/requests/{id}` | Retrieve one request | `200 OK` |
| `PUT` | `/api/requests/{id}` | Update request fields and status | `200 OK` |

## Run and test

Start with the [setup guide](docs/02-SETUP.md). Compiling the application and running the isolated service/controller tests do not require Oracle. Running the API or full local test suite requires Oracle Database Free and the `DB_PASSWORD` environment variable.

GitHub Actions runs the isolated service and controller test classes. Its green result does not cover the Oracle-dependent full-context test or verify Oracle persistence. The database requirements and test commands are detailed in [Database and tests](docs/03-DATABASE-AND-TESTS.md).

Once Oracle is configured and `DB_PASSWORD` is available in the current terminal, start the API with:

```bash
./gradlew bootRun
```

The API listens at `http://localhost:8080`. Use Postman or another API client to try the endpoints. The [Oracle quota and Postman guide](docs/05-ORACLE-QUOTA-AND-POSTMAN.md) includes a manual database-backed check.

## Project boundaries

This is a backend portfolio demonstration. It has no frontend, application authentication or authorization, delete endpoint, deployment configuration, or Oracle persistence integration test in CI. Do not expose it publicly with real user data.

## Documentation

| File | Purpose |
| --- | --- |
| [01-MVP-SCOPE-BACKEND.md](docs/01-MVP-SCOPE-BACKEND.md) | Portfolio demo scope and non-goals. |
| [02-SETUP.md](docs/02-SETUP.md) | Clone instructions, prerequisites, and setup paths. |
| [03-DATABASE-AND-TESTS.md](docs/03-DATABASE-AND-TESTS.md) | Oracle configuration, `DB_PASSWORD`, and test choices. |
| [04-API-REFERENCE.md](docs/04-API-REFERENCE.md) | Implemented API contract and endpoint examples. |
| [05-ORACLE-QUOTA-AND-POSTMAN.md](docs/05-ORACLE-QUOTA-AND-POSTMAN.md) | Oracle quota troubleshooting and manual Postman verification. |
| [`docs/notes/`](docs/notes/) | Learning notes and historical troubleshooting. |
| [CHANGELOG.md](CHANGELOG.md) | Chronological project history. |

`HELP.md` contains optional generic Spring Boot references.

Learning notes:

- [02-create-request-implementation-walkthrough.md](docs/notes/02-create-request-implementation-walkthrough.md) explains request creation and the responsibilities of each application layer.
- [05-automated-test-learning-notes.md](docs/notes/05-automated-test-learning-notes.md) explains how the service and controller tests are set up and what they cover.
- [06-full-suite-test-troubleshooting-history.md](docs/notes/06-full-suite-test-troubleshooting-history.md) records past full-suite troubleshooting and verification.
- [08-github-actions-ci-and-authentication-notes.md](docs/notes/08-github-actions-ci-and-authentication-notes.md) covers CI learning and GitHub push authentication; it does not describe application authentication.
