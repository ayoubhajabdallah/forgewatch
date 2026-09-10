# ForgeWatch frontend

Angular 22 standalone application with strict TypeScript and template checking,
Angular HttpClient, reactive forms, signals, and plain responsive CSS.

## Local development

Use Node.js 24.15+ (tested with 24.20.0) and npm. Keep the existing backend
running at `http://localhost:8080`, then from `frontend/`:

```sh
npm ci
npm start
```

Open `http://127.0.0.1:4200` (or `http://localhost:4200`).
The development proxy in `proxy.conf.json` forwards `/api/**` to
`http://localhost:8080`. The browser uses same-origin requests, so no backend
CORS configuration is needed. Restart the development server after changing
the proxy configuration.

## Views and code structure

- `src/app/core/`: DTO interfaces matching the Java records, typed HttpClient
  API service, and a signal store for machine and incident snapshots.
- `src/app/pages/`: lazy standalone routes for Overview, Machines, and Incidents.
  Machine selection is linkable at `/machines/:id`; all incidents are linkable
  at `/incidents?view=all`.
- `src/app/shared/`: sensor configuration and measurement panel, create-machine
  form, incident table, status badges, and input validators.
- `src/styles.css`: responsive layout, forms, tables, keyboard focus, and status
  styling. No UI framework, remote fonts, decorative charts, or animations.

The overview shows total and running machines, open incidents, **open critical**
incidents, the five most recent incidents, and sensor thresholds for the selected
machine. Counts come from the backend; an unavailable first load displays dashes.
A failed refresh preserves the previous snapshot and labels it as stale.
Sensor failures have their own retry action. Requests time out after ten seconds;
writes are not retried automatically, because their server outcome may be unknown.

Refresh is manual and also runs after machine creation, measurement submission,
and incident resolution. Sensor creation refreshes the selected machine's sensors.
An in-flight sensor request cannot overwrite a newer machine selection.
The forms enforce the backend string limits, nonblank text, finite numbers, and
critical threshold greater than warning threshold. Pending writes disable forms.

## API contracts used

| Method | Endpoint                               | Purpose                                                                     |
| ------ | -------------------------------------- | --------------------------------------------------------------------------- |
| GET    | `/api/machines`                        | Machine list and dashboard counts                                           |
| POST   | `/api/machines`                        | Create with `name`, `location`                                              |
| GET    | `/api/machines/{machineId}/sensors`    | Sensor configuration                                                        |
| POST   | `/api/machines/{machineId}/sensors`    | Create with `name`, `type`, `unit`, `warningThreshold`, `criticalThreshold` |
| POST   | `/api/sensors/{sensorId}/measurements` | Submit `{ value }`                                                          |
| GET    | `/api/incidents/open`                  | Open incidents and critical count                                           |
| GET    | `/api/incidents`                       | History and recent incidents                                                |
| PATCH  | `/api/incidents/{id}/resolve`          | Resolve an incident                                                         |

Sensor types: TEMPERATURE, PRESSURE, VIBRATION, POWER, COOLING_FLOW.
Machine states: RUNNING, IDLE, MAINTENANCE, OFFLINE. New machines start IDLE.
Measurement thresholds are inclusive. Warning readings create alerts; critical
readings may create an incident if the machine has no open incident. The frontend
does not infer changes to machine status from a reading.

## Validation

```sh
npm run typecheck
npm run build
npm run test:e2e
git diff --check
```

`npm run build` also checks Angular templates and writes the production bundle
to `dist/forgewatch/browser/`. Dependency versions are captured in
`package-lock.json`.

Browser tests require the frontend and backend to be running, and an installed
Google Chrome (configured via Playwright's `chrome` channel). The default suite
checks the live overview, mobile layout, incident views, empty states, and
connection failure/recovery. Screenshots and failure traces go to ignored
`test-results/`.

The full write workflow is opt-in because **it leaves a machine, sensor,
measurement, alert, and resolved incident in the local database**. Records use
a unique `Frontend smoke <timestamp>` machine name and `Frontend validation`
location. Existing records are not resolved or modified. The API has no deletion
endpoints, so the test does not attempt cleanup.

PowerShell:

```powershell
$env:FORGEWATCH_LIVE_TEST = '1'
npm run test:e2e
Remove-Item Env:FORGEWATCH_LIVE_TEST
```

POSIX shell:

```sh
FORGEWATCH_LIVE_TEST=1 npm run test:e2e
```

## Current limits

- No polling, streaming, pagination, or measurement-history endpoint. Displayed
  thresholds are configuration, not current sensor readings.
- API timestamps have no timezone offset. The UI displays their supplied wall
  time without claiming a timezone.
- The separate dashboard requests are not an atomic database snapshot.
- The development proxy is not part of the static build. Deployment will need
  same-origin API routing and SPA route fallback; deployment and Compose changes
  are outside this frontend task.
