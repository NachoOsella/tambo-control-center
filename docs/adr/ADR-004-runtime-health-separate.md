# ADR-004: Runtime and health are separate states

Status: Accepted

## Decision

Do not use a single status enum that mixes lifecycle and health.

Examples:

```text
RUNNING + HEALTHY
RUNNING + UNHEALTHY
RUNNING + NOT_CONFIGURED
EXITED + UNKNOWN
```

## Rationale

Docker health is orthogonal to whether the container process exists and is running.
