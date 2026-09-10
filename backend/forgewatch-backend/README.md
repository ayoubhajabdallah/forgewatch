# Backend tests

Use Java 21 and run `./mvnw test` (`.\mvnw.cmd test` on Windows) from this directory.

The integration tests require PostgreSQL, defaulting to `localhost:5432/forgewatch`
with the local development username and password `forgewatch`. To use another
test database, set `FORGEWATCH_TEST_DATABASE_URL`, `FORGEWATCH_TEST_DATABASE_USERNAME`
and `FORGEWATCH_TEST_DATABASE_PASSWORD`.

Each run creates a unique `forgewatch_test_<uuid>` schema and removes it when the
test application context closes. The test database user needs permission to create
schemas. Test cleanup is confined to that schema; development tables are not used.
An interrupted JVM can leave its test schema behind.

The suite covers API validation and DTO responses, threshold boundaries, incident
resolution, and PostgreSQL persistence, rollback and concurrent requests. The
application still uses PostgreSQL; no alternate test database is substituted.
