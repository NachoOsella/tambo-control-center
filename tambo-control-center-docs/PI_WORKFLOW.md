# Working agreement para Pi

Este archivo está pensado para pegarlo o adaptarlo como instrucciones del coding agent durante el proyecto.

```text
# Tambo project instructions

Tambo is a Java TUI for operating the Docker Compose project in the current repository.
The primary UI entity is a Docker Compose service, not a container.

## Decision ownership

I own architecture and product decisions.
Do not introduce a new framework, architectural layer, persistent storage, event bus, reactive library, Docker SDK, plugin system, or major abstraction without asking first.

When a task requires a meaningful design decision:
1. read the relevant code;
2. explain the smallest viable options;
3. state the tradeoffs concretely;
4. stop before implementation if the choice changes architecture.

Do not ask about routine implementation details that follow from an existing decision.

## Existing decisions

- Docker Compose project is the application context.
- `tambo` discovers the nearest Compose file by walking upward from cwd.
- Compose service is the primary domain/UI entity.
- Runtime state and health state are separate.
- V1 uses Docker Compose CLI through Java processes.
- Use `docker compose config --format json` as the effective Compose model.
- Use structured CLI output when available.
- UI must not invoke Docker directly.
- One main screen with panels.
- Logs support selected-service and all-services modes.
- Keep at most one active log follow process for the visible log scope.
- Do not add Spring.
- Prefer simple Java and small dependencies.

## Implementation rules

- Read relevant files before editing.
- Keep changes scoped to the requested slice.
- Do not perform unrelated refactors.
- Preserve boundaries between domain/application/infrastructure/ui.
- Docker/Jackson DTOs stay in infrastructure.
- Rendering code has no external side effects.
- Docker commands never run on the UI event thread.
- Every long-lived process must have explicit cancellation/cleanup.
- Never optimistically fabricate Docker runtime state after an action. Refresh observed state.
- Prefer command argument lists over shell strings.
- Do not parse Compose YAML ourselves unless explicitly approved.
- Add tests for behavior introduced by the change.

## Code style

- Code, comments, commit messages and technical identifiers in English.
- Prefer records for immutable data where they fit.
- Use sealed types only when the closed hierarchy is useful.
- Avoid abstraction for hypothetical future implementations.
- Favor composition over inheritance.
- Keep names concrete.

## Before finishing a task

Report:
- files changed;
- behavior added;
- important Java concepts used;
- failure/cancellation behavior;
- tests run;
- any design issue that should be decided before the next slice.
```

## Prompt de inicio recomendado

Para arrancar Milestone 0:

```text
Read the project documentation first, especially README.md, docs/00-decisions.md,
docs/02-architecture.md, docs/06-tui-ux.md and docs/11-roadmap.md.

We are starting Milestone 0 only.
Do not integrate Docker yet.

Set up the smallest Java project that can run a full-screen TamboUI application with:
- a hardcoded service list;
- keyboard selection;
- services/details/logs panels;
- a status/help bar;
- terminal resize support;
- q to quit cleanly.

Before editing, inspect the current repository and tell me which TamboUI API level you propose to use and why. If that choice has architectural consequences, wait for my decision. Otherwise implement it.
```

## Prompt de revisión

Después de cada milestone:

```text
Review the implementation as a Java mentor, not as a code generator.
Do not edit yet.

Explain:
1. the runtime flow end to end;
2. which classes own which responsibilities;
3. the Java concepts I should understand in this milestone;
4. anything over-engineered;
5. any hidden concurrency/resource lifecycle issue;
6. the smallest refactors worth doing before the next milestone.
```
