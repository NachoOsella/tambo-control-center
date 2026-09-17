# Tambo Control Center

A Java terminal UI for inspecting and operating the Docker Compose project in the current repository.

> [!WARNING]
> Tambo is pre-release and is being implemented in small increments. The current runnable slice provides Compose discovery, automatically refreshed runtime state, and asynchronous startup for the selected service. Stop, restart, logs, and resource statistics are not available yet.

## Why Tambo

Local development stacks spread useful information across several commands and terminal windows. Tambo is intended to bring the most common Compose workflow into one focused terminal view:

- see the services declared by the current Compose project;
- inspect runtime and health state;
- start, stop, and restart services;
- follow logs for the selected service or for the whole project.

The primary UI entity is a Compose **service**, not an individual container. Container instances are runtime information belonging to a service.

## Current status

| Area | Status |
| --- | --- |
| Java project and Maven build | Available |
| Full-screen TUI | Available |
| Compose file discovery | Available from the current directory and its parents |
| Service list | Loaded from the effective Compose configuration |
| Effective Compose configuration loading | Available |
| Runtime and health observation | Refreshed every five seconds and manually with `g` |
| Start, stop, and restart actions | Start selected is available; stop and restart are planned |
| Selected and all-service logs | Planned |
| Stats, events, and resizable panels | Planned |

The repository is intentionally not a complete Docker dashboard yet. The README describes the current implementation separately from the target design so that planned behavior is not mistaken for an available feature.

## Current UI

The current screen has three panels:

```text
┌ Tambo | project ─────────────────────────────────────────────────────┐
│ Services                    │ Details                                │
│ ▸ bank                      │ Service bank                           │
│   challenge                 │ Image redis:7-alpine                   │
│   gateway                   │ Runtime not-created                    │
│   postgres                  │ Health not-configured                  │
├────────────────────────────┴─────────────────────────────────────────┤
│ Logs [selected: bank]                                                  │
│                                                                       │
├───────────────────────────────────────────────────────────────────────┤
│ Tab focus   ↑↓ j/k select   q quit                                    │
└───────────────────────────────────────────────────────────────────────┘
```

### Controls

| Key | Action |
| --- | --- |
| `Tab` | Move focus between panels |
| `Up` / `Down` | Move the service selection |
| `j` / `k` | Move the service selection |
| `u` | Create or start the selected service |
| `g` | Refresh runtime and health state |
| `q` | Quit |

Selection stops at the first and last service. It does not wrap around.

## Requirements

- JDK 21 or newer
- Maven
- A terminal that supports the TUI backend
- Docker Compose must be available on `PATH`.
- The Docker daemon must be accessible for runtime observation.

## Quick start

Run these commands from the repository root:

```bash
mvn test
mvn exec:java -Dexec.mainClass=app.tambo.Main
```

`Main` starts from the current working directory and searches upward for the first supported Compose filename, in this order:

1. `compose.yaml`
2. `compose.yml`
3. `docker-compose.yaml`
4. `docker-compose.yml`

If no file is found, the application exits with an explanatory error instead of opening an empty dashboard.

The application loads service definitions from `docker compose config --format json` and refreshes runtime state from `docker compose ps --all --format json` every five seconds. Press `u` to run `docker compose up -d <service>` for the selected service, or `g` to refresh immediately. Operations run in the background and update runtime from Compose after completion.

## Development fixture

The root [`compose.yaml`](compose.yaml) is a small local stack used as a Compose project context while developing Tambo. It declares:

| Service | Image | Purpose |
| --- | --- | --- |
| `gateway` | `nginx:1.27-alpine` | HTTP service published on port `8080` |
| `challenge` | `alpine:3.22` | Emits a heartbeat every two seconds |
| `bank` | `redis:7-alpine` | Redis service |
| `postgres` | `postgres:17-alpine` | PostgreSQL database named `tambo` |

Start it only when you need the local Docker fixture:

```bash
docker compose up -d
docker compose ps
```

The PostgreSQL credentials in this file are for local development only and must not be reused in production.

## Technology

- Java 21
- Maven
- [TamboUI](https://tamboui.dev/) 0.5.0
- TamboUI JLine 3 terminal backend
- JUnit Jupiter 5.13.4

There is no Spring application, Docker SDK, or packaged executable. Docker integration uses the Docker Compose CLI through Java process APIs rather than calling Docker from UI components.

## Tests

The current tests cover the implemented foundation:

- `ProjectLocatorTest`: supported filenames, parent-directory discovery, precedence, and missing files;
- `ProjectContextTest`: path normalization and project-root validation;
- `ComposeCliConfigReaderTest`: service and image mapping from Compose JSON;
- `ComposeCliRuntimeReaderTest`: runtime, health, ports, exit codes, multiple instances, and missing services;
- `RefreshRuntimeSnapshotTest`: asynchronous results, failures, and overlapping refresh requests;
- `UpServiceTest`: asynchronous startup and duplicate operation rejection;
- `ProcessRunnerTest`: output capture, exit codes, and timeout handling;
- `UiStateTest`: service selection and boundary behavior.

Run the test suite with:

```bash
mvn test
```

## Project structure

```text
src/
├── main/java/app/tambo/
│   ├── Main.java
│   ├── project/             Compose project discovery and context
│   ├── ui/                  TamboUI application and UI state
│   ├── application/         Asynchronous application use cases
│   ├── domain/              Compose service and runtime types
│   └── infrastructure/      Compose CLI and process execution adapters
└── test/java/app/tambo/     Unit tests for the implemented slices

tambo-control-center-docs/
├── docs/                    Product, architecture, UX, and implementation notes
├── adrs/                    Accepted architecture decisions
├── examples/                Example Compose file and UI mockups
├── BACKLOG.md               Prioritized implementation backlog
└── README.md                Documentation index
```

Some empty packages remain as placeholders for future increments. They are not evidence that those layers are already implemented.

## Design direction

The documented design follows a few explicit boundaries:

- Tambo operates on the Compose project discovered from the working directory, not on every container in the Docker daemon.
- Docker Compose remains the source of truth for Compose semantics.
- The first Docker adapter will use `docker compose` commands and structured JSON output where available.
- Runtime state and health state remain separate concepts.
- The UI expresses user intent; infrastructure code owns external processes.
- A visible log view uses at most one active follow process.

These are target decisions for the upcoming increments. They do not imply that the corresponding features are already exposed by the current application.

## Documentation

The project notebook is in [`tambo-control-center-docs`](tambo-control-center-docs/):

- [Decisions](tambo-control-center-docs/docs/00-decisions.md)
- [Product scope](tambo-control-center-docs/docs/01-product.md)
- [Architecture](tambo-control-center-docs/docs/02-architecture.md)
- [Compose integration](tambo-control-center-docs/docs/05-compose-integration.md)
- [TUI and UX](tambo-control-center-docs/docs/06-tui-ux.md)
- [Roadmap](tambo-control-center-docs/docs/11-roadmap.md)
- [Backlog](tambo-control-center-docs/BACKLOG.md)
- [Architecture decision records](tambo-control-center-docs/adrs/)

The project is also being used to learn modern Java through small, reviewable slices. Each increment should leave the code understandable and runnable before the next one begins.
