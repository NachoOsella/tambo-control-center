# ADR-001: Docker Compose project is the operating boundary

Status: Accepted

## Context

The tool could show every container from the Docker daemon or focus on the repository being developed.

## Decision

Tambo operates on the Compose project discovered from the current working directory.

## Consequences

Positive:

- UI matches developer intent.
- Service names come from Compose.
- Global actions have a clear project scope.
- Less noise from unrelated containers.

Negative:

- Tambo is not a global Docker dashboard in V1.
- Cross-project visibility requires a later workspace feature.
