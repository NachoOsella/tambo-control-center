# Prompts por milestone para el coding agent

Estos prompts están pensados para que el agente implemente y vos sigas tomando decisiones.

## Milestone 0: TamboUI spike

```text
Read docs/README.md, docs/guide/00-decisions.md, docs/guide/06-tui-ux.md and
docs/guide/11-roadmap.md.

We are implementing Milestone 0 only. Do not add Docker integration.

First inspect the repository and the current TamboUI documentation/version available to the project. Propose the smallest API level for this UI. Explain the tradeoff between Toolkit DSL and TuiRunner in this specific app.

After I choose, implement:
- a full-screen app;
- hardcoded services;
- keyboard selection;
- services/details/logs panels;
- status/help bar;
- clean quit;
- terminal resize behavior.

Keep architecture minimal. Do not create future Docker ports yet.
```

## Milestone 1: Compose discovery

```text
Implement only Compose project discovery and service definition loading.

Requirements:
- walk upward from cwd looking for supported Compose filenames;
- use `docker compose config --format json`;
- do not parse YAML directly;
- map only data needed to list services;
- execute the command outside the UI event thread;
- show a clear bootstrap error if Compose cannot be loaded.

Before coding, show me the proposed classes and explain why each boundary exists. Do not add runtime status yet.
```

## Milestone 2: runtime

```text
Add runtime and health observation using `docker compose ps --all --format json`.

Keep RuntimeState and HealthState separate.
A service declared in Compose but absent from ps must still appear as NOT_CREATED.
Do not assume one container forever. Group entries by Compose service.

Implement manual refresh first. Do not add events or stats.
Add fixtures for running, unhealthy, exited, not-created and multiple instances.
```

## Milestone 3: lifecycle

```text
Add up/stop/restart for the selected service.

Use Compose commands, not Docker Engine SDK.
Do not block the UI thread.
Track local operation state separately from observed runtime state.
Do not optimistically set Docker runtime after command success. Trigger a fresh runtime observation.
Reject duplicate lifecycle actions for a service while one is active.

Show me the failure and cancellation model before editing if it requires a new abstraction.
```

## Milestone 4: logs selected

```text
Add selected-service live logs.

Use one long-lived `docker compose logs --follow` process.
Implement explicit cancellation and cleanup.
Use a bounded in-memory line buffer.
Changing selected service must replace the active log session.
Late output from a cancelled session must not enter the new buffer.

Do not add all-services mode yet.
```

## Milestone 5: all logs

```text
Add LogMode.SELECTED_SERVICE and LogMode.ALL_SERVICES.
Keep at most one active log process.
Switching mode closes/replaces the previous session.
Add visible scope text in the logs panel.
Do not add per-service log caches yet.
```

## Milestone 6: global actions

```text
Add up all, stop all and restart all.
Global lifecycle operations must not race with individual lifecycle operations.
Keep `down` out of scope.
```

## Milestone 7: resizable UI

```text
Add keyboard-controlled panel resizing and a narrow-terminal layout.
Keep layout ratios in UI state, separate from project/domain state.
Do not persist preferences yet.
Do not add mouse dragging yet.
Add UI tests for min/max ratios and terminal resize.
```

## Milestone 8: events

```text
Add optional `docker compose events --json` observation.
Events should invalidate/request runtime refresh. They are not the sole source of truth.
Keep periodic reconciliation as fallback.
Coalesce event bursts if repeated refreshes become a measured issue.
```

## Milestone 9: stats

```text
Add resource snapshots using `docker compose stats --no-stream --format json`.
Poll at a human-scale interval, not per render frame.
Show CPU/memory in details first. Do not add charts unless the raw values prove insufficient.
```

## Prompt para evitar overengineering

```text
Before implementing, actively look for anything in your proposal that exists only for a hypothetical future requirement. Remove it unless it protects a real external boundary, concurrency lifecycle, or test seam used in this milestone.
```

## Prompt para aprender del código generado

```text
Do not change code. Teach me this milestone from the actual implementation.
Pick the five most important Java concepts present in the diff and point to the exact files/methods where they matter. Then give me three small questions I should be able to answer before moving on.
```
