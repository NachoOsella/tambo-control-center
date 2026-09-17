# ADR-003: Compose service is the primary entity

Status: Accepted

## Decision

The main list and user actions are service-oriented. Containers are runtime instances belonging to a service.

## Consequence

The model must allow zero, one or multiple container instances for a service, even if V1 mostly exercises zero/one.
