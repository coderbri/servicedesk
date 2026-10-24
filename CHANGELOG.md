# Changelog

## October 24, 2026

- Set up the Spring Boot application with Java 21 and the Gradle wrapper.
- Configured an Oracle Free datasource for `localhost:1521/FREEPDB1`, with the password supplied through `DB_PASSWORD`.
- Built and started the application successfully; startup logs confirmed a database connection.
- Confirmed the root URL currently returns 404 because no page or route is defined yet.
