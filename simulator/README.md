# ForgeWatch sensor simulator

A small Go command-line program representing an external industrial sensor.
It sends synthetic JSON readings to the existing Spring Boot endpoint:

`POST /api/sensors/{sensorId}/measurements` with `{"value": 25.3}`.

The simulator uses only the Go standard library. Its bounded random walk makes
small changes around the configured range midpoint, with occasional larger
variation. These are illustrative synthetic readings, **not real ABP process
values** or a calibrated physical process model.

## Run

Use Go 1.26 or newer. From `simulator/`, choose an **existing** sensor ID from
ForgeWatch. The simulator does not create machines or sensors.

```sh
# Send one reading, then exit.
go run . -sensor-id 4 -mode one-shot -min 20 -max 30

# Send immediately, then every two seconds after each request completes.
go run . -sensor-id 4 -interval 2s -min 20 -max 30

# A fixed value is useful for exercising an exact threshold.
go run . -sensor-id 4 -mode one-shot -min 100 -max 100

# Another backend (an optional URL path prefix is supported).
go run . -backend-url http://localhost:8080 -sensor-id 4 -mode one-shot
```

Sensor ID 4 is an example; replace it with an ID that exists in your database.
Values use that sensor's configured unit. Choose the range with its thresholds
in mind: readings are persisted and can trigger backend alerts and incidents.
The backend alone decides their severity and whether to open an incident.
The simulator does not change machine status or resolve incidents.

For continuous operation, build and run the binary so Ctrl+C is delivered directly
to it rather than through `go run`:

```sh
go build -o bin/forgewatch-simulator .
./bin/forgewatch-simulator -sensor-id 4 -interval 2s
```

On Windows:

```powershell
go build -o bin/forgewatch-simulator.exe .
.\bin\forgewatch-simulator.exe -sensor-id 4 -interval 2s
```

Ctrl+C stops the interval wait and cancels an in-flight HTTP request. SIGTERM is
also handled on platforms that send it. Request cancellation cannot undo a
measurement the backend already saved.

## Configuration

CLI flags override environment variables, which override defaults.

| Flag | Environment variable | Default |
| --- | --- | --- |
| `-backend-url` | `FORGEWATCH_BACKEND_URL` | `http://localhost:8080` |
| `-sensor-id` | `FORGEWATCH_SENSOR_ID` | Required, positive integer |
| `-interval` | `FORGEWATCH_INTERVAL` | `5s` |
| `-min` | `FORGEWATCH_MIN_VALUE` | `20` |
| `-max` | `FORGEWATCH_MAX_VALUE` | `30` |
| `-mode` | `FORGEWATCH_MODE` | `continuous` (`one-shot` also supported) |

Empty environment variables use defaults. Intervals must be positive Go durations
such as `500ms` or `2s`. Bounds must be finite, minimum must not exceed maximum,
and their difference must fit a float64. Equal bounds produce a constant reading.
Use `go run . -h` for flag help.

Environment example in PowerShell:

```powershell
$env:FORGEWATCH_SENSOR_ID = '4'
$env:FORGEWATCH_INTERVAL = '2s'
$env:FORGEWATCH_MIN_VALUE = '20'
$env:FORGEWATCH_MAX_VALUE = '30'
go run . -mode one-shot
```

## HTTP behavior and logs

- One request at a time, with a five-second HTTP timeout.
- Every 2xx response is accepted; non-2xx responses and network errors are logged.
  Redirects are rejected, and error response bodies are limited to 4 KiB.
- Logs go to stderr with UTC timestamps, generated value, sensor ID, HTTP status
  (or `unavailable`), and any error.
- One-shot mode returns exit code 0 on acceptance, 1 on request failure.
  Invalid configuration returns 2. Help and graceful shutdown return 0.
  `go run` may report a child's nonzero exit code differently; use the binary
  when exact exit codes matter.
- Continuous mode logs failures and generates the next reading after the interval.
  Failed measurements are not queued or replayed: a request can be saved even
  when its response is lost. There is no delivery guarantee or outage buffering.

## Validate

```sh
gofmt -w *.go
go test ./...
go vet ./...
git diff --check
```

On Windows, use `gofmt -w (Get-ChildItem -File *.go).Name` because PowerShell does
not expand `*.go` for native tools.
Tests use seeded generation, local HTTP test servers, and cancellation. They cover
configuration and precedence, reading bounds and precision, JSON requests, error
responses, redirects, timeouts, one-shot behavior, continuous recovery, and shutdown.
They do not contact your ForgeWatch database.

The frontend and simulator are separate clients of the same backend. This module
does not change Spring Boot, Angular, or Docker Compose. There are no external Go
dependencies, and no `go.sum` is needed. Local tooling, caches, and built binaries
are excluded by `.gitignore`.
