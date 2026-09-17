# ADR-005: Let Compose resolve Compose configuration

Status: Accepted

## Decision

Use `docker compose config --format json` and map its output instead of implementing Compose YAML resolution in Tambo.

## Rationale

Compose itself handles interpolation, merged files, normalization and supported syntax. Reimplementing that logic would create a second Compose interpreter.
