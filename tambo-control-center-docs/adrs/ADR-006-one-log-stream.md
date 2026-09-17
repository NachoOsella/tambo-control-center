# ADR-006: Keep one active visible log stream

Status: Accepted

## Decision

Tambo maintains one `docker compose logs --follow` process for the current log scope.

Scopes:

```text
selected service
all services
```

Changing scope or selected service replaces the stream.

## Consequences

- low resource usage;
- simple lifecycle;
- switching service may lose previous in-memory log buffer in V1;
- per-service cached history can be added later if users miss it.
