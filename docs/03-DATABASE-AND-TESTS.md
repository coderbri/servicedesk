# Oracle Database setup and Gradle tests

This is the project's single reference for preparing Oracle locally, supplying the database password, and choosing which Gradle checks to run. The application configuration expects Oracle at `localhost:1521/FREEPDB1`, with an application account named `SERVICEDESK` and its password in `DB_PASSWORD`.

## Choose what you need

| Goal | Oracle needed? | What to run |
| --- | --- | --- |
| Compile the application | No | `./gradlew compileJava` |
| Run the isolated service and controller tests | No | The two filtered commands in [Run isolated tests](#run-isolated-tests) |
| Start the API or run the complete test suite | Yes | Prepare Oracle below, then run `./gradlew bootRun` or `./gradlew test` |

The isolated tests use mocks. They do not test Oracle persistence. The complete suite includes `ServicedeskApplicationTests`, which starts the full Spring context and connects to Oracle with the current JPA configuration. GitHub Actions runs only the isolated service and controller tests; it excludes `ServicedeskApplicationTests`.

## Prerequisites

For compiling or running isolated tests:

- Java 21 JDK
- Git, to clone the project
- The included Gradle wrapper (`./gradlew`); a separate Gradle installation is not needed

For the API or complete test suite, also install and start Docker Desktop (or another Docker-compatible container runtime) and provide enough memory and disk for an Oracle Database Free container. The exact requirements can change by Oracle image version; check the [Oracle Database Free container guide](https://github.com/oracle/docker-images/blob/main/OracleDatabase/SingleInstance/README.md#running-oracle-database-26ai-free-in-a-container) before downloading an image. Oracle's [installation guide](https://docs.oracle.com/en/database/oracle/oracle-database/26/xeinl/installing-oracle-database-free.html) describes the database and default `FREEPDB1` service.

These are the external setup references used for this procedure. They are maintained by Oracle and may change. This repository's setup instructions have not been executed against a newly created container as part of this documentation update; follow the verification steps below and note the image version if you report a successful fresh setup.

## Prepare a fresh local Oracle database

The following is a Docker-based example for macOS/Linux shells. Oracle's current container guide also documents Podman and may update its image tags or requirements. Follow its current instructions if a command here no longer matches the supported image.

1. Start Docker Desktop. Create a local file named `.env.oracle-db` in the project root containing an administrator password for the new database:

   ```text
   ORACLE_PWD=replace_with_a_private_admin_password
   ```

   Replace the sample value with a long, unique password made of letters and digits. This keeps it straightforward to use in both the environment file and SQL*Plus. Do not commit or share this file. The project `.gitignore` ignores `.env.*`; verify with `git check-ignore .env.oracle-db`. Docker uses the value to initialize administrative accounts. It is different from the application user's `DB_PASSWORD`.

2. Pull and start Oracle Database Free with a persistent named volume:

   ```bash
   docker pull container-registry.oracle.com/database/free:latest
   docker run -d --name oracle-free \
     -p 1521:1521 \
     --env-file .env.oracle-db \
     -v oracle-free-data:/opt/oracle/oradata \
     container-registry.oracle.com/database/free:latest
   ```

   If Oracle Container Registry asks you to sign in or accept terms, follow its prompts. The `latest` tag can change over time; for a repeatable setup, select and record a versioned tag from Oracle's registry. The named volume keeps database files when the container stops or is recreated.

3. Wait for first-time database creation to finish. Check the container output:

   ```bash
   docker logs -f oracle-free
   ```

   Wait for Oracle's ready message, then press **Ctrl+C** to stop following the log (this does not stop the container). Confirm it is running and port 1521 is published:

   ```bash
   docker ps --filter name=oracle-free
   docker port oracle-free
   ```

4. Create the application account in `FREEPDB1`. Open SQL*Plus inside the container; it prompts for the administrator password from `.env.oracle-db`:

   ```bash
   docker exec -it oracle-free sqlplus sys@FREEPDB1 as sysdba
   ```

   At the SQL*Plus prompt, confirm the current database, then create the app user. Replace the password placeholder with a private password of your choice; do not put it in a saved script or commit it.

   ```sql
   SHOW CON_NAME;
   CREATE USER SERVICEDESK IDENTIFIED BY "replace_with_a_private_app_password"
     DEFAULT TABLESPACE USERS QUOTA 100M ON USERS;
   GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE TO SERVICEDESK;
   EXIT
   ```

   `SHOW CON_NAME` should report `FREEPDB1`. If `SERVICEDESK` already exists, do not repeat `CREATE USER`; connect as an administrator to `FREEPDB1` and grant the quota and privileges to the existing user instead:

   ```sql
   ALTER USER SERVICEDESK QUOTA 100M ON USERS;
   GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE TO SERVICEDESK;
   ```

5. Supply the application account's password to this terminal. Create or update the gitignored `.env` file in the project root with one line:

   ```text
   DB_PASSWORD=replace_with_the_private_app_password
   ```

   Use the same long, unique alphanumeric password you entered for `SERVICEDESK` in SQL*Plus. Avoid shell metacharacters so Bash can safely load this simple environment file.

   **Quick fix when `DB_PASSWORD` is missing:** from the project root, load the gitignored `.env` into the current shell and export its variables to child processes. This is a favorite project tip because it fixes the common case where a new terminal has not inherited the database password:

   ```bash
   set -a
   source .env
   set +a
   if [ -n "$DB_PASSWORD" ]; then
     echo "DB_PASSWORD is set in this terminal"
   else
     echo "DB_PASSWORD is NOT set in this terminal"
   fi
   ```

   A `.env` file is not loaded automatically by Gradle or Spring Boot. Repeat this in each new terminal that runs Gradle or the application. Keep the administrator password in `.env.oracle-db` separate from the application password in `.env`.

## Verify the database connection

Connect as the application user and check the PDB and username. SQL*Plus prompts for the app password:

```bash
docker exec -it oracle-free sqlplus SERVICEDESK@FREEPDB1
```

At the SQL*Plus prompt:

```sql
SHOW CON_NAME;
SELECT USER FROM DUAL;
EXIT
```

Expect `FREEPDB1` and `SERVICEDESK`. Then, from the same terminal where `DB_PASSWORD` is loaded, run:

```bash
./gradlew test
```

`BUILD SUCCESSFUL` confirms the complete local test task passed, including the full-context test. For individual test results, open the generated HTML report at `build/reports/tests/test/index.html` or inspect XML results under `build/test-results/test/`. These are generated build outputs ignored by Git.

If a test fails, check the failing test class and the first relevant `Caused by` message. An Oracle/JDBC metadata error during `ServicedeskApplicationTests` startup usually means the full-context test could not connect to the configured database; use the troubleshooting steps below. To verify the API and persistence manually, start it with `./gradlew bootRun` and follow the create and retrieve examples in the [API reference](04-API-REFERENCE.md).

## Run isolated tests without Oracle

These two commands do not need Docker, Oracle, or `DB_PASSWORD`:

```bash
./gradlew test --tests "com.coderbri.servicedesk.service.ServiceRequestServiceTest"
./gradlew test --tests "com.coderbri.servicedesk.controller.ServiceRequestControllerTest"
```

They compile the test sources and execute only the named test classes. They do not execute `ServicedeskApplicationTests` or verify database connectivity or persistence. The same two classes are the ones selected by the GitHub Actions workflow.

## Troubleshooting

### The container is missing, stopped, or still starting

Use `docker ps -a --filter name=oracle-free` to inspect it. Start an existing stopped container with `docker start oracle-free`. For a new container, follow [Prepare a fresh local Oracle database](#prepare-a-fresh-local-oracle-database). A running container is not necessarily ready; wait for its startup log's ready message.

### Port 1521 is already in use

Stop the other database/container using that host port, or choose another host port and update `spring.datasource.url` in `src/main/resources/application.properties` to match. The default setup in this guide uses host port 1521.

### Oracle reports invalid credentials (`ORA-01017`)

Confirm `DB_PASSWORD` is set in the same terminal that launches Gradle or the app and that it is the `SERVICEDESK` password for `FREEPDB1`, not the Oracle administrator password. Do not print the secret to check it; use the presence check above.

### Hibernate cannot determine the dialect or read JDBC metadata

This can follow a failed database connection. Check the container readiness, port, PDB service, application username, and `DB_PASSWORD` before treating it as a separate Hibernate issue.

### Oracle reports insufficient tablespace quota (`ORA-01950`)

Connect as an administrator to `FREEPDB1` and grant the application's `SERVICEDESK` user a quota on `USERS`, as shown in the existing-user SQL above. See [Oracle quota troubleshooting and API verification](05-ORACLE-QUOTA-AND-POSTMAN.md) for the manual Postman verification.
