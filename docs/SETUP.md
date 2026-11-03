# ServiceDesk setup guide

This guide walks through setting up ServiceDesk on a local computer. Follow the **Required** steps to build and run the application. **Optional checks** help confirm that a particular step worked.

## Prerequisites (Required)

- **Java 21 JDK.** The project uses a Java 21 Gradle toolchain. A JDK is needed to compile and run Java code.
- **Docker Desktop.** ServiceDesk connects to the existing Oracle Database Free container.
- **Git.** Used to clone the repository.
- **Gradle wrapper.** The repository includes `gradlew` and `gradlew.bat`; you do not need to install Gradle separately.

After installing Java and Git, open a terminal and check that they are available:

```bash
java -version
git --version
```

The Java version should report 21. Start Docker Desktop and wait until it reports that Docker is running.

## Get the project (Required)

On the ServiceDesk repository page, use its **Code** menu to copy the HTTPS or SSH clone address. This guide does not embed a repository URL; use the address shown for your repository.

```bash
git clone <repository-address-you-copied>
cd servicedesk
```

Replace the text in angle brackets with the copied address. If the cloned directory has a different name, `cd` into that directory instead. Run the remaining commands from the project root, where `gradlew` is located.

## Start and check Oracle (Required)

The checked-in project configuration connects to Oracle at `localhost:1521/FREEPDB1` as user `SERVICEDESK`. Existing project notes refer to the container as `oracle-free`. This guide assumes that container has already been created and initialized for this project; it does not provide a Docker creation command or database initialization steps.

1. Open Docker Desktop and select **Containers**.
2. Find the existing `oracle-free` container. If it is stopped, use Docker Desktop's **Start** control. Wait for its status to show that it is running and for Oracle to finish starting.
3. Continue below to check the container and database connection details.

**Optional checks (Terminal):** from any directory, list the running container and its published ports:

```bash
docker ps --filter name=oracle-free
docker port oracle-free
```

The output should include `oracle-free` and a host mapping for container port `1521` (typically `0.0.0.0:1521` or `127.0.0.1:1521`). A running container alone does not guarantee Oracle is ready to accept connections; the Gradle or application connection check later is the practical readiness check.

## Provide the database password (Required)

The application reads `DB_PASSWORD` from the environment of the process that starts it. A `.env` file is not loaded automatically by Gradle or Spring Boot. Follow the existing project instructions in [Oracle connection and Gradle test setup](01-oracle-connection-and-gradle-tests.md) to check and load `DB_PASSWORD`. Do not copy credentials into this guide, print them, or commit them.

Load the variable in the same terminal session where you will run Gradle or the application. If you open another terminal, follow those instructions again in that terminal.

## Compile the application (Required)

From the project root, run:

```bash
./gradlew compileJava
```

Gradle will download the configured Gradle version and project dependencies on the first run. A successful compile ends with `BUILD SUCCESSFUL`.

## Run the tests (Required verification)

Run the two isolated tests, which use mocked dependencies and do not need Oracle:

```bash
./gradlew test --tests "com.coderbri.servicedesk.service.ServiceRequestServiceTest"
./gradlew test --tests "com.coderbri.servicedesk.controller.ServiceRequestControllerTest"
```

The full suite includes `ServicedeskApplicationTests`, annotated with `@SpringBootTest`. With the current JPA configuration, this full application-context test requires Oracle to be ready and `DB_PASSWORD` to be set in this terminal. Run the full suite with:

```bash
./gradlew test
```

For more detail about what these tests cover, see [Java compilation and Gradle tests](03-verify-java-compilation-and-tests.md) and [test troubleshooting and verification](06-phase-5-test-troubleshooting-and-verification.md).

## Run the application (Required to use the API)

Make sure Oracle is ready and `DB_PASSWORD` is set in this same terminal, then start ServiceDesk:

```bash
./gradlew bootRun
```

The application listens on port `8080`. Leave this terminal open while using the API. For available routes and examples, see the [API reference](07-api-reference.md). Stop the application with **Ctrl+C** in the terminal where it is running.

## Troubleshooting

### Gradle says no Java installation or toolchain for version 21 is available

Check `java -version` and confirm a Java 21 JDK is installed. If you have multiple Java versions, configure your terminal's `JAVA_HOME`/`PATH` to use the JDK 21 installation, then close and reopen the terminal and retry `./gradlew compileJava`. The project requests Java 21 in `build.gradle`.

### The container is missing, stopped, or still starting

In Docker Desktop, confirm the existing `oracle-free` container is present and running. If it is stopped, start it there and wait for Oracle initialization to finish. If no such container exists, the project files do not document how to create or initialize it; consult the maintainer's existing Oracle setup instructions rather than guessing a Docker image, initialization password, or lifecycle command.

If `docker ps --filter name=oracle-free` shows no running container, check Docker Desktop. If `docker port oracle-free` shows no mapping for `1521`, the container is not reachable at the address configured by the application.

### Oracle reports invalid credentials (`ORA-01017`) or login denied

Confirm that `DB_PASSWORD` is set in the terminal that launches Gradle or the app, and that it matches the `SERVICEDESK` user's password for `FREEPDB1`. Use the check and loading instructions in [Oracle connection and Gradle test setup](01-oracle-connection-and-gradle-tests.md); do not print the password. A value configured in an IDE is not automatically available in a separate terminal.

### Hibernate cannot determine the dialect or read JDBC metadata

With this project's configuration, this can be a follow-on symptom when Hibernate cannot connect to Oracle to read metadata. Check that Oracle is ready, that the host port and `FREEPDB1` service match the configured connection, and that the same terminal has the correct `DB_PASSWORD`. The configured JDBC URL is in `src/main/resources/application.properties`.

### The isolated tests pass but the full test suite fails

The service and controller tests use mocks, so they can pass without Oracle. The `@SpringBootTest` full-context test starts JPA and requires a working Oracle connection and `DB_PASSWORD`. Start and verify Oracle, load the password using the linked instructions, then retry `./gradlew test`.

For additional test-specific guidance, see [test troubleshooting and verification](06-phase-5-test-troubleshooting-and-verification.md).
