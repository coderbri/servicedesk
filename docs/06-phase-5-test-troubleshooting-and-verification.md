# Milestone: Phase 5 test troubleshooting and verification

## Problem

The first full `./gradlew test` run failed while loading `ServicedeskApplicationTests`. The error said Hibernate could not obtain JDBC metadata to determine the database dialect. This failure happened as Spring Boot started the full application context; it did not identify a failure in either of the new Phase 5 service or controller tests.

## Root cause: the tests load different parts of the application

`ServicedeskApplicationTests` uses `@SpringBootTest`, which starts the full application context. With the current configuration, that includes JPA and Hibernate, which need a working Oracle connection to read JDBC metadata and initialize the context.

The Phase 5 tests isolate narrower layers:

| Test | What it exercises | Oracle required? |
| --- | --- | --- |
| `ServiceRequestServiceTest` | The real service with a mocked repository | No; repository calls are handled by Mockito. |
| `ServiceRequestControllerTest` | The MVC controller with a mocked service | No; it uses a focused web test setup and does not start the database configuration. |
| `ServicedeskApplicationTests` | The full Spring Boot application context | Yes, with the current configuration. |

The service and controller tests check behavior at their respective layers. They do not test Oracle persistence.

## Solution: run the Phase 5 tests, then the full suite

The two Phase 5 test classes were run independently with these commands, and both passed:

```bash
./gradlew test --tests "com.coderbri.servicedesk.service.ServiceRequestServiceTest"
./gradlew test --tests "com.coderbri.servicedesk.controller.ServiceRequestControllerTest"
```

After confirming `DB_PASSWORD` was set using the method already documented in [01-oracle-connection-and-gradle-tests.md](01-oracle-connection-and-gradle-tests.md), the full suite was run again and passed:

```bash
./gradlew test
```

## Troubleshooting sequence

1. Read the failing test class name in the Gradle output. A failure in `ServicedeskApplicationTests` occurs during full-context startup; it is separate from the isolated Phase 5 tests.
2. Run `ServiceRequestServiceTest` and `ServiceRequestControllerTest` individually to check those layers on their own.
3. If the full-context test reports missing JDBC metadata or an undetermined dialect, check the Oracle connection and the terminal's `DB_PASSWORD` environment using the existing instructions in [01-oracle-connection-and-gradle-tests.md](01-oracle-connection-and-gradle-tests.md).
4. Once the database environment is available, rerun `./gradlew test` to verify the complete suite.

## Key takeaway

An isolated service or controller test can pass without Oracle because its downstream dependency is mocked. The full `@SpringBootTest` context includes the current JPA and Oracle configuration, so it needs a working database connection. In this run, both Phase 5 tests passed individually, and the complete suite passed after the documented database environment was available.
