# ServiceDesk setup guide

This guide gets the project from a fresh clone to compilation, tests, or a running API. **Choose your goal first:** compiling and running the isolated tests do not require Oracle. Running the API and the complete test suite do require Oracle Database Free and the `DB_PASSWORD` environment variable.

## Choose your path

| Goal | Requirements | Follow |
| --- | --- | --- |
| Compile and run isolated service/controller tests | Java 21 and Git | [Oracle-free checks](#oracle-free-checks) |
| Start the API or run the full test suite | Java 21, Git, Docker-compatible runtime, and a configured Oracle database | [Database-dependent setup](#database-dependent-setup) |

The full test suite includes `ServicedeskApplicationTests`, which starts the Spring application context and connects to Oracle. GitHub Actions runs only the isolated service and controller test classes, not this Oracle-dependent test.

## Prerequisites

- **Java 21 JDK.** Check with `java -version`.
- **Git.** Check with `git --version` and use it to clone the repository.
- **Gradle wrapper.** The project includes `gradlew` and `gradlew.bat`; a separate Gradle installation is not needed.
- **Only for the API or complete test suite:** Docker Desktop or another Docker-compatible runtime, an Oracle Database Free instance configured for the project, and its `SERVICEDESK` user's password.

## Get the project

On the repository page, use the **Code** menu to copy its HTTPS or SSH clone address:

```bash
git clone https://github.com/coderbri/servicedesk.git
cd servicedesk
```

Replace the text in angle brackets with the copied address. If the cloned directory has another name, change into that directory. Run the commands below from the project root, where `gradlew` is located.

## Oracle-free checks

Compile production code:

```bash
./gradlew compileJava
```

Run the isolated service and controller tests:

```bash
./gradlew test --tests "com.coderbri.servicedesk.service.ServiceRequestServiceTest"
./gradlew test --tests "com.coderbri.servicedesk.controller.ServiceRequestControllerTest"
```

These tests use mocked dependencies and do not verify Oracle persistence. They do not run `ServicedeskApplicationTests`.

## Database-dependent setup

The project expects Oracle at `localhost:1521/FREEPDB1`, an application user named `SERVICEDESK`, and that user's password in `DB_PASSWORD`. For a new local database, Docker container steps, user creation, password setup, test commands, and connection verification, follow [Database and tests](03-DATABASE-AND-TESTS.md#prepare-a-fresh-local-oracle-database). That guide also links the Oracle-maintained container instructions used as its external reference.

Once Oracle is ready and `DB_PASSWORD` is loaded into the current terminal, run the full test suite:

```bash
./gradlew test
```

Or start the application:

```bash
./gradlew bootRun
```

The API listens on port `8080`. Leave the terminal open while using it, and stop it with **Ctrl+C**. Available routes and request examples are in the [API reference](04-API-REFERENCE.md).

## Troubleshooting

For Oracle container readiness, host port, credentials, Hibernate metadata, or tablespace quota problems, use the troubleshooting section in [Database and tests](03-DATABASE-AND-TESTS.md#troubleshooting). For detailed learning notes about the test setup and past troubleshooting, see [Phase 5 test notes](notes/06-full-suite-test-troubleshooting-history.md).
