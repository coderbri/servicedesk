# ServiceDesk

A Java 21 and Spring Boot application backed by Oracle Free.

## Run locally

Set `DB_PASSWORD` in your environment, start Oracle Free with the `FREEPDB1` service available on `localhost:1521`, then run `./gradlew bootRun`.

The application currently starts and connects to the database; no web routes have been added yet.
