# Procedure: Verify Java compilation and Gradle tests

Run these commands from the project root, in order:

```bash
./gradlew compileJava
./gradlew test
```

### What each command checks

- `compileJava` compiles production Java source code. It catches syntax errors, missing types, and other compile-time problems. It does not run the automated tests.
- `test` compiles and runs the test source using Gradle's test task. This project has a Spring Boot `contextLoads` test, which starts the application context and connects to the configured Oracle database. The database must be reachable and `DB_PASSWORD` must be available to this terminal.

The `./gradlew` wrapper uses the project's configured Gradle version, so a separate system Gradle installation is not needed.

### Supply the database password

If the project `.env` contains `DB_PASSWORD`, load it in the same terminal before running `test`:

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

The check reveals only whether the variable is set, not its value. Do not paste or print credentials. Loading `.env` applies to this shell session; repeat it in a new terminal as needed.

### Inspect the result

Gradle prints `BUILD SUCCESSFUL` when the task completes successfully. For individual test outcomes, open:

- HTML report: `build/reports/tests/test/index.html`
- Machine-readable XML results: `build/test-results/test/`

These are generated build outputs and are ignored by Git. If a task fails, read the first relevant `Caused by` message and the task name in the terminal output; a later framework error can be a symptom of an earlier cause.

### Troubleshooting sequence

1. Run `./gradlew compileJava` first. Resolve source or dependency errors before investigating database behavior.
2. If `test` fails at Spring startup, confirm Oracle is running and `src/main/resources/application.properties` points to the expected service. This project uses `jdbc:oracle:thin:@//localhost:1521/FREEPDB1` and username `SERVICEDESK`.
3. Check `DB_PASSWORD` presence using the safe check above. If missing, load `.env` into this terminal and retry.
4. For `ORA-01017`, verify the credentials and that the database account is for the intended PDB. Hibernate's dialect/metadata error may simply mean the database connection failed first.
5. Re-run `./gradlew test`, then inspect the HTML report or XML results for the failing test and stack trace.

### Key takeaway

Compilation confirms that production code builds. Tests check behavior and, for this project's context test, also depend on its runtime database configuration. A successful compile alone does not prove that the application can connect to Oracle.
