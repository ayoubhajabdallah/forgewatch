# forgewatch
Industrial Machine Monitoring and Incident Management Platform

## Run the backend and PostgreSQL with Docker

From the repository root, with Docker running Linux containers and Compose v2+:

```sh
docker compose config
docker compose build backend
docker compose up -d
docker compose ps
docker compose logs -f backend
```

The multi-stage Dockerfile builds with Java 21 JDK and the checked-in Maven
wrapper, then copies only the Spring Boot jar into a Java 21 JRE image running
as a non-root user. Maven dependencies are cached with BuildKit. Image packaging
skips test execution because the integration suite requires a running database;
see [backend test instructions](backend/forgewatch-backend/README.md) to run it.

Compose starts the backend after PostgreSQL's healthcheck passes. Allow a few
more seconds for Spring Boot to initialize, then smoke-test the database-backed
API (use `curl.exe` in Windows PowerShell):

```sh
curl --fail --show-error http://localhost:8080/api/machines
```

Expect HTTP 200 and a JSON array, which may be empty. The backend is available
at `http://localhost:8080`; use `/api/machines` rather than `/`, which has no
endpoint. Both published ports (8080 and 5432) are bound to the local host.

The backend connects to `postgres:5432` over the Compose network through
`SPRING_DATASOURCE_*` environment overrides. PostgreSQL keeps the existing
`forgewatch-data` named volume. `docker compose down` stops and removes the
containers while retaining data; **do not use `down -v`** unless you intend to
delete the database.

`POSTGRES_DB`, `POSTGRES_USER`, and `POSTGRES_PASSWORD` can be overridden through
the shell environment; all default to `forgewatch` for local development. These
PostgreSQL settings initialize an empty volume only: changing them does not
rename databases or rotate credentials in an existing volume. Use credentials
matching the existing database.

## Run the backend locally

With Java 21 installed, start only the database and run Maven from the backend:

```sh
docker compose stop backend
docker compose up -d postgres
cd backend/forgewatch-backend
./mvnw spring-boot:run
```

On Windows use `.\mvnw.cmd spring-boot:run`. The unchanged application properties
connect to `localhost:5432/forgewatch`. If database credentials were overridden,
set matching `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and
`SPRING_DATASOURCE_PASSWORD` in the local backend process environment too.

## Deployment considerations

The Compose defaults are for local use. Before deployment, supply appropriate
database credentials and backups. The existing Hibernate `ddl-auto=update`
behavior remains unchanged and needs a migration strategy before production.
Base image tags receive updates; pin reviewed digests if reproducible images
are required. PostgreSQL readiness gates initial startup; ongoing application
readiness is checked through the API, not a backend container healthcheck.
