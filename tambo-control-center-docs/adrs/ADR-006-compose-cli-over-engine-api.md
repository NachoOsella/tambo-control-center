# ADR-006: Keep the Compose CLI as the runtime boundary

Status: Accepted

## Decision

Keep Docker Compose CLI execution as Tambo's runtime boundary instead of adding a Docker Engine API adapter.

## Rationale

Compose resolves project names, profiles, dependencies, interpolation, overrides, and lifecycle semantics. Calling the Engine API directly would require Tambo to reproduce those rules and would create a second source of truth. The CLI also keeps the current project-boundary behavior explicit and testable.

Docker Engine API support remains a future evaluation only if a required capability cannot be provided by Compose commands.
