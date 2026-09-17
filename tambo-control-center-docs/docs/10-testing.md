# Estrategia de testing

## Objetivo

Queremos libertad para cambiar UI e infraestructura sin romper el comportamiento. No buscamos 100% coverage.

## Pirámide práctica

```text
few real Docker integration tests
        ↑
adapter/process tests
        ↑
application/domain tests
        ↑
UI interaction tests
```

La forma no tiene que ser literalmente una pirámide. Lo importante es que el core sea rápido de probar.

## Domain tests

Casos buenos:

- mapear ausencia de container a `NOT_CREATED`;
- mantener runtime y health separados;
- agregación de múltiples instances;
- ring buffer elimina líneas viejas correctamente;
- selection no queda apuntando a un service eliminado después de reload.

## Application tests

Usar fakes de ports.

### Restart selected

```text
Given selected=challenge
When restart
Then lifecycle.restart(challenge)
And operation becomes busy
When fake completes
Then refresh is requested
And operation returns idle
```

### No selection

No ejecutar comando y producir feedback local.

### Global operation

Mientras `stop all` está en progreso, individual lifecycle action debe quedar deshabilitada o rechazada.

## Compose command builder tests

Muy importantes porque evitan errores de CLI.

Ejemplos:

```text
up selected -> [docker, compose, up, -d, challenge]
logs all -> [docker, compose, logs, --follow, ...]
```

No testear private methods. Testear comandos generados a través de una unidad con contrato claro.

## DTO mapping tests

Guardar fixtures JSON de outputs representativos de:

- running healthy;
- running no healthcheck;
- exited code 1;
- multiple services;
- multiple containers same service;
- empty `ps`;
- published ports null/empty.

No depender de Docker real para estos tests.

## ProcessRunner tests

Probar con comandos controlados del sistema o scripts fixture, no necesariamente Docker.

Casos:

- stdout;
- stderr;
- nonzero exit;
- timeout;
- streaming output;
- cancellation.

## Integration tests con Docker

Crear un fixture mínimo:

```yaml
services:
  healthy:
    image: nginx:alpine
  fails:
    image: alpine
    command: ["sh", "-c", "exit 7"]
```

Estos tests pueden estar marcados para correr solo cuando Docker está disponible.

No hacer que `mvn test` básico falle en máquinas sin Docker, salvo que decidamos que Docker es requisito del test suite completo.

## UI tests con TamboUI

La documentación de TamboUI incluye runners de test capaces de enviar input y resize.

Casos:

- Down cambia selection;
- `l` cambia log mode;
- `r` manda intención correcta;
- resize cambia ratios respetando min/max;
- narrow terminal usa layout alternativo;
- focus navigation no pierde panel activo.

## Snapshot tests visuales

Pueden servir para algunas vistas, pero no usarlos como única verificación. Son sensibles a cambios cosméticos.

## Concurrency tests

Usar sincronización determinista.

Ejemplo log race:

```text
session A active
switch to B
A emits late line
B emits line
```

Esperado: solo B entra en el buffer actual.

## Error tests

- compose executable missing;
- invalid JSON;
- command exit 1;
- timeout;
- log child exits;
- runtime refresh failure conserva snapshot anterior.

## Manual test checklist

Antes de una release:

```text
[ ] start from project root
[ ] start from nested subdirectory
[ ] compose with all services stopped
[ ] compose partially running
[ ] service without healthcheck
[ ] unhealthy service
[ ] service exits with error
[ ] up selected
[ ] stop selected
[ ] restart selected
[ ] up all
[ ] logs selected
[ ] logs all
[ ] fast selection switching
[ ] terminal resize
[ ] q cleanup
[ ] Ctrl+C cleanup
```
