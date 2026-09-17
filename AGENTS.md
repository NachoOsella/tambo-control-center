# AGENTS.md

Tambo is a Java TUI for managing Docker Compose projects.

The goal is to learn Java and architecture while building the project. The user makes the architectural decisions. The agent mainly implements them.

## Workflow

Work in very small increments.

Do not implement an entire milestone, backlog, or roadmap at once.

Each increment should:

* have one clear goal,
* touch few files,
* introduce one main concept,
* leave the project working,
* end with a focused diff review.

After each increment, stop.

Before a meaningful architectural decision, present the options and wait for the user's choice.

## Project constraints

Tambo runs inside a Docker Compose project and discovers its Compose file from the current directory.

The main entity shown in the UI is the Compose service, not the container instance.

The main screen uses adjustable panels:

* services,
* service details,
* logs.

Logs should eventually support:

* selected service,
* all services.

Runtime state and health state are separate concepts.

Prefer Docker Compose commands for Compose behavior instead of reimplementing Compose semantics.

Do not call Docker directly from TamboUI components.

Do not add Spring, Reactor, dependency injection frameworks, or large libraries unless the user approves them.

Avoid abstractions that are not needed by the current increment.

## After each increment

Explain:

* what changed,
* the important parts of the diff,
* the Java concept involved,
* what was verified,
* one possible next small increment.

Do not implement the next increment automatically.
