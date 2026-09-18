# ADR-002: Use Docker Compose CLI first

Status: Accepted

## Context

Java can interact with Docker via CLI processes or a community Docker Engine client.

## Decision

V1 uses `docker compose` via Java process APIs.

## Rationale

Compose CLI already owns project semantics. It exposes structured JSON for the data we need and keeps initial dependencies small.

## Consequences

Positive:

- simple mental model;
- good Java I/O/concurrency learning surface;
- behavior stays close to commands developers already use;
- easy debugging by reproducing command manually.

Negative:

- process startup overhead;
- streaming lifecycle needs careful cleanup;
- some data may be easier through Engine API later.

## Revisit when

Measured performance or missing capabilities justify the added SDK complexity.
