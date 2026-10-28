# Milestone: Oracle Connection and Gradle Tests Resolved

**Status:** Build and tests succeeded after the terminal process received the database password.

### Problem

Gradle tests failed while Spring Boot initialized JPA. Oracle reported `ORA-01017: invalid credential or not authorized; logon denied`. Hibernate also reported that it could not determine a dialect without JDBC metadata. That dialect message was downstream: Hibernate could not connect to Oracle to read metadata, so it could not identify the database dialect.

### Root cause

The Gradle process did not have the correct `DB_PASSWORD` in its environment. Environment variables set in an IntelliJ Run Configuration are not automatically passed to a separate terminal process. A `.env` file is also not loaded automatically by Gradle or Spring Boot.

### Solution

1. Check whether the password is present without printing the secret:

   ```bash
   if [ -n "$DB_PASSWORD" ]; then
     echo "DB_PASSWORD is set in this terminal"
   else
     echo "DB_PASSWORD is NOT set in this terminal"
   fi
   ```

2. From the project root, load the gitignored `.env` into the current shell and export its variables to child processes:

   ```bash
   set -a
   source .env
   set +a
   ```

3. From that same terminal session, verify production compilation and tests:

   ```bash
   ./gradlew compileJava
   ./gradlew test
   ```

   For a full clean build, you can run `./gradlew clean build`; this also compiles and runs the tests. These commands inherit `DB_PASSWORD` from the current shell. The presence check above confirms it is set without displaying the credential.

### Key takeaway

Credentials must be available to the process that starts the application or tests. Loading `.env` in one terminal does not load it in another terminal or in an IDE run configuration.

**Security:** Keep `.env` out of Git and never print, commit, or share its contents. The project `.gitignore` excludes `.env`. Load the variables again in each new terminal session that needs them.
